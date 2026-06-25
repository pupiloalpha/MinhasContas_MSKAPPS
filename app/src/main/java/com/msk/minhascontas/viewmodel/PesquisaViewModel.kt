package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class PesquisaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    @OptIn(FlowPreview::class)
    val contas: StateFlow<List<Conta>> = _searchText
        .debounce(300)
        .combine(_selectedIds) { text, _ ->
            val filter = DBContas.ContaFilter()
            if (text.isNotBlank()) {
                filter.setNome(text)
            }
            repository.getContas(filter, null)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        if (current.contains(id)) {
            _selectedIds.value = current - id
        } else {
            _selectedIds.value = current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun togglePagamentoSelected() {
        val selected = _selectedIds.value
        selected.forEach { id ->
            val conta = repository.getConta(id)
            if (conta != null) {
                val newStatus = if (ContasContract.STATUS_PAGO_RECEBIDO == conta.pagamento)
                    ContasContract.STATUS_PENDENTE
                else
                    ContasContract.STATUS_PAGO_RECEBIDO
                repository.atualizarPagamento(id, newStatus)
            }
        }
        // Force refresh by updating search text (triggering the flow)
        _searchText.value = _searchText.value
        clearSelection()
    }

    fun deleteMultipleSelected() {
        val selected = _selectedIds.value
        selected.forEach { id ->
            repository.excluirConta(id)
        }
        _searchText.value = _searchText.value
        clearSelection()
    }

    fun deleteSingle(id: Long, tipoExclusao: DBContas.TipoExclusao) {
        val conta = repository.getConta(id)
        if (conta != null) {
            when (tipoExclusao) {
                DBContas.TipoExclusao.SOMENTE_ESTA -> repository.excluirConta(id)
                DBContas.TipoExclusao.DESTA_EM_DIANTE -> repository.excluirContasRecorrentes(
                    id, conta.codigo, conta.nRepete, DBContas.TipoExclusao.DESTA_EM_DIANTE
                )
                DBContas.TipoExclusao.TODAS_AS_REPETICOES -> repository.excluirContasRecorrentes(
                    id, conta.codigo, 1, DBContas.TipoExclusao.TODAS_AS_REPETICOES
                )
            }
        }
        _searchText.value = _searchText.value
        clearSelection()
    }
    
    fun getConta(id: Long): Conta? = repository.getConta(id)
}
