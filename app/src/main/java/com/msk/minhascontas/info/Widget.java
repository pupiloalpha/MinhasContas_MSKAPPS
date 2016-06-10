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

public class Widget extends AppWidgetProvider {

    RemoteViews remoteViews;
    ComponentName thisWidget;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_app);

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

        thisWidget = new ComponentName(context, Widget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, remoteViews);
    }

}
