package com.msk.minhascontas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.msk.minhascontas.R
import com.msk.minhascontas.viewmodel.AjustesViewModel
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    viewModel: AjustesViewModel,
    onBackClick: () -> Unit,
    onNavigateToPersonalizarCategorias: () -> Unit,
    onNavigateToPlanejamento: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onSyncCloudDownload: () -> Unit,
    onSyncCloudUpload: () -> Unit,
    onSelectBackupFolder: () -> Unit,
    onExecuteBackup: () -> Unit,
    onExecuteRestore: () -> Unit,
    onExportExcel: () -> Unit,
    onImportExcel: () -> Unit,
    onImportPDF: () -> Unit,
    onImportOldDB: () -> Unit,
    onDeleteAll: () -> Unit,
    onLogout: () -> Unit,
    isNotificationServiceEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onPreferenceChanged: () -> Unit,
    isLoading: Boolean = false,
    loadingMessage: String = ""
) {
    val appVersion by viewModel.appVersion.observeAsState("N/A")
    val backupLocation by viewModel.backupLocation.observeAsState("")
    val userEmail by viewModel.userEmail.observeAsState(null)

    // Estados para os Diálogos
    var showOrderDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var showAnswerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showConfirmUploadDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_ajustes)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // CATEGORIA: EXIBIÇÃO E COMPORTAMENTO
            item { PreferenceCategory(stringResource(R.string.pref_titulo_geral)) }
            
            item {
                val key = stringResource(R.string.pref_key_ordem)
                val entries = stringArrayResource(R.array.ordem_contas)
                val values = stringArrayResource(R.array.ordem_contas_valores)
                val currentValue = viewModel.getPreference(key, values[0])
                val currentIndex = values.indexOf(currentValue).coerceAtLeast(0)
                
                PreferenceItem(
                    title = stringResource(R.string.pref_dialogo_ordem),
                    summary = entries[currentIndex],
                    onClick = { showOrderDialog = true }
                )

                if (showOrderDialog) {
                    MinhasContasDialogTheme {
                        ListPreferenceDialog(
                            title = stringResource(R.string.pref_titulo_ordem),
                            entries = entries,
                            values = values,
                            currentValue = currentValue,
                            onDismiss = { showOrderDialog = false },
                            onValueSelected = {
                                viewModel.setPreference(key, it)
                                showOrderDialog = false
                            }
                        )
                    }
                }
            }

            item {
                val key = stringResource(R.string.pref_key_resumo)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_resumo),
                    summary = if (checked) stringResource(R.string.pref_descricao_resumo_mensal) else stringResource(R.string.pref_descricao_resumo_diario),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            item {
                val key = stringResource(R.string.pref_key_categoria)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_categoria),
                    summary = if (checked) stringResource(R.string.pref_descricao_categoria) else stringResource(R.string.pref_descricao_sem_categoria),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            item {
                val key = stringResource(R.string.pref_key_saldo)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_saldo),
                    summary = if (checked) stringResource(R.string.pref_descricao_saldo_somado) else stringResource(R.string.pref_descricao_saldo_real),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            item {
                val key = stringResource(R.string.pref_key_auto_import_fixas)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_auto_import_fixas),
                    summary = if (checked) stringResource(R.string.pref_descricao_auto_import_fixas_on) else stringResource(R.string.pref_descricao_auto_import_fixas_off),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            item {
                val key = stringResource(R.string.pref_key_pagamento)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_pagamento),
                    summary = if (checked) stringResource(R.string.pref_descricao_autopagamento) else stringResource(R.string.pref_descricao_editapagamento),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            // CATEGORIA: RECURSOS E PERSONALIZAÇÃO
            item { PreferenceCategory(stringResource(R.string.pref_titulo_categoria_recursos)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_personalizar_categorias),
                    summary = stringResource(R.string.pref_descricao_personalizar_categorias),
                    onClick = onNavigateToPersonalizarCategorias
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_planejamento_financeiro),
                    summary = stringResource(R.string.pref_descricao_planejamento_financeiro),
                    onClick = onNavigateToPlanejamento
                )
            }
            item {
                val key = stringResource(R.string.pref_key_notificacao)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_notificacao),
                    summary = stringResource(R.string.pref_descricao_notificacao),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        if (it && !isNotificationServiceEnabled) {
                            showNotificationDialog = true
                        }
                    }
                )

                if (showNotificationDialog) {
                    MinhasContasDialogTheme {
                        AlertDialog(
                            onDismissRequest = { showNotificationDialog = false },
                            title = { Text(stringResource(R.string.pref_titulo_notificacao)) },
                            text = { Text(stringResource(R.string.msg_permissao_notificacao)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    onOpenNotificationSettings()
                                    showNotificationDialog = false
                                }) {
                                    Text(stringResource(R.string.ir_para_configuracoes))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNotificationDialog = false }) {
                                    Text(stringResource(R.string.cancelar))
                                }
                            }
                        )
                    }
                }
            }

            // CATEGORIA: ALERTAS E NOTIFICAÇÕES INTERNAS
            item { PreferenceCategory(stringResource(R.string.pref_titulo_alertas)) }
            item {
                val key = stringResource(R.string.pref_alert_vencimento_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_vencimento_titulo),
                    summary = stringResource(R.string.pref_alert_vencimento_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }
            item {
                val key = stringResource(R.string.pref_alert_limite_categoria_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_limite_categoria_titulo),
                    summary = stringResource(R.string.pref_alert_limite_categoria_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }
            item {
                val key = stringResource(R.string.pref_alert_objetivo_plano_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_objetivo_plano_titulo),
                    summary = stringResource(R.string.pref_alert_objetivo_plano_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }
            item {
                val key = stringResource(R.string.pref_alert_receita_referencia_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_receita_referencia_titulo),
                    summary = stringResource(R.string.pref_alert_receita_referencia_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }
            item {
                val key = stringResource(R.string.pref_alert_despesa_receita_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_despesa_receita_titulo),
                    summary = stringResource(R.string.pref_alert_despesa_receita_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }
            item {
                val key = stringResource(R.string.pref_alert_falta_aplicacao_key)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, true)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_alert_falta_aplicacao_titulo),
                    summary = stringResource(R.string.pref_alert_falta_aplicacao_desc),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            // CATEGORIA: SEGURANÇA E ACESSO
            item { PreferenceCategory(stringResource(R.string.titulo_acesso)) }
            item {
                val key = stringResource(R.string.pref_key_acesso)
                var checked by remember { mutableStateOf(viewModel.getPreference(key, false)) }
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_titulo_acesso),
                    summary = if (checked) stringResource(R.string.pref_descricao_acesso_negado) else stringResource(R.string.pref_descricao_acesso_livre),
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        viewModel.setPreference(key, it)
                        onPreferenceChanged()
                    }
                )
            }

            item {
                val key = stringResource(R.string.pref_key_senha)
                val senha = viewModel.getPreference(key, "")
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_senha),
                    summary = if (senha.isEmpty()) stringResource(R.string.pref_descricao_senha) else stringResource(R.string.pref_descricao_senha_definida),
                    onClick = { showPasswordDialog = true }
                )
                if (showPasswordDialog) {
                    MinhasContasDialogTheme {
                        EditTextPreferenceDialog(
                            title = stringResource(R.string.pref_dialogo_senha),
                            initialValue = senha,
                            isPassword = true,
                            onDismiss = { showPasswordDialog = false },
                            onValueSaved = {
                                viewModel.setPreference(key, it)
                                showPasswordDialog = false
                            }
                        )
                    }
                }
            }

            item {
                val key = "pergunta_seguranca_id"
                val entries = stringArrayResource(R.array.perguntas_seguranca)
                val values = stringArrayResource(R.array.perguntas_seguranca_valores)
                val currentValue = viewModel.getPreference(key, "0")
                val currentIndex = values.indexOf(currentValue).coerceAtLeast(0)

                PreferenceItem(
                    title = stringResource(R.string.titulo_pergunta_secreta),
                    summary = entries[currentIndex],
                    onClick = { showQuestionDialog = true }
                )

                if (showQuestionDialog) {
                    MinhasContasDialogTheme {
                        ListPreferenceDialog(
                            title = stringResource(R.string.titulo_pergunta_secreta),
                            entries = entries,
                            values = values,
                            currentValue = currentValue,
                            onDismiss = { showQuestionDialog = false },
                            onValueSelected = {
                                viewModel.setPreference(key, it)
                                showQuestionDialog = false
                            }
                        )
                    }
                }
            }

            item {
                val key = "resposta_secreta"
                val resposta = viewModel.getPreference(key, "")
                PreferenceItem(
                    title = stringResource(R.string.dica_resposta_secreta_ajustes),
                    summary = if (resposta.isEmpty()) stringResource(R.string.dica_resposta_secreta_ajustes_summary) else stringResource(R.string.resposta_definida),
                    onClick = { showAnswerDialog = true }
                )
                if (showAnswerDialog) {
                    MinhasContasDialogTheme {
                        EditTextPreferenceDialog(
                            title = stringResource(R.string.titulo_pergunta_secreta),
                            initialValue = resposta,
                            isPassword = true,
                            onDismiss = { showAnswerDialog = false },
                            onValueSaved = {
                                viewModel.setPreference(key, it)
                                showAnswerDialog = false
                            }
                        )
                    }
                }
            }

            // CATEGORIA: SINCRONIZAÇÃO EM NUVEM
            item { PreferenceCategory(stringResource(R.string.pref_titulo_nuvem)) }
            item {
                PreferenceItem(
                    title = if (userEmail == null) stringResource(R.string.pref_titulo_google_login) else stringResource(R.string.pref_titulo_google_logout),
                    summary = if (userEmail == null) stringResource(R.string.pref_descricao_google_login) else stringResource(R.string.pref_descricao_google_logout, userEmail!!),
                    onClick = {
                        if (userEmail == null) {
                            onGoogleLoginClick()
                        } else {
                            showLogoutDialog = true
                        }
                    }
                )

                if (showLogoutDialog) {
                    MinhasContasDialogTheme {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = { Text(stringResource(R.string.pref_titulo_google_logout)) },
                            text = { Text(stringResource(R.string.pref_descricao_google_logout, userEmail ?: "")) },
                            confirmButton = {
                                TextButton(onClick = {
                                    onLogout()
                                    showLogoutDialog = false
                                }) {
                                    Text(stringResource(R.string.salvar))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutDialog = false }) {
                                    Text(stringResource(R.string.cancelar))
                                }
                            }
                        )
                    }
                }
            }
            if (userEmail != null) {
                item {
                    PreferenceItem(
                        title = stringResource(R.string.pref_titulo_cloud_download),
                        summary = stringResource(R.string.pref_descricao_cloud_download),
                        onClick = onSyncCloudDownload
                    )
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.pref_titulo_google_upload),
                        summary = stringResource(R.string.pref_descricao_google_upload),
                        onClick = { showConfirmUploadDialog = true }
                    )

                    if (showConfirmUploadDialog) {
                        MinhasContasDialogTheme {
                            AlertDialog(
                                onDismissRequest = { showConfirmUploadDialog = false },
                                title = { Text(stringResource(R.string.google_upload_confirm_title)) },
                                text = { Text(stringResource(R.string.google_upload_confirm_msg)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onSyncCloudUpload()
                                        showConfirmUploadDialog = false
                                    }) {
                                        Text(stringResource(R.string.sim))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmUploadDialog = false }) {
                                        Text(stringResource(R.string.nao))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // CATEGORIA: BACKUP E RESTAURAÇÃO
            item { PreferenceCategory(stringResource(R.string.pref_titulo_bkup_restaura)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_bkup_select_folder),
                    summary = if (backupLocation.isEmpty()) stringResource(R.string.pref_descricao_bkup_select_folder) else "${stringResource(R.string.pref_descricao_bkup_local_chosen)}: $backupLocation",
                    onClick = onSelectBackupFolder
                )
            }
            if (backupLocation.isNotEmpty()) {
                item {
                    PreferenceItem(
                        title = stringResource(R.string.pref_titulo_bkup_execute),
                        summary = stringResource(R.string.pref_descricao_bkup_execute),
                        onClick = onExecuteBackup
                    )
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.pref_titulo_restaura_execute),
                        summary = stringResource(R.string.pref_descricao_restaura_execute),
                        onClick = onExecuteRestore
                    )
                }
            }

            // CATEGORIA: EXPORTAÇÃO E IMPORTAÇÃO
            item { PreferenceCategory(stringResource(R.string.pref_titulo_categoria_exportar)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_exportar),
                    summary = stringResource(R.string.pref_descricao_exportar),
                    onClick = onExportExcel
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_importar),
                    summary = stringResource(R.string.pref_descricao_importar),
                    onClick = onImportExcel
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_importar_pdf),
                    summary = stringResource(R.string.pref_descricao_importar_pdf),
                    onClick = onImportPDF
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_importar_banco_antigo),
                    summary = stringResource(R.string.pref_descricao_importar_banco_antigo),
                    onClick = onImportOldDB
                )
            }

            // CATEGORIA: MANUTENÇÃO
            item { PreferenceCategory(stringResource(R.string.pref_titulo_apagatudo)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_apagatudo),
                    summary = stringResource(R.string.pref_descricao_apagatudo),
                    titleColor = colorResource(R.color.despesa_color),
                    onClick = { showDeleteAllDialog = true }
                )

                if (showDeleteAllDialog) {
                    MinhasContasDialogTheme {
                        AlertDialog(
                            onDismissRequest = { showDeleteAllDialog = false },
                            title = { Text(stringResource(R.string.titulo_exclui_tudo)) },
                            text = { Text(stringResource(R.string.texto_exclui_tudo)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDeleteAll()
                                    showDeleteAllDialog = false
                                }) {
                                    Text(stringResource(R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAllDialog = false }) {
                                    Text(stringResource(R.string.cancelar))
                                }
                            }
                        )
                    }
                }
            }

            // CATEGORIA: SOBRE
            item { PreferenceCategory(stringResource(R.string.pref_titulo_categoria_info)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_autor),
                    summary = stringResource(R.string.pref_descricao_autor)
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.pref_titulo_versao),
                    summary = appVersion
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    if (loadingMessage.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(text = loadingMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = colorResource(R.color.total_planejado_color),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.total_planejado_color),
                checkedTrackColor = colorResource(R.color.total_planejado_color).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun ListPreferenceDialog(
    title: String,
    entries: Array<String>,
    values: Array<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onValueSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    if (index < values.size) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (values[index] == currentValue),
                                    onClick = { onValueSelected(values[index]) }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (values[index] == currentValue),
                                onClick = { onValueSelected(values[index]) }
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun EditTextPreferenceDialog(
    title: String,
    initialValue: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onValueSaved: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onValueSaved(text) }) {
                Text(stringResource(R.string.salvar), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
