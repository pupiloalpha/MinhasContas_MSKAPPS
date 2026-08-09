package com.msk.minhascontas.ui.layouts

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.utils.DetailDestination
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.ContaFilter
import com.msk.minhascontas.features.graficos.GraficosScreen
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.features.planos.PlanejamentoScreen
import com.msk.minhascontas.utils.AlertaCalendario
import com.msk.minhascontas.features.info.SobreScreen
import com.msk.minhascontas.ui.MetasScreen
import com.msk.minhascontas.features.planos.PlanejamentoViewModel
import com.msk.minhascontas.features.planos.PlanoFinanceiroScreen
import com.msk.minhascontas.ui.EditarContaScreen
import com.msk.minhascontas.ui.PersonalizarCategoriasScreen
import com.msk.minhascontas.ui.PesquisaScreen
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.viewmodel.ContasViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun TabletLayout(
    windowSizeClass: WindowSizeClass,
    contasViewModel: ContasViewModel,
    contasRepository: ContasRepository?,
    fragmentManager: FragmentManager,
    navigator: ThreePaneScaffoldNavigator<DetailDestination>,
    directive: PaneScaffoldDirective,
    listPagerState: PagerState,
    detailPagerState: PagerState,
    totalPages: Int,
    viewPagerPosition: Int,
    hasUnreadNotifications: Boolean,
    onShowNotifications: () -> Unit,
    onShare: () -> Unit,
    onShowFilterDialog: (DetailDestination.Contas, (Int) -> Unit) -> Unit,
    onRestartReasonChange: (String?) -> Unit,
    getAppVersion: () -> String,
    isNotificationServiceEnabled: () -> Boolean,
    executeManualBackup: () -> Unit,
    executeManualRestore: () -> Unit,
    editingAccountIds: List<Long>?,
    onEditContaRequest: (List<Long>?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    var overlayDestination by remember { mutableStateOf<DetailDestination?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedContas by remember { mutableStateOf<List<com.msk.minhascontas.db.Conta>>(emptyList()) }

    LaunchedEffect(selectedIds) {
        if (selectedIds.isNotEmpty() && contasRepository != null) {
            val list = mutableListOf<com.msk.minhascontas.db.Conta>()
            selectedIds.forEach { id ->
                contasRepository.getConta(id)?.let { list.add(it) }
            }
            selectedContas = list
        } else {
            selectedContas = emptyList()
        }
    }

    // Sincroniza o pedido de edição externo com o overlay local
    LaunchedEffect(editingAccountIds) {
        if (editingAccountIds != null) {
            overlayDestination = DetailDestination.EditarConta(editingAccountIds)
        }
    }

    // Limpa o estado global quando o overlay fecha
    LaunchedEffect(overlayDestination) {
        if (overlayDestination !is DetailDestination.EditarConta) {
            onEditContaRequest(null)
        }
    }

    val dateState by contasViewModel.currentDateState.collectAsState()
    val viewState by contasViewModel.viewState.collectAsState()
    val scope = rememberCoroutineScope()
    val isMonthly = viewState?.isMonthlySummary ?: true

    // Identificação reativa de recursos
    val currentDetail = navigator.currentDestination?.contentKey
    val configuration = LocalConfiguration.current

    // Limpa seleção ao trocar de tela
    LaunchedEffect(currentDetail) {
        selectedIds = emptySet()
    }

    val appBarTitle = remember(currentDetail, configuration) {
        when (currentDetail) {
            is DetailDestination.Contas -> {
                if (currentDetail.categoria >= 0) {
                    com.msk.minhascontas.utils.LabelUtils.getCategoriaLabel(context, currentDetail.categoria)
                } else if (currentDetail.filtro >= 0) {
                    val labels = when (currentDetail.tipo) {
                        ContasContract.TIPO_RECEITA -> context.resources.getStringArray(R.array.FiltroReceita)
                        ContasContract.TIPO_DESPESA -> context.resources.getStringArray(R.array.FiltroDespesa)
                        ContasContract.TIPO_APLICACAO -> context.resources.getStringArray(R.array.FiltroAplicacao)
                        else -> emptyArray()
                    }

                    // Aplica rótulos personalizados para classes, mas mantém labels de status (Pago/Falta)
                    val numClasses = when (currentDetail.tipo) {
                        ContasContract.TIPO_DESPESA -> context.resources.getStringArray(R.array.TipoDespesa).size
                        ContasContract.TIPO_RECEITA -> context.resources.getStringArray(R.array.TipoReceita).size
                        ContasContract.TIPO_APLICACAO -> context.resources.getStringArray(R.array.TipoAplicacao).size
                        else -> 0
                    }

                    if (currentDetail.filtro < numClasses) {
                        com.msk.minhascontas.utils.LabelUtils.getClasseLabel(context, currentDetail.tipo, currentDetail.filtro)
                    } else if (currentDetail.filtro < labels.size) {
                        labels[currentDetail.filtro]
                    } else {
                        when (currentDetail.tipo) {
                            ContasContract.TIPO_RECEITA -> context.getString(R.string.linha_receita)
                            ContasContract.TIPO_DESPESA -> context.getString(R.string.linha_despesa)
                            ContasContract.TIPO_APLICACAO -> context.getString(R.string.linha_aplicacoes)
                            else -> context.getString(R.string.nav_contas)
                        }
                    }
                } else {
                    when (currentDetail.tipo) {
                        ContasContract.TIPO_RECEITA -> context.getString(R.string.linha_receita)
                        ContasContract.TIPO_DESPESA -> context.getString(R.string.linha_despesa)
                        ContasContract.TIPO_APLICACAO -> context.getString(R.string.linha_aplicacoes)
                        else -> context.getString(R.string.nav_contas)
                    }
                }
            }
            is DetailDestination.Dashboard -> context.getString(R.string.nav_dashboard)
            is DetailDestination.Metas -> context.getString(R.string.nav_metas)
            else -> context.getString(R.string.app_name)
        }
    }

    val targetColor = when (currentDetail) {
        is DetailDestination.Contas -> {
            when (currentDetail.tipo) {
                ContasContract.TIPO_RECEITA -> colorResource(R.color.azul)
                ContasContract.TIPO_DESPESA -> colorResource(R.color.vermelho)
                ContasContract.TIPO_APLICACAO -> colorResource(R.color.verde)
                else -> colorResource(R.color.primary)
            }
        }
        else -> colorResource(R.color.primary)
    }

    val appBarColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "appBarColor"
    )

    val appBarTotal by produceState<Double?>(initialValue = null, currentDetail, dateState, isMonthly) {
        value = if (currentDetail is DetailDestination.Contas) {
            val mes = dateState?.mes ?: 1
            val ano = dateState?.ano ?: 2024
            val diaFim = if (isMonthly) -1 else (dateState?.dia ?: -1)

            withContext(Dispatchers.IO) {
                val tipo = currentDetail.tipo
                val filtro = currentDetail.filtro
                val categoria = currentDetail.categoria

                if (categoria >= 0) {
                    contasRepository?.somaValoresPorFiltro(ano, mes, tipo, -1, categoria, null, if (isMonthly) -1 else (dateState?.dia ?: -1))
                } else if (tipo == -2) {
                    val totalDespesasCat8 = contasRepository?.somaValoresPorFiltro(ano, mes, ContasContract.TIPO_DESPESA, -1, 8, null) ?: 0.0
                    val totalAplicacoes = contasRepository?.somaValoresPorFiltro(ano, mes, ContasContract.TIPO_APLICACAO, -1, -1, null) ?: 0.0
                    totalDespesasCat8 + totalAplicacoes
                } else if (tipo != -1) {
                    val filter = ContaFilter().setMes(mes).setAno(ano).setDiaFim(diaFim)
                    filter.setTipo(tipo)
                    if (filtro >= 0) {
                        when (tipo) {
                            ContasContract.TIPO_DESPESA -> {
                                when (filtro) {
                                    4 -> filter.setPagamento(ContasContract.STATUS_PENDENTE)
                                    5 -> filter.setPagamento(ContasContract.STATUS_PAGO_RECEBIDO)
                                    else -> filter.setClasse(filtro)
                                }
                            }
                            ContasContract.TIPO_RECEITA -> {
                                when (filtro) {
                                    3 -> filter.setPagamento(ContasContract.STATUS_PENDENTE)
                                    4 -> filter.setPagamento(ContasContract.STATUS_PAGO_RECEBIDO)
                                    else -> filter.setClasse(filtro)
                                }
                            }
                            else -> filter.setClasse(filtro)
                        }
                    }
                    contasRepository?.calcularTotalMensal(mes, ano, tipo, filter)
                } else {
                    contasRepository?.calcularTotalMensal(mes, ano, tipo, ContaFilter().setMes(mes).setAno(ano).setDiaFim(diaFim))
                }
            }
        } else null
    }

    val dinheiro = remember(configuration) { NumberFormat.getCurrencyInstance(configuration.locales[0]) }
    val appBarSubtitle = appBarTotal?.let { dinheiro.format(it) }

    if (overlayDestination != null) {
        BackHandler { overlayDestination = null }
    }

    BackHandler(enabled = navigator.canNavigateBack()) {
        coroutineScope.launch { navigator.navigateBack() }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = colorResource(R.color.surface),
            contentColor = colorResource(R.color.tab_text_unselected)
        ) {
            val railColors = NavigationRailItemDefaults.colors(
                selectedIconColor = colorResource(R.color.tab_text_selected),
                selectedTextColor = colorResource(R.color.tab_text_selected),
                unselectedIconColor = colorResource(R.color.tab_text_unselected),
                unselectedTextColor = colorResource(R.color.tab_text_unselected),
                indicatorColor = colorResource(R.color.tab_indicator).copy(alpha = 0.2f)
            )

            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = currentDetail is DetailDestination.Dashboard,
                onClick = { coroutineScope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, DetailDestination.Dashboard) } },
                icon = { Icon(painterResource(R.drawable.ic_dashboard), null) },
                label = { Text(stringResource(R.string.nav_dashboard)) },
                colors = railColors
            )
            NavigationRailItem(
                selected = currentDetail is DetailDestination.Contas && currentDetail.tipo == -1,
                onClick = { coroutineScope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, DetailDestination.Contas(tipo = -1)) } },
                icon = { Icon(painterResource(R.drawable.ic_accounts), null) },
                label = { Text(stringResource(R.string.nav_contas)) },
                colors = railColors
            )
            NavigationRailItem(
                selected = currentDetail is DetailDestination.Metas,
                onClick = { coroutineScope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, DetailDestination.Metas) } },
                icon = { Icon(painterResource(R.drawable.ic_metas), null) },
                label = { Text(stringResource(R.string.nav_metas)) },
                colors = railColors
            )
            NavigationRailItem(
                selected = false,
                onClick = { overlayDestination = DetailDestination.Planejamento() },
                icon = { Icon(painterResource(R.drawable.ic_planejamento), null) },
                label = { Text(stringResource(R.string.nav_planejamento)) },
                colors = railColors
            )
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = { overlayDestination = DetailDestination.CriarConta },
                containerColor = colorResource(R.color.fab_color),
                contentColor = colorResource(R.color.on_fab_color),
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        }

        Column(modifier = Modifier.weight(1f).fillMaxHeight().statusBarsPadding()) {
            AnimatedContent(
                targetState = selectedIds.isNotEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TopAppBarTransition"
            ) { isSelectionMode ->
                if (isSelectionMode) {
                    SelectionTopAppBar(
                        selectedContas = selectedContas,
                        onSelectionClear = { selectedIds = emptySet() },
                        onTogglePagamento = {
                            coroutineScope.launch(Dispatchers.IO) {
                                selectedContas.forEach { conta ->
                                    val novoStatus = if ("paguei" == conta.pagamento) "pendente" else "paguei"
                                    contasRepository?.atualizarPagamento(conta.idConta, novoStatus)
                                }
                                withContext(Dispatchers.Main) {
                                    selectedIds = emptySet()
                                    // Força a atualização do produceState da TopAppBar
                                    val currentPos = contasViewModel.viewPagerPosition.value
                                    contasViewModel.setViewPagerPosition(-1)
                                    contasViewModel.setViewPagerPosition(currentPos)
                                }
                            }
                        },
                        onEditar = { ids: List<Long> -> onEditContaRequest(ids) },
                        onExcluir = {
                            coroutineScope.launch(Dispatchers.IO) {
                                selectedIds.forEach { contasRepository?.excluirConta(it) }
                                withContext(Dispatchers.Main) {
                                    selectedIds = emptySet()
                                    // Força a atualização do produceState da TopAppBar
                                    val currentPos = contasViewModel.viewPagerPosition.value
                                    contasViewModel.setViewPagerPosition(-1)
                                    contasViewModel.setViewPagerPosition(currentPos)
                                }
                            }
                        },
                        onCoachClick = { metaId: String ->
                            overlayDestination = DetailDestination.Planejamento(metaId)
                        }
                    )
                } else {
                    StandardTopAppBar(
                        title = appBarTitle,
                        subtitle = appBarSubtitle,
                        onBackClick = if (navigator.canNavigateBack()) { { coroutineScope.launch { navigator.navigateBack() } } } else null,
                        onFilterClick = if (currentDetail is DetailDestination.Contas && currentDetail.tipo != -1) {
                            {
                                onShowFilterDialog(currentDetail) { novoFiltro ->
                                    coroutineScope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, currentDetail.copy(filtro = novoFiltro))
                                    }
                                }
                            }
                        } else null,
                        onSearchClick = { overlayDestination = DetailDestination.BuscarConta },
                        onShareClick = onShare,
                        onAiAnalysisClick = {
                            val state = contasViewModel.currentDateState.value
                            if (state != null) {
                                scope.launch {
                                    val contas = contasRepository?.getContasDoMes(state.mes, state.ano, -1, null)
                                    if (contas != null) contasViewModel.runAiAnalysis(contas)
                                }
                            }
                        },
                        onAjustesClick = { overlayDestination = DetailDestination.Ajustes },
                        onSobreClick = { overlayDestination = DetailDestination.Sobre },
                        hasUnreadNotifications = hasUnreadNotifications,
                        onShowNotifications = onShowNotifications,
                        containerColor = appBarColor
                    )
                }
            }

            MonthYearTabBar(
                selectedPosition = viewPagerPosition,
                contasViewModel = contasViewModel,
                pageCount = totalPages,
                months = contasViewModel.fullStringMonths,
                onPositionSelected = { contasViewModel.setViewPagerPosition(it) }
            )

            ListDetailPaneScaffold(
                directive = directive,
                value = navigator.scaffoldValue,
                listPane = {
                    HorizontalPager(state = listPagerState, key = { it }, modifier = Modifier.fillMaxSize()) { page ->
                        val dateForPage = remember(page, isMonthly) { ContasViewModel.calculateDateState(page, isMonthly) }
                        Surface(modifier = Modifier.fillMaxSize()) {
                            SummaryPane(
                                fragmentManager = fragmentManager,
                                contasViewModel = contasViewModel,
                                mes = dateForPage.mes,
                                ano = dateForPage.ano,
                                dia = dateForPage.dia,
                                position = page
                            )
                        }
                    }
                },
                detailPane = {
                    HorizontalPager(state = detailPagerState, key = { it }, modifier = Modifier.fillMaxSize()) { page ->
                        val dateStateDetail = remember(page, isMonthly) { ContasViewModel.calculateDateState(page, isMonthly) }
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            when (val dest = navigator.currentDestination?.contentKey) {
                                is DetailDestination.Dashboard -> GraficosScreen(mes = dateStateDetail.mes, ano = dateStateDetail.ano, dia = if (isMonthly) null else dateStateDetail.dia)
                                is DetailDestination.Metas -> MetasScreen(
                                    mes = dateStateDetail.mes,
                                    ano = dateStateDetail.ano,
                                    dia = if (isMonthly) -1 else dateStateDetail.dia,
                                    onCategoryClick = { tipo, categoria ->
                                        coroutineScope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, DetailDestination.Contas(tipo, -1, categoria))
                                        }
                                    }
                                )
                                is DetailDestination.Contas -> {
                                    contasRepository?.let { repo ->
                                        key(dest) { // Força a recriação do painel se o destino (incluindo filtro) mudar
                                            ContasDetailPane(
                                                dest = dest,
                                                dateState = dateStateDetail,
                                                isMonthly = isMonthly,
                                                position = page,
                                                repository = repo,
                                                selectedIds = selectedIds,
                                                onSelectionChange = { selectedIds = it },
                                                onEditContaRequest = { ids -> onEditContaRequest(ids) },
                                                onCoachClick = { metaId ->
                                                    overlayDestination = DetailDestination.Planejamento(metaId)
                                                },
                                                onListaAtualizada = {
                                                    // Força a atualização do produceState da TopAppBar
                                                    val currentPos = contasViewModel.viewPagerPosition.value
                                                    contasViewModel.setViewPagerPosition(-1)
                                                    contasViewModel.setViewPagerPosition(currentPos)
                                                }
                                            )
                                        }
                                    }
                                }
                                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.dica_inicio)) }
                            }
                        }
                    }
                }
            )
        }
    }

    // Overlays
    if (overlayDestination != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).clickable { overlayDestination = null },
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(if (isExpanded) 0.4f else 0.6f).clickable(enabled = false) {},
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                when (val dest = overlayDestination) {
                    is DetailDestination.Planejamento -> PlanoFinanceiroScreen(
                        initialMetaId = dest.metaId,
                        onVoltar = { overlayDestination = null },
                        onSearch = { overlayDestination = DetailDestination.BuscarConta },
                        onAjustes = { overlayDestination = DetailDestination.Ajustes },
                        onSobre = { overlayDestination = DetailDestination.Sobre }
                    )
                    is DetailDestination.Ajustes -> AjustesDetailPane(
                        onBack = { overlayDestination = null },
                        onPreferenceChanged = { contasViewModel.setViewPagerPosition(-1) },
                        onNavigateToPersonalizarCategorias = { overlayDestination = DetailDestination.PersonalizarCategorias },
                        onNavigateToPlanejamento = { overlayDestination = DetailDestination.DefinirMetas },
                        onRestartReasonChange = onRestartReasonChange,
                        isNotificationServiceEnabled = isNotificationServiceEnabled,
                        executeManualBackup = executeManualBackup,
                        executeManualRestore = executeManualRestore
                    )
                    is DetailDestination.Sobre -> SobreScreen(versao = getAppVersion(), onBack = { overlayDestination = null })
                    is DetailDestination.CriarConta -> CriarContaDetailPane(
                        dateState = dateState,
                        onBack = { overlayDestination = null },
                        onSuccess = {
                            contasViewModel.setViewPagerPosition(contasViewModel.viewPagerPosition.value)
                            overlayDestination = null
                        }
                    )
                    is DetailDestination.BuscarConta -> PesquisaScreen(
                        onBack = { overlayDestination = null },
                        onEditConta = { id ->
                            overlayDestination = DetailDestination.EditarConta(id)
                        },
                        onLembrete = { conta ->
                            AlertaCalendario.adicionarEventoNoCalendario(
                                context.contentResolver,
                                context.getString(R.string.dica_evento, conta.nome),
                                context.getString(
                                    R.string.dica_calendario,
                                    NumberFormat.getCurrencyInstance().format(conta.valor)
                                ),
                                conta.dia, conta.mes, conta.ano, false, 1, 0
                            )
                        }
                    )
                    is DetailDestination.EditarConta -> {
                        val editarViewModel: com.msk.minhascontas.viewmodel.EditarContaViewModel = viewModel()
                        LaunchedEffect(dest.ids) { editarViewModel.loadContas(dest.ids) }
                        EditarContaScreen(
                            viewModel = editarViewModel,
                            onComplete = { mudou ->
                                overlayDestination = null
                                if (mudou) contasViewModel.setViewPagerPosition(-1)
                            },
                            onCancel = { overlayDestination = null }
                        )
                    }
                    is DetailDestination.PersonalizarCategorias -> PersonalizarCategoriasScreen(
                        onBack = { overlayDestination = DetailDestination.Ajustes },
                        onSaved = {
                            AjustesUtils.pendingDataRefresh = true
                            AjustesUtils.pendingRestartReason = Ajustes.REASON_LABELS_CHANGED
                            onRestartReasonChange(Ajustes.REASON_LABELS_CHANGED)
                            overlayDestination = null
                            Toast.makeText(context, R.string.ajustes_salvos, Toast.LENGTH_SHORT).show()
                        }
                    )
                    is DetailDestination.DefinirMetas -> {
                        val planejaViewModel: PlanejamentoViewModel = viewModel()
                        LaunchedEffect(Unit) { planejaViewModel.loadData(context) }
                        PlanejamentoScreen(viewModel = planejaViewModel, onBack = { overlayDestination = DetailDestination.Ajustes })
                    }
                    else -> {}
                }
            }
        }
    }
}