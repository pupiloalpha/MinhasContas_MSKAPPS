package com.msk.minhascontas.info;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.CriarNovaConta;
import com.msk.minhascontas.listas.PesquisaContas;

/**
 * Implementation of App Widget functionality.
 */
public class WidgetBarra extends AppWidgetProvider {



    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views;
        ComponentName thisWidget;

        views = new RemoteViews(context.getPackageName(),
                R.layout.widget_app);

        Intent launchActivity = new Intent(context, MinhasContas.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                launchActivity, 0);
        views.setOnClickPendingIntent(R.id.tvAbreAplciativo, pendingIntent);

        Intent addConta = new Intent(context, CriarNovaConta.class);
        addConta.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent piAdd = PendingIntent
                .getActivity(context, 0, addConta, 0);

        views.setOnClickPendingIntent(R.id.ibAdicionaConta, piAdd);

        Intent pesquisaContas = new Intent(context, PesquisaContas.class);
        pesquisaContas.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent piPesquisa = PendingIntent.getActivity(context, 0,
                pesquisaContas, 0);

        views.setOnClickPendingIntent(R.id.ibPesquisaConta, piPesquisa);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

