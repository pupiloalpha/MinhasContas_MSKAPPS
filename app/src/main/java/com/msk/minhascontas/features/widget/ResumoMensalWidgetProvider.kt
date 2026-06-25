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
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.CriarConta
import com.msk.minhascontas.features.listas.PesquisaContas
import java.text.NumberFormat
import java.util.*

class ResumoMensalWidgetProvider : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_resumo)

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        // Responsividade Horizontal
        if (minWidth < 140) {
            views.setViewVisibility(R.id.tvWidgetTitulo, View.GONE)
        } else {
            views.setViewVisibility(R.id.tvWidgetTitulo, View.VISIBLE)
        }

        // Responsividade Vertical
        if (minHeight < 80) {
            views.setViewVisibility(R.id.llSecaoDetalhes, View.GONE)
            views.setViewVisibility(R.id.divisorWidget, View.GONE)
        } else {
            views.setViewVisibility(R.id.llSecaoDetalhes, View.VISIBLE)
            views.setViewVisibility(R.id.divisorWidget, View.VISIBLE)
        }

        if (minHeight < 50) {
            views.setViewVisibility(R.id.llConteudoResumo, View.GONE)
        } else {
            views.setViewVisibility(R.id.llConteudoResumo, View.VISIBLE)
        }

        val calendar = Calendar.getInstance()
        val mes = calendar.get(Calendar.MONTH) + 1
        val ano = calendar.get(Calendar.YEAR)

        val repository = ContasRepository.getInstance(context)
        
        val totalReceita = repository.calcularTotalMensal(mes, ano, 1, null)
        val totalDespesa = repository.calcularTotalMensal(mes, ano, 0, null)
        val totalAplicacao = repository.calcularTotalMensal(mes, ano, 2, null)
        val saldo = totalReceita - totalDespesa

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val meses = context.resources.getStringArray(R.array.MesesDoAno)
        val mesNome = if (mes > 0 && mes <= meses.size) meses[mes - 1] else ""
        
        views.setTextViewText(R.id.tvWidgetTitulo, if (minWidth < 200) mesNome else "$mesNome $ano")
        
        views.setTextViewText(R.id.tvValorReceitas, currencyFormat.format(totalReceita))
        views.setTextViewText(R.id.tvValorDespesas, currencyFormat.format(totalDespesa))
        views.setTextViewText(R.id.tvValorAplicacoes, currencyFormat.format(totalAplicacao))
        views.setTextViewText(R.id.tvValorSaldoAtual, currencyFormat.format(saldo))

        // Cor dos Ícones: Branco (on_primary) para contrastar com o fundo Verde (primary)
        val onPrimaryColor = ContextCompat.getColor(context, R.color.on_primary)
        views.setInt(R.id.ibAdicionaConta, "setColorFilter", onPrimaryColor)
        views.setInt(R.id.ibPesquisaConta, "setColorFilter", onPrimaryColor)

        // Intents
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val intentApp = Intent(context, MinhasContas::class.java)
        views.setOnClickPendingIntent(R.id.tvAbreAplicativo, 
            PendingIntent.getActivity(context, appWidgetId + 101, intentApp, flag))

        val intentAdd = Intent(context, CriarConta::class.java)
        views.setOnClickPendingIntent(R.id.ibAdicionaConta, 
            PendingIntent.getActivity(context, appWidgetId + 201, intentAdd, flag))

        val intentSearch = Intent(context, PesquisaContas::class.java)
        views.setOnClickPendingIntent(R.id.ibPesquisaConta, 
            PendingIntent.getActivity(context, appWidgetId + 501, intentSearch, flag))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || 
            intent.action == Intent.ACTION_CONFIGURATION_CHANGED ||
            intent.action == "com.msk.minhascontas.UPDATE_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ResumoMensalWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val resumoProvider = ComponentName(context, ResumoMensalWidgetProvider::class.java)
            val resumoIds = appWidgetManager.getAppWidgetIds(resumoProvider)
            if (resumoIds.isNotEmpty()) {
                val intent = Intent(context, ResumoMensalWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, resumoIds)
                context.sendBroadcast(intent)
            }
            
            val addContaProvider = ComponentName(context, AddContaWidgetProvider::class.java)
            val addIds = appWidgetManager.getAppWidgetIds(addContaProvider)
            if (addIds.isNotEmpty()) {
                val intent = Intent(context, AddContaWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, addIds)
                context.sendBroadcast(intent)
            }
        }
    }
}
