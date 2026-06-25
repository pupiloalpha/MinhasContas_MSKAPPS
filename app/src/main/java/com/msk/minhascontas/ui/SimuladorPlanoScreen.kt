package com.msk.minhascontas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.viewmodel.PlanoFinanceiroViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimuladorCoachScreen(
    metaParaEditar: MetaFinanceira? = null,
    onMetaCriada: () -> Unit,
    onVoltar: () -> Unit,
    viewModel: PlanoFinanceiroViewModel = viewModel()
) {
    val locale = LocalConfiguration.current.locales[0]
    val currencyFormat = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
    val dateFormat = remember(locale) { SimpleDateFormat("MMMM yyyy", locale) }
    
    var nomeMeta by remember { mutableStateOf(metaParaEditar?.nome ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(metaParaEditar) {
        if (metaParaEditar != null) {
            viewModel.carregarParaEdicao(metaParaEditar)
        } else {
            viewModel.resetarSimulacao()
        }
    }

    if (showDeleteDialog && metaParaEditar != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.titulo_excluir)) },
            text = { Text(stringResource(R.string.confirmar_exclusao_meta)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirMeta(metaParaEditar)
                    showDeleteDialog = false
                    onVoltar()
                }) {
                    Text(stringResource(R.string.sim), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.nao))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (metaParaEditar == null) stringResource(R.string.coach_title_simulator) 
                               else stringResource(R.string.coach_title_edit),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (metaParaEditar != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seletor de Tipo
            Text(stringResource(R.string.coach_plan_now_question), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoButton(
                    label = stringResource(R.string.coach_debt),
                    isSelected = viewModel.tipoSimulacao == MetaFinanceira.TIPO_DIVIDA,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.tipoSimulacao = MetaFinanceira.TIPO_DIVIDA
                        viewModel.atualizarSimulacao()
                    }
                )
                TipoButton(
                    label = stringResource(R.string.coach_reserve),
                    isSelected = viewModel.tipoSimulacao == MetaFinanceira.TIPO_RESERVA,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.tipoSimulacao = MetaFinanceira.TIPO_RESERVA
                        viewModel.atualizarSimulacao()
                    }
                )
                TipoButton(
                    label = stringResource(R.string.coach_investment),
                    isSelected = viewModel.tipoSimulacao == MetaFinanceira.TIPO_INVESTIMENTO,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.tipoSimulacao = MetaFinanceira.TIPO_INVESTIMENTO
                        viewModel.atualizarSimulacao()
                    }
                )
            }

            // Nome da Meta
            OutlinedTextField(
                value = nomeMeta,
                onValueChange = { nomeMeta = it },
                label = { Text(stringResource(R.string.coach_goal_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Valores
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = if (viewModel.valorTotal == 0.0) "" else viewModel.valorTotal.toString(),
                    onValueChange = { 
                        viewModel.valorTotal = it.toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { 
                        Text(if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_DIVIDA) 
                             stringResource(R.string.coach_total_debt) else stringResource(R.string.coach_target_value)) 
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") }
                )
                
                OutlinedTextField(
                    value = if (viewModel.valorAtual == 0.0) "" else viewModel.valorAtual.toString(),
                    onValueChange = { 
                        viewModel.valorAtual = it.toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { Text(stringResource(R.string.valor_atual)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") }
                )
            }

            // Sugestões para Reserva
            if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_RESERVA) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.coach_reserve_suggestion_value, currencyFormat.format(viewModel.gastosMensaisEstimados)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionButton(stringResource(R.string.coach_3_months), Modifier.weight(1f)) { viewModel.sugerirReserva(3) }
                        SuggestionButton(stringResource(R.string.coach_6_months), Modifier.weight(1f)) { viewModel.sugerirReserva(6) }
                        SuggestionButton(stringResource(R.string.coach_12_months), Modifier.weight(1f)) { viewModel.sugerirReserva(12) }
                    }
                }
            }

            // Juros e Aporte
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = if (viewModel.taxaJuros == 0.0) "" else viewModel.taxaJuros.toString(),
                    onValueChange = { 
                        viewModel.taxaJuros = it.toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { 
                        Text(if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_DIVIDA) 
                             stringResource(R.string.coach_debt_interest) else stringResource(R.string.coach_monthly_yield)) 
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("%") }
                )
                
                OutlinedTextField(
                    value = if (viewModel.aporteMensal == 0.0) "" else viewModel.aporteMensal.toString(),
                    onValueChange = { 
                        viewModel.aporteMensal = it.toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { Text(stringResource(R.string.coach_monthly_contribution)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") },
                    isError = viewModel.aporteMensal > viewModel.valorDisponivelTotal20Porcento
                )
            }

            if (viewModel.aporteMensal > viewModel.valorDisponivelTotal20Porcento) {
                Text(
                    stringResource(R.string.coach_above_free_limit),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Resultado da Simulação
            CalculationResultCard(viewModel, dateFormat)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    viewModel.confirmarMeta(nomeMeta.ifEmpty { "Minha Meta" })
                    onMetaCriada() 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.mesesRestantes > 0 && nomeMeta.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.total_planejado_color))
            ) {
                Text(stringResource(R.string.coach_confirm_plan).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TipoButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SuggestionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CalculationResultCard(viewModel: PlanoFinanceiroViewModel, dateFormat: SimpleDateFormat) {
    val meses = viewModel.mesesRestantes
    val dataFim = viewModel.dataPrevisaoFim

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.coach_impact_future),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            if (meses == -1) {
                Text(
                    stringResource(R.string.coach_contribution_too_low),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else if (meses > 0) {
                Text(
                    stringResource(R.string.coach_months_count, meses),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (dataFim != null) {
                    Text(
                        stringResource(R.string.coach_estimated_completion, dateFormat.format(dataFim)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            } else {
                Text(
                    stringResource(R.string.coach_fill_values),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
