package com.msk.minhascontas.info;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.CriarConta;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.listas.PesquisaContas;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class WidgetResumo extends AppWidgetProvider {

    private RemoteViews remoteViews;
    private ComponentName thisWidget;
    private AppWidgetManager manager;

    private SharedPreferences buscaPreferencias = null;

    private Calendar c = Calendar.getInstance();

    private DBContas dbContas;

    private Boolean somaSaldo = false;

    private double[] valores;
    private double rec, desp, mes_ant;
    private int mes, ano;

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

        Intent addConta = new Intent(context, CriarConta.class);
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

        Locale current = context.getResources().getConfiguration().locale;
        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

        remoteViews.setTextViewText(R.id.tvValorSaldoAtual, dinheiro.format(valores[3]));
        remoteViews.setTextViewText(R.id.tvValorReceitas, dinheiro.format(valores[0]));
        remoteViews.setTextViewText(R.id.tvValorDespesas, dinheiro.format(valores[1]));
        remoteViews.setTextViewText(R.id.tvValorAplicacoes, dinheiro.format(valores[2]));
        if (valores[3] < 0)
            remoteViews.setTextColor(R.id.tvValorSaldoAtual, Color.parseColor("#CC0000"));


        thisWidget = new ComponentName(context, WidgetResumo.class);
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
        dbContas.open();
        Cursor somador = null;

        // PREENCHE AS LINHAS DA TABELA

        // VALORES DE RECEITAS
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 1);
        if (somador.getCount() > 0)
            valores[0] = SomaContas(somador);
        else
            valores[0] = 0.0D;

        // VALORES DE DESPESAS
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 0);
        if (somador.getCount() > 0)
            valores[1] = SomaContas(somador);
        else
            valores[1] = 0.0D;

        // VALORES DE APLICACOES
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 2);
        if (somador.getCount() > 0)
            valores[2] = SomaContas(somador);
        else
            valores[2] = 0.0D;

        // VALOR SALDO ATUAL

        // VALOR RECEITAS RECEBIDAS
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 1, "paguei");
        if (somador.getCount() > 0)
            rec = SomaContas(somador);
        else
            rec = 0.0D;

        // VALOR CONTAS PAGAS
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 0, "paguei");
        if (somador.getCount() > 0)
            desp = SomaContas(somador);
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
        somador = dbContas.buscaContasTipo(0, mes_anterior, ano_anterior, null, 1);
        if (somador.getCount() > 0)
            r = SomaContas(somador);

        double d = 0.0D; // DESPESA MES ANTERIOR
        somador = dbContas.buscaContasTipo(0, mes_anterior, ano_anterior, null, 0);
        if (somador.getCount() > 0)
            d = SomaContas(somador);

        somador.close(); // FECHA O CURSOR DO SOMADOR

        double s = r - d; // SALDO MES ANTERIOR
        if (dbContas.quantasContasPorMes(mes_anterior, ano_anterior) > 0)
            mes_ant = s;
        else
            mes_ant = 0.0D;

        // VALOR DO SALDO ATUAL

        if (somaSaldo) {
            valores[3] = rec - desp + mes_ant;
        } else {
            valores[3] = rec - desp;
        }

        dbContas.close();


    }

    private double SomaContas(Cursor cursor) {
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        for (int j = 0; ; j++) {
            if (j >= i) {
                cursor.close();
                return d;
            }
            d += cursor.getDouble(9);
            cursor.moveToPrevious();
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        AtualizaSaldo(context);
        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_resumo);
    }

}