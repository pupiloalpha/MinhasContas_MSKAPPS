package com.msk.minhascontas.tarefas

import android.content.Context
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas.TipoAtualizacao
import com.msk.minhascontas.features.widget.ResumoMensalWidgetProvider

/**
 * Tarefa executável para atualizar uma ou mais contas no banco de dados.
 * Utiliza BarraProgresso para exibir o progresso.
 */
class AtualizarContaTarefa(
    private val contaParaAtualizar: Conta,
    private val tipoAtualizacao: TipoAtualizacao?
) : TarefaExecutavel {
    private var atualizacaoBemSucedida = false

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.titulo_atualizando_contas)
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.mensagem_atualizando_contas)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        if (context == null) return false
        val repository = ContasRepository.getInstance(context)
        try {
            if (tipoAtualizacao == TipoAtualizacao.SOMENTE_ESTA) {
                repository.atualizarConta(contaParaAtualizar)
            } else if (tipoAtualizacao != null) {
                // Ao atualizar contas recorrentes, a contaParaAtualizar já contém
                // as informações da "conta base" para a atualização (incluindo nRepete, qtRepete, etc.)
                repository.atualizarContasRecorrentes(contaParaAtualizar, tipoAtualizacao)
            }
            atualizacaoBemSucedida = true

            onProgress?.invoke(1, 1)

            // Notificar Widgets
            ResumoMensalWidgetProvider.updateAllWidgets(context)

            return true
        } catch (e: Exception) {
            Log.e("AtualizarContaTarefa", "Erro ao atualizar conta(s): " + e.message)
            atualizacaoBemSucedida = false
            return false
        }
    }

    override fun getMensagemResultado(context: Context?): String? {
        if (context == null) return null
        return if (atualizacaoBemSucedida) {
            String.format(
                context.getString(R.string.dica_conta_alterada),
                contaParaAtualizar.nome
            )
        } else {
            context.getString(R.string.erro_atualizar_conta)
        }
    }

    override fun getQuantidadePassos(): Int {
        return 1
    }
}