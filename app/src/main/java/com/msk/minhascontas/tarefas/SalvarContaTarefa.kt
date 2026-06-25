package com.msk.minhascontas.tarefas

import android.content.Context
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.widget.ResumoMensalWidgetProvider

/**
 * Tarefa executável para salvar uma ou mais contas no banco de dados.
 * Utiliza BarraProgresso para exibir o progresso.
 */
class SalvarContaTarefa(
    private val contaParaSalvar: Conta,
    private val qtRepeteTask: Int,
    private val intervaloTask: Int
) : TarefaExecutavel {
    var isSalvamentoBemSucedido: Boolean = false // Flag para comunicar o sucesso
        private set

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.titulo_salvando_contas)
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.mensagem_salvando_contas)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        if (context == null) return false
        val repository = ContasRepository.getInstance(context)

        try {
            if (qtRepeteTask <= 1) {
                repository.salvarConta(contaParaSalvar)
                onProgress?.invoke(1, 1)
            } else {
                repository.salvarContasRecorrentes(contaParaSalvar, qtRepeteTask, intervaloTask)
                // Como salvarContasRecorrentes é síncrona e interna, 
                // relatamos o fim do processo se não pudermos monitorar passo a passo.
                onProgress?.invoke(qtRepeteTask, qtRepeteTask)
            }
            this.isSalvamentoBemSucedido = true
            
            // Notificar Widgets
            ResumoMensalWidgetProvider.updateAllWidgets(context)

            return true
        } catch (e: Exception) {
            Log.e("SalvarContaTarefa", "Erro ao salvar conta(s): " + e.message)
            this.isSalvamentoBemSucedido = false
            return false
        }
    }

    override fun getMensagemResultado(context: Context?): String? {
        return if (this.isSalvamentoBemSucedido) {
            context?.getString(R.string.dica_conta_criada)?.let {
                String.format(it, contaParaSalvar.nome)
            }
        } else {
            context?.getString(R.string.erro_salvar_conta)
        }
    }

    override fun getQuantidadePassos(): Int {
        // Se você quer que a barra de progresso mostre cada repetição como um passo, use qtRepeteTask.
        // Se for um único processo de salvamento (mesmo que crie várias contas internamente), use 1.
        return if (qtRepeteTask > 1) qtRepeteTask else 1
    }
}