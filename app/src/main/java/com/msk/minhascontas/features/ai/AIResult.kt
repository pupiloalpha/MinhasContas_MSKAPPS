package com.msk.minhascontas.features.ai

/**
 * Representa o resultado de uma operação da Assistente de IA.
 */
sealed class AIResult {
    /**
     * Indica que a operação foi concluída com sucesso.
     * @param content O conteúdo retornado pela IA (geralmente HTML limpo).
     */
    data class Success(val content: String) : AIResult()

    /**
     * Indica que ocorreu um erro durante a operação.
     * @param message A mensagem de erro amigável.
     * @param fullPrompt O prompt que causou o erro (para fins de depuração).
     */
    data class Error(val message: String, val fullPrompt: String = "") : AIResult()
}
