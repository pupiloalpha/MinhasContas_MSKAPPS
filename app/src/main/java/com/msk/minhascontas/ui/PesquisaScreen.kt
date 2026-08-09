package com.msk.minhascontas.ui

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Payment
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
import com.msk.minhascontas.db.TipoExclusao
import com.msk.minhascontas.ui.layouts.MCAlertDialog
import com.msk.minhascontas.ui.layouts.SelectionTopAppBar
import com.msk.minhascontas.ui.layouts.SharedContaItem
import com.msk.minhascontas.ui.layouts.StandardTopAppBar
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme
import com.msk.minhascontas.viewmodel.PesquisaViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesquisaScreen(
    viewModel: PesquisaViewModel = viewModel(),
    onBack: () -> Unit,
    onEditConta: (List<Long>) -> Unit,
    onCoachClick: ((String) -> Unit)? = null,
    onLembrete: (Conta) -> Unit
) {
    val searchText by viewModel.searchText.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val filterTipo by viewModel.filterTipo.collectAsState()
    val contas by viewModel.contas.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val selectedContas = remember(selectedIds, contas) {
        contas.filter { it.idConta in selectedIds }
    }

    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            Column {
                if (selectedIds.isEmpty()) {
                    StandardTopAppBar(
                        onBackClick = onBack,
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContent = {
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
                        }
                    )
                } else {
                    SelectionTopAppBar(
                        selectedContas = selectedContas,
                        onSelectionClear = { viewModel.clearSelection() },
                        onTogglePagamento = { viewModel.togglePagamentoSelected() },
                        onEditar = { ids: List<Long> ->
                            onEditConta(ids)
                            viewModel.clearSelection()
                        },
                        onExcluir = {
                            if (selectedIds.size == 1) {
                                showDeleteDialog = selectedIds.first()
                            } else {
                                showMultiDeleteDialog = true
                            }
                        },
                        onLembrete = {
                            val conta = selectedContas.firstOrNull() ?: return@SelectionTopAppBar
                            onLembrete(conta)
                            viewModel.clearSelection()
                        },
                        onCoachClick = { metaId: String ->
                            if (onCoachClick != null) {
                                onCoachClick(metaId)
                            } else {
                                val intent = Intent(context, com.msk.minhascontas.features.planos.PlanoFinanceiroActivity::class.java).apply {
                                    putExtra("metaId", metaId)
                                }
                                context.startActivity(intent)
                            }
                            viewModel.clearSelection()
                        }
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
                        SharedContaItem(
                            conta = conta,
                            isSelected = selectedIds.contains(conta.idConta),
                        showFullDate = true, // Pesquisa precisa da data completa
                        onClick = {
                            if (selectedIds.isEmpty()) {
                                if (conta.isCoach() && onCoachClick != null) {
                                    onCoachClick(conta.codigo)
                                } else {
                                    viewModel.toggleSelection(conta.idConta)
                                }
                            } else {
                                viewModel.toggleSelection(conta.idConta)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelection(conta.idConta)
                        },
                            onTogglePagamento = {
                                viewModel.togglePagamento(conta.idConta)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        val contaSelecionada = contas.find { it.idConta == showDeleteDialog }
        if (contaSelecionada != null && contaSelecionada.qtRepete > 1) {
            MCAlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = stringResource(R.string.dica_menu_exclusao),
                onConfirm = null,
                dismissLabel = stringResource(R.string.cancelar),
                content = {
                    Column {
                        val options = context.resources.getStringArray(R.array.TipoAjusteConta)
                        options.forEachIndexed { index, option ->
                            TextButton(
                                onClick = {
                                    val tipo = when (index) {
                                        0 -> TipoExclusao.SOMENTE_ESTA
                                        1 -> TipoExclusao.DESTA_EM_DIANTE
                                        else -> TipoExclusao.TODAS_AS_REPETICOES
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
                }
            )
        } else {
            MCAlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = stringResource(R.string.titulo_excluir),
                text = pluralStringResource(R.plurals.confirmar_exclusao_multipla_mensagem, 1),
                confirmLabel = stringResource(R.string.sim),
                onConfirm = {
                    viewModel.deleteSingle(showDeleteDialog!!, TipoExclusao.SOMENTE_ESTA)
                    showDeleteDialog = null
                },
                dismissLabel = stringResource(R.string.nao)
            )
        }
    }

    if (showMultiDeleteDialog) {
        MCAlertDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            title = stringResource(R.string.confirmar_exclusao_multipla_titulo),
            text = pluralStringResource(
                R.plurals.confirmar_exclusao_multipla_mensagem,
                selectedIds.size,
                selectedIds.size
            ),
            confirmLabel = stringResource(R.string.sim),
            onConfirm = {
                viewModel.deleteMultipleSelected()
                showMultiDeleteDialog = false
            },
            dismissLabel = stringResource(R.string.nao)
        )
    }
}
