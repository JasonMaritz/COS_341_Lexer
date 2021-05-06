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
    public void procRules(){
        procVarCrawl();
        procCrawl();
    }
    public void procVarCrawl(){
        Vector<String> varnames = new Vector<>();
        procVarCrawl(treeRoot, varnames, 0);
        procVarCrawl(treeRoot, varnames, 1);
    }
    private void procVarCrawl(SyntaxNode curr, Vector<String> names, int mode){
        //mode 0 means add varnames
        //mode 1 means check proc names
        if(curr == null){
            return;
        }
        switch (mode){
            case 0:
                if(curr.getData("symbol").equals("VAR")){
                    if(!names.contains(curr.getChildren().elementAt(0).getData("symbol"))){
                        names.add(curr.getChildren().elementAt(0).getData("symbol"));
                    }
                }
                break;
            case 1:
                if(curr.getData("symbol").equals(("PROC"))){
                    if(names.contains(curr.getChildren().elementAt(1).getData("symbol"))){
                        //error
                        treeRoot.error = true;
                        treeRoot.errMessage = "Proc "+curr.getChildren().elementAt(1).getData("symbol")+
                                " shares name with variable";
                    }
                }
                break;
        }
        for(SyntaxNode n: curr.getChildren()){
            procVarCrawl(n, names, mode);
        }
    }
    public void procCrawl(){
        Vector<String> procnames = new Vector<>();
        procCrawl(treeRoot, procnames);
        for(String s: procnames){
            for(String ss: procnames){
                if(s.equals(ss))
                    continue;
                //check parent scope
                String temp, sName, ssName, sScope, ssScope, ssFullscope, sFullscope;
                int pos;
                temp = s;
                pos = temp.indexOf("#");
                sName = temp.substring(0,pos);
                temp = temp.substring(pos+1);
                sFullscope = temp;
                //get parent scope
                if(temp.lastIndexOf(".") > 0)
                    sScope = temp.substring(0,temp.lastIndexOf("."));
                else
                    sScope = temp;
                temp = ss;
                pos = temp.indexOf("#");
                ssName = temp.substring(0,pos);
                temp = temp.substring(pos+1);
                ssFullscope = temp;
                //get parent scope
                if(temp.lastIndexOf(".") > 0)
                    ssScope = temp.substring(0,temp.lastIndexOf("."));
                else
                    ssScope = temp;
                if(sName.equals(ssName)&&sScope.equals(ssScope)){
                    treeRoot.error = true;
                    treeRoot.errMessage = "Procedures in same scope share a name: "+sName;
                }

                if(sName.equals(ssName)&&(ssFullscope.contains(sFullscope))){
                    treeRoot.error = true;
                    treeRoot.errMessage = "Procedure cannot redefine an ancestor procedure";
                }
            }
        }
    }
    public void procCrawl(SyntaxNode curr, Vector<String> procs){
        if(curr == null){
            return;
        }
        if(curr.getData("symbol").equals("PROC")){
            if(!procs.contains(curr.getChildren().elementAt(1).getData("symbol").concat(curr.getChildren().elementAt(1).getData("scope")))){
                procs.add(curr.getChildren().elementAt(1).getData("symbol").concat("#").concat(curr.getChildren().elementAt(1).getData("scope")));
            }
        }
        for(SyntaxNode n: curr.getChildren()){
            procCrawl(n, procs);
        }
    }
}
