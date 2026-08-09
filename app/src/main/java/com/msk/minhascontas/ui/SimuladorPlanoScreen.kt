package com.msk.minhascontas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.ui.layouts.MCAlertDialog
import com.msk.minhascontas.ui.theme.*
import com.msk.minhascontas.utils.Pontoprojecao
import com.msk.minhascontas.viewmodel.PlanoFinanceiroViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showDatePicker by remember { mutableStateOf(false) }

    var valorTotalText by remember { mutableStateOf("") }
    var valorAtualText by remember { mutableStateOf("") }
    var taxaJurosText by remember { mutableStateOf("") }
    var aporteMensalText by remember { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()

    val fieldTextColor = if (isDark) DarkOnSurface else OnSurface
    val fieldFocusedColor = if (isDark) DarkReceitaColor else Primary
    val fieldUnfocusedColor = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
    val fieldErrorColor = MaterialTheme.colorScheme.error

    val directTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = fieldTextColor,
        unfocusedTextColor = fieldTextColor,
        focusedBorderColor = fieldFocusedColor,
        unfocusedBorderColor = fieldUnfocusedColor,
        focusedLabelColor = fieldFocusedColor,
        unfocusedLabelColor = fieldUnfocusedColor,
        focusedPrefixColor = fieldFocusedColor,
        unfocusedPrefixColor = fieldUnfocusedColor,
        focusedSuffixColor = fieldFocusedColor,
        unfocusedSuffixColor = fieldUnfocusedColor,
        errorBorderColor = fieldErrorColor,
        errorLabelColor = fieldErrorColor,
        errorPrefixColor = fieldErrorColor,
        errorSuffixColor = fieldErrorColor
    )

    LaunchedEffect(metaParaEditar) {
        if (metaParaEditar != null) {
            viewModel.carregarParaEdicao(metaParaEditar)
            valorTotalText = String.format(Locale.getDefault(), "%.2f", metaParaEditar.valorObjetivo)
            valorAtualText = String.format(Locale.getDefault(), "%.2f", metaParaEditar.valorAtual)
            taxaJurosText = String.format(Locale.getDefault(), "%.2f", metaParaEditar.taxaJurosMensal)
            aporteMensalText = String.format(Locale.getDefault(), "%.2f", metaParaEditar.aporteMensalAlvo)
        } else {
            viewModel.resetarSimulacao()
            valorTotalText = ""
            valorAtualText = ""
            taxaJurosText = ""
            aporteMensalText = if (viewModel.capacidadeAporteLivre20 > 0) {
                String.format(Locale.getDefault(), "%.2f", viewModel.capacidadeAporteLivre20)
            } else ""
        }
    }

    if (showDeleteDialog && metaParaEditar != null) {
        MCAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.titulo_excluir),
            text = stringResource(R.string.confirmar_exclusao_meta),
            confirmLabel = stringResource(R.string.sim),
            onConfirm = {
                viewModel.excluirMeta(metaParaEditar)
                showDeleteDialog = false
                onVoltar()
            },
            dismissLabel = stringResource(R.string.nao)
        )
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (metaParaEditar == null) stringResource(R.string.coach_title_simulator)
                        else stringResource(R.string.coach_title_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (metaParaEditar != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.coach_plan_now_question),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fieldTextColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

            OutlinedTextField(
                value = nomeMeta,
                onValueChange = { nomeMeta = it },
                label = { Text(stringResource(R.string.coach_goal_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = directTextFieldColors
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = fieldUnfocusedColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, fieldUnfocusedColor)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(dateFormat.format(viewModel.dataInicioMeta))
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = viewModel.dataInicioMeta.time
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.dataInicioMeta = Date(it)
                            }
                            showDatePicker = false
                        }) { Text(stringResource(R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancelar)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = valorTotalText,
                    onValueChange = {
                        valorTotalText = it
                        viewModel.valorTotal = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = {
                        Text(
                            if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_DIVIDA)
                                stringResource(R.string.coach_total_debt)
                            else stringResource(R.string.coach_target_value)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") },
                    colors = directTextFieldColors
                )

                OutlinedTextField(
                    value = valorAtualText,
                    onValueChange = {
                        valorAtualText = it
                        viewModel.valorAtual = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { Text(stringResource(R.string.valor_atual)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") },
                    colors = directTextFieldColors
                )
            }

            if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_RESERVA) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.coach_reserve_suggestion_value,
                            currencyFormat.format(viewModel.despesaMediaReal)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = fieldUnfocusedColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionButton(
                            stringResource(R.string.coach_3_months),
                            Modifier.weight(1f)
                        ) {
                            viewModel.sugerirReserva(3)
                            valorTotalText = String.format(Locale.getDefault(), "%.2f", viewModel.valorTotal)
                        }
                        SuggestionButton(
                            stringResource(R.string.coach_6_months),
                            Modifier.weight(1f)
                        ) {
                            viewModel.sugerirReserva(6)
                            valorTotalText = String.format(Locale.getDefault(), "%.2f", viewModel.valorTotal)
                        }
                        SuggestionButton(
                            stringResource(R.string.coach_12_months),
                            Modifier.weight(1f)
                        ) {
                            viewModel.sugerirReserva(12)
                            valorTotalText = String.format(Locale.getDefault(), "%.2f", viewModel.valorTotal)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = taxaJurosText,
                    onValueChange = {
                        taxaJurosText = it
                        viewModel.taxaJuros = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = {
                        Text(
                            if (viewModel.tipoSimulacao == MetaFinanceira.TIPO_DIVIDA)
                                stringResource(R.string.coach_debt_interest)
                            else stringResource(R.string.coach_monthly_yield)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("%") },
                    colors = directTextFieldColors
                )

                OutlinedTextField(
                    value = aporteMensalText,
                    onValueChange = {
                        aporteMensalText = it
                        viewModel.aporteMensal = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                        viewModel.atualizarSimulacao()
                    },
                    label = { Text(stringResource(R.string.coach_monthly_contribution)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencyFormat.currency?.symbol ?: "") },
                    isError = viewModel.aporteMensal > viewModel.capacidadeAporteLivre20,
                    colors = directTextFieldColors
                )
            }

            if (viewModel.capacidadeAporteLivre20 > 0) {
                Column {
                    Text(
                        text = stringResource(R.string.simular_aporte_interativo),
                        style = MaterialTheme.typography.labelMedium,
                        color = fieldTextColor
                    )
                    Slider(
                        value = viewModel.aporteMensal.toFloat().coerceIn(0f, (viewModel.capacidadeAporteLivre20 * 1.5).toFloat()),
                        onValueChange = { val valDouble = it.toDouble()
                            viewModel.aporteMensal = valDouble
                            aporteMensalText = String.format(Locale.getDefault(), "%.2f", valDouble)
                            viewModel.atualizarSimulacao()
                        },
                        valueRange = 0f..(viewModel.capacidadeAporteLivre20 * 1.5).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = fieldFocusedColor,
                            activeTrackColor = fieldFocusedColor
                        )
                    )
                }
            }

            if (viewModel.aporteMensal > viewModel.capacidadeAporteLivre20) {
                Text(
                    stringResource(R.string.coach_above_free_limit),
                    color = fieldErrorColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Resultado do Cálculo
            CalculationResultCard(viewModel, dateFormat)

            // Gráfico Interativo de Evolução com Biblioteca Vico
            AnimatedVisibility(visible = viewModel.serieProjecao.isNotEmpty()) {
                GraficoEvolucaoCard(
                    serie = viewModel.serieProjecao,
                    tipo = viewModel.tipoSimulacao,
                    totalJuros = viewModel.totalJurosEstimado,
                    currencyFormat = currencyFormat
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.confirmarMeta(nomeMeta.ifEmpty { "Minha Meta" })
                    onMetaCriada()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.mesesRestantes > 0 && nomeMeta.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = fieldTextColor.copy(alpha = 0.12f),
                    disabledContentColor = fieldTextColor.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    stringResource(R.string.coach_confirm_plan).uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GraficoEvolucaoCard(
    serie: List<Pontoprojecao>,
    tipo: Int,
    totalJuros: Double,
    currencyFormat: NumberFormat
) {
    val lineColor = if (tipo == MetaFinanceira.TIPO_DIVIDA) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val dinheiroSemCentavos = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
    }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            background = rememberLineComponent(fill = fill(MaterialTheme.colorScheme.surface), thickness = 2.dp)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = lineColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (tipo == MetaFinanceira.TIPO_DIVIDA)
                        stringResource(R.string.curva_amortizacao)
                    else
                        stringResource(R.string.curva_crescimento_patrimonial),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val model = remember(serie) {
                if (serie.size > 1) {
                    val xValues = serie.indices.toList()
                    val yValues = serie.map { it.saldo.toFloat() }
                    CartesianChartModel(
                        LineCartesianLayerModel.build {
                            series(xValues, yValues)
                        }
                    )
                } else null
            }

            if (model != null) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(fill(lineColor)))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = CartesianValueFormatter { _, value, _ -> dinheiroSemCentavos.format(value) }
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(),
                        marker = marker
                    ),
                    model = model,
                    zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
                    modifier = Modifier.height(180.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (tipo == MetaFinanceira.TIPO_DIVIDA)
                            stringResource(R.string.juros_pagos)
                        else
                            stringResource(R.string.rendimento_estimado),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalJuros),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }
        }
    }
}

@Composable
fun TipoButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val activeContainer = if (isDark) DarkPrimary else Primary
    val activeContent = if (isDark) DarkOnPrimary else OnPrimary
    val inactiveContainer = if (isDark) DarkSurface else Color(0xFFE0E0E0)
    val inactiveContent = if (isDark) DarkOnSurface else OnSurface

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) activeContainer else inactiveContainer,
            contentColor = if (isSelected) activeContent else inactiveContent
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SuggestionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val buttonColor = if (isDark) DarkReceitaColor else Primary

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = buttonColor
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(buttonColor)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CalculationResultCard(viewModel: PlanoFinanceiroViewModel, dateFormat: SimpleDateFormat) {
    val meses = viewModel.mesesRestantes
    val dataFim = viewModel.dataPrevisaoFim
    val isDark = isSystemInDarkTheme()

    val cardBg = if (isDark) DarkPrimary else Primary
    val cardContent = if (isDark) DarkOnPrimary else OnPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = cardContent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.coach_impact_future),
                style = MaterialTheme.typography.labelLarge,
                color = cardContent,
                textAlign = TextAlign.Center
            )

            if (meses == -1) {
                Text(
                    text = stringResource(R.string.coach_contribution_too_low),
                    color = DarkDespesaColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (meses > 0) {
                Text(
                    text = stringResource(R.string.coach_months_count, meses),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cardContent,
                    textAlign = TextAlign.Center
                )
                if (dataFim != null) {
                    Text(
                        text = stringResource(R.string.coach_estimated_completion, dateFormat.format(dataFim)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardContent.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.coach_fill_values),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardContent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}