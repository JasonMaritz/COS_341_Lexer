package CompilerStages;

import Nodes.SyntaxNode;
import Nodes.TokenNode;

public class Parser {
    TokenNode input;
    TokenNode LLHead;
    SyntaxNode SyntaxTreeRoot;

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
            System.err.print(" expected: "+ expected.getData());
            System.exit(-1);
        }
    }

    private SyntaxNode parsePROGPRIME(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "PROGPRIME");
        if(input.equals("halt")||input.equals("input")||input.equals("output")||input.equals(TokenNode.Type.VARNAME)||
                input.equals("if")||input.equals("for")|| input.equals("while")){
            res.addChild(parsePROG());
            match(new TokenNode(TokenNode.Type.EOF, "$"));
            return res;
        }else{
            return null;
        }
    }
    private SyntaxNode parsePROG(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "PROG");
        if(input.equals("halt")||input.equals("input")||input.equals("output")||input.equals(TokenNode.Type.VARNAME)||
                input.equals("if")||input.equals("for")|| input.equals("while")){
            res.addChild(parseCODE());
            if(input.equals(";")){
                match(input);
                res.addChild(parsePROC_DEFS());
            }
            return res;
        }else{
            return null;
        }
    }
    private SyntaxNode parseCODE(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "CODE");
        if(input.equals("halt")||input.equals("input")||input.equals("output")||input.equals(TokenNode.Type.VARNAME)||
                input.equals("if")||input.equals("for")|| input.equals("while")){
              res.addChild(parseINSTR());
              if(input.equals(";")){
                  match(new TokenNode(TokenNode.Type.SEMICOLON, ";"));
                  res.addChild(parseCODE());
              }
            return res;
        }else{
            return null;
        }
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
