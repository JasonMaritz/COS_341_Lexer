package Nodes;

import java.util.HashMap;
import java.util.Vector;

public class SyntaxNode {
    public enum type{TERMINAL, NONTERMINAL}
    type nodeType;
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
        String res = "";
        for(int j = 0; j < i; j++){
            res += "\t";
        }
        int k = ++i;
        res += nodeType.name() + ": " + data.get("symbol") + "\n";
        for(SyntaxNode n: children){
            if(n!=null) {
                res += n.toString(k);
            }
        }
        return res;
    }
}
