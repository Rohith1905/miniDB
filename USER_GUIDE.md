# MiniDB - User Guide

A lightweight database system implementing core database concepts including transactions, recovery, indexing, and SQL query processing.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Building the Project](#building-the-project)
- [Running MiniDB](#running-minidb)
- [Using the Database](#using-the-database)
- [SQL Commands](#sql-commands)
- [Phase Demos](#phase-demos)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- **Java 21** or higher
- **Maven 3.6+** (for building)
- **Windows/Linux/macOS** terminal

### Verify Installation

```bash
java -version    # Should show Java 21+
mvn -version     # Should show Maven 3.6+
```

---

## Building the Project

### 1. Navigate to Project Directory

```bash
cd C:\Users\rohit\code\java\minidb
```

### 2. Clean and Compile

```bash
mvn clean compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

---

## Running MiniDB

### Option 1: Network Server (Phase 4 - Recommended)

This runs a client-server database with network access.

#### Start the Server

```bash
java -cp target/classes com.minidb.Phase4Demo
```

**Expected Output:**
```
=== MiniDB Phase 4 Demo: Network Server ===

Initializing MiniDB...
✓ Components initialized

╔════════════════════════════════════════════════╗
║          MiniDB Server Started                 ║
║          Listening on port 5432                ║
╚════════════════════════════════════════════════╝

Waiting for client connections...
```

#### Connect a Client

Open a **new terminal** and run:

```bash
java -cp target/classes com.minidb.network.SimpleClient localhost 5432
```

**Expected Output:**
```
Connected to MiniDB server at localhost:5432
Type SQL commands (END with semicolon) or EXIT to quit

minidb>
```

---

### Option 2: Standalone Demos

Run individual phase demonstrations:

#### Phase 1: Storage Layer
```bash
java -cp target/classes com.minidb.Phase1Demo
```
Demonstrates: Page management, slotted pages, record storage

#### Phase 2: Indexing
```bash
java -cp target/classes com.minidb.Phase2Demo
```
Demonstrates: B+ tree indexing, range queries

#### Phase 3: Transactions
```bash
java -cp target/classes com.minidb.Phase3Demo
```
Demonstrates: ACID transactions, concurrency control, recovery

---

## Using the Database

### Basic Workflow

1. **Start a transaction**
2. **Execute SQL commands**
3. **Commit or rollback**
4. **Exit**

### Example Session

```sql
minidb> BEGIN
Transaction started: 1

minidb> CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100))
Table users created

minidb> INSERT INTO users VALUES (1, 'Alice', 'alice@example.com')
Inserted 1 row with ID 1

minidb> INSERT INTO users VALUES (2, 'Bob', 'bob@example.com')
Inserted 1 row with ID 2

minidb> SELECT * FROM users
Retrieved 2 rows

Results:
  1. Record[fields=[[49], [65, 108, 105, 99, 101], [97, 108, 105, 99, 101, 64, 101, 120, 97, 109, 112, 108, 101, 46, 99, 111, 109]]]
  2. Record[fields=[[50], [66, 111, 98], [98, 111, 98, 64, 101, 120, 97, 109, 112, 108, 101, 46, 99, 111, 109]]]

minidb> UPDATE users SET name = 'Alice Smith' WHERE id = 1
Updated 1 rows

minidb> DELETE FROM users WHERE id = 2
Deleted 1 rows

minidb> COMMIT
Transaction 1 committed

minidb> EXIT
Disconnected from server
```

---

## SQL Commands

### Supported Statements

#### CREATE TABLE
```sql
CREATE TABLE table_name (
    column1 INT PRIMARY KEY,
    column2 VARCHAR(length),
    column3 LONG
)
```

**Example:**
```sql
CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(50), price LONG)
```

#### INSERT
```sql
INSERT INTO table_name VALUES (value1, value2, value3)
```

**Example:**
```sql
INSERT INTO products VALUES (1, 'Laptop', 999)
```

#### SELECT
```sql
SELECT * FROM table_name
SELECT column1, column2 FROM table_name WHERE condition
```

**Examples:**
```sql
SELECT * FROM products
SELECT name, price FROM products WHERE id = 1
SELECT * FROM products WHERE id BETWEEN 1 AND 10
```

#### UPDATE
```sql
UPDATE table_name SET column1 = value1, column2 = value2 WHERE condition
```

**Example:**
```sql
UPDATE products SET price = 899 WHERE id = 1
```

#### DELETE
```sql
DELETE FROM table_name WHERE condition
```

**Example:**
```sql
DELETE FROM products WHERE id = 1
```

### Transaction Commands

```sql
BEGIN       -- Start a new transaction
COMMIT      -- Commit the current transaction
ROLLBACK    -- Abort and rollback the current transaction
```

### Special Commands

```sql
EXIT        -- Disconnect from server and quit
```

---

## Phase Demos

### Phase 1: Storage Layer Demo

**What it demonstrates:**
- Page-based storage
- Slotted page format
- Record serialization/deserialization

**Run:**
```bash
java -cp target/classes com.minidb.Phase1Demo
```

**Output shows:**
- Creating and managing pages
- Inserting records into pages
- Reading records back

---

### Phase 2: Indexing Demo

**What it demonstrates:**
- B+ tree index creation
- Point queries (exact match)
- Range queries (BETWEEN)

**Run:**
```bash
java -cp target/classes com.minidb.Phase2Demo
```

**Output shows:**
- Building B+ tree index
- Searching for specific keys
- Range scans

---

### Phase 3: Transactions Demo

**What it demonstrates:**
- ACID transaction properties
- Write-Ahead Logging (WAL)
- Crash recovery
- Concurrency control

**Run:**
```bash
java -cp target/classes com.minidb.Phase3Demo
```

**Output shows:**
- Starting transactions
- Logging operations
- Simulating crashes
- Recovery process

---

### Phase 4: Network Server Demo

**What it demonstrates:**
- Client-server architecture
- Network protocol
- Multi-client support
- Full SQL query processing

**Run:**
```bash
# Terminal 1: Start server
java -cp target/classes com.minidb.Phase4Demo

# Terminal 2: Connect client
java -cp target/classes com.minidb.network.SimpleClient localhost 5432
```

---

## Troubleshooting

### Port Already in Use

**Error:**
```
java.net.BindException: Address already in use: bind
```

**Solutions:**

1. **Find and kill the process using port 5432:**
   ```bash
   # Windows
   netstat -ano | findstr :5432
   taskkill /PID <PID> /F
   
   # Linux/Mac
   lsof -i :5432
   kill -9 <PID>
   ```

2. **Or change the port in Phase4Demo.java:**
   ```java
   private static final int PORT = 5433; // Change from 5432
   ```

---

### Compilation Errors

**Error:**
```
[ERROR] COMPILATION ERROR
```

**Solution:**
```bash
mvn clean compile
```

If errors persist, ensure:
- Java 21 is installed: `java -version`
- No duplicate files with Chinese characters exist
- All imports are correct

---

### Client Connection Failed

**Error:**
```
Connection refused
```

**Solutions:**
1. Ensure server is running first
2. Check firewall settings
3. Verify port number matches (default: 5432)
4. Try `127.0.0.1` instead of `localhost`

---

### Transaction Errors

**Error:**
```
Transaction X is not active
```

**Solution:**
Always start with `BEGIN` before executing SQL commands:
```sql
BEGIN
-- your SQL commands here
COMMIT
```

---

## Advanced Usage

### Running Multiple Clients

You can connect multiple clients to the same server:

```bash
# Terminal 1: Server
java -cp target/classes com.minidb.Phase4Demo

# Terminal 2: Client 1
java -cp target/classes com.minidb.network.SimpleClient localhost 5432

# Terminal 3: Client 2
java -cp target/classes com.minidb.network.SimpleClient localhost 5432
```

Each client gets its own transaction context.

---

### Data Persistence

MiniDB creates the following files in the project directory:
- `minidb.wal` - Write-Ahead Log for recovery
- `minidb.catalog` - System catalog (table metadata)
- Page files for data storage

**To reset the database:**
```bash
# Delete all database files
rm minidb.wal minidb.catalog
```

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│         Client Application              │
│    (SimpleClient / Your App)            │
└──────────────┬──────────────────────────┘
               │ TCP/IP (Port 5432)
┌──────────────▼──────────────────────────┐
│         Network Layer                   │
│    (DBServer, ClientHandler)            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Query Executor                  │
│    (SQL Parser, Executor)               │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Transaction Manager                │
│    (ACID, Concurrency Control)          │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Storage Layer                   │
│  (Pages, Buffer Pool, B+ Tree Index)    │
└─────────────────────────────────────────┘
```

---

## Features

✅ **Storage Management**
- Page-based storage with slotted page format
- Buffer pool for caching
- Record serialization

✅ **Indexing**
- B+ tree index for fast lookups
- Range query support

✅ **Transactions**
- ACID properties
- Write-Ahead Logging (WAL)
- Crash recovery (ARIES-style)
- Lock-based concurrency control

✅ **SQL Support**
- CREATE TABLE
- INSERT, SELECT, UPDATE, DELETE
- WHERE clauses with conditions
- BETWEEN operator

✅ **Network Access**
- Client-server architecture
- Custom wire protocol
- Multi-client support

---

## Learning Resources

This project demonstrates:
- **Database internals** - How databases work under the hood
- **Storage engines** - Page management, indexing
- **Transaction processing** - ACID, WAL, recovery
- **Concurrency control** - Locking mechanisms
- **Network protocols** - Client-server communication

---

---

## Support

For issues or questions:
1. Review the phase demos for examples
2. Examine the source code comments
3. Reach out to me.

