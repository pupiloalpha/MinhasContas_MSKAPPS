package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.features.ai.AIAssistant
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import androidx.lifecycle.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ContasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)
    // VARIÁVEIS DE RECURSO: Arrays de meses, carregados pelo Application Context
    val stringMonths: Array<String?> = application.resources.getStringArray(R.array.MesResumido)
    val fullStringMonths: Array<String?> = application.resources.getStringArray(R.array.MesesDoAno)

    // View State Holder (Mantido)
    class ViewState(@JvmField val isMonthlySummary: Boolean, @JvmField val isCategorySummary: Boolean)

    // Data Holder (Mantido)
    class DateState(
        @JvmField val mes: Int,
        @JvmField val ano: Int,
        @JvmField val nrPagina: Int,
        @JvmField val dia: Int,
    )

    // StateFlow para o estado de Configuração da View
    private val _viewState = MutableStateFlow<ViewState?>(null)
    val viewState: StateFlow<ViewState?> = _viewState.asStateFlow()

    // StateFlow para a posição do ViewPager.
    private val _viewPagerPosition = MutableStateFlow(MinhasContas.START_PAGE)
    val viewPagerPosition: StateFlow<Int> = _viewPagerPosition.asStateFlow()

    // Controle de total de páginas
    private val _totalPages = MutableStateFlow(2000)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    // Cache local para evitar múltiplas chamadas de auto-importação na mesma sessão
    private val mesesImportadosSessionCache = mutableSetOf<String>()

    // StateFlow que contém a data e a posição calculada.
    private val _currentDateState = MutableStateFlow<DateState?>(null)
    val currentDateState: StateFlow<DateState?> = _currentDateState.asStateFlow()

    // Flow reativo de contas para o estado atual (Mês/Ano ou Dia/Mês/Ano)
    val currentContas: StateFlow<List<Conta>> = combine(
        _currentDateState,
        _viewState
    ) { dateState, viewState ->
        if (dateState == null || viewState == null) null
        else dateState to viewState
    }.flatMapLatest { pair ->
        if (pair == null) flowOf(emptyList())
        else {
            val (date, state) = pair
            val filter = com.msk.minhascontas.db.DBContas.ContaFilter()
                .setMes(date.mes)
                .setAno(date.ano)

            if (!state.isMonthlySummary) {
                // Ao contrário de filtrar por um único dia, permitimos que o Flow traga as contas do mês
                // para que os fragmentos diários possam calcular o acumulado (MTD) conforme a necessidade.
                // filter.setDia(date.dia) // REMOVIDO: Agora os fragmentos filtram o acumulado até date.dia
            }
            repository.getContasFlow(filter, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // IA Assistente
    private val aiAssistant = AIAssistant(application)
    private val _aiAnalysisResult = MutableStateFlow<AIResult?>(null)
    val aiAnalysisResult: StateFlow<AIResult?> = _aiAnalysisResult.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Estado das Notificações Internas (Alertas)
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

    // NOVO: Flow para o total financeiro do contexto atual (Mês/Ano selecionado)
    val currentMonthTotal: StateFlow<Double> = _currentDateState.flatMapLatest { date ->
        if (date == null) flowOf(0.0)
        else {
            repository.calcularTotalFlow(date.mes, date.ano, com.msk.minhascontas.db.ContasContract.TIPO_DESPESA, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        loadViewState(application)
        // Garante que o total de páginas inicial suporte a posição de início
        if (MinhasContas.START_PAGE >= (_totalPages.value)) {
            _totalPages.value = MinhasContas.START_PAGE + 1000
        }
        calculateAndSetDateState(MinhasContas.START_PAGE)
    }

    private fun loadViewState(application: Application) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(application)

        val resumoMensal = prefs.getBoolean(application.getString(R.string.pref_key_resumo), true)
        val resumoCategoria =
            prefs.getBoolean(application.getString(R.string.pref_key_categoria), false)

        val newState = ViewState(resumoMensal, resumoCategoria)
        _viewState.value = newState
    }

    fun setViewPagerPosition(position: Int) {
        // Recarrega o estado da view sempre que houver sincronização forçada (position == -1)
        if (position == -1) {
            loadViewState(getApplication())
        }

        val currentPosition = _viewPagerPosition.value
        val validPosition = if (position == -1) currentPosition else position
        _viewPagerPosition.value = validPosition
        
        expandPagesIfNeeded(validPosition)
        calculateAndSetDateState(validPosition)
    }

    fun expandPagesIfNeeded(currentPage: Int) {
        val currentTotal = _totalPages.value
        if (currentPage >= currentTotal - 5 && currentTotal < 2000) {
            _totalPages.value = currentTotal + 24
        }
    }

    fun runAiAnalysis(contas: List<Conta>) {
        val currentState = _currentDateState.value
        val mes = currentState?.mes ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
        val ano = currentState?.ano ?: Calendar.getInstance().get(Calendar.YEAR)

        val application = getApplication<Application>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(application)
        val temSaldoSomado = prefs.getBoolean(application.getString(R.string.pref_key_saldo), false)
        val temInvestimentoAcumulado = prefs.getBoolean(application.getString(R.string.pref_key_aplicacao_acumulada), false)

        viewModelScope.launch {
            val saldoAnterior = if (temSaldoSomado) repository.somaSaldoAnterior(0, mes, ano, true) else 0.0
            val investimentoAnterior = if (temInvestimentoAcumulado) repository.somaAplicacoesAnteriores(0, mes, ano, true, -1) else 0.0

            _isAiLoading.value = true
            val result = aiAssistant.analisarFinancas(
                contas = contas,
                saldoAnterior = saldoAnterior,
                investimentoAcumulado = investimentoAnterior,
                temSaldoSomado = temSaldoSomado,
                temInvestimentoAcumulado = temInvestimentoAcumulado
            )
            _aiAnalysisResult.value = result
            _isAiLoading.value = false
        }
    }

    fun clearAiResult() {
        _aiAnalysisResult.value = null
    }

    /**
     * Atualiza o estado das notificações não lidas consultando o banco de dados.
     */
    fun refreshNotifications() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tem = com.msk.minhascontas.db.DBContas.getInstance(getApplication()).temNotificacoesNaoLidas()
            _hasUnreadNotifications.value = tem
        }
    }

    private fun calculateAndSetDateState(position: Int) {
        val viewState = _viewState.value
        val isMonthly = viewState == null || viewState.isMonthlySummary

        val newState: DateState = calculateDateState(position, isMonthly)
        _currentDateState.value = newState

        // Melhoria: Importação automática de fixas se habilitado
        if (isMonthly) {
            checkAndAutoImportFixas(newState.mes, newState.ano)
        }
    }

    private fun checkAndAutoImportFixas(mes: Int, ano: Int) {
        val chave = "$mes-$ano"
        if (mesesImportadosSessionCache.contains(chave)) return

        val application = getApplication<Application>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(application)
        val autoImport = prefs.getBoolean(application.getString(R.string.pref_key_auto_import_fixas), false)
        
        if (autoImport) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                // Importa para o mês visualizado
                val result = repository.importarFixasDeMesAnterior(mes, ano)
                if (result >= 0) {
                    mesesImportadosSessionCache.add(chave)
                }
                
                // Projeção: Tenta preparar também o mês seguinte para suavizar a navegação
                val calNext = Calendar.getInstance()
                calNext.set(ano, mes - 1, 1)
                calNext.add(Calendar.MONTH, 1)
                val nextMes = calNext.get(Calendar.MONTH) + 1
                val nextAno = calNext.get(Calendar.YEAR)
                
                if (!mesesImportadosSessionCache.contains("$nextMes-$nextAno")) {
                    repository.importarFixasDeMesAnterior(nextMes, nextAno)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun calculateDateState(position: Int, isMonthlySummary: Boolean): DateState {
            val currentCalendar = Calendar.getInstance()
            val positionOffset = position - MinhasContas.START_PAGE

            if (isMonthlySummary) {
                currentCalendar.add(Calendar.MONTH, positionOffset)
            } else {
                currentCalendar.add(Calendar.DAY_OF_YEAR, positionOffset)
            }

            val mes =
                currentCalendar[Calendar.MONTH] // Calendar.MONTH retorna 0-indexado (0-11)
            val ano = currentCalendar[Calendar.YEAR]
            val dia = currentCalendar[Calendar.DAY_OF_MONTH]

            // Mês indexado como 1-indexado (1 para janeiro)
            return DateState(mes + 1, ano, position, dia)
        }
    }
}