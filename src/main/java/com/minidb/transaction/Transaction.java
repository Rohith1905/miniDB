package com.minidb.transaction;

/**
 * Transaction context
 */
public class Transaction {
    private final long txnId;
    private final IsolationLevel isolationLevel;
    private TransactionState state;
    private final long startTime;
    
    public Transaction(long txnId, IsolationLevel isolationLevel) {
        this.txnId = txnId;
        this.isolationLevel = isolationLevel;
        this.state = TransactionState.ACTIVE;
        this.startTime = System.currentTimeMillis();
    }
    
    public long getTxnId() { return txnId; }
    public IsolationLevel getIsolationLevel() { return isolationLevel; }
    public TransactionState getState() { return state; }
    public void setState(TransactionState state) { this.state = state; }
    public long getStartTime() { return startTime; }
}