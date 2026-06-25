package com.msk.minhascontas.db

import java.util.UUID

/**
 * POJO que representa uma Meta Financeira estratégica.
 * Removido do Room para evitar erros de migração em produção.
 */
data class MetaFinanceira(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val tipoMeta: Int, 
    val valorObjetivo: Double,
    val valorAtual: Double = 0.0,
    val taxaJurosMensal: Double = 0.0,
    val aporteMensalAlvo: Double = 0.0,
    val dataInicio: Long = System.currentTimeMillis(),
    val dataPrevisaoFim: Long? = null,
    val ativa: Boolean = true,
    val codigoVinculo: String? = null,
    val prioridade: Int = 0
) {
    companion object {
        const val TIPO_DIVIDA = 0
        const val TIPO_RESERVA = 1
        const val TIPO_INVESTIMENTO = 2
        const val TIPO_APOSENTADORIA = 3
    }
}
