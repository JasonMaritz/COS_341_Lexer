package CompilerStages;

import Nodes.SymbolTable;
import Nodes.SyntaxNode;

import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;

public class TreeCrawler {
    SyntaxNode treeRoot;
    Vector<String> usedScopes;
    SymbolTable symTable = new SymbolTable();
    HashMap<String, Integer> labelMap = new HashMap<>();
    int nextScope=1;
    int nextVarName = 1;
    int nextProcName = 0;
    int nextLineNum = 10;
    int nextLabel = 1;
    int nextTVar = 1;

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
        HashMap<String, HashMap<String, String>> syms = new HashMap<>();
        while (!symTable.getTable().equals(syms)) {
            syms = clone(symTable);
            typeCrawl(treeRoot,false);
        }
    }
    public boolean errorOut(){
        boolean ret = false;
        if(!treeRoot.error) {
            treeRoot.errMessage = "";
            ret = errorOut(treeRoot);
            treeRoot.error = ret;
        }
        return ret;
    }
    public void deadCodeCrawl(){
        deadCodeCrawl(treeRoot);
        deadCodePrune(treeRoot);
    }
    public void valueCrawl(){
        HashMap<String, String> varValues = new HashMap<>();
        HashMap<String, SyntaxNode> procNodes = new HashMap<>();
        populateMaps(treeRoot, varValues, procNodes);
        valueCrawl(treeRoot, varValues, procNodes);
    }
    public boolean valueError(){
        return treeRoot.error;
    }

    public String translate(){
        return replaceLabels(translate(treeRoot, false));
    }
    private String translate(SyntaxNode curr, boolean procedureProgram){
        String translatedText = "";
        HashMap<String, HashMap<String, String>> syms = symTable.getTable();

        int production = identifyProd(curr);
        String varName;
        switch (production){
            case 1:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 7:
            case 4:
                translatedText += translate(curr.getChildren().get(0), procedureProgram);
                    break;
            case 2:
                translatedText +=translate(curr.getChildren().get(0), procedureProgram);
                if(procedureProgram){
                    translatedText += nextLineNum + " RETURN\n";
                }else{
                    translatedText += nextLineNum + " END\n";
                }
                nextLineNum += 10;
                break;
            case 3:
                translatedText += translate(curr.getChildren().get(0), procedureProgram);
                if(procedureProgram){
                    translatedText += nextLineNum + " RETURN\n";
                }else{
                    translatedText += nextLineNum + " END\n";
                }
                nextLineNum += 10;
                translatedText += translate(curr.getChildren().get(1), true);
                break;
            case 5:
            case 8:
                translatedText += translate(curr.getChildren().get(0), procedureProgram);
                translatedText += translate(curr.getChildren().get(1), procedureProgram);
                break;
            case 6:
                syms.get(curr.getChildren().get(1).getData("internalName")).put("labelLocation", Integer.toString(nextLineNum));
                translatedText += translate(curr.getChildren().get(2), procedureProgram);
                break;
            case 9:
                translatedText += nextLineNum +  " STOP\n";
                nextLineNum += 10;
                break;
            case 15:
                varName = curr.getChildren().get(1).getChildren().get(0).getData("internalName");
                if(syms.get(varName).get("type").equals("s"))
                    varName+="$";
                varName += "\n";
                translatedText += nextLineNum +  " INPUT \"\"; " + varName;
                nextLineNum += 10;
                break;
            case 16:
                varName = curr.getChildren().get(1).getChildren().get(0).getData("internalName");
                if(syms.get(varName).get("type").equals("s"))
                    varName+="$";
                varName += "\n";
                translatedText += nextLineNum + " PRINT "+varName;
                nextLineNum += 10;
                break;
            case 17:
                translatedText += nextLineNum + " GOSUB " + "[" + curr.getChildren().get(0).getData("internalName") + "]\n" ;
                nextLineNum += 10;
                break;
            case 18:
                translatedText += curr.getChildren().get(0).getData("internalName");
                break;
            case 19:
                translatedText += nextLineNum + " LET ";
                translatedText += curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                translatedText += "$";
                translatedText += " = ";
                translatedText += curr.getChildren().get(1).getData("symbol");
                translatedText += "\n";
                nextLineNum += 10;
                break;
            case 20:
                translatedText += nextLineNum + " LET ";
                varName = curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                if(syms.get(varName).get("type").equals("s"))
                    varName += "$";
                translatedText += varName;
                translatedText += " = ";
                varName = curr.getChildren().get(1).getChildren().get(0).getData("internalName");
                if(syms.get(varName).get("type").equals("s"))
                    varName += "$";
                translatedText += varName;
                translatedText += "\n";
                nextLineNum += 10;
                break;
            case 21:
                /*translatedText += nextLineNum + " LET ";
                varName = curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                translatedText += varName;
                translatedText += " = ";*/
                translatedText += translateArithmetic(curr.getChildren().get(1), curr.getChildren().get(0).getChildren().get(0).getData("internalName"));
                /*translatedText += "\n";
                nextLineNum += 10;*/
                break;
            case 28:
                String itLabel1 = String.format("[%d]", nextLabel++);
                String itLabel2 = String.format("[%d]", nextLabel++);
                /*translatedText += nextLineNum + " IF ";
                translatedText += translate(curr.getChildren().get(1), procedureProgram);
                translatedText += " THEN " + itLabel1 + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " GOTO " + itLabel2 + "\n";
                nextLineNum += 10;*/
                translatedText += translateBool(curr.getChildren().get(1), itLabel1, itLabel2);
                labelMap.put(itLabel1, nextLineNum);
                translatedText += translate(curr.getChildren().get(3), procedureProgram);
                labelMap.put(itLabel2, nextLineNum);
                break;
            case 29:
                String iteLabel1 = String.format("[%d]", nextLabel++);
                String iteLabel2 = String.format("[%d]", nextLabel++);
                String iteLabel3 = String.format("[%d]", nextLabel++);
                /*translatedText += nextLineNum + " IF ";
                translatedText += translate(curr.getChildren().get(1), procedureProgram);
                translatedText += " THEN " + iteLabel1 + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " GOTO " + iteLabel2 + "\n";
                nextLineNum += 10;*/
                translatedText += translateBool(curr.getChildren().get(1), iteLabel1, iteLabel2);
                labelMap.put(iteLabel1, nextLineNum);
                translatedText += translate(curr.getChildren().get(3), procedureProgram);
                translatedText += nextLineNum + " GOTO " + iteLabel3 + "\n";
                nextLineNum += 10;
                labelMap.put(iteLabel2, nextLineNum);
                translatedText += translate(curr.getChildren().get(5), procedureProgram);
                labelMap.put(iteLabel3, nextLineNum);
                break;
            case 30:
                //while
                String wLabel1 = String.format("[%d]", nextLabel++);
                String wLabel2 = String.format("[%d]", nextLabel++);
                String wLabel3 = String.format("[%d]", nextLabel++);
                labelMap.put(wLabel1, nextLineNum);
                /*translatedText += nextLineNum + " IF ";
                translatedText += translate(curr.getChildren().get(1), procedureProgram);
                translatedText += " THEN " + wLabel2 + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " GOTO " + wLabel3 + "\n";
                nextLineNum += 10;*/
                translatedText += translateBool(curr.getChildren().get(1), wLabel2, wLabel3);
                labelMap.put(wLabel2, nextLineNum);
                translatedText += translate(curr.getChildren().get(2), procedureProgram);
                translatedText += nextLineNum + " GOTO " + wLabel1 + "\n";
                nextLineNum += 10;
                labelMap.put(wLabel3, nextLineNum);
                break;
            case 31:
                //for
                String fLabel1 = String.format("[%d]", nextLabel++);
                String fLabel2 = String.format("[%d]", nextLabel++);
                String fLabel3 = String.format("[%d]", nextLabel++);
                String fCodeName = curr.getChildren().get(1).getChildren().get(0).getData("internalName");
                translatedText += nextLineNum + " LET " + fCodeName;
                translatedText += " = 0\n";
                nextLineNum += 10;
                labelMap.put(fLabel1, nextLineNum);
                translatedText += nextLineNum + " IF " + fCodeName;
                translatedText += " < " + curr.getChildren().get(3).getChildren().get(0).getData("internalName");
                translatedText += " THEN " + fLabel2 + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " GOTO " + fLabel3 + "\n";
                nextLineNum += 10;
                labelMap.put(fLabel2, nextLineNum);
                translatedText += translate(curr.getChildren().get(6), procedureProgram);

                String fAStore1 = String.format("T%d", nextTVar++);
                String fAStore2 = String.format("T%d", nextTVar++);
                String fAStore3 = String.format("T%d", nextTVar++);
                translatedText += nextLineNum + " LET " + fAStore2 + " = " + fCodeName + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " LET " + fAStore3 + " = 1\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " LET " + fAStore1 + " = " + fAStore2 + " + " + fAStore3 + "\n";
                nextLineNum += 10;
                translatedText += nextLineNum + " LET " + fCodeName + " = " + fAStore1 + "\n";
                nextLineNum += 10;

                translatedText += nextLineNum + " GOTO " + fLabel1 + "\n";
                nextLineNum += 10;
                labelMap.put(fLabel3, nextLineNum);
                break;
        }

        return translatedText;
    }
    private String replaceLabels(String intermediateTranslation){
        String[] lines = intermediateTranslation.split("\n");
        for(int i = 0; i < lines.length; i++){
            String s = lines[i];
            if(s.contains("[")){
                String temp = s.substring(0, s.indexOf("["));
                String labelName = s.substring(s.indexOf("[")+1, s.length()-1);
                if(labelName.charAt(0) == 'p'){
                    temp += symTable.getTable().get(labelName).get("labelLocation");
                }else{
                    temp += labelMap.get("["+labelName+"]");
                }
                s = temp;
            }
            lines[i] = s;
        }
        String returnString = "";
        for(String s: lines){
            returnString += s + "\n";
        }
        return returnString;
    }
    private String translateBool(SyntaxNode curr, String trueLabel, String falseLabel){
        String translatedCondition = "";
        switch (identifyProd(curr)){
            case 32:
                translatedCondition += nextLineNum + " IF (" + curr.getChildren().get(1).getChildren().get(0).getData("internalName");
                translatedCondition += " = ";
                if(curr.getChildren().get(2).getChildren().get(0).getData("internalName")!=null)
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("internalName");
                else
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("symbol");
                translatedCondition += ") THEN " + trueLabel + "\n";
                nextLineNum += 10;
                translatedCondition += nextLineNum + " GOTO " + falseLabel + "\n";
                nextLineNum += 10;
                break;
            case 33:
                //bool bool
                String eLabel1 = String.format("[%d]", nextLabel++);
                String eLabel2 = String.format("[%d]", nextLabel++);
                translatedCondition += translateBool(curr.getChildren().get(1), eLabel1, eLabel2);
                labelMap.put(eLabel1, nextLineNum);
                translatedCondition += translateBool(curr.getChildren().get(2), trueLabel, falseLabel);
                labelMap.put(eLabel2, nextLineNum);
                translatedCondition += translateBool(curr.getChildren().get(2), falseLabel, trueLabel);
            case 34:
                //num num
                String nVar1 = String.format("T%d", nextTVar++);
                String nVar2 = String.format("T%d", nextTVar++);
                translatedCondition += translateArithmetic(curr.getChildren().get(1), nVar1);
                translatedCondition += translateArithmetic(curr.getChildren().get(2), nVar2);
                translatedCondition += nextLineNum + " IF ("+ nVar1;
                translatedCondition += " = " + nVar2;
                translatedCondition += ") THEN " + trueLabel + "\n";
                nextLineNum += 10;
                translatedCondition += nextLineNum + " GOTO " + falseLabel + "\n";
                nextLineNum += 10;
                break;
            case 35:
                translatedCondition += nextLineNum + " IF (" + curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                translatedCondition += " < ";
                if(curr.getChildren().get(2).getChildren().get(0).getData("internalName")!=null)
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("internalName");
                else
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("symbol");
                translatedCondition += ") THEN " + trueLabel + "\n";
                nextLineNum += 10;
                translatedCondition += nextLineNum + " GOTO " + falseLabel + "\n";
                nextLineNum += 10;
                break;
            case 36:
                translatedCondition += nextLineNum + " IF (" + curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                translatedCondition += " > ";
                if(curr.getChildren().get(2).getChildren().get(0).getData("internalName")!=null)
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("internalName");
                else
                    translatedCondition += curr.getChildren().get(2).getChildren().get(0).getData("symbol");
                translatedCondition += ") THEN " + trueLabel + "\n";
                nextLineNum += 10;
                translatedCondition += nextLineNum + " GOTO " + falseLabel + "\n";
                nextLineNum += 10;
                break;
            case 37:
                // not
                translatedCondition += translateBool(curr.getChildren().get(1), falseLabel, trueLabel);
                break;
            case 38:
                // or
                String oLabel = String.format("[%d]", nextLabel++);
                String oCode1 = translateBool(curr.getChildren().get(1), trueLabel, oLabel);
                labelMap.put(oLabel, nextLineNum);
                String oCode2 = translateBool(curr.getChildren().get(2), trueLabel, falseLabel);
                translatedCondition += oCode1 + oCode2;
                break;
            case 39:
                // and
                String aLabel = String.format("[%d]", nextLabel++);
                String aCode1 = translateBool(curr.getChildren().get(1), aLabel, falseLabel);
                labelMap.put(aLabel, nextLineNum);
                String aCode2 = translateBool(curr.getChildren().get(2), trueLabel, falseLabel);
                translatedCondition += aCode1 + aCode2;
                break;
        }
        return  translatedCondition;
    }
    private String translateArithmetic(SyntaxNode curr, String storeName){
        String translatedSum = "";
        int production = identifyProd(curr);
        switch (production){
            case 22:
                //translatedText += curr.getChildren().get(0).getChildren().get(0).getData("internalName");
                translatedSum += nextLineNum + " LET " + storeName + " = " + curr.getChildren().get(0).getChildren().get(0).getData("internalName") + "\n";
                nextLineNum += 10;
                break;
            case 23:
                //translatedText += curr.getChildren().get(0).getData("symbol");
                translatedSum += nextLineNum + " LET " + storeName + " = " + curr.getChildren().get(0).getData("symbol") + "\n";
                nextLineNum += 10;
                break;
            case 24:
                /*String sVar1 = String.format("T%d", nextTVar++);
                String sVar2 = String.format("T%d", nextTVar++);
                translatedText += translateArithmetic(curr.getChildren().get(0), sVar1, sVar2);*/
                //String sVar1 = String.format("T%d", nextTVar++);
                //String sVar2 = String.format("T%d", nextTVar++);
                //translatedSum += ;
                String sVar1 = String.format("T%d", nextTVar++);
                translatedSum += translateArithmetic(curr.getChildren().get(0), sVar1);
                translatedSum += nextLineNum + " LET " + storeName + " = " + sVar1 + "\n";
                nextLineNum += 10;
                break;
            case 25:
                /*translatedSum += "(";
                translatedSum += translate(curr.getChildren().get(1), false);
                translatedSum += " + ";
                translatedSum += translate(curr.getChildren().get(2), false);
                translatedSum += ")";*/
                String aStore1 = String.format("T%d", nextTVar++);
                String aStore2 = String.format("T%d", nextTVar++);
                translatedSum += translateArithmetic(curr.getChildren().get(1), aStore1);
                translatedSum += translateArithmetic(curr.getChildren().get(2), aStore2);
                translatedSum += nextLineNum + " LET " + storeName + " = " + aStore1 + " + " + aStore2 + "\n";
                nextLineNum += 10;
                break;
            case 26:
                /*translatedSum += "(";
                translatedSum += translate(curr.getChildren().get(1), false);
                translatedSum += " - ";
                translatedSum += translate(curr.getChildren().get(2), false);
                translatedSum += ")";*/
                String sStore1 = String.format("T%d", nextTVar++);
                String sStore2 = String.format("T%d", nextTVar++);
                translatedSum += translateArithmetic(curr.getChildren().get(1), sStore1);
                translatedSum += translateArithmetic(curr.getChildren().get(2), sStore2);
                translatedSum += nextLineNum + " LET " + storeName + " = " + sStore1 + " - " + sStore2 + "\n";
                nextLineNum += 10;
                break;
            case 27:
                /*translatedSum += "(";
                translatedSum += translate(curr.getChildren().get(1), false);
                translatedSum += " * ";
                translatedSum += translate(curr.getChildren().get(2), false);
                translatedSum += ")";*/
                String mStore1 = String.format("T%d", nextTVar++);
                String mStore2 = String.format("T%d", nextTVar++);
                translatedSum += translateArithmetic(curr.getChildren().get(1), mStore1);
                translatedSum += translateArithmetic(curr.getChildren().get(2), mStore2);
                translatedSum += nextLineNum + " LET " + storeName + " = " + mStore1 + " * " + mStore2 + "\n";
                nextLineNum += 10;
                break;
        }
        return translatedSum;
    }

    private Vector<String> valueCrawl(SyntaxNode curr, HashMap<String, String> vars, HashMap<String, SyntaxNode> procs){
        if(curr == null || curr.getNodeType().name().equals(SyntaxNode.type.TERMINAL.name()))
            return new Vector<>();
        Vector<String> ret = new Vector<>();
        int production = identifyProd(curr);
        switch (production){
            case 1:
            case 2:
            case 3:
            case 4:
            case 7:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 24:
                for(SyntaxNode c: curr.getChildren()) {
                    ret.addAll(valueCrawl(c, vars, procs));
                }
                boolean toggle = true;
                for(SyntaxNode c: curr.getChildren()) {
                    if (!c.getData("value").equals("+"))
                        toggle = false;
                }
                if(toggle)
                    curr.addValue("+");
                break;
            case 5:
            case 8:
                ret.addAll(valueCrawl(curr.getChildren().get(0), vars, procs));
                ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                if(curr.getChildren().get(0).getData("value").equals("+")&&
                        curr.getChildren().get(1).getData("value").equals("+"))
                    curr.addValue("+");
                break;
                case 6:
                    ret.addAll(valueCrawl(curr.getChildren().get(2), vars, procs));
                    if(curr.getChildren().get(2).getData("value").equals("+"))
                        curr.addValue("+");
                    break;
            case 9:
            case 23:
                curr.addValue("+");
                break;
            case 15:
                    curr.getChildren().get(1).addValue("+");
                    vars.put(curr.getChildren().get(1).getChildren().get(0).getData("internalName"), "+");
                    ret.add(curr.getChildren().get(1).getChildren().get(0).getData("internalName"));
                    curr.addValue("+");
                    break;
                case 16:
                    if(vars.get(curr.getChildren().get(1).getChildren().get(0).getData("internalName")).equals("+")) {
                        curr.addValue("+");
                        curr.getChildren().get(1).addValue("+");
                    }else{
                        treeRoot.error = true;
                        treeRoot.errMessage += "Trying to output a variable without a value\n";
                    }
                    break;
                case 17:
                    ret.addAll(valueCrawl(procs.get(curr.getChildren().get(0).getData("internalName")), vars, procs));
                    if(procs.get(curr.getChildren().get(0).getData("internalName")).getData("value").equals("+"))
                        curr.addValue("+");
                    break;
                case 18:
                    break;
                case 19:
                    curr.getChildren().get(0).addValue("+");
                    vars.put(curr.getChildren().get(0).getChildren().get(0).getData("internalName"), "+");
                    ret.add(curr.getChildren().get(0).getChildren().get(0).getData("internalName"));
                    curr.addValue("+");
                    break;
                case 20:
                    if(vars.get(curr.getChildren().get(1).getChildren().get(0).getData("internalName"))!=null &&
                        vars.get(curr.getChildren().get(1).getChildren().get(0).getData("internalName")).equals("+")){
                        curr.getChildren().get(0).addValue("+");
                        curr.getChildren().get(1).addValue("+");
                        vars.put(curr.getChildren().get(0).getChildren().get(0).getData("internalName"), "+");
                        vars.put(curr.getChildren().get(1).getChildren().get(0).getData("internalName"), "+");
                        ret.add(curr.getChildren().get(1).getChildren().get(0).getData("internalName"));
                        ret.add(curr.getChildren().get(0).getChildren().get(0).getData("internalName"));
                        curr.addValue("+");
                    }else{
                        treeRoot.error = true;
                        treeRoot.errMessage += "Trying to assign a variable to another variable without a value\n";
                    }
                    break;
                case 21:
                    ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                    if(curr.getChildren().get(1).getData("value").equals("+")){
                        curr.getChildren().get(0).addValue("+");
                        ret.add(curr.getChildren().get(0).getChildren().get(0).getData("internalName"));
                        vars.put(curr.getChildren().get(0).getChildren().get(0).getData("internalName"), "+");
                        curr.addValue("+");
                    }else{
                        treeRoot.error = true;
                        treeRoot.errMessage += "Trying to assign a value-less calculation to a variable\n";
                    }
                    break;
                case 22:
                    if(vars.get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).equals("+")) {
                        curr.addValue("+");
                        curr.getChildren().get(0).addValue("+");
                    }
                    break;
                case 25:
            case 26:
            case 27:
                ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                ret.addAll(valueCrawl(curr.getChildren().get(2), vars, procs));
                if(curr.getChildren().get(1).getData("value").equals("+")&&curr.getChildren().get(2).getData("value").equals("+")){
                    curr.addValue("+");
                }
                break;
            case 28:
            case 30:
                ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                HashMap<String, String> varsTemp = (HashMap<String, String>) vars.clone();
                ret.addAll(valueCrawl(curr.getChildren().get(2), varsTemp, procs));
                if(curr.getChildren().get(2).getData("value").equals("+"))
                    curr.addValue("+");
                break;
                case 29:
                    //if-then-else---------------------------------------------------------------------------------------
                    ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                    Vector<String> thenCode, elseCode;
                    varsTemp = (HashMap<String, String>) vars.clone();
                    thenCode = valueCrawl(curr.getChildren().get(3), varsTemp, procs);
                    varsTemp = (HashMap<String, String>) vars.clone();
                    elseCode = valueCrawl(curr.getChildren().get(5), varsTemp, procs);
                    if(curr.getChildren().get(1).getData("value").equals("+")&&
                            curr.getChildren().get(3).getData("value").equals("+")&&
                            curr.getChildren().get(5).getData("value").equals("+")){
                        curr.addValue("+");
                    }
                    thenCode.removeIf((n)->!elseCode.contains(n));
                    for(String s: thenCode){
                        vars.put(s, "+");
                        ret.add(s);
                    }
                    break;
            case 31:
                    //for loop
                SyntaxNode var1, var2;
                var1 = curr.getChildren().get(1).getChildren().get(0);
                var2 = curr.getChildren().get(3).getChildren().get(0);
                vars.put(var1.getData("internalName"), "+");
                curr.getChildren().get(1).addValue("+");
                curr.getChildren().get(2).addValue("+");
                curr.getChildren().get(4).addValue("+");
                curr.getChildren().get(5).addValue("+");
                ret.addAll(valueCrawl(curr.getChildren().get(6), vars, procs));
                if(vars.get(var1.getData("internalName")).equals("+")&&
                        vars.get(var2.getData("internalName")).equals("+")&&
                        curr.getChildren().get(6).getData("value").equals("+")){
                    curr.getChildren().get(3).addValue("+");
                    curr.addValue("+");
                }
                break;
                case 32:
                    //rest are booleans
                    if (vars.get(curr.getChildren().get(2).getChildren().get(0).getData("internalName")) != null) {
                        if (vars.get(curr.getChildren().get(1).getChildren().get(0).getData("internalName")).equals("+") &&
                                vars.get(curr.getChildren().get(2).getChildren().get(0).getData("internalName")).equals("+")) {
                            curr.getChildren().get(1).addValue("+");
                            curr.getChildren().get(2).addValue("+");
                            curr.addValue("+");
                        }
                    }else{
                        if (vars.get(curr.getChildren().get(1).getChildren().get(0).getData("internalName")).equals("+") &&
                                curr.getChildren().get(2).getChildren().get(0).getData("symbol").equals("NUMEXPR")) {
                            curr.getChildren().get(1).addValue("+");
                            curr.getChildren().get(2).addValue("+");
                            curr.addValue("+");
                        }
                    }
                    break;
                case 33:
            case 34:
            case 38:
            case 39:
                ret.addAll(valueCrawl(curr.getChildren().get(1), vars,procs));
                ret.addAll(valueCrawl(curr.getChildren().get(2), vars,procs));
                if(curr.getChildren().get(2).getData("value").equals("+")&&
                        curr.getChildren().get(1).getData("value").equals("+")){
                    curr.addValue("+");
                    curr.getChildren().get(1).addValue("+");
                    curr.getChildren().get(2).addValue("+");
                }
                break;
            case 35:
            case 36:
                ret.addAll(valueCrawl(curr.getChildren().get(0), vars,procs));
                ret.addAll(valueCrawl(curr.getChildren().get(2), vars,procs));
                if(vars.get(curr.getChildren().get(2).getChildren().get(0).getData("internalName")).equals("+")&&
                        vars.get(curr.getChildren().get(0).getChildren().get(0).getData("internalName")).equals("+")){
                    curr.addValue("+");
                    curr.getChildren().get(2).addValue("+");
                    curr.getChildren().get(0).addValue("+");
                }
                break;
            case 37:
                ret.addAll(valueCrawl(curr.getChildren().get(1), vars, procs));
                if(curr.getChildren().get(1).getData("value").equals("+")) {
                    curr.addValue("+");
                    curr.getChildren().get(1).addValue("+");
                }
                break;
        }
        return ret;
    }
    private void populateMaps(SyntaxNode curr, HashMap<String, String> varVals, HashMap<String, SyntaxNode> procs){
        if(curr == null || curr.getNodeType().equals(SyntaxNode.type.TERMINAL))
            return;
        if(curr.equals(treeRoot))
            for(String s: symTable.getTable().keySet()){
            if(s.charAt(0) == 'V'){
                varVals.put(s,"-");
            }
        }
        else if(curr.getData("symbol").equals("PROC")){
            procs.put(curr.getChildren().get(1).getData("internalName"), curr.getChildren().get(2));
        }
        for(SyntaxNode c: curr.getChildren()){
            populateMaps(c, varVals, procs);
        }
    }
    private HashMap<String, HashMap<String, String>> clone(SymbolTable symTable) {
        HashMap<String, HashMap<String, String>> ret = new HashMap<>();
        for(String s: symTable.getTable().keySet()){
            String r = new String(s);
            ret.put(r, (HashMap<String, String>) symTable.getTable().get(r).clone());
        }
        return ret;
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
            if(c!=null)
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
                        if(f1Bool.getData("type").equals("f")){
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
            int production = identifyProd(curr);
            switch (production){
                //cases for all error types
                case 15:
                    treeRoot.errMessage += "Type error detected! input() tries to input into string var\n";
                    break;
                case 19:    //var string
                    treeRoot.errMessage += "Type error detected! Trying to set variable to a string literal when var is of type number\n";
                    break;
                case 20:    //var var
                    treeRoot.errMessage += "Type error detected! Trying to assign variables of different types\n";
                    break;
                case 21:    //var num
                    treeRoot.errMessage += "Type error detected! Trying to assign a variable of type string a value of type number\n";
                    break;
                case 22: //num var
                    treeRoot.errMessage += "Type error detected! Variable of type number expected but variable of type string provided\n";
                    break;
                case 35:
                case 36:
                    treeRoot.errMessage += "Type error detected! Trying to perform \"<\" or \">\" comparisons with strings\n";
                    break;
                case 31:
                    treeRoot.errMessage += "Type error detected! for loop contains a variable of type string\n";
                    break;
            }
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
                if(row==null){
                    tab.put(intName, new HashMap<>());
                    row = tab.get(intName);
                }
                row.put("internalName", intName);
                row.put("scope", curr.getData("scope"));
                row.put("type", "u");
            }
        }
        for(SyntaxNode c: curr.getChildren()){
            populateSymbolTable(c);
        }
    }
    private void typeCrawl(SyntaxNode curr, boolean checked){
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
                if (c.getData("type").equals("c")&&!checked) {
                    curr.addType("c");
                    typeCrawl(c, false);
                }else if(c.getData("type").equals("c")&&!checked) {
                    curr.addType("c");
                }else{
                    typeCrawl(c,false);
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
                if (c1.getData("type").equals("c") && c2.getData("type").equals("c")&&checked) {
                    curr.addType("c");
                }else if(c1.getData("type").equals("c") && c2.getData("type").equals("c")&&!checked) {
                    typeCrawl(c1,true);
                    typeCrawl(c2, true);
                }else {
                    typeCrawl(c1,false);
                    typeCrawl(c2, false);
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
                    typeCrawl(prog, false);
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
                typeCrawl(aVar1,false);
                typeCrawl(aVar2,false);
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
                else if (!(nVar1T.equals("s")) && nNum1.getData("type").equals("n")) {
                    nVar1.addType("n");
                    symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    curr.addType("c");
                } else {
                    nVar1.addType("n");
                    symTable.getTable().get(nVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    nVar1 = curr.getChildren().get(0);
                    nNum1 = curr.getChildren().get(1);
                    typeCrawl(nNum1,false);
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
                typeCrawl((curr.getChildren().get(0)),false);
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
                    typeCrawl(Calc,false);
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
                    typeCrawl(num1,false);
                    typeCrawl(num2,false);
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
                    typeCrawl(bool,false);
                    typeCrawl(code1,false);
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
                    typeCrawl(bool2,false);
                    typeCrawl(code21,false);
                    typeCrawl(code22,false);
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
                    typeCrawl(wBool,false);
                    typeCrawl(wCode,false);
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
                typeCrawl(fVar1,false);
                typeCrawl(fVar2,false);
                typeCrawl(fVar3,false);
                typeCrawl(fVar4,false);
                typeCrawl(fVar5,false);
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
                    typeCrawl(fCode,false);
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
                        typeCrawl(eVar1,false);
                        eVar1T = eVar1.getData("type");
                    }
                }
                if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null) {
                    eVar2T = symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).get("type");
                }else if(eVar2.getData("symbol").equals("NUMEXPR")) {
                    if (eVar2.getData("type").equals("n")) {
                        eVar2T = "n";
                    } else {
                        typeCrawl(eVar2,false);
                        eVar2T = eVar2.getData("type");
                    }
                }else{
                    eVar2T = "u";
                }
                if ((eVar1T.equals("n") && eVar2T.equals("s")) || (eVar2T.equals("n") && eVar1T.equals("s"))){
                    curr.addType("f");
                    typeCrawl(eVar1,false);
                    typeCrawl(eVar2,false);
                }else if ((eVar1T.equals("n") && !eVar2T.equals("s")) || (eVar2T.equals("n") && !eVar1T.equals("s"))){
                    curr.addType("b");//assign vars to n
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "n");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "n");
                    typeCrawl(eVar1,false);
                    typeCrawl(eVar2,false);
                }else if((!eVar1T.equals("n")&&eVar2T.equals("s"))||(!eVar2T.equals("n")&&eVar1T.equals("s"))) {
                    curr.addType("b");//assign vars to s
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "s");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "s");
                    typeCrawl(eVar1,false);
                    typeCrawl(eVar2,false);
            }   else{
                    curr.addType("b");//assign vars to o
                    if(symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")) != null)
                        symTable.getTable().get(eVar1.getChildren().get(0).getData("internalName")).put("type", "o");
                    if(symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName"))!=null)
                        symTable.getTable().get(eVar2.getChildren().get(0).getData("internalName")).put("type", "o");
                    typeCrawl(eVar1,false);
                    typeCrawl(eVar2,false);
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
                    typeCrawl(bBool1,false);
                    typeCrawl(bBool2,false);
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
                    typeCrawl(bNum1,false);
                    typeCrawl(bNum2,false);
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
                    typeCrawl(nBool,false);
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
                    typeCrawl(oBool1,false);
                    typeCrawl(oBool2,false);
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
                    typeCrawl(aBool1,false);
                    typeCrawl(aBool2,false);
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
        curr.getChildren().removeIf(Objects::isNull);
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
            curr.addValue("-");
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
