package com.minidb.transaction.lock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LockManager - Row-level locking with deadlock detection
 */
public class LockManager {
    private final Map<String, List<LockRequest>> lockTable;
    private final Map<Long, Set<String>> txnLocks; // Track locks per transaction
    private final Map<Long, Long> waitForGraph; // Simple wait-for: txn -> blocking txn
    private final ReentrantLock managerLock;
    
    public LockManager() {
        this.lockTable = new ConcurrentHashMap<>();
        this.txnLocks = new ConcurrentHashMap<>();
        this.waitForGraph = new ConcurrentHashMap<>();
        this.managerLock = new ReentrantLock();
    }
    
    /**
     * Acquire lock (blocking)
     */
    public boolean acquireLock(long txnId, String resourceId, LockMode mode, long timeoutMs) 
            throws InterruptedException {
        LockRequest request = new LockRequest(txnId, resourceId, mode);
        long deadline = System.currentTimeMillis() + timeoutMs;
        
        while (System.currentTimeMillis() < deadline) {
            managerLock.lock();
            try {
                if (canGrantLock(request)) {
                    grantLock(request);
                    return true;
                } else {
                    // Update wait-for graph
                    Long blockingTxn = findBlockingTransaction(resourceId, txnId);
                    if (blockingTxn != null) {
                        waitForGraph.put(txnId, blockingTxn);
                        
                        // Check for deadlock
                        if (hasDeadlock(txnId)) {
                            waitForGraph.remove(txnId);
                            throw new RuntimeException("Deadlock detected! Aborting txn " + txnId);
                        }
                    }
                }
            } finally {
                managerLock.unlock();
            }
            
            Thread.sleep(10); // Wait before retry
        }
        
        return false; // Timeout
    }
    
    /**
     * Release all locks held by transaction
     */
    public void releaseAllLocks(long txnId) {
        managerLock.lock();
        try {
            Set<String> resources = txnLocks.remove(txnId);
            if (resources != null) {
                for (String resourceId : resources) {
                    List<LockRequest> requests = lockTable.get(resourceId);
                    if (requests != null) {
                        requests.removeIf(req -> req.txnId == txnId);
                        if (requests.isEmpty()) {
                            lockTable.remove(resourceId);
                        }
                    }
                }
            }
            waitForGraph.remove(txnId);
        } finally {
            managerLock.unlock();
        }
    }
    
    private boolean canGrantLock(LockRequest request) {
        List<LockRequest> existing = lockTable.get(request.resourceId);
        if (existing == null || existing.isEmpty()) {
            return true;
        }
        
        // Check compatibility
        for (LockRequest held : existing) {
            if (held.txnId == request.txnId) continue; // Same transaction
            
            if (request.lockMode == LockMode.EXCLUSIVE || 
                held.lockMode == LockMode.EXCLUSIVE) {
                return false; // Conflict
            }
        }
        
        return true; // All shared locks - compatible
    }
    
    private void grantLock(LockRequest request) {
        lockTable.computeIfAbsent(request.resourceId, k -> new ArrayList<>())
                 .add(request);
        txnLocks.computeIfAbsent(request.txnId, k -> ConcurrentHashMap.newKeySet())
                .add(request.resourceId);
        request.granted = true;
        waitForGraph.remove(request.txnId);
    }
    
    private Long findBlockingTransaction(String resourceId, long txnId) {
        List<LockRequest> requests = lockTable.get(resourceId);
        if (requests != null) {
            for (LockRequest req : requests) {
                if (req.txnId != txnId && req.granted) {
                    return req.txnId;
                }
            }
        }
        return null;
    }
    
    /**
     * Simple cycle detection in wait-for graph
     */
    private boolean hasDeadlock(long txnId) {
        Set<Long> visited = new HashSet<>();
        Long current = txnId;
        
        while (current != null) {
            if (visited.contains(current)) {
                return true; // Cycle found!
            }
            visited.add(current);
            current = waitForGraph.get(current);
        }
        
        return false;
    }
}