package com.msk.minhascontas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.features.pdf.ContaImportada
import com.msk.minhascontas.features.pdf.ImportSummary
import com.msk.minhascontas.utils.LabelUtils
import java.text.NumberFormat
import java.util.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportarPreviewScreen(
    summary: ImportSummary,
    onConfirm: (List<ContaImportada>, Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var showReviewOnly by remember { mutableStateOf(false) }
    var generateFutureInstallments by remember { mutableStateOf(false) }
    val editableContas = remember { mutableStateListOf<ContaImportada>().apply { addAll(summary.contas) } }
    
    val currentList = if (showReviewOnly) {
        editableContas.filter { it.needsReview }
    } else {
        editableContas
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.import_ignore_btn))
                    }
                    Button(
                        onClick = { onConfirm(editableContas.toList(), generateFutureInstallments) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.dialog_button))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_confirm_btn))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorResource(R.color.background)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ImportSummaryCard(summary, editableContas.size)
            }

            if (summary.tipoArquivo == "Fatura") {
                item {
                    InstallmentOptionsCard(
                        enabled = generateFutureInstallments,
                        onToggle = { generateFutureInstallments = it }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.nav_contas),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (summary.itensParaRevisao > 0 || editableContas.any { it.needsReview }) {
                        FilterChip(
                            selected = showReviewOnly,
                            onClick = { showReviewOnly = !showReviewOnly },
                            label = { Text(stringResource(R.string.import_review_btn)) },
                            leadingIcon = {
                                if (showReviewOnly) Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                else Icon(Icons.Default.Warning, null, Modifier.size(18.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorResource(R.color.despesa_color).copy(alpha = 0.1f),
                                selectedLabelColor = colorResource(R.color.despesa_color)
                            )
                        )
                    }
                }
            }

            items(currentList) { item ->
                TransactionItemCard(
                    item = item,
                    onUpdate = { updated ->
                        val index = editableContas.indexOf(item)
                        if (index != -1) {
                            editableContas[index] = updated
                        }
                    },
                    onDelete = {
                        editableContas.remove(item)
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ImportSummaryCard(summary: ImportSummary, currentCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.import_summary_card_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val summaryIcon = when {
                    summary.tipoArquivo == "Planilha" -> Icons.Default.TableChart
                    summary.banco.lowercase() == "itau" -> Icons.Default.AccountBalance
                    summary.banco.lowercase() == "bb" -> Icons.Default.AccountBalance
                    summary.banco.lowercase() == "inter" -> Icons.Default.AccountBalance
                    summary.banco.lowercase() == "card" -> Icons.Default.CreditCard
                    else -> Icons.Default.Description
                }
                
                val summaryColor = when {
                    summary.tipoArquivo == "Planilha" -> colorResource(R.color.aplicacao_color)
                    summary.tipoArquivo == "Fatura" -> colorResource(R.color.despesa_color)
                    else -> colorResource(R.color.receita_color)
                }

                Icon(
                    imageVector = summaryIcon,
                    contentDescription = null,
                    tint = summaryColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(summary.banco.uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val typeLabel = when(summary.tipoArquivo) {
                        "Planilha" -> stringResource(R.string.import_type_spreadsheet)
                        "Fatura" -> stringResource(R.string.import_type_fatura)
                        else -> stringResource(R.string.import_type_extrato)
                    }
                    Text(
                        "${stringResource(R.string.import_type_label)} $typeLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryInfoItem(
                    Icons.Default.CalendarToday,
                    stringResource(R.string.import_period_label),
                    formatPeriod(summary.dataInicio, summary.dataFim)
                )
                SummaryInfoItem(
                    Icons.AutoMirrored.Filled.ListAlt,
                    stringResource(R.string.dica_numero),
                    if (currentCount == summary.totalRegistros) currentCount.toString() 
                    else "$currentCount / ${summary.totalRegistros}"
                )
            }
            
            val totalRevisao = summary.contas.count { it.needsReview }
            if (totalRevisao > 0 || summary.totalDuplicados > 0) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (totalRevisao > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorResource(R.color.despesa_color).copy(alpha = 0.1f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = colorResource(R.color.despesa_color), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.import_review_alert, totalRevisao),
                                color = colorResource(R.color.despesa_color),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (summary.totalDuplicados > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorResource(R.color.despesa_color).copy(alpha = 0.1f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CopyAll, null, tint = colorResource(R.color.despesa_color), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.import_duplicate_warning, summary.totalDuplicados),
                                color = colorResource(R.color.despesa_color),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InstallmentOptionsCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.receita_color).copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.import_parcelas_label), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.import_parcelas_desc), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun TransactionItemCard(
    item: ContaImportada,
    onUpdate: (ContaImportada) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val categoryName = LabelUtils.getCategoriaLabel(context, item.conta.categoria)
    val className = LabelUtils.getClasseLabel(context, item.conta.tipo, item.conta.classeConta)
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    
    val typeColor = when (item.conta.tipo) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.aplicacao_color)
        else -> colorResource(R.color.despesa_color)
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.needsReview) 1.dp else 0.dp,
                color = if (item.needsReview) colorResource(R.color.despesa_color).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Data Circle
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item.conta.dia.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(Modifier.weight(1f)) {
                    // Nome editável direto na lista ou via clique
                    Text(
                        text = item.conta.nome,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showEditDialog = true }
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showEditDialog = true }
                    ) {
                        val displayInfo = if (item.conta.tipo == ContasContract.TIPO_DESPESA) {
                            "$className • $categoryName"
                        } else {
                            className
                        }
                        
                        Text(
                            text = displayInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.isParcelada) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = colorResource(R.color.receita_color).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${item.parcelaAtual}/${item.totalParcelas}",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = colorResource(R.color.receita_color),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Text(
                    currencyFormat.format(item.conta.valor),
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = colorResource(R.color.despesa_color).copy(alpha = 0.6f))
                }
            }
            
            AnimatedVisibility(visible = item.needsReview) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.titulo_atencao)} ${item.reviewReason ?: ""}",
                        color = colorResource(R.color.despesa_color),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = stringResource(R.string.import_original_text, item.originalText),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        ImportEditDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedItem ->
                onUpdate(updatedItem)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ImportEditDialog(
    item: ContaImportada,
    onDismiss: () -> Unit,
    onConfirm: (ContaImportada) -> Unit
) {
    var nome by remember { mutableStateOf(item.conta.nome) }
    var tipo by remember { mutableIntStateOf(item.conta.tipo) }
    var categoria by remember { mutableIntStateOf(item.conta.categoria) }
    var classe by remember { mutableIntStateOf(item.conta.classeConta) }

    // Novos estados para Valor e Data
    var valor by remember { mutableStateOf(String.format(Locale.US, "%.2f", item.conta.valor).replace(".", ",")) }
    var dia by remember { mutableStateOf(item.conta.dia.toString()) }
    var mes by remember { mutableStateOf(item.conta.mes.toString()) }
    var ano by remember { mutableStateOf(item.conta.ano.toString()) }

    val context = LocalContext.current
    val categoriasArr = stringArrayResource(R.array.CategoriaConta)

    val classesArr = when (tipo) {
        ContasContract.TIPO_RECEITA -> stringArrayResource(R.array.TipoReceita)
        ContasContract.TIPO_APLICACAO -> stringArrayResource(R.array.TipoAplicacao)
        else -> stringArrayResource(R.array.TipoDespesa)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.titulo_editar)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text(stringResource(R.string.dica_conta)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Edição de Tipo
                Text(stringResource(R.string.titulo_filtro), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tipos = listOf(
                        ContasContract.TIPO_DESPESA to stringResource(R.string.linha_despesa),
                        ContasContract.TIPO_RECEITA to stringResource(R.string.linha_receita),
                        ContasContract.TIPO_APLICACAO to stringResource(R.string.linha_aplicacoes)
                    )
                    tipos.forEach { (t, label) ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = {
                                if (tipo != t) {
                                    tipo = t
                                    // Reset categoria e classe para padrões se mudar o tipo
                                    categoria = if (t == ContasContract.TIPO_DESPESA) ContasContract.CATEGORIA_OUTROS else 0
                                    classe = 0
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Edição de Valor
                OutlinedTextField(
                    value = valor,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^[\d,]*$"""))) valor = it },
                    label = { Text(stringResource(R.string.dica_valor)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(stringResource(R.string.currency_prefix)) }
                )

                // Edição de Data
                Text(stringResource(R.string.import_period_label), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dia,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) dia = it },
                        label = { Text(stringResource(R.string.dica_vencimento)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = mes,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) mes = it },
                        label = { Text(stringResource(R.string.label_mes)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = ano,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) ano = it },
                        label = { Text(stringResource(R.string.label_ano)) },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (tipo == ContasContract.TIPO_DESPESA) {
                    Text(stringResource(R.string.categoria_label), style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriasArr.forEachIndexed { index, label ->
                            // Usamos LabelUtils para pegar o nome personalizado se existir
                            val displayLabel = LabelUtils.getCategoriaLabel(context, index)
                            FilterChip(
                                selected = categoria == index,
                                onClick = { categoria = index },
                                label = { Text(displayLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text(stringResource(R.string.classe), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classesArr.forEachIndexed { index, label ->
                        val displayLabel = LabelUtils.getClasseLabel(context, tipo, index)
                        FilterChip(
                            selected = classe == index,
                            onClick = { classe = index },
                            label = { Text(displayLabel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val valorDouble = valor.replace(",", ".").toDoubleOrNull() ?: item.conta.valor
                val diaInt = dia.toIntOrNull() ?: item.conta.dia
                val mesInt = mes.toIntOrNull() ?: item.conta.mes
                val anoInt = ano.toIntOrNull() ?: item.conta.ano

                val updatedConta = item.conta.copy(
                    nome = nome,
                    tipo = tipo,
                    categoria = categoria,
                    classeConta = classe,
                    valor = valorDouble,
                    dia = diaInt,
                    mes = mesInt,
                    ano = anoInt
                )
                onConfirm(item.copy(conta = updatedConta, needsReview = false))
            }) {
                Text(stringResource(R.string.confirmar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

private fun formatPeriod(start: Calendar?, end: Calendar?): String {
    if ((start == null) || (end == null)) return "--"
    val sdf = java.text.SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return "${sdf.format(start.time)} - ${sdf.format(end.time)}"
}
