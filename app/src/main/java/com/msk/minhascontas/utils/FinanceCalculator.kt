package com.msk.minhascontas.utils

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/**
 * Motor de Cálculo para o Coach Financeiro do Minhas Contas.
 * Lida com juros compostos, amortizações e projeções de patrimônio.
 */
object FinanceCalculator {

    /**
     * DÍVIDAS: Calcula em quantos meses a dívida será quitada.
     * n = -ln(1 - (PV * i) / P) / ln(1 + i)
     * 
     * @param saldo Saldo devedor atual.
     * @param taxaMensalPercentual Taxa de juros mensal (ex: 2.5 para 2.5%).
     * @param aporte Valor pago mensalmente.
     * @return Número de meses ou -1 se o aporte for menor que os juros (dívida infinita).
     */
    fun calcularMesesParaQuitarDivida(saldo: Double, taxaMensalPercentual: Double, aporte: Double): Int {
        val i = taxaMensalPercentual / 100.0
        if (i == 0.0) return if (aporte > 0) ceil(saldo / aporte).toInt() else -1
        if (aporte <= saldo * i) return -1 
        
        val n = -ln(1 - (saldo * i) / aporte) / ln(1 + i)
        return ceil(n).toInt()
    }

    /**
     * PATRIMÔNIO: Calcula quantos meses faltam para atingir o valor objetivo.
     * n = ln((FV*i + P) / (PV*i + P)) / ln(1 + i)
     * 
     * @param objetivo Valor que se deseja atingir.
     * @param inicial Valor já acumulado.
     * @param aporte Valor investido mensalmente.
     * @param taxaMensalPercentual Rendimento esperado mensal.
     */
    fun calcularMesesParaMetaInvestimento(
        objetivo: Double, 
        inicial: Double, 
        aporte: Double, 
        taxaMensalPercentual: Double
    ): Int {
        val i = taxaMensalPercentual / 100.0
        if (i == 0.0) {
            return if (aporte > 0) ceil((objetivo - inicial) / aporte).toInt() else -1
        }
        
        // Evita log de número negativo se os aportes/inicial já superam o objetivo (meses = 0)
        if (inicial >= objetivo) return 0
        
        val numerador = objetivo * i + aporte
        val denominador = inicial * i + aporte
        
        val n = ln(numerador / denominador) / ln(1 + i)
        return ceil(n).toInt()
    }
    
    /**
     * PROJEÇÃO: Calcula o valor futuro (FV) após N meses.
     * FV = PV * (1+i)^n + P * [((1+i)^n - 1) / i]
     */
    fun calcularValorFuturo(inicial: Double, aporte: Double, taxaMensalPercentual: Double, meses: Int): Double {
        val i = taxaMensalPercentual / 100.0
        val fatorJuros = (1 + i).pow(meses)
        
        val principal = inicial * fatorJuros
        val acumuladoAportes = if (i == 0.0) {
            aporte * meses
        } else {
            aporte * (fatorJuros - 1) / i
        }
        
        return principal + acumuladoAportes
    }
}
