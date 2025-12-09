package com.minidb;

import com.minidb.query.parser.*;
import com.minidb.query.executor.*;
import com.minidb.storage.table.*;
import com.minidb.transaction.*;

import java.util.List;

public class Phase3Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MiniDB Phase 3 Demo: SQL Query Processing ===\n");
        
        // Initialize minimal components for demo
        TransactionManager txnManager = null; // Simplified for demo
        Executor executor = new Executor(txnManager);
        
        // Demo 1: CREATE TABLE
        System.out.println("--- Demo 1: CREATE TABLE ---");
        String createSQL = """
            CREATE TABLE users (
                id INT PRIMARY KEY,
                name VARCHAR(100),
                email VARCHAR(255),
                age INT
            )
            """;
        
        executeSQL(createSQL, executor, null);
        
        // Demo 2: INSERT statements
        System.out.println("\n--- Demo 2: INSERT ---");
        
        String[] inserts = {
            "INSERT INTO users VALUES (1, 'Alice Johnson', 'alice@example.com', 30)",
            "INSERT INTO users VALUES (2, 'Bob Smith', 'bob@example.com', 25)",
            "INSERT INTO users VALUES (3, 'Charlie Brown', 'charlie@example.com', 35)",
            "INSERT INTO users VALUES (4, 'Diana Prince', 'diana@example.com', 28)",
            "INSERT INTO users VALUES (5, 'Eve Adams', 'eve@example.com', 32)"
        };
        
        for (String sql : inserts) {
            executeSQL(sql, executor, null);
        }
        
        // Demo 3: SELECT queries
        System.out.println("\n--- Demo 3: SELECT Queries ---");
        
        System.out.println("\n3a. SELECT * FROM users");
        executeSQL("SELECT * FROM users", executor, null);
        
        System.out.println("\n3b. SELECT with WHERE (exact match)");
        executeSQL("SELECT * FROM users WHERE id = 3", executor, null);
        
        System.out.println("\n3c. SELECT with BETWEEN (range scan)");
        executeSQL("SELECT * FROM users WHERE id BETWEEN 2 AND 4", executor, null);
        
        // Demo 4: UPDATE
        System.out.println("\n--- Demo 4: UPDATE ---");
        executeSQL("UPDATE users SET name = 'Robert Smith' WHERE id = 2", executor, null);
        executeSQL("SELECT * FROM users WHERE id = 2", executor, null);
        
        // Demo 5: DELETE
        System.out.println("\n--- Demo 5: DELETE ---");
        executeSQL("DELETE FROM users WHERE id = 5", executor, null);
        System.out.println("\nAfter delete:");
        executeSQL("SELECT * FROM users", executor, null);
        
        // Demo 6: Lexer demonstration
        System.out.println("\n--- Demo 6: Lexer (Tokenization) ---");
        demonstrateLexer();
        
        // Demo 7: Parser demonstration
        System.out.println("\n--- Demo 7: Parser (AST Generation) ---");
        demonstrateParser();
        
        System.out.println("\n✅ Phase 3 Complete!");
        System.out.println("\nSupported SQL:");
        System.out.println("  ✅ CREATE TABLE with PRIMARY KEY");
        System.out.println("  ✅ INSERT INTO ... VALUES");
        System.out.println("  ✅ SELECT * FROM ... WHERE");
        System.out.println("  ✅ UPDATE ... SET ... WHERE");
        System.out.println("  ✅ DELETE FROM ... WHERE");
        System.out.println("  ✅ BETWEEN for range queries");
        System.out.println("  ✅ Primary key index optimization");
    }
    
    private static void executeSQL(String sql, Executor executor, Transaction txn) {
        try {
            System.out.println("SQL: " + sql);
            
            // Tokenize
            Lexer lexer = new Lexer(sql);
            List<Token> tokens = lexer.tokenize();
            
            // Parse
            SQLParser parser = new SQLParser(tokens);
            Statement statement = parser.parse();
            
            // Execute
            ExecutionResult result = executor.execute(statement, txn);
            result.print();
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void demonstrateLexer() {
        String sql = "SELECT name, age FROM users WHERE id = 42";
        System.out.println("Input SQL: " + sql);
        System.out.println("\nTokens:");
        
        Lexer lexer = new Lexer(sql);
        List<Token> tokens = lexer.tokenize();
        
        for (Token token : tokens) {
            System.out.println("  " + token);
        }
    }
    
    private static void demonstrateParser() {
        String sql = "SELECT * FROM users WHERE id BETWEEN 10 AND 20";
        System.out.println("Input SQL: " + sql);
        
        Lexer lexer = new Lexer(sql);
        List<Token> tokens = lexer.tokenize();
        
        SQLParser parser = new SQLParser(tokens);
        Statement statement = parser.parse();
        
        System.out.println("\nParsed AST:");
        if (statement instanceof SelectStatement select) {
            System.out.println("  Statement Type: SELECT");
            System.out.println("  Columns: " + select.columns());
            System.out.println("  Table: " + select.tableName());
            System.out.println("  Where Clause: " + select.whereClause());
        }
    }
}
