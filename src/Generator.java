import java.util.*;
import java.io.*;
class Generator {
    void error(int c, AST n) {
        System.out.print(n);
//        throw new RuntimeException("" + (400 + c));
        System.exit(1);
    }

    int globalOffset = 0,
        paramOffset = 0,
        localOffset = 0,
        staticOffset = 0;
    BasicBlock.Function currentFn;
    Map <String, Integer> fnmap = new HashMap<>();
    Map <String, BasicBlock.Function> fnblock = new HashMap<>();
    int fncount = 0;
    List <BasicBlock.Function> fns = new ArrayList<>();
    List <String> globals = new ArrayList<>();
    Env globalEnv;
    
    void addVariable(Env env, AST n) {
        Env.Variable v = null;
        if (n.is("decl_stmt")) {
           String vname = n.get(1).token.lexeme;
           String vtype = n.get(2).token.lexeme;
           boolean visConst = n.get(0).equals(Token.TYPE_CONST_KW);
           
           v = new Env.Variable() {{
               name = vname;
               type = vtype;
               isConst = visConst;
               node = n;
               if (currentFn == null) {
                    segment = "g";
                    offset = globalOffset++;
                    globals.add(null);
               } else {
                    segment = "l";
                    offset = localOffset++;
               }
           }};

        } else if (n.is("function")) {
           String vname = n.get(0).token.lexeme;
           String vtype = n.get(2).token.lexeme;
           boolean visConst = false;
           v = new Env.Variable() {{
               name = vname;
               type = vtype;
               isConst = visConst;
               node = n;
               data = globalOffset++;
           }};
           
           globals.add(vname);
           ++fncount;
           fnmap.put(vname, fncount);
           currentFn = new BasicBlock.Function();
           fnblock.put(vname, currentFn);
           fns.add(currentFn);
        } else if (n.is("function_param")) {
           v = new Env.Variable() {{
               name = n.get(1).token.lexeme;
               type = n.get(2).token.lexeme;
               isConst = n.get(0).is(Token.TYPE_CONST_KW);
               node = n;
               segment = "p";
               offset = paramOffset++;

           }};
        } else if(n.is("literal_expr")) {
           String vname = "a" + globalOffset;
           boolean visConst = true;
           
           v = new Env.Variable() {{
               name = vname;
               type = "string";
               isConst = visConst;
               node = n;
               segment = "g";
               offset = globalOffset++;
               globals.add(n.get(0).token.lexeme);
           }};
        }
        if (v == null) {
            error(1, n);
        }
        if (!env.addVariable(v)) {
            System.out.println(v);
            error(2, n);
        }
    }

    void loadVariable(Env.Variable a, BasicBlock bb) {
        switch (a.segment) {
            case "p": bb.add(new BasicBlock.Code() {{ opcode = 0x0b; param = a.offset; }}); break;
            case "l": bb.add(new BasicBlock.Code() {{ opcode = 0x0a; param = a.offset; }}); break;
            case "g": bb.add(new BasicBlock.Code() {{ opcode = 0x0c; param = a.offset; }}); break;
            default:
                error(4, null);
                break;
        }
    }


