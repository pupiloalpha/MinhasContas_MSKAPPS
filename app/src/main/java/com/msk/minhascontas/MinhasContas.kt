package com.msk.minhascontas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.features.graficos.PaginadorGraficos
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.features.listas.PaginadorListas
import com.msk.minhascontas.features.planos.PlanoFinanceiroActivity
import com.msk.minhascontas.features.planos.PaginadorMetas
import com.msk.minhascontas.ui.AiLoadingDialog
import com.msk.minhascontas.ui.AiResultDialog
import com.msk.minhascontas.ui.AppLockDialog
import com.msk.minhascontas.ui.NotificationsDialog
import com.msk.minhascontas.ui.RestartAppDialog
import com.msk.minhascontas.ui.layouts.MobileLayout
import com.msk.minhascontas.ui.layouts.TabletLayout
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.utils.BackupUtils
import com.msk.minhascontas.utils.DetailDestination
import com.msk.minhascontas.viewmodel.ContasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.jvm.java
import android.app.backup.BackupManager as AndroidBackupManager
import androidx.appcompat.app.AlertDialog as AppCompatAlertDialog

/**
 * Activity principal da aplicação "Minhas Contas" migrada para Jetpack Compose.
 */
class MinhasContas : AppCompatActivity() {

    private var contasRepository: ContasRepository? = null
    private var contasViewModel: ContasViewModel? = null
    var onResumoCardClickAction: ((Int, Int) -> Unit)? = null

    private var autobkup = true
    private var bloqueioApp = false
    private var atualizaPagamento = false

    private val restartReasonState = mutableStateOf<String?>(null)
    private val editingAccountId = mutableStateOf<Long?>(null)
    private var isTablet = false

    private val someActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        val viewModel = contasViewModel ?: return@registerForActivityResult
        val currentPage = viewModel.viewPagerPosition.value
        var positionToSync = currentPage

        when (result.resultCode) {
            Ajustes.RESULT_RESTART_REQUIRED -> {
                val restartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
                AjustesUtils.pendingRestartReason = restartReason
                Log.d(TAG, "ActivityResult: Reinício obrigatório detectado. Razão: $restartReason")
            }
            RESULT_OK -> {
                val data = result.data
                if (data != null && data.hasExtra(RETURN_KEY_PAGINA)) {
                    positionToSync = data.getIntExtra(RETURN_KEY_PAGINA, currentPage)
                }
                syncViewPagerPositionAndRefresh(positionToSync)
            }
            else -> {
                Log.w(TAG, "Resultado da Activity filha não foi OK.")
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            savedInstanceState.remove("android:support:fragments")
            savedInstanceState.remove("android:fragments")
        }
        
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        contasRepository = ContasRepository.getInstance(this)
        contasViewModel = ViewModelProvider(this)[ContasViewModel::class.java]

        loadUserPreferences()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MinhasContasTheme {
                MainScreen(windowSizeClass)
            }
        }

        lifecycleScope.launch {
            checkDatabaseReset()
            checkAndRequestPermissions()
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    fun MainScreen(windowSizeClass: WindowSizeClass) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
                        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
        
        LaunchedEffect(isCompact) {
            isTablet = !isCompact
        }

        val viewModel = contasViewModel ?: return
        val viewPagerPosition by viewModel.viewPagerPosition.collectAsState()
        
        val aiAnalysisResult by viewModel.aiAnalysisResult.collectAsState()
        val isAiLoading by viewModel.isAiLoading.collectAsState()

        var showAppLock by remember { mutableStateOf(bloqueioApp) }
        var showNotifications by remember { mutableStateOf(false) }
        val restartReason by restartReasonState

        if (isAiLoading) {
            AiLoadingDialog()
        }

        aiAnalysisResult?.let { result ->
            AiResultDialog(
                result = result,
                onDismiss = { viewModel.clearAiResult() }
            )
        }

        // 1. Mantenha a instância do banco de dados (que é leve) no remember
        val db = remember { DBContas.getInstance(context) }

        // 2. Inicie o estado das notificações como "falso" temporariamente
        var hasUnreadNotifications by remember { mutableStateOf(false) }

        // 3. Busque o valor real de forma assíncrona fora da Main Thread
        LaunchedEffect(db, showNotifications) {
            val temNotificacoes = withContext(Dispatchers.IO) {
                db.temNotificacoesNaoLidas()
            }
            hasUnreadNotifications = temNotificacoes
        }
        
        val totalPages by viewModel.totalPages.collectAsState()
        val listPagerState = rememberPagerState(initialPage = START_PAGE) { totalPages }
        val detailPagerState = rememberPagerState(initialPage = START_PAGE) { totalPages }

        var isFirstSync by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            if (isFirstSync) {
                viewModel.setViewPagerPosition(START_PAGE)
                listPagerState.scrollToPage(START_PAGE)
                detailPagerState.scrollToPage(START_PAGE)
                isFirstSync = false
            }
        }

