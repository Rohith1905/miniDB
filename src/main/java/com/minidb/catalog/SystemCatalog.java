package com.minidb.catalog;

import com.minidb.query.parser.CreateTableStatement;
import java.io.*;
import java.util.*;

/**
 * System Catalog - Persists database metadata
 */
public class SystemCatalog {
    private final String catalogFile;
    private final Map<String, TableMetadata> tables;
    
    public SystemCatalog(String catalogFile) {
        this.catalogFile = catalogFile;
        this.tables = new HashMap<>();
        load();
    }
    
    public void addTable(String tableName, TableMetadata metadata) {
        tables.put(tableName, metadata);
        save();
    }
    
    public TableMetadata getTable(String tableName) {
        return tables.get(tableName);
    }
    
    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName);
    }
    
    public Set<String> getAllTables() {
        return tables.keySet();
    }
    
    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(catalogFile))) {
            oos.writeObject(tables);
        } catch (IOException e) {
            System.err.println("Failed to save catalog: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void load() {
        File file = new File(catalogFile);
        if (!file.exists()) return;
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(catalogFile))) {
            Map<String, TableMetadata> loaded = (Map<String, TableMetadata>) ois.readObject();
            tables.putAll(loaded);
            System.out.println("Loaded " + tables.size() + " tables from catalog");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load catalog: " + e.getMessage());
        }
    }
}

/**
 * Table metadata
 */
record TableMetadata(
    String name,
    List<CreateTableStatement.ColumnDefinition> columns,
    String primaryKeyColumn,
    int fileId
) implements Serializable {}