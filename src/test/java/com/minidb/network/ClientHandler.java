package com.minidb.network;

import com.minidb.query.parser.*;
import com.minidb.query.executor.*;
import com.minidb.transaction.*;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Handles individual client connection (runs on virtual thread)
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Executor executor;
    private final TransactionManager txnManager;
    private final int clientId;
    private Transaction currentTransaction;

    public ClientHandler(Socket socket, Executor executor,
            TransactionManager txnManager, int clientId) {
        this.clientSocket = socket;
        this.executor = executor;
        this.txnManager = txnManager;
        this.clientId = clientId;
        this.currentTransaction = null;
    }

    @Override
    public void run() {
        System.out.println("[Client " + clientId + "] Connected from " +
                clientSocket.getInetAddress());

        try (
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();) {
            // Start implicit transaction
            if (txnManager != null) {
                currentTransaction = txnManager.begin(IsolationLevel.READ_COMMITTED);
                System.out.println("[Client " + clientId + "] Transaction started: " +
                        currentTransaction.getTxnId());
            }

            while (!clientSocket.isClosed()) {
                // Read request
                byte[] lengthBytes = in.readNBytes(4);
                if (lengthBytes.length < 4)
                    break; // Connection closed

                int length = ByteBuffer.wrap(lengthBytes).getInt();
                byte[] sqlBytes = in.readNBytes(length);
                String sql = new String(sqlBytes, StandardCharsets.UTF_8);

                System.out.println("[Client " + clientId + "] Received: " + sql);

                // Handle special commands
                if (sql.equalsIgnoreCase("BEGIN")) {
                    handleBegin(out);
                    continue;
                } else if (sql.equalsIgnoreCase("COMMIT")) {
                    handleCommit(out);
                    continue;
                } else if (sql.equalsIgnoreCase("ROLLBACK")) {
                    handleRollback(out);
                    continue;
                } else if (sql.equalsIgnoreCase("EXIT") || sql.equalsIgnoreCase("QUIT")) {
                    sendResponse(out, Protocol.STATUS_OK, "Goodbye!");
                    break;
                }

                // Process SQL
                try {
                    Lexer lexer = new Lexer(sql);
                    SQLParser parser = new SQLParser(lexer.tokenize());
                    Statement statement = parser.parse();

                    ExecutionResult result = executor.execute(statement, currentTransaction);

                    if (result.success()) {
                        String response = formatResult(result);
                        sendResponse(out, Protocol.STATUS_RESULT, response);
                    } else {
                        sendResponse(out, Protocol.STATUS_ERROR, result.message());
                    }

                } catch (Exception e) {
                    sendResponse(out, Protocol.STATUS_ERROR, "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Auto-commit on disconnect
            if (currentTransaction != null && txnManager != null) {
                txnManager.commit(currentTransaction.getTxnId());
                System.out.println("[Client " + clientId + "] Auto-committed transaction");
            }

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Connection error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("[Client " + clientId + "] Disconnected");
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void handleBegin(OutputStream out) throws IOException {
        if (currentTransaction != null && txnManager != null) {
            txnManager.commit(currentTransaction.getTxnId());
        }

        if (txnManager != null) {
            currentTransaction = txnManager.begin(IsolationLevel.READ_COMMITTED);
            sendResponse(out, Protocol.STATUS_OK,
                    "Transaction started: " + currentTransaction.getTxnId());
        } else {
            sendResponse(out, Protocol.STATUS_OK, "Transaction started (no txn manager)");
        }
    }

    private void handleCommit(OutputStream out) throws IOException {
        if (currentTransaction != null && txnManager != null) {
            txnManager.commit(currentTransaction.getTxnId());
            sendResponse(out, Protocol.STATUS_OK,
                    "Transaction committed: " + currentTransaction.getTxnId());
            currentTransaction = txnManager.begin(IsolationLevel.READ_COMMITTED);
        } else {
            sendResponse(out, Protocol.STATUS_OK, "Committed (no active transaction)");
        }
    }

    private void handleRollback(OutputStream out) throws IOException {
        if (currentTransaction != null && txnManager != null) {
            txnManager.abort(currentTransaction.getTxnId());
            sendResponse(out, Protocol.STATUS_OK,
                    "Transaction rolled back: " + currentTransaction.getTxnId());
            currentTransaction = txnManager.begin(IsolationLevel.READ_COMMITTED);
        } else {
            sendResponse(out, Protocol.STATUS_OK, "Rolled back (no active transaction)");
        }
    }

    private String formatResult(ExecutionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.message()).append("\n");

        if (!result.resultSet().isEmpty()) {
            sb.append("\nResults (").append(result.resultSet().size()).append(" rows):\n");
            for (int i = 0; i < result.resultSet().size(); i++) {
                sb.append("  ").append(i + 1).append(". ")
                        .append(result.resultSet().get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    private void sendResponse(OutputStream out, byte status, String message)
            throws IOException {
        byte[] response = Protocol.encodeResponse(status, message);
        out.write(response);
        out.flush();
    }
}
