package com.msk.minhascontas.features.planos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.LabelUtils
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// State class for each category
data class CategoriaPlanejamentoState(
    val index: Int,
    val displayName: String,
    val porcentagem: Double,
    val sugeridoPerc: Double,
    val colorRes: Int,
    val onColorRes: Int,
    val isLocked: Boolean = false
)

class PlanejamentoViewModel : ViewModel() {
    var receitaReferencia by mutableDoubleStateOf(0.0)
    val categorias = mutableStateListOf<CategoriaPlanejamentoState>()

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance()
    val currencySymbol: String = currencyFormat.currency?.symbol ?: ""

    private val defaultPercentages = mapOf(
        0 to 15.0, // Alimentação
        1 to 10.0, // Educação
        2 to 10.0, // Lazer
        3 to 25.0, // Moradia
        4 to 5.0,  // Saúde
        5 to 5.0,  // Transporte
        6 to 5.0,  // Vestuário
        7 to 5.0,  // Outros
        8 to 20.0  // Investimentos/Dívidas
    )

    private val categoriaCoresRes = listOf(
        R.color.cat_alimentacao_container,
        R.color.cat_educacao_container,
        R.color.cat_lazer_container,
        R.color.cat_moradia_container,
        R.color.cat_saude_container,
        R.color.cat_transporte_container,
        R.color.cat_vestuario_container,
        R.color.cat_outros_container,
        R.color.cat_invest_dividas_container
    )

    private val categoriaOnCoresRes = listOf(
        R.color.cat_alimentacao_on_container,
        R.color.cat_educacao_on_container,
        R.color.cat_lazer_on_container,
        R.color.cat_moradia_on_container,
        R.color.cat_saude_on_container,
        R.color.cat_transporte_on_container,
        R.color.cat_vestuario_on_container,
        R.color.cat_outros_on_container,
        R.color.cat_invest_dividas_on_container
    )

    fun loadData(context: Context) {
        if (categorias.isNotEmpty()) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        receitaReferencia = prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()
        val numCategorias = context.resources.getStringArray(R.array.CategoriaConta).size
        categorias.clear()
        for (i in 0 until numCategorias) {
            val savedPerc = prefs.getFloat("plan_perc_$i", -1.0f).toDouble()
            val perc = if (savedPerc >= 0) savedPerc else defaultPercentages[i] ?: 0.0
            val isLocked = prefs.getBoolean("plan_locked_$i", false)
            categorias.add(
                CategoriaPlanejamentoState(
                    index = i,
                    displayName = LabelUtils.getCategoriaLabel(context, i),
                    porcentagem = perc,
                    sugeridoPerc = defaultPercentages[i] ?: 0.0,
                    colorRes = categoriaCoresRes.getOrElse(i) { R.color.cinza },
                    onColorRes = categoriaOnCoresRes.getOrElse(i) { R.color.on_surface },
                    isLocked = isLocked
                )
            )
        }
    }

    fun saveData(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            putFloat("plan_receita_referencia", receitaReferencia.toFloat())
            categorias.forEach {
                putFloat("plan_perc_${it.index}", it.porcentagem.toFloat())
                putBoolean("plan_locked_${it.index}", it.isLocked)
            }
            apply()
        }
    }

    fun updateReceita(newValue: Double) { receitaReferencia = newValue }

    fun toggleLock(index: Int) {
        categorias[index] = categorias[index].copy(isLocked = !categorias[index].isLocked)
    }

    fun resetToDefaults() {
        for (i in categorias.indices) {
            categorias[i] = categorias[i].copy(porcentagem = defaultPercentages[i] ?: 0.0, isLocked = false)
        }
    }

    fun adjustOthers(index: Int, newPercentage: Double) {
        val catToAdjust = categorias[index]
        if (catToAdjust.isLocked) return

        val otherUnlockedIndices = categorias.indices.filter { it != index && !categorias[it].isLocked }
        val sumLocked = categorias.filter { it.isLocked }.sumOf { it.porcentagem }

        // O valor máximo permitido para uma categoria desbloqueada é 100% menos a soma das travadas
        val maxAvailable = (100.0 - sumLocked).coerceAtLeast(0.0)
        val safeNewPerc = newPercentage.coerceIn(0.0, maxAvailable)

        val oldPerc = catToAdjust.porcentagem
        val diff = safeNewPerc - oldPerc

        categorias[index] = catToAdjust.copy(porcentagem = safeNewPerc)

        if (otherUnlockedIndices.isNotEmpty()) {
            val sumOtherUnlocked = otherUnlockedIndices.sumOf { categorias[it].porcentagem }
            if (sumOtherUnlocked > 0.0) {
                // Redistribuição proporcional entre as outras categorias desbloqueadas
                for (i in otherUnlockedIndices) {
                    val cat = categorias[i]
                    val ratio = cat.porcentagem / sumOtherUnlocked
                    categorias[i] = cat.copy(porcentagem = (cat.porcentagem - (diff * ratio)).coerceIn(0.0, 100.0))
                }
            } else {
                // Se todas as outras estavam em zero, distribui o espaço restante igualmente
                val remainingSpace = (100.0 - sumLocked - safeNewPerc).coerceAtLeast(0.0)
                val equalShare = remainingSpace / otherUnlockedIndices.size
                for (i in otherUnlockedIndices) {
                    categorias[i] = categorias[i].copy(porcentagem = equalShare)
                }
            }

            // Ajuste fino final para garantir que a soma seja exatamente 100.0 (corrige arredondamentos)
            val currentSum = categorias.sumOf { it.porcentagem }
            if (abs(currentSum - 100.0) > 0.001) {
                val lastIdx = otherUnlockedIndices.last()
                val correction = 100.0 - currentSum
                categorias[lastIdx] = categorias[lastIdx].copy(
                    porcentagem = (categorias[lastIdx].porcentagem + correction).coerceIn(0.0, 100.0)
                )
            }
        }
    }

    fun formatCurrency(value: Double): String = currencyFormat.format(value)
}

