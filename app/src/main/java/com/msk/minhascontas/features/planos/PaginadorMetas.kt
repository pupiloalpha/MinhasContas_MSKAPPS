package com.msk.minhascontas.features.planos

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
import com.msk.minhascontas.db.ContasRepository.Companion.getInstance
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.features.planos.MetasFragment.Companion.newInstance
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.viewmodel.ContasViewModel
import com.msk.minhascontas.viewmodel.ContasViewModel.Companion.calculateDateState

class PaginadorMetas : AppCompatActivity() {
    private lateinit var repository: ContasRepository
    private lateinit var mPaginas: Paginas
    private lateinit var mViewPager: ViewPager2
    private lateinit var contasViewModel: ContasViewModel

    private val mPaginaListaLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result!!.resultCode == Ajustes.RESULT_RESTART_REQUIRED) {
            AjustesUtils.pendingRestartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
        } else if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Resultado OK recebido. Atualização automática deve ocorrer.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pagina_graficos)

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

        repository = getInstance(this)
        contasViewModel = ViewModelProvider(this).get(ContasViewModel::class.java)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val toolbarColor = ContextCompat.getColor(this, R.color.primary)
        supportActionBar?.apply {
            setBackgroundDrawable(ColorDrawable(toolbarColor))
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.titulo_acompanhamento_metas)
        }

        val extras = intent.extras
        var initialPosition: Int = START_PAGE

        if (extras != null) {
            initialPosition = extras.getInt(MinhasContas.KEY_PAGINA, START_PAGE)
        }

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

        findViewById<ImageButton>(R.id.ibfab).setOnClickListener { v: View? ->
            val intent = Intent("com.msk.minhascontas.NOVACONTA")
            val current = contasViewModel.currentDateState.value
            if (current != null) {
                intent.putExtra(MinhasContas.KEY_PAGINA, current.nrPagina)
                intent.putExtra(MinhasContas.KEY_MES, current.mes)
                intent.putExtra(MinhasContas.KEY_ANO, current.ano)
            }
            mPaginaListaLauncher.launch(intent)
        }

        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                contasViewModel.setViewPagerPosition(position)
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
                            AlertDialog.Builder(this@PaginadorMetas, R.style.TemaDialogo)
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
                            AlertDialog.Builder(this@PaginadorMetas, R.style.TemaDialogo)
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
                                            Toast.makeText(this@PaginadorMetas, "Erro ao abrir Gemini", Toast.LENGTH_SHORT)
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
                                        val clip = ClipData.newPlainText("AI Prompt", error.fullPrompt)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(this@PaginadorMetas, R.string.ai_prompt_copied, Toast.LENGTH_SHORT)
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
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MinhasContas.KEY_PAGINA, mViewPager.currentItem)
    }

    private fun prepararResultadoParaRetorno() {
        val returnIntent = Intent()
        returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.currentItem)
        setResult(RESULT_OK, returnIntent)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        prepararResultadoParaRetorno()
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.barra_botoes_lista, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            prepararResultadoParaRetorno()
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
        }
        return super.onOptionsItemSelected(item)
    }

    inner class Paginas(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun createFragment(position: Int): Fragment {
            val isMonthly = contasViewModel.viewState.value?.isMonthlySummary ?: true
            val dateState = calculateDateState(position, isMonthly)
            return if (isMonthly) {
                newInstance(dateState.mes, dateState.ano)
            } else {
                // Se for diário, passamos o dia para o fragmento.
                // Precisamos atualizar o newInstance do MetasFragment para suportar dia.
                MetasFragment.newInstance(dateState.mes, dateState.ano, dateState.dia)
            }
        }

        override fun getItemCount(): Int {
            return START_PAGE * 2
        }

        fun getPageTitle(position: Int): CharSequence {
            val mesesResumidos: Array<String?> = contasViewModel.stringMonths
            val isMonthly = contasViewModel.viewState.value?.isMonthlySummary ?: true
            val dateState = calculateDateState(position, isMonthly)
            val mesIndex = dateState.mes - 1
            val anoString = dateState.ano.toString()
            val anoAbreviado = if (anoString.length >= 4) anoString.substring(2) else anoString
            
            return if (isMonthly) {
                "  " + mesesResumidos[mesIndex] + "/" + anoAbreviado + "  "
            } else {
                "  " + dateState.dia + "/" + (mesIndex + 1) + "/" + anoAbreviado + "  "
            }
        }
    }

    companion object {
        private const val TAG = "PaginadorMetas"
        private val START_PAGE = MinhasContas.START_PAGE
    }
}
