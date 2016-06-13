package com.msk.minhascontas.info;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.CriarNovaConta;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.listas.PesquisaContas;

import java.util.Calendar;

public class NewWidget extends AppWidgetProvider {

    RemoteViews remoteViews;
    ComponentName thisWidget;
    AppWidgetManager manager;

    SharedPreferences buscaPreferencias = null;

    Calendar c = Calendar.getInstance();

    DBContas dbContas;

    Boolean somaSaldo = false;

    String despesa, receita, aplicacao;
    double[] valores;
    double rec, desp, mes_ant;
    int mes, ano;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_resumo);

        Intent launchActivity = new Intent(context, MinhasContas.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                launchActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.tvAbreAplciativo, pendingIntent);

        Intent addConta = new Intent(context, CriarNovaConta.class);
        addConta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent piAdd = PendingIntent
                .getActivity(context, 0, addConta, 0);

        remoteViews.setOnClickPendingIntent(R.id.ibAdicionaConta, piAdd);

        Intent pesquisaContas = new Intent(context, PesquisaContas.class);
        pesquisaContas.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent piPesquisa = PendingIntent.getActivity(context, 0,
                pesquisaContas, 0);

        remoteViews.setOnClickPendingIntent(R.id.ibPesquisaConta, piPesquisa);

        AtualizaSaldo(context);

        remoteViews.setTextViewText(R.id.tvValorSaldoAtual, context.getString(
                R.string.dica_dinheiro,
                String.format("%.2f", valores[3])));
        remoteViews.setTextViewText(R.id.tvValorReceitas, context.getString(
                R.string.dica_dinheiro,
                String.format("%.2f", valores[0])));
        remoteViews.setTextViewText(R.id.tvValorDespesas, context.getString(
                R.string.dica_dinheiro,
                String.format("%.2f", valores[1])));
        remoteViews.setTextViewText(R.id.tvValorAplicacoes, context.getString(
                R.string.dica_dinheiro,
                String.format("%.2f", valores[2])));
        if (valores[3] < 0)
            remoteViews.setTextColor(R.id.tvValorSaldoAtual, Color.parseColor("#CC0000"));


        thisWidget = new ComponentName(context, NewWidget.class);
        manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, remoteViews);
    }

    private void AtualizaSaldo(Context context) {

        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(context);
        somaSaldo = buscaPreferencias.getBoolean("saldo", false);

        c = Calendar.getInstance();
        ano = c.get(Calendar.YEAR);
        mes = c.get(Calendar.MONTH);

        valores = new double[4];

        dbContas = new DBContas(context);

        despesa = context.getString(R.string.linha_despesa);
        receita = context.getString(R.string.linha_receita);
        aplicacao = context.getString(R.string.linha_aplicacoes);

        dbContas.open();

        // VALOR DE RECEITAS
        if (dbContas.quantasContasPorTipo(receita, 0, mes, ano) > 0)
            valores[0] = dbContas.somaContas(receita, 0, mes, ano);
        else
            valores[0] = 0.0D;

        // VALOR DE DESPESAS
        if (dbContas.quantasContasPorTipo(despesa, 0, mes, ano) > 0)
            valores[1] = dbContas.somaContas(despesa, 0, mes, ano);
        else
            valores[1] = 0.0D;

        // VALOR DE APLICACOES
        if (dbContas.quantasContasPorTipo(aplicacao, 0, mes, ano) > 0)
            valores[2] = dbContas.somaContas(aplicacao, 0, mes, ano);
        else
            valores[2] = 0.0D;

        // VALOR SALDO ATUAL

        if (dbContas.quantasContasPagasPorTipo(receita, "paguei", 0, mes, ano) > 0)
            rec = dbContas.somaContasPagas(receita, "paguei", 0, mes,
                    ano);
        else
            rec = 0.0D;

        if (dbContas.quantasContasPagasPorTipo(despesa, "paguei", 0, mes, ano) > 0)
            desp = dbContas.somaContasPagas(despesa, "paguei", 0,
                    mes, ano);
        else
            desp = 0.0D;

        // VALOR DO SALDO DO MES ANTERIOR

        int mes_anterior = mes - 1; // DEFINE MES ANTERIOR
        int ano_anterior = ano;
        if (mes_anterior < 0) {
            mes_anterior = 11;
            ano_anterior = ano_anterior - 1;
        }
        double r = 0.0D; // RECEITA MES ANTERIOR
        if (dbContas.quantasContasPorTipo(receita, 0, mes_anterior,
                ano_anterior) > 0)
            r = dbContas.somaContas(receita, 0, mes_anterior, ano_anterior);

        double d = 0.0D; // DESPESA MES ANTERIOR
        if (dbContas.quantasContasPorTipo(despesa, 0, mes_anterior,
                ano_anterior) > 0)
            d = dbContas.somaContas(despesa, 0, mes_anterior, ano_anterior);

        double s = r - d; // SALDO MES ANTERIOR
        if (dbContas.quantasContasPorMes(mes_anterior, ano_anterior) > 0)
            mes_ant = s;
        else
            mes_ant = 0.0D;

        // VALOR DO SALDO ATUAL

        if (somaSaldo == true) {
            valores[3] = rec - desp + mes_ant;
        } else {
            valores[3] = rec - desp;
        }

        dbContas.close();


    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        AtualizaSaldo(context);
        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_resumo);
    }

}