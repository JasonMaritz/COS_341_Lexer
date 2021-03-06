package Nodes;

import java.util.HashMap;
import java.util.Vector;

public class SyntaxNode {
    public enum type{TERMINAL, NONTERMINAL}
    type nodeType;
    public boolean error;
    public String errMessage;
    public boolean warn;
    public String warnMessage="";
    HashMap<String, String> data;
    Vector<SyntaxNode> children;

    public SyntaxNode(SyntaxNode syntaxNode) {
        nodeType = syntaxNode.nodeType;
        error = syntaxNode.error;
        errMessage = syntaxNode.errMessage;
        warn = syntaxNode.warn;
        warnMessage = syntaxNode.warnMessage;
        data = (HashMap<String, String>) syntaxNode.data.clone();
        children = (Vector<SyntaxNode>) syntaxNode.children.clone();
    }

    public Vector<SyntaxNode> getChildren() {
        return  children;
    }

    public String getData(String key) {
        return data.get(key);
    }


    public type getNodeType(){
        return nodeType;
    }

    public SyntaxNode(type ntype, String symbol){
        data = new HashMap<>();
        data.put("symbol", symbol);
        data.put("type", "u");
        nodeType = ntype;
        children = new Vector<>();
    }

    public void addChild(SyntaxNode nChild){
        children.add(nChild);
    }

    public String toString(int i){
        StringBuilder res = new StringBuilder();
        if(error){
            return errMessage;
        }else if(warn){
            res.append(warnMessage).append('\n');
        }
        res.append("\t".repeat(Math.max(0, i)));
        int k = ++i;
        res.append(nodeType.name()).append(": ").append(data.get("symbol")).append(", Scope: ").append(data.get("scope"));
        if(nodeType.name().equals(type.TERMINAL.name())){
            if(data.get("internalName")!=null)
                res.append(", internalName: ").append(data.get("internalName"));
        }
        if(nodeType.name().equals(type.NONTERMINAL.name()))
            res.append(", type: ").append(data.get("type")).append(", value: ").append(data.get("value"));

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
    public void addType(String s){data.put("type", s);}
    public void addValue(String s){data.put("value", s);}
}
