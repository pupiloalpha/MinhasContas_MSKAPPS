package com.msk.minhascontas.ui

import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.ui.layouts.StandardTopAppBar
import com.msk.minhascontas.ui.theme.FabColor
import com.msk.minhascontas.viewmodel.PlanoFinanceiroViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCoachScreen(
    onNavegarSimulador: () -> Unit,
    onMetaClick: (MetaFinanceira) -> Unit,
    onVoltar: () -> Unit,
    onSearch: () -> Unit,
    onAjustes: () -> Unit,
    onSobre: () -> Unit,
    viewModel: PlanoFinanceiroViewModel = viewModel()
) {
    // Coleta as metas recalculadas dinamicamente via StateFlow
    val metas by viewModel.metasComProgressoReal.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val currencyFormat = remember(locale) { NumberFormat.getCurrencyInstance(locale) }

    // Recalcula o progresso ao entrar ou retomar a tela
    LaunchedEffect(Unit) {
        viewModel.recalcularProgressoMetasComBanco()
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            StandardTopAppBar(
                title = stringResource(R.string.coach_title_dashboard),
                onBackClick = onVoltar,
                onSearchClick = onSearch,
                onAjustesClick = onAjustes,
                onSobreClick = onSobre,
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavegarSimulador,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OrcamentoEstrategicoCard(viewModel, currencyFormat) }
            item { DiagnosticoAICard(viewModel) }
            item {
                Text(
                    text = stringResource(R.string.coach_your_plans),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (metas.isEmpty()) {
                item { EmptyGoalsCard(onNavegarSimulador) }
            } else {
                items(metas, key = { it.id }) { meta ->
                    MetaItemCard(meta, currencyFormat, onMetaClick)
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun OrcamentoEstrategicoCard(viewModel: PlanoFinanceiroViewModel, currencyFormat: NumberFormat) {
    val totalDisponivel = viewModel.valorMetaPrioridade20
    val metas by viewModel.metasComProgressoReal.collectAsState()
    val valorComprometido = metas.sumOf { it.aporteMensalAlvo }
    val valorLivre = (totalDisponivel - valorComprometido).coerceAtLeast(0.0)

    val progresso = if (totalDisponivel > 0) (valorComprometido / totalDisponivel).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.coach_strategic_budget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(R.string.coach_monthly_budget_20),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progresso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem(stringResource(R.string.coach_total), currencyFormat.format(totalDisponivel), MaterialTheme.colorScheme.onPrimary)
                InfoItem(stringResource(R.string.coach_free), currencyFormat.format(valorLivre), MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun DiagnosticoAICard(viewModel: PlanoFinanceiroViewModel) {
    val result = viewModel.diagnosticoIA
    val isLoading = viewModel.isAnalyzingIA

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.coach_diag_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (result == null && !isLoading) {
                Text(
                    text = if (viewModel.valorPrestacoesAtivas > 0)
                        stringResource(R.string.coach_diag_debt_found, NumberFormat.getCurrencyInstance().format(viewModel.valorPrestacoesAtivas))
                    else
                        stringResource(R.string.coach_diag_no_investments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.gerarAnaliseIA() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.coach_diag_btn_ai))
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (result is AIResult.Success) {
                val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            text = Html.fromHtml(result.content, Html.FROM_HTML_MODE_LEGACY)
                            setTextColor(textColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = { viewModel.gerarAnaliseIA() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.consultoria_ia))
                }
            }
        }
    }
}

@Composable
fun MetaItemCard(meta: MetaFinanceira, currencyFormat: NumberFormat, onClick: (MetaFinanceira) -> Unit) {
    val progresso = if (meta.valorObjetivo > 0) (meta.valorAtual / meta.valorObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted = meta.valorAtual >= meta.valorObjetivo && meta.valorObjetivo > 0

    val icon: ImageVector = when (meta.tipoMeta) {
        MetaFinanceira.TIPO_DIVIDA -> Icons.AutoMirrored.Filled.TrendingDown
        MetaFinanceira.TIPO_RESERVA -> Icons.Default.Shield
        MetaFinanceira.TIPO_INVESTIMENTO -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.LocalAtm
    }

    val iconColor = when (meta.tipoMeta) {
        MetaFinanceira.TIPO_DIVIDA -> MaterialTheme.colorScheme.error
        MetaFinanceira.TIPO_RESERVA, MetaFinanceira.TIPO_INVESTIMENTO -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(meta) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.15f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meta.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = when (meta.tipoMeta) {
                            MetaFinanceira.TIPO_DIVIDA -> stringResource(R.string.coach_debt_amortization)
                            MetaFinanceira.TIPO_RESERVA -> stringResource(R.string.coach_emergency_reserve)
                            MetaFinanceira.TIPO_INVESTIMENTO -> stringResource(R.string.coach_wealth_accumulation)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isCompleted) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(stringResource(R.string.coach_completed), color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(4.dp))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.coach_percent_completed, (progresso * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progresso },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = iconColor,
                trackColor = iconColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = stringResource(R.string.valor), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = currencyFormat.format(meta.valorAtual), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.coach_target_value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = currencyFormat.format(meta.valorObjetivo), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun EmptyGoalsCard(onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = FabColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.coach_no_goals_msg),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(stringResource(R.string.coach_start_now).uppercase())
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}