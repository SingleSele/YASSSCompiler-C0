import java.util.*;
class Env {
    public static class Variable {
        public boolean isConst;
        public String  name;
        public String  type;
        public AST     node;
        public Object  data;
        public String  segment;
        public int     offset;
        public String toString() {
            return String.format("%s %s %s %s (%s, %d)\n",
                isConst ? "const" : "let", name, type, data, segment, offset);
        }
        @SuppressWarnings("unchecked")
        public <T> T getData(Class<T> a) {
            assert data.getClass().equals(a);
            return (T)data;
        }
    }

    Map <String, Variable> data = new HashMap<>();
    Env next;

    public Env subenv() {
        Env self = this;
        return new Env() {{
            next = self;
        }};
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<");
        for (Map.Entry<String, Variable> d : data.entrySet()) {
            sb.append(d.getKey()).append(" = ").append(d.getValue()).append("\n");
        }
        if (next != null) {
            sb.append("--next--\n").append(next.toString());
        }
        sb.append(">");
        return sb.toString();
    }

    public boolean addVariable(Variable v) {
        if (data.containsKey(v.name)) {
            return false;
        }
        data.put(v.name, v);
        return true;
    }

    public Variable findVariable(String name) {
        if (data.containsKey(name)) {
            return data.get(name);
        }
        if (next != null) {
            return next.findVariable(name);
        }
        return null;
    }
}
