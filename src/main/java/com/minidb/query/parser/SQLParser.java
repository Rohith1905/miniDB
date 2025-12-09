package com.minidb.query.parser;

import java.util.*;

/**
 * SQL Parser - Converts tokens into statement AST
 */
public class SQLParser {
    private List<Token> tokens;
    private int position;
    private Token currentToken;
    
    public SQLParser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.currentToken = tokens.get(0);
    }
    
    /**
     * Parse SQL statement
     */
    public Statement parse() {
        return switch (currentToken.type()) {
            case CREATE -> parseCreateTable();
            case INSERT -> parseInsert();
            case SELECT -> parseSelect();
            case UPDATE -> parseUpdate();
            case DELETE -> parseDelete();
            default -> throw new ParseException("Unexpected token: " + currentToken);
        };
    }
    
    // CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR, email VARCHAR)
    private Statement parseCreateTable() {
        consume(Token.TokenType.CREATE);
        consume(Token.TokenType.TABLE);
        
        String tableName = consume(Token.TokenType.IDENTIFIER).value();
        consume(Token.TokenType.LPAREN);
        
        List<CreateTableStatement.ColumnDefinition> columns = new ArrayList<>();
        String primaryKey = null;
        
        do {
            if (currentToken.type() == Token.TokenType.COMMA) {
                consume(Token.TokenType.COMMA);
            }
            
            String colName = consume(Token.TokenType.IDENTIFIER).value();
            Token typeToken = advance();
            
            CreateTableStatement.ColumnDefinition.ColumnType colType = switch (typeToken.type()) {
                case INT -> CreateTableStatement.ColumnDefinition.ColumnType.INT;
                case LONG -> CreateTableStatement.ColumnDefinition.ColumnType.LONG;
                case VARCHAR -> CreateTableStatement.ColumnDefinition.ColumnType.VARCHAR;
                default -> throw new ParseException("Expected type, got: " + typeToken);
            };
            
            int length = 0;
            if (colType == CreateTableStatement.ColumnDefinition.ColumnType.VARCHAR) {
                if (currentToken.type() == Token.TokenType.LPAREN) {
                    consume(Token.TokenType.LPAREN);
                    length = Integer.parseInt(consume(Token.TokenType.NUMBER).value());
                    consume(Token.TokenType.RPAREN);
                } else {
                    length = 255; // Default
                }
            }
            
            columns.add(new CreateTableStatement.ColumnDefinition(colName, colType, length));
            
            // Check for PRIMARY KEY
            if (currentToken.type() == Token.TokenType.PRIMARY) {
                consume(Token.TokenType.PRIMARY);
                consume(Token.TokenType.KEY);
                primaryKey = colName;
            }
            
        } while (currentToken.type() == Token.TokenType.COMMA);
        
        consume(Token.TokenType.RPAREN);
        
        return new CreateTableStatement(tableName, columns, primaryKey);
    }
    
    // INSERT INTO users VALUES (1, 'Alice', 'alice@example.com')
    private Statement parseInsert() {
        consume(Token.TokenType.INSERT);
        consume(Token.TokenType.INTO);
        
        String tableName = consume(Token.TokenType.IDENTIFIER).value();
        
        // Optional column list (not implemented for simplicity)
        List<String> columns = List.of();
        
        consume(Token.TokenType.VALUES);
        consume(Token.TokenType.LPAREN);
        
        List<Object> values = new ArrayList<>();
        do {
            if (currentToken.type() == Token.TokenType.COMMA) {
                consume(Token.TokenType.COMMA);
            }
            
            if (currentToken.type() == Token.TokenType.NUMBER) {
                values.add(Integer.parseInt(currentToken.value()));
                advance();
            } else if (currentToken.type() == Token.TokenType.STRING) {
                values.add(currentToken.value());
                advance();
            } else {
                throw new ParseException("Expected value, got: " + currentToken);
            }
            
        } while (currentToken.type() == Token.TokenType.COMMA);
        
        consume(Token.TokenType.RPAREN);
        
        return new InsertStatement(tableName, columns, values);
    }
    
    // SELECT * FROM users WHERE id = 1
    // SELECT name, email FROM users WHERE id BETWEEN 1 AND 10
    private Statement parseSelect() {
        consume(Token.TokenType.SELECT);
        
        List<String> columns = new ArrayList<>();
        if (currentToken.type() == Token.TokenType.STAR) {
            columns.add("*");
            advance();
        } else {
            do {
                if (currentToken.type() == Token.TokenType.COMMA) {
                    consume(Token.TokenType.COMMA);
                }
                columns.add(consume(Token.TokenType.IDENTIFIER).value());
            } while (currentToken.type() == Token.TokenType.COMMA);
        }
        
        consume(Token.TokenType.FROM);
        String tableName = consume(Token.TokenType.IDENTIFIER).value();
        
        Condition whereClause = null;
        if (currentToken.type() == Token.TokenType.WHERE) {
            consume(Token.TokenType.WHERE);
            whereClause = parseCondition();
        }
        
        return new SelectStatement(columns, tableName, whereClause);
    }
    
    // UPDATE users SET name = 'Bob' WHERE id = 1
    private Statement parseUpdate() {
        consume(Token.TokenType.UPDATE);
        String tableName = consume(Token.TokenType.IDENTIFIER).value();
        
        consume(Token.TokenType.SET);
        
        Map<String, Object> assignments = new HashMap<>();
        do {
            if (currentToken.type() == Token.TokenType.COMMA) {
                consume(Token.TokenType.COMMA);
            }
            
            String column = consume(Token.TokenType.IDENTIFIER).value();
            consume(Token.TokenType.EQUALS);
            
            Object value;
            if (currentToken.type() == Token.TokenType.NUMBER) {
                value = Integer.parseInt(currentToken.value());
                advance();
            } else if (currentToken.type() == Token.TokenType.STRING) {
                value = currentToken.value();
                advance();
            } else {
                throw new ParseException("Expected value");
            }
            
            assignments.put(column, value);
            
        } while (currentToken.type() == Token.TokenType.COMMA);
        
        Condition whereClause = null;
        if (currentToken.type() == Token.TokenType.WHERE) {
            consume(Token.TokenType.WHERE);
            whereClause = parseCondition();
        }
        
        return new UpdateStatement(tableName, assignments, whereClause);
    }
    
    // DELETE FROM users WHERE id = 1
    private Statement parseDelete() {
        consume(Token.TokenType.DELETE);
        consume(Token.TokenType.FROM);
        
        String tableName = consume(Token.TokenType.IDENTIFIER).value();
        
        Condition whereClause = null;
        if (currentToken.type() == Token.TokenType.WHERE) {
            consume(Token.TokenType.WHERE);
            whereClause = parseCondition();
        }
        
        return new DeleteStatement(tableName, whereClause);
    }
    
    private Condition parseCondition() {
        String column = consume(Token.TokenType.IDENTIFIER).value();
        
        // BETWEEN condition
        if (currentToken.type() == Token.TokenType.BETWEEN) {
            consume(Token.TokenType.BETWEEN);
            Object start = parseValue();
            consume(Token.TokenType.AND);
            Object end = parseValue();
            return new BetweenCondition(column, start, end);
        }
        
        // Simple condition
        SimpleCondition.Operator op = switch (currentToken.type()) {
            case EQUALS -> SimpleCondition.Operator.EQ;
            case NOT_EQUALS -> SimpleCondition.Operator.NEQ;
            case LESS_THAN -> SimpleCondition.Operator.LT;
            case GREATER_THAN -> SimpleCondition.Operator.GT;
            case LESS_EQUALS -> SimpleCondition.Operator.LTE;
            case GREATER_EQUALS -> SimpleCondition.Operator.GTE;
            default -> throw new ParseException("Expected operator");
        };
        advance();
        
        Object value = parseValue();
        
        return new SimpleCondition(column, op, value);
    }
    
    private Object parseValue() {
        if (currentToken.type() == Token.TokenType.NUMBER) {
            int value = Integer.parseInt(currentToken.value());
            advance();
            return value;
        } else if (currentToken.type() == Token.TokenType.STRING) {
            String value = currentToken.value();
            advance();
            return value;
        } else {
            throw new ParseException("Expected value");
        }
    }
    
    private Token consume(Token.TokenType expected) {
        if (currentToken.type() != expected) {
            throw new ParseException("Expected " + expected + ", got " + currentToken.type());
        }
        Token token = currentToken;
        advance();
        return token;
    }
    
    private Token advance() {
        Token token = currentToken;
        position++;
        if (position < tokens.size()) {
            currentToken = tokens.get(position);
        }
        return token;
    }
    
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }
}
