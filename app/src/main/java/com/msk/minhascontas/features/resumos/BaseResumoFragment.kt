package com.msk.minhascontas.features.resumos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.card.MaterialCardView
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContasRepository.Companion.getInstance
import com.msk.minhascontas.db.DBContas.ContaFilter
import com.msk.minhascontas.viewmodel.ContasViewModel
import com.msk.minhascontas.viewmodel.ContasViewModel.DateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

// Adicionar import
abstract class BaseResumoFragment : Fragment(), View.OnClickListener {
    protected var dia: Int = 0
    protected var mes: Int = 0
    protected var ano: Int = 0
    protected var nrPagina: Int = 0

    protected var contasViewModel: ContasViewModel? = null

    protected var valores: DoubleArray? = null
    protected var valoresDesp: DoubleArray? = null
    protected var valoresRec: DoubleArray? = null
    protected var valoresSaldo: DoubleArray? = null
    protected var valoresAplicados: DoubleArray? = null
    protected var valoresAplicAnterior: DoubleArray? = null

    protected var repository: ContasRepository? = null
    protected var buscaPreferencias: SharedPreferences? = null
    protected var preferences: SharedPreferences? = null
    protected val dadosMes: Bundle = Bundle()

    protected var layoutAplicacoes: MaterialCardView? = null
    protected var layoutDespesas: MaterialCardView? = null
    protected var layoutReceitas: MaterialCardView? = null
    protected var layoutSaldo: MaterialCardView? = null

    private var refreshJob: kotlinx.coroutines.Job? = null

    private val mContasMesLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result!!.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Garante que getActivity() não é nulo e é uma instância de MinhasContas
            val activity: Activity? = activity
            if (data != null && data.hasExtra(MinhasContas.RETURN_KEY_PAGINA) && activity is MinhasContas) {
                val returnedPosition =
                    data.getIntExtra(MinhasContas.RETURN_KEY_PAGINA, nrPagina)
                activity.syncViewPagerPositionAndRefresh(returnedPosition)
                Log.d(
                    "BaseResumoFragment",
                    "Posição retornada: $returnedPosition. Sincronizando ViewPager principal."
                )
            } else {
                // Se não retornou a chave de página, os dados podem ter mudado de qualquer forma.
                // Recarregamos os dados do fragmento atual.
                Log.d(
                    "BaseResumoFragment",
                    "Dados alterados ou RETURN_KEY_PAGINA ausente. Chamando refreshData() no fragmento atual."
                )
                refreshData() // Adicionado para garantir o refresh, mesmo sem a chave de retorno.
            }
        } else {
            Log.d(
                "BaseResumoFragment",
                "Resultado da Activity filha não foi OK. Nenhum refresh automático."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = getInstance(requireContext())
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        contasViewModel =
            ViewModelProvider(requireActivity())[ContasViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        repository = getInstance(context)
        buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(this.layoutResId, container, false)

        val args = arguments
        if (args != null) {
            ano = args.getInt("ano", 0)
            mes = args.getInt("mes", 1)
            dia = args.getInt("dia", 0)
            nrPagina = args.getInt(ARG_NR_PAGINA, MinhasContas.START_PAGE)
            Log.d(
                "BaseResumoFragment",
                "onCreateView: Argumentos iniciais - mes: $mes, ano: $ano, nrPagina: $nrPagina"
            )
        }

        initializeArrays()
        iniciarViews(rootView)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(
            "BaseResumoFragment",
            "onViewCreated: Initializing click listeners and LiveData observers."
        )

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        layoutAplicacoes?.setOnClickListener(this)
        layoutDespesas?.setOnClickListener(this)
        layoutReceitas?.setOnClickListener(this)
        layoutSaldo?.setOnClickListener(this)
    }

    private fun setupObservers() {
        // NOVO: Observação reativa do Flow de Contas do Room (Fonte única de verdade)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                contasViewModel?.currentContas?.collect { lista ->
                    Log.d("BaseResumoFragment", "Flow de contas recebido: ${lista.size} itens.")
                    processarDadosResumo(lista)
                }
            }
        }
    }

    private fun processarDadosResumo(lista: List<Conta>) {
        lifecycleScope.launch {
            // 1. Processamento (Somas de meses anteriores ainda podem precisar de IO)
            withContext(Dispatchers.IO) {
                if (context != null && buscaPreferencias == null) {
                    buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(requireContext())
                }
                saldo(lista)
            }

            // 2. UI Update na Main Thread
            if (isAdded && !isDetached) {
                insereValores()
                onDadosAtualizados()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // O Flow reativo agora garante a sincronização automática.
    }

    abstract fun onDadosAtualizados()
    protected abstract val layoutResId: Int
    protected abstract fun initializeArrays()
    protected abstract fun iniciarViews(view: View?)
    
    // MUDANÇA: Novo método saldo recebendo a lista do Flow
    protected abstract fun saldo(contasAtuais: List<Conta>)
    
    protected abstract fun insereValores()
    protected abstract val contaFilter: ContaFilter?

    override fun onClick(v: View) {
        Log.d("BaseResumoFragment", "onClick: Clique detectado na view com ID: ${v.id}")

        val viewId = v.id
        val tipo = when (viewId) {
            R.id.resumo_saldo -> -1
            R.id.resumo_aplicacoes -> ContasContract.TIPO_APLICACAO
            R.id.resumo_despesas -> ContasContract.TIPO_DESPESA
            R.id.resumo_receitas -> ContasContract.TIPO_RECEITA
            else -> -1
        }

        // Tenta usar a ação definida na activity (MinhasContas)
        val act = activity
        if (act is MinhasContas) {
            act.onResumoCardClickAction?.invoke(tipo, -1)
        } else {
            // Comportamento original se não estiver em MinhasContas
            dadosMes.putInt("mes", mes)
            dadosMes.putInt("ano", ano)
            dadosMes.putInt("dia", dia)
            dadosMes.putInt(MinhasContas.KEY_PAGINA, nrPagina)
            dadosMes.putInt("tipo", tipo)
            
            val mostraResumo = Intent("com.msk.minhascontas.CONTASDOMES")
            mostraResumo.putExtras(dadosMes)
            mContasMesLauncher.launch(mostraResumo)
        }
    }

    protected val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance()

    protected fun getBalanceColor(value: Double): Int {
        val ctx = context ?: return android.graphics.Color.BLACK
        return if (value < 0) {
            ContextCompat.getColor(ctx, R.color.despesa_color)
        } else {
            ContextCompat.getColor(ctx, R.color.receita_color)
        }
    }

    protected fun getSaldoMesAnterior(mes: Int, ano: Int): Double {
        val mesAnt = if (mes == 1) 12 else mes - 1
        val anoAnt = if (mes == 1) ano - 1 else ano
        
        val recAnt = getSumForFilter(ContaFilter().setMes(mesAnt).setAno(anoAnt).setTipo(ContasContract.TIPO_RECEITA))
        val despAnt = getSumForFilter(ContaFilter().setMes(mesAnt).setAno(anoAnt).setTipo(ContasContract.TIPO_DESPESA))
        
        return recAnt - despAnt
    }

    protected fun getSumForFilter(filter: ContaFilter?): Double {
        val repo = repository ?: return 0.0
        val mes = filter?.mes ?: -1
        val ano = filter?.ano ?: -1
        val tipo = filter?.tipo ?: -1
        return repo.calcularTotalMensal(mes, ano, tipo, filter)
    }

    fun refreshData() {
        // Agora o Flow reativo no BaseResumoFragment.setupObservers() cuida disso.
    }

    companion object {
        const val ARG_NR_PAGINA: String = "nrPagina"
    }
}