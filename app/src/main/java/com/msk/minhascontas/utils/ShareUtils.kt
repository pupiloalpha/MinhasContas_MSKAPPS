package com.msk.minhascontas.utils

import android.content.Context
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import java.text.NumberFormat
import java.util.Locale

object ShareUtils {

    fun generateSummaryText(
        context: Context,
        month: Int,
        year: Int,
        contas: List<Conta>,
        saldoAnterior: Double,
        investimentoAnterior: Double,
        usaSaldoSomado: Boolean,
        usaInvestimentoAcumulado: Boolean
    ): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        val mesesArray = context.resources.getStringArray(R.array.MesesDoAno)
        val mesNome = mesesArray.getOrNull(month - 1) ?: month.toString()

        val receitas = contas.filter { it.tipo == ContasContract.TIPO_RECEITA }.sumOf { it.valor }
        val despesas = contas.filter { it.tipo == ContasContract.TIPO_DESPESA }.sumOf { it.valor }
        val aplicacoes = contas.filter { it.tipo == ContasContract.TIPO_APLICACAO }.sumOf { it.valor }

        val despesasPagas = contas.filter { it.tipo == ContasContract.TIPO_DESPESA && it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO }.sumOf { it.valor }
        val despesasPendentes = contas.filter { it.tipo == ContasContract.TIPO_DESPESA && it.pagamento == ContasContract.STATUS_PENDENTE }.sumOf { it.valor }

        val saldoMes = receitas - despesas
        val saldoFinal = if (usaSaldoSomado) saldoMes + saldoAnterior else saldoMes
        val totalInvestido = if (usaInvestimentoAcumulado) aplicacoes + investimentoAnterior else aplicacoes

        val sb = StringBuilder()
        sb.append("📊 *${context.getString(R.string.app_name)} - ${context.getString(R.string.titulo_resumo)}*")
        sb.append("\n📅 Período: $mesNome/$year")
        sb.append("\n\n------------------------------")
        sb.append("\n💰 *${context.getString(R.string.linha_receita)}:* ${currencyFormat.format(receitas)}")
        sb.append("\n💸 *${context.getString(R.string.linha_despesa)}:* ${currencyFormat.format(despesas)}")
        sb.append("\n📈 *${context.getString(R.string.linha_aplicacoes)}:* ${currencyFormat.format(totalInvestido)}")
        
        sb.append("\n\n------------------------------")
        sb.append("\n✅ ${context.getString(R.string.resumo_pagas)}: ${currencyFormat.format(despesasPagas)}")
        sb.append("\n⏳ ${context.getString(R.string.resumo_faltam)}: ${currencyFormat.format(despesasPendentes)}")
        
        if (usaSaldoSomado) {
            sb.append("\n\n------------------------------")
            sb.append("\n🏦 ${context.getString(R.string.resumo_mes_anterior)}: ${currencyFormat.format(saldoAnterior)}")
            sb.append("\n🏁 *${context.getString(R.string.linha_saldo)}:* ${currencyFormat.format(saldoFinal)}")
        } else {
            sb.append("\n\n------------------------------")
            sb.append("\n🏁 *${context.getString(R.string.resumo_saldo)}:* ${currencyFormat.format(saldoMes)}")
        }

        sb.append("\n\n_${context.getString(R.string.share_footer)}_")
        
        return sb.toString()
    }

    fun generateListText(
        context: Context,
        month: Int,
        year: Int,
        contas: List<Conta>
    ): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        val mesesArray = context.resources.getStringArray(R.array.MesesDoAno)
        val mesNome = mesesArray.getOrNull(month - 1) ?: month.toString()

        val sb = StringBuilder()
        sb.append("📋 *${context.getString(R.string.app_name)} - ${context.getString(R.string.dica_resumo)}*")
        sb.append("\n📅 $mesNome/$year")
        sb.append("\n\n")

        if (contas.isEmpty()) {
            sb.append(context.getString(R.string.dica_nenhuma_conta))
        } else {
            contas.forEach { conta ->
                val emoji = when (conta.tipo) {
                    ContasContract.TIPO_RECEITA -> "💰"
                    ContasContract.TIPO_DESPESA -> "💸"
                    ContasContract.TIPO_APLICACAO -> "📈"
                    else -> "🔹"
                }
                val status = if (conta.pagamento == ContasContract.STATUS_PAGO_RECEBIDO) "✅" else "⏳"
                sb.append("$emoji $status ${conta.nome}: ${currencyFormat.format(conta.valor)}\n")
            }
        }

        sb.append("\n_${context.getString(R.string.share_footer)}_")
        return sb.toString()
    }
}
