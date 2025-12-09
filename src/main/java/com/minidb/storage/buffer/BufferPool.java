package com.minidb.storage.buffer;

import com.minidb.storage.page.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BufferPool - Main buffer manager with STEAL + NO-FORCE policy
 */
public class BufferPool {
    private final int poolSize;
    private final Frame[] frames;
    private final Map<PageId, Integer> pageTable; // pageId -> frame index
    private final PageManager pageManager;
    private final ReentrantLock poolLock;
    
    public BufferPool(int poolSize, PageManager pageManager) {
        this.poolSize = poolSize;
        this.frames = new Frame[poolSize];
        this.pageTable = new ConcurrentHashMap<>();
        this.pageManager = pageManager;
        this.poolLock = new ReentrantLock();
        
        for (int i = 0; i < poolSize; i++) {
            frames[i] = new Frame();
        }
    }
    
    /**
     * Fetch page (pin it in buffer pool)
     */
    public Page fetchPage(PageId pageId) throws IOException {
        poolLock.lock();
        try {
            // Check if page already in buffer
            Integer frameIdx = pageTable.get(pageId);
            if (frameIdx != null) {
                Frame frame = frames[frameIdx];
                frame.lockShared();
                try {
                    Page page = frame.getPage();
                    page.pin();
                    return page;
                } finally {
                    frame.unlockShared();
                }
            }
            
            // Page not in buffer - need to load it
            int victimFrameIdx = findVictimFrame();
            Frame victimFrame = frames[victimFrameIdx];
            
            victimFrame.lockExclusive();
            try {
                // Evict old page if present
                if (!victimFrame.isEmpty()) {
                    Page oldPage = victimFrame.getPage();
                    if (oldPage.isDirty()) {
                        pageManager.writePage(oldPage); // STEAL policy
                    }
                    pageTable.remove(oldPage.getPageId());
                }
                
                // Load new page
                Page newPage = pageManager.readPage(pageId);
                newPage.pin();
                victimFrame.setPage(newPage);
                pageTable.put(pageId, victimFrameIdx);
                
                return newPage;
            } finally {
                victimFrame.unlockExclusive();
            }
        } finally {
            poolLock.unlock();
        }
    }
    
    /**
     * Unpin page (allow eviction)
     */
    public void unpinPage(PageId pageId, boolean dirty) {
        Integer frameIdx = pageTable.get(pageId);
        if (frameIdx != null) {
            Frame frame = frames[frameIdx];
            frame.lockShared();
            try {
                Page page = frame.getPage();
                if (page != null) {
                    if (dirty) page.markDirty();
                    page.unpin();
                }
            } finally {
                frame.unlockShared();
            }
        }
    }
    
    /**
     * Flush specific page to disk
     */
    public void flushPage(PageId pageId) throws IOException {
        Integer frameIdx = pageTable.get(pageId);
        if (frameIdx != null) {
            Frame frame = frames[frameIdx];
            frame.lockShared();
            try {
                Page page = frame.getPage();
                if (page != null && page.isDirty()) {
                    pageManager.writePage(page);
                }
            } finally {
                frame.unlockShared();
            }
        }
    }
    
    /**
     * Flush all dirty pages to disk
     */
    public void flushAllPages() throws IOException {
        for (Frame frame : frames) {
            frame.lockShared();
            try {
                Page page = frame.getPage();
                if (page != null && page.isDirty()) {
                    pageManager.writePage(page);
                }
            } finally {
                frame.unlockShared();
            }
        }
    }
    
    /**
     * Find victim frame for eviction (LRU policy)
     */
    private int findVictimFrame() {
        // First, try to find an empty frame
        for (int i = 0; i < poolSize; i++) {
            if (frames[i].isEmpty()) {
                return i;
            }
        }
        
        // Find unpinned page with oldest access time (LRU)
        int victimIdx = -1;
        long oldestTime = Long.MAX_VALUE;
        
        for (int i = 0; i < poolSize; i++) {
            Page page = frames[i].getPage();
            if (page != null && !page.isPinned()) {
                if (page.getLastAccessTime() < oldestTime) {
                    oldestTime = page.getLastAccessTime();
                    victimIdx = i;
                }
            }
        }
        
        if (victimIdx == -1) {
            throw new RuntimeException("No victim frame available - all pages pinned!");
        }
        
        return victimIdx;
    }
    
    public int getPoolSize() { return poolSize; }
}