    Stack <BasicBlock> returns = new Stack<>(),
                       breaks = new Stack<>(), 
                       continues = new Stack<>();
    void gen(AST a, Env env, BasicBlock begin, BasicBlock next) {
        switch (a.head) {
	        case "program":  {
    	        globalEnv = env;
    	        BasicBlock.Function start = new BasicBlock.Function();
	            start.nParam = 1;
	            fns.add(0, start);
	            fnmap.put("_start", 0);
	            fnblock.put("_start", start);
	            BasicBlock bb = start.newBlock("start");
	            globals.add("_start");
	            ++globalOffset;
	            for (AST sub : a.subs) {
	                gen(sub, env, null, null);
	            }    
	            for (AST sub : a.subs) {
	                if (sub.is("decl_stmt")) {
           	            if (sub.size() == 4) {
               	            loadVariable(env.findVariable(sub.get(1).token.lexeme), bb);
        	                gen(sub.get(3), env, bb, null);
                           	bb.add(new BasicBlock.Code() {{ opcode = 0x17; }});
            	        }
    	            }
	            }
                bb.add(new BasicBlock.Code() {{
                    opcode = 0x1a;
                    param  = 1;
                }});

	            bb.add(new BasicBlock.Code() {{
                    opcode = 0x48; param = fnmap.get("main");
	            }});
                bb.add(new BasicBlock.Code() {{
                    opcode = 0x03;
                    param  = 1;
                }});

	            ++fncount;
	            
	            return;
	        }
	        case "decl_stmt": {
     	        addVariable(env, a);
   	            if (begin != null) {
       	            if (a.size() == 4) {
           	            loadVariable(env.findVariable(a.get(1).token.lexeme), begin);
    	                gen(a.get(3), env, begin, next);
                       	begin.add(new BasicBlock.Code() {{ opcode = 0x17; }});
        	        }
        	        if (next != null) {
                        begin.jumpLink(next);
                    }
	            }
	            return;
	        }
            case "function": {
                addVariable(env, a);
                currentFn.nRet = 1;
                currentFn.fnIndex = globalOffset - 1;

                Env newenv = new Env() {{
                   next = env;
                }};
                localOffset = 0;
                paramOffset = 1;
                AST params = a.get(1);
                AST block = a.get(3);
                for (AST param : params.subs) {
                    gen(param, newenv, null, null);
                }
                BasicBlock body    = currentFn.newBlock("body");
                BasicBlock ret     = currentFn.newBlock("return");
                returns.push(ret);
                ret.add(new BasicBlock.Code() {{
                        opcode = 0x0b;
                        param = 0;
                }});

                ret.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}}); 
                ret.add(new BasicBlock.Code() {{
                    opcode = 0x17;
                }});
                ret.add(new BasicBlock.Code() {{
                    opcode = 0x49;
                }});
                gen(block, newenv, body, ret);
                body.jumpLink(ret);
                body.add(new BasicBlock.Code());
                returns.pop();
                currentFn.processBlocks();
                currentFn.nParam = paramOffset;
                currentFn.nLoc = localOffset;
                currentFn = null;
	            return;
            }
            case "function_param": {
                addVariable(env, a);
                return;
            }
            case "block_stmt": {  
                List <BasicBlock> lbb = new ArrayList<>();
                for (int i = 0; i < a.subs.size(); ++i) {
                    lbb.add(currentFn.newBlock("block-statement" + String.format("%d-%x", i, a.hashCode())));
                }
                if (a.subs.size() > 0) {
                    begin.jumpLink(lbb.get(0));
                } else {
                    begin.jumpLink(next);
                    return;
                }
                lbb.add(next);
                int i = 0;
                for (AST sub : a.subs) {
                    gen(sub, env, lbb.get(i), lbb.get(i + 1));
                    ++i;
                }
                return;
            }
            case "if_stmt": {
                List <BasicBlock> lbb = new ArrayList<>();
                for (int i = 0; i + 1 < a.subs.size(); i += 2) {
                    lbb.add(currentFn.newBlock("if-branch" + String.format("%d-%x", i / 2, a.hashCode())));
                }
                if (a.subs.size() >= 2) {
                    begin.jumpLink(lbb.get(0));
                }
                if (a.subs.size() % 2 == 1) {
                    lbb.add(currentFn.newBlock("else-branch" + String.format("%x", a.hashCode())));
                }
                lbb.add(next);
                for (int i = 0; i + 1 < a.subs.size(); i += 2) {
                    BasicBlock bb = lbb.get(i / 2),
                               nextblock = lbb.get(i / 2 + 1);
                    gen(a.get(i), env, bb, nextblock);
                    bb.jumpFalse(nextblock);
                    Env newenv = new Env() {{
                      next = env;
                    }};

                    gen(a.get(i + 1), newenv, bb, next);
                    bb.jumpLink(next);
                }
                if (a.subs.size() % 2 == 1) {
                    BasicBlock bb = lbb.get(lbb.size() - 1);
                    gen(a.get(a.subs.size() - 1), env, bb, next);
                }
                
                return;                
            }
            case "while_stmt": {
                BasicBlock bb = currentFn.newBlock("while-start");
                begin.jumpLink(bb);
                gen(a.get(0), env, bb, next);
                bb.jumpFalse(next);
                continues.push(bb);
                breaks.push(next);
                BasicBlock body = currentFn.newBlock("while-body");
                bb.jumpLink(body);
                Env newenv = new Env() {{
                    next = env;
                }};

                gen(a.get(1), newenv, body, bb);
                body.jumpLink(bb);
                breaks.pop();
                continues.pop();
                return;
            }
            case "empty_stmt": {
                begin.jumpLink(next);
                return;
            }
            case "break_stmt": {
                begin.jumpLink(breaks.peek());
                return;
            }
            case "continue_stmt": {
                begin.jumpLink(continues.peek());
                return;
            } 
            case "expr_stmt": {
                gen(a.get(0), env, begin, next);
                begin.add(new BasicBlock.Code() {{
                    opcode = 0x02;    // POP
                }});
                begin.jumpLink(next);
                return;
            }
            case "operator_expr": {
                gen(a.get(0), env, begin, next);
                gen(a.get(2), env, begin, next);
                int cmp = 0x30;
                int opadd_ = 0;
                if (a.get(0).type.equals("double")) {
                    cmp = 0x32;
                    opadd_ = 4;
                }
                int opadd = opadd_;
                switch (a.get(1).token.type) {
                    case Token.TYPE_GT: {
                         for (int c :new int[]{cmp, 0x3a}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }
                         return;
                    }
                    case Token.TYPE_GE: {
                         for (int c : new int[]{cmp, 0x39, 0x2e}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }
                         return;
                    }
                    case Token.TYPE_EQ: {
                         for (int c : new int[]{cmp, 0x2e}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }                        
                         return;
                    }
                    case Token.TYPE_NEQ:
                         for (int c : new int[]{cmp, 0x2e, 0x2e}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }                        
                         return;
                    case Token.TYPE_LT:
                         for (int c : new int[]{cmp, 0x39}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }
                         return;
                    case Token.TYPE_LE:
                         for (int c : new int[]{cmp, 0x3a, 0x2e}) {
                             begin.add(new BasicBlock.Code() {{ opcode = c;  }});
                         }
                         return;
                    case Token.TYPE_PLUS:
                    {
                         begin.add(new BasicBlock.Code() {{ opcode = opadd + 0x20;  }});
                         return;
                    }
                    case Token.TYPE_MINUS:
                    {
                         begin.add(new BasicBlock.Code() {{ opcode = opadd + 0x21;  }});
                         return;
                    }

                    case Token.TYPE_MUL:
                    {
                         begin.add(new BasicBlock.Code() {{ opcode = opadd + 0x22;  }});
                         return;
                    }

                    case Token.TYPE_DIV:
                    {
                         begin.add(new BasicBlock.Code() {{ opcode = opadd + 0x23;  }});
                         return;
                    }
                    default:
                        error(6, a);
                }
                return;
            }
            case "ident_expr": {
                loadVariable(env.findVariable(a.get(0).token.lexeme), begin);
               	begin.add(new BasicBlock.Code() {{ opcode = 0x13; }}); 
               	return;
            }
            
            case "literal_expr": {
                if (a.get(0).token.is(Token.TYPE_LITI)) {
                    int val = Integer.parseInt(a.get(0).token.lexeme);
                    begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = val;}}); 
                } else {
                    addVariable(globalEnv, a);
                    begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = globalOffset - 1;}});             
                }
                return;
            }
            case "negate_expr": {
                gen(a.get(0), env, begin, next);
                begin.add(new BasicBlock.Code() {{ opcode = a.type.equals("int") ? 0x34 : 0x35; }});
                return;
            }
            case "as_expr": {
                if (a.get(0).type.equals(a.type)) {
                    return;
                }
                if (a.get(0).type.equals("int") && a.type.equals("double")) {
                    begin.add(new BasicBlock.Code() {{ opcode = 0x36; }});                 
                } else if (a.get(0).type.equals("double") && a.type.equals("int")) {
                    begin.add(new BasicBlock.Code() {{ opcode = 0x37; }});                 
                }
                return;
            }
            case "return_stmt": {
                if (a.size() == 0) {
                    begin.add(new BasicBlock.Code() {{
                        opcode = 0x0b;
                        param = 0;
                    }});

                    begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}}); 
                    begin.add(new BasicBlock.Code() {{  opcode = 0x17;   }});
                    begin.add(new BasicBlock.Code() {{  opcode = 0x49;   }});

                    return;
                } else {
                    begin.add(new BasicBlock.Code() {{
                        opcode = 0x0b;
                        param = 0;
                    }});
                    gen(a.get(0), env, begin, next);
                    begin.add(new BasicBlock.Code() {{  opcode = 0x17;   }});
                    begin.add(new BasicBlock.Code() {{  opcode = 0x49;   }});
                    return;                      
                }
            }
            case "assign_expr": {
                loadVariable(env.findVariable(a.get(0).token.lexeme), begin);
                gen(a.get(1), env, begin, next);
               	begin.add(new BasicBlock.Code() {{ opcode = 0x17; }}); 
               	begin.add(new BasicBlock.Code() {{ opcode = 0x1; param = 0; }}); 
               	return;
            }
            
            case "call_expr": {
                String name = a.get(0).token.lexeme;
                Integer id_ = null;
                BasicBlock.Function fn = fnblock.get(name);
                if (fn != null) {
                   id_ = fnmap.get(name);
                   begin.add(new BasicBlock.Code() {{
                      opcode = 0x1a;
                      param  = 1;
                   }});
                }
                Integer id = id_;
                for (int i = 1; i < a.size(); ++i) {
                    gen(a.get(i), env, begin, next);
                }


                switch (name) {
                    case "getint": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x50; }});
                         return;
                    } 
                    case "getchar": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x51; }});
                         return;
                    } 
                    case "getdouble": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x52; }});
                         return;
                    } 
                    case "putint": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x54; }});
                         begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}});
                         return;
                    } 
                    case "putdouble": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x56; }});
                         begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}});
                         return;
                    } 
                    case "putchar": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x55; }});
                         begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}});
                         return;
                    } 
                    case "putstr": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x57; }});
                         begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}});
                         return;
                    } 
                    case "putln": {
                         begin.add(new BasicBlock.Code() {{ opcode = 0x58; }});
                         begin.add(new BasicBlock.Code() {{ opcode = 0x01; param = 0;}});
                         return;
                    } 
                    default: {
                         if (id == null) {
                            error(10, a);
                         }
                         begin.add(new BasicBlock.Code() {{ opcode = 0x48; param = id;}});
                         /*
                         if (a.size() > 1) {
                            begin.add(new BasicBlock.Code() {{ opcode = 0x03; param = a.size() - 1;}});
                         }
                         */
                         return; 
                    }
                }
            }
            
        }

        return; 
    }


    public void output(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(0x72303b3e);
        dos.writeInt(1);
        
        dos.writeInt(globals.size());
        for (int i = 0; i < globals.size(); ++i) {
            if (globals.get(i) == null) {
                dos.writeByte(0);
                dos.writeInt(8);
                dos.writeLong(0);
            } else {
                dos.writeByte(1);
                dos.writeInt(globals.get(i).length());
                dos.write(globals.get(i).getBytes(), 0, globals.get(i).length());
            }
        }
        
        dos.writeInt(fncount);
        for (int i = 0; i < fns.size(); ++i) {
            BasicBlock.Function fn = fns.get(i);
            dos.writeInt(fn.fnIndex);
            dos.writeInt(fn.nRet);
            dos.writeInt(fn.nParam - 1);
            dos.writeInt(fn.nLoc);
            dos.writeInt(fn.count());
            for (BasicBlock bb : fn.blocks) {
                for (BasicBlock.Code cc : bb.codes) {
                    dos.writeByte(cc.opcode);
                    if (cc.size() == 5) {
                        dos.writeInt((int)cc.param);
                    } else if (cc.size() == 9) {
                        dos.writeLong(cc.param);
                    }
                }
            }
        }
        dos.flush();
        
    }
    

    public static void main(String []args) {
        try {
        System.out.println("1");
            var f = new FileInputStream(new File(args[0]));
            var t = new Tokenizer(f);
            var p = new Parser(t);
            var c = new Checker();
            AST a = p.parse_program();
            c.check(a, new Env());
            var g = new Generator();
            g.gen(a, new Env(), null, null);
            var o = new FileOutputStream(new File(args[1]));
            g.output(o);
            o.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