        LaunchedEffect(listPagerState.settledPage) { viewModel.expandPagesIfNeeded(listPagerState.settledPage) }
        LaunchedEffect(detailPagerState.settledPage) { viewModel.expandPagesIfNeeded(detailPagerState.settledPage) }

        val navigator = rememberListDetailPaneScaffoldNavigator<DetailDestination>()

        // Sincronização Pagers -> ViewModel (Apenas para Tablet/Desktop onde os Pagers Compose são usados)
        if (!isCompact) {
            LaunchedEffect(listPagerState) {
                snapshotFlow { listPagerState.settledPage }.collect { settled ->
                    val isListActive = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.List || navigator.currentDestination?.pane == null
                    if (isListActive && settled != viewPagerPosition) {
                        viewModel.setViewPagerPosition(settled)
                    }
                }
            }

            LaunchedEffect(detailPagerState) {
                snapshotFlow { detailPagerState.settledPage }.collect { settled ->
                    val isDetailActive = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
                    if (isDetailActive && settled != viewPagerPosition) {
                        viewModel.setViewPagerPosition(settled)
                    }
                }
            }
        }

        // Sincronização Pagers <- ViewModel
        LaunchedEffect(viewPagerPosition) {
            viewModel.expandPagesIfNeeded(viewPagerPosition)
            
            // No celular, não precisamos sincronizar listPagerState/detailPagerState 
            // pois usamos o PaginadorResumos (ViewPager2) que já se sincroniza no update{} do AndroidView
            if (!isCompact) {
                if (viewPagerPosition != listPagerState.currentPage) {
                    listPagerState.animateScrollToPage(viewPagerPosition.coerceIn(0, totalPages - 1))
                }
                if (viewPagerPosition != detailPagerState.currentPage) {
                    detailPagerState.animateScrollToPage(viewPagerPosition.coerceIn(0, totalPages - 1))
                }
            }
        }

