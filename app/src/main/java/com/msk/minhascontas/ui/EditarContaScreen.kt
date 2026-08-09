package com.msk.minhascontas.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.TipoAtualizacao
import com.msk.minhascontas.ui.layouts.StandardTopAppBar
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.EditarContaViewModel
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarContaScreen(
    viewModel: EditarContaViewModel,
    onComplete: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val contaBase = viewModel.conta

    if (contaBase == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // States
    var nome by remember { mutableStateOf(contaBase.nome) }
    var valor by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", contaBase.valor)) }
    var tipoConta by remember { mutableIntStateOf(contaBase.tipo) }
    var classeConta by remember { mutableIntStateOf(contaBase.classeConta) }
    var categoriaConta by remember { mutableIntStateOf(contaBase.categoria) }
    var dia by remember { mutableIntStateOf(contaBase.dia) }
    var mes by remember { mutableIntStateOf(contaBase.mes) }
    var ano by remember { mutableIntStateOf(contaBase.ano) }
    var pagamento by remember { mutableStateOf(contaBase.pagamento == ContasContract.STATUS_PAGO_RECEBIDO) }
    var qtPrest by remember { mutableStateOf(contaBase.qtRepete.toString()) }
    var intervalo by remember { mutableIntStateOf(contaBase.intervalo) }
    var valorJuros by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", contaBase.valorJuros * 100)) }

    var showScopeDialog by remember { mutableStateOf(false) }
    var showBulkConfirmDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isBulk = viewModel.isBulkEdit
    
    val primaryColor = if (isBulk) {
        MaterialTheme.colorScheme.primary
    } else {
        when (tipoConta) {
            ContasContract.TIPO_RECEITA -> colorResource(R.color.azul)
            ContasContract.TIPO_APLICACAO -> colorResource(R.color.verde)
            else -> colorResource(R.color.vermelho)
        }
    }

    val secondaryColor = if (isBulk) {
        MaterialTheme.colorScheme.primary
    } else {
        when (tipoConta) {
            ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
            ContasContract.TIPO_APLICACAO -> colorResource(R.color.aplicacao_color)
            else -> colorResource(R.color.despesa_color)
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            StandardTopAppBar(
                title = stringResource(if (isBulk) R.string.confirmacao_edicao_massa_titulo else R.string.titulo_editar),
                onBackClick = onCancel,
                containerColor = primaryColor,
                actions = {
                    IconButton(onClick = {
                        if (isBulk) {
                            if (viewModel.modifiedFields.isNotEmpty()) {
                                showBulkConfirmDialog = true
                            } else {
                                onCancel()
                            }
                        } else if (viewModel.isRecurring) {
                            showScopeDialog = true
                        } else {
                            val updated = contaBase.copy(
                                nome = nome,
                                valor = valor.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                tipo = tipoConta,
                                classeConta = classeConta,
                                categoria = categoriaConta,
                                dia = dia,
                                mes = mes,
                                ano = ano,
                                pagamento = if (pagamento) ContasContract.STATUS_PAGO_RECEBIDO else ContasContract.STATUS_PENDENTE,
                                qtRepete = qtPrest.toIntOrNull() ?: 1,
                                intervalo = intervalo,
                                valorJuros = (valorJuros.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100.0
                            )
                            viewModel.updateConta(updated, TipoAtualizacao.SOMENTE_ESTA)
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val isNomeDivergent = isBulk && viewModel.divergentFields.contains("nome") && !viewModel.modifiedFields.contains("nome")
            OutlinedTextField(
                value = if (isNomeDivergent) "" else nome,
                onValueChange = { 
                    nome = it
                    viewModel.markFieldAsModified("nome")
                },
                label = { Text(stringResource(R.string.dica_conta)) },
                placeholder = { if (isNomeDivergent) Text(stringResource(R.string.valores_diversos), color = Color.Gray, fontStyle = FontStyle.Italic) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = secondaryColor,
                    focusedLabelColor = secondaryColor,
                    cursorColor = secondaryColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                val isValorDivergent = isBulk && viewModel.divergentFields.contains("valor") && !viewModel.modifiedFields.contains("valor")
                OutlinedTextField(
                    value = if (isValorDivergent) "" else valor,
                    onValueChange = { 
                        valor = it
                        viewModel.markFieldAsModified("valor")
                    },
                    label = { Text(stringResource(R.string.dica_valor)) },
                    placeholder = { if (isValorDivergent) Text(stringResource(R.string.valores_diversos), color = Color.Gray, fontStyle = FontStyle.Italic) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                val dateCalendar = Calendar.getInstance().apply { set(ano, mes - 1, dia) }
                val configuration = LocalConfiguration.current
                val currentLocale = configuration.locales.get(0)
                val dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, currentLocale)
                val isDataDivergent = isBulk && viewModel.divergentFields.contains("data") && !viewModel.modifiedFields.contains("data")

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = secondaryColor
                    ),
                    border = BorderStroke(1.dp, secondaryColor)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isDataDivergent) stringResource(R.string.valores_diversos) else dataFormato.format(dateCalendar.time))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor de Tipo Modernizado
            val isTipoDivergent = isBulk && viewModel.divergentFields.contains("tipo") && !viewModel.modifiedFields.contains("tipo")
            TipoContaSelector(
                selectedType = if (isTipoDivergent) -1 else tipoConta,
                onTypeSelected = { 
                    tipoConta = it
                    viewModel.markFieldAsModified("tipo")
                },
                isDivergent = isTipoDivergent,
                neutralColor = secondaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Class Selection
            val classesLabels = remember(tipoConta) {
                if (tipoConta == -1) return@remember emptyList<String>()
                val count = when (tipoConta) {
                    ContasContract.TIPO_DESPESA -> 4
                    ContasContract.TIPO_APLICACAO -> 4
                    else -> 3
                }
                (0 until count).map { LabelUtils.getClasseLabel(context, tipoConta, it) }
            }
            
            val isClasseDivergent = isBulk && viewModel.divergentFields.contains("classe") && !viewModel.modifiedFields.contains("classe")
            
            DropdownField(
                label = stringResource(R.string.dica_spinner),
                options = classesLabels,
                selectedOption = if (isClasseDivergent) stringResource(R.string.valores_diversos) else if (classeConta < classesLabels.size) classesLabels[classeConta] else "",
                onOptionSelected = { index, _ -> 
                    classeConta = index
                    viewModel.markFieldAsModified("classe")
                },
                themeColor = secondaryColor,
                isDivergent = isClasseDivergent
            )

            if (tipoConta == ContasContract.TIPO_DESPESA) {
                Spacer(modifier = Modifier.height(16.dp))
                val categoriasLabels = stringArrayResource(R.array.CategoriaConta).indices.map { LabelUtils.getCategoriaLabel(context, it) }
                val isCategoriaDivergent = isBulk && viewModel.divergentFields.contains("categoria") && !viewModel.modifiedFields.contains("categoria")
                DropdownField(
                    label = stringResource(R.string.titulo_categoria),
                    options = categoriasLabels,
                    selectedOption = if (isCategoriaDivergent) stringResource(R.string.valores_diversos) else if (categoriaConta < categoriasLabels.size) categoriasLabels[categoriaConta] else "",
                    onOptionSelected = { index, _ -> 
                        categoriaConta = index
                        viewModel.markFieldAsModified("categoria")
                    },
                    themeColor = secondaryColor,
                    isDivergent = isCategoriaDivergent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tipoConta != ContasContract.TIPO_APLICACAO && tipoConta != -1) {
                val isPagamentoDivergent = isBulk && viewModel.divergentFields.contains("pagamento") && !viewModel.modifiedFields.contains("pagamento")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(
                        state = if (isPagamentoDivergent) androidx.compose.ui.state.ToggleableState.Indeterminate else if (pagamento) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off,
                        onClick = { 
                            pagamento = !pagamento
                            viewModel.markFieldAsModified("pagamento")
                        },
                        colors = CheckboxDefaults.colors(checkedColor = secondaryColor)
                    )
                    Text(
                        text = if (isPagamentoDivergent) stringResource(R.string.valores_diversos) 
                               else if (tipoConta == ContasContract.TIPO_DESPESA) stringResource(R.string.dica_pagamento) 
                               else stringResource(R.string.dica_recebe),
                        fontStyle = if (isPagamentoDivergent) FontStyle.Italic else FontStyle.Normal,
                        color = if (isPagamentoDivergent) Color.Gray else Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                val repeteOptions = stringArrayResource(R.array.repete_conta).toList()
                val isIntervaloDivergent = isBulk && viewModel.divergentFields.contains("intervalo") && !viewModel.modifiedFields.contains("intervalo")
                val currentIntervalText = when (intervalo) {
                    101 -> repeteOptions.getOrNull(0) ?: ""
                    107 -> repeteOptions.getOrNull(1) ?: ""
                    300 -> repeteOptions.getOrNull(2) ?: ""
                    3650 -> repeteOptions.getOrNull(3) ?: ""
                    else -> ""
                }
                Box(modifier = Modifier.weight(2f)) {
                    DropdownField(
                        label = stringResource(R.string.dica_repete),
                        options = repeteOptions,
                        selectedOption = if (isIntervaloDivergent) stringResource(R.string.valores_diversos) else currentIntervalText,
                        onOptionSelected = { index, _ ->
                            intervalo = when (index) {
                                0 -> 101
                                1 -> 107
                                2 -> 300
                                3 -> 3650
                                else -> 300
                            }
                            viewModel.markFieldAsModified("intervalo")
                        },
                        themeColor = secondaryColor,
                        isDivergent = isIntervaloDivergent
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val isQtRepeteDivergent = isBulk && viewModel.divergentFields.contains("qtRepete") && !viewModel.modifiedFields.contains("qtRepete")
                OutlinedTextField(
                    value = if (isQtRepeteDivergent) "" else qtPrest,
                    onValueChange = { 
                        if (it.length <= 3) {
                            qtPrest = it.filter { c -> c.isDigit() }
                            viewModel.markFieldAsModified("qtRepete")
                        }
                    },
                    label = { Text(stringResource(R.string.dica_numero)) },
                    placeholder = { if (isQtRepeteDivergent) Text(stringResource(R.string.valores_diversos), color = Color.Gray, fontStyle = FontStyle.Italic) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )
            }

            if (tipoConta != ContasContract.TIPO_RECEITA) {
                Spacer(modifier = Modifier.height(16.dp))
                val isJurosDivergent = isBulk && viewModel.divergentFields.contains("juros") && !viewModel.modifiedFields.contains("juros")
                OutlinedTextField(
                    value = if (isJurosDivergent) "" else valorJuros,
                    onValueChange = { 
                        valorJuros = it
                        viewModel.markFieldAsModified("juros")
                    },
                    label = { Text(stringResource(R.string.dica_valor_juros)) },
                    placeholder = { if (isJurosDivergent) Text(stringResource(R.string.valores_diversos), color = Color.Gray, fontStyle = FontStyle.Italic) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )
            }
        }
    }

    if (showScopeDialog) {
        MinhasContasDialogTheme {
            AlertDialog(
                onDismissRequest = { showScopeDialog = false },
                title = { Text(stringResource(R.string.dica_menu_edicao)) },
                text = {
                    Column {
                        stringArrayResource(R.array.TipoAjusteConta).forEachIndexed { index, s ->
                            TextButton(
                                onClick = {
                                    showScopeDialog = false
                                    val updated = contaBase.copy(
                                        nome = nome,
                                        valor = valor.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                        tipo = tipoConta,
                                        classeConta = classeConta,
                                        categoria = categoriaConta,
                                        dia = dia,
                                        mes = mes,
                                        ano = ano,
                                        pagamento = if (pagamento) ContasContract.STATUS_PAGO_RECEBIDO else ContasContract.STATUS_PENDENTE,
                                        qtRepete = qtPrest.toIntOrNull() ?: 1,
                                        intervalo = intervalo,
                                        valorJuros = (valorJuros.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100.0
                                    )

                                    val tipo = when (index) {
                                        0 -> TipoAtualizacao.SOMENTE_ESTA
                                        1 -> TipoAtualizacao.DESTA_EM_DIANTE
                                        2 -> TipoAtualizacao.TODAS_AS_REPETICOES
                                        else -> TipoAtualizacao.SOMENTE_ESTA
                                    }

                                    if (tipo == TipoAtualizacao.SOMENTE_ESTA) {
                                        updated.qtRepete = 1
                                        updated.intervalo = 0
                                    }

                                    viewModel.updateConta(updated, tipo)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = secondaryColor)
                            ) {
                                Text(s, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }

    if (showBulkConfirmDialog) {
        MinhasContasDialogTheme {
            AlertDialog(
                onDismissRequest = { showBulkConfirmDialog = false },
                title = { Text(stringResource(R.string.confirmacao_edicao_massa_titulo)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(
                                R.string.confirmacao_edicao_massa_mensagem,
                                viewModel.modifiedFields.size,
                                viewModel.selectedIds.size
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.campos_alterados), style = MaterialTheme.typography.labelLarge)
                        viewModel.modifiedFields.forEach { field ->
                            val label = when(field) {
                                "nome" -> stringResource(R.string.dica_conta)
                                "valor" -> stringResource(R.string.dica_valor)
                                "tipo" -> stringResource(R.string.dica_spinner)
                                "classe" -> stringResource(R.string.classe)
                                "categoria" -> stringResource(R.string.categoria_label)
                                "data" -> stringResource(R.string.resumo_data, "", "", "").replace("//", "") // Fallback
                                "pagamento" -> stringResource(R.string.dica_pagamento)
                                "intervalo" -> stringResource(R.string.dica_repete)
                                "qtRepete" -> stringResource(R.string.dica_numero)
                                "juros" -> stringResource(R.string.dica_valor_juros)
                                else -> field
                            }
                            Text("• $label", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBulkConfirmDialog = false
                        val updated = contaBase.copy(
                            nome = nome,
                            valor = valor.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            tipo = tipoConta,
                            classeConta = classeConta,
                            categoria = categoriaConta,
                            dia = dia,
                            mes = mes,
                            ano = ano,
                            pagamento = if (pagamento) ContasContract.STATUS_PAGO_RECEBIDO else ContasContract.STATUS_PENDENTE,
                            qtRepete = qtPrest.toIntOrNull() ?: 1,
                            intervalo = intervalo,
                            valorJuros = (valorJuros.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100.0
                        )
                        viewModel.updateConta(updated, TipoAtualizacao.SOMENTE_ESTA)
                    }) {
                        Text(stringResource(R.string.sim))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkConfirmDialog = false }) {
                        Text(stringResource(R.string.nao))
                    }
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(ano, mes - 1, dia)
            }.timeInMillis
        )
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = secondaryColor)) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                            ano = cal.get(Calendar.YEAR)
                            mes = cal.get(Calendar.MONTH) + 1
                            dia = cal.get(Calendar.DAY_OF_MONTH)
                            viewModel.markFieldAsModified("data")
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = secondaryColor)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onTaskComplete.collect { success ->
            onComplete(success)
        }
    }
}


@Composable
private fun TipoContaSelector(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit,
    isDivergent: Boolean = false,
    neutralColor: Color = MaterialTheme.colorScheme.primary
) {
    val types = listOf(
        Triple(ContasContract.TIPO_RECEITA, stringResource(R.string.dica_receita), colorResource(R.color.azul)),
        Triple(ContasContract.TIPO_DESPESA, stringResource(R.string.dica_despesa), colorResource(R.color.vermelho)),
        Triple(ContasContract.TIPO_APLICACAO, stringResource(R.string.dica_aplicacao), colorResource(R.color.verde))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { (type, label, color) ->
            val isSelected = selectedType == type
            val bgColor by animateColorAsState(
                if (isSelected) (if (selectedType == -1) neutralColor else color) else Color.Transparent, 
                label = ""
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgColor)
                    .clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDivergent && selectedType == -1) stringResource(R.string.valores_diversos) else label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = if (isDivergent && selectedType == -1) FontStyle.Italic else FontStyle.Normal
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (Int, String) -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    isDivergent: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                focusedLabelColor = themeColor,
                cursorColor = themeColor
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontStyle = if (isDivergent && !selectedOption.isEmpty() && selectedOption == stringResource(R.string.valores_diversos)) FontStyle.Italic else FontStyle.Normal,
                color = if (isDivergent && selectedOption == stringResource(R.string.valores_diversos)) Color.Gray else Color.Unspecified
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(index, option)
                        expanded = false
                    }
                )
            }
        }
    }
}
