package br.ufscar.dc.compiladores.fastsqlcompiler;

import br.ufscar.dc.compiladores.fastsqlcompiler.ErrorHandlers.ErrorMessages;
import java.util.ArrayList;
import java.util.List;

public class FastSqlSemantic extends FastSqlBaseVisitor<Void>{
    
    public class Table{
        String name;
        ArrayList<Field> fields;
//        ArrayList<ArrayList<Object>> lines;;

        public Table(String name, ArrayList<Field> fields) {
            this.name = name;
            this.fields = fields;
        }
    }
    
    public class Field{
        String name;
        int len;
        Type type;

        public Field(String name, int len, Type type) {
            this.name = name;
            this.len = len;
            this.type = type;
        }
    }

    public enum Type {
        BOOLEAN, VARCHAR, DATE, INT, REAL
    }
    
    ArrayList<Table> tables;

    public FastSqlSemantic() {
        tables = new ArrayList<>();
    }
    
    private Type str_to_type(String text) {
        switch(text){
            case "boolean": return Type.BOOLEAN;
            case "varchar": return Type.VARCHAR;
            case "date": return Type.DATE;
            case "int": return Type.INT;
            case "real": return Type.REAL;
        }
        return null;
    }
    
    private Table get_table_by_name(String name){
        for(var table: this.tables){
            if(table.name == name){
                return table;
            }
        }
        return null;
    }
    
     private Type value_to_type(FastSqlParser.ValueContext ctx){
        if(ctx.INT() != null){
            return Type.INT;
        }
        else if(ctx.REAL() != null){
            return Type.REAL;
        }
        else if(ctx.BOOLEAN() != null){
            return Type.BOOLEAN;
        }
        else if(ctx.DATE() != null){
            return Type.DATE;
        }
        else if(ctx.VARCHAR() != null){
            return Type.VARCHAR;
        }
        return null;
    }
     
    private int get_value_line(FastSqlParser.ValueContext ctx){
        if(ctx.INT() != null){
            return ctx.INT().getSymbol().getLine();
        }
        else if(ctx.REAL() != null){
            return ctx.REAL().getSymbol().getLine();
        }
        else if(ctx.BOOLEAN() != null){
            return ctx.BOOLEAN().getSymbol().getLine();
        }
        else if(ctx.DATE() != null){
            return ctx.DATE().getSymbol().getLine();
        }
        else if(ctx.VARCHAR() != null){
            return ctx.VARCHAR().getSymbol().getLine();
        }
        return -1;
    }
    
    private boolean validate_values(Table table, List<FastSqlParser.ValueContext> values){
        int i = 0;
        for(var value: values){
            if(value_to_type(value) == table.fields.get(i).type){
                i++;
            }
            else{
                ErrorMessages.WrongType(i+1, table.fields.get(i).type.name(), get_value_line(value));
                return false;
            }
        }
        return true;
    }
    
    private boolean validate_value_with_field(Table table, String field_name, FastSqlParser.ValueContext value){
        for(var field: table.fields){
            if(field.name == field_name){
                if(field.type == value_to_type(value)){
                    return true;
                }
                else{
                    ErrorMessages.FieldWrongType(field_name, field.type.name(), value_to_type(value).name(), get_value_line(value));
                    return false;
                }
            }
        }
        ErrorMessages.TableHasNoField(table.name, field_name, get_value_line(value));
        return false;
    }
    
    private boolean field_on_table(Table table, String field_name){
        for(var field: table.fields){
            if(field.name == field_name){
                return true;
            }
        }
        
        return false;
    }

    @Override
    public Void visitScript(FastSqlParser.ScriptContext ctx) {
        System.out.println("Visiting script context");
        for(var c: ctx.commands()){
            this.visitCommands(c);
        }
        return null;
    }
    
    @Override
    public Void visitCommands(FastSqlParser.CommandsContext ctx) {
        System.out.println("Visiting commands context");
        if(ctx.create_table() != null){
            System.err.println("Have create table");
            this.visitCreate_table(ctx.create_table());
        }
        if(ctx.insert() != null){
            visitInsert(ctx.insert());
        }
        if(ctx.find() != null){
            visitFind(ctx.find());
        }
        if(ctx.delete() != null){
            visitDelete(ctx.delete());
        }
        return null;
    }

    @Override
    public Void visitCreate_table(FastSqlParser.Create_tableContext ctx) {
        System.out.println("Visiting create_table context");
        String table_name = ctx.IDENT().getText();
        ArrayList<Field> table_fields = new ArrayList<>();
        
        for(var decl_column: ctx.decl_column()){
            String field_name = decl_column.IDENT().getText();
            Type field_type = str_to_type(decl_column.TYPE().getText());
            int field_size = 0;
            
            if(decl_column.sized() != null){
                field_size = Integer.parseInt(decl_column.sized().INT().getText());
            }
            
            Field field = new Field(field_name, field_size, field_type);
            table_fields.add(field);
        }
        
        Table table = new Table(table_name, table_fields);
        this.tables.add(table);
        return super.visitCreate_table(ctx); 
    }

    @Override
    public Void visitInsert(FastSqlParser.InsertContext ctx) {
        System.out.println("Visiting insert context");
        Table table = get_table_by_name(ctx.IDENT().getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine());
            return null;
        }
        
        if(!validate_values(table, ctx.value())){
            return null;
        }
        
        return super.visitInsert(ctx); 
    }

    @Override
    public Void visitFind(FastSqlParser.FindContext ctx) {
        System.out.println("Visiting find context");
        // precisamos verificar para cada itemWhere se o campo existe e se o valor
        // passado confere com o tipo do campo, vamos lá
        
        // verifica se a tabela existe
        Table table = get_table_by_name(ctx.tableName.getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.tableName.getText(), ctx.tableName.getLine());
            return null;
        }
        
        int i = 0;
        for(var itemWhere: ctx.itemWhere()){
            String field_name = itemWhere.IDENT().getText();
            if(!validate_value_with_field(table, field_name, itemWhere.value())){
                return null;
            }
        }        
        
        // precisamos conferir se a tabela contém os campos que estão sendo solicitados
        i = 0;
        for(var ident: ctx.IDENT()){
            // pular o primeiro IDENT que é o nome da tabela
            if(i == 0){
                //os demais são campos, podemos verificar
                String field_name = ident.getText();
                if(!field_on_table(table, field_name)){
                    ErrorMessages.TableHasNoField(table.name, field_name, ident.getSymbol().getLine());
                    return null;
                }
            }
            i++;
        }
        
        return super.visitFind(ctx);
    }

    @Override
    public Void visitDelete(FastSqlParser.DeleteContext ctx) {
        System.out.println("Visiting delete context");

        Table table = get_table_by_name(ctx.IDENT().getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine());
            return null;
        }
        
        int i = 0;
        for(var itemWhere: ctx.itemWhere()){
            String field_name = itemWhere.IDENT().getText();
            if(!validate_value_with_field(table, field_name, itemWhere.value())){
                return null;
            }
        }    
        return super.visitDelete(ctx);
    }
    
    
}
