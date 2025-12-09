package com.minidb.query.parser;

import java.util.*;

/**
 * Lexical analyzer - converts SQL text into tokens
 */
public class Lexer {
    private final String input;
    private int position;
    private char currentChar;
    
    private static final Map<String, Token.TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("SELECT", Token.TokenType.SELECT),
        Map.entry("INSERT", Token.TokenType.INSERT),
        Map.entry("UPDATE", Token.TokenType.UPDATE),
        Map.entry("DELETE", Token.TokenType.DELETE),
        Map.entry("CREATE", Token.TokenType.CREATE),
        Map.entry("TABLE", Token.TokenType.TABLE),
        Map.entry("INTO", Token.TokenType.INTO),
        Map.entry("FROM", Token.TokenType.FROM),
        Map.entry("WHERE", Token.TokenType.WHERE),
        Map.entry("VALUES", Token.TokenType.VALUES),
        Map.entry("SET", Token.TokenType.SET),
        Map.entry("PRIMARY", Token.TokenType.PRIMARY),
        Map.entry("KEY", Token.TokenType.KEY),
        Map.entry("INT", Token.TokenType.INT),
        Map.entry("VARCHAR", Token.TokenType.VARCHAR),
        Map.entry("LONG", Token.TokenType.LONG),
        Map.entry("AND", Token.TokenType.AND),
        Map.entry("OR", Token.TokenType.OR),
        Map.entry("BETWEEN", Token.TokenType.BETWEEN)
    );
    
    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.currentChar = input.isEmpty() ? '\0' : input.charAt(0);
    }
    
    /**
     * Tokenize entire input
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        
        while ((token = nextToken()).type() != Token.TokenType.EOF) {
            tokens.add(token);
        }
        tokens.add(token); // Add EOF
        
        return tokens;
    }
    
    /**
     * Get next token
     */
    public Token nextToken() {
        while (currentChar != '\0') {
            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
                continue;
            }
            
            // Skip comments
            if (currentChar == '-' && peek() == '-') {
                skipComment();
                continue;
            }
            
            // Numbers
            if (Character.isDigit(currentChar)) {
                return number();
            }
            
            // Identifiers and keywords
            if (Character.isLetter(currentChar) || currentChar == '_') {
                return identifierOrKeyword();
            }
            
            // Strings
            if (currentChar == '\'' || currentChar == '"') {
                return string();
            }
            
            // Operators and delimiters
            int pos = position;
            switch (currentChar) {
                case '=' -> { advance(); return new Token(Token.TokenType.EQUALS, "=", pos); }
                case '<' -> {
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(Token.TokenType.LESS_EQUALS, "<=", pos);
                    } else if (currentChar == '>') {
                        advance();
                        return new Token(Token.TokenType.NOT_EQUALS, "<>", pos);
                    }
                    return new Token(Token.TokenType.LESS_THAN, "<", pos);
                }
                case '>' -> {
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(Token.TokenType.GREATER_EQUALS, ">=", pos);
                    }
                    return new Token(Token.TokenType.GREATER_THAN, ">", pos);
                }
                case '!' -> {
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(Token.TokenType.NOT_EQUALS, "!=", pos);
                    }
                }
                case '+' -> { advance(); return new Token(Token.TokenType.PLUS, "+", pos); }
                case '-' -> { advance(); return new Token(Token.TokenType.MINUS, "-", pos); }
                case '*' -> { advance(); return new Token(Token.TokenType.STAR, "*", pos); }
                case '/' -> { advance(); return new Token(Token.TokenType.SLASH, "/", pos); }
                case '(' -> { advance(); return new Token(Token.TokenType.LPAREN, "(", pos); }
                case ')' -> { advance(); return new Token(Token.TokenType.RPAREN, ")", pos); }
                case ',' -> { advance(); return new Token(Token.TokenType.COMMA, ",", pos); }
                case ';' -> { advance(); return new Token(Token.TokenType.SEMICOLON, ";", pos); }
            }
            
            // Unknown character
            char unknown = currentChar;
            advance();
            return new Token(Token.TokenType.UNKNOWN, String.valueOf(unknown), pos);
        }
        
        return new Token(Token.TokenType.EOF, "", position);
    }
    
    private Token number() {
        int startPos = position;
        StringBuilder sb = new StringBuilder();
        
        while (Character.isDigit(currentChar)) {
            sb.append(currentChar);
            advance();
        }
        
        return new Token(Token.TokenType.NUMBER, sb.toString(), startPos);
    }
    
    private Token identifierOrKeyword() {
        int startPos = position;
        StringBuilder sb = new StringBuilder();
        
        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            sb.append(currentChar);
            advance();
        }
        
        String value = sb.toString();
        String upper = value.toUpperCase();
        Token.TokenType type = KEYWORDS.getOrDefault(upper, Token.TokenType.IDENTIFIER);
        
        return new Token(type, value, startPos);
    }
    
    private Token string() {
        int startPos = position;
        char quote = currentChar;
        advance(); // Skip opening quote
        
        StringBuilder sb = new StringBuilder();
        while (currentChar != '\0' && currentChar != quote) {
            sb.append(currentChar);
            advance();
        }
        
        if (currentChar == quote) {
            advance(); // Skip closing quote
        }
        
        return new Token(Token.TokenType.STRING, sb.toString(), startPos);
    }
    
    private void skipWhitespace() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) {
            advance();
        }
    }
    
    private void skipComment() {
        while (currentChar != '\0' && currentChar != '\n') {
            advance();
        }
    }
    
    private void advance() {
        position++;
        currentChar = position < input.length() ? input.charAt(position) : '\0';
    }
    
    private char peek() {
        int peekPos = position + 1;
        return peekPos < input.length() ? input.charAt(peekPos) : '\0';
    }
}