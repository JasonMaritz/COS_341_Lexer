package CompilerStages;

import Nodes.SyntaxNode;

import java.util.Vector;

public class TreeCrawler {
    SyntaxNode treeRoot;
    Vector<String> usedScopes;

    public TreeCrawler(SyntaxNode root){
        usedScopes = new Vector<>();
        treeRoot = root;
    }
    public void scopeCrawl(){
        scopeCrawl(treeRoot, "0");
    }

    private void scopeCrawl(SyntaxNode curr, String parentScope){
        if(curr.getNodeType() == SyntaxNode.type.NONTERMINAL){
            //add scope but dont return to allow children to be scoped as well
        }else{
            //add scope then return to previous frame
            return;
        }
        //recur through all children adding scope as you go
        for(SyntaxNode n: curr.getChildren()){
            scopeCrawl(n, curr.getData("scope"));
        }
    }
}
