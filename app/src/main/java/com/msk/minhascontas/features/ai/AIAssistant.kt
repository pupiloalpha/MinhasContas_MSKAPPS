package com.msk.minhascontas.features.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.msk.minhascontas.BuildConfig
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.utils.LabelUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Assistente de IA que utiliza o Google Gemini para analisar dados financeiros.
 */
class AIAssistant(private val context: Context) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    /**
     * Analisa uma lista de contas e retorna um insight formatado em HTML.
     */
    suspend fun analisarFinancas(
        contas: List<Conta>,
        saldoAnterior: Double = 0.0,
        investimentoAcumulado: Double = 0.0,
        temSaldoSomado: Boolean = false,
        temInvestimentoAcumulado: Boolean = false,
    ): AIResult {
        if (contas.isEmpty() && (saldoAnterior == 0.0) && (investimentoAcumulado == 0.0)) 
            return AIResult.Success(context.getString(R.string.ai_no_data))

        return withContext(Dispatchers.IO) {
            val resumo = prepararResumo(contas, saldoAnterior, investimentoAcumulado, temSaldoSomado, temInvestimentoAcumulado)
            val promptBase = context.getString(R.string.ai_prompt_template, resumo)
            val prompt = "$promptBase\n\nResponda APENAS em HTML formatado, sem blocos de código markdown (sem ```html)."
            
            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: context.getString(R.string.ai_error_generating)
                
                val cleanText = text.replace(Regex("```html|```"), "")
                    .replace(Regex("^html", RegexOption.IGNORE_CASE), "")
                    .trim()
                
                AIResult.Success(cleanText)
            } catch (e: Exception) {
                AIResult.Error(e.message ?: "", prompt)
            }
        }
    }

    /**
     * Gera uma orientação personalizada baseada no diagnóstico de registros reais.
     */
    suspend fun gerarOrientacaoDiagnostico(
        dadosResumo: String,
        valorPrestacoes: Double,
        patrimonioTotal: Double,
        valorDisponivel20: Double,
        temInvestimentoAcumulado: Boolean = false
    ): AIResult {
        return withContext(Dispatchers.IO) {
            val contextoAdicional = if (temInvestimentoAcumulado) {
                "\nObs: O patrimônio informado inclui valores acumulados de meses anteriores."
            } else {
                "\nObs: O patrimônio informado reflete apenas o balanço deste mês (visão mensal)."
            }

            val currencyFormat = NumberFormat.getCurrencyInstance()
            val prompt = context.getString(
                R.string.ai_prompt_diagnostico,
                dadosResumo + contextoAdicional,
                currencyFormat.format(valorPrestacoes),
                currencyFormat.format(patrimonioTotal),
                currencyFormat.format(valorDisponivel20)
            )

            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: context.getString(R.string.ai_error_generating)

                val cleanText = text.replace(Regex("```html|```"), "")
                    .replace(Regex("^html", RegexOption.IGNORE_CASE), "")
                    .trim()
                
                AIResult.Success(cleanText)
            } catch (e: Exception) {
                AIResult.Error(e.message ?: "", prompt)
            }
        }
    }

    internal fun prepararResumo(
        contas: List<Conta>,
        saldoAnterior: Double,
        investimentoAcumulado: Double,
        temSaldoSomado: Boolean,
        temInvestimentoAcumulado: Boolean,
    ): String {
        val currencyFormat = NumberFormat.getCurrencyInstance()
        val totalReceitas = contas.asSequence().filter { it.tipo == ContasContract.TIPO_RECEITA }.sumOf { it.valor }
        val totalDespesas = contas.asSequence().filter { it.tipo == ContasContract.TIPO_DESPESA }.sumOf { it.valor }
        val investimentosMes = contas.asSequence().filter { it.tipo == ContasContract.TIPO_APLICACAO }.sumOf { it.valor }

        val despesas = contas.asSequence().filter { it.tipo == ContasContract.TIPO_DESPESA }.toList()

        val porCategoria = despesas.asSequence()
            .groupBy { it.categoria }
            .entries.joinToString("\n") { (cat, list) -> 
                val nomeCat = LabelUtils.getCategoriaLabel(context, cat)
                "- $nomeCat: ${currencyFormat.format(list.sumOf { it.valor })}"
            }

        val porClasse = despesas.asSequence()
            .groupBy { it.classeConta }
            .entries.joinToString("\n") { (classe, list) ->
                val nomeClasse = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, classe)
                "- $nomeClasse: ${currencyFormat.format(list.sumOf { it.valor })}"
            }

        val infoHistorica = StringBuilder()
        if (temSaldoSomado) {
            infoHistorica.append(context.getString(R.string.ai_summary_prev_balance, currencyFormat.format(saldoAnterior))).append("\n")
        }
        
        if (temInvestimentoAcumulado) {
            infoHistorica.append(context.getString(R.string.ai_summary_total_investments, currencyFormat.format(investimentosMes + investimentoAcumulado))).append("\n")
        } else {
            infoHistorica.append(context.getString(R.string.ai_summary_monthly_investments, currencyFormat.format(investimentosMes))).append("\n")
        }

        val labelClasse = context.getString(R.string.classe).uppercase(Locale.ROOT)

        return """
            ${context.getString(R.string.ai_summary_header)}
            ${context.getString(R.string.ai_summary_income, currencyFormat.format(totalReceitas))}
            ${context.getString(R.string.ai_summary_expenses, currencyFormat.format(totalDespesas))}
            $infoHistorica
            
            ${context.getString(R.string.ai_summary_expenses_breakdown)}
            $porCategoria
            
            $labelClasse:
            $porClasse
        """.trimIndent()
    }
}
