package com.msk.minhascontas.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.abs
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.features.resumos.ResumoCategoriaDiario
import com.msk.minhascontas.features.resumos.ResumoCategoriaMensal
import com.msk.minhascontas.features.resumos.ResumoTipoDiario
import com.msk.minhascontas.features.resumos.ResumoTipoMensal
import com.msk.minhascontas.viewmodel.ContasViewModel

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
            // Isso evita que o Compose "puxe de volta" a página durante o gesto de swipe
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
            if (isCategory) ResumoCategoriaMensal.newInstance(position, dState.mes, dState.ano)
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
                        android.util.Log.e("SummaryPane", "Erro ao remover fragmento $fragmentTag", e)
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
                        android.util.Log.e("SummaryPane", "Erro ao remover fragmento antigo", e)
                    }
                }

                val fragment = if (isMonthly) {
                    if (isCategory) ResumoCategoriaMensal.newInstance(position, mes, ano)
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
                    android.util.Log.w("SummaryPane", "Falha na transação de fragmento $fragmentTag: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
