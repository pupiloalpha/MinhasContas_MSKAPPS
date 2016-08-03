package com.msk.minhascontas.info;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.ExportarExcel;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class BarraProgresso extends AsyncTask<Void, Integer, Void> {

    private ProgressDialog progressDialog = null;
    private String title = null, message = null;
    private Context context = null;
    private DBContas dbMinhasContas;
    private ExportarExcel excel = new ExportarExcel();
    private int quantidade = 0, tempoEspera = 0;
    private Resources res = null;
    private NumberFormat dinheiro;
    private int erro, categorias, ajusteReceita;
    private String[] linhas;
    private String despesa, receita, aplicacao, pastaBackUp;
    private String[] despesas, receitas, aplicacoes;

    public BarraProgresso(Context context, String title, String message,
                          int qt, int tempo, String pasta) {
        this.context = context;
        this.title = title;
        this.message = message;
        quantidade = qt;
        tempoEspera = tempo;
        pastaBackUp = pasta;
        res = context.getResources();
        Locale current = res.getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);
        dbMinhasContas = new DBContas(context);
    }

    @Override
    protected void onPreExecute() {
        this.progressDialog = new ProgressDialog(context);
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setTitle(this.title);
        this.progressDialog.setMessage(this.message);
        this.progressDialog.setMax(this.quantidade);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        if (!pastaBackUp.equals("mskapp")) {
            dbMinhasContas.open();
            CriaArquivoExcel();
            dbMinhasContas.close();
        } else {
            try {
                for (int progress = 1; progress <= quantidade; progress++) {
                    if (tempoEspera == 0)
                        Thread.sleep(100);
                    else
                        Thread.sleep(tempoEspera);

                    publishProgress(progress);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        try {
            this.progressDialog.dismiss();
            this.progressDialog = null;
        } catch (Exception e) {
            // nothing
        }
    }

    @Override
    protected void onProgressUpdate(Integer... integers) {
        this.progressDialog.setProgress(integers[0]);
    }

    private void CriaArquivoExcel() {

        // COLOCA VALORES DE DADOS NOS VETORES
        int ano = Calendar.getInstance().get(Calendar.YEAR);
        String[] jan, fev, mar, abr, mai, jun, jul, ago, set, out, nov,
                dez;

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
        NomeLinhas(); // DEFINE O NOME DAS LINHAS DA TABELA

        erro = excel.CriaExcel(res.getString(R.string.planilha, String.format(
                res.getConfiguration().locale, "%d", ano)), jan, fev,
                mar, abr, mai, jun, jul, ago, set, out, nov, dez, colunas,
                linhas, pastaBackUp);
        publishProgress(100);
    }

    private String[] SaldoMensal(int mes, int ano) {

        // DEFINE OS NOMES DA LINHAS DA TABELA
        despesas = res.getStringArray(R.array.TipoDespesa);
        receitas = res.getStringArray(R.array.TipoReceita);
        aplicacoes = res.getStringArray(R.array.TipoAplicacao);

        // AJUSTE QUANDO EXISTE APENAS UMA RECEITA
        if (receitas.length > 1)
            ajusteReceita = receitas.length;
        else
            ajusteReceita = 0;

        categorias = despesas.length + ajusteReceita
                + aplicacoes.length + 7;

        String[] valores = new String[categorias];
        double dvalor0, dvalor1;

        // PREENCHE OS VALORES DE DESPESAS
        if (dbMinhasContas.quantasContasPorTipo(0, 0, mes, ano) > 0) {
            valores[0] = dinheiro.format(dbMinhasContas.somaContas(0, 0, mes, ano));
            dvalor0 = dbMinhasContas.somaContas(0, 0, mes, ano);
        } else {
            valores[0] = dinheiro.format(0.0D);
            dvalor0 = 0.0D;
        }
        for (int i = 0; i < despesas.length; i++) {
            if (dbMinhasContas.quantasContasPorClasse(i, 0, mes, ano) > 0)
                valores[i + 1] = dinheiro.format(dbMinhasContas.somaContasPorClasse(
                        i, 0, mes, ano));
            else
                valores[i + 1] = dinheiro.format(0.0D);
        }
        // VALORES DE RECEITAS
        if (dbMinhasContas.quantasContasPorTipo(1, 0, mes, ano) > 0) {
            valores[despesas.length + 1] = dinheiro.format(dbMinhasContas.somaContas(
                    1, 0, mes, ano));
            dvalor1 = dbMinhasContas.somaContas(1, 0, mes, ano);
        } else {
            valores[despesas.length + 1] = dinheiro.format(0.0D);
            dvalor1 = 0.0D;
        }
        if (receitas.length > 1)
            for (int j = 0; j < receitas.length; j++) {
                if (dbMinhasContas.quantasContasPorClasse(
                        j, 0, mes, ano) > 0)
                    valores[j + despesas.length + 2] = dinheiro.format(
                            dbMinhasContas.somaContasPorClasse(j, 0, mes, ano));
                else
                    valores[j + despesas.length + 2] = dinheiro.format(0.0D);
            }
        // VALORES DE APLICACOES
        if (dbMinhasContas.quantasContasPorTipo(2, 0, mes, ano) > 0)
            valores[despesas.length + ajusteReceita + 2] = dinheiro.format(
                    dbMinhasContas.somaContas(2, 0, mes, ano));
        else
            valores[despesas.length + ajusteReceita + 2] = dinheiro.format(0.0D);
        for (int k = 0; k < aplicacoes.length; k++) {
            if (dbMinhasContas.quantasContasPorClasse(k, 0, mes, ano) > 0)
                valores[k + despesas.length + ajusteReceita + 3] = dinheiro.format(
                        dbMinhasContas.somaContasPorClasse(k, 0, mes, ano));
            else
                valores[k + despesas.length + ajusteReceita + 3] = dinheiro.format(0.0D);

        }

        // VALOR DO SALDO MENSAL
        valores[categorias - 4] = dinheiro.format(dvalor1 - dvalor0);

        // VALOR CONTAS PAGAS
        if (dbMinhasContas.quantasContasPagasPorTipo(0, "paguei", 0, mes, ano) > 0) {
            valores[categorias - 3] = dinheiro.format(dbMinhasContas.somaContasPagas(
                    0, "paguei", 0, mes, ano));
            dvalor1 = dbMinhasContas.somaContasPagas(0, "paguei", 0, mes, ano);
        } else {
            valores[categorias - 3] = dinheiro.format(0.0D);
            dvalor1 = 0.0D;
        }

        // VALOR CONTAS A PAGAR
        if (dbMinhasContas.quantasContasPagasPorTipo(0, "falta", 0, mes, ano) > 0)
            valores[categorias - 2] = dinheiro.format(dbMinhasContas.somaContasPagas(
                    0, "falta", 0, mes, ano));
        else
            valores[categorias - 2] = dinheiro.format(0.0D);

        // VALOR DO SALDO ATUAL
        valores[categorias - 1] = dinheiro.format(dvalor0 - dvalor1);

        return valores;
    }

    private void NomeLinhas() {
        // DEFINE OS NOMES DA LINHAS DA TABELA
        despesa = res.getString(R.string.linha_despesa);
        receita = res.getString(R.string.linha_receita);
        aplicacao = res.getString(R.string.linha_aplicacoes);

        // AJUSTE QUANDO EXISTE APENAS UMA RECEITA
        if (receitas.length > 1)
            ajusteReceita = receitas.length;
        else
            ajusteReceita = 0;

        categorias = despesas.length + ajusteReceita
                + aplicacoes.length + 7;
        linhas = new String[categorias];

        // PREENCHE AS LINHAS DA TABELA
        linhas[0] = despesa;
        for (int i = 0; i < despesas.length; i++) {
            linhas[i + 1] = despesas[i];
        }
        // VALORES DE RECEITAS
        linhas[despesas.length + 1] = receita;
        if (receitas.length > 1)
            for (int j = 0; j < receitas.length; j++) {
                linhas[j + despesas.length + 2] = receitas[j];
            }
        // VALORES DE APLICACOES
        linhas[despesas.length + ajusteReceita + 2] = aplicacao;
        for (int k = 0; k < aplicacoes.length; k++) {
            linhas[k + despesas.length + ajusteReceita + 3] = aplicacoes[k];
        }

        // VALOR DO SALDO MENSAL
        linhas[categorias - 4] = res.getString(R.string.linha_saldo);

        // VALOR CONTAS PAGAS E A PAGAR
        linhas[categorias - 3] = res.getString(R.string.resumo_pagas);

        linhas[categorias - 2] = res.getString(R.string.resumo_faltam);

        // VALOR DO SALDO ATUAL
        linhas[categorias - 1] = res.getString(R.string.resumo_saldo);
    }
}