        LaunchedEffect(isCompact, navigator, viewPagerPosition) {
            onResumoCardClickAction = { tipo, filtro ->
                if (isCompact) {
                    val intent = Intent(this@MinhasContas, PaginadorListas::class.java).apply {
                        putExtra("tipo", tipo)
                        putExtra("filtro", filtro)
                        putExtra(KEY_PAGINA, viewPagerPosition)
                    }
                    someActivityResultLauncher.launch(intent)
                } else {
                    coroutineScope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, DetailDestination.Contas(tipo, filtro))
                    }
                }
            }
        }

        if (showAppLock) {
            AppLockDialog(onDismiss = { showAppLock = false }, onRecoverPassword = { showPasswordRecoveryDialog() })
        }
        if (restartReason != null) {
            RestartAppDialog(reason = restartReason, onDismiss = { restartReasonState.value = null })
        }
        if (showNotifications) {
            NotificationsDialog(
                onDismiss = { showNotifications = false },
                onRenovarSerie = { idConta ->
                    AppCompatAlertDialog.Builder(context, R.style.TemaDialogo).apply {
                        setTitle(R.string.msg_fim_serie_titulo)
                        setMessage(R.string.confirmar_renovar_msg)
                        setPositiveButton(R.string.sim) { _, _ ->
                            contasRepository?.renovarSerie(idConta)
                            Toast.makeText(context, R.string.ajustes_salvos, Toast.LENGTH_SHORT).show()
                        }
                        setNegativeButton(R.string.nao, null)
                        show()
                    }
                }
            )
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            if (isCompact) {
                MobileLayout(
                    contasViewModel = viewModel,
                    contasRepository = contasRepository,
                    fragmentManager = supportFragmentManager,
                    totalPages = totalPages,
                    viewPagerPosition = viewPagerPosition,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onShowNotifications = { showNotifications = true },
                    onNavigateToDashboard = { someActivityResultLauncher.launch(Intent(this@MinhasContas, PaginadorGraficos::class.java).apply { putExtra(KEY_PAGINA, viewPagerPosition) }) },
                    onNavigateToContas = { someActivityResultLauncher.launch(Intent(this@MinhasContas, PaginadorListas::class.java).apply { putExtra("tipo", -1); putExtra(KEY_PAGINA, viewPagerPosition) }) },
                    onNavigateToMetas = { someActivityResultLauncher.launch(Intent(this@MinhasContas, PaginadorMetas::class.java).apply { putExtra(KEY_PAGINA, viewPagerPosition) }) },
                    onNavigateToPlanejamento = { startActivity(Intent(this@MinhasContas,
                        PlanoFinanceiroActivity::class.java)) },
                    onNavigateToBusca = { openSearch() },
                    onShare = { shareContas() },
                    onNavigateToAjustes = { someActivityResultLauncher.launch(Intent(this@MinhasContas, Ajustes::class.java)) },
                    onNavigateToSobre = { startActivity(Intent("com.msk.minhascontas.SOBRE")) },
                    onNavigateToNovaConta = { openNovaConta() }
                )
            } else {
                TabletLayout(
                    windowSizeClass = windowSizeClass,
                    contasViewModel = viewModel,
                    contasRepository = contasRepository,
                    fragmentManager = supportFragmentManager,
                    navigator = navigator,
                    directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
                    listPagerState = listPagerState,
                    detailPagerState = detailPagerState,
                    totalPages = totalPages,
                    viewPagerPosition = viewPagerPosition,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onShowNotifications = { showNotifications = true },
                    onShare = { shareContas() },
                    onSearch = { openSearch() },
                    onShowFilterDialog = { dest, onFilterSelected -> showFilterDialog(dest, onFilterSelected) },
                    onRestartReasonChange = { restartReasonState.value = it },
                    getAppVersion = { getAppVersion() },
                    isNotificationServiceEnabled = { isNotificationServiceEnabled() },
                    executeManualBackup = { executeManualBackup() },
                    executeManualRestore = { executeManualRestore() },
                    editingAccountId = editingAccountId.value,
                    onEditContaRequest = { id -> editingAccountId.value = id }
                )
            }
        }
    }

    fun onEditarConta(id: Long) {
        if (isTablet) {
            editingAccountId.value = id
        } else {
            val intent = Intent("com.msk.minhascontas.EDITACONTA")
            intent.putExtra("id", id)
            someActivityResultLauncher.launch(intent)
        }
    }

    private fun showFilterDialog(dest: DetailDestination.Contas, onFilterSelected: (Int) -> Unit) {
        val labels = when (dest.tipo) {
            ContasContract.TIPO_DESPESA -> resources.getStringArray(R.array.FiltroDespesa)
            ContasContract.TIPO_RECEITA -> resources.getStringArray(R.array.FiltroReceita)
            ContasContract.TIPO_APLICACAO -> resources.getStringArray(R.array.FiltroAplicacao)
            else -> return
        }
        AppCompatAlertDialog.Builder(this, R.style.TemaDialogo)
            .setTitle(R.string.titulo_filtro)
            .setItems(labels) { _, id ->
                val filtro = when (dest.tipo) {
                    ContasContract.TIPO_DESPESA -> if (id < 6) id else -1
                    ContasContract.TIPO_RECEITA -> if (id < 5) id else -1
                    ContasContract.TIPO_APLICACAO -> if (id < 3) id else -1
                    else -> -1
                }
                onFilterSelected(filtro)
            }.show()
    }

    private fun checkDatabaseReset() {
        val prefs = getSharedPreferences("MinhasContasPrefs", MODE_PRIVATE)
        if (prefs.getBoolean(DBContas.PREF_DB_RESET_FLAG, false)) {
            AppCompatAlertDialog.Builder(this, R.style.TemaDialogo)
                .setTitle(R.string.atencao)
                .setMessage(R.string.msg_db_reset)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            prefs.edit { putBoolean(DBContas.PREF_DB_RESET_FLAG, false) }
        }
    }

    private fun loadUserPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        autobkup = prefs.getBoolean(getString(R.string.pref_key_auto_bkup), true)
        bloqueioApp = prefs.getBoolean(getString(R.string.pref_key_acesso), false)
        atualizaPagamento = prefs.getBoolean(getString(R.string.pref_key_pagamento), false)
    }

    private fun openSearch() {
        val intent = Intent("com.msk.minhascontas.BUSCACONTA")
        val viewModel = contasViewModel ?: return
        val current = viewModel.currentDateState.value
        intent.putExtra(KEY_PAGINA, current?.nrPagina ?: START_PAGE)
        someActivityResultLauncher.launch(intent)
    }

    private fun openNovaConta() {
        val intent = Intent("com.msk.minhascontas.NOVACONTA")
        val viewModel = contasViewModel ?: return
        viewModel.currentDateState.value?.let {
            intent.putExtra(KEY_PAGINA, it.nrPagina)
            intent.putExtra(KEY_MES, it.mes)
            intent.putExtra(KEY_ANO, it.ano)
        } ?: intent.putExtra(KEY_PAGINA, START_PAGE)
        someActivityResultLauncher.launch(intent)
    }

    private fun shareContas() {
        val viewModel = contasViewModel ?: return
        val repository = contasRepository ?: return
        val current = viewModel.currentDateState.value ?: return
        val mesesArray = viewModel.stringMonths
        val texto = StringBuilder("${getString(R.string.app_name)} ${mesesArray[current.mes - 1]}/${current.ano}:")
        try {
            val filter = DBContas.ContaFilter().setMes(current.mes).setAno(current.ano)
            val lista = repository.getContas(filter, null)
            if (lista.isNotEmpty()) {
                for (conta in lista) {
                    texto.append("\n${conta.nome} - ${conta.valor}")
                }
            } else {
                texto.append(" ${getString(R.string.dica_nenhuma_conta)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao compartilhar contas", e)
            texto.append(" ${getString(R.string.erro_geral_bd)}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, texto.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, texto.toString()))
    }

    private fun showPasswordRecoveryDialog() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val perguntaId = prefs.getString("pergunta_seguranca_id", null)
        val respostaSalva = prefs.getString("resposta_secreta", null)
        if (perguntaId == null || respostaSalva.isNullOrEmpty()) {
            Toast.makeText(this, R.string.erro_recuperacao_nao_configurada, Toast.LENGTH_LONG).show()
            return
        }
        val pergunta = resources.getStringArray(R.array.perguntas_seguranca)[perguntaId.toInt()]
        val input = AppCompatEditText(this).apply { setHint(R.string.dica_resposta_secreta) }
        AppCompatAlertDialog.Builder(this, R.style.TemaDialogo)
            .setTitle(R.string.recuperacao_senha).setMessage(pergunta).setView(input)
            .setPositiveButton(R.string.confirmar) { _, _ ->
                val resposta = input.text?.toString()?.trim() ?: ""
                if (resposta.equals(respostaSalva, ignoreCase = true)) showNewPasswordDialog()
                else Toast.makeText(this, R.string.senha_errada, Toast.LENGTH_SHORT).show()
            }.setNegativeButton(R.string.cancelar, null).show()
    }

    private fun showNewPasswordDialog() {
        val input = AppCompatEditText(this).apply {
            setHint(R.string.nova_senha)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AppCompatAlertDialog.Builder(this, R.style.TemaDialogo)
            .setTitle(R.string.redefinir_senha).setView(input)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val novaSenha = input.text?.toString() ?: ""
                if (novaSenha.isNotEmpty()) {
                    PreferenceManager.getDefaultSharedPreferences(this).edit { putString("senha", novaSenha) }
                    Toast.makeText(this, R.string.senha_redefinida_sucesso, Toast.LENGTH_LONG).show()
                    recreate()
                }
            }.setNegativeButton(R.string.cancelar, null).show()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        } else performDatabaseAdjustments()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) performDatabaseAdjustments()
    }

    private fun performDatabaseAdjustments() {
        if (atualizaPagamento) {
            lifecycleScope.launch(Dispatchers.IO) {
                val c = Calendar.getInstance()
                contasRepository?.atualizarPagamentoContas(c.get(Calendar.DAY_OF_MONTH) + 1, c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
            }
        }
    }

    fun syncViewPagerPositionAndRefresh(returnedPosition: Int) {
        contasViewModel?.setViewPagerPosition(returnedPosition)
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {
            syncViewPagerPositionAndRefresh(-1)
        }
    }

    override fun onDestroy() {
        if (autobkup) AndroidBackupManager(this).dataChanged()
        super.onDestroy()
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            else packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "?"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter versão do app", e)
            "?"
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun executeManualBackup() {
        val uriString = getSharedPreferences("backup", MODE_PRIVATE).getString("backup_uri", "")
        if (uriString.isNullOrEmpty()) { Toast.makeText(this, getString(R.string.error_backup_location_not_set), Toast.LENGTH_LONG).show(); return }
        val backupUri = uriString.toUri(); BackupUtils.copiaBD(this, backupUri); BackupUtils.copiaSharedPreferences(this, backupUri); Toast.makeText(this, getString(R.string.backup_complete), Toast.LENGTH_SHORT).show()
    }

    private fun executeManualRestore() {
        val uriString = getSharedPreferences("backup", MODE_PRIVATE).getString("backup_uri", "")
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_restore_location_not_set), Toast.LENGTH_LONG).show()
            return
        }
        val restoreUri = uriString.toUri()
        
        // Fecha conexões ativas antes da restauração de arquivos
        DBContas.getInstance(this).close()
        com.msk.minhascontas.db.AppDatabase.getDatabase(this).close()
        com.msk.minhascontas.db.AppDatabase.closeDatabase()
        
        BackupUtils.restauraBD(this, restoreUri)
        BackupUtils.restauraSharedPreferences(this, restoreUri)
        
        restartReasonState.value = Ajustes.REASON_DB_RESTORE
    }

    companion object {
        private const val TAG = "MinhasContas"
        const val START_PAGE = 1000
        const val KEY_PAGINA = "nrPagina"
        const val KEY_MES = "mes"
        const val KEY_ANO = "ano"
        const val RETURN_KEY_PAGINA = "nr_pagina"
    }
}
