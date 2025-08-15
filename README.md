# MiniDB - Quick Start

## 🚀 Quick Start (30 seconds)

### 1. Build
```bash
cd C:\Users\rohit\code\java\minidb
mvn clean compile
```

### 2. Start Server
```bash
java -cp target/classes com.minidb.Phase4Demo
```

### 3. Connect Client (new terminal)
```bash
java -cp target/classes com.minidb.network.SimpleClient localhost 5432
```

### 4. Run SQL
```sql
BEGIN
CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100))
INSERT INTO users VALUES (1, 'Alice')
SELECT * FROM users
COMMIT
EXIT
```

---

## 📖 Full Documentation

See [USER_GUIDE.md](USER_GUIDE.md) for complete documentation.

---

## 🎯 What You Can Do

- ✅ Create tables with primary keys
- ✅ Insert, select, update, delete data
- ✅ Use WHERE clauses and BETWEEN
- ✅ Run ACID transactions
- ✅ Connect multiple clients
- ✅ Automatic crash recovery

---

## 🔧 Troubleshooting

**Port in use?**
```bash
# Windows: Kill process on port 5432
netstat -ano | findstr :5432
taskkill /PID <PID> /F
```

**Build failed?**
```bash
mvn clean compile
```

---

## 📚 Learn More

- **Phase 1 Demo**: Storage layer → `java -cp target/classes com.minidb.Phase1Demo`
- **Phase 2 Demo**: Indexing → `java -cp target/classes com.minidb.Phase2Demo`
- **Phase 3 Demo**: Transactions → `java -cp target/classes com.minidb.Phase3Demo`
- **Phase 4 Demo**: Network server → `java -cp target/classes com.minidb.Phase4Demo`
"# miniDB" 
