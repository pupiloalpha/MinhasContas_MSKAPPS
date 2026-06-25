package com.msk.minhascontas.tarefas

import android.content.Context

/**
 * Interface que define o contrato para qualquer tarefa assíncrona que será
 * executada pela BarraProgresso (Executor Genérico).
 */
interface TarefaExecutavel {
    /**
     * Define o título que será exibido na barra de progresso.
     */
    fun getTitulo(context: Context?): String?

    /**
     * Define a mensagem inicial exibida na barra de progresso.
     */
    fun getMensagemInicial(context: Context?): String?

    /**
     * Onde a lógica principal da tarefa será executada em background.
     * @param context O contexto atual.
     * @param onProgress Callback opcional para relatar o progresso (atual, total).
     * @return true se a execução foi bem-sucedida, false caso contrário.
     */
    fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)? = null): Boolean

    /**
     * Retorna a mensagem final (sucesso/falha) para ser exibida ao usuário (Toast).
     */
    fun getMensagemResultado(context: Context?): String?

    /**
     * Retorna o número de passos de progresso. Usado para inicializar a barra.
     */
    fun getQuantidadePassos(): Int
}
