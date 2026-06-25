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

    var isRecurring by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _onTaskComplete = MutableSharedFlow<Boolean>()
    val onTaskComplete: SharedFlow<Boolean> = _onTaskComplete

    fun loadConta(id: Long) {
        viewModelScope.launch {
            isLoading = true
            val c = withContext(Dispatchers.IO) {
                repository.getConta(id)
            }
            conta = c
            if (c != null && c.codigo.isNotEmpty()) {
                // Se qtRepete > 1, já é um forte indicativo de recorrência
                if (c.qtRepete > 1) {
                    isRecurring = true
                } else {
                    // Caso contrário, verificamos se existem outras contas com o mesmo código
                    // Isso trata casos onde a série pode ter sido reduzida para 1 ou importações com UUIDs únicos
                    val filter = DBContas.ContaFilter().setCodigoConta(c.codigo)
                    val series = withContext(Dispatchers.IO) {
                        repository.getContas(filter, null)
                    }
                    isRecurring = series.size > 1
                }
            } else {
                isRecurring = false
            }
            isLoading = false
        }
    }

    fun updateConta(updatedConta: Conta, tipo: DBContas.TipoAtualizacao) {
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