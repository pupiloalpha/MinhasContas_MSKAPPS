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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.DBContas.TipoAtualizacao
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
    var showDatePicker by remember { mutableStateOf(false) }

    val primaryColor = when (tipoConta) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.azul)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.verde)
        else -> colorResource(R.color.vermelho)
    }

    val secondaryColor = when (tipoConta) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.aplicacao_color)
        else -> colorResource(R.color.despesa_color)
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_editar)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (viewModel.isRecurring) {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text(stringResource(R.string.dica_conta)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = secondaryColor,
                    focusedLabelColor = secondaryColor,
                    cursorColor = secondaryColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text(stringResource(R.string.dica_valor)) },
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
                    Text(dataFormato.format(dateCalendar.time))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor de Tipo Modernizado
            TipoContaSelector(
                selectedType = tipoConta,
                onTypeSelected = { tipoConta = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Class Selection
            val classesLabels = remember(tipoConta) {
                val count = when (tipoConta) {
                    ContasContract.TIPO_DESPESA -> 4
                    else -> 3
                }
                (0 until count).map { LabelUtils.getClasseLabel(context, tipoConta, it) }
            }
            DropdownField(
                label = stringResource(R.string.dica_spinner),
                options = classesLabels,
                selectedOption = if (classeConta < classesLabels.size) classesLabels[classeConta] else "",
                onOptionSelected = { index, _ -> classeConta = index },
                themeColor = secondaryColor
            )

            if (tipoConta == ContasContract.TIPO_DESPESA) {
                Spacer(modifier = Modifier.height(16.dp))
                val categoriasLabels = stringArrayResource(R.array.CategoriaConta).indices.map { LabelUtils.getCategoriaLabel(context, it) }
                DropdownField(
                    label = stringResource(R.string.titulo_categoria),
                    options = categoriasLabels,
                    selectedOption = if (categoriaConta < categoriasLabels.size) categoriasLabels[categoriaConta] else "",
                    onOptionSelected = { index, _ -> categoriaConta = index },
                    themeColor = secondaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tipoConta != ContasContract.TIPO_APLICACAO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pagamento,
                        onCheckedChange = { pagamento = it },
                        colors = CheckboxDefaults.colors(checkedColor = secondaryColor)
                    )
                    Text(if (tipoConta == ContasContract.TIPO_DESPESA) stringResource(R.string.dica_pagamento) else stringResource(R.string.dica_recebe))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                val repeteOptions = stringArrayResource(R.array.repete_conta).toList()
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
                        selectedOption = currentIntervalText,
                        onOptionSelected = { index, _ ->
                            intervalo = when (index) {
                                0 -> 101
                                1 -> 107
                                2 -> 300
                                3 -> 3650
                                else -> 300
                            }
                        },
                        themeColor = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = qtPrest,
                    onValueChange = { if (it.length <= 3) qtPrest = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.dica_numero)) },
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
                OutlinedTextField(
                    value = valorJuros,
                    onValueChange = { valorJuros = it },
                    label = { Text(stringResource(R.string.dica_valor_juros)) },
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance().apply {
                set(ano, mes - 1, dia)
            }.timeInMillis
        )
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = secondaryColor)) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val cal = Calendar.getInstance().apply { timeInMillis = it }
                            ano = cal.get(Calendar.YEAR)
                            mes = cal.get(Calendar.MONTH) + 1
                            dia = cal.get(Calendar.DAY_OF_MONTH)
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
    onTypeSelected: (Int) -> Unit
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
            val bgColor by animateColorAsState(if (isSelected) color else Color.Transparent, label = "")
            
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
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    themeColor: Color = MaterialTheme.colorScheme.primary
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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
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
