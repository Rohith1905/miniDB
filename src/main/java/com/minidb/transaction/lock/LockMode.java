package com.minidb.transaction.lock;

public enum LockMode {
    SHARED,      // S - Multiple readers
    EXCLUSIVE    // X - Single writer
}