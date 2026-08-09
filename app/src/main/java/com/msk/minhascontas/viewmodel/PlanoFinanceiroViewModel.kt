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
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.TipoExclusao
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.features.ai.AIAssistant
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.R
import com.msk.minhascontas.utils.FinanceCalculator
import com.msk.minhascontas.utils.Pontoprojecao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class PlanoFinanceiroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val aiAssistant = AIAssistant(application)

    // Dados Reais do Orçamento (Apurados via BD)
    var receitaMediaReal by mutableDoubleStateOf(0.0)
    var despesaMediaReal by mutableDoubleStateOf(0.0)

    // Regra 50/30/20 calculada sobre a receita real
    var valorMetaNecessidades50 by mutableDoubleStateOf(0.0)
    var valorMetaDesejos30 by mutableDoubleStateOf(0.0)
    var valorMetaPrioridade20 by mutableDoubleStateOf(0.0)
    var capacidadeAporteLivre20 by mutableDoubleStateOf(0.0)

    val metasAtivas = repository.getMetasAtivas()

    // Flow com as metas atualizadas com o saldo real do Room DB
    private val _metasComProgressoReal = MutableStateFlow<List<MetaFinanceira>>(emptyList())
    val metasComProgressoReal: StateFlow<List<MetaFinanceira>> = _metasComProgressoReal.asStateFlow()

    // Estado para a Simulação
    var metaEditando by mutableStateOf<MetaFinanceira?>(null)
    var tipoSimulacao by mutableIntStateOf(MetaFinanceira.TIPO_DIVIDA)
    var valorTotal by mutableDoubleStateOf(0.0)
    var valorAtual by mutableDoubleStateOf(0.0)
    var taxaJuros by mutableDoubleStateOf(0.0)
    var aporteMensal by mutableDoubleStateOf(0.0)
    var dataInicioMeta by mutableStateOf(Date())

    // Resultado da Simulação e Análise Avançada
    var mesesRestantes by mutableStateOf(0)
        private set

    var dataPrevisaoFim by mutableStateOf<Date?>(null)
        private set

    var serieProjecao by mutableStateOf<List<Pontoprojecao>>(emptyList())
        private set

    var totalJurosEstimado by mutableDoubleStateOf(0.0)
        private set

    var totalAportadoEstimado by mutableDoubleStateOf(0.0)
        private set

    // Diagnóstico e IA
    var valorPrestacoesAtivas by mutableDoubleStateOf(0.0)
    var temInvestimentos by mutableStateOf(false)
    var diagnosticoIA by mutableStateOf<AIResult?>(null)
    var isAnalyzingIA by mutableStateOf(false)

    init {
        carregarDadosReaisEOrcamento()
        realizarDiagnosticoFinanceiro()

        // Observa alterações nas metas cadastradas para atualizar com dados do Room
        metasAtivas.observeForever { listaMetas ->
            recalcularProgressoMetasComBanco(listaMetas)
        }
    }

    /**
     * Recalcula o progresso de cada meta consultando os registros reais do banco de dados Room.
     * Considera apenas pagamentos realizados até a data atual.
     */
    fun recalcularProgressoMetasComBanco(lista: List<MetaFinanceira>? = metasAtivas.value) {
        val metasBase = lista ?: return
        val dataHoje = Calendar.getInstance().time
        viewModelScope.launch {
            val listaAtualizada = metasBase.map { meta ->
                // Filtra contas da meta que não estão no futuro
                val progressoReal = repository.calcularProgressoRealDaMeta(meta)
                // O método do repositório deve ser verificado se já filtra por data, 
                // caso contrário, filtramos aqui.
                meta.copy(valorAtual = progressoReal)
            }
            _metasComProgressoReal.value = listaAtualizada
        }
    }

    /**
     * Carrega as médias reais de receita e despesa do banco de dados (últimos 3 meses)
     * e aplica a distribuição do orçamento 50/30/20.
     */
    fun carregarDadosReaisEOrcamento() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val mesAtual = calendar.get(Calendar.MONTH) + 1
            val anoAtual = calendar.get(Calendar.YEAR)

            var somaReceitas = 0.0
            var somaDespesas = 0.0
            val mesesAnalise = 3

            for (i in 0 until mesesAnalise) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                val m = cal.get(Calendar.MONTH) + 1
                val a = cal.get(Calendar.YEAR)

                somaReceitas += repository.calcularTotalMensal(m, a, ContasContract.TIPO_RECEITA, null)
                somaDespesas += repository.calcularTotalMensal(m, a, ContasContract.TIPO_DESPESA, null)
            }

            receitaMediaReal = if (somaReceitas > 0) somaReceitas / mesesAnalise else prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()
            despesaMediaReal = if (somaDespesas > 0) somaDespesas / mesesAnalise else (receitaMediaReal * 0.8)

            val percPrioridade = prefs.getFloat("plan_perc_8", 20.0f).toDouble()

            valorMetaNecessidades50 = receitaMediaReal * 0.50
            valorMetaDesejos30 = receitaMediaReal * 0.30
            valorMetaPrioridade20 = receitaMediaReal * (percPrioridade / 100.0)

            valorPrestacoesAtivas = repository.somaValoresPorFiltro(
                anoAtual, mesAtual, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_PRESTACOES, -1, null
            )

            capacidadeAporteLivre20 = (valorMetaPrioridade20 - valorPrestacoesAtivas).coerceAtLeast(0.0)

            if (aporteMensal == 0.0) {
                aporteMensal = capacidadeAporteLivre20
            }
        }
    }

    fun sugerirReserva(meses: Int) {
        valorTotal = despesaMediaReal * meses
        if (aporteMensal == 0.0) {
            aporteMensal = capacidadeAporteLivre20
        }
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

        if (mesesRestantes > 0 && mesesRestantes < 1200) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, mesesRestantes)
            dataPrevisaoFim = calendar.time

            serieProjecao = FinanceCalculator.gerarSérieProjeção(
                tipo = tipoSimulacao,
                valorInicial = if (tipoSimulacao == MetaFinanceira.TIPO_DIVIDA) (valorTotal - valorAtual) else valorAtual,
                valorAlvo = valorTotal,
                aporte = aporteMensal,
                taxaMensalPercentual = taxaJuros,
                mesesMax = mesesRestantes
            )

            val ultimoPonto = serieProjecao.lastOrNull()
            if (ultimoPonto != null) {
                totalJurosEstimado = ultimoPonto.totalJuros
                totalAportadoEstimado = ultimoPonto.totalAportado
            }
        } else {
            dataPrevisaoFim = null
            serieProjecao = emptyList()
            totalJurosEstimado = 0.0
            totalAportadoEstimado = 0.0
        }
    }

    fun carregarParaEdicao(meta: MetaFinanceira) {
        metaEditando = meta
        tipoSimulacao = meta.tipoMeta
        valorTotal = meta.valorObjetivo
        valorAtual = meta.valorAtual
        taxaJuros = meta.taxaJurosMensal
        aporteMensal = meta.aporteMensalAlvo
        dataInicioMeta = Date(meta.dataInicio)
        atualizarSimulacao()
    }

    suspend fun getMetaById(id: String): MetaFinanceira? {
        return repository.getMetaById(id)
    }

    fun resetarSimulacao() {
        metaEditando = null
        tipoSimulacao = MetaFinanceira.TIPO_DIVIDA
        valorTotal = 0.0
        valorAtual = 0.0
        taxaJuros = 0.0
        aporteMensal = capacidadeAporteLivre20
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
                dataInicio = dataInicioMeta.time,
                dataPrevisaoFim = dataPrevisaoFim?.time,
                codigoVinculo = codigo
            )
            repository.salvarMeta(meta)
            gerarRegistrosFuturos(nome, codigo)
            recalcularProgressoMetasComBanco()
        }
    }

    fun excluirMeta(meta: MetaFinanceira) {
        viewModelScope.launch {
            repository.excluirMeta(meta)
            if (!meta.codigoVinculo.isNullOrEmpty()) {
                repository.excluirContasRecorrentes(0, meta.codigoVinculo, 0, TipoExclusao.TODAS_AS_REPETICOES)
            }
            recalcularProgressoMetasComBanco()
        }
    }

    private fun gerarRegistrosFuturos(nome: String, codigo: String) {
        if (mesesRestantes <= 0) return

        val calendar = Calendar.getInstance()
        val contaBase = Conta.Builder(
            nome = "[COACH] $nome",
            valor = aporteMensal,
            dia = calendar.get(Calendar.DAY_OF_MONTH),
            mes = calendar.get(Calendar.MONTH) + 1,
            ano = calendar.get(Calendar.YEAR),
            codigo = codigo
        ).apply {
            when (tipoSimulacao) {
                MetaFinanceira.TIPO_DIVIDA -> {
                    setTipo(ContasContract.TIPO_DESPESA)
                    setClasseConta(ContasContract.CLASSE_DESPESA_PRESTACOES)
                    setCategoria(ContasContract.CATEGORIA_OUTROS)
                }
                MetaFinanceira.TIPO_RESERVA,
                MetaFinanceira.TIPO_INVESTIMENTO,
                MetaFinanceira.TIPO_APOSENTADORIA -> {
                    setTipo(ContasContract.TIPO_APLICACAO)
                    setClasseConta(ContasContract.CLASSE_APLICACAO_OUTRAS)
                }
            }
            setValorJuros(taxaJuros / 100.0)
            setQtRepete(mesesRestantes)
        }.build()

        viewModelScope.launch {
            repository.salvarContasRecorrentes(contaBase, mesesRestantes, 300)
        }
    }

    fun realizarDiagnosticoFinanceiro() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val mes = calendar.get(Calendar.MONTH) + 1
            val ano = calendar.get(Calendar.YEAR)

            valorPrestacoesAtivas = repository.somaValoresPorFiltro(
                ano, mes, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_PRESTACOES, -1, null
            )

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
                valorDisponivel20 = valorMetaPrioridade20,
                temInvestimentoAcumulado = temInvestimentoAcumulado
            )
            isAnalyzingIA = false
        }
    }
}