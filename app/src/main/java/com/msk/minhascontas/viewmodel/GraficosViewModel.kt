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
class GraficosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val _mes = MutableStateFlow(1)
    private val _ano = MutableStateFlow(2000)
    private val _dia = MutableStateFlow<Int?>(null)

    val allContas: StateFlow<List<Conta>> = combine(_mes, _ano, _dia) { mes, ano, dia ->
        Triple(mes, ano, dia)
    }.flatMapLatest { (mes, ano, dia) ->
        val filter = ContaFilter().setMes(mes).setAno(ano)
        if (dia != null && dia > 0) filter.setDiaFim(dia)
        repository.getContasFlow(filter, null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateDate(mes: Int, ano: Int, dia: Int?) {
        _mes.value = mes
        _ano.value = ano
        _dia.value = dia
    }
    
    suspend fun getMediaCategoria(categoria: Int): Double {
        return repository.getMediaCategoriaUltimosMeses(categoria, 3)
    }
}
