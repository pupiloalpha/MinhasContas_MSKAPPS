package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.ContasContract.Colunas
import com.msk.minhascontas.tarefas.AtualizarContaTarefa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarContaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    var conta: Conta? by mutableStateOf(null)
        private set

    var selectedIds by mutableStateOf(listOf<Long>())
        private set

    var isBulkEdit by mutableStateOf(false)
        private set

    var divergentFields by mutableStateOf(setOf<String>())
        private set

    var modifiedFields by mutableStateOf(setOf<String>())
        private set

    var isRecurring by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _onTaskComplete = MutableSharedFlow<Boolean>()
    val onTaskComplete: SharedFlow<Boolean> = _onTaskComplete

    fun loadConta(id: Long) {
        loadContas(listOf(id))
    }

    fun loadContas(ids: List<Long>) {
        selectedIds = ids
        if (ids.isEmpty()) return

        viewModelScope.launch {
            isLoading = true
            if (ids.size == 1) {
                isBulkEdit = false
                divergentFields = emptySet()
                modifiedFields = emptySet()
                
                val c = withContext(Dispatchers.IO) {
                    repository.getConta(ids[0])
                }
                conta = c
                if (c != null && c.codigo.isNotEmpty()) {
                    if (c.qtRepete > 1) {
                        isRecurring = true
                    } else {
                        val filter = DBContas.ContaFilter().setCodigoConta(c.codigo)
                        val series = withContext(Dispatchers.IO) {
                            repository.getContas(filter, null)
                        }
                        isRecurring = series.size > 1
                    }
                } else {
                    isRecurring = false
                }
            } else {
                isBulkEdit = true
                isRecurring = false
                modifiedFields = emptySet()
                
                val contas = withContext(Dispatchers.IO) {
                    ids.mapNotNull { repository.getConta(it) }
                }
                
                if (contas.isNotEmpty()) {
                    val base = contas[0]
                    val divergentes = mutableSetOf<String>()
                    
                    if (contas.any { it.nome != base.nome }) divergentes.add("nome")
                    if (contas.any { it.valor != base.valor }) divergentes.add("valor")
                    if (contas.any { it.tipo != base.tipo }) divergentes.add("tipo")
                    if (contas.any { it.classeConta != base.classeConta }) divergentes.add("classe")
                    if (contas.any { it.categoria != base.categoria }) divergentes.add("categoria")
                    if (contas.any { it.dia != base.dia || it.mes != base.mes || it.ano != base.ano }) divergentes.add("data")
                    if (contas.any { it.pagamento != base.pagamento }) divergentes.add("pagamento")
                    if (contas.any { it.intervalo != base.intervalo }) divergentes.add("intervalo")
                    if (contas.any { it.qtRepete != base.qtRepete }) divergentes.add("qtRepete")
                    if (contas.any { it.valorJuros != base.valorJuros }) divergentes.add("juros")

                    conta = base
                    divergentFields = divergentes
                }
            }
            isLoading = false
        }
    }

    fun markFieldAsModified(fieldName: String) {
        modifiedFields = modifiedFields + fieldName
    }

    fun updateConta(updatedConta: Conta, tipo: DBContas.TipoAtualizacao) {
        if (isBulkEdit) {
            updateBulk(updatedConta)
        } else {
            viewModelScope.launch {
                isLoading = true
                val success = withContext(Dispatchers.IO) {
                    val tarefa = AtualizarContaTarefa(updatedConta, tipo)
                    tarefa.executarTarefa(getApplication())
                }
                _onTaskComplete.emit(success)
                isLoading = false
            }
        }
    }

    private fun updateBulk(template: Conta) {
        viewModelScope.launch {
            isLoading = true
            val updates = mutableMapOf<String, Any?>()
            
            if (modifiedFields.contains("nome")) updates[Colunas.COLUNA_NOME_CONTA] = template.nome
            if (modifiedFields.contains("valor")) updates[Colunas.COLUNA_VALOR_CONTA] = template.valor
            if (modifiedFields.contains("tipo")) updates[Colunas.COLUNA_TIPO_CONTA] = template.tipo
            if (modifiedFields.contains("classe")) updates[Colunas.COLUNA_CLASSE_CONTA] = template.classeConta
            if (modifiedFields.contains("categoria")) updates[Colunas.COLUNA_CATEGORIA_CONTA] = template.categoria
            if (modifiedFields.contains("data")) {
                updates[Colunas.COLUNA_DIA_DATA_CONTA] = template.dia
                updates[Colunas.COLUNA_MES_DATA_CONTA] = template.mes
                updates[Colunas.COLUNA_ANO_DATA_CONTA] = template.ano
            }
            if (modifiedFields.contains("pagamento")) updates[Colunas.COLUNA_PAGOU_CONTA] = template.pagamento
            if (modifiedFields.contains("intervalo")) updates[Colunas.COLUNA_INTERVALO_CONTA] = template.intervalo
            if (modifiedFields.contains("qtRepete")) updates[Colunas.COLUNA_QT_REPETICOES_CONTA] = template.qtRepete
            if (modifiedFields.contains("juros")) updates[Colunas.COLUNA_VALOR_JUROS] = template.valorJuros

            if (updates.isEmpty()) {
                _onTaskComplete.emit(true)
                isLoading = false
                return@launch
            }

            val successCount = withContext(Dispatchers.IO) {
                repository.atualizarContasEmMassa(selectedIds, updates)
            }
            
            _onTaskComplete.emit(successCount > 0)
            isLoading = false
        }
    }
}
