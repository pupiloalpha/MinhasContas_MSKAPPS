package com.msk.minhascontas.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme
import com.msk.minhascontas.utils.AlertaCalendario
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.CriarContaUiState
import com.msk.minhascontas.viewmodel.CriarContaViewModel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarContaScreen(
    initialMes: Int,
    initialAno: Int,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CriarContaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val primaryColor = when (uiState.tipo) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.azul)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.verde)
        else -> colorResource(R.color.vermelho)
    }

    val secondaryColor = when (uiState.tipo) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.aplicacao_color)
        else -> colorResource(R.color.despesa_color)
    }

    LaunchedEffect(initialMes, initialAno) {
        viewModel.initData(initialMes, initialAno, context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            adicionarLembrete(context, uiState)
            onSuccess()
        } else {
            Toast.makeText(context, "Permissão negada. Lembrete não configurado.", Toast.LENGTH_LONG).show()
            onSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_criar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.salvar(context) {
                            if (uiState.lembrete) {
                                val permEscrever = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
                                val permLer = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                                if (permEscrever == PackageManager.PERMISSION_GRANTED && permLer == PackageManager.PERMISSION_GRANTED) {
                                    adicionarLembrete(context, uiState)
                                    onSuccess()
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                                }
                            } else {
                                onSuccess()
                            }
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
                .verticalScroll(scrollState)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AutocompleteTextField(
                value = uiState.nome,
                onValueChange = { viewModel.updateNome(it) },
                suggestions = uiState.sugestoes,
                onSuggestionSelected = { viewModel.selecionarSugestao(it) },
                label = stringResource(R.string.dica_conta),
                modifier = Modifier.fillMaxWidth(),
                themeColor = secondaryColor
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.valor,
                    onValueChange = { viewModel.updateValor(it) },
                    label = { Text(stringResource(R.string.dica_valor)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )

                var showDatePicker by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = secondaryColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    val date = Calendar.getInstance().apply {
                        set(Calendar.YEAR, uiState.ano)
                        set(Calendar.MONTH, uiState.mes - 1)
                        set(Calendar.DAY_OF_MONTH, uiState.dia)
                    }
                    Text(DateFormat.getDateInstance(DateFormat.SHORT).format(date.time))
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            clear()
                            set(uiState.ano, uiState.mes - 1, uiState.dia)
                        }.timeInMillis
                    )
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = secondaryColor)) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                                        viewModel.updateData(
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH) + 1,
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        )
                                    }
                                    showDatePicker = false
                                }) { Text(stringResource(R.string.ok)) }
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
            }

            // Tipo de Conta (Modernizado)
            TipoContaSelector(
                selectedType = uiState.tipo,
                onTypeSelected = { viewModel.updateTipo(it) }
            )

            LabelDropdown(
                label = stringResource(R.string.dica_spinner),
                options = getClasseOptions(context, uiState.tipo),
                selectedOption = uiState.classe,
                onOptionSelected = { viewModel.updateClasse(it) },
                themeColor = secondaryColor
            )

            if (uiState.tipo == ContasContract.TIPO_DESPESA) {
                val categoriasLabels = stringArrayResource(R.array.CategoriaConta).toList()
                LabelDropdown(
                    label = stringResource(R.string.titulo_categoria),
                    options = categoriasLabels.indices.map { LabelUtils.getCategoriaLabel(context, it) },
                    selectedOption = uiState.categoria,
                    onOptionSelected = { viewModel.updateCategoria(it) },
                    themeColor = secondaryColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.paga,
                    onCheckedChange = { viewModel.updatePaga(it) },
                    colors = CheckboxDefaults.colors(checkedColor = secondaryColor)
                )
                Text(if (uiState.tipo == ContasContract.TIPO_RECEITA) stringResource(R.string.dica_recebe) else stringResource(R.string.dica_pagamento))
                
                if (uiState.tipo == ContasContract.TIPO_DESPESA && (uiState.classe == ContasContract.CLASSE_DESPESA_CARTAO || uiState.classe == ContasContract.CLASSE_DESPESA_PRESTACOES)) {
                    Spacer(Modifier.width(16.dp))
                    Checkbox(
                        checked = uiState.parcelar,
                        onCheckedChange = { viewModel.updateParcelar(it) },
                        colors = CheckboxDefaults.colors(checkedColor = secondaryColor)
                    )
                    Text(stringResource(R.string.dica_valor_total))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelDropdown(
                        label = stringResource(R.string.dica_repete),
                        options = stringArrayResource(R.array.repete_conta).toList(),
                        selectedOption = uiState.intervaloPosicao,
                        onOptionSelected = { viewModel.updateIntervalo(it) },
                        modifier = Modifier.weight(2f),
                        themeColor = secondaryColor
                    )

                    OutlinedTextField(
                        value = uiState.qtRepete,
                        onValueChange = { viewModel.updateQtRepete(it) },
                        label = { Text(stringResource(R.string.dica_numero)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = secondaryColor,
                            focusedLabelColor = secondaryColor,
                            cursorColor = secondaryColor
                        )
                    )
                }
            }

            val dataFinal = remember(uiState.dia, uiState.mes, uiState.ano, uiState.qtRepete, uiState.intervaloPosicao) {
                val qtExtra = uiState.qtRepete.toIntOrNull() ?: 0
                if (qtExtra <= 0) return@remember null

                val totalQt = qtExtra + 1
                val cal = Calendar.getInstance().apply { set(uiState.ano, uiState.mes - 1, uiState.dia) }
                for (i in 2..totalQt) {
                    when (uiState.intervaloPosicao) {
                        0 -> cal.add(Calendar.DATE, 1)
                        1 -> cal.add(Calendar.DATE, 7)
                        2 -> cal.add(Calendar.MONTH, 1)
                        3 -> cal.add(Calendar.YEAR, 1)
                    }
                }
                DateFormat.getDateInstance(DateFormat.SHORT).format(cal.time)
            }

            if (dataFinal != null) {
                Text(
                    text = stringResource(R.string.ate_data_feedback, dataFinal),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryColor,
                    modifier = Modifier.padding(start = 8.dp).align(Alignment.End)
                )
            }

            if (uiState.intervaloPosicao == 2) { // Mensal
                val infoAteDezembro = remember(uiState.dia, uiState.mes, uiState.ano) {
                    val mesesRestantes = 12 - uiState.mes + 1
                    val cal = Calendar.getInstance().apply { set(uiState.ano, uiState.mes - 1, uiState.dia) }
                    if (mesesRestantes > 1) {
                        cal.add(Calendar.MONTH, mesesRestantes - 1)
                        val dataFormatada = DateFormat.getDateInstance(DateFormat.SHORT).format(cal.time)
                        Pair(mesesRestantes - 1, dataFormatada)
                    } else null
                }

                TextButton(
                    onClick = { viewModel.repetirAteFimDoAno() },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors(contentColor = secondaryColor)
                ) {
                    val suffix = infoAteDezembro?.let { (qtd, data) ->
                        stringResource(R.string.mais_vezes_ate, qtd, data)
                    } ?: ""
                    Text(
                        text = "${stringResource(R.string.repetir_ate_fim_ano)} $suffix",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (uiState.tipo != ContasContract.TIPO_RECEITA) {
                OutlinedTextField(
                    value = uiState.juros,
                    onValueChange = { viewModel.updateJuros(it) },
                    label = { Text(stringResource(R.string.dica_valor_juros)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.lembrete,
                    onCheckedChange = { viewModel.updateLembrete(it) },
                    colors = CheckboxDefaults.colors(checkedColor = secondaryColor)
                )
                Text(stringResource(R.string.titulo_calendario))
            }
        }
    }

    if (uiState.isLoading) {
        MinhasContasDialogTheme {
            Dialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { uiState.progress },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.mensagem_salvando_contas))
                    }
                }
            }
        }
    }

    if (uiState.showAplicacaoDialog) {
        MinhasContasDialogTheme {
            AlertDialog(
                onDismissRequest = { viewModel.setShowAplicacaoDialog(false) },
                title = { Text(stringResource(R.string.titulo_despesa_saque)) },
                text = { Text(stringResource(R.string.texto_despesa_saque)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmarAplicacao(context, onSuccess) }
                    ) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.salvar(context, true, onSuccess) }
                    ) { Text(stringResource(R.string.cancelar)) }
                }
            )
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
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<Conta>,
    onSuggestionSelected: (Conta) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.length < 2) emptyList()
        else suggestions.filter { it.nome.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                focusedLabelColor = themeColor,
                cursorColor = themeColor
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                filteredSuggestions.take(10).forEach { conta ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(conta.nome, style = MaterialTheme.typography.bodyLarge)
                                // Opcional: mostrar categoria para ajudar a diferenciar
                                val context = LocalContext.current
                                val info = when (conta.tipo) {
                                    ContasContract.TIPO_DESPESA -> LabelUtils.getCategoriaLabel(context, conta.categoria)
                                    else -> LabelUtils.getClasseLabel(context, conta.tipo, conta.classeConta)
                                }
                                Text(info, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        },
                        onClick = {
                            onSuggestionSelected(conta)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDropdown(
    label: String,
    options: List<String>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedOption) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                focusedLabelColor = themeColor,
                cursorColor = themeColor
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun getClasseOptions(context: Context, tipo: Int): List<String> {
    val size = when (tipo) {
        ContasContract.TIPO_DESPESA -> 4
        ContasContract.TIPO_RECEITA -> 4
        ContasContract.TIPO_APLICACAO -> 4
        else -> 3
    }
    return (0 until size).map { LabelUtils.getClasseLabel(context, tipo, it) }
}

private fun adicionarLembrete(context: Context, uiState: CriarContaUiState) {
    val valorFinal = uiState.valor.replace(",", ".").toDoubleOrNull() ?: 0.0
    val qtRepete = uiState.qtRepete.toIntOrNull() ?: 1
    val intervalo = when (uiState.intervaloPosicao) {
        0 -> 101
        1 -> 107
        2 -> 300
        3 -> 3650
        else -> 300
    }
    AlertaCalendario.adicionarEventoNoCalendario(
        context.contentResolver,
        context.getString(R.string.dica_evento, uiState.nome),
        context.getString(R.string.dica_calendario, NumberFormat.getCurrencyInstance().format(valorFinal)),
        uiState.dia, uiState.mes, uiState.ano, true, qtRepete, intervalo
    )
}
