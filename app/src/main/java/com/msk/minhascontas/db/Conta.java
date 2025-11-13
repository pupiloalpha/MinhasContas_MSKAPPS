package com.msk.minhascontas.db; // Certifique-se que o pacote está correto

import java.util.Objects;

public class Conta {

    private long id;
    private String nome;
    private int tipo; // 0: Despesa, 1: Receita, 2: Aplicação
    private int classeConta;
    private int categoria;
    private int dia, mes, ano;
    private double valor;
    private String pagamento; // "paguei" ou "falta"
    private int qtRepete; // Quantidade total de repetições
    private int nRepete;  // Número da repetição atual (começando de 1)
    private int intervalo; // Ex: 300 para mensal
    private String codigo; // Código único para identificar uma série de contas recorrentes
    private double valorJuros; // Taxa de juros (como decimal, ex: 0.05 para 5%)

    // --- Construtor para criar NOVAS contas (sem ID inicial) ---
    // Este construtor inclui o valorJuros
    public Conta(String nome, int tipo, int classeConta, int categoria, int dia, int mes, int ano, double valor, String pagamento, int qtRepete, int nRepete, int intervalo, String codigo, double valorJuros) {
        // this.id = id; // ID é gerado pelo banco de dados, não definido aqui
        this.nome = nome;
        this.tipo = tipo;
        this.classeConta = classeConta;
        this.categoria = categoria;
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
        this.valor = valor; // Valor base
        this.pagamento = pagamento;
        this.qtRepete = qtRepete;
        this.nRepete = nRepete;
        this.intervalo = intervalo;
        this.codigo = codigo;
        this.valorJuros = valorJuros; // Definindo o valor de juros
    }

    // --- Construtor para CONTAS EXISTENTES (com ID) ---
    // Este construtor também inclui o valorJuros
    public Conta(long id, String nome, int tipo, int classeConta, int categoria, int dia, int mes, int ano, double valor, String pagamento, int qtRepete, int nRepete, int intervalo, String codigo, double valorJuros) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.classeConta = classeConta;
        this.categoria = categoria;
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
        this.valor = valor; // Valor pode já ter sido calculado com juros em repetições anteriores
        this.pagamento = pagamento;
        this.qtRepete = qtRepete;
        this.nRepete = nRepete;
        this.intervalo = intervalo;
        this.codigo = codigo;
        this.valorJuros = valorJuros; // Definindo o valor de juros
    }

    // --- Construtor genérico (pode ser útil para alguns casos, sem juros por padrão) ---
    // Se você precisar de um construtor que não exige juros, pode mantê-lo, mas certifique-se
    // de que o valorJuros é inicializado (ex: para 0.0)
    // Se o seu construtor original não tinha valorJuros, pode mantê-lo e inicializar para 0.0
    // ou remover se não for mais necessário. Exemplo:
    public Conta(String nome, int tipo, int classeConta, int categoria, int dia, int mes, int ano, double valor, String pagamento, int qtRepete, int nRepete, int intervalo, String codigo) {
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
        this.valorJuros = 0.0; // Inicializa juros para 0.0 se não for especificado
    }

    // --- Getters ---
    public long getId() { return id; }
    public String getNome() { return nome; }
    public int getTipo() { return tipo; }
    public int getClasseConta() { return classeConta; }
    public int getCategoria() { return categoria; }
    public int getDia() { return dia; }
    public int getMes() { return mes; }
    public int getAno() { return ano; }
    public double getValor() { return valor; }
    public String getPagamento() { return pagamento; }
    public int getQtRepete() { return qtRepete; }
    public int getnRepete() { return nRepete; }
    public int getIntervalo() { return intervalo; }
    public String getCodigo() { return codigo; }

    // Getter para o valor dos juros
    public double getValorJuros() {
        return valorJuros;
    }

    // --- Setters ---
    public void setId(long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTipo(int tipo) { this.tipo = tipo; }
    public void setClasseConta(int classeConta) { this.classeConta = classeConta; }
    public void setCategoria(int categoria) { this.categoria = categoria; }
    public void setDia(int dia) { this.dia = dia; }
    public void setMes(int mes) { this.mes = mes; }
    public void setAno(int ano) { this.ano = ano; }
    public void setValor(double valor) { this.valor = valor; }
    public void setPagamento(String pagamento) { this.pagamento = pagamento; }
    public void setQtRepete(int qtRepete) { this.qtRepete = qtRepete; }
    public void setnRepete(int nRepete) { this.nRepete = nRepete; }
    public void setIntervalo(int intervalo) { this.intervalo = intervalo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    // Setter para o valor dos juros
    public void setValorJuros(double valorJuros) {
        this.valorJuros = valorJuros;
    }

    // --- Métodos Adicionais (Opcional, mas recomendado) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conta conta = (Conta) o;
        return id == conta.id; // Compara pelo ID para igualdade
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Conta{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", valor=" + valor +
                ", valorJuros=" + valorJuros + // Inclui juros no toString
                '}';
    }
}