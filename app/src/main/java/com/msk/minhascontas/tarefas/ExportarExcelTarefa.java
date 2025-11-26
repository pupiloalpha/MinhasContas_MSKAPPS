package com.msk.minhascontas.tarefas;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.excel.ExportarExcel;

import java.util.List;

public class ExportarExcelTarefa implements TarefaExecutavel {

    private static final String TAG = "ExportarExcelTarefa";

    private final Uri arquivoUri;
    private final int mesExportacao;
    private final int anoExportacao;
    private String mensagemResultado;
    private final ExportarExcel excel = new ExportarExcel(); // Instância do serviço Excel

    public ExportarExcelTarefa(Uri arquivoUri, int mesExportacao, int anoExportacao) {
        this.arquivoUri = arquivoUri;
        this.mesExportacao = mesExportacao;
        this.anoExportacao = anoExportacao;
    }

    @Override
    public String getTitulo(Context context) {
        return context.getString(R.string.exportar_excel);
    }

    @Override
    public String getMensagemInicial(Context context) {
        return context.getString(R.string.aguarde_exportacao);
    }

    @Override
    public int getQuantidadePassos() {
        return 100; // Valor fixo, pois o número de passos é difícil de prever
    }

    @Override
    public boolean executarTarefa(Context context) {
        DBContas dbMinhasContas = DBContas.getInstance(context);
        Cursor dadosDetalheCursor = null;
        boolean sucesso = false;

        try {
            // 1. Coleta os dados de resumo e nomes das linhas
            String[] valoresResumo = dbMinhasContas.coletaDadosResumo(context, mesExportacao, anoExportacao);
            String[] nomesLinhas = dbMinhasContas.NomeLinhas(context);

            // 2. Coleta os dados detalhados como Cursor
            dadosDetalheCursor = dbMinhasContas.listaContasCompleta(mesExportacao, anoExportacao);

            // 3. CONVERTE O CURSOR PARA LISTA DE CONTAS
            List<Conta> dadosDetalheLista = dbMinhasContas.cursorToListaContas(dadosDetalheCursor);

            // 4. Chama CriaExcel com a Lista
            String[] nomesColunas = dbMinhasContas.getNomeColunas();

            // 4. Chama CriaExcel com a Lista (AGORA COM 5 ARGUMENTOS)
            int erro = excel.CriaExcel(
                    context,
                    arquivoUri,
                    nomesLinhas,    // Argumento 3: Nomes das linhas (Aba RESUMO)
                    valoresResumo,  // Argumento 4: Valores do resumo (Aba RESUMO)
                    dadosDetalheLista // Argumento 5: Lista de objetos Conta (Aba DADOS)
            );

            if (erro == 0) {
                mensagemResultado = context.getString(R.string.dica_exporta_excel);
                sucesso = true;
            } else {
                mensagemResultado = context.getString(R.string.dica_erro_exporta_excel);
                sucesso = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro durante a exportação: " + e.getMessage(), e);
            mensagemResultado = context.getString(R.string.dica_erro_exporta_excel);
            sucesso = false;
        } finally {
            if (dadosDetalheCursor != null) {
                dadosDetalheCursor.close();
            }
        }
        return sucesso;
    }

    @Override
    public String getMensagemResultado(Context context) {
        return mensagemResultado;
    }
}