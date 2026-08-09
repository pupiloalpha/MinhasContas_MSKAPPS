package com.msk.minhascontas.ui.layouts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.NotificationRepository
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.Notificacao
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.ui.theme.DarkOnSurface
import com.msk.minhascontas.ui.theme.DarkReceitaColor
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme
import com.msk.minhascontas.ui.theme.OnSurface
import com.msk.minhascontas.ui.theme.Primary
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.utils.ShareUtils
import kotlinx.coroutines.launch

enum class LockState {
    LOGIN,
    RECOVERY_QUESTION,
    NEW_PASSWORD
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

// No Android 15, edge-to-edge is enforced by default.

    var currentState by remember { mutableStateOf(LockState.LOGIN) }

    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var recoveryAnswer by remember { mutableStateOf("") }

    var newPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val savedPassword = remember { prefs.getString("senha", "") ?: "" }
    val perguntaId = remember { prefs.getString("pergunta_seguranca_id", null) }
    val respostaSalva = remember { prefs.getString("resposta_secreta", null) }

    // Cores para os campos de texto
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

    // Cores otimizadas para botões de ação com contraste alto em Dark Mode
    val primaryButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
        contentColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (currentState) {
                LockState.LOGIN -> {
                    Text(
                        text = stringResource(R.string.acesso),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.senha)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = directTextFieldColors
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (password == savedPassword) {
                                onUnlock()
                            } else {
                                errorMessage = context.getString(R.string.senha_errada)
                                password = ""
                            }
                        },
                        colors = primaryButtonColors,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        if (perguntaId == null || respostaSalva.isNullOrEmpty()) {
                            Toast.makeText(context, R.string.erro_recuperacao_nao_configurada, Toast.LENGTH_LONG).show()
                        } else {
                            errorMessage = null
                            currentState = LockState.RECOVERY_QUESTION
                        }
                    }) {
                        Text(
                            text = stringResource(R.string.esqueci_senha),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                LockState.RECOVERY_QUESTION -> {
                    val perguntas = context.resources.getStringArray(R.array.perguntas_seguranca)
                    val perguntaTexto = perguntaId?.toIntOrNull()?.let { idx ->
                        if (idx in perguntas.indices) perguntas[idx] else null
                    } ?: ""

                    Text(
                        text = stringResource(R.string.recuperacao_senha),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = perguntaTexto,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = {
                            recoveryAnswer = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.dica_resposta_secreta)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (recoveryAnswer.trim().equals(respostaSalva, ignoreCase = true)) {
                                errorMessage = null
                                currentState = LockState.NEW_PASSWORD
                            } else {
                                errorMessage = context.getString(R.string.senha_errada)
                            }
                        },
                        colors = primaryButtonColors,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.confirmar))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        errorMessage = null
                        currentState = LockState.LOGIN
                    }) {
                        Text(
                            text = stringResource(R.string.cancelar),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                LockState.NEW_PASSWORD -> {
                    Text(
                        text = stringResource(R.string.redefinir_senha),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.nova_senha)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(
                                    imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),

                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newPassword.isNotBlank()) {
                                prefs.edit().putString("senha", newPassword).apply()
                                Toast.makeText(context, R.string.senha_redefinida_sucesso, Toast.LENGTH_LONG).show()
                                onUnlock()
                            } else {
                                errorMessage = context.getString(R.string.atencao)
                            }
                        },
                        colors = primaryButtonColors,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.salvar))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        errorMessage = null
                        currentState = LockState.LOGIN
                    }) {
                        Text(
                            text = stringResource(R.string.cancelar),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestartAppDialog(reason: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = when (reason) {
        Ajustes.REASON_DB_RESTORE -> stringResource(R.string.dica_restaura_bd_titulo)
        Ajustes.REASON_PREFERENCES_CHANGED, Ajustes.REASON_LABELS_CHANGED -> stringResource(R.string.dica_restart_app_titulo)
        else -> stringResource(R.string.atencao)
    }
    val message = when (reason) {
        Ajustes.REASON_DB_RESTORE -> stringResource(R.string.dica_restaura_bd_completa)
        Ajustes.REASON_PREFERENCES_CHANGED, Ajustes.REASON_LABELS_CHANGED -> stringResource(R.string.dica_restart_app_message)
        else -> stringResource(R.string.dica_texto_reinicio)
    }

    MCAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = message,
        confirmLabel = stringResource(R.string.reiniciar_agora),
        onConfirm = { (context as? Activity)?.let { AjustesUtils.restartApplication(it) } },
        dismissLabel = stringResource(R.string.cancelar)
    )
}

@Composable
fun AiLoadingDialog() {
    MCAlertDialog(
        onDismissRequest = { },
        title = stringResource(R.string.gemini_title),
        onConfirm = null,
        dismissLabel = null,
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.gemini_loading))
            }
        }
    )
}

