package CompilerStages;

import Nodes.SyntaxNode;

import java.util.Vector;

public class TreeCrawler {
    SyntaxNode treeRoot;
    Vector<String> usedScopes;
    int nextScope=1;
    int nextVarName = 1;
    int nextProcName = 1;

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
    public void varCrawl(){
        Vector<SyntaxNode> vars = new Vector<>();
        varCrawl(treeRoot, vars);
    }
    public void loopCrawl(){
        forLoopCrawl(treeRoot);
    }
    public void procRename(){
        procRename(treeRoot);
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
    private void procCrawl(SyntaxNode curr, Vector<String> procs){
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
    private void varCrawl(SyntaxNode curr, Vector<SyntaxNode> vars){
        if(curr == null || curr.getNodeType().name().equals("TERMINAL"))
            return;

        if(curr.getData("symbol").equals("LOOP")&&curr.getChildren().elementAt(0).getData("symbol").equals("for")){
            SyntaxNode child = curr.getChildren().get(1).getChildren().get(0);
            //child is the var
            child.addInternalName("V".concat(String.valueOf(nextVarName++)));
            vars.add(child);
        }

        if(curr.getData("symbol").equals("VAR")){
            SyntaxNode child = curr.getChildren().get(0);
            if(child.getData("internalName")==null){
                //child is the var and is not counting var of a for loop
                for(SyntaxNode node: vars){
                    if(node.getData("symbol").equals(child.getData("symbol"))){
                        //node matches curr symbol
                        //check scopes to determine new name
                        if(child.getData("scope").contains(node.getData("scope"))){
                            child.addInternalName(node.getData("internalName"));
                        }
                    }
                }
                if(child.getData("internalName")==null){
                    child.addInternalName("V".concat(String.valueOf(nextVarName++)));
                }
                vars.add(child);
            }
        }

        for(SyntaxNode node: curr.getChildren()){
            varCrawl(node, vars);
        }
    }
    private void forLoopCrawl(SyntaxNode curr){
        if(curr != null){
            if(curr.getData("symbol").equals("LOOP")&&curr.getChildren().elementAt(0).getData("symbol").equals("for")){
                //check for loop and call check assignment
                SyntaxNode tempV1, tempV2, tempV3, tempV4;
                tempV1 = curr.getChildren().elementAt(1).getChildren().elementAt(0);
                tempV2 = curr.getChildren().elementAt(2).getChildren().elementAt(0);
                tempV3 = curr.getChildren().elementAt(4).getChildren().elementAt(0);
                tempV4 = curr.getChildren().elementAt(5).getChildren().elementAt(0);
                if(!(tempV1.getData("internalName").equals(tempV2.getData("internalName")))||
                        !(tempV1.getData("internalName").equals(tempV3.getData("internalName")))||
                        !(tempV1.getData("internalName").equals(tempV4.getData("internalName")))){
                    treeRoot.error = true;
                    treeRoot.errMessage = "for loop contains incorrect variable structure in declaration";
                    return;
                }
                if(checkAssignment(curr.getChildren().elementAt(6), tempV1.getData("internalName"))){
                    treeRoot.error = true;
                    treeRoot.errMessage = "for loop counting variable : " + tempV1.getData("symbol") +
                            ", reassigned";
                    return;
                }
            }
            for(SyntaxNode c: curr.getChildren()){
                forLoopCrawl(c);
            }
        }
    }
    private boolean checkAssignment(SyntaxNode curr, String intName){
        if(curr != null){
            if(curr.getData("symbol").equals("ASSIGN")&&curr.getChildren().elementAt(0).getChildren().elementAt(0).getData("internalName").equals(intName)){
                return true;
            }
            for(SyntaxNode c: curr.getChildren()){
                if(checkAssignment(c, intName)){
                    return true;
                }
            }
        }
        return false;
    }
    private void procRename(SyntaxNode curr){
        //TODO:Implement procedure renaming
    }
}
