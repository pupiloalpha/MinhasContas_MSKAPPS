package com.msk.minhascontas.db

/**
 * Representa os dados brutos para o resumo financeiro mensal.
 */
data class ReportSummary(
    val despesasPorCategoria: Map<Int, Double>,
    val receitasPorCategoria: Map<Int, Double>,
    val aplicacoesPorClasse: Map<Int, Double>,
    val receitasPorDia: Map<Int, Double>,
    val despesasPorDia: Map<Int, Double>,
    val totalDespesasPagas: Double,
    val totalDespesasPendentes: Double,
    val totalReceitasRecebidas: Double,
    val totalReceitasPendentes: Double,
    val totalAplicacoes: Double,
    val saldo: Double
)
