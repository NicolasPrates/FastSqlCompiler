package br.ufscar.dc.compiladores.fastsqlcompiler.ErrorHandlers;

import org.antlr.v4.runtime.Token;

public class ErrorMessages {
    public static String errorsOutput = "";     // STORES ALL ERROR MESSAGES 
    
    public static void undefinedSymbol (String ident, int line) {
        String message = "Linha " + line + ": " + ident + " - Simbolo não identificado";
        errorsOutput += message;
    }
    
    public static void unclosedString (int line) {
        String message = "Linha " + line + ": cadeia literal nao fechada\n";
        errorsOutput += message;
    }

    public static void SyntacticError(String ident, int line) {
        String message = "Linha " + line + ": "+ "Erro sintatico proximo a " + ident +"\n";
        errorsOutput += message;
    }
    
    public static void NoTableWithThisName(String ident, int line) {
        String message = "Linha " + line + ": "+ "Tabela " + ident +" não existe\n";
        errorsOutput += message;
    }
    
    public static void WrongType(int index, String type, int line){
        String message = "Linha " + line + ": "+ "Parametro " + index +" deveria ser um '" + type + "'\n";
        errorsOutput += message;
    }
    
    public static void FieldWrongType(String field_name, String field_type, String type, int line){
        String message = "Linha " + line + ": "+ "Campo " + field_name +" é um '" + field_type +
        "' mas está recebendo um '" + type + "'\n";
        errorsOutput += message;
    }
    
    public static void TableHasNoField(String table_name, String field_name, int line){
        String message = "Linha " + line + ": " + "Tabela " + table_name + " não possui campo " + field_name + "\n";
        errorsOutput += message;
    }
}
