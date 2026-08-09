package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.minhascontas.app.ui.resumo.ResumoCategoriaState
import com.minhascontas.app.ui.resumo.ResumoTipoState
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContaFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TipoFiltroResumo {
    MENSAL, DIARIO
}

@OptIn(ExperimentalCoroutinesApi::class)
class ResumoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContasRepository.getInstance(application)

    data class DateFilterState(
        val mes: Int,
        val ano: Int,
        val dia: Int,
        val tipoFiltro: TipoFiltroResumo
    )

    private val _filterState = MutableStateFlow<DateFilterState?>(null)

    private val _tipoState = MutableStateFlow(ResumoTipoState())
    val tipoState: StateFlow<ResumoTipoState> = _tipoState.asStateFlow()

    private val _categoriaState = MutableStateFlow(ResumoCategoriaState())
    val categoriaState: StateFlow<ResumoCategoriaState> = _categoriaState.asStateFlow()

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val temSaldoSomado: Boolean
        get() = prefs.getBoolean(getApplication<Application>().getString(R.string.pref_key_saldo), false)

    private val temInvestimentoAcumulado: Boolean
        get() = prefs.getBoolean(getApplication<Application>().getString(R.string.pref_key_aplicacao_acumulada), false)

    val contasFlow: StateFlow<List<Conta>> = _filterState.flatMapLatest { filterState ->
        if (filterState == null) flowOf(emptyList())
        else {
            val filter = ContaFilter()
                .setMes(filterState.mes)
                .setAno(filterState.ano)

            repository.getContasFlow(filter, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(_filterState, contasFlow) { filter, contas ->
                filter to contas
            }.collectLatest { (filter, contas) ->
                if (filter != null) {
                    processarCalculos(filter, contas)
                }
            }
        }
    }

    fun setFiltro(mes: Int, ano: Int, dia: Int = 1, tipoFiltro: TipoFiltroResumo = TipoFiltroResumo.MENSAL) {
        val novoFiltro = DateFilterState(mes, ano, dia, tipoFiltro)

        // Só dispara o carregamento e a consulta se o filtro (mês/ano) realmente mudou
        if (_filterState.value != novoFiltro) {
            _tipoState.update { it.copy(isLoading = true) }
            _categoriaState.update { it.copy(isLoading = true) }
            _filterState.value = novoFiltro
        }
    }

    private suspend fun processarCalculos(filter: DateFilterState, contas: List<Conta>) {
        val isMonthly = filter.tipoFiltro == TipoFiltroResumo.MENSAL

        val contasFiltradas = if (isMonthly) {
            contas
        } else {
            contas.filter { it.dia <= filter.dia }
        }

        // 1. Calcula o resumo por Tipo e obtém o estado gerado
        val novoTipoState = calcularResumoTipo(filter, contasFiltradas)
        _tipoState.value = novoTipoState

        // 2. Passa o estado de tipo atualizado para o resumo por Categoria
        val novoCategoriaState = calcularResumoCategoria(contasFiltradas, novoTipoState)
        _categoriaState.value = novoCategoriaState
    }

    private suspend fun calcularResumoTipo(filter: DateFilterState, contas: List<Conta>): ResumoTipoState {
        val isMonthly = filter.tipoFiltro == TipoFiltroResumo.MENSAL

        var recReceber = 0.0
        var recRecebido = 0.0
        var despPaga = 0.0
        var despPagar = 0.0

        var despFixa = 0.0
        var despVar = 0.0
        var prestacoes = 0.0
        var cartao = 0.0

        var fundos = 0.0
        var poupanca = 0.0
        var previdencia = 0.0

        contas.forEach { conta ->
            when (conta.tipo) {
                ContasContract.TIPO_RECEITA -> {
                    if (conta.pagamento == ContasContract.STATUS_PAGO_RECEBIDO) {
                        recRecebido += conta.valor
                    } else {
                        recReceber += conta.valor
                    }
                }
                ContasContract.TIPO_DESPESA -> {
                    if (conta.pagamento == ContasContract.STATUS_PAGO_RECEBIDO) {
                        despPaga += conta.valor
                    } else {
                        despPagar += conta.valor
                    }

                    when (conta.classeConta) {
                    ContasContract.CLASSE_DESPESA_FIXA -> despFixa += conta.valor
                    ContasContract.CLASSE_DESPESA_VARIAVEL -> despVar += conta.valor
                    ContasContract.CLASSE_DESPESA_PRESTACOES -> prestacoes += conta.valor
                    ContasContract.CLASSE_DESPESA_CARTAO -> cartao += conta.valor
                    else -> despVar += conta.valor // Inclui classes não mapeadas como variáveis por padrão
                }
            }
            ContasContract.TIPO_APLICACAO -> {
                when (conta.classeConta) {
                    ContasContract.CLASSE_APLICACAO_FUNDOS -> fundos += conta.valor
                    ContasContract.CLASSE_APLICACAO_POUPANCA -> poupanca += conta.valor
                    ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL -> previdencia += conta.valor
                    else -> poupanca += conta.valor // Inclui CLASSE_APLICACAO_OUTRAS em Poupança ou novo campo
                }
            }
            }
        }

        if (temInvestimentoAcumulado) {
            fundos += repository.somaAplicacoesAnteriores(filter.dia, filter.mes, filter.ano, isMonthly, ContasContract.CLASSE_APLICACAO_FUNDOS)
            poupanca += repository.somaAplicacoesAnteriores(filter.dia, filter.mes, filter.ano, isMonthly, ContasContract.CLASSE_APLICACAO_POUPANCA)
            previdencia += repository.somaAplicacoesAnteriores(filter.dia, filter.mes, filter.ano, isMonthly, ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL)
        }

        val totalReceitas = recReceber + recRecebido
        val totalDespesas = despPaga + despPagar
        val totalAplicacoes = fundos + poupanca + previdencia

        val saldoAtual = totalReceitas - totalDespesas

        val saldoAnterior: Double
        val saldoTotal: Double

        if (temSaldoSomado) {
            saldoAnterior = repository.somaSaldoAnterior(filter.dia, filter.mes, filter.ano, isMonthly)
            saldoTotal = (recRecebido - despPaga) + saldoAnterior
        } else {
            saldoAnterior = getSaldoMesAnterior(filter.mes, filter.ano)
            saldoTotal = recRecebido - despPaga
        }

        return ResumoTipoState(
            valorReceber = recReceber,
            valorRecebido = recRecebido,
            valorReceitasTotal = totalReceitas,
            valorDespPaga = despPaga,
            valorDespPagar = despPagar,
            valorDespFixa = despFixa,
            valorDespVar = despVar,
            valorPrestacoes = prestacoes,
            valorCartaoCredito = cartao,
            valorDespesasTotal = totalDespesas,
            valorFundos = fundos,
            valorPoupancas = poupanca,
            valorPrevidencias = previdencia,
            valorAplicacoesTotal = totalAplicacoes,
            valorSaldoAtual = saldoAtual,
            valorSaldoAnterior = saldoAnterior,
            valorSaldoTotal = saldoTotal,
            isLoading = false
        )
    }

    private suspend fun getSaldoMesAnterior(mesAtual: Int, anoAtual: Int): Double {
        var mesAnt = mesAtual - 1
        var anoAnt = anoAtual
        if (mesAnt < 1) {
            mesAnt = 12
            anoAnt--
        }
        val filterAnt = ContaFilter().setMes(mesAnt).setAno(anoAnt)
        val contasAnt = repository.getContas(filterAnt, null)

        val recPagaAnt = contasAnt.filter {
            it.tipo == ContasContract.TIPO_RECEITA && it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO
        }.sumOf { it.valor }

        val despPagaAnt = contasAnt.filter {
            it.tipo == ContasContract.TIPO_DESPESA && it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO
        }.sumOf { it.valor }

        return recPagaAnt - despPagaAnt
    }

    private fun calcularResumoCategoria(contas: List<Conta>, tipoSt: ResumoTipoState): ResumoCategoriaState {
        var alimentacao = 0.0
        var educacao = 0.0
        var moradia = 0.0
        var saude = 0.0
        var transporte = 0.0
        var outros = 0.0

        // Processa as despesas espelhando rigorosamente a checagem de tipo usada no calcularResumoTipo
        contas.forEach { conta ->
            if (conta.tipo == ContasContract.TIPO_DESPESA) {
                when (conta.categoria) {
                    ContasContract.CATEGORIA_ALIMENTACAO -> alimentacao += conta.valor
                    ContasContract.CATEGORIA_EDUCACAO -> educacao += conta.valor
                    ContasContract.CATEGORIA_MORADIA -> moradia += conta.valor
                    ContasContract.CATEGORIA_SAUDE -> saude += conta.valor
                    ContasContract.CATEGORIA_TRANSPORTE -> transporte += conta.valor
                    else -> outros += conta.valor
                }
            }
        }

        return ResumoCategoriaState(
            // Dados herdados do Resumo por Tipo (Receitas, Saldo e Aplicações)
            valorReceitasTotal = tipoSt.valorReceitasTotal,
            valorRecebido = tipoSt.valorRecebido,
            valorReceber = tipoSt.valorReceber,

            // Garantia de simetria para o total de despesas, pagas e a pagar
            valorDespesasTotal = tipoSt.valorDespesasTotal,
            valorDespPaga = tipoSt.valorDespPaga,
            valorDespPagar = tipoSt.valorDespPagar,

            // Categorias de despesas somadas
            valorAlimentacao = alimentacao,
            valorEducacao = educacao,
            valorMoradia = moradia,
            valorSaude = saude,
            valorTransporte = transporte,
            valorOutros = outros,

            // Aplicações herdadas do Resumo por Tipo
            valorAplicacoesTotal = tipoSt.valorAplicacoesTotal,
            valorFundos = tipoSt.valorFundos,
            valorPoupancas = tipoSt.valorPoupancas,
            valorPrevidencias = tipoSt.valorPrevidencias,

            // Saldo herdado do Resumo por Tipo
            valorSaldoTotal = tipoSt.valorSaldoTotal,
            valorSaldoAtual = tipoSt.valorSaldoAtual,
            valorSaldoAnterior = tipoSt.valorSaldoAnterior,
            valorBanco = tipoSt.valorRecebido,
            valorCartaoCredito = tipoSt.valorCartaoCredito,
            isLoading = false
        )
    }
}