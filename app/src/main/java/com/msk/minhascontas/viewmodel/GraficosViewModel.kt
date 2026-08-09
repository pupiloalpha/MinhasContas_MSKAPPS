package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.db.ContaDao
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ReportSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class GraficosUiState(
    val summary: ReportSummary? = null,
    val gastosPorCategoria: Map<Int, Double> = emptyMap(),
    val receitasPorClasse: Map<Int, Double> = emptyMap(),
    val despesasPorClasse: Map<Int, Double> = emptyMap(),
    val aplicacoesPorClasse: Map<Int, Double> = emptyMap(),
    val gastosDetalhados: List<ContaDao.CategoryStatusSum> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class GraficosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val _mes = MutableStateFlow(1)
    private val _ano = MutableStateFlow(2000)
    private val _dia = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<GraficosUiState> = combine(_mes, _ano, _dia) { mes, ano, dia ->
        Triple(mes, ano, dia)
    }.flatMapLatest { (mes, ano, _) ->
        flow {
            val summary = repository.getReportSummary(mes, ano)

            // Calculo consolidado de despesas por classe (0: Fixa, 1: Variável, 2: Prestações, 3: Cartão)
            val despesasClasseMap = mutableMapOf<Int, Double>()
            for (i in 0..3) {
                despesasClasseMap[i] = repository.somaValoresNoPeriodo(1, 31, mes, ano, ContasContract.TIPO_DESPESA, i, -1, null)
            }

            // Receitas por Classe (0: Salário, 1: Extras, 2: Outros)
            val receitasClasseMap = mutableMapOf<Int, Double>()
            for (i in 0..2) {
                receitasClasseMap[i] = repository.somaValoresNoPeriodo(1, 31, mes, ano, ContasContract.TIPO_RECEITA, i, -1, null)
            }

            emit(
                GraficosUiState(
                    summary = summary,
                    gastosPorCategoria = summary.despesasPorCategoria,
                    receitasPorClasse = receitasClasseMap,
                    despesasPorClasse = despesasClasseMap,
                    aplicacoesPorClasse = summary.aplicacoesPorClasse,
                    isLoading = false
                )
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GraficosUiState()
    )

    val mediaCategorias: StateFlow<Map<Int, Double>> = combine(_mes, _ano) { mes, ano ->
        Pair(mes, ano)
    }.flatMapLatest {
        flow {
            val result = mutableMapOf<Int, Double>()
            for (i in 0..8) {
                result[i] = repository.getMediaCategoriaUltimosMeses(i, 3)
            }
            emit(result)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val mediaTipos: StateFlow<Map<Int, Double>> = combine(_mes, _ano) { mes, ano ->
        Pair(mes, ano)
    }.flatMapLatest {
        flow {
            val result = mutableMapOf<Int, Double>()
            result[ContasContract.TIPO_RECEITA] = repository.getMediaTipoUltimosMeses(ContasContract.TIPO_RECEITA, 3)
            result[ContasContract.TIPO_DESPESA] = repository.getMediaTipoUltimosMeses(ContasContract.TIPO_DESPESA, 3)
            result[ContasContract.TIPO_APLICACAO] = repository.getMediaTipoUltimosMeses(ContasContract.TIPO_APLICACAO, 3)
            emit(result)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updateDate(mes: Int, ano: Int, dia: Int?) {
        _mes.value = mes
        _ano.value = ano
        _dia.value = dia
    }

    suspend fun getMediaCategoria(categoria: Int): Double {
        return repository.getMediaCategoriaUltimosMeses(categoria, 3)
    }
}