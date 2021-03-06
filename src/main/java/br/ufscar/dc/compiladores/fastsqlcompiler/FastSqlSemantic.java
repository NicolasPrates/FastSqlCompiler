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
            if(table.name.equals(name)){
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
        if(values.size() != table.fields.size()){
            int line = 0;
            if(values.get(0).INT() != null){ line = values.get(0).INT().getSymbol().getLine();}
            else if(values.get(0).BOOLEAN() != null){ line = values.get(0).BOOLEAN().getSymbol().getLine();}
            else if(values.get(0).DATE() != null){ line = values.get(0).DATE().getSymbol().getLine();}
            else if(values.get(0).REAL() != null){ line = values.get(0).REAL().getSymbol().getLine();}
            else if(values.get(0).VARCHAR() != null){ line = values.get(0).VARCHAR().getSymbol().getLine();}
                    
            ErrorMessages.WrongNumberOfCollumns(table.fields.size(), values.size(), line);
            return false;
        }
        for(var value: values){
            if(value_to_type(value) == table.fields.get(i).type){
                if(value_to_type(value) == Type.VARCHAR){
                    if(table.fields.get(i).len < value.VARCHAR().getText().length() -2){
                        ErrorMessages.ToBigVarchar(table.fields.get(i).name, table.fields.get(i).len, value.VARCHAR().getSymbol().getLine());
                        return false;
                    }
                }
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
            if(field.name.equals(field_name)){
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
            if(field.name.equals(field_name)){
                return true;
            }
        }
        
        return false;
    }

    @Override
    public Void visitScript(FastSqlParser.ScriptContext ctx) {
        if(ctx.create_table() != null) {
            for(var c: ctx.create_table()){
                this.visitCreate_table(c);
            }
        }
        if(ctx.commands() != null){
            for(var c: ctx.commands()){
                this.visitCommands(c);
            }
        }
        
        return null;    
    }

    @Override
    public Void visitCreate_table(FastSqlParser.Create_tableContext ctx) {
        String table_name = ctx.IDENT().getText();
        for(var table: tables){
            if(table.name.equals(table_name)){
                ErrorMessages.TableExists(table_name, ctx.IDENT().getSymbol().getLine());
                return null;
            }
        }
        ArrayList<Field> table_fields = new ArrayList<>();
        
        for(var decl_column: ctx.decl_column()){
            String field_name = decl_column.IDENT().getText();
            for(var field: table_fields){
                if(field.name.equals(field_name)){
                    ErrorMessages.AlreadyACollumn(field_name, table_name, ctx.IDENT().getSymbol().getLine());
                    return null;
                }
            }
            Type field_type = str_to_type(decl_column.TYPE().getText());
            int field_size = 0;
            if(decl_column.sized() != null ){
                field_size = Integer.parseInt(decl_column.sized().INT().getText());
            }
            Field field = new Field(field_name, field_size, field_type);
            table_fields.add(field);
        }
        
        Table table = new Table(table_name, table_fields);
        this.tables.add(table);
        
        //CREATE TABLE table_name (
        //    column1 datatype,
        //    column2 datatype,
        //    column3 datatype,
        //   ....
        //);
        GeradorDeCodigo.addLine("CREATE TABLE " + table.name + "(");
        for(var field: table.fields){
            GeradorDeCodigo.addLine(field.name + " " + GeradorDeCodigo.type_to_sql(field), 1);
        }
        GeradorDeCodigo.addLine(");");
        return null;
    }

    @Override
    public Void visitInsert(FastSqlParser.InsertContext ctx) {
        Table table = get_table_by_name(ctx.IDENT().getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine());
            return null;
        }
        
        if(validate_values(table, ctx.value())){
            //INSERT INTO table_name (column1, column2, column3, ...)
            //VALUES (value1, value2, value3, ...);
            String s = "INSERT INTO " + table.name +"(";
            String columns = "";
            String values = "";
                    
            for(var value: ctx.value()){
                values += value.getText();
                if(value != ctx.value(ctx.value().size() - 1)){
                    values += ", ";
                } 
            }
            
            int nFields = table.fields.size();
            int i = 0;
            for(var field: table.fields){
                columns += field.name;
                if(i < nFields){
                    columns += ", ";
                }
                i++;
            }
            
            s += columns + ") VALUES (" + values + ");";
            GeradorDeCodigo.addLine(s);
            
        };
        return null;
    }

    @Override
    public Void visitFind(FastSqlParser.FindContext ctx) {
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
            if(i != 0){
                //os demais são campos, podemos verificar
                String field_name = ident.getText();
                if(!field_on_table(table, field_name)){
                    ErrorMessages.TableHasNoField(table.name, field_name, ident.getSymbol().getLine());
                    return null;
                }
            }
            i++;
        }
        
        // gerar código
        // SELECT (columns) FROM table WHERE conditions
        String s = "SELECT ";
        String columns = "(";
        i = 0;
        for(var ident: ctx.IDENT()) {
            if(i != 0) {
                columns += ident.getText();
                if(ident != ctx.IDENT(ctx.IDENT().size() - 1)) {
                    columns += ", ";
                }
            }
            i++;
        }
        
        if(columns.equals("(")){    
            s += "* "; // nenhuma coluna foi especificada
        } else {
            s += columns + ") ";
        }
        
        s += "FROM " + table.name +" WHERE ";
        
        for(var itemWhere: ctx.itemWhere()) {
            s+= itemWhere.IDENT().getText() + " = ";
            
            if(itemWhere.value().VARCHAR() != null) {
                s+= itemWhere.value().VARCHAR().getText();
            } else if (itemWhere.value().INT() != null) {
                s+= itemWhere.value().INT().getText();
            }
            else if (itemWhere.value().REAL() != null) {
                s+= itemWhere.value().REAL().getText();
            } else if (itemWhere.value().BOOLEAN() != null) {
                s+= itemWhere.value().BOOLEAN().getText();
            } else if (itemWhere.value().DATE() != null) {
                s+= itemWhere.value().DATE().getText();
            }
            
            // se não for o ultimo
            if(itemWhere != ctx.itemWhere(ctx.itemWhere().size() - 1)) {
                s += " AND ";
            }
        }
        
        s+=";";
        GeradorDeCodigo.addLine(s);
        
        return null;
    }

    @Override
    public Void visitDelete(FastSqlParser.DeleteContext ctx) {

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
        
        // gerar codigo
        // DELETE FROM tableName WHERE conditions
        String s = "DELETE FROM " + table.name + " WHERE ";
        
        for(var itemWhere: ctx.itemWhere()) {
            s+= itemWhere.IDENT().getText() + " = ";
            
            if(itemWhere.value().VARCHAR() != null) {
                s+= itemWhere.value().VARCHAR().getText();
            } else if (itemWhere.value().INT() != null) {
                s+= itemWhere.value().INT().getText();
            }
            else if (itemWhere.value().REAL() != null) {
                s+= itemWhere.value().REAL().getText();
            } else if (itemWhere.value().BOOLEAN() != null) {
                s+= itemWhere.value().BOOLEAN().getText();
            } else if (itemWhere.value().DATE() != null) {
                s+= itemWhere.value().DATE().getText();
            }
            
            // se não for o ultimo
            if(itemWhere != ctx.itemWhere(ctx.itemWhere().size() - 1)) {
                s += " AND ";
            }
        }
        
        s+=";";
        GeradorDeCodigo.addLine(s);
        
        return null;
    }

    @Override
    public Void visitDeleteAll(FastSqlParser.DeleteAllContext ctx) {
        Table table = get_table_by_name(ctx.IDENT().getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine());
            return null;
        }
        
        // gerar codigo
        // DELETE FROM tableName
        String s = "DELETE FROM " +table.name +";";
        GeradorDeCodigo.addLine(s);
        return null;
    }

    @Override
    public Void visitFindAll(FastSqlParser.FindAllContext ctx) {
        // verifica se a tabela existe
        Table table = get_table_by_name(ctx.tableName.getText());
        
        if(table == null){
            ErrorMessages.NoTableWithThisName(ctx.tableName.getText(), ctx.tableName.getLine());
            return null;
        }
        
        int i = 0;
        for(var ident: ctx.IDENT()){
            // pular o primeiro IDENT que é o nome da tabela
            if(i != 0){
                //os demais são campos, podemos verificar
                String field_name = ident.getText();
                if(!field_on_table(table, field_name)){
                    ErrorMessages.TableHasNoField(table.name, field_name, ident.getSymbol().getLine());
                    return null;
                }
            }
            i++;
        }
        
        // gerar código
        // SELECT (columns) FROM table WHERE conditions
        String s = "SELECT ";
        String columns = "(";
        i = 0;
        for(var ident: ctx.IDENT()) {
            if(i != 0) {
                columns += ident.getText();
                if(ident != ctx.IDENT(ctx.IDENT().size() - 1)) {
                    columns += ", ";
                }
            }
            i++;
        }
        
        if(columns.equals("(")){    
            s += "* "; // nenhuma coluna foi especificada
        } else {
            s += columns + ") ";
        }
        
        s+= "FROM " + table.name + ";";
        GeradorDeCodigo.addLine(s);
        return null;
    }
    
    
    
    
    
    
}
