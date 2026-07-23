package com.msk.minhascontas.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.MetaFinanceira
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Classe auxiliar para verificar regras de negócio e gerar notificações internas.
 */
class AlertHelper(private val context: Context) {

    private val repository: ContasRepository = ContasRepository.getInstance(context)
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val db: DBContas = DBContas.getInstance(context)
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    /**
     * Executa todas as verificações de alertas habilitadas pelo usuário.
     */
    suspend fun verificarTodosAlertas() {
        // Realiza higiene do banco de notificações antes de processar novas
        db.limparNotificacoesAntigas()

        val cal = Calendar.getInstance()
        val dia = cal.get(Calendar.DAY_OF_MONTH)
        val mes = cal.get(Calendar.MONTH) + 1 // 1-indexado
        val ano = cal.get(Calendar.YEAR)
        val nomeMes = DateFormatSymbols().months[mes - 1]

        if (prefs.getBoolean(context.getString(R.string.pref_alert_vencimento_key), true)) {
            checkVencimentos(dia, mes, ano)
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_limite_categoria_key), true)) {
            checkLimitesCategorias(mes, ano)
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_objetivo_plano_key), true)) {
            checkObjetivosMetas()
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_receita_referencia_key), true)) {
            checkReceitaReferencia(mes, ano)
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_despesa_receita_key), true)) {
            checkDespesasVsReceitas(mes, ano, nomeMes)
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_falta_aplicacao_key), true)) {
            checkFaltaAplicacoes(mes, ano, nomeMes)
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_fim_serie_key), true)) {
            checkFimSeries(mes, ano)
        }
    }

    private fun checkFimSeries(mes: Int, ano: Int) {
        repository.verificarFimDeSeries(mes, ano)
    }

    private suspend fun checkVencimentos(dia: Int, mes: Int, ano: Int) {
        val filter = DBContas.ContaFilter()
            .setDia(dia).setMes(mes).setAno(ano)
            .setPagamento(ContasContract.STATUS_PENDENTE)
        
        val hoje = repository.getContas(filter, null)
        for (c in hoje) {
            val msg = context.getString(R.string.alert_vencimento_msg, c.nome, currencyFormat.format(c.valor))
            db.addNotificacao(context.getString(R.string.pref_alert_vencimento_titulo), msg, "alert_vencimento")
        }
    }

    private suspend fun checkLimitesCategorias(mes: Int, ano: Int) {
        val receitaRef = prefs.getFloat("plan_receita_referencia", 0f).toDouble()
        if (receitaRef <= 0) return

        val gastos = repository.getGastosPorCategoria(mes, ano)
        for (i in 0..8) {
            val perc = prefs.getFloat("plan_perc_$i", -1f)
            if (perc > 0) {
                val limite = (perc / 100.0) * receitaRef
                val gastoAtual = gastos[i]
                if (gastoAtual != null && gastoAtual > limite) {
                    val ultrapassou = gastoAtual - limite
                    val catName = LabelUtils.getCategoriaLabel(context, i)
                    val msg = context.getString(R.string.alert_limite_categoria_msg, currencyFormat.format(ultrapassou), catName)
                    db.addNotificacao(context.getString(R.string.pref_alert_limite_categoria_titulo), msg, "alert_limite_categoria")
                }
            }
        }
    }

    private fun checkObjetivosMetas() {
        val metas = repository.getMetasSincrono()

        for (m in metas) {
            if (m.ativa && m.valorAtual >= m.valorObjetivo && m.valorObjetivo > 0) {
                val detalhes = currencyFormat.format(m.valorAtual)
                val msg = context.getString(R.string.alert_objetivo_atingido_msg, m.nome, detalhes)
                db.addNotificacao(context.getString(R.string.pref_alert_objetivo_plano_titulo), msg, "alert_objetivo_plano")
            }
        }
    }

    private suspend fun checkReceitaReferencia(mes: Int, ano: Int) {
        val receitaRef = prefs.getFloat("plan_receita_referencia", 0f).toDouble()
        if (receitaRef <= 0) return

        val totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null)
        if (totalReceitas > receitaRef) {
            val dif = totalReceitas - receitaRef
            val msg = context.getString(R.string.alert_receita_acima_ref_msg, currencyFormat.format(dif))
            db.addNotificacao(context.getString(R.string.pref_alert_receita_referencia_titulo), msg, "alert_receita_referencia")
        }
    }

    private suspend fun checkDespesasVsReceitas(mes: Int, ano: Int, nomeMes: String) {
        val totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null)
        val totalDespesas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_DESPESA, null)
        
        if (totalDespesas > totalReceitas && totalReceitas > 0) {
            val deficit = totalDespesas - totalReceitas
            val msg = context.getString(R.string.alert_deficit_mensal_msg, nomeMes, currencyFormat.format(deficit))
            db.addNotificacao(context.getString(R.string.pref_alert_despesa_receita_titulo), msg, "alert_despesa_receita")
        }
    }

    private suspend fun checkFaltaAplicacoes(mes: Int, ano: Int, nomeMes: String) {
        val totalAplicacoes = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_APLICACAO, null)
        if (totalAplicacoes <= 0) {
            val totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null)
            if (totalReceitas > 0) {
                // Sugestão de 10% da receita ou saldo disponível
                val sugestao = totalReceitas * 0.1
                val msg = context.getString(R.string.alert_falta_investimento_msg, nomeMes, currencyFormat.format(sugestao))
                db.addNotificacao(context.getString(R.string.pref_alert_falta_aplicacao_titulo), msg, "alert_falta_aplicacao")
            }
        }
    }
}
