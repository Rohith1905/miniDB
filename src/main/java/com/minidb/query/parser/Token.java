package com.minidb.query.parser;

/**
 * Token representation from lexical analysis
 */
public record Token(TokenType type, String value, int position) {
    
    public enum TokenType {
        // Keywords
        SELECT, INSERT, UPDATE, DELETE, CREATE, TABLE, INTO, FROM, WHERE, VALUES,
        SET, PRIMARY, KEY, INT, VARCHAR, LONG, AND, OR, BETWEEN,
        
        // Operators
        EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_EQUALS, GREATER_EQUALS,
        PLUS, MINUS, STAR, SLASH,
        
        // Delimiters
        LPAREN, RPAREN, COMMA, SEMICOLON,
        
        // Literals
        IDENTIFIER, NUMBER, STRING,
        
        // Special
        EOF, UNKNOWN
    }
    
    @Override
    public String toString() {
        return String.format("Token(%s, '%s', pos=%d)", type, value, position);
    }
}
