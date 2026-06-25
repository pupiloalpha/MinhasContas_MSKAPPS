package com.msk.minhascontas.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasContract.STATUS_PAGO_RECEBIDO
import com.msk.minhascontas.db.ContasContract.STATUS_PENDENTE
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.tarefas.SalvarContaTarefa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

data class CriarContaUiState(
    val nome: String = "",
    val valor: String = "",
    val dia: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val mes: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val ano: Int = Calendar.getInstance().get(Calendar.YEAR),
    val tipo: Int = ContasContract.TIPO_DESPESA,
    val classe: Int = 0,
    val categoria: Int = ContasContract.CATEGORIA_OUTROS,
    val paga: Boolean = false,
    val parcelar: Boolean = false,
    val qtRepete: String = "",
    val intervaloPosicao: Int = 2, // Mensal
    val juros: String = "",
    val lembrete: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val showAplicacaoDialog: Boolean = false
)

class CriarContaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CriarContaUiState())
    val uiState: StateFlow<CriarContaUiState> = _uiState.asStateFlow()

    fun initData(initialMes: Int, initialAno: Int) {
        val c = Calendar.getInstance()
        var d = c.get(Calendar.DAY_OF_MONTH)
        var m = c.get(Calendar.MONTH) + 1
        var a = c.get(Calendar.YEAR)

        if (initialMes != -1 && initialAno != -1) {
            m = initialMes
            a = initialAno
            val hoje = Calendar.getInstance()
            d = if (m == (hoje.get(Calendar.MONTH) + 1) && a == hoje.get(Calendar.YEAR)) {
                hoje.get(Calendar.DAY_OF_MONTH)
            } else {
                1
            }
        }
        _uiState.update { it.copy(dia = d, mes = m, ano = a) }
    }

    fun updateNome(nome: String) = _uiState.update { it.copy(nome = nome) }
    fun updateValor(valor: String) = _uiState.update { it.copy(valor = valor) }
    fun updateData(ano: Int, mes: Int, dia: Int) = _uiState.update { it.copy(ano = ano, mes = mes, dia = dia) }
    fun updateTipo(tipo: Int) = _uiState.update { 
        val categoria = if (tipo == ContasContract.TIPO_APLICACAO) ContasContract.CATEGORIA_INVESTIMENTOS else it.categoria
        it.copy(tipo = tipo, classe = 0, categoria = categoria) 
    }
    fun updateClasse(classe: Int) = _uiState.update { 
        val newState = it.copy(classe = classe)
        // Melhoria 4: Preenchimento automático ao selecionar Despesa Fixa
        if (it.tipo == ContasContract.TIPO_DESPESA && classe == ContasContract.CLASSE_DESPESA_FIXA) {
            val mesesRestantes = 12 - it.mes + 1
            newState.copy(qtRepete = mesesRestantes.toString(), intervaloPosicao = 2) // 2 = Mensal
        } else {
            newState
        }
    }

    fun repetirAteFimDoAno() = _uiState.update { 
        val totalMeses = 12 - it.mes + 1
        it.copy(qtRepete = (totalMeses - 1).coerceAtLeast(0).toString(), intervaloPosicao = 2)
    }
    fun updateCategoria(categoria: Int) = _uiState.update { it.copy(categoria = categoria) }
    fun updatePaga(paga: Boolean) = _uiState.update { it.copy(paga = paga) }
    fun updateParcelar(parcelar: Boolean) = _uiState.update { it.copy(parcelar = parcelar) }
    fun updateQtRepete(qt: String) = _uiState.update { it.copy(qtRepete = qt) }
    fun updateIntervalo(pos: Int) = _uiState.update { it.copy(intervaloPosicao = pos) }
    fun updateJuros(juros: String) = _uiState.update { it.copy(juros = juros) }
    fun updateLembrete(lembrete: Boolean) = _uiState.update { it.copy(lembrete = lembrete) }
    fun setShowAplicacaoDialog(show: Boolean) = _uiState.update { it.copy(showAplicacaoDialog = show) }

    fun salvar(context: Context, skipDialog: Boolean = false, onSuccess: () -> Unit) {
        val state = _uiState.value
        val nomeFinal = state.nome.ifBlank { context.getString(R.string.sem_nome) }
        val valorFinal = state.valor.replace(",", ".").toDoubleOrNull() ?: 0.0
        val jurosFinal = (state.juros.replace(",", ".").toDoubleOrNull() ?: 0.0) / 100.0
        
        val inputQt = state.qtRepete.toIntOrNull() ?: 0
        var qtRepeteFinal = inputQt + 1
        if (qtRepeteFinal <= 0) qtRepeteFinal = 1

        var valorCalculado = valorFinal
        if (state.parcelar) {
            valorCalculado = valorFinal / qtRepeteFinal
        }

        val intervaloFinal = when (state.intervaloPosicao) {
            0 -> 101
            1 -> 107
            2 -> 300
            3 -> 3650
            else -> 300
        }

        val conta = Conta.Builder(nomeFinal, valorCalculado, state.dia, state.mes, state.ano, UUID.randomUUID().toString())
            .setTipo(state.tipo)
            .setClasseConta(state.classe)
            .setCategoria(state.categoria)
            .setPagamento(if (state.paga) STATUS_PAGO_RECEBIDO else STATUS_PENDENTE)
            .setQtRepete(qtRepeteFinal)
            .setNRepete(1)
            .setIntervalo(if (qtRepeteFinal > 1) intervaloFinal else 0)
            .setValorJuros(jurosFinal)
            .build()

        if (!skipDialog && state.tipo == ContasContract.TIPO_APLICACAO && !state.showAplicacaoDialog) {
            _uiState.update { it.copy(showAplicacaoDialog = true) }
            return
        }

        // Garante que o diálogo seja fechado ao prosseguir com o salvamento
        _uiState.update { it.copy(showAplicacaoDialog = false) }

        executarSalvamento(context, conta, qtRepeteFinal, intervaloFinal, onSuccess)
    }

    fun confirmarAplicacao(context: Context, onSuccess: () -> Unit) {
        _uiState.update { it.copy(showAplicacaoDialog = false) }
        val state = _uiState.value
        val repository = ContasRepository.getInstance(context)
        
        viewModelScope.launch {
            val valorFinal = state.valor.replace(",", ".").toDoubleOrNull() ?: 0.0
            val contaSaque = Conta.Builder(state.nome, valorFinal, state.dia, state.mes, state.ano, UUID.randomUUID().toString())
                .setTipo(ContasContract.TIPO_DESPESA)
                .setClasseConta(ContasContract.CLASSE_DESPESA_FIXA)
                .setCategoria(ContasContract.CATEGORIA_INVESTIMENTOS)
                .setPagamento(STATUS_PAGO_RECEBIDO)
                .setQtRepete(1)
                .setNRepete(1)
                .setIntervalo(0)
                .setValorJuros(0.0)
                .build()
            repository.salvarConta(contaSaque)
            salvar(context, true, onSuccess)
        }
    }

    private fun executarSalvamento(context: Context, conta: Conta, qtRepete: Int, intervalo: Int, onSuccess: () -> Unit) {
        val tarefa = SalvarContaTarefa(conta, qtRepete, intervalo)
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val success = tarefa.executarTarefa(context) { atual, total ->
                _uiState.update { it.copy(progress = atual.toFloat() / total.toFloat()) }
            }
            _uiState.update { it.copy(isLoading = false) }
            
            val message = tarefa.getMensagemResultado(context)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            if (success) {
                onSuccess()
            }
        }
    }
}
