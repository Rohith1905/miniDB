package com.minidb.storage.table;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Record {
    private final List<byte[]> fields;
    
    public Record() {
        this.fields = new ArrayList<>();
    }
    
    public Record(List<byte[]> fields) {
        this.fields = new ArrayList<>(fields);
    }
    
    // Add typed fields
    public void addField(byte[] data) {
        fields.add(data);
    }
    
    public void addField(String str) {
        fields.add(str.getBytes(StandardCharsets.UTF_8));
    }
    
    public void addField(int value) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(value);
        fields.add(buf.array());
    }
    
    public void addField(long value) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(value);
        fields.add(buf.array());
    }
    
    // Get typed fields
    public byte[] getField(int index) {
        return fields.get(index);
    }
    
    public String getFieldAsString(int index) {
        return new String(fields.get(index), StandardCharsets.UTF_8);
    }
    
    public int getFieldAsInt(int index) {
        return ByteBuffer.wrap(fields.get(index)).getInt();
    }
    
    public long getFieldAsLong(int index) {
        return ByteBuffer.wrap(fields.get(index)).getLong();
    }
    
    public int getFieldCount() {
        return fields.size();
    }
    
    /**
     * Serialize to bytes
     */
    public byte[] serialize() {
        int totalSize = 4; // field count
        for (byte[] field : fields) {
            totalSize += 4 + field.length;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(fields.size());
        
        for (byte[] field : fields) {
            buffer.putInt(field.length);
            buffer.put(field);
        }
        
        return buffer.array();
    }
    
    /**
     * Deserialize from bytes
     */
    public static Record deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int fieldCount = buffer.getInt();
        
        List<byte[]> fields = new ArrayList<>();
        for (int i = 0; i < fieldCount; i++) {
            int len = buffer.getInt();
            byte[] field = new byte[len];
            buffer.get(field);
            fields.add(field);
        }
        
        return new Record(fields);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Record(");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(", ");
            
            byte[] field = fields.get(i);
            if (field.length == 4) {
                sb.append(ByteBuffer.wrap(field).getInt());
            } else if (field.length == 8) {
                sb.append(ByteBuffer.wrap(field).getLong());
            } else {
                sb.append("'").append(new String(field, StandardCharsets.UTF_8)).append("'");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
