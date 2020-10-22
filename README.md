# Fast Sql Compiler
#### Compilador e linguagem criados para a disciplina Construção de Compiladores
####               do Departamento de Computação - UFSCar  
_____________________________________________________________________________________

## Linguagem Fast Sql
A linguagem FastSql foi criada com o intenção de facilitar a escrita de scripts sql
de forma mais simples e direta. Com ela é possível criar, fazer pesquisas, deletar e
incluir dados em tabelas de bancos de dados.

### Exemplos de uso
- Criando uma tabela
```
createTable(Pessoas)
    .columns(
        id:int, 
        nome:varchar(50), 
        idade:varchar(3), 
        endereco:varchar(50)
    )
```
- inserindo linhas
```
Pessoas.insert(1, "Fulano", "20", "Rua dois")
```


- Pesquisando linhas
``` 
Pessoas.find(nome: "Fulano", idade: "20")
```

``` 
Pessoas.find(nome: "Fulano", idade: "20").columns(nome, endereco)
``` 

- Deletando linhas
```
Pessoas.delete(nome: "Fulano")
```



******************************************************
## O compilador
O compilador da linguagem Fast Sql gera código sql e realiza análises léxicas, 
sintáticas e semanticas para auxiliar o programador na hora de codar. 
Ele foi criado utilizando a biblioteca [Antlr 4.8](https://www.antlr.org/) e a
linguagem Java
### Exemplo de geração de código
- Código escrito em Fast Sql: 
```
createTable(Pessoas)
    .columns(
        id:int, 
        nome:varchar(50), 
        idade:varchar(3), 
        endereco:varchar(50)
    )

Pessoas.insert(1, "Fulano", "20", "Rua dois")

Pessoas.find(nome: "Fulano", idade: "20")
Pessoas.find(nome: "Fulano", idade: "20").columns(nome, endereco)

Pessoas.delete(nome: "Fulano")
```
- Código Sql gerado pelo compilador:

```
CREATE TABLE Pessoas (
  id int, 
  nome varchar(50), 
  idade varchar(3), 
  endereco varchar(50),
);

INSERT INTO Pessoas VALUES (1, 'Fulano', '20', 'Rua dois');

SELECT * FROM Pessoas WHERE nome = 'Fulano' AND idade = '20';
SELECT (nome, endereco) FROM Pessoas WHERE nome = 'Fulano' AND idade = '20';

DELETE FROM Pessoas WHERE nome = 'Fulano';
```
******************************************************
## Compilando o projeto
Caso você deseje compilar o código fonte siga os passos a baixo:

1. Abra o projeto em uma IDE Java (recomendação: [Apache NetBeans](http://netbeans.apache.org/))
2. Execute a verificação de pacotes/plugins do maven
3. Clique no botão de Build da sua IDE

## Utilizando o compilador
Para utilizar o compilador siga os passos a baixo:

1. Escreva um código em Fast Sql e salve em um arquivo .txt
2. Baixe o [executável do compilador](https://github.com/vitor0x5/FastSqlCompiler/blob/master/target/fastSqlCompiler-1.0-SNAPSHOT-jar-with-dependencies.jar) ou compile o projeto você mesmo
3. Abra um terminal e execute o seguinte comando:
```
java -jar fastSqlCompiler-1.0-SNAPSHOT-jar-with-dependencies.jar seu-código-fast-sql.txt arquivo-de-saída.txt
```
4. O compilador irá analisar seu-código-fast-sql.txt e imprimir no arquivo-de-saída.txt os erros (caso haja algum) ou o código sql gerado
5. Use o código gerado em seu banco de dados de preferência






