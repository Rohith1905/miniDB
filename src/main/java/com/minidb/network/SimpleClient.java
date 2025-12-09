package com.minidb.network;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Simple command-line client for MiniDB
 */
public class SimpleClient {
    private final String host;
    private final int port;

    public SimpleClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MiniDB Client v1.0                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Connecting to " + host + ":" + port + "...");

        try (
                Socket socket = new Socket(host, port);
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                Scanner scanner = new Scanner(System.in);) {
            System.out.println("✓ Connected!\n");
            System.out.println("Enter SQL commands (or EXIT to quit):");
            System.out.println("Special commands: BEGIN, COMMIT, ROLLBACK\n");

            while (true) {
                System.out.print("minidb> ");
                String sql = scanner.nextLine().trim();

                if (sql.isEmpty())
                    continue;

                // Send request
                byte[] request = Protocol.encodeRequest(sql);
                out.write(request);
                out.flush();

                if (sql.equalsIgnoreCase("EXIT") || sql.equalsIgnoreCase("QUIT")) {
                    // Read final response
                    readResponse(in);
                    break;
                }

                // Read response
                readResponse(in);
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void readResponse(InputStream in) throws IOException {
        byte[] statusByte = in.readNBytes(1);
        if (statusByte.length < 1) {
            System.out.println("Connection closed by server");
            return;
        }

        byte status = statusByte[0];
        byte[] lengthBytes = in.readNBytes(4);
        int length = ByteBuffer.wrap(lengthBytes).getInt();
        byte[] msgBytes = in.readNBytes(length);
        String message = new String(msgBytes, StandardCharsets.UTF_8);

        if (status == Protocol.STATUS_OK) {
            System.out.println("✓ " + message);
        } else if (status == Protocol.STATUS_ERROR) {
            System.out.println("✗ Error: " + message);
        } else if (status == Protocol.STATUS_RESULT) {
            System.out.println(message);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5432;

        SimpleClient client = new SimpleClient(host, port);
        try {
            client.start();
        } catch (IOException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
}
