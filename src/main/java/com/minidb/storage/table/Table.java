package com.minidb.storage.table;

import com.minidb.storage.index.BPlusTree;
import java.nio.ByteBuffer;
import java.util.*;

public class Table {
    private final String tableName;
    private final List<SlottedPage> pages;
    private final BPlusTree primaryIndex;
    private final boolean hasPrimaryKey;
    private int nextRecordId;

    public Table(String tableName, boolean hasPrimaryKey) {
        this.tableName = tableName;
        this.pages = new ArrayList<>();
        this.hasPrimaryKey = hasPrimaryKey;
        this.primaryIndex = hasPrimaryKey ? new BPlusTree(1) : null;
        this.nextRecordId = 1;

        // Create first page
        pages.add(new SlottedPage());
    }

    /**
     * Insert record, returns record ID
     */
    public int insertRecord(Record record) {
        byte[] serialized = record.serialize();

        // Try to insert in existing pages
        for (SlottedPage page : pages) {
            int slotId = page.insertRecord(serialized);
            if (slotId != -1) {
                int recordId = nextRecordId++;

                // Add to index if primary key exists
                if (hasPrimaryKey && record.getFieldCount() > 0) {
                    int primaryKey = record.getFieldAsInt(0);
                    primaryIndex.insert(primaryKey, intToBytes(recordId));
                }

                return recordId;
            }
        }

        // Need new page
        SlottedPage newPage = new SlottedPage();
        int slotId = newPage.insertRecord(serialized);
        pages.add(newPage);

        int recordId = nextRecordId++;
        if (hasPrimaryKey && record.getFieldCount() > 0) {
            int primaryKey = record.getFieldAsInt(0);
            primaryIndex.insert(primaryKey, intToBytes(recordId));
        }

        return recordId;
    }

    /**
     * Get record by ID
     */
    public Record getRecord(int recordId) {
        // Linear scan for now (in real DB, use page directory)
        for (SlottedPage page : pages) {
            for (int i = 0; i < page.getSlotCount(); i++) {
                byte[] data = page.getRecord(i);
                if (data != null) {
                    Record rec = Record.deserialize(data);
                    // Check if this is our record (simplified)
                    return rec;
                }
            }
        }
        return null;
    }

    /**
     * Search by primary key
     */
    public Record searchByPrimaryKey(int primaryKey) {
        if (!hasPrimaryKey) {
            throw new UnsupportedOperationException("No primary key index");
        }

        byte[] recordIdBytes = primaryIndex.search(primaryKey);
        if (recordIdBytes == null)
            return null;

        int recordId = bytesToInt(recordIdBytes);
        return getRecord(recordId);
    }

    /**
     * Range scan by primary key
     */
    public List<Record> rangeScanByPrimaryKey(int startKey, int endKey) {
        if (!hasPrimaryKey) {
            throw new UnsupportedOperationException("No primary key index");
        }

        List<byte[]> recordIds = primaryIndex.rangeScan(startKey, endKey);
        List<Record> results = new ArrayList<>();

        for (byte[] recordIdBytes : recordIds) {
            int recordId = bytesToInt(recordIdBytes);
            Record record = getRecord(recordId);
            if (record != null) {
                results.add(record);
            }
        }

        return results;
    }

    /**
     * Full table scan
     */
    public List<Record> fullScan() {
        List<Record> results = new ArrayList<>();

        for (SlottedPage page : pages) {
            for (int i = 0; i < page.getSlotCount(); i++) {
                byte[] data = page.getRecord(i);
                if (data != null) {
                    results.add(Record.deserialize(data));
                }
            }
        }

        return results;
    }

    /**
     * Update record
     */
    public boolean updateRecord(int recordId, Record newRecord) {
        // Simplified: find and update
        for (SlottedPage page : pages) {
            for (int i = 0; i < page.getSlotCount(); i++) {
                byte[] data = page.getRecord(i);
                if (data != null) {
                    return page.updateRecord(i, newRecord.serialize());
                }
            }
        }
        return false;
    }

    /**
     * Delete record
     */
    public boolean deleteRecord(int recordId) {
        for (SlottedPage page : pages) {
            for (int i = 0; i < page.getSlotCount(); i++) {
                byte[] data = page.getRecord(i);
                if (data != null) {
                    page.deleteRecord(i);
                    return true;
                }
            }
        }
        return false;
    }

    public String getTableName() {
        return tableName;
    }

    public int getPageCount() {
        return pages.size();
    }

    public int getRecordCount() {
        return nextRecordId - 1;
    }

    // Utility methods
    private byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
