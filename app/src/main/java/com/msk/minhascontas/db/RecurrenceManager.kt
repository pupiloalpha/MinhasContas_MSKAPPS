package com.msk.minhascontas.db

import androidx.sqlite.db.SimpleSQLiteQuery
import java.util.Calendar

/**
 * Especialista na gestão e cálculo de contas recorrentes.
 */
class RecurrenceManager(private val appDatabase: AppDatabase) {

    /**
     * Calcula e gera a lista de contas para uma série recorrente.
     */
    fun generateSeries(contaBase: Conta, qtRepeticoes: Int, intervalo: Int): List<Conta> {
        val series = mutableListOf<Conta>()
        val cal = Calendar.getInstance()
        cal.set(contaBase.ano, contaBase.mes - 1, contaBase.dia)
        
        val valorBase = contaBase.valor
        val taxaJuros = contaBase.valorJuros
        val codigo = contaBase.codigo

        for (i in 1..qtRepeticoes) {
            if (i > 1) {
                when (intervalo) {
                    300 -> cal.add(Calendar.MONTH, 1)
                    3650 -> cal.add(Calendar.YEAR, 1)
                    else -> cal.add(Calendar.DATE, if (intervalo > 100) intervalo - 100 else 1)
                }
            }

            val valorCalculado = if (i == 1) valorBase else {
                if ((contaBase.tipo == 0 || contaBase.tipo == 2) && taxaJuros != 0.0) {
                    valorBase * Math.pow(1.0 + taxaJuros, (i - 1).toDouble())
                } else {
                    valorBase
                }
            }

            val conta = if (i == 1) contaBase.apply {
                valor = valorCalculado
                dia = cal.get(Calendar.DAY_OF_MONTH)
                mes = cal.get(Calendar.MONTH) + 1
                ano = cal.get(Calendar.YEAR)
                nRepete = i
                this.qtRepete = qtRepeticoes
                this.intervalo = intervalo
            } else {
                Conta.Builder(contaBase.nome, valorCalculado, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR), codigo)
                    .setTipo(contaBase.tipo)
                    .setClasseConta(contaBase.classeConta)
                    .setCategoria(contaBase.categoria)
                    .setPagamento(ContasContract.STATUS_PENDENTE)
                    .setQtRepete(qtRepeticoes)
                    .setNRepete(i)
                    .setIntervalo(intervalo)
                    .setValorJuros(taxaJuros)
                    .build()
            }
            series.add(conta)
        }
        return series
    }

    /**
     * Calcula as atualizações para uma série existente.
     */
    fun calculateUpdates(seriesParaAtualizar: List<Conta>, contaTemplate: Conta, tipoAtualizacao: TipoAtualizacao): List<Conta> {
        val novoValorBase = contaTemplate.valor
        val novaTaxaJuros = contaTemplate.valorJuros
        val novoIntervalo = contaTemplate.intervalo
        val novaQtRepeticoes = contaTemplate.qtRepete
        val nRepeteBase = contaTemplate.nRepete
        val calCalculo = Calendar.getInstance()

        return seriesParaAtualizar.map { contaAntiga ->
            val nRepeteAtual = contaAntiga.nRepete
            
            // 1. Recálculo do Valor
            val valorRecalculado = if ((contaTemplate.tipo == 0 || contaTemplate.tipo == 2) && novaTaxaJuros != 0.0) {
                novoValorBase * Math.pow(1.0 + novaTaxaJuros, (nRepeteAtual - nRepeteBase).toDouble())
            } else {
                novoValorBase
            }

            // 2. Recálculo da Data
            calCalculo.set(contaTemplate.ano, contaTemplate.mes - 1, contaTemplate.dia)
            val diffRepete = nRepeteAtual - nRepeteBase
            if (diffRepete != 0 && novoIntervalo > 0) {
                when (novoIntervalo) {
                    300 -> calCalculo.add(Calendar.MONTH, diffRepete)
                    3650 -> calCalculo.add(Calendar.YEAR, diffRepete)
                    else -> if (novoIntervalo > 100) calCalculo.add(Calendar.DATE, (novoIntervalo - 100) * diffRepete)
                }
            }

            contaAntiga.apply {
                nome = contaTemplate.nome
                valor = valorRecalculado
                dia = calCalculo.get(Calendar.DAY_OF_MONTH)
                mes = calCalculo.get(Calendar.MONTH) + 1
                ano = calCalculo.get(Calendar.YEAR)
                tipo = contaTemplate.tipo
                classeConta = contaTemplate.classeConta
                categoria = contaTemplate.categoria
                qtRepete = novaQtRepeticoes
                intervalo = novoIntervalo
                valorJuros = novaTaxaJuros
            }
        }
    }
}
