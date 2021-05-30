package CompilerStages;

import Nodes.SymbolTable;
import Nodes.SyntaxNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class TreeCrawler {
    SyntaxNode treeRoot;
    Vector<String> usedScopes;
    SymbolTable symTable = new SymbolTable();
    int nextScope=1;
    int nextVarName = 1;
    int nextProcName = 0;

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
        procRename(treeRoot, null, null);
        treePrune(treeRoot);
    }
    public void typeCrawl(){
        populateSymbolTable(treeRoot);
        initTypes(treeRoot);
        typeCrawl(treeRoot);
    }
    public boolean errorOut(){
        if(!treeRoot.error)
            treeRoot.errMessage = "";
        boolean ret = errorOut(treeRoot);
        treeRoot.error = ret;
        return ret;
    }
    public void deadCodeCrawl(){
        deadCodeCrawl(treeRoot);
        deadCodePrune(treeRoot);
    }
    private void deadCodeCrawl(SyntaxNode curr){
        if(curr == null || curr.getNodeType().name().equals(SyntaxNode.type.TERMINAL.name()))
            return;
        for(SyntaxNode c: curr.getChildren()){
            deadCodeCrawl(c);
        }
        //<editor-fold desc="Meta Rule">
        boolean isDead = curr.getChildren().get(0).getData("type").equals("d");
        for(SyntaxNode c: curr.getChildren()){
            isDead = isDead && c.getData("type").equals("d");
        }
        if(isDead){
            curr.addType("d");
            return;
        }
        //</editor-fold>
        //<editor-fold desc="Branch or Loop">
        if(curr.getData("symbol").equals("BRANCH")){
            int production = identifyProd(curr);
            switch (production){
                case 28:
                    //if then
                    SyntaxNode fBool = curr.getChildren().get(1);
                    if(fBool.getChildren().get(0).getData("symbol").equals("not")){
                        SyntaxNode fBool2 = fBool.getChildren().get(1);
                        if(fBool2.getData("type").equals("f")){
                            SyntaxNode ifCode = new SyntaxNode(curr.getChildren().get(3));
                            curr.getChildren().removeAllElements();
                            curr.getChildren().add(ifCode);
                        }
                    }else{
                        if(fBool.getData("type").equals("f")){
                            curr.addType("d");
                        }
                    }
                    break;
                case 29:
                    SyntaxNode f1Bool = curr.getChildren().get(1);
                    if(f1Bool.getChildren().get(0).getData("symbol").equals("not")){
                        SyntaxNode fBool2 = f1Bool.getChildren().get(1);
                        if(fBool2.getData("type").equals("f")){
                            SyntaxNode ifCode = new SyntaxNode(curr.getChildren().get(3));
                            curr.getChildren().removeAllElements();
                            curr.getChildren().add(ifCode);
                        }
                    }else{
                        SyntaxNode fBool2 = f1Bool.getChildren().get(1);
                        if(fBool2.getData("type").equals("f")){
                            SyntaxNode ifCode = new SyntaxNode(curr.getChildren().get(5));
                            curr.getChildren().removeAllElements();
                            curr.getChildren().add(ifCode);
                        }
                    }
                    break;
            }
        }else if(curr.getData("symbol").equals("LOOP")){
            int production = identifyProd(curr);
            switch (production){
                case 30:
                    //while
                    SyntaxNode wBool = curr.getChildren().get(1);
                    if(wBool.getChildren().get(0).getData("symbol").equals("not")){
                        if(wBool.getChildren().get(1).getData("type").equals("f")) {
                            treeRoot.warn = true;
                            treeRoot.warnMessage = "WARNING: infinite loop";
                        }
                    }else{
                        if(wBool.getData("type").equals("f"))
                            curr.addType("d");
                    }
                    break;
                case 31:
                    //for
                    SyntaxNode fVar1, fVar2;
                    fVar1 = curr.getChildren().get(2).getChildren().get(0);
                    fVar2 = curr.getChildren().get(3).getChildren().get(0);
                    if(fVar1.getData("internalName").equals(fVar2.getData("internalName")))
                        curr.addType("d");
                    break;
            }
        }
        //</editor-fold>
    }
    private void deadCodePrune(SyntaxNode curr){
        if(curr == null||curr.getData("type").equals("d"))
            return;
        curr.getChildren().removeIf(c -> c!=null && c.getData("type").equals("d"));
        for(SyntaxNode c: curr.getChildren()){
            deadCodePrune(c);
        }
    }
    private boolean errorOut(SyntaxNode curr){
        if(curr == null)
            return false;
        if(curr.getNodeType().name().equals(SyntaxNode.type.TERMINAL.name()))
            return false;
        boolean ret = false;
        if(curr.getData("type").equals("u")||curr.getData("type").equals("e")){
            ret = true;
            treeRoot.error = true;
            treeRoot.errMessage = "Type error detected!";
        }
        for(SyntaxNode c: curr.getChildren()){
            boolean ret1 = errorOut(c);
            ret = ret || ret1;
        }
        return ret;
    }
    private void populateSymbolTable(SyntaxNode curr){
        if(curr == null){
            return;
        }
        if(curr.getNodeType().equals(SyntaxNode.type.TERMINAL)){
            String intName = curr.getData("internalName");
            if(intName!=null){
                HashMap<String, HashMap<String, String>> tab = symTable.getTable();
                HashMap<String, String> row = tab.get(intName);
                if(row!=null){
                    row.put("internalName", intName);
                    row.put("scope", curr.getData("scope"));
                    row.put("type", "u");
                }else{
                    tab.put(intName, new HashMap<>());
                    row = tab.get(intName);
                    row.put("internalName", intName);
                    row.put("scope", curr.getData("scope"));
                    row.put("type", "u");
                }
            }
        }
        for(SyntaxNode c: curr.getChildren()){
            populateSymbolTable(c);
        }
    }
    private void typeCrawl(SyntaxNode curr){
        if(curr == null)
            return;
        if(curr.getNodeType().name().equals(SyntaxNode.type.TERMINAL.name()))
            return;
        int production = identifyProd(curr);
        switch (production) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 10:
            case 11:
            case 13:
            case 12:
            case 14:
                //<editor-fold desc="single Line Prods">
                SyntaxNode c = curr.getChildren().get(0);
                if (c.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(c);
                    if (c.getData("type").equals("c"))
                        curr.addType("c");
                }
                break;
            //</editor-fold>
            case 3:
            case 5:
            case 8:
                //<editor-fold desc="two prod productions">
                SyntaxNode c1, c2;
                c1 = curr.getChildren().get(0);
                c2 = curr.getChildren().get(1);
                if (c1.getData("type").equals("c") && c2.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(c1);
                    typeCrawl(c2);
                    if (c2 != null) {
                        if (c1.getData("type").equals("c") && c2.getData("type").equals("c"))
                            curr.addType("c");
                    } else {
                        if (c1.getData("type").equals("c"))
                            curr.addType("c");
                    }
                }
                break;
            //</editor-fold>
            case 6:
                //<editor-fold desc="proc">
                SyntaxNode prog = curr.getChildren().get(2);
                if (prog.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(prog);
                    if (prog.getData("type").equals("c"))
                        curr.addType("c");
                }
                break;
            //</editor-fold>
            case 9:
            case 17:
                //<editor-fold desc="halt">
                curr.addType("c");
                break;
            //</editor-fold>
            case 15:
                //<editor-fold desc="input">
                SyntaxNode inputVar = curr.getChildren().get(1);
                String inputVarT = symTable.getTable().get(inputVar.getChildren().get(0).getData("internalName")).get("type");
                if (inputVarT.equals("s"))
                    curr.addType("e");
                else {
                    curr.addType("c");
                    inputVar.addType("n");
                    symTable.getTable().get(inputVar.getChildren().get(0).getData("internalName")).put("type", "n");
                }
                break;
            //</editor-fold>
            case 16:
                //<editor-fold desc="output">
                SyntaxNode outputVar = curr.getChildren().get(1);
                String outputVarT = symTable.getTable().get(outputVar.getChildren().get(0).getData("internalName")).get("type");
                if (outputVarT.equals("s")) {
                    outputVar.addType("c");
                    curr.addType("c");
                }else if (outputVarT.equals("n")) {
                    outputVar.addType("c");
                    curr.addType("c");
                } else {
                    outputVar.addType("o");
                    symTable.getTable().get(outputVar.getChildren().get(0).getData("internalName")).put("type", "o");
                    curr.addType("c");
                }
                break;
            //</editor-fold>
            case 18:
                //<editor-fold desc="Var name">
                String varNameT = symTable.getTable().get(curr.getChildren().get(0).getData("internalName")).get("type");
                if (varNameT.equals("u")) {
                    curr.addType("o");
                    symTable.getTable().get(curr.getChildren().get(0).getData("internalName")).put("type", "o");
                }else{
                    curr.addType(varNameT);
                }
                break;
            //</editor-fold>
            case 19:
                //<editor-fold desc="var = string">
                String aVarST = symTable.getTable().get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).get("type");
                if (aVarST.equals("n"))
                    curr.addType("e");
                else {
                    curr.addType("c");
                    curr.getChildren().get(0).addType("s");
                    symTable.getTable().get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).put("type", "s");
                }
                break;
            //</editor-fold>
            case 20:
                //<editor-fold desc="var = var">
                //VAR=VAR
                SyntaxNode aVar1, aVar2;
                String aVar1T, aVar2T;
                aVar1 = curr.getChildren().get(0);
                aVar2 = curr.getChildren().get(1);
                //get Var types from ST
                aVar1T = symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).get("type");
                aVar2T = symTable.getTable().get(aVar2.getChildren().get(0).getData("internalName")).get("type");
                if ((aVar1T.equals("n") && aVar2T.equals("s")) || (aVar2T.equals("n") && aVar1T.equals("s")))
                    curr.addType("e");
                else if ((aVar1T.equals("n") && !aVar2T.equals("s")) || (aVar2T.equals("n") && !aVar1T.equals("s"))) {
                    curr.addType("c");//assign vars to n
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                } else if ((!aVar1T.equals("n") && aVar2T.equals("s")) || (!aVar2T.equals("n") && aVar1T.equals("s"))) {
                    curr.addType("c");//assign vars to s
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "s");
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "s");
                } else {
                    curr.addType("c");//assign vars to o
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "o");
                    symTable.getTable().get(aVar1.getChildren().get(0).getData("internalName")).put("type", "o");
                }
                typeCrawl(aVar1);
                typeCrawl(aVar2);
                break;
            //</editor-fold>
            case 21:
                //<editor-fold desc="var = numexpr">
                SyntaxNode nVar1, nNum1;
                String nVar1T;
                nVar1 = curr.getChildren().get(0);
                nNum1 = curr.getChildren().get(1);
                nVar1T = symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).get("type");
                if (nVar1T.equals("s"))
                    curr.addType("e");
                if (!(nVar1T.equals("s")) && nNum1.getData("type").equals("n")) {
                    nVar1.addType("n");
                    symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    curr.addType("c");
                } else {
                    nVar1.addType("n");
                    symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    nVar1 = curr.getChildren().get(0);
                    nNum1 = curr.getChildren().get(1);
                    typeCrawl(nNum1);
                    if (nVar1T.equals("s"))
                        curr.addType("e");
                    if (!(nVar1T.equals("s")) && nNum1.getData("type").equals("n")) {
                        nVar1.addType("n");
                        symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                        curr.addType("c");
                    }
                }
                break;
            //</editor-fold>
            case 22:
                //<editor-fold desc="numexpr var">
                String numVarT = symTable.getTable().get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).get("type");
                if (numVarT.equals("s"))
                    curr.addType("e");
                else {
                    curr.addType("n");
                    symTable.getTable().get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).put("type", "n");
                }
                typeCrawl((curr.getChildren().get(0)));
                break;
            //</editor-fold>
            case 23:
                //<editor-fold desc="numexpr = num">
                curr.addType("n");
                break;
            //</editor-fold>
            case 24:
                //<editor-fold desc="NUM CALC">
                SyntaxNode Calc = curr.getChildren().get(0);
                if (Calc.getData("type").equals("n"))
                    curr.addType("n");
                else {
                    typeCrawl(Calc);
                    if (Calc.getData("type").equals("n"))
                        curr.addType("n");
                }
                break;
            //</editor-fold>
            case 25:
            case 27:
            case 26:
                //<editor-fold desc="calcs">
                SyntaxNode num1, num2;
                num1 = curr.getChildren().get(1);
                num2 = curr.getChildren().get(2);
                if (num1.getData("type").equals("n") && num2.getData("type").equals("n"))
                    curr.addType("n");
                else {
                    typeCrawl(num1);
                    typeCrawl(num2);
                    if (num1.getData("type").equals("n") && num2.getData("type").equals("n"))
                        curr.addType("n");
                }
                break;
            //</editor-fold>
            case 28:
                //<editor-fold desc="if then">
                SyntaxNode bool, code1;
                bool = curr.getChildren().get(1);
                code1 = curr.getChildren().get(3);
                if ((bool.getData("type").equals("b") || bool.getData("type").equals("f")) && code1.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(bool);
                    typeCrawl(code1);
                    if ((bool.getData("type").equals("b") || bool.getData("type").equals("f")) && code1.getData("type").equals("c"))
                        curr.addType("c");
                }
                break;
            //</editor-fold>
            case 29:
                //<editor-fold desc="if then else">
                SyntaxNode bool2, code21, code22;
                bool2 = curr.getChildren().get(1);
                code21 = curr.getChildren().get(3);
                code22 = curr.getChildren().get(5);
                if ((bool2.getData("type").equals("b") || bool2.getData("type").equals("f")) && code21.getData("type").equals("c") && code22.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(bool2);
                    typeCrawl(code21);
                    typeCrawl(code22);
                    if ((bool2.getData("type").equals("b") || bool2.getData("type").equals("f")) && code21.getData("type").equals("c") && code22.getData("type").equals("c"))
                        curr.addType("c");
                }
                break;
            //</editor-fold>
            case 30:
                //<editor-fold desc="while">
                SyntaxNode wBool, wCode;
                wBool = curr.getChildren().get(1);
                wCode = curr.getChildren().get(2);
                if ((wBool.getData("type").equals("b") || wBool.getData("type").equals("f")) && wCode.getData("type").equals("c"))
                    curr.addType("c");
                else {
                    typeCrawl(wBool);
                    typeCrawl(wCode);
                    if ((wBool.getData("type").equals("b") || wBool.getData("type").equals("f")) && wCode.getData("type").equals("c"))
                        curr.addType("c");
                }
                break;
            //</editor-fold>
            case 31:
                //<editor-fold desc="for loop">
                SyntaxNode fVar1, fVar2, fVar3, fVar4, fVar5, fCode;
                String fVar1T, fVar2T, fVar3T, fVar4T, fVar5T;
                fVar1 = curr.getChildren().get(1);
                fVar2 = curr.getChildren().get(2);
                fVar3 = curr.getChildren().get(3);
                fVar4 = curr.getChildren().get(4);
                fVar5 = curr.getChildren().get(5);
                fCode = curr.getChildren().get(6);
                typeCrawl(fVar1);
                typeCrawl(fVar2);
                typeCrawl(fVar3);
                typeCrawl(fVar4);
                typeCrawl(fVar5);
                //get var types from ST
                fVar1T = symTable.getTable().get(fVar1.getChildren().get(0).getData("internalName")).get("type");
                fVar2T = symTable.getTable().get(fVar2.getChildren().get(0).getData("internalName")).get("type");
                fVar3T = symTable.getTable().get(fVar3.getChildren().get(0).getData("internalName")).get("type");
                fVar4T = symTable.getTable().get(fVar4.getChildren().get(0).getData("internalName")).get("type");
                fVar5T = symTable.getTable().get(fVar5.getChildren().get(0).getData("internalName")).get("type");
                if (fVar1T.equals("s") || fVar2T.equals("s") ||
                        fVar3T.equals("s") || fVar4T.equals("s") ||
                        fVar5T.equals("s")) {
                    curr.addType("e");
                } else if (fCode.getData("type").equals("c")) {
                    curr.addType("c");
                    fVar1.addType("n");
                    fVar2.addType("n");
                    fVar3.addType("n");
                    fVar4.addType("n");
                    fVar5.addType("n");
                    symTable.getTable().get(fVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(fVar2.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(fVar3.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(fVar4.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(fVar5.getChildren().get(0).getData("internalName")).put("type", "n");
                } else {
                    typeCrawl(fCode);
                    if (fVar1T.equals("s") || fVar2T.equals("s") ||
                            fVar3T.equals("s") || fVar4T.equals("s") ||
                            fVar5T.equals("s")) {
                        curr.addType("e");
                    } else if (fCode.getData("type").equals("c")) {
                        curr.addType("c");
                        fVar1.addType("n");
                        fVar2.addType("n");
                        fVar3.addType("n");
                        fVar4.addType("n");
                        fVar5.addType("n");
                        symTable.getTable().get(fVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                        symTable.getTable().get(fVar2.getChildren().get(0).getData("internalName")).put("type", "n");
                        symTable.getTable().get(fVar3.getChildren().get(0).getData("internalName")).put("type", "n");
                        symTable.getTable().get(fVar4.getChildren().get(0).getData("internalName")).put("type", "n");
                        symTable.getTable().get(fVar5.getChildren().get(0).getData("internalName")).put("type", "n");
                    }
                }
                break;
            //</editor-fold>
            case 32:
                //<editor-fold desc="eq var var">
                SyntaxNode eVar1, eVar2;
                String eVar1T, eVar2T;
                eVar1 = curr.getChildren().get(1);
                eVar2 = curr.getChildren().get(2);
                eVar1T = "u";
                eVar2T = "u";
                //get types from ST
                if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName"))!=null) {
                    eVar1T = symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).get("type");
                }else if(eVar1.getData("symbol").equals("NUMEXPR")){
                    if (eVar1.getData("type").equals("n")) {
                        eVar1T = "n";
                    } else {
                        typeCrawl(eVar1);
                        eVar1T = eVar1.getData("type");
                    }
                }
                if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null) {
                    eVar2T = symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).get("type");
                }else if(eVar2.getData("symbol").equals("NUMEXPR")) {
                    if (eVar2.getData("type").equals("n")) {
                        eVar2T = "n";
                    } else {
                        typeCrawl(eVar2);
                        eVar2T = eVar2.getData("type");
                    }
                }else{
                    eVar2T = "u";
                }
                if ((eVar1T.equals("n") && eVar2T.equals("s")) || (eVar2T.equals("n") && eVar1T.equals("s"))){
                    curr.addType("f");
                    typeCrawl(eVar1);
                    typeCrawl(eVar2);
                }else if ((eVar1T.equals("n") && !eVar2T.equals("s")) || (eVar2T.equals("n") && !eVar1T.equals("s"))){
                    curr.addType("b");//assign vars to n
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "n");
                    typeCrawl(eVar1);
                    typeCrawl(eVar2);
                }else if((!eVar1T.equals("n")&&eVar2T.equals("s"))||(!eVar2T.equals("n")&&eVar1T.equals("s"))) {
                    curr.addType("b");//assign vars to s
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "s");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "s");
                    typeCrawl(eVar1);
                    typeCrawl(eVar2);
            }   else{
                    curr.addType("b");//assign vars to o
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "o");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "o");
                    typeCrawl(eVar1);
                    typeCrawl(eVar2);
                }
                break;
                //</editor-fold>
            case 33:
                //<editor-fold desc="eq BOOL">
                SyntaxNode bBool1, bBool2;
                bBool1 = curr.getChildren().get(1);
                bBool2 = curr.getChildren().get(2);
                if((bBool1.getData("type").equals("b")||bBool1.getData("type").equals("f"))&&(bBool2.getData("type").equals("b")||bBool2.getData("type").equals("f")))
                    curr.addType("b");
                else{
                    typeCrawl(bBool1);
                    typeCrawl(bBool2);
                    if((bBool1.getData("type").equals("b")||bBool1.getData("type").equals("f"))&&(bBool2.getData("type").equals("b")||bBool2.getData("type").equals("f")))
                        curr.addType("b");
                }
                break;
                //</editor-fold>
            case 34:
                //<editor-fold desc="eq NUMS">
                SyntaxNode bNum1, bNum2;
                bNum1 = curr.getChildren().get(1);
                bNum2 = curr.getChildren().get(2);
                if((bNum1.getData("type").equals("n"))&&(bNum2.getData("type").equals("n")))
                    curr.addType("b");
                else {
                    typeCrawl(bNum1);
                    typeCrawl(bNum2);
                    if((bNum1.getData("type").equals("n"))&&(bNum2.getData("type").equals("n")))
                        curr.addType("b");
                }
                break;
            //</editor-fold>
            case 35:
            case 36:
                //<editor-fold desc="Var comp">
                SyntaxNode bVar1, bVar2;
                String bVar1T, bVar2T;
                bVar1 = curr.getChildren().get(0);
                bVar2 = curr.getChildren().get(2);
                bVar1T = symTable.getTable().get(bVar1.getChildren().get(0).getData("internalName")).get("type");
                bVar2T = symTable.getTable().get(bVar2.getChildren().get(0).getData("internalName")).get("type");
                if(bVar1T.equals("s")||bVar2T.equals("s")){
                    curr.addType("e");
                }else{
                    symTable.getTable().get(bVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    symTable.getTable().get(bVar2.getChildren().get(0).getData("internalName")).put("type", "n");
                    bVar1.addType("n");
                    bVar2.addType("n");
                    curr.addType("b");
                }
                break;
                //</editor-fold>
            case 37:
                //<editor-fold desc="not">
                SyntaxNode nBool = curr.getChildren().get(1);
                if(nBool.getData("type").equals("b")||nBool.getData("type").equals("f")) {
                    curr.addType("b");
                }
                else {
                    typeCrawl(nBool);
                    if(nBool.getData("type").equals("b")||nBool.getData("type").equals("f")) {
                        curr.addType("b");
                    }
                }
                break;
                //</editor-fold>
            case 38:
                //<editor-fold desc="or">
                SyntaxNode oBool1, oBool2;
                oBool1 = curr.getChildren().get(1);
                oBool2 = curr.getChildren().get(2);
                if((oBool1.getData("type").equals("b")||oBool1.getData("type").equals("f"))&&(oBool2.getData("type").equals("b")||oBool2.getData("type").equals("f")))
                    curr.addType("b");
                else{
                    typeCrawl(oBool1);
                    typeCrawl(oBool2);
                    if((oBool1.getData("type").equals("b")||oBool1.getData("type").equals("f"))&&(oBool2.getData("type").equals("b")||oBool2.getData("type").equals("f")))
                        curr.addType("b");
                }
                break;
                //</editor-fold>
            case 39:
                //<editor-fold desc="and">
                SyntaxNode aBool1, aBool2;
                aBool1 = curr.getChildren().get(1);
                aBool2 = curr.getChildren().get(2);
                if(aBool1.getData("type").equals("f")&&aBool2.getData("type").equals("b")){
                    curr.addType("f");
                }else if(aBool1.getData("type").equals("b")&&aBool2.getData("type").equals("f")){
                    curr.addType("f");
                }else if(aBool1.getData("type").equals("f")&&aBool2.getData("type").equals("f")){
                    curr.addType("f");
                }else if(aBool1.getData("type").equals("b")&&aBool2.getData("type").equals("b")){
                    curr.addType("b");
                }else{
                    typeCrawl(aBool1);
                    typeCrawl(aBool2);
                    if(aBool1.getData("type").equals("f")&&aBool2.getData("type").equals("b")){
                        curr.addType("f");
                    }else if(aBool1.getData("type").equals("b")&&aBool2.getData("type").equals("f")){
                        curr.addType("f");
                    }else if(aBool1.getData("type").equals("f")&&aBool2.getData("type").equals("f")){
                        curr.addType("f");
                    }else if(aBool1.getData("type").equals("b")&&aBool2.getData("type").equals("b")) {
                        curr.addType("b");
                    }
                }
                break;
                //</editor-fold>
        }
    }
    private int identifyProd(SyntaxNode curr){
        switch (curr.getData("symbol")) {
            case "PROGPRIME":
                return 1;
            case "PROG":
                Vector<SyntaxNode> rHandProd = curr.getChildren();
                if (rHandProd.size() > 1 && rHandProd.get(1).getData("symbol").equals("PROC_DEFS"))
                    return 3;
                return 2;
            case "PROC_DEFS":
                if(curr.getChildren().size() == 1)
                    return 4;
                return 5;
            case "PROC":
                return 6;
            case "CODE":
                if(curr.getChildren().size() == 1)
                    return 7;
                return 8;
            case "INSTR":
                SyntaxNode child = curr.getChildren().get(0);
                switch (child.getData("symbol")){
                    case "halt":
                        return 9;
                    case "IO":
                        return 10;
                    case "CALL":
                        return 11;
                    case "ASSIGN":
                        return 12;
                    case "BRANCH":
                        return 13;
                    case "LOOP":
                        return 14;
                }
            case "IO":
                if(curr.getChildren().get(0).getData("symbol").equals("input"))
                    return 15;
                return 16;
            case "CALL":
                return 17;
            case "VAR":
                return 18;
            case "ASSIGN":
                if(curr.getChildren().get(1).getData("symbol").equals("VAR"))
                    return 20;
                else if(curr.getChildren().get(1).getData("symbol").equals("NUMEXPR"))
                    return 21;
                return 19;
            case "NUMEXPR":
                if(curr.getChildren().get(0).getData("symbol").equals("VAR"))
                    return 22;
                else if(curr.getChildren().get(0).getData("symbol").equals("CALC"))
                    return 24;
                return 23;
            case "CALC":
                if(curr.getChildren().get(0).getData("symbol").equals("add"))
                    return 25;
                else if(curr.getChildren().get(0).getData("symbol").equals("sub"))
                    return 26;
                return 27;
            case "BRANCH":
                if(curr.getChildren().size() == 4)
                    return 28;
                return 29;
            case "LOOP":
                if(curr.getChildren().get(0).getData("symbol").equals("while"))
                    return 30;
                return 31;
            case "BOOL":
                switch (curr.getChildren().get(0).getData("symbol")) {
                    case "eq":
                        switch (curr.getChildren().get(1).getData("symbol")) {
                            case "VAR":
                                return 32;
                            case "BOOL":
                                return 33;
                            case "NUMEXPR":
                                return 34;
                        }
                        break;
                    case "VAR":
                        if (curr.getChildren().get(1).getData("symbol").equals("<"))
                            return 35;
                        return 36;
                    case "not":
                        return 37;
                    case "or":
                        return 38;
                    default:
                        return 39;
                }
        }
        return -1;
    }
    private void initTypes(SyntaxNode curr){
        if(curr != null){
            curr.addType("u");
            for(SyntaxNode c: curr.getChildren()){
                initTypes(c);
            }
        }
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
                SyntaxNode tempV1, tempV2, tempV3, tempV4, tempV5;
                tempV1 = curr.getChildren().elementAt(1).getChildren().elementAt(0);
                tempV2 = curr.getChildren().elementAt(2).getChildren().elementAt(0);
                tempV3 = curr.getChildren().elementAt(3).getChildren().elementAt(0);
                tempV4 = curr.getChildren().elementAt(4).getChildren().elementAt(0);
                tempV5 = curr.getChildren().elementAt(5).getChildren().elementAt(0);
                if(!(tempV1.getData("internalName").equals(tempV2.getData("internalName")))||
                        !(tempV1.getData("internalName").equals(tempV4.getData("internalName")))||
                        !(tempV1.getData("internalName").equals(tempV5.getData("internalName")))){
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
    private void procRename(SyntaxNode curr, SyntaxNode AncestorProg, SyntaxNode currProc){
        if(curr != null){
            switch (curr.getData("symbol")) {
                case "PROG":
                    for (SyntaxNode c : curr.getChildren()) {
                        procRename(c, curr, currProc);
                    }
                    break;
                case "PROC":
                    for (SyntaxNode c : curr.getChildren()) {
                        procRename(c, AncestorProg, curr);
                    }
                    break;
                case "CALL":
                    //check rules and rename matching func
                    String procname = curr.getChildren().elementAt(0).getData("symbol");

                    if (currProc != null && currProc.getChildren().elementAt(1).getData("symbol").equals(procname)) {
                        //recursive call matched

                        if (currProc.getChildren().elementAt(1).getData("internalName") != null) {
                            curr.getChildren().elementAt(0).addInternalName(currProc.getChildren().elementAt(1).getData("internalName"));
                        }
                    } else {
                        //check prog procs for match
                        if (!(AncestorProg.getChildren().size() > 1)){
                            treeRoot.error = true;
                            treeRoot.errMessage = "Invalid call to procedure";
                        } else {
                            SyntaxNode ProcNode = AncestorProg.getChildren().elementAt(1);
                            SyntaxNode calledProc = curr.getChildren().elementAt(0);
                            if (ProcNode != null) {
                                for (SyntaxNode c : ProcNode.getChildren()) {
                                    if (c.getData("symbol").equals("PROC")) {
                                        SyntaxNode tempProc = c.getChildren().elementAt(1);
                                        if (tempProc.getData("symbol").equals(calledProc.getData("symbol"))) {
                                            if (tempProc.getData("internalName") == null) {
                                                tempProc.addInternalName("p" + nextProcName++);
                                            }
                                            calledProc.addInternalName(tempProc.getData("internalName"));
                                        }
                                    }
                                }
                                if (calledProc.getData("internalName") == null) {
                                    if (!treeRoot.error) {
                                        treeRoot.error = true;
                                        treeRoot.errMessage = "Illegal call to procedure: " + calledProc.getData("symbol");
                                    }
                                }
                            } else {
                                if (!treeRoot.error) {
                                    treeRoot.error = true;
                                    treeRoot.errMessage = "Procedure without definition called";
                                }
                            }
                        }
                    }
                    for (SyntaxNode c : curr.getChildren()) {
                        procRename(c, AncestorProg, currProc);
                    }
                    break;
                default:
                    //recur for all children
                    for (SyntaxNode c : curr.getChildren()) {
                        procRename(c, AncestorProg, currProc);
                    }
                    break;
            }
        }
    }
    private void treePrune(SyntaxNode curr){
        if(curr!=null) {
            if (curr.getData("symbol").equals("PROC_DEFS")) {
                //check children internal names
                Vector<SyntaxNode> children = curr.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    SyntaxNode terminal = children.elementAt(i).getChildren().get(1);
                    if (terminal.getData("internalName") == null) {
                        treeRoot.warn = true;
                        treeRoot.warnMessage += "WARNING:Procedure " + terminal.getData("symbol") +" declared but never used\n";
                        curr.getChildren().remove(i);
                    }
                }
            }

            for (SyntaxNode c : curr.getChildren()) {
                treePrune(c);
            }
            if (curr.getData("symbol").equals("PROG")) {
                if (curr.getChildren().size()>1&&curr.getChildren().elementAt(1) != null && curr.getChildren().elementAt(1).getChildren().isEmpty()) {
                    curr.getChildren().remove(1);
                }
            }
        }
    }
}
