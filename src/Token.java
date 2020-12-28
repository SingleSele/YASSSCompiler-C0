class Token {
    public String type;
    public String lexeme;
    public Token() {}
    public Token(String type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    @Override
    public String toString() {
        return String.format("%s<%s>", type, lexeme);
    }

    public boolean is(String type) {
        return this.type.equals(type);
    }

    public static final String TYPE_FN_KW = "FN_KW";
    public static final String TYPE_LET_KW = "LET_KW";
    public static final String TYPE_CONST_KW = "CONST_KW";
    public static final String TYPE_AS_KW = "AS_KW";
    public static final String TYPE_WHILE_KW = "WHILE_KW";
    public static final String TYPE_IF_KW = "IF_KW";
    public static final String TYPE_ELSE_KW = "ELSE_KW";
    public static final String TYPE_RETURN_KW = "RETURN_KW";
    public static final String TYPE_BREAK_KW = "BREAK_KW";
    public static final String TYPE_CONTINUE_KW = "CONTINUE_KW";
    public static final String TYPE_LITS = "LITS";
    public static final String TYPE_LITI = "LITI";
    public static final String TYPE_LITF = "LITF";
    public static final String TYPE_LITC = "LITC";
    public static final String TYPE_PLUS = "PLUS";
    public static final String TYPE_MINUS = "MINUS";
    public static final String TYPE_MUL = "MUL";
    public static final String TYPE_DIV = "DIV";
    public static final String TYPE_ASSIGN = "ASSIGN";
    public static final String TYPE_EQ = "EQ";
    public static final String TYPE_NEQ = "NEQ";
    public static final String TYPE_LT = "LT";
    public static final String TYPE_GT = "GT";
    public static final String TYPE_LE = "LE";
    public static final String TYPE_GE = "GE";
    public static final String TYPE_L_PAREN = "L_PAREN";
    public static final String TYPE_R_PAREN = "R_PAREN";
    public static final String TYPE_L_BRACE = "L_BRACE";
    public static final String TYPE_R_BRACE = "R_BRACE";
    public static final String TYPE_ARROW = "ARROW";
    public static final String TYPE_COMMA = "COMMA";
    public static final String TYPE_COLON = "COLON";
    public static final String TYPE_SEMICOLON = "SEMICOLON";
    public static final String TYPE_EOF = "EOF";
    public static final String TYPE_IDENT = "IDENT";
    public static final String TYPE_COMMENT = "COMMENT";
}


