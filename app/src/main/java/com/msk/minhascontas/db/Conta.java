// Conta.java
package com.msk.minhascontas.db;

import static com.msk.minhascontas.db.ContasContract.Colunas;

import android.database.Cursor;

/**
 * POJO (Plain Old Java Object) que representa a entidade Conta.
 * Esta classe encapsula os dados de uma conta, melhorando a coesão
 * e a manutenibilidade do código, reduzindo a necessidade de longas
 * listas de parâmetros nas chamadas de metodos.
 *
 * MODIFICAÇÃO: Inclusão do Padrão Builder para simplificar a criação
 * de novas instâncias, especialmente nas Activities CriarConta e EditarConta.
 */
public class Conta {

    private long idConta;
    private String nome;
    private int tipo;
    private int classeConta;
    private int categoria;
    private int dia;
    private int mes;
    private int ano;
    private double valor;
    private String pagamento;
    private int qtRepete;
    private int nRepete;
    private int intervalo;
    private String codigo;
    // Campo para o novo Juros, conforme DBContas.java
    private double valorJuros;

    /**
     * Construtor padrão sem argumentos, necessário para classes que
     * criam o objeto e depois usam setters para preencher os dados
     * (como a nova classe ImportarExcel.java).
     */
    public Conta() {
        // Inicializa com valores padrão para garantir consistência
        this.idConta = 0;
        this.nome = "";
        this.tipo = 0; // Padrão
        this.classeConta = 0; // Padrão
        this.categoria = 0; // Padrão
        this.dia = 1; // Padrão: 1
        this.mes = 1; // Padrão: 1
        this.ano = 2000; // Padrão: 2000
        this.valor = 0.0;
        this.pagamento = "";
        this.qtRepete = 1;
        this.nRepete = 1;
        this.intervalo = 0;
        this.codigo = "";
        this.valorJuros = 0.0;
    }

    // Construtor completo (MANTIDO para compatibilidade com o codigo DBContas.java atual)
    public Conta(long idConta, String nome, int tipo, int classeConta, int categoria,
                 int dia, int mes, int ano, double valor, String pagamento, int qtRepete,
                 int nRepete, int intervalo, String codigo) {
        this(idConta, nome, tipo, classeConta, categoria, dia, mes, ano, valor, pagamento,
                qtRepete, nRepete, intervalo, codigo, 0.0);
    }

    // NOVO: Construtor completo, incluindo o campo valorJuros (para DBContas refatorado)
    public Conta(long idConta, String nome, int tipo, int classeConta, int categoria,
                 int dia, int mes, int ano, double valor, String pagamento, int qtRepete,
                 int nRepete, int intervalo, String codigo, double valorJuros) {
        this.idConta = idConta;
        this.nome = nome;
        this.tipo = tipo;
        this.classeConta = classeConta;
        this.categoria = categoria;
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
        this.valor = valor;
        this.pagamento = pagamento;
        this.qtRepete = qtRepete;
        this.nRepete = nRepete;
        this.intervalo = intervalo;
        this.codigo = codigo;
        this.valorJuros = valorJuros;
    }

    // Construtor privado para forçar o uso do Builder
    private Conta(Builder builder) {
        this.idConta = builder.idConta;
        this.nome = builder.nome;
        this.tipo = builder.tipo;
        this.classeConta = builder.classeConta;
        this.categoria = builder.categoria;
        this.dia = builder.dia;
        this.mes = builder.mes;
        this.ano = builder.ano;
        this.valor = builder.valor;
        this.pagamento = builder.pagamento;
        this.qtRepete = builder.qtRepete;
        this.nRepete = builder.nRepete;
        this.intervalo = builder.intervalo;
        this.codigo = builder.codigo;
        this.valorJuros = builder.valorJuros;
    }

    public static Conta fromCursor(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            return null;
        }

        // Nota: O uso de getColumnIndexOrThrow garante que o aplicativo falhe rapidamente
        // se uma coluna for renomeada ou removida, forçando a correção.
        return new Conta(
                cursor.getLong(cursor.getColumnIndexOrThrow(Colunas._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA)),
                // Garante a leitura correta como INTEGER após a migração
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA)),
                cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA)),
                cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS)) // Campo de Juros
        );
    }

    /**
     * Classe Builder para construção flexível de objetos Conta.
     */
    public static class Builder {
        // Campos obrigatórios (ou com valor padrão)
        private final String nome;
        private final double valor;
        private final int dia;
        private final int mes;
        private final int ano;
        private final String codigo;

        // Campos opcionais (com valores padrão)
        private long idConta = 0;
        private int tipo = 0; // Padrão: Despesa
        private int classeConta = 0;
        private int categoria = 0;
        private String pagamento = "falta";
        private int qtRepete = 1;
        private int nRepete = 1;
        private int intervalo = 300; // Padrão: Mensalmente (baseado em CriarConta.java)
        private double valorJuros = 0.0;


        /**
         * Construtor do Builder com os campos essenciais.
         * @param nome Nome da conta.
         * @param valor Valor da conta.
         * @param dia Dia da data.
         * @param mes Mês da data.
         * @param ano Ano da data.
         * @param codigo Código de recorrência (UUID).
         */
        public Builder(String nome, double valor, int dia, int mes, int ano, String codigo) {
            this.nome = nome;
            this.valor = valor;
            this.dia = dia;
            this.mes = mes;
            this.ano = ano;
            this.codigo = codigo;
        }

        // Métodos de configuração (setters do Builder)
        public Builder setIdConta(long idConta) {
            this.idConta = idConta;
            return this;
        }

        public Builder setTipo(int tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder setClasseConta(int classeConta) {
            this.classeConta = classeConta;
            return this;
        }

        public Builder setCategoria(int categoria) {
            this.categoria = categoria;
            return this;
        }

        public Builder setPagamento(String pagamento) {
            this.pagamento = pagamento;
            return this;
        }

        public Builder setQtRepete(int qtRepete) {
            this.qtRepete = qtRepete;
            return this;
        }

        public Builder setNRepete(int nRepete) {
            this.nRepete = nRepete;
            return this;
        }

        public Builder setIntervalo(int intervalo) {
            this.intervalo = intervalo;
            return this;
        }

        public Builder setValorJuros(double valorJuros) {
            this.valorJuros = valorJuros;
            return this;
        }

        /**
         * Método final para criar a instância de Conta.
         */
        public Conta build() {
            return new Conta(this);
        }
    }


    // ------------------------------------------
    // Getters e Setters (Mantidos e Atualizados)
    // ------------------------------------------

    public long getIdConta() { return idConta; }
    public void setIdConta(long idConta) { this.idConta = idConta; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public int getClasseConta() { return classeConta; }
    public void setClasseConta(int classeConta) { this.classeConta = classeConta; }

    public int getCategoria() { return categoria; }
    public void setCategoria(int categoria) { this.categoria = categoria; }

    public int getDia() { return dia; }
    public void setDia(int dia) { this.dia = dia; }

    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getPagamento() { return pagamento; }
    public void setPagamento(String pagamento) { this.pagamento = pagamento; }

    public int getQtRepete() { return qtRepete; }
    public void setQtRepete(int qtRepete) { this.qtRepete = qtRepete; }

    public int getNRepete() { return nRepete; }
    public void setNRepete(int nRepete) { this.nRepete = nRepete; }

    public int getIntervalo() { return intervalo; }
    public void setIntervalo(int intervalo) { this.intervalo = intervalo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    // NOVO: Getters e Setters para valorJuros
    public double getValorJuros() { return valorJuros; }
    public void setValorJuros(double valorJuros) { this.valorJuros = valorJuros; }
}