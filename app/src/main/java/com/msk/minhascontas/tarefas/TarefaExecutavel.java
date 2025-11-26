package com.msk.minhascontas.tarefas;

import android.content.Context;

/**
 * Interface que define o contrato para qualquer tarefa assíncrona que será
 * executada pela BarraProgresso (Executor Genérico).
 */
public interface TarefaExecutavel {

    /**
     * Define o título que será exibido na barra de progresso.
     */
    String getTitulo(Context context);

    /**
     * Define a mensagem inicial exibida na barra de progresso.
     */
    String getMensagemInicial(Context context);

    /**
     * Onde a lógica principal da tarefa será executada em background.
     * @param context O contexto atual.
     * @return true se a execução foi bem-sucedida, false caso contrário.
     */
    boolean executarTarefa(Context context);

    /**
     * Retorna a mensagem final (sucesso/falha) para ser exibida ao usuário (Toast).
     */
    String getMensagemResultado(Context context);

    /**
     * Retorna o número de passos de progresso. Usado para inicializar a barra.
     */
    int getQuantidadePassos();
}