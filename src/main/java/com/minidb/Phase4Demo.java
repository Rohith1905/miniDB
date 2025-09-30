package com.minidb;

import com.minidb.network.*;
import com.minidb.query.executor.Executor;
import com.minidb.transaction.TransactionManager;
import com.minidb.recovery.RecoveryManager;
import com.minidb.storage.buffer.BufferPool;
import com.minidb.storage.page.PageManager;
import com.minidb.transaction.wal.WALManager;
import com.minidb.transaction.lock.LockManager;

import java.io.IOException;

public class Phase4Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MiniDB Phase 4 Demo: Network Server ===\n");

        // Initialize all components
        System.out.println("Initializing MiniDB...");

        PageManager pageManager = new PageManager("./data");
        BufferPool bufferPool = new BufferPool(100, pageManager);
        WALManager walManager = new WALManager("./data/wal.log");
        LockManager lockManager = new LockManager();
        TransactionManager txnManager = new TransactionManager(
                walManager, lockManager, bufferPool);

        Executor executor = new Executor(txnManager);
        RecoveryManager recoveryManager = new RecoveryManager(walManager, bufferPool);

        System.out.println("✓ Components initialized\n");

        // Start server
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5432;
        DBServer server = new DBServer(port, executor, txnManager, recoveryManager);

        server.start();
    }
}
