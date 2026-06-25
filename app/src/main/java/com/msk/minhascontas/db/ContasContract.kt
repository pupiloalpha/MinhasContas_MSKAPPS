package com.msk.minhascontas.db

import android.provider.BaseColumns

/**
 * Classe Contract (Contrato) que define o esquema do banco de dados para a tabela 'contasListadas'.
 * Segue a recomendação do Android de isolar as constantes do esquema.
 */
object ContasContract {
    // =========================================================
    // NOVAS CONSTANTES DE VALOR (PARA USO EM OUTRAS CLASSES)
    // =========================================================
    /**
     * Constantes para a COLUNA_TIPO_CONTA.
     */
    const val TIPO_DESPESA: Int = 0
    const val TIPO_RECEITA: Int = 1
    const val TIPO_APLICACAO: Int = 2


    /**
     * Constantes para a COLUNA_CLASSE_CONTA.
     * Mapeamento usado em ResumoTipoMensal.java
     */
    // Classes de DESPESA (Usadas quando TIPO_CONTA = TIPO_DESPESA)
    const val CLASSE_DESPESA_CARTAO: Int = 0
    const val CLASSE_DESPESA_FIXA: Int = 1
    const val CLASSE_DESPESA_VARIAVEL: Int = 2
    const val CLASSE_DESPESA_PRESTACOES: Int = 3

    // Classes de APLICAÇÃO (Usadas quando TIPO_CONTA = TIPO_APLICACAO)
    const val CLASSE_APLICACAO_FUNDOS: Int = 0
    const val CLASSE_APLICACAO_POUPANCA: Int = 1
    const val CLASSE_APLICACAO_RENDAVARIAVEL: Int = 2 // Novo valor de 2017
    const val CLASSE_APLICACAO_OUTRAS: Int = 3

    const val CATEGORIA_ALIMENTACAO: Int = 0
    const val CATEGORIA_EDUCACAO: Int = 1
    const val CATEGORIA_LAZER: Int = 2
    const val CATEGORIA_MORADIA: Int = 3
    const val CATEGORIA_SAUDE: Int = 4
    const val CATEGORIA_TRANSPORTE: Int = 5
    const val CATEGORIA_VESTUARIO: Int = 6
    const val CATEGORIA_OUTROS: Int = 7
    const val CATEGORIA_INVESTIMENTOS: Int = 8

    /**
     * Constantes para a COLUNA_PAGOU_CONTA.
     */
    const val STATUS_PAGO_RECEBIDO: String = "paguei"
    const val STATUS_PENDENTE: String = "falta"

    /**
     * Classe interna que define as constantes para as colunas da tabela de Contas.
     * Implementa BaseColumns para incluir colunas _ID.
     */
    object Colunas : BaseColumns {
        const val TABELA_NOME: String = "contasListadas"

        // Nomes das Colunas do Banco de Dados
        const val COLUNA_NOME_CONTA: String = "nome_conta"
        const val COLUNA_TIPO_CONTA: String = "tipo_conta"
        const val COLUNA_CLASSE_CONTA: String = "classe_conta"
        const val COLUNA_CATEGORIA_CONTA: String = "categoria_conta"
        const val COLUNA_DIA_DATA_CONTA: String = "dia_data"
        const val COLUNA_MES_DATA_CONTA: String = "mes_data"
        const val COLUNA_ANO_DATA_CONTA: String = "ano_data"
        const val COLUNA_VALOR_CONTA: String = "valor_conta"
        const val COLUNA_PAGOU_CONTA: String = "pagou_conta"
        const val COLUNA_QT_REPETICOES_CONTA: String = "qt_repeticoes"
        const val COLUNA_NR_REPETICAO_CONTA: String = "nr_repeticao"
        const val COLUNA_INTERVALO_CONTA: String = "intervalo_conta"
        const val COLUNA_CODIGO_CONTA: String = "codigo"
        const val COLUNA_VALOR_JUROS: String = "valor_juros"
    }

    /**
     * Classe interna que define o esquema da tabela 'notificacoes'.
     */
    object Notificacoes : BaseColumns {
        const val TABELA_NOME: String = "notificacoes"

        const val COLUNA_TITULO: String = "titulo"
        const val COLUNA_MENSAGEM: String = "mensagem"
        const val COLUNA_DATA: String = "data_criacao"
        const val COLUNA_LIDA: String = "lida"
        const val COLUNA_TIPO: String = "tipo_notificacao" // Para identificar a regra que gerou o alerta
    }
}
