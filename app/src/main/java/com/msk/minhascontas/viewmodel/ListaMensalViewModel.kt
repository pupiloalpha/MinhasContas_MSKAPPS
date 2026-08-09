import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContaFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ListaMensalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val keyOrdem = application.getString(R.string.pref_key_ordem)

    private val _mes = MutableStateFlow(1)
    private val _ano = MutableStateFlow(2000)
    private val _dia = MutableStateFlow(0)
    private val _tipo = MutableStateFlow(-1)
    private val _filtro = MutableStateFlow(-1)
    private val _categoria = MutableStateFlow(-1)
    private val _ordem = MutableStateFlow(prefs.getString(keyOrdem, "dia_data ASC") ?: "dia_data ASC")

    init {
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == keyOrdem) {
                _ordem.value = sharedPreferences.getString(key, "dia_data ASC") ?: "dia_data ASC"
            }
        }
    }

    val contas: StateFlow<List<Conta>> = combine(
        _mes, _ano, _dia, _tipo, _filtro, _categoria, _ordem
    ) { args: Array<Any?> ->
        FilterState(
            mes = args[0] as Int,
            ano = args[1] as Int,
            dia = args[2] as Int,
            tipo = args[3] as Int,
            filtro = args[4] as Int,
            categoria = args[5] as Int,
            ordem = args[6] as String
        )
    }.flatMapLatest { state ->
        if (state.tipo == -2) {
            val filterDespesas = ContaFilter().setMes(state.mes).setAno(state.ano)
                .setTipo(0) // ContasContract.TIPO_DESPESA
                .setCategoria(8) // ContasContract.CATEGORIA_INVESTIMENTOS
            if (state.dia > 0) filterDespesas.setDiaFim(state.dia)

            val filterAplicacoes = ContaFilter().setMes(state.mes).setAno(state.ano)
                .setTipo(2) // ContasContract.TIPO_APLICACAO
            if (state.dia > 0) filterAplicacoes.setDiaFim(state.dia)

            combine(
                repository.getContasFlow(filterDespesas, state.ordem),
                repository.getContasFlow(filterAplicacoes, state.ordem)
            ) { d, a -> d + a }
        } else {
            val filter = ContaFilter().setMes(state.mes).setAno(state.ano)
            if (state.dia > 0) filter.setDiaFim(state.dia)
            if (state.tipo != -1) {
                filter.setTipo(state.tipo)
                if (state.categoria != -1) {
                    filter.setCategoria(state.categoria)
                } else {
                    applyFiltro(filter, state.tipo, state.filtro)
                }
            } else if (state.categoria != -1) {
                filter.setCategoria(state.categoria)
            }
            repository.getContasFlow(filter, state.ordem)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterState(
        val mes: Int, val ano: Int, val dia: Int, 
        val tipo: Int, val filtro: Int, val categoria: Int,
        val ordem: String
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

    fun updateState(mes: Int, ano: Int, dia: Int, tipo: Int, filtro: Int, categoria: Int) {
        _mes.value = mes
        _ano.value = ano
        _dia.value = dia
        _tipo.value = tipo
        _filtro.value = filtro
        _categoria.value = categoria
    }

    fun setOrdem(ordem: String) {
        _ordem.value = ordem
    }

    fun togglePagamento(conta: Conta) {
        viewModelScope.launch {
            val novoStatus = if ("paguei" == conta.pagamento) "" else "paguei"
            repository.atualizarPagamento(conta.idConta, novoStatus)
        }
    }
}
