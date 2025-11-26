package com.msk.minhascontas.tarefas;

import android.content.Context;
import com.msk.minhascontas.db.Conta; // A classe Conta
import com.msk.minhascontas.db.DBContas; // O serviço de banco de dados
import com.msk.minhascontas.R; // Para acessar strings

import java.util.List;

public class CriarRepeticoesTarefa implements TarefaExecutavel {

    // Dados de ENTRADA para a tarefa
    private final Conta contaOriginal;
    private final List<Conta> repeticoes; // As novas contas geradas (as repetições)

    // Dados de SAÍDA (feedback)
    private String mensagemResultado;

    /**
     * Construtor que recebe os dados necessários para a tarefa.
     * @param contaOriginal A conta base a ser repetida.
     * @param repeticoes A lista das contas (repetições) a serem inseridas.
     */
    public CriarRepeticoesTarefa(Conta contaOriginal, List<Conta> repeticoes) {
        this.contaOriginal = contaOriginal;
        this.repeticoes = repeticoes;
    }

    @Override
    public String getTitulo(Context context) {
        return context.getString(R.string.titulo_criar_repeticoes); // Nova string
    }

    @Override
    public String getMensagemInicial(Context context) {
        // Exemplo: "Criando 12 repetições para 'Aluguel'..."
        return String.format(context.getString(R.string.msg_criando_repeticoes),
                repeticoes.size(), contaOriginal.getNome()); // Nova string
    }

    @Override
    public boolean executarTarefa(Context context) {
        DBContas db = DBContas.getInstance(context);

        // Assumindo que DBContas terá um método para inserir várias contas (como inserirContasEmMassa)
        int inseridas = db.inserirContasEmMassa(repeticoes);

        if (inseridas == repeticoes.size()) {
            // Sucesso
            mensagemResultado = String.format(context.getString(R.string.dica_repeticoes_sucesso), inseridas); // Nova string
            return true;
        } else if (inseridas > 0) {
            // Sucesso Parcial
            mensagemResultado = String.format(context.getString(R.string.dica_repeticoes_parcial), inseridas, repeticoes.size()); // Nova string
            return true;
        } else {
            // Falha
            mensagemResultado = context.getString(R.string.dica_repeticoes_falha); // Nova string
            return false;
        }
    }

    @Override
    public String getMensagemResultado(Context context) {
        return mensagemResultado;
    }

    @Override
    public int getQuantidadePassos() {
        // A barra de progresso terá o número de repetições + 1 para a conta original.
        return repeticoes.size() + 1;
    }
}