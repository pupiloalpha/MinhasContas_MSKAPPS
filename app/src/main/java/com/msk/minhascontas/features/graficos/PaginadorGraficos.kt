package com.msk.minhascontas.features.graficos

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.msk.minhascontas.ui.MonthYearTabBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.viewmodel.ContasViewModel
import com.msk.minhascontas.viewmodel.ContasViewModel.Companion.calculateDateState

class PaginadorGraficos : AppCompatActivity() {

    private lateinit var repository: ContasRepository
    private lateinit var mViewPager: ViewPager2
    private lateinit var mPaginas: Paginas
    private lateinit var contasViewModel: ContasViewModel

    private val mPaginaListaLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == Ajustes.RESULT_RESTART_REQUIRED) {
            AjustesUtils.pendingRestartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
        } else if (result.resultCode == RESULT_OK) {
            val currentPage = mViewPager.currentItem
            val fragment = mPaginas.getFragment(currentPage)
            if (fragment is GraficoFragment) {
                fragment.refresh()
            }
            Log.d(TAG, "Resultado OK recebido. Gráfico atualizado.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pagina_graficos)

        val mainView = findViewById<View>(R.id.main_content)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            
            findViewById<Toolbar>(R.id.toolbar)?.let { toolbar ->
                toolbar.setPadding(0, 0, 0, 0)
                val params = toolbar.layoutParams
                params.height = (64 * resources.displayMetrics.density).toInt()
                toolbar.layoutParams = params
            }
            insets
        }

        repository = ContasRepository.getInstance(this)
        contasViewModel = ViewModelProvider(this).get(ContasViewModel::class.java)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val toolbarColor = ContextCompat.getColor(this, R.color.primary)
        supportActionBar?.apply {
            setBackgroundDrawable(ColorDrawable(toolbarColor))
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.titulo_grafico)
        }

        var initialPosition = intent.getIntExtra(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE)
        if (savedInstanceState != null) {
            initialPosition = savedInstanceState.getInt(MinhasContas.KEY_PAGINA, initialPosition)
        }
        
        contasViewModel.setViewPagerPosition(initialPosition)

        mPaginas = Paginas(supportFragmentManager, lifecycle)
        mViewPager = findViewById(R.id.paginas)
        mViewPager.adapter = mPaginas

        if (initialPosition < 0) initialPosition = 0
        else if (initialPosition >= mPaginas.itemCount) initialPosition = mPaginas.itemCount - 1
        mViewPager.setCurrentItem(initialPosition, false)

        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                contasViewModel.setViewPagerPosition(position)
            }
        })

        // Substitui o TabLayout nativo pelo MonthYearTabBar (Compose) para evitar ANR
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

        findViewById<ImageButton>(R.id.ibfab).setOnClickListener {
            val intent = Intent("com.msk.minhascontas.NOVACONTA")
            val current = contasViewModel.currentDateState.value
            if (current != null) {
                intent.putExtra(MinhasContas.KEY_PAGINA, current.nrPagina)
                intent.putExtra(MinhasContas.KEY_MES, current.mes)
                intent.putExtra(MinhasContas.KEY_ANO, current.ano)
            }
            mPaginaListaLauncher.launch(intent)
        }

        setupAiObservers()
    }

    private fun setupAiObservers() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.gemini_loading))
            setCancelable(false)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contasViewModel.isAiLoading.collect { isLoading ->
                    if (isLoading == true) progressDialog.show() else progressDialog.dismiss()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                contasViewModel.aiAnalysisResult.collect { result ->
                    result?.let {
                        if (it is AIResult.Success) {
                            AlertDialog.Builder(this@PaginadorGraficos, R.style.TemaDialogo)
                                .setTitle(R.string.gemini_title)
                                .setMessage(Html.fromHtml(it.content, Html.FROM_HTML_MODE_LEGACY))
                                .setPositiveButton(R.string.gemini_entendi) { _, _ -> contasViewModel.clearAiResult() }
                                .setOnCancelListener { contasViewModel.clearAiResult() }
                                .show()
                        } else if (it is AIResult.Error) {
                            val error = it
                            AlertDialog.Builder(this@PaginadorGraficos, R.style.TemaDialogo)
                                .setTitle(R.string.gemini_title)
                                .setMessage(getString(R.string.ai_error_fallback_msg))
                                .setPositiveButton(R.string.ai_send_gemini) { _, _ ->
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/")))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@PaginadorGraficos, "Erro ao abrir Gemini", Toast.LENGTH_SHORT).show()
                                    }
                                    contasViewModel.clearAiResult()
                                }
                                .setNeutralButton(R.string.ai_copy_prompt) { _, _ ->
                                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AI Prompt", error.fullPrompt)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(this@PaginadorGraficos, R.string.ai_prompt_copied, Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton(R.string.cancelar) { _, _ -> contasViewModel.clearAiResult() }
                                .show()
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {
            val currentPage = mViewPager.currentItem
            val fragment = mPaginas.getFragment(currentPage)
            if (fragment is GraficoFragment) {
                fragment.refresh()
            }
        }
    }

    private fun finishWithResult() {
        val returnIntent = Intent()
        returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.currentItem)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        finishWithResult()
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.barra_botoes_lista, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finishWithResult()
                return true
            }
            R.id.menu_ajustes -> {
                mPaginaListaLauncher.launch(Intent(this, Ajustes::class.java))
                return true
            }
            R.id.menu_sobre -> {
                startActivity(Intent("com.msk.minhascontas.SOBRE"))
                return true
            }
            R.id.botao_pesquisar -> {
                val intent = Intent("com.msk.minhascontas.BUSCACONTA")
                intent.putExtra(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
                mPaginaListaLauncher.launch(intent)
                return true
            }
            R.id.menu_ia_assistente -> {
                val state = contasViewModel.currentDateState.value
                if (state != null) {
                    val contas = repository.getContasDoMes(state.mes, state.ano, -1, null)
                    contasViewModel.runAiAnalysis(contas)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class Paginas(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        private val mFragmentos = HashMap<Int, Fragment>()

        override fun createFragment(position: Int): Fragment {
            val isMonthly = contasViewModel.viewState.value?.isMonthlySummary ?: true
            val dateState = calculateDateState(position, isMonthly)
            val fragment = if (isMonthly) {
                GraficoFragment.newInstance(dateState.mes, dateState.ano)
            } else {
                GraficoFragment.newInstance(dateState.mes, dateState.ano, dateState.dia)
            }
            mFragmentos[position] = fragment
            return fragment
        }

        override fun getItemCount() = MinhasContas.START_PAGE * 2

        fun getFragment(position: Int): Fragment? = mFragmentos[position]
    }

    class GraficoFragment : Fragment() {
        private var mes: Int = 1
        private var ano: Int = 2024
        private var dia: Int? = null
        private var refreshTrigger = mutableIntStateOf(0)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            arguments?.let {
                mes = it.getInt("mes")
                ano = it.getInt("ano")
                if (it.containsKey("dia")) {
                    dia = it.getInt("dia")
                }
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return ComposeView(requireContext()).apply {
                setContent {
                    MinhasContasTheme {
                        GraficosScreen(mes = mes, ano = ano, dia = dia, refreshTrigger = refreshTrigger.intValue)
                    }
                }
            }
        }

        fun refresh() {
            refreshTrigger.intValue += 1
        }

        companion object {
            fun newInstance(mes: Int, ano: Int, dia: Int? = null) = GraficoFragment().apply {
                arguments = Bundle().apply {
                    putInt("mes", mes)
                    putInt("ano", ano)
                    dia?.let { putInt("dia", it) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PaginadorGraficos"
    }
}
