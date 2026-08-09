package com.msk.minhascontas.ui.layouts

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.minhascontas.app.ui.resumo.ResumoCategoriaDiario
import com.minhascontas.app.ui.resumo.ResumoCategoriaMensal
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.features.resumos.ResumoTipoDiario
import com.msk.minhascontas.features.resumos.ResumoTipoMensal
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.ContasViewModel
import kotlinx.coroutines.flow.filter
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun PaginadorResumos(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    contasViewModel: ContasViewModel,
    totalPages: Int,
    viewPagerPosition: Int,
    onPageSelected: (Int) -> Unit
) {
    val viewState by contasViewModel.viewState.collectAsState()
    val isMonthly = viewState?.isMonthlySummary ?: true
    val isCategory = viewState?.isCategorySummary ?: false

    // O ViewPager2 nativo é mais estável para gerenciar fragmentos no Celular que o HorizontalPager do Compose
    AndroidView(
        factory = { context ->
            ViewPager2(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                adapter = SummaryFragmentStateAdapter(fragmentManager, lifecycle, totalPages, contasViewModel)
                offscreenPageLimit = 1 
                setCurrentItem(viewPagerPosition, false)
                
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        onPageSelected(position)
                    }
                })
            }
        },
        update = { view ->
            // Sincroniza a posição APENAS se o usuário não estiver interagindo com o ViewPager
            // Isso evita que o Compose \"puxe de volta\" a página durante o gesto de swipe
            val isUserInteracting = view.isFakeDragging || view.scrollState != ViewPager2.SCROLL_STATE_IDLE
            if (!isUserInteracting && view.currentItem != viewPagerPosition && viewPagerPosition != -1) {
                view.setCurrentItem(viewPagerPosition, false)
            }
            
            val currentAdapter = view.adapter as? SummaryFragmentStateAdapter
            if (currentAdapter != null) {
                if (currentAdapter.isMonthly != isMonthly || currentAdapter.isCategory != isCategory) {
                    currentAdapter.updateConfig(isMonthly, isCategory)
                    currentAdapter.notifyDataSetChanged()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class SummaryFragmentStateAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val totalPages: Int,
    private val viewModel: ContasViewModel
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    var isMonthly: Boolean = true
    var isCategory: Boolean = false

    init {
        val state = viewModel.viewState.value
        isMonthly = state?.isMonthlySummary ?: true
        isCategory = state?.isCategorySummary ?: false
    }

    fun updateConfig(monthly: Boolean, category: Boolean) {
        isMonthly = monthly
        isCategory = category
    }

    override fun getItemCount(): Int = totalPages

    override fun createFragment(position: Int): Fragment {
        val dState = ContasViewModel.calculateDateState(position, isMonthly)
        return if (isMonthly) {
            if (isCategory) ResumoCategoriaMensal.newInstance( dState.mes, dState.ano)
            else ResumoTipoMensal.newInstance(position, dState.mes, dState.ano)
        } else {
            if (isCategory) ResumoCategoriaDiario.newInstance(dState.dia, dState.mes, dState.ano)
            else ResumoTipoDiario.newInstance(dState.dia, dState.mes, dState.ano)
        }
    }

    override fun getItemId(position: Int): Long {
        val modeId = (if (isMonthly) 0 else 1) + (if (isCategory) 0 else 2)
        return (position.toLong() shl 4) or modeId.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        val position = (itemId shr 4).toInt()
        val modeId = (itemId and 0xF).toInt()
        val currentModeId = (if (isMonthly) 0 else 1) + (if (isCategory) 0 else 2)
        return position in 0 until totalPages && modeId == currentModeId
    }
}

@Composable
fun MonthYearTabBar(
    selectedPosition: Int,
    contasViewModel: ContasViewModel,
    pageCount: Int,
    months: Array<String?> = contasViewModel.stringMonths,
    onPositionSelected: (Int) -> Unit
) {
    val viewState by contasViewModel.viewState.collectAsState()
    val isMonthly = viewState?.isMonthlySummary ?: true

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isFirstScroll = rememberSaveable { mutableStateOf(true) }

    // Design: 5 abas exatas para mobile (compact), largura fixa para tablets
    val isCompact = configuration.screenWidthDp < 600
    val screenWidth = configuration.screenWidthDp.dp
    val tabWidth = if (isCompact) screenWidth / 5 else 100.dp

    // Sincroniza o scroll da TabBar quando a página muda no Pager
    LaunchedEffect(selectedPosition, pageCount, isMonthly) {
        if (selectedPosition in 0 until pageCount) {
            snapshotFlow { lazyListState.layoutInfo.viewportSize.width }
                .filter { it > 0 }
                .collect { viewportWidth ->
                    val itemWidthPx = with(density) { tabWidth.toPx() }.toInt()
                    val centerOffset = (viewportWidth / 2) - (itemWidthPx / 2)

                    if (isFirstScroll.value) {
                        lazyListState.scrollToItem(selectedPosition, -centerOffset)
                        isFirstScroll.value = false
                    } else {
                        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                        val targetItem = visibleItems.find { it.index == selectedPosition }
                        // Tolerância mínima para garantir centralização precisa
                        if (targetItem == null || abs(targetItem.offset + (targetItem.size / 2) - (viewportWidth / 2)) > 2) {
                            lazyListState.animateScrollToItem(selectedPosition, -centerOffset)
                        }
                    }
                }
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colorResource(R.color.surface)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            count = pageCount,
            key = { it } // Adicionado para garantir estabilidade e recomposição correta
        ) { i ->
            val dState = remember(i, isMonthly) { ContasViewModel.calculateDateState(i, isMonthly) }
            val label = if (isMonthly) {
                "${months[dState.mes - 1]}/${dState.ano % 100}"
            } else {
                "${dState.dia}/${months[dState.mes - 1]}"
            }

            Column(
                modifier = Modifier
                    .width(tabWidth)
                    .clickable { onPositionSelected(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedPosition == i) colorResource(R.color.tab_text_selected)
                    else colorResource(R.color.tab_text_unselected)
                )
                if (selectedPosition == i) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .background(colorResource(R.color.tab_indicator))
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryPane(
    fragmentManager: FragmentManager,
    contasViewModel: ContasViewModel,
    mes: Int,
    ano: Int,
    dia: Int,
    position: Int
) {
    val viewState by contasViewModel.viewState.collectAsState()
    val isMonthly = viewState?.isMonthlySummary ?: true
    val isCategory = viewState?.isCategorySummary ?: false
    val fragmentTag = "Summary_$position"
    
    // Usamos View.generateViewId() para garantir IDs únicos e válidos que o Android reconheça.
    val containerId = rememberSaveable(position) { View.generateViewId() }

    DisposableEffect(fragmentTag) {
        onDispose {
            if (!fragmentManager.isDestroyed) {
                val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)
                if (existingFragment != null) {
                    try {
                        fragmentManager.beginTransaction()
                            .remove(existingFragment)
                            .commitNowAllowingStateLoss()
                    } catch (e: Exception) {
                        Log.e("SummaryPane", "Erro ao remover fragmento $fragmentTag", e)
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = containerId }
        },
        update = { view ->
            if (fragmentManager.isDestroyed || fragmentManager.isStateSaved) return@AndroidView

            view.post {
                if (!view.isAttachedToWindow) return@post
                
                val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)
                
                val expectedClass = if (isMonthly) {
                    if (isCategory) ResumoCategoriaMensal::class.java else ResumoTipoMensal::class.java
                } else {
                    if (isCategory) ResumoCategoriaDiario::class.java else ResumoTipoDiario::class.java
                }

                // Se o fragmento já existe no container correto e é do tipo esperado, não faz nada
                if (existingFragment != null && existingFragment.javaClass == expectedClass) {
                    return@post
                }
                
                // Remove fragmento existente se for diferente
                if (existingFragment != null) {
                    try {
                        fragmentManager.beginTransaction()
                            .remove(existingFragment)
                            .commitNowAllowingStateLoss()
                    } catch (e: Exception) {
                        Log.e("SummaryPane", "Erro ao remover fragmento antigo", e)
                    }
                }

                val fragment = if (isMonthly) {
                    if (isCategory) ResumoCategoriaMensal.newInstance(mes, ano)
                    else ResumoTipoMensal.newInstance(position, mes, ano)
                } else {
                    if (isCategory) ResumoCategoriaDiario.newInstance(dia, mes, ano)
                    else ResumoTipoDiario.newInstance(dia, mes, ano)
                }
                
                try {
                    if (!view.isAttachedToWindow) return@post
                    fragmentManager.beginTransaction()
                        .replace(view.id, fragment, fragmentTag)
                        .commitNowAllowingStateLoss()
                } catch (e: Exception) {
                    Log.w("SummaryPane", "Falha na transação de fragmento $fragmentTag: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Componente unificado para exibição de itens de conta em listas (Mensal ou Pesquisa).
 * Segue diretrizes de design para alta escaneabilidade financeira.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedContaItem(
    conta: Conta,
    isSelected: Boolean,
    showFullDate: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onTogglePagamento: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val currencyFormat = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
    val dateFormat = remember(locale) { DateFormat.getDateInstance(DateFormat.SHORT, locale) }
    val semana = remember { context.resources.getStringArray(R.array.Semana) }

    val backgroundColor = if (isSelected) colorResource(R.color.linha_selecionada) else Color.Transparent

    val valorColor = when (conta.tipo) {
        ContasContract.TIPO_DESPESA -> colorResource(R.color.despesa_color)
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        else -> colorResource(R.color.aplicacao_color)
    }

    val cal = remember(conta) {
        Calendar.getInstance().apply { set(conta.ano, conta.mes - 1, conta.dia) }
    }
    val diaSemana = semana[cal.get(Calendar.DAY_OF_WEEK) - 1]

    val displayName = remember(conta) {
        if (conta.qtRepete > 1 && (conta.tipo == ContasContract.TIPO_DESPESA) &&
            (conta.classeConta == ContasContract.CLASSE_DESPESA_CARTAO ||
                    conta.classeConta == ContasContract.CLASSE_DESPESA_PRESTACOES)
        ) {
            "${conta.nome} ${conta.nRepete}/${conta.qtRepete}"
        } else conta.nome
    }

    val subTitulo = remember(conta) {
        val classeLabel = LabelUtils.getClasseLabel(context, conta.tipo, conta.classeConta)
        val categoriaLabel = LabelUtils.getCategoriaLabel(context, conta.categoria)
        if (conta.tipo == ContasContract.TIPO_DESPESA) "$classeLabel | $categoriaLabel" else classeLabel
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coluna da Esquerda: Temporal
        if (showFullDate) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(text = dateFormat.format(cal.time), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = diaSemana, fontSize = 11.sp, color = Color.Gray)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Text(text = conta.dia.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = diaSemana, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Coluna Central: Nome e Contexto
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subTitulo, fontSize = 12.sp, color = Color.Gray)
        }

        // Coluna da Direita: Valor e Status
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conta.tipo != ContasContract.TIPO_APLICACAO) {
                IconButton(
                    onClick = { onTogglePagamento() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if ("paguei" == conta.pagamento) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = valorColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = valorColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Para aplicações, mostramos o ícone de visto se for pesquisa e já estiver concluído
                if (showFullDate && conta.pagamento == ContasContract.STATUS_PAGO_RECEBIDO) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = valorColor ,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = currencyFormat.format(conta.valor),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valorColor,
                modifier = Modifier.clickable { if (conta.tipo != ContasContract.TIPO_APLICACAO) onTogglePagamento() else onClick() }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

/**
 * TopAppBar unificada para modo de seleção.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedContas: List<Conta>,
    onSelectionClear: () -> Unit,
    onTogglePagamento: () -> Unit,
    onEditar: (List<Long>) -> Unit,
    onExcluir: () -> Unit,
    onLembrete: (() -> Unit)? = null,
    onCoachClick: ((String) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val currencyFormat = remember(configuration.locales[0]) { NumberFormat.getCurrencyInstance(configuration.locales[0]) }
    val total = selectedContas.sumOf { it.valor }
    val count = selectedContas.size
    val hasCoachSelected = selectedContas.any { it.isCoach() }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = if (count == 1) selectedContas.first().nome else pluralStringResource(R.plurals.selecao, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
                if (total > 0) {
                    Text(text = currencyFormat.format(total), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onSelectionClear) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
            }
        },
        actions = {
            IconButton(onClick = onTogglePagamento) {
                Icon(Icons.Default.Payment, contentDescription = "Pagar/Pendente")
            }

            if (count == 1 && onLembrete != null) {
                IconButton(onClick = onLembrete) {
                    Icon(Icons.Default.Event, contentDescription = "Lembrete")
                }
            }

            // Lógica Unificada de Edição (Normal ou Coach)
            if (!hasCoachSelected || count == 1) {
                IconButton(onClick = {
                    if (count == 1 && selectedContas.first().isCoach()) {
                        onCoachClick?.invoke(selectedContas.first().codigo)
                    } else {
                        onEditar(selectedContas.map { it.idConta })
                    }
                    onSelectionClear()
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            }

            IconButton(onClick = onExcluir) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * TopAppBar unificada para modo padrão (sem seleção).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopAppBar(
    title: String? = null,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onAiAnalysisClick: (() -> Unit)? = null,
    onAjustesClick: (() -> Unit)? = null,
    onSobreClick: (() -> Unit)? = null,
    hasUnreadNotifications: Boolean = false,
    onShowNotifications: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    actions: @Composable (RowScope.() -> Unit)? = null,
    titleContent: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            if (titleContent != null) {
                titleContent()
            } else if (title != null) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    if (!subtitle.isNullOrBlank()) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = {
            if (actions != null) {
                actions()
            } else {
                if (hasUnreadNotifications && onShowNotifications != null) {
                    IconButton(onClick = onShowNotifications) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Yellow)
                    }
                }
                if (onFilterClick != null) {
                    IconButton(onClick = onFilterClick) {
                        Icon(painterResource(R.drawable.ic_filter_list_white), contentDescription = "Filtrar")
                    }
                }
                if (onSearchClick != null) {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
                if (onShareClick != null) {
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar")
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                val showAi = onAiAnalysisClick != null
                val showAjustes = onAjustesClick != null
                val showSobre = onSobreClick != null

                if (showAi || showAjustes || showSobre) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (showAi) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.consultoria_ia)) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                onClick = { showMenu = false; onAiAnalysisClick?.invoke() }
                            )
                        }
                        if (showAjustes) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_ajustes)) },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { showMenu = false; onAjustesClick?.invoke() }
                            )
                        }
                        if (showSobre) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sobre)) },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = { showMenu = false; onSobreClick?.invoke() }
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
