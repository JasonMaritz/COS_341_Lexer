package Nodes;

import java.util.HashMap;
import java.util.Vector;

public class SyntaxNode {
    public boolean error;
    public String errMessage;

    public Vector<SyntaxNode> getChildren() {
        return  children;
    }

    public String getData(String key) {
        return data.get(key);
    }

    public enum type{TERMINAL, NONTERMINAL}
    type nodeType;
    public type getNodeType(){
        return nodeType;
    }
    HashMap<String, String> data;
    Vector<SyntaxNode> children;
    public SyntaxNode(type ntype, String symbol){
        data = new HashMap<>();
        data.put("symbol", symbol);
        nodeType = ntype;
        children = new Vector<>();
    }

    public void addChild(SyntaxNode nChild){
        children.add(nChild);
    }

    public String toString(int i){
        if(error){
            return errMessage;
        }
        StringBuilder res = new StringBuilder();
        res.append("\t".repeat(Math.max(0, i)));
        int k = ++i;
        res.append(nodeType.name()).append(": ").append(data.get("symbol")).append(", Scope: ").append(data.get("scope"));
        if(nodeType.name().equals(type.TERMINAL.name())){
            if(data.get("internalName")!=null)
                res.append(", internalName: ").append(data.get("internalName"));
        }
        res.append("\n");
        for(SyntaxNode n: children){
            if(n!=null) {
                res.append(n.toString(k));
            }
        }
        return res.toString();
    }

    public void addScope(String s){
        data.put("scope", s);
    }
    public void addInternalName(String s){ data.put("internalName", s); }
}
