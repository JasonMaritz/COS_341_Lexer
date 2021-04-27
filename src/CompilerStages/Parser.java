package CompilerStages;

import Nodes.SyntaxNode;
import Nodes.TokenNode;

public class Parser {
    String input;
    TokenNode LLHead = null;
    SyntaxNode SyntaxTreeRoot = null;

    public Parser(TokenNode LexerInput) {
        LLHead = LexerInput;
    }

    public void parse() {
        //recursive descent
        SyntaxTreeRoot = parsePROGPRIME();
    }

    public SyntaxNode getOutput(){
        return SyntaxTreeRoot;
    }

    public String toString(){
        return "Back to doing fuck-all";
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
