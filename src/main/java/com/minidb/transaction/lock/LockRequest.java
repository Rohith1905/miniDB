package com.minidb.transaction.lock;

/**
 * Lock request information
 */
public class LockRequest {
    final long txnId;
    final String resourceId;
    final LockMode lockMode;
    final long requestTime;
    volatile boolean granted;
    
    public LockRequest(long txnId, String resourceId, LockMode lockMode) {
        this.txnId = txnId;
        this.resourceId = resourceId;
        this.lockMode = lockMode;
        this.requestTime = System.currentTimeMillis();
        this.granted = false;
    }
}