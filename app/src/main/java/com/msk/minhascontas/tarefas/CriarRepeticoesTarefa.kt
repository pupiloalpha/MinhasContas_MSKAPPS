package com.msk.minhascontas.tarefas

import android.content.Context
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository

// A classe Conta
// O serviço de banco de dados
// Para acessar strings
class CriarRepeticoesTarefa(// Dados de ENTRADA para a tarefa
    private val contaOriginal: Conta, // As novas contas geradas (as repetições)
    private val repeticoes: MutableList<Conta?>
) : TarefaExecutavel {
    // Dados de SAÍDA (feedback)
    private var mensagemResultado: String? = null

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.titulo_criar_repeticoes) // Nova string
    }

    override fun getMensagemInicial(context: Context?): String? {
        if (context == null) return null
        // Exemplo: "Criando 12 repetições para 'Aluguel'..."
        return String.format(
            context.getString(R.string.msg_criando_repeticoes),
            repeticoes.size, contaOriginal.nome
        ) // Nova string
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        // Need to handle the nullable context here.
        if (context == null) return false

        val repository = ContasRepository.getInstance(context)

        // Filtra nulos e converte para lista não nula para o repositório
        val listaParaInserir = repeticoes.filterNotNull()
        
        // O repositório já trata a sincronização em nuvem (Firestore)
        val inseridas = repository.inserirContasEmMassa(listaParaInserir)

        if (inseridas > 0) {
            onProgress?.invoke(inseridas, repeticoes.size)
        }

        if (inseridas == listaParaInserir.size) {
            // Sucesso
            mensagemResultado = String.format(
                context.getString(R.string.dica_repeticoes_sucesso),
                inseridas
            ) // Nova string
            return true
        } else if (inseridas > 0) {
            // Sucesso Parcial
            mensagemResultado = String.format(
                context.getString(R.string.dica_repeticoes_parcial),
                inseridas,
                repeticoes.size
            ) // Nova string
            return true
        } else {
            // Falha
            mensagemResultado = context.getString(R.string.dica_repeticoes_falha) // Nova string
            return false
        }
    }

    override fun getMensagemResultado(context: Context?): String? {
        return mensagemResultado
    }

    override fun getQuantidadePassos(): Int {
        // A barra de progresso terá o número de repetições + 1 para a conta original.
        return repeticoes.size + 1
    }
}