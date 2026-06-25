package com.msk.minhascontas.features.graficos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas.ContaFilter
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.GraficosViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill as vicoFill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.NumberFormat
import java.util.*

@Composable
fun GraficosScreen(
    mes: Int, 
    ano: Int, 
    dia: Int? = null, 
    refreshTrigger: Int = 0,
    viewModel: GraficosViewModel = viewModel()
) {
    val context = LocalContext.current
    val repository = remember { ContasRepository.getInstance(context) }
    val dinheiro = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // Atualiza o estado do ViewModel quando os parâmetros mudam
    LaunchedEffect(mes, ano, dia) {
        viewModel.updateDate(mes, ano, dia)
    }

    // Observa os dados do Room via Flow
    val allContas by viewModel.allContas.collectAsState()

    // Cores do Tema
    val colorRec = colorResource(R.color.receita_color)
    val colorDesp = colorResource(R.color.despesa_color)
    val colorAplic = colorResource(R.color.aplicacao_color)
    val colorRecCard = colorResource(R.color.card_receita_color)
    val colorDespCard = colorResource(R.color.card_despesa_color)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Marcador para exibir valores ao tocar
    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            background = rememberLineComponent(vicoFill(MaterialTheme.colorScheme.surface), thickness = 2.dp)
        )
    )

    // Constantes do Contrato
    val tipoDespesa = ContasContract.TIPO_DESPESA
    val tipoReceita = ContasContract.TIPO_RECEITA
    val tipoAplicacao = ContasContract.TIPO_APLICACAO
    val statusPago = ContasContract.STATUS_PAGO_RECEBIDO
    val statusPendente = ContasContract.STATUS_PENDENTE

    if (allContas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.dica_vazio), color = onSurfaceVariant)
        }
        return
    }

    // Cálculos Gerais para o Donut
    val vDesp = allContas.filter { it.tipo == tipoDespesa }.sumOf { it.valor }.toFloat()
    val vRec = allContas.filter { it.tipo == tipoReceita }.sumOf { it.valor }.toFloat()
    val vAplic = allContas.filter { it.tipo == tipoAplicacao }.sumOf { it.valor }.toFloat()
    val vSaldoGeral = vRec - vDesp

    // Dados para Regra 50/30/20 e Metas
    val receitaRef = prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()
    val defaultPercentages = mapOf(
        0 to 15.0, 1 to 10.0, 2 to 10.0, 3 to 25.0, 4 to 5.0,
        5 to 5.0, 6 to 5.0, 7 to 5.0, 8 to 20.0
    )

    val vNecessidades = allContas.filter {
        it.tipo == tipoDespesa && it.categoria in listOf(3, 4, 1, 5)
    }.sumOf { it.valor }.toFloat()

    val vDesejos = allContas.filter {
        it.tipo == tipoDespesa && it.categoria in listOf(0, 2, 6, 7)
    }.sumOf { it.valor }.toFloat()

    val vInvestimentosRegra = remember(allContas, vAplic) {
        val despesasInvestimento = allContas.filter { it.tipo == tipoDespesa && it.categoria == 8 }
        val aplicacoes = allContas.filter { it.tipo == tipoAplicacao }
        val vExtra = despesasInvestimento.filter { desp ->
            aplicacoes.none { it.nome == desp.nome && it.valor == desp.valor && it.dia == desp.dia }
        }.sumOf { it.valor }.toFloat()
        vAplic + vExtra
    }

    val totalRegra = vNecessidades + vDesejos + vInvestimentosRegra

    val labelEssencial = stringResource(R.string.grafico_essencial)
    val labelDesejos = stringResource(R.string.grafico_desejos)
    val labelInvest = stringResource(R.string.grafico_investimentos)
    val unitK = stringResource(R.string.grafico_unidade_milhar)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ChartCard(title = stringResource(R.string.dica_grafico_contas)) {
                DonutChart(
                    values = listOf(vDesp, vRec, vAplic),
                    colors = listOf(colorDesp, colorRec, colorAplic),
                    centerText = "${stringResource(R.string.linha_saldo)}\n${dinheiro.format(vSaldoGeral.toDouble())}",
                    labels = stringArrayResource(R.array.GraficoContas).toList()
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.grafico_saude_financeira_titulo)) {
                val model = remember(totalRegra) {
                    if (totalRegra > 0) {
                        CartesianChartModel(
                            ColumnCartesianLayerModel.build {
                                series(x = listOf(0, 1, 2), y = listOf(50f, 30f, 20f))
                                series(x = listOf(0, 1, 2), y = listOf(
                                    (vNecessidades / totalRegra) * 100,
                                    (vDesejos / totalRegra) * 100,
                                    (vInvestimentosRegra / totalRegra) * 100
                                ))
                            }
                        )
                    } else null
                }
                if (model != null) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(
                                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                    rememberLineComponent(fill = vicoFill(onSurfaceVariant.copy(0.2f)), thickness = 20.dp),
                                    rememberLineComponent(fill = vicoFill(colorDesp), thickness = 20.dp, shape = CorneredShape.rounded(allPercent = 40)),
                                    rememberLineComponent(fill = vicoFill(colorDesp), thickness = 20.dp, shape = CorneredShape.rounded(allPercent = 40)),
                                    rememberLineComponent(fill = vicoFill(colorAplic), thickness = 20.dp, shape = CorneredShape.rounded(allPercent = 40))
                                ),
                                mergeMode = { ColumnCartesianLayer.MergeMode.Grouped() }
                            ),
                            startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> "${v.toInt()}%" }),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = { _, v, _ ->
                                    when(v.toInt()) {
                                        0 -> labelEssencial
                                        1 -> labelDesejos
                                        2 -> labelInvest
                                        else -> ""
                                    }
                                },
                                labelRotationDegrees = -45f
                            ),
                            marker = marker
                        ),
                        model = model,
                        modifier = Modifier.height(240.dp)
                    )
                    ChartLegend(listOf(
                        "Ideal" to onSurfaceVariant.copy(0.2f),
                        "Real" to colorDesp
                    ))
                }
            }
        }

        item {
            ChartCard(title = stringResource(R.string.resumo_saldo)) {
                val model = remember(allContas) {
                    var current = 0f
                    val daysInMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, ano)
                        set(Calendar.MONTH, mes - 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val xValues = (1..daysInMonth).toList()
                    val yValues = xValues.map { i ->
                        val dayRec = allContas.filter { it.tipo == tipoReceita && it.dia == i }.sumOf { it.valor }.toFloat()
                        val dayDesp = allContas.filter { it.tipo == tipoDespesa && it.dia == i }.sumOf { it.valor }.toFloat()
                        current += (dayRec - dayDesp)
                        current
                    }
                    CartesianChartModel(LineCartesianLayerModel.build { series(xValues, yValues) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(vicoFill(colorRec)))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = CartesianValueFormatter { _, value, _ -> dinheiro.format(value) }
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(labelRotationDegrees = -45f),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.grafico_tendencia_gasto_titulo)) {
                val model = remember(allContas) {
                    val daysInMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, ano)
                        set(Calendar.MONTH, mes - 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    var acumuladoReal = 0f
                    val yReal = (1..daysInMonth).map { d ->
                        acumuladoReal += allContas.filter { it.tipo == tipoDespesa && it.dia == d }.sumOf { it.valor }.toFloat()
                        acumuladoReal
                    }
                    val yLimite = (1..daysInMonth).map { d -> (receitaRef.toFloat() / daysInMonth) * d }
                    CartesianChartModel(LineCartesianLayerModel.build { series(yReal); series(yLimite) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(vicoFill(colorDesp))),
                                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(vicoFill(colorRec.copy(0.3f))))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> dinheiro.format(v) }),
                        bottomAxis = HorizontalAxis.rememberBottom(labelRotationDegrees = -45f),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
                ChartLegend(listOf(
                    "Real" to colorDesp,
                    "Limite" to colorRec.copy(0.3f)
                ))
            }
        }

        item {
            ChartCard(title = stringResource(R.string.dica_grafico_pagamentos)) {
                val labelEntradas = stringResource(R.string.linha_receita).lowercase().replaceFirstChar { it.uppercase() }
                val labelSaidas = stringResource(R.string.linha_despesa).lowercase().replaceFirstChar { it.uppercase() }
                val model = remember(allContas) {
                    val recPaga = allContas.filter { it.tipo == tipoReceita && it.pagamento == statusPago }.sumOf { it.valor }.toFloat()
                    val recFalta = allContas.filter { it.tipo == tipoReceita && it.pagamento == statusPendente }.sumOf { it.valor }.toFloat()
                    val despPaga = allContas.filter { it.tipo == tipoDespesa && it.pagamento == statusPago }.sumOf { it.valor }.toFloat()
                    val despFalta = allContas.filter { it.tipo == tipoDespesa && it.pagamento == statusPendente }.sumOf { it.valor }.toFloat()
                    CartesianChartModel(
                        ColumnCartesianLayerModel.build {
                            series(x = listOf(0), y = listOf(recPaga))
                            series(x = listOf(0), y = listOf(recFalta))
                            series(x = listOf(1), y = listOf(despPaga))
                            series(x = listOf(1), y = listOf(despFalta))
                        }
                    )
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(colorRec), thickness = 24.dp, shape = CorneredShape.rounded(allPercent = 20)),
                                rememberLineComponent(fill = vicoFill(colorRecCard), thickness = 24.dp, shape = CorneredShape.rounded(allPercent = 20)),
                                rememberLineComponent(fill = vicoFill(colorDesp), thickness = 24.dp, shape = CorneredShape.rounded(allPercent = 20)),
                                rememberLineComponent(fill = vicoFill(colorDespCard), thickness = 24.dp, shape = CorneredShape.rounded(allPercent = 20))
                            ),
                            mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = CartesianValueFormatter { _, value, _ -> dinheiro.format(value) }),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = CartesianValueFormatter { _, value, _ -> if (value == 0.0) labelEntradas else labelSaidas }
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
                ChartLegend(listOf(
                    "$labelEntradas (Paga)" to colorRec,
                    "$labelEntradas (Pendente)" to colorRecCard,
                    "$labelSaidas (Paga)" to colorDesp,
                    "$labelSaidas (Pendente)" to colorDespCard
                ))
            }
        }

        item {
            ChartCard(title = stringResource(R.string.dica_grafico_receitas)) {
                val model = remember(allContas) {
                    val xValues = (0..2).toList()
                    val yValues = xValues.map { classeId ->
                        allContas.filter { it.tipo == tipoReceita && it.classeConta == classeId }.sumOf { it.valor }.toFloat()
                    }
                    CartesianChartModel(ColumnCartesianLayerModel.build { series(xValues, yValues) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(colorRec), thickness = 16.dp, shape = CorneredShape.rounded(allPercent = 40))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> dinheiro.format(v) }),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, v, _ -> LabelUtils.getClasseLabel(context, tipoReceita, v.toInt()) },
                            labelRotationDegrees = -45f
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.dica_grafico_despesas)) {
                val model = remember(allContas) {
                    val xValues = (0..3).toList()
                    val yValues = xValues.map { classeId ->
                        allContas.filter { it.tipo == tipoDespesa && it.classeConta == classeId }.sumOf { it.valor }.toFloat()
                    }
                    CartesianChartModel(ColumnCartesianLayerModel.build { series(xValues, yValues) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(colorDesp), thickness = 16.dp, shape = CorneredShape.rounded(allPercent = 40))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> dinheiro.format(v) }),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, v, _ -> LabelUtils.getClasseLabel(context, tipoDespesa, v.toInt()) },
                            labelRotationDegrees = -45f
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.pref_titulo_categoria)) {
                val model = remember(allContas) {
                    val xValues = (0..8).toList()
                    val yValues = xValues.map { catId ->
                        allContas.filter { it.tipo == tipoDespesa && it.categoria == catId }.sumOf { it.valor }.toFloat()
                    }
                    CartesianChartModel(ColumnCartesianLayerModel.build { series(xValues, yValues) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(colorDesp), thickness = 16.dp, shape = CorneredShape.rounded(allPercent = 40))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> dinheiro.format(v) }),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, v, _ -> LabelUtils.getCategoriaLabel(context, v.toInt()) },
                            labelRotationDegrees = -45f
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(280.dp)
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.dica_grafico_aplicacoes)) {
                val model = remember(allContas) {
                    val xValues = (0..2).toList()
                    val yValues = xValues.map { classeId ->
                        allContas.filter { it.tipo == tipoAplicacao && it.classeConta == classeId }.sumOf { it.valor }.toFloat()
                    }
                    CartesianChartModel(ColumnCartesianLayerModel.build { series(xValues, yValues) })
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(colorAplic), thickness = 16.dp, shape = CorneredShape.rounded(allPercent = 40))
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> dinheiro.format(v) }),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, v, _ -> LabelUtils.getClasseLabel(context, tipoAplicacao, v.toInt()) },
                            labelRotationDegrees = -45f
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(240.dp)
                )
            }
        }

        item {
            ChartCard(title = stringResource(R.string.grafico_real_limite_media_titulo)) {
                val model = remember(allContas, receitaRef) {
                    val xValues = (0..8).toList()
                    val yReal = xValues.map { catId ->
                        if (catId == 8) {
                            val despesasInvestimento = allContas.filter { it.tipo == tipoDespesa && it.categoria == 8 }
                            val aplicacoes = allContas.filter { it.tipo == tipoAplicacao }
                            val vExtra = despesasInvestimento.filter { desp ->
                                aplicacoes.none { it.nome == desp.nome && it.valor == desp.valor && it.dia == desp.dia }
                            }.sumOf { it.valor }.toFloat()
                            vAplic + vExtra
                        } else {
                            allContas.filter { it.tipo == tipoDespesa && it.categoria == catId }.sumOf { it.valor }.toFloat()
                        }
                    }
                    val yPlanejado = xValues.map { catId ->
                        val perc = prefs.getFloat("plan_perc_$catId", defaultPercentages[catId]?.toFloat() ?: 0f)
                        ((perc / 100.0) * receitaRef).toFloat()
                    }
                    val yMedia = xValues.map { catId ->
                        repository.getMediaCategoriaUltimosMeses(catId, 3).toFloat()
                    }
                    CartesianChartModel(
                        ColumnCartesianLayerModel.build {
                            series(xValues, yPlanejado)
                            series(xValues, yMedia)
                            series(xValues, yReal)
                        }
                    )
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(fill = vicoFill(Color(0xFF2196F3)), thickness = 8.dp),
                                rememberLineComponent(fill = vicoFill(Color(0xFFFFB300)), thickness = 8.dp),
                                rememberLineComponent(fill = vicoFill(colorDesp), thickness = 8.dp)
                            ),
                            mergeMode = { ColumnCartesianLayer.MergeMode.Grouped() }
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = { _, v, _ ->
                                if (v >= 1000) "${(v / 1000).toInt()}$unitK" else v.toInt().toString()
                            }
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, v, _ -> LabelUtils.getCategoriaLabel(context, v.toInt()) },
                            labelRotationDegrees = -45f
                        ),
                        marker = marker
                    ),
                    model = model,
                    modifier = Modifier.height(450.dp)
                )
                ChartLegend(listOf(
                    "Limite" to Color(0xFF2196F3),
                    "Média" to Color(0xFFFFB300),
                    "Real" to colorDesp
                ))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(2.dp),
            color = color
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    val chunked = items.chunked(if (items.size > 3) 2 else items.size)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        chunked.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEachIndexed { index, item ->
                    LegendItem(item.first, item.second)
                    if (index < rowItems.size - 1) {
                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
            if (chunked.size > 1) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    centerText: String,
    labels: List<String>
) {
    val total = values.sum()
    if (total <= 0f) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                var startAngle = -90f
                values.forEachIndexed { index, value ->
                    if (value > 0) {
                        val sweepAngle = (value / total) * 360f
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            Text(
                text = centerText, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold, 
                color = onSurface,
                textAlign = TextAlign.Center
            )
        }
        
        Column(Modifier.padding(start = 16.dp)) {
            values.forEachIndexed { index, value ->
                if (value > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(10.dp), 
                            shape = RoundedCornerShape(2.dp), 
                            color = colors[index]
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${labels[index]}: ${( (value/total)*100 ).toInt()}%", 
                            fontSize = 11.sp, 
                            color = onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
