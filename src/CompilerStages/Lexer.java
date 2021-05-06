package CompilerStages;

import Nodes.TokenNode;
import java.util.ArrayList;

public class Lexer {
    String output;
    TokenNode head = null;

    public void lex(String substr) {
        //already split spaces
        String[] partialTokens = substr.split(" ");
        StringBuilder ret = new StringBuilder();
        //first symbols
        partialTokens = matchSym(partialTokens);
        //switch for keywords
        for(String s: partialTokens)
            ret.append(s);
        partialTokens = matchKey(ret.toString().split(" "));
        ret.delete(0, ret.length());
        //then match on dfa
        for(String s: partialTokens)
            ret.append(s);
        partialTokens = matchDfa(partialTokens);
        ret.delete(0, ret.length());
        for(String s: partialTokens)
            ret.append(s);
        output = ret.toString();
    }

    public String toString() {
        return output;
    }

    public TokenNode getOutput(){
        //make LL from output
        String[] tokens = output.split(" ");
        for(String s: tokens) {
            if(s.equals("")){
                continue;
            }
            switch (s){
                case "[TOKEN_EQ]->":
                    nodeAdd(TokenNode.Type.EQ, "eq");
                    break;
                case "[TOKEN_AND]->":
                    nodeAdd(TokenNode.Type.AND, "and");
                    break;
                case "[TOKEN_OR]->":
                    nodeAdd(TokenNode.Type.OR, "or");
                    break;
                case "[TOKEN_NOT]->":
                    nodeAdd(TokenNode.Type.NOT, "not");
                    break;
                case "[TOKEN_ADD]->":
                    nodeAdd(TokenNode.Type.ADD, "add");
                    break;
                case "[TOKEN_MULT]->":
                    nodeAdd(TokenNode.Type.MULT, "mult");
                    break;
                case "[TOKEN_SUB]->":
                    nodeAdd(TokenNode.Type.SUB, "sub");
                    break;
                case "[TOKEN_IF]->":
                    nodeAdd(TokenNode.Type.IF, "if");
                    break;
                case "[TOKEN_THEN]->":
                    nodeAdd(TokenNode.Type.THEN, "then");
                    break;
                case "[TOKEN_ELSE]->":
                    nodeAdd(TokenNode.Type.ELSE, "else");
                    break;
                case "[TOKEN_WHILE]->":
                    nodeAdd(TokenNode.Type.WHILE, "while");
                    break;
                case "[TOKEN_FOR]->":
                    nodeAdd(TokenNode.Type.FOR, "for");
                    break;
                case "[TOKEN_INPUT]->":
                    nodeAdd(TokenNode.Type.INPUT, "input");
                    break;
                case "[TOKEN_OUTPUT]->":
                    nodeAdd(TokenNode.Type.OUTPUT, "output");
                    break;
                case "[TOKEN_HALT]->":
                    nodeAdd(TokenNode.Type.HALT, "halt");
                    break;
                case "[TOKEN_PROC]->":
                    nodeAdd(TokenNode.Type.PROC, "proc");
                    break;
                case "[TOKEN_GREATCOMP]->":
                    nodeAdd(TokenNode.Type.GREATCOMP, ">");
                    break;
                case "[TOKEN_LESSCOMP]->":
                    nodeAdd(TokenNode.Type.LESSCOMP, "<");
                    break;
                case "[TOKEN_LPAREN]->":
                    nodeAdd(TokenNode.Type.LPAREN, "(");
                    break;
                case "[TOKEN_RPAREN]->":
                    nodeAdd(TokenNode.Type.RPAREN, ")");
                    break;
                case "[TOKEN_LBRACE]->":
                    nodeAdd(TokenNode.Type.LBRACE, "{");
                    break;
                case "[TOKEN_RBRACE]->":
                    nodeAdd(TokenNode.Type.RBRACE, "}");
                    break;
                case "[TOKEN_COMMA]->":
                    nodeAdd(TokenNode.Type.COMMA, ",");
                    break;
                case "[TOKEN_SEMICOLON]->":
                    nodeAdd(TokenNode.Type.SEMICOLON, ";");
                    break;
                case "[TOKEN_ASSIGN]->":
                    nodeAdd(TokenNode.Type.ASSIGN, "=");
                    break;
                default:
                    String dfaMatching;
                    try{
                        dfaMatching = s.substring(s.indexOf(':')+1, s.indexOf(','));
                    }catch (Exception e){
                        head = new TokenNode(TokenNode.Type.LEXERERROR, s);
                        return head;
                    }
                    String substring = s.substring(s.indexOf(':', s.indexOf(':')+1)+1, s.indexOf("]"));
                    switch (dfaMatching){
                        case "STRING":
                            nodeAdd(TokenNode.Type.STRING, substring);
                            break;
                        case "INTEGER":
                            nodeAdd(TokenNode.Type.INTEGER, substring);
                            break;
                        case "VARNAME":
                            nodeAdd(TokenNode.Type.VARNAME, substring);
                            break;
                        default:
                    }
            }
        }
        TokenNode newHead = new TokenNode(TokenNode.Type.PSEUDOSTART, "PROG'");
        newHead.setNext(head);
        head.addNode(new TokenNode(TokenNode.Type.EOF, "$"));
        head = newHead;
        return head;
    }

