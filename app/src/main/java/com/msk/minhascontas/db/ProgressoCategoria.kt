package com.msk.minhascontas.db

/**
 * Representa o progresso de uma categoria de gastos em relação ao planejado.
 */
data class ProgressoCategoria(
    val index: Int,
    val nome: String,
    val valorPlanejado: Double,
    val valorReal: Double,
    val corRes: Int,
    val onCorRes: Int
)
