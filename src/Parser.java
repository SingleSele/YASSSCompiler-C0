import java.io.*;
import java.net.*;
class Parser {
    Tokenizer t;
    Token current;
    public Parser(Tokenizer t) {
        this.t = t;
        nextToken();
    }

    Token nextToken() {
        Token out = current;
        do {
            current = t.getToken();
        } while (current.is(Token.TYPE_COMMENT));
        return out;
    }

    Token peekToken() {
        return current;
    }

    boolean skip(String type) {
        if (peekToken().is(type)) {
            nextToken();
            return true;
        }
        return false;
    }


    boolean have(String ...types) {
        for (String type : types) {
            if (peekToken().is(type)) {
                return true;
            }
        }
        return false;
    }

    boolean have_statement() {
        return have_expr() || have(Token.TYPE_LET_KW, Token.TYPE_CONST_KW, Token.TYPE_IF_KW, Token.TYPE_WHILE_KW, Token.TYPE_BREAK_KW, Token.TYPE_CONTINUE_KW, Token.TYPE_RETURN_KW, Token.TYPE_L_BRACE, Token.TYPE_SEMICOLON);
    }

    boolean have_expr() {
        return have(Token.TYPE_IDENT, Token.TYPE_L_PAREN, Token.TYPE_LITI, Token.TYPE_LITC, Token.TYPE_LITF, Token.TYPE_LITS, Token.TYPE_MINUS);
    }

    void error(int n) {
        System.exit(1);
    }

    Token must(String type) {
        if (!have(type)) {
            System.out.println(type + " expected, got " + peekToken().type);
            error(1);
            return null;
        } else {
            return nextToken();
        }
    }
        

    AST parse_as_expr() {
        AST ret = new AST("as_expr");
        ret.add(parse_negate_expr());
        if (skip(Token.TYPE_AS_KW)) {
            ret.add(parse_ident_expr());
        }
        if (ret.size() == 1) {
            return ret.get(0);
        }
        return ret;
    }
    AST parse_assign_expr(Token ident) {
        AST ret = new AST("assign_expr");
        must(Token.TYPE_ASSIGN);
        ret.add(AST.token(ident));
        ret.add(parse_expr());
        return ret;
    }
    AST parse_block_stmt() {
        AST ret = new AST("block_stmt");
        must(Token.TYPE_L_BRACE);
        while (have_statement()) {
            ret.add(parse_stmt());
        }
        must(Token.TYPE_R_BRACE);
        return ret;
    }
    AST parse_call_expr(Token ident) {
        AST ret = new AST("call_expr");
        ret.add(AST.token(ident));
        must(Token.TYPE_L_PAREN);
        if (have_expr()) {
            ret.add(parse_expr());
            while (skip(Token.TYPE_COMMA)) {
                ret.add(parse_expr());
            }
        }
        must(Token.TYPE_R_PAREN);
        return ret;
    }
    AST parse_decl_stmt() {
        AST ret = new AST("decl_stmt");
        if (have(Token.TYPE_LET_KW, Token.TYPE_CONST_KW))   {
            AST t = AST.token(nextToken());
            ret.add(t);
            ret.add(AST.token(must(Token.TYPE_IDENT)));
            must(Token.TYPE_COLON);
            ret.add(parse_ty());
            if (t.token.type.equals(Token.TYPE_LET_KW)) {
                if (skip(Token.TYPE_ASSIGN)) {
                    ret.add(parse_expr());
                }
                must(Token.TYPE_SEMICOLON);
            } else {
                must(Token.TYPE_ASSIGN);
                ret.add(parse_expr());
                must(Token.TYPE_SEMICOLON);
            }
        }
        return ret;
    }

