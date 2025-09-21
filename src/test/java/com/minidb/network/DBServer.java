package com.minidb.network;

import com.minidb.query.executor.Executor;
import com.minidb.transaction.TransactionManager;
import com.minidb.recovery.RecoveryManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniDB Server - TCP server using Java 21 virtual threads
 */
public class DBServer {
    private final int port;
    private final Executor executor;
    private final TransactionManager txnManager;
    private final RecoveryManager recoveryManager;
    private final ExecutorService virtualThreadExecutor;
    private final AtomicInteger clientIdCounter;
    private volatile boolean running;
    
    public DBServer(int port, Executor executor, TransactionManager txnManager,
                   RecoveryManager recoveryManager) {
        this.port = port;
        this.executor = executor;
        this.txnManager = txnManager;
        this.recoveryManager = recoveryManager;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.clientIdCounter = new AtomicInteger(1);
        this.running = false;
    }
    
    /**
     * Start the server
     */
    public void start() throws IOException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MiniDB Server v1.0                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Run recovery on startup
        if (recoveryManager != null) {
            System.out.println("Running crash recovery...");
            recoveryManager.recover();
            System.out.println("Recovery complete.\n");
        }
        
        running = true;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✓ Server started on port " + port);
            System.out.println("✓ Virtual threads enabled");
            System.out.println("✓ Waiting for clients...\n");
            
            // Shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n\nShutting down server...");
                shutdown();
            }));
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientIdCounter.getAndIncrement();
                    
                    // Handle client on virtual thread
                    ClientHandler handler = new ClientHandler(
                        clientSocket, executor, txnManager, clientId
                    );
                    virtualThreadExecutor.submit(handler);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Shutdown server gracefully
     */
    public void shutdown() {
        running = false;
        
        if (recoveryManager != null) {
            System.out.println("Creating checkpoint...");
            try {
                recoveryManager.checkpoint();
            } catch (IOException e) {
                System.err.println("Checkpoint failed: " + e.getMessage());
            }
        }
        
        virtualThreadExecutor.shutdown();
        System.out.println("Server stopped.");
    }
}