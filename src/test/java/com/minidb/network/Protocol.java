package com.minidb.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Simple text-based wire protocol for client-server communication
 * 
 * Message Format:
 * [4 bytes: length][payload]
 * 
 * Request: SQL statement as UTF-8 string
 * Response: Status byte + message
 */
public class Protocol {
    
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERROR = 1;
    public static final byte STATUS_RESULT = 2;
    
    /**
     * Encode request message
     */
    public static byte[] encodeRequest(String sql) {
        byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + sqlBytes.length);
        buffer.putInt(sqlBytes.length);
        buffer.put(sqlBytes);
        return buffer.array();
    }
    
    /**
     * Decode request message
     */
    public static String decodeRequest(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.getInt();
        byte[] sqlBytes = new byte[length];
        buffer.get(sqlBytes);
        return new String(sqlBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Encode response message
     */
    public static byte[] encodeResponse(byte status, String message) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + msgBytes.length);
        buffer.put(status);
        buffer.putInt(msgBytes.length);
        buffer.put(msgBytes);
        return buffer.array();
    }
    
    /**
     * Decode response message
     */
    public static Response decodeResponse(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte status = buffer.get();
        int length = buffer.getInt();
        byte[] msgBytes = new byte[length];
        buffer.get(msgBytes);
        String message = new String(msgBytes, StandardCharsets.UTF_8);
        return new Response(status, message);
    }
    
    public record Response(byte status, String message) {
        public boolean isOk() { return status == STATUS_OK; }
        public boolean isError() { return status == STATUS_ERROR; }
        public boolean hasResult() { return status == STATUS_RESULT; }
    }
}