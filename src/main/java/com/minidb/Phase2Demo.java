package com.minidb;

import com.minidb.storage.table.*;
import com.minidb.storage.index.BPlusTree;

public class Phase2Demo {
    public static void main(String[] args) {
        System.out.println("=== MiniDB Phase 2 Demo ===\n");
        
        // Demo 1: B+ Tree Index
        System.out.println("--- Demo 1: B+ Tree Index ---");
        BPlusTree index = new BPlusTree(1);
        
        // Insert key-value pairs
        index.insert(10, "Alice".getBytes());
        index.insert(20, "Bob".getBytes());
        index.insert(5, "Charlie".getBytes());
        index.insert(15, "David".getBytes());
        index.insert(25, "Eve".getBytes());
        
        // Search
        byte[] result = index.search(15);
        System.out.println("Search key 15: " + (result != null ? new String(result) : "not found"));
        
        // Range scan
        System.out.println("\nRange scan [10, 20]:");
        for (byte[] value : index.rangeScan(10, 20)) {
            System.out.println("  - " + new String(value));
        }
        
        // Demo 2: Slotted Page
        System.out.println("\n--- Demo 2: Slotted Page ---");
        SlottedPage page = new SlottedPage();
        
        com.minidb.storage.table.Record rec1 = new com.minidb.storage.table.Record();
        rec1.addField("John Doe");
        rec1.addField(30);
        
        com.minidb.storage.table.Record rec2 = new com.minidb.storage.table.Record();
        rec2.addField("Jane Smith");
        rec2.addField(25);
        
        int slot1 = page.insertRecord(rec1.serialize());
        int slot2 = page.insertRecord(rec2.serialize());
        
        System.out.println("Inserted 2 records into slots: " + slot1 + ", " + slot2);
        System.out.println("Free space: " + page.getFreeSpace() + " bytes");
        
        // Retrieve
        byte[] data = page.getRecord(slot1);
        com.minidb.storage.table.Record retrieved = com.minidb.storage.table.Record.deserialize(data);
        System.out.println("Retrieved: " + retrieved);
        
        // Demo 3: Table with Primary Key Index
        System.out.println("\n--- Demo 3: Table Operations ---");
        Table usersTable = new Table("users", true);
        
        // Insert records
        com.minidb.storage.table.Record user1 = new com.minidb.storage.table.Record();
        user1.addField(100); // Primary key
        user1.addField("Alice");
        user1.addField("alice@example.com");
        
        com.minidb.storage.table.Record user2 = new com.minidb.storage.table.Record();
        user2.addField(200);
        user2.addField("Bob");
        user2.addField("bob@example.com");
        
        com.minidb.storage.table.Record user3 = new com.minidb.storage.table.Record();
        user3.addField(150);
        user3.addField("Charlie");
        user3.addField("charlie@example.com");
        
        usersTable.insertRecord(user1);
        usersTable.insertRecord(user2);
        usersTable.insertRecord(user3);
        
        System.out.println("Inserted 3 users");
        
        // Search by primary key
        com.minidb.storage.table.Record found = usersTable.searchByPrimaryKey(150);
        System.out.println("\nSearch by PK 150: " + found);
        
        // Range scan
        System.out.println("\nRange scan [100, 200]:");
        for (com.minidb.storage.table.Record user : usersTable.rangeScanByPrimaryKey(100, 200)) {
            System.out.println("  " + user);
        }
        
        // Full scan
        System.out.println("\nFull table scan:");
        for (com.minidb.storage.table.Record user : usersTable.fullScan()) {
            System.out.println("  " + user);
        }
        
        System.out.println("\n✅ Phase 2 Complete!");
        System.out.println("Table stats:");
        System.out.println("  - Pages: " + usersTable.getPageCount());
        System.out.println("  - Records: " + usersTable.getRecordCount());
    }
}