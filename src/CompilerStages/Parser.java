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
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "INSTR");
        if(input.equals("halt")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "halt"));
            match(input);
        }else if(input.equals("input")||input.equals("output")){
            res.addChild(parseIO());
        }else if(input.equals(TokenNode.Type.VARNAME)&&input.getNext().equals("=")){
            res.addChild(parseASSIGN());
        }else if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(parseCALL());
        }else if(input.equals("if")){
            res.addChild(parseBRANCH());
        }else{
            res.addChild(parseLOOP());
        }
        return res;
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
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "ASSIGN");
        if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.ASSIGN, "="));
            if(input.equals(TokenNode.Type.STRING)){
                res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            }else if(input.equals(TokenNode.Type.VARNAME)){
                res.addChild(parseVAR());
            }else{
                res.addChild(parseNUMEXPR());
            }
        }
        return res;
    }
    private SyntaxNode parseNUMEXPR(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "NUMEXPR");
        if(input.equals(TokenNode.Type.INTEGER)){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            match(input);
        }else if(input.equals(TokenNode.Type.VARNAME)){
            res.addChild(parseVAR());
        }else{
            res.addChild(parseCALC());
        }
        return res;
    }
    private SyntaxNode parseCALC(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "CALC");
        if(input.equals("add")||input.equals("sub")|| input.equals("mult")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            match(input);
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseNUMEXPR());
            match(new TokenNode(TokenNode.Type.COMMA, ","));
            res.addChild(parseNUMEXPR());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
        }else{
            System.err.print("Expected calculation add/sub/mult, received: "+input.getData());
            System.exit(-1);
        }
        return res;
    }
    private SyntaxNode parseBRANCH(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "BRANCH");
        if(input.equals("if")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "if"));
            match(input);
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseBOOL());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            if(input.equals("then")){
                res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "then"));
                match(input);
            }else{
                System.err.print("Expected \"then\", received: " + input.getData());
                System.exit(-1);
            }
            match(new TokenNode(TokenNode.Type.LBRACE, "{"));
            res.addChild(parseCODE());
            match(new TokenNode(TokenNode.Type.RBRACE, "}"));
            if(input.equals("else")){
                res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "else"));
                match(input);
                match(new TokenNode(TokenNode.Type.LBRACE, "{"));
                res.addChild(parseCODE());
                match(new TokenNode(TokenNode.Type.RBRACE, "}"));
            }
        }
        return res;
    }
    private SyntaxNode parseLOOP() {
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "LOOP");
        if (input.equals("for")) {
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "for"));
            match(input);
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.ASSIGN, "="));
            match(new TokenNode(TokenNode.Type.INTEGER, "0"));
            match(new TokenNode(TokenNode.Type.SEMICOLON, ";"));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.GREATCOMP, "<"));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.SEMICOLON, ";"));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.ASSIGN, "="));
            match(new TokenNode(TokenNode.Type.ADD, "add"));
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.COMMA, ","));
            match(new TokenNode(TokenNode.Type.INTEGER, "1"));
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            match(new TokenNode(TokenNode.Type.LBRACE, "{"));
            res.addChild(parseCODE());
            match(new TokenNode(TokenNode.Type.RBRACE, "}"));
        } else if(input.equals("while")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "while"));
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseBOOL());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            match(new TokenNode(TokenNode.Type.LBRACE, "{"));
            res.addChild(parseCODE());
            match(new TokenNode(TokenNode.Type.RBRACE, "}"));
        }
        return res;
    }
    private SyntaxNode parseBOOL(){
        SyntaxNode res = new SyntaxNode(SyntaxNode.type.NONTERMINAL, "BOOL");
        if(input.equals("eq")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "eq"));
            match(input);
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            if(input.equals(TokenNode.Type.VARNAME)){
                res.addChild(parseVAR());
                match(new TokenNode(TokenNode.Type.COMMA, ","));
                res.addChild(parseVAR());
                match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            }else if(input.equals(TokenNode.Type.INTEGER)||input.equals("add")||input.equals("sub")||input.equals("mult")){//NUMEXPR
                res.addChild(parseNUMEXPR());
                match(new TokenNode(TokenNode.Type.COMMA, ","));
                res.addChild(parseNUMEXPR());
                match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            }else{//BOOL
                res.addChild(parseBOOL());
                match(new TokenNode(TokenNode.Type.COMMA, ","));
                res.addChild(parseBOOL());
                match(new TokenNode(TokenNode.Type.RPAREN, ")"));
            }
        }else if(input.equals("(")){
            match(input);
            res.addChild(parseVAR());
            if(input.equals("<")||input.equals(">")) {
                res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
                match(input);
            }else {
                System.err.print("Expected < or >, received: " + input.getData());
                System.exit(-1);
            }
            res.addChild(parseVAR());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
        }else if(input.equals("not")) {
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, "not"));
            match(input);
            res.addChild(parseBOOL());
        }else if(input.equals("or")|| input.equals("and")){
            res.addChild(new SyntaxNode(SyntaxNode.type.TERMINAL, input.getData()));
            match(input);
            match(new TokenNode(TokenNode.Type.LPAREN, "("));
            res.addChild(parseBOOL());
            match(new TokenNode(TokenNode.Type.COMMA, ","));
            res.addChild(parseBOOL());
            match(new TokenNode(TokenNode.Type.RPAREN, ")"));
        }else{
            System.err.print("Expected boolean expression, received: " + input.getData());
            System.exit(-1);
        }
        return res;
    }
}
