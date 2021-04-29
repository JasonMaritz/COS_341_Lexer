package Nodes;

import java.util.HashMap;
import java.util.Vector;

public class SyntaxNode {
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
        StringBuilder res = new StringBuilder();
        res.append("\t".repeat(Math.max(0, i)));
        int k = ++i;
        res.append(nodeType.name()).append(": ").append(data.get("symbol")).append(" Scope: ").append(data.get("scope")).
                append("\n");
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
}
