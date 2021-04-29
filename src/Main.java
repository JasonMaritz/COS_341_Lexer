import CompilerStages.Lexer;
import CompilerStages.Parser;
import CompilerStages.TreeCrawler;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String output;

        //-------------------LEXER CALL-------------------------------------------------------
        try {
            File inputFile = new File(args[0]);
            if(!inputFile.exists()) {
                System.out.println("No such File");
                System.exit(-1);
            }
            Scanner fScanner = new Scanner(inputFile);
            FileWriter writer = new FileWriter(args[1]);
            StringBuilder curr= new StringBuilder();
            while(fScanner.hasNext()){
                curr.append(fScanner.next()).append(" ");
            }
            //-------------------CompilerStages.Lexer CALL--------------------------------------------------------------
            Lexer lexer = new Lexer();
            lexer.lex(curr.toString());
            fScanner.close();
            //-------------------CompilerStages.Parser CALL-------------------------------------------------------------
            Parser parser = new Parser(lexer.getOutput());
            parser.parse();
            //-------------------ScopeCrawling--------------------------------------------------------------------------
            TreeCrawler scoper = new TreeCrawler(parser.getOutput());
            scoper.scopeCrawl();
            output = parser.getOutput().toString(0);
            //----------------------------------------------------------------------------------------------------------
            writer.append(output);
            writer.close();
        }catch (Exception e){
                e.printStackTrace();
        }
    }


}