    void nodeAdd(TokenNode.Type type, String data){
        if(head == null){
            head = new TokenNode(type, data);
        }else{
            head.addNode(new TokenNode(type, data));
        }
    }

    private static String[] matchDfa(String[] substr) {
        ArrayList<String> ret = new ArrayList<>();
        boolean returnBool = false;
        for (int i = 0; i < substr.length; i++) {
            String s = substr[i];
            if (s.length() > 0) {
                if (s.charAt(0) == ' ' || s.charAt(0) == '['){
                    ret.add(s);
                    continue;
                }
                //-----------------------------
                if(s.charAt(0) == '"'){
                    while(s.charAt(s.length()-1) != '"'){
                        if(i == s.length()){
                            ret.clear();
                            ret.add("Lexical_Error_Token_" + s + "_too_long");
                            return ret.toArray(String[]::new);
                        }
                        s += substr[i+1];
                        i++;
                    }
                    if(s.length() > 10){
                        //System.err.println("Lexical error: invalid string " + s);
                        ret.clear();
                        ret.add("Lexical_error:invalid_string_" + s);
                        return ret.toArray(String[]::new);
                    }
                    for(int j = 1; j < s.length()-1; j++){
                        if(!((s.charAt(j)>= 'a' && s.charAt(j) <= 'z')||(s.charAt(j)>='0' && s.charAt(j) <= '9')||(s.charAt(j)==' '))){
                            //System.err.println("Lexical error: invalid string " + s);
                            ret.clear();
                            ret.add("Lexical_error:invalid_string_" + s);
                            return ret.toArray(String[]::new);
                        }
                    }
                    ret.add(" [TOKEN:STRING,DATA:" + s + "]->");
                    continue;
                }
                //-----------------------------
                //match and add or prompt error
                //actual dfa stuff
                char State = 'A';
                String Token = "";
                String Data = "";
                while(!s.equals("")){
                    char c = s.charAt(0);
                    switch (State){
                        case 'A':
                            if(c >= '0' && c<= '9'){
                                State = 'B';
                                Token = " [TOKEN:INTEGER,DATA:";
                            }else if(c >= 'a' && c <= 'z') {
                                State = 'D';
                                Token = " [TOKEN:VARNAME,DATA:";
                            }else if(c == '-') {
                                returnBool = true;
                                State = 'C';
                                Token = " [TOKEN:INTEGER,DATA:";
                            }else{
                                //System.err.println("Lexical Error invalid symbol: " + c);
                                ret.clear();
                                ret.add("Lexical_Error_invalid_symbol:" + c);
                                return ret.toArray(String[]::new);
                            }
                            Data += c;
                            break;
                        case 'B':
                            if(!(c >= '0' && c <= '9')){
                                ret.add(Token + Data+"]->");
                                State = 'A';
                                Data = "";
                                continue;
                            }
                            Data += c;
                            break;
                        case 'C':
                            if(c >= '1' && c <= '9'){
                                returnBool = false;
                                State = 'B';
                                Data += c;
                            }else{
                                //System.err.println("Lexical Error invalid symbol in token: " + s);
                                ret.clear();
                                ret.add("Lexical_Error_invalid_symbol_in_token:" + s);
                                return ret.toArray(String[]::new);
                            }
                            break;
                        case 'D':
                            if((c >= 'a' && c <= 'z')||(c >= '0' && c <= '9')){
                                Data += c;
                            }else{
                                //System.err.println("Lexical Error invalid symbol in token: " + s);
                                ret.clear();
                                ret.add("Lexical_Error_invalid_symbol_in_token:" + s);
                                return ret.toArray(String[]::new);
                            }
                            break;
                    }
                    s = s.substring(1);
                }
                if(!returnBool) {
                    ret.add(Token + Data + "]-> ");
                }else{
                    ret.clear();
                    ret.add("Lexical_Error_invalid_symbol_in_token:-");
                    return ret.toArray(String[]::new);
                }
            }
        }

        return ret.toArray(String[]::new);
    }

