package CompilerStages;

import Nodes.SyntaxNode;
import Nodes.TokenNode;

public class Parser {
    TokenNode input;
    TokenNode LLHead = null;
    SyntaxNode SyntaxTreeRoot = null;

    public Parser(TokenNode LexerInput) {
        LLHead = LexerInput;
    }

    public void parse() {
        //recursive descent
        input = LLHead;
        SyntaxTreeRoot = parsePROGPRIME();
    }

    public SyntaxNode getOutput(){
        return SyntaxTreeRoot;
    }

    public String toString(){
        return "Back to doing fuck-all";
    }


    private void match(TokenNode expected){
        if(input.equals(expected)){
            input = input.getNext();
        }else{
            System.err.print("Unexpected Token:" + input.toString());
            System.exit(-1);
        }
    }
    private SyntaxNode parsePROGPRIME(){
        return null;
    }
    private SyntaxNode parsePROG(){
        return null;
    }
    private SyntaxNode parseCODE(){
        return null;
    }
    private SyntaxNode parsePROC_DEFS(){
        return null;
    }
    private SyntaxNode parsePROC(){
        return null;
    }
    private SyntaxNode parseINSTR(){
        return null;
    }
    private SyntaxNode parseIO(){
        return null;
    }
    private SyntaxNode parseCALL(){
        return null;
    }
    private SyntaxNode parseVAR(){
        return null;
    }
    private SyntaxNode parseASSIGN(){
        return null;
    }
    private SyntaxNode parseNUMEXPR(){
        return null;
    }
    private SyntaxNode parseCALC(){
        return null;
    }
    private SyntaxNode parseBRANCH(){
        return null;
    }
    private SyntaxNode parseLOOP(){
        return null;
    }
    private SyntaxNode parseBOOL(){
        return null;
    }
}
