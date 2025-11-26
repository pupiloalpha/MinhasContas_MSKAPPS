package com.msk.minhascontas.db;

import android.provider.BaseColumns;

/**
 * Classe Contract (Contrato) que define o esquema do banco de dados para a tabela 'contasListadas'.
 * Segue a recomendação do Android de isolar as constantes do esquema.
 */
public final class ContasContract {
    // Para evitar que alguém instancie a classe contract, defina um construtor vazio privado.
    private ContasContract() {}

    /**
     * Classe interna que define as constantes para as colunas da tabela de Contas.
     * Implementa BaseColumns para incluir colunas _ID.
     */
    public static class Colunas implements BaseColumns {
        public static final String TABELA_NOME = "contasListadas";

        // Nomes das Colunas do Banco de Dados
        public static final String COLUNA_NOME_CONTA = "nome_conta";
        public static final String COLUNA_TIPO_CONTA = "tipo_conta";
        public static final String COLUNA_CLASSE_CONTA = "classe_conta";
        public static final String COLUNA_CATEGORIA_CONTA = "categoria_conta";
        public static final String COLUNA_DIA_DATA_CONTA = "dia_data";
        public static final String COLUNA_MES_DATA_CONTA = "mes_data";
        public static final String COLUNA_ANO_DATA_CONTA = "ano_data";
        public static final String COLUNA_VALOR_CONTA = "valor_conta";
        public static final String COLUNA_PAGOU_CONTA = "pagou_conta";
        public static final String COLUNA_QT_REPETICOES_CONTA = "qt_repeticoes";
        public static final String COLUNA_NR_REPETICAO_CONTA = "nr_repeticao";
        public static final String COLUNA_INTERVALO_CONTA = "intervalo_conta";
        public static final String COLUNA_CODIGO_CONTA = "codigo";
        public static final String COLUNA_VALOR_JUROS = "valor_juros";
    }

    // =========================================================
    // NOVAS CONSTANTES DE VALOR (PARA USO EM OUTRAS CLASSES)
    // =========================================================

    /**
     * Constantes para a COLUNA_TIPO_CONTA.
     */
    public static final int TIPO_DESPESA = 0;
    public static final int TIPO_RECEITA = 1;
    public static final int TIPO_APLICACAO = 2;


    /**
     * Constantes para a COLUNA_CLASSE_CONTA.
     * Mapeamento usado em ResumoTipoMensal.java
     */
    // Classes de DESPESA (Usadas quando TIPO_CONTA = TIPO_DESPESA)
    public static final int CLASSE_DESPESA_CARTAO = 0;
    public static final int CLASSE_DESPESA_FIXA = 1;
    public static final int CLASSE_DESPESA_VARIAVEL = 2;
    public static final int CLASSE_DESPESA_PRESTACOES = 3;

    // Classes de APLICAÇÃO (Usadas quando TIPO_CONTA = TIPO_APLICACAO)
    public static final int CLASSE_APLICACAO_FUNDOS = 0;
    public static final int CLASSE_APLICACAO_POUPANCA = 1;
    public static final int CLASSE_APLICACAO_RENDAVARIAVEL = 2; // Novo valor de 2017
    public static final int CLASSE_APLICACAO_OUTRAS = 3;

    public static final int CATEGORIA_ALIMENTACAO = 0;
    public static final int CATEGORIA_EDUCACAO = 1;
    public static final int CATEGORIA_LAZER = 2;
    public static final int CATEGORIA_MORADIA = 3;
    public static final int CATEGORIA_SAUDE = 4;
    public static final int CATEGORIA_TRANSPORTE = 5;
    public static final int CATEGORIA_VESTUARIO = 6;
    public static final int CATEGORIA_OUTROS = 7;

    /**
     * Constantes para a COLUNA_PAGOU_CONTA.
     */
    public static final String STATUS_PAGO_RECEBIDO = "paguei";
    public static final String STATUS_PENDENTE = "falta";
}