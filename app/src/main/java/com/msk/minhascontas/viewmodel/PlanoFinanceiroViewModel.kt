package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.features.ai.AIAssistant
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.R
import com.msk.minhascontas.utils.FinanceCalculator
import kotlinx.coroutines.launch
import java.util.*

class PlanoFinanceiroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val aiAssistant = AIAssistant(application)

    // Dados do Orçamento (Vindo do PlanejamentoFinanceiro)
    var receitaReferencia by mutableDoubleStateOf(0.0)
    var percPrioridadeFinanceira by mutableDoubleStateOf(0.0)
    var valorDisponivelTotal20Porcento by mutableDoubleStateOf(0.0)
    var gastosMensaisEstimados by mutableDoubleStateOf(0.0)

    // Observáveis para a UI
    val metasAtivas = repository.getMetasAtivas()

    // Estado para a Simulação
    var metaEditando by mutableStateOf<MetaFinanceira?>(null)
    var tipoSimulacao by mutableIntStateOf(MetaFinanceira.TIPO_DIVIDA)
    var valorTotal by mutableDoubleStateOf(0.0)
    var valorAtual by mutableDoubleStateOf(0.0)
    var taxaJuros by mutableDoubleStateOf(0.0)
    var aporteMensal by mutableDoubleStateOf(0.0)
    
    // Resultado da Simulação
    var mesesRestantes by mutableStateOf(0)
        private set
    
    var dataPrevisaoFim by mutableStateOf<Date?>(null)
        private set

    // Diagnóstico e IA
    var valorPrestacoesAtivas by mutableDoubleStateOf(0.0)
    var temInvestimentos by mutableStateOf(false)
    var diagnosticoIA by mutableStateOf<AIResult?>(null)
    var isAnalyzingIA by mutableStateOf(false)

    init {
        carregarDadosOrcamento()
        realizarDiagnosticoFinanceiro()
    }

    private fun carregarDadosOrcamento() {
        receitaReferencia = prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()
        // Categoria 8 é "Investimentos/Dívidas" (Os 20% da regra)
        percPrioridadeFinanceira = prefs.getFloat("plan_perc_8", 20.0f).toDouble()
        valorDisponivelTotal20Porcento = (percPrioridadeFinanceira / 100.0) * receitaReferencia
        
        // Gastos mensais estimados = Receita - Aporte Prioritário
        gastosMensaisEstimados = receitaReferencia - valorDisponivelTotal20Porcento
    }

    fun sugerirReserva(meses: Int) {
        valorTotal = gastosMensaisEstimados * meses
        atualizarSimulacao()
    }

    fun atualizarSimulacao() {
        mesesRestantes = if (tipoSimulacao == MetaFinanceira.TIPO_DIVIDA) {
            FinanceCalculator.calcularMesesParaQuitarDivida(
                saldo = valorTotal - valorAtual,
                taxaMensalPercentual = taxaJuros,
                aporte = aporteMensal
            )
        } else {
            FinanceCalculator.calcularMesesParaMetaInvestimento(
                objetivo = valorTotal,
                inicial = valorAtual,
                aporte = aporteMensal,
                taxaMensalPercentual = taxaJuros
            )
        }

        if (mesesRestantes > 0 && mesesRestantes < 1200) { // Limite de 100 anos para sanidade
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, mesesRestantes)
            dataPrevisaoFim = calendar.time
        } else {
            dataPrevisaoFim = null
        }
    }

    fun carregarParaEdicao(meta: MetaFinanceira) {
        metaEditando = meta
        tipoSimulacao = meta.tipoMeta
        valorTotal = meta.valorObjetivo
        valorAtual = meta.valorAtual
        taxaJuros = meta.taxaJurosMensal
        aporteMensal = meta.aporteMensalAlvo
        atualizarSimulacao()
    }

    fun resetarSimulacao() {
        metaEditando = null
        tipoSimulacao = MetaFinanceira.TIPO_DIVIDA
        valorTotal = 0.0
        valorAtual = 0.0
        taxaJuros = 0.0
        aporteMensal = 0.0
        atualizarSimulacao()
    }

    fun confirmarMeta(nome: String) {
        viewModelScope.launch {
            val codigo = metaEditando?.codigoVinculo ?: UUID.randomUUID().toString()
            val meta = MetaFinanceira(
                id = metaEditando?.id ?: UUID.randomUUID().toString(),
                nome = nome,
                tipoMeta = tipoSimulacao,
                valorObjetivo = valorTotal,
                valorAtual = valorAtual,
                taxaJurosMensal = taxaJuros,
                aporteMensalAlvo = aporteMensal,
                dataPrevisaoFim = dataPrevisaoFim?.time,
                codigoVinculo = codigo
            )
            repository.salvarMeta(meta)
            
            // Gerar registros futuros no banco (apenas se for nova ou se mudou muito?)
            // Por simplicidade, vamos gerar/atualizar. 
            // O repository.salvarContasRecorrentes pode precisar tratar duplicidade se usarmos o mesmo código.
            gerarRegistrosFuturos(nome, codigo)
        }
    }

    fun excluirMeta(meta: MetaFinanceira) {
        viewModelScope.launch {
            repository.excluirMeta(meta)
            // Também excluímos as contas futuras vinculadas
            if (!meta.codigoVinculo.isNullOrEmpty()) {
                repository.excluirContasRecorrentes(0, meta.codigoVinculo, 0, com.msk.minhascontas.db.DBContas.TipoExclusao.TODAS_AS_REPETICOES)
            }
        }
    }

    private fun gerarRegistrosFuturos(nome: String, codigo: String) {
        if (mesesRestantes <= 0) return

        val calendar = Calendar.getInstance()
        val contaBase = com.msk.minhascontas.db.Conta.Builder(
            nome = "[COACH] $nome",
            valor = aporteMensal,
            dia = calendar.get(Calendar.DAY_OF_MONTH),
            mes = calendar.get(Calendar.MONTH) + 1,
            ano = calendar.get(Calendar.YEAR),
            codigo = codigo
        ).apply {
            if (tipoSimulacao == MetaFinanceira.TIPO_DIVIDA) {
                setTipo(0) // TIPO_DESPESA
                setClasseConta(3) // CLASSE_DESPESA_PRESTACOES
            } else {
                setTipo(2) // TIPO_APLICACAO
                setClasseConta(3) // CLASSE_APLICACAO_OUTRAS
            }
            setCategoria(8) // CATEGORIA_INVESTIMENTOS
            setValorJuros(taxaJuros / 100.0)
            setQtRepete(mesesRestantes)
        }.build()

        repository.salvarContasRecorrentes(contaBase, mesesRestantes, 300) // 300 = Mensal
    }

    /**
     * Força a atualização do progresso real de todas as metas ativas.
     */
    fun atualizarProgressoDeTodasAsMetas() {
        viewModelScope.launch {
            metasAtivas.value?.forEach { meta ->
                repository.atualizarProgressoRealMeta(meta)
            }
        }
    }

    fun realizarDiagnosticoFinanceiro() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val mes = calendar.get(Calendar.MONTH) + 1
            val ano = calendar.get(Calendar.YEAR)

            // Busca prestações ativas (Classe 3 de Despesa)
            valorPrestacoesAtivas = repository.somaValoresPorFiltro(
                ano, mes, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_PRESTACOES, -1, null
            )

            // Busca se existem aplicações (Tipo 2)
            val totalAplicacoes = repository.somaValoresPorFiltro(
                ano, mes, ContasContract.TIPO_APLICACAO, -1, -1, null
            )
            
            val temInvestimentoAcumulado = prefs?.getBoolean(getApplication<Application>().getString(R.string.pref_key_aplicacao_acumulada), false) ?: false
            val aplicacoesAnteriores = if (temInvestimentoAcumulado) {
                repository.somaAplicacoesAnteriores(
                    calendar.get(Calendar.DAY_OF_MONTH), mes, ano, true, -1
                )
            } else 0.0
            
            temInvestimentos = (totalAplicacoes + aplicacoesAnteriores) > 0
        }
    }

    fun gerarAnaliseIA() {
        if (isAnalyzingIA) return
        
        viewModelScope.launch {
            isAnalyzingIA = true
            val calendar = Calendar.getInstance()
            val mes = calendar.get(Calendar.MONTH) + 1
            val ano = calendar.get(Calendar.YEAR)
            
            val context = getApplication<Application>()
            val temSaldoSomado = prefs?.getBoolean(context.getString(R.string.pref_key_saldo), false) ?: false
            val temInvestimentoAcumulado = prefs?.getBoolean(context.getString(R.string.pref_key_aplicacao_acumulada), false) ?: false

            val contas = repository.getContasDoMes(mes, ano, -1, null)
            val totalAplicacoesMes = repository.somaValoresPorFiltro(
                ano, mes, ContasContract.TIPO_APLICACAO, -1, -1, null
            )
            
            val saldoAnterior = if (temSaldoSomado) {
                repository.somaSaldoAnterior(calendar.get(Calendar.DAY_OF_MONTH), mes, ano, true)
            } else 0.0
            val aplicacoesAnteriores = if (temInvestimentoAcumulado) {
                repository.somaAplicacoesAnteriores(calendar.get(Calendar.DAY_OF_MONTH), mes, ano, true, -1)
            } else 0.0

            val resumoTexto = aiAssistant.prepararResumo(
                contas = contas,
                saldoAnterior = saldoAnterior,
                investimentoAcumulado = aplicacoesAnteriores,
                temSaldoSomado = temSaldoSomado,
                temInvestimentoAcumulado = temInvestimentoAcumulado
            )

            diagnosticoIA = aiAssistant.gerarOrientacaoDiagnostico(
                dadosResumo = resumoTexto,
                valorPrestacoes = valorPrestacoesAtivas,
                patrimonioTotal = saldoAnterior + aplicacoesAnteriores + totalAplicacoesMes,
                valorDisponivel20 = valorDisponivelTotal20Porcento,
                temInvestimentoAcumulado = temInvestimentoAcumulado
            )
            isAnalyzingIA = false
        }
    }
}
