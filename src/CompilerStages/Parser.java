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
        
    }

    public SyntaxNode getOutput(){
        return SyntaxTreeRoot;
    }

    public String toString(){
        return "Back to doing fuck-all";
    }
}
