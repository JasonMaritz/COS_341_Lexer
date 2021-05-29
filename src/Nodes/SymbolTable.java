package Nodes;

import java.util.HashMap;

public class SymbolTable {
    HashMap<String, HashMap<String, String>> symbols = new HashMap<>();

    public HashMap<String, HashMap<String, String>> getTable() {
        return symbols;
    }


}
