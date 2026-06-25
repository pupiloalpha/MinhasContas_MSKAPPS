package com.msk.minhascontas.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.features.ai.AIResult
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.utils.AjustesUtils

@Composable
fun AppLockDialog(onDismiss: () -> Unit, onRecoverPassword: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val savedPassword = prefs.getString("senha", "") ?: ""

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.acesso)) },
        text = {
            Column {
                TextField(
                    value = password,
                    onValueChange = { password = it; error = false },
                    label = { Text(stringResource(R.string.senha)) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text(stringResource(R.string.senha_errada), color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onRecoverPassword) {
                    Text(stringResource(R.string.esqueci_senha))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password == savedPassword) onDismiss() else { error = true; password = "" }
            }) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
fun RestartAppDialog(reason: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = when (reason) {
        Ajustes.REASON_DB_RESTORE -> stringResource(R.string.dica_restaura_bd_titulo)
        Ajustes.REASON_PREFERENCES_CHANGED -> stringResource(R.string.dica_restart_app_titulo)
        else -> stringResource(R.string.atencao)
    }
    val message = when (reason) {
        Ajustes.REASON_DB_RESTORE -> stringResource(R.string.dica_restaura_bd_completa)
        Ajustes.REASON_PREFERENCES_CHANGED -> stringResource(R.string.dica_restart_app_message)
        else -> stringResource(R.string.dica_texto_reinicio)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = { (context as? android.app.Activity)?.let { AjustesUtils.restartApplication(it) } }) {
                Text(stringResource(R.string.reiniciar_agora))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

@Composable
fun AiLoadingDialog() {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = { },
        title = { Text(stringResource(R.string.gemini_title)) },
        text = {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gemini_title)) },
        text = {
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
        confirmButton = {
            if (result is AIResult.Success) {
                Button(onClick = onDismiss) { Text(stringResource(R.string.gemini_entendi)) }
            } else {
                Button(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/"))
                        context.startActivity(intent)
                    } catch (ignore: Exception) {
                        Toast.makeText(context, R.string.erro_abrir_gemini, Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }) { Text(stringResource(R.string.ai_send_gemini)) }
            }
        },
        dismissButton = {
            if (result is AIResult.Error) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("AI Prompt", result.fullPrompt)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.ai_prompt_copied, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.ai_copy_prompt)) }
            } else if (result is AIResult.Success) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
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
    val db = remember { DBContas.getInstance(context) }
    val cursor = remember { db.getNotificacoesNaoLidasCursor() }

    if (cursor == null || cursor.count == 0) {
        Toast.makeText(context, R.string.nenhuma_notificacao, Toast.LENGTH_SHORT).show()
        cursor?.close()
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.titulo_notificacoes)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                AndroidView(
                    factory = { ctx ->
                        val listView = ListView(ctx)
                        val from = arrayOf(ContasContract.Notificacoes.COLUNA_TITULO, ContasContract.Notificacoes.COLUNA_MENSAGEM)
                        val to = intArrayOf(R.id.tvTituloNotificacao, R.id.tvMensagemNotificacao)
                        val adapter = SimpleCursorAdapter(ctx, R.layout.item_notificacao, cursor, from, to, 0)
                        listView.adapter = adapter
                        listView.setOnItemClickListener { _, _, _, id ->
                            val cursorItem = adapter.cursor
                            val typeIndex = cursorItem.getColumnIndex(ContasContract.Notificacoes.COLUNA_TIPO)
                            val type = if (typeIndex != -1) cursorItem.getString(typeIndex) else ""

                            if (type.startsWith("fim_serie|")) {
                                val idContaStr = type.substringAfter("|")
                                val idConta = idContaStr.toLongOrNull()
                                if (idConta != null) {
                                    onRenovarSerie(idConta)
                                }
                            }
                            db.marcarNotificacaoComoLida(id)
                            val newCursor = db.getNotificacoesNaoLidasCursor()
                            adapter.changeCursor(newCursor)
                            if (newCursor.count == 0) onDismiss()
                        }
                        listView
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { db.marcarTodasNotificacoesComoLidas(); onDismiss() }) {
                Text(stringResource(R.string.marcar_todas_lidas))
            }
        }
    )
}
