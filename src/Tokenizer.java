import java.util.*;
import java.util.function.*;
import java.io.*;

class Tokenizer implements Iterable<Token>, Iterator<Token> {
    InputStream is;
    char currentChar;
    static final char EOFCHAR = (char)-1;
    public Tokenizer() {}
    public Tokenizer(InputStream is) {
        this.is = is;
        nextch();
    }

    public Iterator<Token> iterator() {
        return this;
    }

    public boolean hasNext() {
        return currentChar == EOFCHAR;
    }

    public Token next() {
        return getToken();
    }

    char nextch() {
        if (currentChar == EOFCHAR) {
            return EOFCHAR;
        }
        char out = currentChar;
        try {
            int c = is.read();
            currentChar = c != -1 ? (char)c : EOFCHAR;
            System.out.print(currentChar);            
            return out;
        } catch (EOFException ex) {
            currentChar = EOFCHAR;
            return out;
        } catch (IOException ex) {
            error(1);
            return 0;
        }
    }

    char peekch() {
        return currentChar;
    }

    String scan(Predicate <Character> pred) {
        String out = "";
        while (pred.test(peekch())) {
            out += peekch();
            nextch();
        }
        return out;
    }

    static final String SYMBOL = "+-*/=!><*(){},:;";

    Token ident(String in) {
        switch (in) {
            case "fn": return new Token(Token.TYPE_FN_KW, "fn");
            case "let": return new Token(Token.TYPE_LET_KW, "let");
            case "const": return new Token(Token.TYPE_CONST_KW, "const");
            case "as": return new Token(Token.TYPE_AS_KW, "as");
            case "while": return new Token(Token.TYPE_WHILE_KW, "while");
            case "if": return new Token(Token.TYPE_IF_KW, "if");
            case "else": return new Token(Token.TYPE_ELSE_KW, "else");
            case "return": return new Token(Token.TYPE_RETURN_KW, "return");
            case "break": return new Token(Token.TYPE_BREAK_KW, "break");
            case "continue": return new Token(Token.TYPE_CONTINUE_KW, "continue");
        }
        return new Token(Token.TYPE_IDENT, in);
    }   

    void error(int c) {
        System.exit(1);
    }

    Token symbol() {
        String in = "" + nextch();
        switch (in.charAt(0)) {
            case '+': return new Token(Token.TYPE_PLUS, in);
            case '*': return new Token(Token.TYPE_MUL, in);
            case '/':
                 if (peekch() != '/') {
                     return new Token(Token.TYPE_DIV, in);
                 } else {
                     nextch();
                     in = "";
                     while (peekch() != '\n') {
                         in += nextch();
                     }
                     nextch();
                     return new Token(Token.TYPE_COMMENT, in);
                 }
            case '=': 
                 if (peekch() == '=') {
                      nextch();
                      return new Token(Token.TYPE_EQ, in + "=");
                 } else {
                      return new Token(Token.TYPE_ASSIGN, in);
                 }
            case '!': return new Token(Token.TYPE_NEQ, in);
            case '<':  
                 if (peekch() != '=') {
                      return new Token(Token.TYPE_LT, in);
                 } else {
                      nextch();
                      return new Token(Token.TYPE_LE, in + '=');
                 }
            case '>':  
                 if (peekch() != '=') {
                      return new Token(Token.TYPE_GT, in);
                 } else {
                      nextch();
                      return new Token(Token.TYPE_GE, in + '=');
                 }

            case '(': return new Token(Token.TYPE_L_PAREN, in);
            case ')': return new Token(Token.TYPE_R_PAREN, in);
            case '{': return new Token(Token.TYPE_L_BRACE, in);
            case '}': return new Token(Token.TYPE_R_BRACE, in);
            case '-': 
                 if (peekch() != '>') {
                     return new Token(Token.TYPE_MINUS, in);
                 } else {
                     nextch();
                     return new Token(Token.TYPE_ARROW, in + ">");
                 }
            case ',': return new Token(Token.TYPE_COMMA, in);
            case ':': return new Token(Token.TYPE_COLON, in);
            case ';': return new Token(Token.TYPE_SEMICOLON, in);
        }
        error(3);
        return null;
    }

    Token num() {
        String part1 = scan(Character::isDigit);
        if (peekch() != '.') {
            return new Token(Token.TYPE_LITI, part1);
        }
        nextch();
        String part2 = scan(Character::isDigit);
        if (peekch() == 'E' || peekch() == 'e') {
            part2 += nextch();
            if (peekch() == '+' || peekch() == '-') {
                part2 += nextch();
            }
            part2 += scan(Character::isDigit);
        }
        return new Token(Token.TYPE_LITF, part1 + "." + part2);
    }

    boolean isEscaped(char s) {
        return "\\\"'nrt".indexOf(s) > -1;
    }

    char escape(char s) {
        switch (s) {
            case '\\': return '\\';
            case '"': return '"';
            case '\'': return '\'';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
        }
        error(7);
        return 0;
    }


    Token charLiteral() {
        String ch = "";
        nextch();
        while (peekch() != '\'') {
            if (peekch() == '\\') {
                nextch();
                if (isEscaped(peekch())) {
                    ch += escape(nextch());
                } else {
                    error(5);
                }
            } else if (peekch() == EOFCHAR) {
                error(5);
            } else {
                ch += nextch();
            }
        }
        nextch();
        return new Token(Token.TYPE_LITC, ch);
    }

    Token strLiteral() {
        String ch = "";
        nextch();
        while (peekch() != '"') {
            if (peekch() == '\\') {
                nextch();
                if (isEscaped(peekch())) {
                    ch += escape(nextch());
                } else {
                    error(4);
                }
            } else if (peekch() == EOFCHAR) {
                error(4);
            } else {
                ch += nextch();
            }
        }
        nextch();
        return new Token(Token.TYPE_LITS, ch);
    }

    Token getToken() {
        scan(x -> x == ' ' || x == '\t' || x == '\n' || x == '\r');
        if (peekch() == EOFCHAR) {
            return new Token(Token.TYPE_EOF, "<EOF>");
        }

        if (Character.isLetter(peekch()) || peekch() == '_') {
            return ident(scan(x -> x == '_' || Character.isLetterOrDigit(peekch())));
        }
        if (SYMBOL.indexOf(peekch()) > -1) {
            return symbol();
        }
        
        if (Character.isDigit(peekch())) {
            return num();
        }

        if (peekch() == '\'') {
            return charLiteral();
        }

        if (peekch() == '\"') {
            return strLiteral();
        }
        System.out.println(peekch());
        error(10);
        return null;
    }
}


