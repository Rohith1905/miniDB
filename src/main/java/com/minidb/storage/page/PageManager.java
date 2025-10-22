package com.minidb.storage.page;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * PageManager - Manages page files on disk
 */
public class PageManager {
    private final Path dataDirectory;
    private final Map<Integer, RandomAccessFile> openFiles;
    private final Map<Integer, FileChannel> fileChannels;
    private final ReentrantLock lock;
    
    public PageManager(String dataDir) throws IOException {
        this.dataDirectory = Paths.get(dataDir);
        this.openFiles = new ConcurrentHashMap<>();
        this.fileChannels = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        
        Files.createDirectories(dataDirectory);
    }
    
    /**
     * Read page from disk
     */
    public Page readPage(PageId pageId) throws IOException {
        FileChannel channel = getFileChannel(pageId.fileId());
        Page page = new Page(pageId);
        
        long position = (long) pageId.pageNumber() * Page.PAGE_SIZE;
        ByteBuffer buffer = page.getData();
        buffer.clear();
        
        channel.position(position);
        channel.read(buffer);
        buffer.flip();
        
        return page;
    }
    
    /**
     * Write page to disk
     */
    public void writePage(Page page) throws IOException {
        FileChannel channel = getFileChannel(page.getPageId().fileId());
        
        long position = (long) page.getPageId().pageNumber() * Page.PAGE_SIZE;
        ByteBuffer buffer = page.getData();
        buffer.position(0);
        
        channel.position(position);
        channel.write(buffer);
        channel.force(false); // Sync to disk
        
        page.markClean();
    }
    
    /**
     * Allocate a new page in the file
     */
    public PageId allocatePage(int fileId) throws IOException {
        lock.lock();
        try {
            FileChannel channel = getFileChannel(fileId);
            int pageNumber = (int) (channel.size() / Page.PAGE_SIZE);
            
            // Extend file
            Page newPage = new Page(new PageId(fileId, pageNumber));
            writePage(newPage);
            
            return new PageId(fileId, pageNumber);
        } finally {
            lock.unlock();
        }
    }
    
    private FileChannel getFileChannel(int fileId) throws IOException {
        return fileChannels.computeIfAbsent(fileId, id -> {
            try {
                Path filePath = dataDirectory.resolve("file_" + id + ".db");
                RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
                openFiles.put(id, raf);
                return raf.getChannel();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
    
    public void close() throws IOException {
        for (FileChannel channel : fileChannels.values()) {
            channel.close();
        }
        for (RandomAccessFile raf : openFiles.values()) {
            raf.close();
        }
    }
}