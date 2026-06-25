package com.msk.minhascontas.features.listas

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.msk.minhascontas.ui.MonthYearTabBar
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContasRepository.Companion.getInstance
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.features.listas.ListaMensalContas.Companion.newInstance
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.utils.LabelUtils.getClasseLabel
import com.msk.minhascontas.viewmodel.ContasViewModel
import com.msk.minhascontas.viewmodel.ContasViewModel.Companion.calculateDateState
import java.text.NumberFormat

class PaginadorListas : AppCompatActivity() {
    private lateinit var repository: ContasRepository
    private lateinit var mViewPager: ViewPager2
    private lateinit var mPaginas: Paginas
    private lateinit var dinheiro: NumberFormat
    private var classes: Array<String>? = null
    private lateinit var contasViewModel: ContasViewModel
    private var tipo = 0
    private var filtro = 0

    // ActivityResultLauncher para CriarConta.java ou Ajustes.java
    private val mPaginaListaLauncher = registerForActivityResult(
        StartActivityForResult(),
        ActivityResultCallback { result: ActivityResult ->
            if (result.resultCode == Ajustes.RESULT_RESTART_REQUIRED) {
                AjustesUtils.pendingRestartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
            } else if (result.resultCode == RESULT_OK) {
                // Após criar ou editar uma conta, o fragmento atual precisa recarregar
                val currentPage = mViewPager.currentItem
                val fragment = mPaginas.getFragment(currentPage)
                if (fragment is ListaMensalContas) {
                    fragment.refreshLista()
                }
                AtualizaActionBar()
                Log.d(TAG, "Resultado OK recebido. ActionBar atualizado e fragmento recarregado.")
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pagina_lista_mensal)

        val mainView = findViewById<View?>(R.id.main_content)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(
                mainView,
                OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                    val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                    v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    
                    findViewById<Toolbar>(R.id.toolbar)?.let { toolbar ->
                        toolbar.setPadding(0, 0, 0, 0)
                        val params = toolbar.layoutParams
                        params.height = (64 * resources.displayMetrics.density).toInt()
                        toolbar.layoutParams = params
                    }
                    insets
                })
        }

        contasViewModel = ViewModelProvider(this).get(ContasViewModel::class.java)

        val current = resources.configuration.locales.get(0)
        dinheiro = NumberFormat.getCurrencyInstance(current)

        repository = getInstance(this)

        val localBundle = intent.extras
        var tipoFromMinhasContas = ContasContract.TIPO_DESPESA
        var initialPosition: Int = START_PAGE

        if (localBundle != null) {
            tipoFromMinhasContas = localBundle.getInt("tipo", ContasContract.TIPO_DESPESA)
            val receivedPosition = localBundle.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE)