@Composable
fun AiResultDialog(result: AIResult, onDismiss: () -> Unit) {
    val context = LocalContext.current
    MCAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.gemini_title),
        content = {
            Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                when (result) {
                    is AIResult.Success -> {
                        AndroidView(
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    movementMethod = LinkMovementMethod.getInstance()
                                }
                            },
                            update = { it.text = Html.fromHtml(result.content, Html.FROM_HTML_MODE_LEGACY) },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    is AIResult.Error -> {
                        Text(stringResource(R.string.ai_error_fallback_msg))
                    }
                }
            }
        },
        confirmLabel = if (result is AIResult.Success) stringResource(R.string.gemini_entendi) else stringResource(R.string.ai_send_gemini),
        onConfirm = {
            if (result is AIResult.Error) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/"))
                    context.startActivity(intent)
                } catch (ignore: Exception) {
                    Toast.makeText(context, R.string.erro_abrir_gemini, Toast.LENGTH_SHORT).show()
                }
            }
            onDismiss()
        },
        dismissLabel = if (result is AIResult.Error) stringResource(R.string.ai_copy_prompt) else stringResource(android.R.string.cancel),
        onDismiss = {
            if (result is AIResult.Error) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Prompt", result.fullPrompt)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.ai_prompt_copied, Toast.LENGTH_SHORT).show()
            } else {
                onDismiss()
            }
        }
    )
}

@Composable
fun NotificationsDialog(
    onDismiss: () -> Unit,
    onRenovarSerie: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NotificationRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<Notificacao>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        notifications = repository.getNotificacoesNaoLidas()
        isLoading = false
        if (notifications.isEmpty()) {
            Toast.makeText(context, R.string.nenhuma_notificacao, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    if (isLoading) return

    if (notifications.isNotEmpty()) {
        MCAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.titulo_notificacoes),
            content = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp).fillMaxWidth()) {
                    items(notifications) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val tipo = item.tipo
                                    if (tipo?.startsWith("fim_serie|") == true) {
                                        val idConta = tipo.substringAfter("|").toLongOrNull()
                                        if (idConta != null) onRenovarSerie(idConta)
                                    }
                                    scope.launch {
                                        repository.marcarNotificacaoLida(item.id)
                                        notifications = repository.getNotificacoesNaoLidas()
                                        if (notifications.isEmpty()) onDismiss()
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = item.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.mensagem,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            },
            confirmLabel = stringResource(R.string.marcar_todas_lidas),
            onConfirm = {
                scope.launch {
                    repository.marcarTodasNotificacoesLidas()
                    onDismiss()
                }
            },
            dismissLabel = stringResource(R.string.cancelar),
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ShareSelectionDialog(
    month: Int,
    year: Int,
    contas: List<Conta>,
    onDismiss: () -> Unit,
    onShare: (text: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val keySaldo = stringResource(R.string.pref_key_saldo)
    val keyAplic = stringResource(R.string.pref_key_aplicacao_acumulada)

    val usaSaldoSomado = prefs.getBoolean(keySaldo, false)
    val usaInvestimentoAcumulado = prefs.getBoolean(keyAplic, false)

    var shareType by remember { mutableStateOf("summary") }
    val mesesArray = remember { context.resources.getStringArray(R.array.MesesDoAno) }
    val mesNome = mesesArray.getOrNull(month - 1) ?: month.toString()

    MCAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.titulo_enviar),
        content = {
            Column {
                Text(
                    text = "${stringResource(R.string.import_period_label)} $mesNome/$year",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { shareType = "summary" }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = shareType == "summary", onClick = { shareType = "summary" })
                    Text(stringResource(R.string.titulo_resumo), modifier = Modifier.padding(start = 8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { shareType = "list" }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = shareType == "list", onClick = { shareType = "list" })
                    Text(stringResource(R.string.share_list_detailed), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = {
            val text = if (shareType == "summary") {
                ShareUtils.generateSummaryText(
                    context, month, year, contas,
                    0.0, 0.0,
                    usaSaldoSomado, usaInvestimentoAcumulado
                )
            } else {
                ShareUtils.generateListText(context, month, year, contas)
            }
            onShare(text)
            onDismiss()
        },
        dismissLabel = stringResource(R.string.cancelar),
        onDismiss = onDismiss
    )
}
