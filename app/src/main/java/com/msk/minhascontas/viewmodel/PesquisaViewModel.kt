package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.ContaFilter
import com.msk.minhascontas.db.TipoExclusao
import com.msk.minhascontas.utils.LabelUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

class PesquisaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus: StateFlow<String?> = _filterStatus

    private val _filterTipo = MutableStateFlow<Int?>(null)
    val filterTipo: StateFlow<Int?> = _filterTipo

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val contas: StateFlow<List<Conta>> = combine(
        _searchText.debounce(300),
        _filterStatus,
        _filterTipo
    ) { text, status, tipo ->
        val filter = ContaFilter()
        if (text.isNotBlank()) {
            val (categories, classes, types) = resolveSearchIndices(text)
            filter.setNome(text)
            filter.setCategoriasIn(categories)
            filter.setClassesIn(classes)
            filter.setTiposIn(types)
            
            // Pesquisa por Valor
            val valor = parseDouble(text)
            if (valor != null) filter.setValorGlobal(valor)

            // Pesquisa por Data (Inteligente)
            val (dia, mes, ano) = parseDate(text)
            if (dia != null) filter.setDiaGlobal(dia)
            if (mes != null) filter.setMesGlobal(mes)
            if (ano != null) filter.setAnoGlobal(ano)

            filter.setPesquisaGlobal(true)
        }
        
        if (status != null) filter.setPagamento(status)
        if (tipo != null) filter.setTipo(tipo)
        
        filter
    }.flatMapLatest { filter ->
        repository.getContasFlow(filter, null)
    }
    .flowOn(Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilterStatus(status: String?) {
        _filterStatus.value = if (_filterStatus.value == status) null else status
    }

    fun setFilterTipo(tipo: Int?) {
        _filterTipo.value = if (_filterTipo.value == tipo) null else tipo
    }

    private fun parseDouble(text: String): Double? {
        val context = getApplication<Application>()
        val locale = context.resources.configuration.locales[0]
        val numberFormat = java.text.NumberFormat.getNumberInstance(locale)
        
        // Remove currency symbols and extra spaces
        val cleanText = text.replace(Regex("[^0-9,.\\-\\s]"), "").trim()
        
        return try {
            numberFormat.parse(cleanText)?.toDouble()
        } catch (e: Exception) {
            // Fallback for simple numeric string
            cleanText.toDoubleOrNull()
        }
    }

    private fun parseDate(text: String): Triple<Int?, Int?, Int?> {
        val context = getApplication<Application>()
        val locale = context.resources.configuration.locales[0]
        
        // Detect if the locale uses Day before Month (like PT, ES, FR) or Month before Day (like EN-US)
        val pattern = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
            .let { (it as java.text.SimpleDateFormat).toPattern() }
        val isDayBeforeMonth = pattern.indexOf('d') < pattern.indexOf('M')

        val parts = text.split("/", "-", ".", " ")
        var dia: Int? = null
        var mes: Int? = null
        var ano: Int? = null

        when (parts.size) {
            1 -> {
                val n = parts[0].toIntOrNull()
                if (n != null) {
                    if (n in 1..31) dia = n
                    if (n in 1..12) mes = n
                    if (n in 1900..2100) ano = n
                }
            }
            2 -> {
                val n1 = parts[0].toIntOrNull()
                val n2 = parts[1].toIntOrNull()
                if (n1 != null && n2 != null) {
                    if (n1 in 1..31 && n2 in 1..12) {
                        if (isDayBeforeMonth) {
                            dia = n1
                            mes = n2
                        } else {
                            mes = n1
                            dia = n2
                        }
                    } else if (n1 in 1..12 && n2 in 1900..2100) {
                        mes = n1
                        ano = n2
                    }
                }
            }
            3 -> {
                val n1 = parts[0].toIntOrNull()
                val n2 = parts[1].toIntOrNull()
                val n3 = parts[2].toIntOrNull()
                if (n1 != null && n2 != null && n3 != null) {
                    // Typical DD/MM/YYYY or MM/DD/YYYY
                    if (isDayBeforeMonth) {
                        if (n1 in 1..31 && n2 in 1..12) {
                            dia = n1
                            mes = n2
                            ano = if (n3 < 100) 2000 + n3 else n3
                        }
                    } else {
                        if (n2 in 1..31 && n1 in 1..12) {
                            mes = n1
                            dia = n2
                            ano = if (n3 < 100) 2000 + n3 else n3
                        }
                    }
                }
            }
        }
        return Triple(dia, mes, ano)
    }

    private fun resolveSearchIndices(text: String): Triple<List<Int>, List<Int>, List<Int>> {
        val context = getApplication<Application>()
        val matchedCategories = mutableListOf<Int>()
        val matchedClasses = mutableListOf<Int>()
        val matchedTypes = mutableListOf<Int>()

        val query = text.trim().lowercase()

        // 1. Categorias
        for (i in 0..8) {
            val label = LabelUtils.getCategoriaLabel(context, i).lowercase()
            if (label.contains(query)) {
                matchedCategories.add(i)
            }
        }

        // 2. Classes (Tipos de Despesa, Receita, Aplicação)
        // Despesa (0)
        for (i in 0..3) {
            val label = LabelUtils.getClasseLabel(context, 0, i).lowercase()
            if (label.contains(query)) matchedClasses.add(i)
        }
        // Receita (1)
        for (i in 0..2) {
            val label = LabelUtils.getClasseLabel(context, 1, i).lowercase()
            if (label.contains(query)) matchedClasses.add(i)
        }
        // Aplicação (2)
        for (i in 0..3) {
            val label = LabelUtils.getClasseLabel(context, 2, i).lowercase()
            if (label.contains(query)) matchedClasses.add(i)
        }

        // 3. Tipos (Despesa, Receita, Aplicação)
        val tipoLabels = mapOf(
            ContasContract.TIPO_DESPESA to context.getString(R.string.linha_despesa),
            ContasContract.TIPO_RECEITA to context.getString(R.string.linha_receita),
            ContasContract.TIPO_APLICACAO to context.getString(R.string.linha_aplicacoes)
        )
        tipoLabels.forEach { (tipo, label) ->
            if (label.lowercase().contains(query)) {
                matchedTypes.add(tipo)
            }
        }

        return Triple(matchedCategories, matchedClasses, matchedTypes)
    }

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

    fun togglePagamento(id: Long) {
        viewModelScope.launch {
            val conta = repository.getConta(id)
            if (conta != null) {
                val newStatus = if (ContasContract.STATUS_PAGO_RECEBIDO == conta.pagamento)
                    ContasContract.STATUS_PENDENTE
                else
                    ContasContract.STATUS_PAGO_RECEBIDO
                repository.atualizarPagamento(id, newStatus)
            }
        }
    }

    fun togglePagamentoSelected() {
        val selected = _selectedIds.value.toList()
        viewModelScope.launch {
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
            clearSelection()
        }
    }

    fun deleteMultipleSelected() {
        val selected = _selectedIds.value.toList()
        viewModelScope.launch {
            selected.forEach { id ->
                repository.excluirConta(id)
            }
            clearSelection()
        }
    }

    fun deleteSingle(id: Long, tipoExclusao: TipoExclusao) {
        viewModelScope.launch {
            val conta = repository.getConta(id)
            if (conta != null) {
                when (tipoExclusao) {
                    TipoExclusao.SOMENTE_ESTA -> repository.excluirConta(id)
                    TipoExclusao.DESTA_EM_DIANTE -> repository.excluirContasRecorrentes(
                        id, conta.codigo, conta.nRepete, TipoExclusao.DESTA_EM_DIANTE
                    )
                    TipoExclusao.TODAS_AS_REPETICOES -> repository.excluirContasRecorrentes(
                        id, conta.codigo, 1, TipoExclusao.TODAS_AS_REPETICOES
                    )
                }
            }
            clearSelection()
        }
    }
    
    suspend fun getConta(id: Long): Conta? = repository.getConta(id)

    /**
     * Verifica se uma conta é um registro de Meta (formato [COACH]).
     */
    suspend fun isContaMeta(id: Long): Boolean {
        val conta = repository.getConta(id)
        return conta?.nome?.startsWith("[COACH]") == true
    }
}