            if (receivedPosition >= 0) {
                initialPosition = receivedPosition
            }
        }

        if (savedInstanceState != null) {
            initialPosition = savedInstanceState.getInt(MinhasContas.KEY_PAGINA, initialPosition)
        }
        this.tipo = tipoFromMinhasContas

        contasViewModel.setViewPagerPosition(initialPosition)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dynamicColor = when (tipo) {
            ContasContract.TIPO_RECEITA -> ContextCompat.getColor(this, R.color.azul)
            ContasContract.TIPO_DESPESA -> ContextCompat.getColor(this, R.color.vermelho)
            ContasContract.TIPO_APLICACAO -> ContextCompat.getColor(this, R.color.verde)
            else -> ContextCompat.getColor(this, R.color.primary)
        }

        supportActionBar?.apply {
            setBackgroundDrawable(ColorDrawable(dynamicColor))
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            repository.confirmaPagamentos()
            repository.ajustaRepeticoesContas()
        }

        if (localBundle != null && localBundle.containsKey("filtro")) {
            this.filtro = localBundle.getInt("filtro")
        } else {
            if (tipo == -1) filtro = -2
            else filtro = -1
        }

        findViewById<ImageButton>(R.id.ibfab).setOnClickListener {
            val intent = Intent("com.msk.minhascontas.NOVACONTA")
            val currentDate = contasViewModel.currentDateState.value
            if (currentDate != null) {
                intent.putExtra(MinhasContas.KEY_PAGINA, currentDate.nrPagina)
                intent.putExtra(MinhasContas.KEY_MES, currentDate.mes)
                intent.putExtra(MinhasContas.KEY_ANO, currentDate.ano)
            } else {
                intent.putExtra(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
            }
            mPaginaListaLauncher.launch(intent)
        }

        mPaginas = Paginas(supportFragmentManager, lifecycle)

        mViewPager = findViewById(R.id.paginas)
        mViewPager.adapter = mPaginas

        if (initialPosition < 0) initialPosition = 0
        else if (initialPosition >= mPaginas.itemCount) initialPosition = mPaginas.itemCount - 1
        mViewPager.setCurrentItem(initialPosition, false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contasViewModel.currentDateState.collect {
                    AtualizaActionBar()
                }
            }
        }

        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                contasViewModel.setViewPagerPosition(position)
                if (ListaMensalContas.mActionMode != null) ListaMensalContas.mActionMode!!.finish()
            }
        })

        findViewById<ComposeView>(R.id.tablayout)?.setContent {
            val viewPagerPosition by contasViewModel.viewPagerPosition.collectAsState(MinhasContas.START_PAGE)
            MinhasContasTheme {
                MonthYearTabBar(
                    selectedPosition = viewPagerPosition ?: MinhasContas.START_PAGE,
                    contasViewModel = contasViewModel,
                    pageCount = mPaginas.itemCount,
                    onPositionSelected = { position ->
                        mViewPager.currentItem = position
                    }
                )
            }
        }

        // Observadores para a IA
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.gemini_loading))
        progressDialog.setCancelable(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contasViewModel.isAiLoading.collect { isLoading: Boolean? ->
                    if (isLoading == true) progressDialog.show()
                    else progressDialog.dismiss()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contasViewModel.aiAnalysisResult.collect { result: AIResult? ->
                    if (result != null) {
                        if (result is AIResult.Success) {
                            AlertDialog.Builder(this@PaginadorListas, R.style.TemaDialogo)
                                .setTitle(R.string.gemini_title)
                                .setMessage(Html.fromHtml(result.content, Html.FROM_HTML_MODE_LEGACY))
                                .setPositiveButton(
                                    R.string.gemini_entendi,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> contasViewModel.clearAiResult() })
                                .setOnCancelListener(DialogInterface.OnCancelListener { dialog: DialogInterface? ->
                                    contasViewModel.clearAiResult()
                                    dialog!!.dismiss()
                                })
                                .show()
                        } else if (result is AIResult.Error) {
                            val error = result
                            AlertDialog.Builder(this@PaginadorListas, R.style.TemaDialogo)
                                .setTitle(R.string.gemini_title)
                                .setMessage(getString(R.string.ai_error_fallback_msg))
                                .setPositiveButton(
                                    R.string.ai_send_gemini,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://gemini.google.com/")
                                            )
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(this@PaginadorListas, R.string.erro_abrir_gemini, Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                        contasViewModel.clearAiResult()
                                    })
                                .setNeutralButton(
                                    R.string.ai_copy_prompt,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        val clipboard = getSystemService(
                                            CLIPBOARD_SERVICE
                                        ) as ClipboardManager
                                        val clip = ClipData.newPlainText(getString(R.string.ai_prompt_clipboard), error.fullPrompt)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(this@PaginadorListas, R.string.ai_prompt_copied, Toast.LENGTH_SHORT)
                                            .show()
                                    })
                                .setNegativeButton(
                                    R.string.cancelar,
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        contasViewModel.clearAiResult()
                                        dialog!!.dismiss()
                                    })
                                .setOnCancelListener(DialogInterface.OnCancelListener { dialog: DialogInterface? ->
                                    contasViewModel.clearAiResult()
                                    dialog!!.dismiss()
                                })
                                .show()
                        }
                    }
                }
            }
        }

        AtualizaActionBar()
    }

    private fun prepararResultadoParaRetorno() {
        val resultIntent = Intent()
        resultIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.currentItem)
        setResult(RESULT_OK, resultIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        prepararResultadoParaRetorno()
        super.onBackPressed()
    }

    private fun getFiltroLabels(tipo: Int): Array<String> {
        val labels: Array<String>
        val numClasses: Int

        if (tipo == ContasContract.TIPO_DESPESA) {
            labels = resources.getStringArray(R.array.FiltroDespesa)
            numClasses = resources.getStringArray(R.array.TipoDespesa).size
        } else if (tipo == ContasContract.TIPO_RECEITA) {
            labels = resources.getStringArray(R.array.FiltroReceita)
            numClasses = resources.getStringArray(R.array.TipoReceita).size
        } else if (tipo == ContasContract.TIPO_APLICACAO) {
            labels = resources.getStringArray(R.array.FiltroAplicacao)
            numClasses = resources.getStringArray(R.array.TipoAplicacao).size
        } else {
            return emptyArray()
        }

        for (i in 0..<numClasses) {
            labels[i] = getClasseLabel(this, tipo, i)
        }
        return labels
    }

    private fun FiltroContas() {
        val dialogoBuilder = AlertDialog.Builder(this, R.style.TemaDialogo)
        dialogoBuilder.setTitle(getString(R.string.titulo_filtro))

        if (tipo == ContasContract.TIPO_DESPESA) {
            classes = getFiltroLabels(tipo)
            dialogoBuilder.setItems(
                classes,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    if (id < 6) {
                        filtro = id
                    } else {
                        filtro = -1
                    }
                    aplicarFiltroAoFragmentoAtual()
                })
        }
        if (tipo == ContasContract.TIPO_RECEITA) {
            classes = getFiltroLabels(tipo)
            dialogoBuilder.setItems(
                classes,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    if (id < 5) {
                        filtro = id
                    } else {
                        filtro = -1
                    }
                    aplicarFiltroAoFragmentoAtual()
                })
        }

        if (tipo == ContasContract.TIPO_APLICACAO) {
            classes = getFiltroLabels(tipo)
            dialogoBuilder.setItems(
                classes,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                    if (id < 3) {
                        filtro = id
                    } else {
                        filtro = -1
                    }
                    aplicarFiltroAoFragmentoAtual()
                })
        }
        val alertDialog = dialogoBuilder.create()
        alertDialog.show()
    }

    private fun aplicarFiltroAoFragmentoAtual() {
        val currentPage = mViewPager.currentItem
        val fragment = mPaginas.getFragment(currentPage)
        if (fragment is ListaMensalContas) {
            fragment.updateFilter(filtro)
        }
        AtualizaActionBar()
    }

    private fun MontaLista() {
        aplicarFiltroAoFragmentoAtual()
    }

    fun AtualizaActionBar() {
        if (supportActionBar == null) return

        var valores = 0.0
        classes = null

        val currentDateState = contasViewModel.currentDateState.value
        if (currentDateState == null) return
        val currentMes = currentDateState.mes
        val currentAno = currentDateState.ano

        if (tipo == ContasContract.TIPO_RECEITA) {
            supportActionBar!!.title = resources.getString(R.string.linha_receita)
            classes = getFiltroLabels(tipo)
        } else if (tipo == ContasContract.TIPO_DESPESA) {
            supportActionBar!!.title = resources.getString(R.string.linha_despesa)
            classes = getFiltroLabels(tipo)
        } else if (tipo == ContasContract.TIPO_APLICACAO) {
            supportActionBar!!.title = resources.getString(R.string.linha_aplicacoes)
            classes = getFiltroLabels(tipo)
        } else if (tipo == -1) {
            supportActionBar!!.title = resources.getString(R.string.app_name)
        }

        if (filtro >= 0) {
            if (tipo == ContasContract.TIPO_DESPESA && filtro == 4) { // Despesa FALTA
                valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, -1, -1, ContasContract.STATUS_PENDENTE)
            } else if (tipo == ContasContract.TIPO_DESPESA && filtro == 5) { // Despesa PAGO
                valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO)
            } else if (tipo == ContasContract.TIPO_RECEITA && filtro == 3) { // Receita FALTA
                valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, -1, -1, ContasContract.STATUS_PENDENTE)
            } else if (tipo == ContasContract.TIPO_RECEITA && filtro == 4) { // Receita PAGO
                valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO)
            } else { // Filtro por Classe
                valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, filtro, -1, null)
            }

            if (classes != null && filtro < classes!!.size) {
                supportActionBar!!.title = classes!![filtro]
            }

        } else {
            valores = repository.somaValoresPorFiltro(currentAno, currentMes, tipo, -1, -1, null)
        }

        if (tipo != -1)
            supportActionBar!!.subtitle = dinheiro.format(valores)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (tipo == -1) {
            menuInflater.inflate(R.menu.barra_botoes_lista, menu)
        } else {
            menuInflater.inflate(R.menu.barra_botoes_filtra_lista, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            val returnIntent = Intent()
            returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.currentItem)
            setResult(RESULT_OK, returnIntent)
            finish()
            return true
        } else if (itemId == R.id.menu_ajustes) {
            val intent = Intent(this, Ajustes::class.java)
            mPaginaListaLauncher.launch(intent)
            return true
        } else if (itemId == R.id.menu_sobre) {
            startActivity(Intent("com.msk.minhascontas.SOBRE"))
            return true
        } else if (itemId == R.id.botao_pesquisar) {
            val intent = Intent("com.msk.minhascontas.BUSCACONTA")
            intent.putExtra(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
            mPaginaListaLauncher.launch(intent)
            return true
        } else if (itemId == R.id.menu_ia_assistente) {
            val state = contasViewModel.currentDateState.value
            if (state != null) {
                val contas = repository.getContasDoMes(state.mes, state.ano, -1, null)
                contasViewModel.runAiAnalysis(contas)
            }
            return true
        } else if (itemId == R.id.botao_filtrar) {
            FiltroContas()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {
            MontaLista()
        }
    }

    inner class Paginas(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        private val mFragmentos = HashMap<Int, Fragment>()

        override fun createFragment(position: Int): Fragment {
            val isMonthly = contasViewModel.viewState.value?.isMonthlySummary ?: true
            val dateState = calculateDateState(position, isMonthly)
            val diaParaFragment = if (isMonthly) 0 else dateState.dia
            val fragment: Fragment = newInstance(dateState.mes, dateState.ano, diaParaFragment, tipo, filtro)
            mFragmentos[position] = fragment
            return fragment
        }

        override fun getItemCount(): Int {
            return START_PAGE * 2
        }

        fun getFragment(position: Int): Fragment? {
            return mFragmentos[position]
        }
    }

    companion object {
        private const val TAG = "PaginadorListas"
        private val START_PAGE = MinhasContas.START_PAGE
    }
}
