package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas.ContaFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ListaMensalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val _mes = MutableStateFlow(1)
    private val _ano = MutableStateFlow(2000)
    private val _dia = MutableStateFlow(0)
    private val _tipo = MutableStateFlow(-1)
    private val _filtro = MutableStateFlow(-1)
    private val _ordem = MutableStateFlow("dia_data ASC")

    val contas: StateFlow<List<Conta>> = combine(
        _mes, _ano, _dia, _tipo, _filtro, _ordem
    ) { args: Array<Any?> ->
        FilterState(
            mes = args[0] as Int,
            ano = args[1] as Int,
            dia = args[2] as Int,
            tipo = args[3] as Int,
            filtro = args[4] as Int,
            ordem = args[5] as String
        )
    }.flatMapLatest { state ->
        val filter = ContaFilter().setMes(state.mes).setAno(state.ano)
        if (state.dia > 0) filter.setDiaFim(state.dia)
        if (state.tipo != -1) {
            filter.setTipo(state.tipo)
            applyFiltro(filter, state.tipo, state.filtro)
        }
        repository.getContasFlow(filter, state.ordem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterState(
        val mes: Int, val ano: Int, val dia: Int, 
        val tipo: Int, val filtro: Int, val ordem: String
    )

    private fun applyFiltro(filter: ContaFilter, tipo: Int, filtroValue: Int) {
        if (filtroValue >= 0) {
            when (tipo) {
                0 -> { // TIPO_DESPESA
                    when (filtroValue) {
                        4 -> filter.setPagamento("falta")
                        5 -> filter.setPagamento("paguei")
                        else -> filter.setClasse(filtroValue)
                    }
                }
                1 -> { // TIPO_RECEITA
                    when (filtroValue) {
                        3 -> filter.setPagamento("falta")
                        4 -> filter.setPagamento("paguei")
                        else -> filter.setClasse(filtroValue)
                    }
                }
                else -> filter.setClasse(filtroValue)
            }
        }
    }

    fun updateState(mes: Int, ano: Int, dia: Int, tipo: Int, filtro: Int) {
        _mes.value = mes
        _ano.value = ano
        _dia.value = dia
        _tipo.value = tipo
        _filtro.value = filtro
    }

    fun setOrdem(ordem: String) {
        _ordem.value = ordem
    }
}
