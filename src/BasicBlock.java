import java.util.*;

class BasicBlock {
    public static class Code {
        public int          opcode;
        public long         param;
        public BasicBlock   target_param;
        public Env.Variable ref_param;
        public static final int []sizes = new int[256];
        static {
            sizes[1] = 8;
            for (int i : new int [] { 0x3, 0xa, 0xb, 0xc, 0x1a, 0x41, 0x42, 0x43, 0x48, 0x4a}) {
                sizes[i] = 4;
            }
        }
        public int size() {
            return sizes[opcode] + 1;
        }
        public String toString() {
            return String.format("%X %d %s %s", opcode, 
                param, 
                target_param == null ? "null" : target_param.getName(), 
                ref_param); 
        }
    }

    public void jumpLink(BasicBlock bb) {
        add(new Code() {{
            opcode = 0x41;
            target_param = bb;
        }});
    }

    public void jumpTrue(BasicBlock bb) {
        add(new Code() {{
            opcode = 0x43;
            target_param = bb;
        }});
    }


    public void jumpFalse(BasicBlock bb) {
        add(new Code() {{
            opcode = 0x42;
            target_param = bb;
        }});
    }

    public static class Function {
        public List<BasicBlock> blocks = new ArrayList<>();
        public int nRet = 0, nLoc = 0, nParam = 0, fnIndex = 0;
        public BasicBlock newBlock() {
            BasicBlock bb = new BasicBlock();
            blocks.add(bb);
            return bb;
        }

        public int count() {
            return blocks.stream().map(BasicBlock::count).reduce(0, (a, b) -> a + b);
        }

        public BasicBlock newBlock(String name) {
            BasicBlock a = newBlock();
            a.name = name;
            return a;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Function:\n");
            for (BasicBlock bb : blocks) {
                sb.append(bb).append("\n");
            }
            return sb.toString();
        }

        public void processBlocks() {
            int offset = 0;
            for (BasicBlock bb : blocks) {
                bb.offset = offset;
                offset += bb.count();
            }
            int cnt = 0;
            for (BasicBlock bb : blocks) {
                for (Code cc : bb.codes) {
                    ++cnt;
                    if (cc.target_param != null) {
                        cc.param = cc.target_param.offset - cnt;
                    }
                }
            }
        }
    }

    public String name;
    public List<Code> codes = new ArrayList<>();
    public int offset;
    public BasicBlock add(Code code) {
        codes.add(code);
        return this;
    }
    public String getName() {
        return name != null ? name : String.format("%X", hashCode());
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("BB[").append(getName()).append("]").append(":").append(size()).append("\n");
        for (Code c : codes) {
            sb.append(c).append("\n");
        }
        return sb.toString();
    }
    public int size() {
        return codes.stream()
                    .map(Code::size)
                    .reduce(0, (a, b) -> a + b);
    }
    public int count() {
        return codes.size();
    }

    

}
