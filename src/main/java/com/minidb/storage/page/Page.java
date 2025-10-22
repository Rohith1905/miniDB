package com.minidb.storage.page;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Page - Fixed 4KB block of data
 */
public class Page {
    public static final int PAGE_SIZE = 4096;
    
    private final PageId pageId;
    private final ByteBuffer data;
    private boolean dirty;
    private int pinCount;
    private long lastAccessTime;
    
    public Page(PageId pageId) {
        this.pageId = pageId;
        this.data = ByteBuffer.allocate(PAGE_SIZE);
        this.dirty = false;
        this.pinCount = 0;
        this.lastAccessTime = System.nanoTime();
    }
    
    public PageId getPageId() { return pageId; }
    public ByteBuffer getData() { return data; }
    
    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void markClean() { this.dirty = false; }
    
    public void pin() { 
        pinCount++; 
        lastAccessTime = System.nanoTime();
    }
    
    public void unpin() { 
        if (pinCount > 0) pinCount--; 
    }
    
    public boolean isPinned() { return pinCount > 0; }
    public long getLastAccessTime() { return lastAccessTime; }
    
    public void writeTo(int offset, byte[] bytes) {
        data.position(offset);
        data.put(bytes);
        markDirty();
    }
    
    public byte[] readFrom(int offset, int length) {
        byte[] bytes = new byte[length];
        data.position(offset);
        data.get(bytes);
        return bytes;
    }
    
    public void clear() {
        data.clear();
        Arrays.fill(data.array(), (byte) 0);
    }
}