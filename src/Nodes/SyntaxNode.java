package Nodes;

import java.util.Vector;

public class SyntaxNode {
    public enum type{TERMINAL, NONTERMINAL}
    type nodeType;
    String grammerSymbol;
    Vector<SyntaxNode> children;
    public SyntaxNode(type ntype, String symbol){
        nodeType = ntype;
        grammerSymbol = symbol;
        children = new Vector<>();
    }

    public void addChild(SyntaxNode nChild){
        children.add(nChild);
    }

    public String toString(){
        return "Fuckall for now";
    }
}
