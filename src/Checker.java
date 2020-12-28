import java.util.*;
import java.io.*;
class Checker {
    void error(int c, AST n) {
        System.out.print(n);
//        throw new RuntimeException("" + (300 + c));
        System.exit(1);
    }
    
    void addVariable(Env env, AST n) {
        Env.Variable v = null;
        if (n.is("decl_stmt")) {
           String vname = n.get(1).token.lexeme;
           String vtype = n.get(2).token.lexeme;
           boolean visConst = n.get(0).token.is(Token.TYPE_CONST_KW);
           v = new Env.Variable() {{
               name = vname;
               type = vtype;
               isConst = visConst;
               node = n;
           }};
        } else if (n.is("function")) {
           String vname = n.get(0).token.lexeme;
           String vtype = n.get(2).token.lexeme;
           if (vname.equals("main")) {
               mainFound = true;
           }
           boolean visConst = false;
           v = new Env.Variable() {{
               name = vname;
               type = vtype;
               isConst = visConst;
               node = n;
               data = "";
           }};
           currentFunction = v;
        } else if (n.is("function_param")) {
           v = new Env.Variable() {{
               name = n.get(1).token.lexeme;
               type = n.get(2).token.lexeme;
               isConst = n.get(0).is(Token.TYPE_CONST_KW);
               node = n;
           }};
           currentFunction.data = currentFunction.getData(String.class) +
            ";" + v.type;
        }
        if (v == null) {
            error(1, n);
        }
        if (!env.addVariable(v)) {
            System.out.println(v);
            error(2, n);
        }
    }
    Env.Variable currentFunction;

    String tryCall(String name, Env env, List<String> calltypes, AST n) {
        String s = calltypes.stream().reduce("", (a, b) -> a + ";" + b);
        switch (name) {
            case "getint": {
                type_equal(s, "", n);
                return "int";
            }
            case "getdouble": {
                type_equal(s, "", n);
                return "double";
            }
            case "getchar": {
                type_equal(s, "", n);
                return "int";
            }
            case "putint": {
                type_equal(s, ";int", n);
                return "void";
            }
            case "putdouble": {
                type_equal(s, ";double", n);
                return "void";
            }
            case "putchar": {
                type_equal(s, ";int", n);
                return "void";
            }
            case "putstr": {
                type_equal(s, ";int", n);
                return "void";
            }
            case "putln": {
                type_equal(s, "", n);
                return "void";
            }
        }
        Env.Variable fn = env.findVariable(name);
        if (fn == null || !fn.node.is("function")) {
            error(12, n);
        }
        if (!fn.data.toString().equals(s)) {
            error(13, n);
        }
        return fn.type;
            
    }


    void type_equal(String a, String b, AST n) {
        if (!a.equals(b)) {
            System.out.println(a + " <=> " + b);
            error(3, n);
        }
    }

    int inWhile = 0;
    String currentReturn = "";

