package com.minidb.storage.table;

import java.nio.ByteBuffer;
import java.util.*;

public class SlottedPage {
    private static final int PAGE_SIZE = 4096;
    private static final int HEADER_SIZE = 8;
    private static final int SLOT_SIZE = 8;
    
    private final ByteBuffer buffer;
    private int slotCount;
    private int freeSpacePointer;
    
    public SlottedPage() {
        this.buffer = ByteBuffer.allocate(PAGE_SIZE);
        this.slotCount = 0;
        this.freeSpacePointer = PAGE_SIZE;
        writeHeader();
    }
    
    public SlottedPage(byte[] pageData) {
        this.buffer = ByteBuffer.wrap(pageData);
        readHeader();
    }
    
    /**
     * Insert record, returns slot ID or -1 if no space
     */
    public int insertRecord(byte[] record) {
        int requiredSpace = record.length;
        int headerSpace = HEADER_SIZE + (slotCount + 1) * SLOT_SIZE;
        
        if (freeSpacePointer - headerSpace < requiredSpace) {
            return -1; // No space
        }
        
        // Write record from end
        freeSpacePointer -= record.length;
        buffer.position(freeSpacePointer);
        buffer.put(record);
        
        // Add slot
        int slotId = slotCount;
        writeSlot(slotId, freeSpacePointer, record.length);
        slotCount++;
        writeHeader();
        
        return slotId;
    }
    
    /**
     * Get record by slot ID
     */
    public byte[] getRecord(int slotId) {
        if (slotId < 0 || slotId >= slotCount) return null;
        
        Slot slot = readSlot(slotId);
        if (slot.length == 0) return null; // Deleted
        
        byte[] record = new byte[slot.length];
        buffer.position(slot.offset);
        buffer.get(record);
        return record;
    }
    
    /**
     * Update record
     */
    public boolean updateRecord(int slotId, byte[] newRecord) {
        Slot slot = readSlot(slotId);
        
        if (newRecord.length == slot.length) {
            // In-place update
            buffer.position(slot.offset);
            buffer.put(newRecord);
            return true;
        }
        
        // Delete and reinsert
        deleteRecord(slotId);
        return insertRecord(newRecord) != -1;
    }
    
    /**
     * Delete record (mark slot as free)
     */
    public void deleteRecord(int slotId) {
        writeSlot(slotId, 0, 0);
    }
    
    /**
     * Compact page to reclaim space
     */
    public void compact() {
        List<byte[]> liveRecords = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            byte[] record = getRecord(i);
            if (record != null) {
                liveRecords.add(record);
            }
        }
        
        // Reset page
        buffer.clear();
        slotCount = 0;
        freeSpacePointer = PAGE_SIZE;
        writeHeader();
        
        // Reinsert
        for (byte[] record : liveRecords) {
            insertRecord(record);
        }
    }
    
    public int getFreeSpace() {
        return freeSpacePointer - (HEADER_SIZE + slotCount * SLOT_SIZE);
    }
    
    public int getSlotCount() {
        return slotCount;
    }
    
    public byte[] toBytes() {
        return buffer.array();
    }
    
    private void readHeader() {
        buffer.position(0);
        slotCount = buffer.getInt();
        freeSpacePointer = buffer.getInt();
    }
    
    private void writeHeader() {
        buffer.position(0);
        buffer.putInt(slotCount);
        buffer.putInt(freeSpacePointer);
    }
    
    private Slot readSlot(int slotId) {
        int pos = HEADER_SIZE + slotId * SLOT_SIZE;
        buffer.position(pos);
        int offset = buffer.getInt();
        int length = buffer.getInt();
        return new Slot(offset, length);
    }
    
    private void writeSlot(int slotId, int offset, int length) {
        int pos = HEADER_SIZE + slotId * SLOT_SIZE;
        buffer.position(pos);
        buffer.putInt(offset);
        buffer.putInt(length);
    }
    
    private record Slot(int offset, int length) {}
}
