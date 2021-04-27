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
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "PROC_DEFS");
        if(input.equals("proc")){
            res.addChild(parsePROC());
            if(!input.equals("$")){
                res.addChild(parsePROC());
            }
            return res;
        }else{
            return null;
        }
    }
    private SyntaxNode parsePROC(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "PROC");
        match(new TokenNode(TokenNode.Type.PROC, "proc"));
        res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "proc"));
        if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
        }else{
            System.err.print("Error: expected procedure name, received: " + input.getData());
            System.exit(-1);
        }
        match(input);
        match(new TokenNode(TokenNode.Type.LBRACE, "{"));
        res.addChild(parseCODE());
        match(new TokenNode(TokenNode.Type.RBRACE, "}"));
        return res;
    }
    private SyntaxNode parseINSTR(){
        return null;
    }
    private SyntaxNode parseIO(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "IO");
        if(input.equals("input")){
            match(new TokenNode(TokenNode.Type.INPUT, "input"));
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "input"));
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
        }else if(input.equals("output")){
            match(new TokenNode(TokenNode.Type.OUTPUT, "output"));
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "output"));
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
        }else{
            System.err.print("expected IO call \"input\" or \"output\", received: " + input.getData());
            System.exit(-1);
        }
        return res;
    }
    private SyntaxNode parseCALL(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "CALL");
        if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            match(input);
        }else{
            System.err.print("Expected procedure name, received: " + input.getData());
            System.exit(-1);
        }
        return res;
    }
    private SyntaxNode parseVAR(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "VAR");
        if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            match(input);
        }else{
            System.err.print("Expected variable name, received: " + input.getData());
            System.exit(-1);
        }
        return res;
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
