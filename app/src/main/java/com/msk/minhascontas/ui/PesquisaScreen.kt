package com.msk.minhascontas.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.PesquisaViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesquisaScreen(
    viewModel: PesquisaViewModel = viewModel(),
    onBack: () -> Unit,
    onEditConta: (List<Long>) -> Unit,
    onLembrete: (Conta) -> Unit
) {
    val searchText by viewModel.searchText.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val filterTipo by viewModel.filterTipo.collectAsState()
    val contas by viewModel.contas.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val currencyFormat = remember(locale) { NumberFormat.getCurrencyInstance(locale) }

    val selectedContas = remember(selectedIds, contas) {
        contas.filter { it.idConta in selectedIds }
    }
    val selectedSum = remember(selectedContas) {
        selectedContas.sumOf { it.valor }
    }

    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            Column {
                if (selectedIds.isEmpty()) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchText,
                                onValueChange = { viewModel.onSearchTextChange(it) },
                                placeholder = { Text(stringResource(R.string.dica_pesquisa_conta)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchTextChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                val titleText = if (selectedIds.size == 1) {
                                    val conta = selectedContas.firstOrNull()
                                    if (conta != null) {
                                        if (conta.qtRepete > 1 && conta.tipo == ContasContract.TIPO_DESPESA &&
                                            (conta.classeConta == ContasContract.CLASSE_DESPESA_CARTAO ||
                                                    conta.classeConta == ContasContract.CLASSE_DESPESA_PRESTACOES)
                                        ) {
                                            "${conta.nome} ${conta.nRepete}/${conta.qtRepete}"
                                        } else {
                                            conta.nome
                                        }
                                    } else {
                                        ""
                                    }
                                } else {
                                    pluralStringResource(
                                        R.plurals.selecao,
                                        selectedIds.size,
                                        selectedIds.size
                                    )
                                }
                                Text(text = titleText, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = currencyFormat.format(selectedSum),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            if (selectedIds.isNotEmpty()) {
                                IconButton(onClick = { 
                                    onEditConta(selectedIds.toList())
                                    viewModel.clearSelection()
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.titulo_editar))
                                }
                            }
                            if (selectedIds.size == 1) {
                                IconButton(onClick = {
                                    scope.launch {
                                        val conta = viewModel.getConta(selectedIds.first())
                                        if (conta != null) onLembrete(conta)
                                        viewModel.clearSelection()
                                    }
                                }) {
                                    Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.titulo_calendario))
                                }
                            }
                            IconButton(onClick = { viewModel.togglePagamentoSelected() }) {
                                Icon(
                                    painterResource(id = R.drawable.paga),
                                    contentDescription = stringResource(R.string.dica_pagamento),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                if (selectedIds.size == 1) {
                                    showDeleteDialog = selectedIds.first()
                                } else {
                                    showMultiDeleteDialog = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.titulo_excluir))
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

                if (selectedIds.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Chips de TIPO (Prioridade para definir o contexto)
                        val tipoChips = listOf(
                            Triple(ContasContract.TIPO_DESPESA, stringResource(R.string.linha_despesa), filterTipo == ContasContract.TIPO_DESPESA),
                            Triple(ContasContract.TIPO_RECEITA, stringResource(R.string.linha_receita), filterTipo == ContasContract.TIPO_RECEITA),
                            Triple(ContasContract.TIPO_APLICACAO, stringResource(R.string.linha_aplicacoes), filterTipo == ContasContract.TIPO_APLICACAO)
                        )
                        tipoChips.forEach { (tipo, label, isSelected) ->
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilterTipo(tipo as Int) },
                                label = { Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    labelColor = Color.White,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.5f),
                                    selectedBorderColor = Color.White
                                )
                            )
                        }

                        // 2. Chips de STATUS (Dinâmicos conforme o tipo selecionado)
                        val statusChips = when (filterTipo) {
                            ContasContract.TIPO_RECEITA -> listOf(
                                Triple(ContasContract.STATUS_PAGO_RECEBIDO, stringResource(R.string.resumo_recebidas), filterStatus == ContasContract.STATUS_PAGO_RECEBIDO),
                                Triple(ContasContract.STATUS_PENDENTE, stringResource(R.string.resumo_areceber), filterStatus == ContasContract.STATUS_PENDENTE)
                            )
                            ContasContract.TIPO_DESPESA -> listOf(
                                Triple(ContasContract.STATUS_PAGO_RECEBIDO, stringResource(R.string.resumo_pagas), filterStatus == ContasContract.STATUS_PAGO_RECEBIDO),
                                Triple(ContasContract.STATUS_PENDENTE, stringResource(R.string.resumo_faltam), filterStatus == ContasContract.STATUS_PENDENTE)
                            )
                            else -> listOf(
                                // Se nenhum tipo selecionado, usa rótulos genéricos removendo o prefixo "Despesas"
                                Triple(
                                    ContasContract.STATUS_PAGO_RECEBIDO,
                                    stringResource(R.string.resumo_pagas).replace("Despesas ", "").replaceFirstChar { it.uppercase() },
                                    filterStatus == ContasContract.STATUS_PAGO_RECEBIDO
                                ),
                                Triple(
                                    ContasContract.STATUS_PENDENTE,
                                    stringResource(R.string.resumo_faltam).replace("Despesas ", "").replaceFirstChar { it.uppercase() },
                                    filterStatus == ContasContract.STATUS_PENDENTE
                                )
                            )
                        }

                        statusChips.forEach { (status, label, isSelected) ->
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilterStatus(status as String) },
                                label = { Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    labelColor = Color.White,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.5f),
                                    selectedBorderColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (contas.isEmpty()) {
                Text(
                    text = stringResource(R.string.dica_pesquisa),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(contas, key = { it.idConta }) { conta ->
                        ContaItem(
                            conta = conta,
                            isSelected = selectedIds.contains(conta.idConta),
                            onClick = {
                                viewModel.toggleSelection(conta.idConta)
                            },
                            onLongClick = {
                                viewModel.toggleSelection(conta.idConta)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        val contaSelecionada = contas.find { it.idConta == showDeleteDialog }
        MinhasContasDialogTheme {
            if (contaSelecionada != null && contaSelecionada.qtRepete > 1) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text(stringResource(R.string.dica_menu_exclusao)) },
                    text = {
                        Column {
                            val options = context.resources.getStringArray(R.array.TipoAjusteConta)
                            options.forEachIndexed { index, option ->
                                TextButton(
                                    onClick = {
                                        val tipo = when (index) {
                                            0 -> DBContas.TipoExclusao.SOMENTE_ESTA
                                            1 -> DBContas.TipoExclusao.DESTA_EM_DIANTE
                                            else -> DBContas.TipoExclusao.TODAS_AS_REPETICOES
                                        }
                                        viewModel.deleteSingle(showDeleteDialog!!, tipo)
                                        showDeleteDialog = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        option,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text(stringResource(R.string.cancelar))
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text(stringResource(R.string.titulo_excluir)) },
                    text = { Text(pluralStringResource(R.plurals.confirmar_exclusao_multipla_mensagem, 1)) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteSingle(showDeleteDialog!!, DBContas.TipoExclusao.SOMENTE_ESTA)
                            showDeleteDialog = null
                        }) {
                            Text(stringResource(R.string.sim))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text(stringResource(R.string.nao))
                        }
                    }
                )
            }
        }
    }

    if (showMultiDeleteDialog) {
        MinhasContasDialogTheme {
            AlertDialog(
                onDismissRequest = { showMultiDeleteDialog = false },
                title = { Text(stringResource(R.string.confirmar_exclusao_multipla_titulo)) },
                text = {
                    Text(
                        pluralStringResource(
                            R.plurals.confirmar_exclusao_multipla_mensagem,
                            selectedIds.size,
                            selectedIds.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMultipleSelected()
                        showMultiDeleteDialog = false
                    }) {
                        Text(stringResource(R.string.sim))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMultiDeleteDialog = false }) {
                        Text(stringResource(R.string.nao))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContaItem(
    conta: Conta,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val currencyFormat = remember(locale) { NumberFormat.getCurrencyInstance(locale) }
    val dateFormat = remember(locale) { DateFormat.getDateInstance(DateFormat.SHORT, locale) }
    val semana = remember { context.resources.getStringArray(R.array.Semana) }

    val backgroundColor = if (isSelected) {
        colorResource(R.color.linha_selecionada)
    } else {
        Color.Transparent
    }

    val valorColor = when (conta.tipo) {
        ContasContract.TIPO_DESPESA -> colorResource(R.color.despesa_color)
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        else -> colorResource(R.color.aplicacao_color)
    }

    val displayName = if (conta.qtRepete > 1 &&
        (conta.tipo == ContasContract.TIPO_DESPESA) &&
        (conta.classeConta == ContasContract.CLASSE_DESPESA_CARTAO ||
                conta.classeConta == ContasContract.CLASSE_DESPESA_PRESTACOES)
    ) {
        "${conta.nome} ${conta.nRepete}/${conta.qtRepete}"
    } else {
        conta.nome
    }

    val cal = remember(conta) {
        Calendar.getInstance().apply { set(conta.ano, conta.mes - 1, conta.dia) }
    }
    val dataStr = dateFormat.format(cal.time)
    val diaSemana = semana[cal.get(Calendar.DAY_OF_WEEK) - 1]

    val categoriaStr = remember(conta) {
        val classeLabel = LabelUtils.getClasseLabel(context, conta.tipo, conta.classeConta)
        val categoriaLabel = LabelUtils.getCategoriaLabel(context, conta.categoria)
        if (conta.tipo == ContasContract.TIPO_DESPESA) {
            "$classeLabel | $categoriaLabel"
        } else {
            classeLabel
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = categoriaStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(conta.valor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valorColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dataStr ($diaSemana)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (conta.pagamento == ContasContract.STATUS_PAGO_RECEBIDO &&
                        (conta.tipo == ContasContract.TIPO_DESPESA || conta.tipo == ContasContract.TIPO_RECEITA)
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.visto),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = valorColor
                        )
                    }
                }
            }
        }
    }
}
