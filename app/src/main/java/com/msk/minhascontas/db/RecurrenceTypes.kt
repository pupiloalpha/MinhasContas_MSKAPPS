package com.msk.minhascontas.db

/** Options for deleting recurring accounts. */
enum class TipoExclusao {
    SOMENTE_ESTA,      // Delete only this specific account instance
    DESTA_EM_DIANTE,   // Delete this account and all subsequent recurring accounts
    TODAS_AS_REPETICOES // Delete all recurring accounts in the series
}

/** Options for updating recurring accounts. */
enum class TipoAtualizacao {
    SOMENTE_ESTA,      // Update only this specific account instance
    DESTA_EM_DIANTE,   // Update this account and all subsequent recurring accounts (recalculating values)
    TODAS_AS_REPETICOES // Update all recurring accounts in the series (recalculating values)
}
