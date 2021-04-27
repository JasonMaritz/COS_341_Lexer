package Nodes;

public class TokenNode {
    //enum token type
    public enum Type{
        LEXERERROR, STRING, INTEGER, VARNAME, EQ, AND, OR, NOT, ADD, MULT, SUB, IF, THEN, ELSE,
        WHILE, FOR, INPUT, OUTPUT, HALT, PROC, GREATCOMP, LESSCOMP, LPAREN, RPAREN, LBRACE,
        RBRACE, COMMA, SEMICOLON, ASSIGN, EOF, PSEUDOSTART
    }
    //token data as string
    String Data;
    Type tokenType;
    TokenNode next = null;

    public TokenNode(Type type, String data){
        Data = data;
        tokenType = type;
    }

    public void addNode(TokenNode newNode){
        if(next == null){
            next = newNode;
        }else{
            next.addNode(newNode);
        }
    }

    public String toString(){
        String ret = tokenType.name()+":"+Data+"->";
        if(next != null){
            ret += next.toString();
        }
        return ret;
    }

    public Type getTokenType() {
        return tokenType;
    }

    public void setTokenType(Type tokenType) {
        this.tokenType = tokenType;
    }

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        this.Data = data;
    }

    public TokenNode getNext() {
        return next;
    }

    public void setNext(TokenNode next) {
        this.next = next;
    }

}
