package com.msk.minhascontas.features.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.db.CriarConta
import com.msk.minhascontas.features.listas.PesquisaContas

class AddContaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_barra)

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        
        // Responsividade
        if (minWidth < 140) { 
            views.setViewVisibility(R.id.tvWidgetTitulo, View.GONE)
        } else {
            views.setViewVisibility(R.id.tvWidgetTitulo, View.VISIBLE)
        }

        // Cor dos Ícones: Branco (on_primary) para contrastar com o fundo Verde (primary)
        val onPrimaryColor = ContextCompat.getColor(context, R.color.on_primary)
        views.setInt(R.id.ibAdicionaConta, "setColorFilter", onPrimaryColor)
        views.setInt(R.id.ibPesquisaConta, "setColorFilter", onPrimaryColor)

        // Configurar Intents
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val intentApp = Intent(context, MinhasContas::class.java)
        views.setOnClickPendingIntent(R.id.tvAbreAplicativo, 
            PendingIntent.getActivity(context, appWidgetId + 301, intentApp, flag))

        val intentAdd = Intent(context, CriarConta::class.java)
        views.setOnClickPendingIntent(R.id.ibAdicionaConta, 
            PendingIntent.getActivity(context, appWidgetId + 401, intentAdd, flag))

        val intentSearch = Intent(context, PesquisaContas::class.java)
        views.setOnClickPendingIntent(R.id.ibPesquisaConta, 
            PendingIntent.getActivity(context, appWidgetId + 501, intentSearch, flag))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED || 
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AddContaWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