    boolean validVariableType(String a) {
        return a.equals("int") || a.equals("double");
    }
    boolean mainFound = false;
    String check(AST a, Env env) {
	    switch (a.head) {
	        case "program":  {
	            for (AST sub : a.subs) {
                    check(sub, env);
	            }
	            if (!mainFound) {
                    error(12, a);
	            }
	            return "void";
	        }
	        case "decl_stmt": {
	            if (!validVariableType(a.get(2).token.lexeme)) {
	                error(1, a);
	            }
	            if (a.get(0).token.is(Token.TYPE_CONST_KW)) {
                    if (a.size() == 3) {
                        error(1, a);
                    }
	            }
	            if (a.size() == 4) {
                    String r = check(a.get(3), env);
                    type_equal(a.get(2).token.lexeme, r, a);
	            }
                addVariable(env, a);
                a.type = "return void";
                return "return void";
	        }
            case "function": {
                addVariable(env, a);
                Env newenv = new Env() {{
                   next = env;
                }};
                AST params = a.get(1);
                AST block = a.get(3);
                currentReturn = "return " + a.get(2).token.lexeme;
                for (AST param : params.subs) {
                    check(param, newenv);
                }
                String actualReturn = check(block, newenv);
                type_equal(currentReturn, actualReturn, a);
                a.type = "void";
                return "void";
            }
            case "function_param": {
                if (!validVariableType(a.get(2).token.lexeme)) {
	                error(1, a);
	            }
                addVariable(env, a);
                a.type = "void";
                return "void";
            }
            case "block_stmt": {      
                String retvalue = "return void";
                for (AST sub : a.subs) {
                    String ret = check(sub, sub.is("block_stmt") ? env.subenv() : env);
                    if (!ret.equals("return void")) {
                        retvalue = ret;
                    }
                }
                return retvalue;
            }
            case "if_stmt": {
                String rettype = currentReturn;
                for (int i = 0; i < a.subs.size() - 1; i += 2) {
                    type_equal(check(a.get(i), env), "int", a.get(i));
                    String ret = check(a.get(i + 1), env.subenv());
                    if (!ret.equals(currentReturn)) {
                        rettype = ret;
                    }
                }
                if (a.subs.size() % 2 == 1) {
                    String ret = check(a.get(a.subs.size() - 1), env.subenv());
                    if (!ret.equals(currentReturn)) {
                        rettype = ret;
                   }
                } else {
                   rettype = "return void";
                }
                a.type = rettype;
                return rettype;
            }
            case "while_stmt": {
                type_equal(check(a.get(0), env), "int", a);
                ++inWhile;
                String ret = check(a.get(1), env.subenv());
                --inWhile;
                a.type = "return void";
                return "return void";
            }
            case "empty_stmt": {
                a.type = "return void";
                return "return void";
            }
            case "break_stmt": {
                if (inWhile == 0) {
                    error(4, a);
                }
                return "return void";
            }
            case "continue_stmt": {
                if (inWhile == 0) {
                    error(5, a);
                }
                return "return void";
            } 
            case "expr_stmt": {
                check(a.get(0), env);
                a.type = "return void";
                return "return void";
            }
            case "operator_expr": {
                switch (a.get(1).token.type) {
                    case Token.TYPE_GT:
                    case Token.TYPE_GE:
                    case Token.TYPE_EQ:
                    case Token.TYPE_NEQ:
                    case Token.TYPE_LT:
                    case Token.TYPE_LE:
                    {
                        String lhs = check(a.get(0), env);
                        String rhs = check(a.get(2), env);
                        type_equal(lhs, rhs, a);
                        if (lhs.equals("void")) {
                            error(6, a);
                        }
                        a.type = "int";
                        return "int";
                    }
                    case Token.TYPE_PLUS:
                    case Token.TYPE_MINUS:
                    case Token.TYPE_MUL:
                    case Token.TYPE_DIV:
                    {
                        String lhs = check(a.get(0), env);
                        String rhs = check(a.get(2), env);
                        type_equal(lhs, rhs, a);
                        if (!(lhs.equals("int") || lhs.equals("double"))) {
                            error(6, a);
                        }
                        a.type = lhs;
                        return lhs;
                    }
                    default:
                        error(6, a);
                }
            }
            case "ident_expr": {
                Env.Variable v = env.findVariable(a.get(0).token.lexeme);
                if (v == null) {
                    error(7, a);
                }
                a.type = v.type;
                return v.type;
            }
            case "literal_expr": {
                switch (a.get(0).token.type) {
                    case Token.TYPE_LITI: return (a.type = "int");
                    case Token.TYPE_LITS: return (a.type = "int");
                    case Token.TYPE_LITF: return (a.type = "double");
                    case Token.TYPE_LITC: return (a.type = "int");
                    default:
                        error(8, a);
                        return "";
                }
            }
            case "negate_expr": {
                String num = check(a.get(0), env);
                if (!validVariableType(num)) {
                   error(15, a);
                }
                a.type = num;
                return num;
            }
            case "as_expr": {
                String num = check(a.get(0), env);
                String ty  = a.get(1).get(0).token.lexeme;
                if (!validVariableType(num) || !validVariableType(ty)) {
                   error(16, a);
                }
                a.type = ty;
                return ty;
            }
            case "return_stmt": {
                String ret = "";
                if (a.size() > 0) {
                    ret = "return " + check(a.get(0), env);
                } else {
                    ret = "return void";
                }
                if (!ret.equals(currentReturn)) {
                    error(9, a);
                }
                a.type = ret;
                return ret;
            }
            case "assign_expr": {
                if (!a.get(0).token.is(Token.TYPE_IDENT)) {
                    error(10, a);
                }
                Env.Variable v = env.findVariable(a.get(0).token.lexeme);
                if (v.isConst) {
                    error(11, a);
                }
                type_equal(v.type, check(a.get(1), env), a);
                a.type = "void";
                return "void";
            }
            case "call_expr": {
                String fname = a.get(0).token.lexeme;
                List <String> r = new ArrayList<>();
                for (AST sub : a.subs.subList(1, a.subs.size())) {
                    String ret = check(sub, env);
                    if (ret.equals("void")) {
                        error(12, a);
                    }
                    r.add(ret);   
                }
                String ret = tryCall(fname, env, r, a);
                a.type = ret;
                return ret;
            }
        }
        error(50, a);
        return "";
    }
}
