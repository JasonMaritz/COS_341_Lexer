package CompilerStages;

import Nodes.SyntaxNode;

import java.util.Vector;

public class TreeCrawler {
    SyntaxNode treeRoot;
    Vector<String> usedScopes;
    int nextScope=1;

    public TreeCrawler(SyntaxNode root){
        usedScopes = new Vector<>();
        treeRoot = root;
    }
    public void scopeCrawl(){
        usedScopes.add("0");
        scopeCrawl(treeRoot, "0");
    }

    private void scopeCrawl(SyntaxNode curr, String parentScope){
        if(curr == null){
            return;
        }
        if(curr.getNodeType() == SyntaxNode.type.NONTERMINAL){
            //add scope but dont return to allow children to be scoped as well
            if((curr.getData("symbol").equals("LOOP")&&curr.getChildren().get(0).getData("symbol").equals("for"))||curr.getData("symbol").equals("PROC")){
                    //int i = 1;
//                    while(usedScopes.contains(parentScope+"."+i)) {
//                        i++;
//                    }
                    curr.addScope(parentScope+"."+nextScope++);
                    //usedScopes.add(parentScope+"."+i);
            }else{
                curr.addScope(parentScope);
            }
        }else{
            //add scope then return to previous frame
            curr.addScope(parentScope);
            return;
        }
        //recur through all children adding scope as you go
        for(SyntaxNode n: curr.getChildren()){
            scopeCrawl(n, curr.getData("scope"));
        }
    }
}
