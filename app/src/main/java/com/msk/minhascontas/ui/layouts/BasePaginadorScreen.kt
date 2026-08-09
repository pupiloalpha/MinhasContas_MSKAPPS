package com.msk.minhascontas.ui.layouts

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.viewmodel.ContasViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar

@SuppressLint("LocalContextGetResourceValueCall", "LocalContextConfigurationRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePaginadorScreen(
    title: String,
    subtitle: String? = null,
    topBarColor: Color = colorResource(R.color.primary),
    initialPage: Int,
    contasViewModel: ContasViewModel,
    repository: ContasRepository?,
    selectedIds: Set<Long> = emptySet(),
    onSelectionClear: () -> Unit = {},
    onEditarConta: ((List<Long>) -> Unit)? = null,
    onExcluirContas: ((Set<Long>) -> Unit)? = null,
    onListaAtualizada: (() -> Unit)? = null,
    onBackClick: () -> Unit,
    onFilterClick: (() -> Unit)? = null,
    onSearchClick: () -> Unit,
    onAjustesClick: () -> Unit,
    onSobreClick: () -> Unit,
    onFabClick: () -> Unit,
    pageContent: @Composable (pagePosition: Int, dateState: ContasViewModel.DateState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalPages by contasViewModel.totalPages.collectAsState()
    val isAiLoading by contasViewModel.isAiLoading.collectAsState()
    val aiAnalysisResult by contasViewModel.aiAnalysisResult.collectAsState()
    val viewState by contasViewModel.viewState.collectAsState()
    val isMonthly = viewState?.isMonthlySummary ?: true

    var selectedContas by remember { mutableStateOf<List<Conta>>(emptyList()) }
    val dinheiro = remember { NumberFormat.getCurrencyInstance(context.resources.configuration.locales[0]) }

    LaunchedEffect(selectedIds) {
        if (selectedIds.isNotEmpty() && repository != null) {
            val list = mutableListOf<Conta>()
            selectedIds.forEach { id ->
                repository.getConta(id)?.let { list.add(it) }
            }
            selectedContas = list
        } else {
            selectedContas = emptyList()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            contasViewModel.setViewPagerPosition(page)
        }
    }

    val currentViewModelPosition by contasViewModel.viewPagerPosition.collectAsState()
    LaunchedEffect(currentViewModelPosition) {
        if (currentViewModelPosition in 0 until totalPages && pagerState.currentPage != currentViewModelPosition) {
            pagerState.animateScrollToPage(currentViewModelPosition)
        }
    }

    if (isAiLoading) AiLoadingDialog()

    aiAnalysisResult?.let { result ->
        when (result) {
            is AIResult.Success -> AiResultDialog(result = result, onDismiss = { contasViewModel.clearAiResult() })
            is AIResult.Error -> {
                AlertDialog(
                    onDismissRequest = { contasViewModel.clearAiResult() },
                    title = { Text(stringResource(R.string.gemini_title)) },
                    text = { Text(stringResource(R.string.ai_error_fallback_msg)) },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/")))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao abrir browser", Toast.LENGTH_SHORT).show()
                            }
                            contasViewModel.clearAiResult()
                        }) { Text(stringResource(R.string.ai_send_gemini)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Prompt", result.fullPrompt)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.ai_prompt_copied, Toast.LENGTH_SHORT).show()
                            contasViewModel.clearAiResult()
                        }) { Text(stringResource(R.string.ai_copy_prompt)) }
                    }
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AnimatedContent(
                targetState = selectedIds.isNotEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TopAppBarTransition"
            ) { isSelectionMode ->
                if (isSelectionMode) {
                    SelectionTopAppBar(
                        selectedContas = selectedContas,
                        onSelectionClear = onSelectionClear,
                        onTogglePagamento = {
                            scope.launch {
                                selectedContas.forEach { conta ->
                                    val novoStatus = if ("paguei" == conta.pagamento) "pendente" else "paguei"
                                    repository?.atualizarPagamento(conta.idConta, novoStatus)
                                }
                                onSelectionClear()
                                onListaAtualizada?.invoke()
                            }
                        },
                        onEditar = { ids: List<Long> -> onEditarConta?.invoke(ids) },
                        onExcluir = {
                            if (onExcluirContas != null) {
                                onExcluirContas(selectedIds)
                            } else {
                                scope.launch {
                                    selectedIds.forEach { repository?.excluirConta(it) }
                                    onSelectionClear()
                                    onListaAtualizada?.invoke()
                                }
                            }
                        },
                        onLembrete = {
                            val c = selectedContas.firstOrNull() ?: return@SelectionTopAppBar
                            val cal = Calendar.getInstance().apply { set(c.ano, c.mes - 1, c.dia) }
                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                type = "vnd.android.cursor.item/event"
                                putExtra(CalendarContract.Events.TITLE, context.getString(R.string.dica_evento, c.nome))
                                putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.dica_calendario, dinheiro.format(c.valor)))
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis)
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                Toast.makeText(context, R.string.atencao, Toast.LENGTH_SHORT).show()
                            }
                            onSelectionClear()
                        },
                        onCoachClick = { metaId: String ->
                            // Redirecionamento Mobile para Coach
                            val intent = Intent(context, com.msk.minhascontas.features.planos.PlanoFinanceiroActivity::class.java).apply {
                                putExtra("metaId", metaId)
                            }
                            context.startActivity(intent)
                        }
                    )
                } else {
                    StandardTopAppBar(
                        title = title,
                        subtitle = subtitle,
                        onBackClick = onBackClick,
                        onFilterClick = onFilterClick,
                        onSearchClick = onSearchClick,
                        onAiAnalysisClick = {
                            val state = contasViewModel.currentDateState.value
                            if (state != null) {
                                scope.launch {
                                    val contas = repository?.getContasDoMes(state.mes, state.ano, -1, null)
                                    if (contas != null) contasViewModel.runAiAnalysis(contas)
                                }
                            }
                        },
                        onAjustesClick = onAjustesClick,
                        onSobreClick = onSobreClick,
                        containerColor = topBarColor
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = colorResource(R.color.fab_color),
                contentColor = colorResource(R.color.on_fab_color)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthYearTabBar(
                selectedPosition = pagerState.currentPage,
                contasViewModel = contasViewModel,
                pageCount = totalPages,
                onPositionSelected = { pos ->
                    scope.launch { pagerState.animateScrollToPage(pos) }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val dateState = remember(page, isMonthly) {
                    ContasViewModel.calculateDateState(page, isMonthly)
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    pageContent(page, dateState)
                }
            }
        }
    }
}