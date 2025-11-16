package com.msk.minhascontas.info;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import java.lang.ref.WeakReference;
import android.app.Activity;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.ExportarExcel;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public class BarraProgresso extends AsyncTask<Void, Integer, Void> {

    private ProgressDialog progressDialog;
    private final WeakReference<Context> contextRef;
    private final String title;
    private final String message;
    private final DBContas dbMinhasContas;
    private final ExportarExcel excel = new ExportarExcel();
    private final int quantidade;
    private final int tempoEspera;
    private final String pastaBackUp;
    private final Resources res;
    private final NumberFormat dinheiro;

    public BarraProgresso(Context context, String title, String message,
                          int qt, int tempo, String pasta) {
        this.contextRef = new WeakReference<>(context); // Armazena a referência fraca
        this.title = title;
        this.message = message;
        this.quantidade = qt;
        this.tempoEspera = tempo;
        this.pastaBackUp = pasta;
        this.res = context.getResources();
        Locale current = res.getConfiguration().getLocales().get(0);
        this.dinheiro = NumberFormat.getCurrencyInstance(current);
        this.dbMinhasContas = DBContas.getInstance(context);
    }

    @Override
    protected void onPreExecute() {
        Context context = contextRef.get();

        // 1. Checagem de segurança (mantida do passo anterior)
        if (context == null || (context instanceof Activity && ((Activity) context).isFinishing())) {
            return; // Aborta a exibição se a Activity estiver indisponível ou finalizando
        }

        try {
            // 2. Inicialização do diálogo
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setMax(quantidade);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            // 3. Tenta mostrar o diálogo. A linha 64 é esta:
            progressDialog.show();
        } catch (Exception e) {
            // 4. Último recurso: Captura qualquer exceção (incluindo o WindowLeaked/BadTokenException)
            // que possa ocorrer devido a uma condição de corrida.
            // Isso previne o crash.
            Log.e("BarraProgresso", "Falha ao exibir ProgressDialog: " + e.getMessage());
        }

    }

    @Override
    protected Void doInBackground(Void... arg0) {
        if (!"mskapp".equals(pastaBackUp)) {
            CriaArquivoExcel();
        } else {
            try {
                for (int progress = 1; progress <= quantidade; progress++) {
                    if (tempoEspera == 0)
                        Thread.sleep(100);
                    else
                        Thread.sleep(tempoEspera);

                    publishProgress(progress);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // 1. Verifica se a tarefa foi cancelada (se mProgressTask.cancel(true) foi chamado em onDestroy)
        if (isCancelled()) {
            return;
        }

        // 2. Tenta obter a referência forte para o Context/Activity
        Context context = contextRef.get();

        // 3. Verifica se o Context é nulo OU se a Activity associada está finalizando
        if (context == null || (context instanceof Activity && ((Activity) context).isFinishing())) {
            return; // Aborta a operação se a Activity não estiver mais disponível
        }

        // 4. Se o contexto está OK, tenta fechar o diálogo
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                // Captura o erro para evitar crash, caso o WindowManager já tenha removido a view
                Log.e("BarraProgresso", "Falha ao fechar o diálogo: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... integers) {
        this.progressDialog.setProgress(integers[0]);
    }

    private void CriaArquivoExcel() {
        int ano = Calendar.getInstance().get(Calendar.YEAR);
        String[] jan, fev, mar, abr, mai, jun, jul, ago, set, out, nov, dez;

        jan = SaldoMensal(0, ano);
        publishProgress(10);
        fev = SaldoMensal(1, ano);
        mar = SaldoMensal(2, ano);
        publishProgress(20);
        abr = SaldoMensal(3, ano);
        mai = SaldoMensal(4, ano);
        publishProgress(40);
        jun = SaldoMensal(5, ano);
        jul = SaldoMensal(6, ano);
        publishProgress(50);
        ago = SaldoMensal(7, ano);
        set = SaldoMensal(8, ano);
        publishProgress(60);
        out = SaldoMensal(9, ano);
        nov = SaldoMensal(10, ano);
        publishProgress(70);
        dez = SaldoMensal(11, ano);

        String[] colunas = res.getStringArray(R.array.MesesDoAno);
        publishProgress(90);
        String[] linhas = NomeLinhas(); // NomeLinhas now returns String[]

        excel.CriaExcel(res.getString(R.string.planilha, String.format(
                        res.getConfiguration().getLocales().get(0), "%d", ano)), jan, fev,
                mar, abr, mai, jun, jul, ago, set, out, nov, dez, colunas,
                linhas, pastaBackUp);
        publishProgress(100);
    }

    // Removed unused getCursorCount method

    private double getSumFromCursor(Cursor cursor, String columnName) {
        double total = 0;
        // Use try-with-resources for automatic cursor closing
        try (cursor) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    do {
                        total += cursor.getDouble(columnIndex);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e("BarraProgresso", "Error getting sum from cursor", e);
        }
        return total;
    }

    private double somaContas(int tipo, int mes, int ano) {
        // Removed unused 'dia' parameter as it was always 0
        return getSumFromCursor(dbMinhasContas.buscaContasTipo(0, mes, ano, null, tipo), "valor");
    }

    private double somaContasPorClasse(int classe, int tipo, int mes, int ano) {
        return getSumFromCursor(dbMinhasContas.buscaContasClasse(0, mes, ano, null, tipo, classe), "valor");
    }

    private double somaContasPagas(int tipo, String pagamento, int mes, int ano) {
        // Removed unused 'dia' parameter as it was always 0
        return getSumFromCursor(dbMinhasContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, pagamento), "valor");
    }

    private String[] SaldoMensal(int mes, int ano) {
        String[] despesas = res.getStringArray(R.array.TipoDespesa);
        String[] receitas = res.getStringArray(R.array.TipoReceita);
        String[] aplicacoes = res.getStringArray(R.array.TipoAplicacao);

        int ajusteReceita = (receitas.length > 1) ? receitas.length : 0;
        int categorias = despesas.length + ajusteReceita + aplicacoes.length + 7;

        String[] valores = new String[categorias];
        double dvalor0, dvalor1;

        // DESPESAS
        dvalor0 = somaContas(0, mes, ano);
        valores[0] = dinheiro.format(dvalor0);
        for (int i = 0; i < despesas.length; i++) {
            valores[i + 1] = dinheiro.format(somaContasPorClasse(i, 0, mes, ano));
        }

        // RECEITAS
        dvalor1 = somaContas(1, mes, ano);
        valores[despesas.length + 1] = dinheiro.format(dvalor1);
        if (receitas.length > 1) {
            for (int j = 0; j < receitas.length; j++) {
                valores[j + despesas.length + 2] = dinheiro.format(somaContasPorClasse(j, 1, mes, ano));
            }
        }

        // APLICACOES
        valores[despesas.length + ajusteReceita + 2] = dinheiro.format(somaContas(2, mes, ano));
        for (int k = 0; k < aplicacoes.length; k++) {
            valores[k + despesas.length + ajusteReceita + 3] = dinheiro.format(somaContasPorClasse(k, 2, mes, ano));
        }

        // SALDO MENSAL
        valores[categorias - 4] = dinheiro.format(dvalor1 - dvalor0);

        // CONTAS PAGAS
        double pagas = somaContasPagas(0, DBContas.PAGAMENTO_PAGO, mes, ano);
        valores[categorias - 3] = dinheiro.format(pagas);

        // CONTAS A PAGAR
        valores[categorias - 2] = dinheiro.format(somaContasPagas(0, DBContas.PAGAMENTO_FALTA, mes, ano));

        // SALDO ATUAL
        valores[categorias - 1] = dinheiro.format(dvalor0 - pagas);

        return valores;
    }

    private String[] NomeLinhas() { // Changed return type to String[]
        String despesa = res.getString(R.string.linha_despesa);
        String receita = res.getString(R.string.linha_receita);
        String aplicacao = res.getString(R.string.linha_aplicacoes);

        String[] despesas = res.getStringArray(R.array.TipoDespesa);
        String[] receitas = res.getStringArray(R.array.TipoReceita);
        String[] aplicacoes = res.getStringArray(R.array.TipoAplicacao);

        int ajusteReceita = (receitas.length > 1) ? receitas.length : 0;

        int categorias = despesas.length + ajusteReceita + aplicacoes.length + 7;
        String[] linhas = new String[categorias];

        linhas[0] = despesa;
        System.arraycopy(despesas, 0, linhas, 1, despesas.length);

        linhas[despesas.length + 1] = receita;
        if (receitas.length > 1) {
            System.arraycopy(receitas, 0, linhas, despesas.length + 2, receitas.length);
        }

        linhas[despesas.length + ajusteReceita + 2] = aplicacao;
        System.arraycopy(aplicacoes, 0, linhas, despesas.length + ajusteReceita + 3, aplicacoes.length);

        linhas[categorias - 4] = res.getString(R.string.linha_saldo);
        linhas[categorias - 3] = res.getString(R.string.resumo_pagas);
        linhas[categorias - 2] = res.getString(R.string.resumo_faltam);
        linhas[categorias - 1] = res.getString(R.string.resumo_saldo);

        return linhas;
    }
}