    private static String[] matchKey(String[] substr) {
        ArrayList<String> retArr = new ArrayList<>();
        for (String val : substr) {
            switch (val) {
                case "eq":
                    retArr.add(" [TOKEN_EQ]-> ");
                    break;
                case "and":
                    retArr.add(" [TOKEN_AND]-> ");
                    break;
                case "or":
                    retArr.add(" [TOKEN_OR]-> ");
                    break;
                case "not":
                    retArr.add(" [TOKEN_NOT]-> ");
                    break;
                case "add":
                    retArr.add(" [TOKEN_ADD]-> ");
                    break;
                case "mult":
                    retArr.add(" [TOKEN_MULT]-> ");
                    break;
                case "sub":
                    retArr.add(" [TOKEN_SUB]-> ");
                    break;
                case "if":
                    retArr.add(" [TOKEN_IF]-> ");
                    break;
                case "then":
                    retArr.add(" [TOKEN_THEN]-> ");
                    break;
                case "else":
                    retArr.add(" [TOKEN_ELSE]-> ");
                    break;
                case "while":
                    retArr.add(" [TOKEN_WHILE]-> ");
                    break;
                case "for":
                    retArr.add(" [TOKEN_FOR]-> ");
                    break;
                case "input":
                    retArr.add(" [TOKEN_INPUT]-> ");
                    break;
                case "output":
                    retArr.add(" [TOKEN_OUTPUT]-> ");
                    break;
                case "halt":
                    retArr.add(" [TOKEN_HALT]-> ");
                    break;
                case "proc":
                    retArr.add(" [TOKEN_PROC]-> ");
                    break;
                default:
                    retArr.add(val);
                    retArr.add(" ");
                    break;
            }
        }
        return retArr.toArray(new String[0]);
    }

    private static String[] matchSym(String[] substr) {
        ArrayList<String> partialTokens = new ArrayList<>();
        for(String val: substr){
            for(char c: val.toCharArray()){
                int index;
                switch (c){
                    case '>':
                        index =val.indexOf(">");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_GREATCOMP]-> ");
                        break;
                    case '<':
                        index =val.indexOf("<");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_LESSCOMP]-> ");
                        break;
                    case '(':
                        index =val.indexOf("(");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_LPAREN]-> ");
                        break;
                    case ')':
                        index =val.indexOf(")");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_RPAREN]-> ");
                        break;
                    case '{':
                        index =val.indexOf("{");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_LBRACE]-> ");
                        break;
                    case '}':
                        index =val.indexOf("}");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_RBRACE]-> ");
                        break;
                    case ',':
                        index =val.indexOf(",");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_COMMA]-> ");
                        break;
                    case ';':
                        index =val.indexOf(";");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_SEMICOLON]-> ");
                        break;
                    case '=':
                        index =val.indexOf("=");
                        if(index > 0) {
                            partialTokens.add(val.substring(0, index));
                        }
                        val = val.substring(1);
                        partialTokens.add(" [TOKEN_ASSIGN]-> ");
                        break;
                    default:
                        partialTokens.add(val.substring(0,1));
                        val = val.substring(1);
                }
            }
            partialTokens.add(" ");
        }
        return partialTokens.toArray(String[]::new);
    }
}
