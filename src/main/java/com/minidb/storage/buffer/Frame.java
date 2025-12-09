package com.minidb.storage.buffer;

import com.minidb.storage.page.Page;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Frame - Wrapper for a page in buffer pool
 */
public class Frame {
    private Page page;
    private final ReentrantReadWriteLock latch;
    
    public Frame() {
        this.page = null;
        this.latch = new ReentrantReadWriteLock();
    }
    
    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }
    public boolean isEmpty() { return page == null; }
    
    public void lockShared() { latch.readLock().lock(); }
    public void unlockShared() { latch.readLock().unlock(); }
    public void lockExclusive() { latch.writeLock().lock(); }
    public void unlockExclusive() { latch.writeLock().unlock(); }
}