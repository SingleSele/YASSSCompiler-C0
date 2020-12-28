import java.util.*;
public class AST {
    public String head;
    public String type;
    // Branch
    public List <AST> subs = new ArrayList<>();
    // Leaf
    public Token token;

    public AST() {}
    public AST(String head) { 
        this.head = head;
    }
    public static AST token(Token t) {
        AST ast = new AST(TYPE_TOKEN);
        ast.token = t;
        return ast;
    }
    public AST add(AST ast) {
        subs.add(ast);
        return this;
    }
    public AST addAll(AST ...ast) {
        subs.addAll(Arrays.asList(ast));
        return this;
    }

    public AST get(int index) {
        return subs.get(index);
    }
    public int size() {
        return subs.size();
    }

    public boolean is(String type) {
        return this.head.equals(type);
    }
    public String toString(int n, boolean next) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < n; ++i) sb.append(' ');
        sb.append("(").append(type == null ? "unknown" : type).append(")");
        if (next) sb.append(","); else sb.append(" ");
        sb.append(head);
        sb.append("[");
        if (subs.size() > 0) {
          sb.append("\n");
          boolean first = true;
          for (AST a : subs) {
            sb.append(a.toString(n + 4, !first));
            if (first) first = false;
         }
         for (int i = 0; i < n; ++i) sb.append(' ');
         sb.append("]\n");
        } else {
            if (token != null) {
                sb.append(token);
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(0, false);
    }

    static final String TYPE_OPERATOR_EXPR = "operator_expr";
    static final String TYPE_NEGATE_EXPR = "negate_expr";
    static final String TYPE_ASSIGN_EXPR = "assign_expr";
    static final String TYPE_AS_EXPR = "as_expr";
    static final String TYPE_CALL_EXPR = "call_expr";
    static final String TYPE_LITERAL_EXPR = "literal_expr";
    static final String TYPE_IDENT_EXPR = "ident_expr";
    static final String TYPE_GROUP_EXPR = "group_expr";
    static final String TYPE_EXPR_STMT = "expr_stmt";
    static final String TYPE_DECL_STMT = "decl_stmt";
    static final String TYPE_IF_STMT = "if_stmt";
    static final String TYPE_WHILE_STMT = "while_stmt";
    static final String TYPE_RETURN_STMT = "return_stmt";
    static final String TYPE_BLOCK_STMT = "block_stmt";
    static final String TYPE_EMPTY_STMT = "empty_stmt";
    static final String TYPE_BREAK_STMT = "break_stmt";
    static final String TYPE_CONTINUE_STMT = "continue_stmt";
    static final String TYPE_EXPR = "expr";
    static final String TYPE_STMT = "stmt";
    static final String TYPE_TY = "ty";
    static final String TYPE_CONST_TY = "const_ty";
    static final String TYPE_PROGRAM = "program";
    static final String TYPE_FUNCTION = "function";
    static final String TYPE_FUNCTION_PARAM = "function_param";
    static final String TYPE_FUNCTION_PARAM_LIST = "function_param_list";
    static final String TYPE_TOKEN = "token";
    
}