    AST parse_empty_stmt() {
        AST ret = new AST("empty_stmt");
        must(Token.TYPE_SEMICOLON);
        return ret;
    }
    AST parse_expr() {
        return parse_operator_expr();
    }
    AST parse_expr_stmt() {
        AST ret = new AST("expr_stmt");
        ret.add(parse_expr());
        must(Token.TYPE_SEMICOLON);
        return ret;
    }
    AST parse_function() {
        AST ret = new AST("function");
        must(Token.TYPE_FN_KW);
        ret.add(AST.token(must(Token.TYPE_IDENT)));
        must(Token.TYPE_L_PAREN);
        ret.add(parse_function_param_list());
        must(Token.TYPE_R_PAREN);
        must(Token.TYPE_ARROW);
        ret.add(parse_ty());
        ret.add(parse_block_stmt());
        return ret;
    }
    AST parse_function_param() {
        AST ret = new AST("function_param");
        if (skip(Token.TYPE_CONST_KW)) {
            ret.add(AST.token(new Token(Token.TYPE_CONST_KW, "const")));
        } else {
            ret.add(AST.token(new Token(Token.TYPE_LET_KW, "let")));
        }
        ret.add(AST.token(must(Token.TYPE_IDENT)));
        must(Token.TYPE_COLON);
        ret.add(parse_ty());
        return ret;
    }
    AST parse_function_param_list() {
        AST ret = new AST("function_param_list");
        if (!have(Token.TYPE_R_PAREN)) {
            ret.add(parse_function_param());
            while (skip(Token.TYPE_COMMA)) {
                ret.add(parse_function_param());
            }
        }
        return ret;
    }
    AST parse_group_expr() {
        must(Token.TYPE_L_PAREN);
        AST expr = parse_expr();
        must(Token.TYPE_R_PAREN);
        return expr;
    }
    AST parse_ident_expr() {
        Token ident = must(Token.TYPE_IDENT);
        if (have(Token.TYPE_L_PAREN)) {
            return parse_call_expr(ident);
        } else if (have(Token.TYPE_ASSIGN)) {
            return parse_assign_expr(ident);
        } else {
            AST ret = new AST("ident_expr");
            ret.add(AST.token(ident));
            return ret;
        }
    }
    AST parse_if_stmt() {
        AST ret = new AST("if_stmt");
        must(Token.TYPE_IF_KW);
        ret.add(parse_expr());
        ret.add(parse_block_stmt());
        while (skip(Token.TYPE_ELSE_KW)) {
            if (skip(Token.TYPE_IF_KW)) {
                 ret.add(parse_expr());
                 ret.add(parse_block_stmt());    
            } else {
                 ret.add(parse_block_stmt());
            }
        }
        return ret;
    }
    AST parse_break_stmt() {
        AST ret = new AST("break_stmt");
        must(Token.TYPE_BREAK_KW);
        must(Token.TYPE_SEMICOLON);
        return ret;
    }
    AST parse_continue_stmt() {
        AST ret = new AST("continue_stmt");
        must(Token.TYPE_CONTINUE_KW);
        must(Token.TYPE_SEMICOLON);
        return ret;
    }
    AST parse_literal_expr() {
        AST ret = new AST("literal_expr");
        Token t = nextToken();
        if (t.is(Token.TYPE_LITC)) {
            ret.add(AST.token(new Token(Token.TYPE_LITI, "" + (int)t.lexeme.charAt(0))));
        } else {
            ret.add(AST.token(t));
        }
        return ret;
    }
    AST parse_negate_expr() {
        if (skip(Token.TYPE_MINUS)) {
            AST ret = new AST("negate_expr");
            ret.add(parse_expr());
            return ret;
        } else if (have(Token.TYPE_IDENT)) {
            return parse_ident_expr();
        } else if (have(Token.TYPE_L_PAREN)) {
            return parse_group_expr();
        } else if (have(Token.TYPE_LITI, Token.TYPE_LITC, Token.TYPE_LITF, Token.TYPE_LITS)) {
            return parse_literal_expr();
        } else {
            System.out.println("Unexpected " + peekToken());
            error(2);
            return null;
        }
    }
    AST parse_operator_expr() {
        return parse_compare_expr();
    }
    AST parse_compare_expr() {
        AST ret = new AST("operator_expr");
        ret.add(parse_add_expr());
        while (have(Token.TYPE_LT, Token.TYPE_LE, Token.TYPE_EQ, Token.TYPE_NEQ, Token.TYPE_GE, Token.TYPE_GT)) {
            ret.add(AST.token(nextToken()));
            ret.add(parse_add_expr());
        }
        if (ret.size() == 1) {
            return ret.get(0);
        }
        return ret;
    }
    AST parse_add_expr() {
        AST ret = new AST("operator_expr");
        ret.add(parse_mul_expr());
        while (have(Token.TYPE_PLUS, Token.TYPE_MINUS)) {
            ret.add(AST.token(nextToken()));
            ret.add(parse_mul_expr());
        }
        if (ret.size() == 1) {
            return ret.get(0);
        }
        return ret;
    }
    AST parse_mul_expr() {
        AST ret = new AST("operator_expr");
        ret.add(parse_as_expr());
        while (have(Token.TYPE_MUL, Token.TYPE_DIV)) {
            ret.add(AST.token(nextToken()));
            ret.add(parse_as_expr());
        }
        if (ret.size() == 1) {
            return ret.get(0);
        }
        return ret;
    }
    AST parse_program() {
        AST ret = new AST("program");
        while (have(Token.TYPE_LET_KW, Token.TYPE_CONST_KW, Token.TYPE_FN_KW)) {
            ret.add(parse_item());
        }
        return ret;
    }
    AST parse_item() {
        if (have(Token.TYPE_LET_KW, Token.TYPE_CONST_KW)) {
            return parse_decl_stmt();
        } else if (have(Token.TYPE_FN_KW)) {
            return parse_function();
        } else {
            error(2);
            return null;
        }
    }

    AST parse_return_stmt() {
        AST ret = new AST("return_stmt");
        must(Token.TYPE_RETURN_KW);
        if (have_expr()) {
            ret.add(parse_expr());
        }
        must(Token.TYPE_SEMICOLON);
        return ret;
    }
    AST parse_stmt() {
        AST ret = new AST("stmt");
        if (have_expr()) {
            return parse_expr_stmt();
        } else if (have(Token.TYPE_IF_KW)) {
            return parse_if_stmt();
        } else if (have(Token.TYPE_WHILE_KW)){
            return parse_while_stmt();
        } else if (have(Token.TYPE_BREAK_KW)) {
            return parse_break_stmt();
        } else if (have(Token.TYPE_CONTINUE_KW)) {
            return parse_continue_stmt();
        } else if (have(Token.TYPE_RETURN_KW)) {
            return parse_return_stmt();
        } else if (have(Token.TYPE_LET_KW, Token.TYPE_CONST_KW)) {
            return parse_decl_stmt();
        } else {
            System.out.println("Unexpected " + peekToken());
            error(4);
        }
        return ret;
    }
    AST parse_ty() {
        AST ret = new AST("ty");
        ret.token = must(Token.TYPE_IDENT);
        return ret;
    }
    AST parse_while_stmt() {
        AST ret = new AST("while_stmt");
        must(Token.TYPE_WHILE_KW);
        ret.add(parse_expr());
        ret.add(parse_block_stmt());
        return ret;
    } 

    
}
