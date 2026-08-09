package com.msk.minhascontas.ui.layouts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.viewmodel.ContasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileLayout(
    contasViewModel: ContasViewModel,
    contasRepository: ContasRepository?,
    fragmentManager: FragmentManager,
    totalPages: Int,
    viewPagerPosition: Int,
    hasUnreadNotifications: Boolean,
    onShowNotifications: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToContas: () -> Unit,
    onNavigateToMetas: () -> Unit,
    onNavigateToPlanejamento: () -> Unit,
    onNavigateToBusca: () -> Unit,
    onShare: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToSobre: () -> Unit,
    onNavigateToNovaConta: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Para o MobileLayout da Activity principal, o título é fixo como o nome do app
    // Já que as listas detalhadas abrem em novas Activities.
    val appBarTitle = stringResource(R.string.app_name)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        StandardTopAppBar(
            title = appBarTitle,
            onSearchClick = onNavigateToBusca,
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
            onAjustesClick = onNavigateToAjustes,
            onSobreClick = onNavigateToSobre,
            hasUnreadNotifications = hasUnreadNotifications,
            onShowNotifications = onShowNotifications,
            containerColor = colorResource(R.color.primary)
        )

        MonthYearTabBar(
            selectedPosition = viewPagerPosition,
            contasViewModel = contasViewModel,
            pageCount = totalPages,
            months = contasViewModel.stringMonths,
            onPositionSelected = { contasViewModel.setViewPagerPosition(it) }
        )

        Box(modifier = Modifier.weight(1f)) {
            PaginadorResumos(
                fragmentManager = fragmentManager,
                lifecycle = lifecycleOwner.lifecycle,
                contasViewModel = contasViewModel,
                totalPages = totalPages,
                viewPagerPosition = viewPagerPosition,
                onPageSelected = { position ->
                    // Removemos a trava position != viewPagerPosition para garantir
                    // que o ViewModel e o ViewPager2 sempre se alinhem no START_PAGE
                    contasViewModel.setViewPagerPosition(position)
                }
            )
        }

        // Simulação de Bottom Navigation (como no original NavigationSuiteScaffold mas adaptado)
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(painterResource(R.drawable.ic_dashboard), null) },
                label = { Text(stringResource(R.string.nav_dashboard)) },
                selected = false,
                onClick = onNavigateToDashboard
            )
            NavigationBarItem(
                icon = { Icon(painterResource(R.drawable.ic_accounts), null) },
                label = { Text(stringResource(R.string.nav_contas)) },
                selected = false,
                onClick = onNavigateToContas
            )
            NavigationBarItem(
                icon = { Icon(painterResource(R.drawable.ic_metas), null) },
                label = { Text(stringResource(R.string.nav_metas)) },
                selected = false,
                onClick = onNavigateToMetas
            )
            NavigationBarItem(
                icon = { Icon(painterResource(R.drawable.ic_planejamento), null) },
                label = { Text(stringResource(R.string.nav_planejamento)) },
                selected = false,
                onClick = onNavigateToPlanejamento
            )
        }
    }

    // FAB
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = onNavigateToNovaConta,
            containerColor = colorResource(R.color.fab_color),
            contentColor = colorResource(R.color.on_fab_color),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) // Sobre a BottomBar
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}
