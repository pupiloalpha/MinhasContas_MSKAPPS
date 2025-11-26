// ImportarExcelTarefa.java
package com.msk.minhascontas.tarefas;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.excel.ImportarExcel;

import java.util.List;

public class ImportarExcelTarefa implements TarefaExecutavel {

    private static final String TAG = "ImportarExcelTarefa";

    private final Uri arquivoUri;
    private String mensagemResultado;
    private int contasInseridas = 0;
    private final ImportarExcel importador = new ImportarExcel();

    public ImportarExcelTarefa(Uri arquivoUri) {
        this.arquivoUri = arquivoUri;
    }

    @Override
    public String getTitulo(Context context) {
        return context.getString(R.string.importar_excel);
    }

    @Override
    public String getMensagemInicial(Context context) {
        return context.getString(R.string.aguarde_importacao);
    }

    @Override
    public int getQuantidadePassos() {
        return 100; // Estimativa de passos
    }

    @Override
    public boolean executarTarefa(Context context) {
        DBContas dbMinhasContas = DBContas.getInstance(context);
        boolean sucesso = false;

        try {
            // CORREÇÃO: 1. Chama o novo método lerExcel que retorna a lista de Contas
            List<Conta> contasParaImportar = importador.lerExcel(context, arquivoUri);

            // Verifica se houve falha na leitura (ex: erro de I/O, formato inválido)
            if (contasParaImportar == null) {
                mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou);
                return false; // Falha na execução da tarefa
            }

            // Verifica se o arquivo foi lido, mas estava vazio
            if (contasParaImportar.isEmpty()) {
                mensagemResultado = context.getString(R.string.dica_importacao_vazia);
                return true; // Sucesso (o arquivo foi lido, mas estava vazio)
            }

            // 2. Insere as contas em massa no banco de dados
            contasInseridas = dbMinhasContas.inserirContasEmMassa(contasParaImportar);

            if (contasInseridas > 0) {
                mensagemResultado = String.format(context.getString(R.string.dica_importacao_sucesso), contasInseridas);
                sucesso = true;
            } else {
                // Caso o arquivo tenha contas, mas a inserção em massa retorne 0
                mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou);
                sucesso = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro durante a importação: " + e.getMessage(), e);
            mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou);
            sucesso = false;
        }
        return sucesso;
    }

    @Override
    public String getMensagemResultado(Context context) {
        return mensagemResultado;
    }
}