class PlanejamentoFinanceiro : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: PlanejamentoViewModel = viewModel()
            LaunchedEffect(Unit) { viewModel.loadData(this@PlanejamentoFinanceiro) }
            MinhasContasTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PlanejamentoScreen(viewModel = viewModel, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanejamentoScreen(viewModel: PlanejamentoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var receitaReferenciaInput by remember(viewModel.receitaReferencia) {
        mutableStateOf(String.format(Locale.getDefault(), "%.2f", viewModel.receitaReferencia))
    }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.confirmar_reverter_titulo)) },
            text = { Text(stringResource(R.string.confirmar_reverter_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefaults()
                    showResetDialog = false
                    Toast.makeText(context, R.string.revertido_sucesso, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.sim)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.nao)) }
            }
        )
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_planejamento_financeiro), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveData(context)
                        Toast.makeText(context, R.string.ajustes_salvos, Toast.LENGTH_SHORT).show()
                        onBack()
                    }) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            item {
                OutlinedTextField(
                    value = receitaReferenciaInput,
                    onValueChange = {
                        receitaReferenciaInput = it
                        it.replace(",", ".").toDoubleOrNull()?.let { valDouble -> viewModel.updateReceita(valDouble) }
                    },
                    label = { Text(stringResource(R.string.receita_referencia)) },
                    prefix = { Text("${viewModel.currencySymbol} ") },
                    supportingText = { Text(stringResource(R.string.ajuda_planejamento)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.total_planejado_color),
                        focusedLabelColor = colorResource(R.color.total_planejado_color),
                        cursorColor = colorResource(R.color.total_planejado_color)
                    )
                )
            }
            items(viewModel.categorias, key = { it.index }) { CategoriaCard(it, viewModel) }
            item {
                val total = viewModel.categorias.sumOf { (it.porcentagem / 100.0) * viewModel.receitaReferencia }
                val isBalanced = Math.abs(total - viewModel.receitaReferencia) < 0.1
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.total_planejado),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorResource(R.color.total_planejado_on_container)
                        )
                        val balanceColor = if (isBalanced) {
                            colorResource(R.color.total_planejado_on_container)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = viewModel.formatCurrency(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                        if (!isBalanced) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.erro_soma_planejamento),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(MaterialTheme.colorScheme.secondary))
                ) { Text(stringResource(R.string.reverter_padrao).uppercase()) }
            }
        }
    }
}

@Composable
fun CategoriaCard(cat: CategoriaPlanejamentoState, viewModel: PlanejamentoViewModel) {
    val valor = (cat.porcentagem / 100.0) * viewModel.receitaReferencia
    val sugeridoValor = (cat.sugeridoPerc / 100.0) * viewModel.receitaReferencia
    val catColor = colorResource(cat.colorRes)
    val onCatColor = colorResource(cat.onColorRes)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(catColor).padding(vertical = 2.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = cat.displayName, color = onCatColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.travar), color = onCatColor, style = MaterialTheme.typography.labelSmall)
                    Checkbox(
                        checked = cat.isLocked,
                        onCheckedChange = { viewModel.toggleLock(cat.index) },
                        colors = CheckboxDefaults.colors(checkedColor = onCatColor, uncheckedColor = onCatColor, checkmarkColor = catColor)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = stringResource(R.string.valor_referencia, viewModel.formatCurrency(sugeridoValor), cat.sugeridoPerc.toInt()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var percText by remember(cat.porcentagem) { mutableStateOf(String.format(Locale.getDefault(), "%.1f", cat.porcentagem)) }
                    var valorText by remember(valor) { mutableStateOf(String.format(Locale.getDefault(), "%.2f", valor)) }
                    OutlinedTextField(value = percText, onValueChange = {
                        percText = it
                        it.replace(",", ".").toDoubleOrNull()?.let { newPerc -> viewModel.adjustOthers(cat.index, newPerc) }
                    }, label = { Text("%", fontSize = 12.sp) }, modifier = Modifier.weight(1f),
                        enabled = !cat.isLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = onCatColor, focusedLabelColor = onCatColor))
                    OutlinedTextField(value = valorText, onValueChange = {
                        valorText = it
                        it.replace(",", ".").toDoubleOrNull()?.let { newVal ->
                            if (viewModel.receitaReferencia > 0) viewModel.adjustOthers(cat.index, (newVal / viewModel.receitaReferencia) * 100.0)
                        }
                    }, label = { Text(stringResource(R.string.valor), fontSize = 12.sp) }, prefix = { Text(viewModel.currencySymbol, fontSize = 12.sp) },
                        modifier = Modifier.weight(1.6f), enabled = !cat.isLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = onCatColor, focusedLabelColor = onCatColor, cursorColor = onCatColor))
                }
                Slider(
                    value = cat.porcentagem.toFloat(),
                    onValueChange = { viewModel.adjustOthers(cat.index, it.toDouble()) },
                    valueRange = 0f..100f,
                    enabled = !cat.isLocked,
                    colors = SliderDefaults.colors(thumbColor = onCatColor, activeTrackColor = onCatColor, disabledThumbColor = onCatColor.copy(alpha = 0.5f))
                )
            }
        }
    }
}
