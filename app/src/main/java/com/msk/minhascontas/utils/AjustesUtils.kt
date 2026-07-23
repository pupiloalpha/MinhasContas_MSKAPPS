package com.msk.minhascontas.utils

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.msk.minhascontas.R
import com.msk.minhascontas.features.info.Ajustes

/**
 * Utilitário para gerenciar o status de atualizações e reinícios após alterações nas configurações.
 */
object AjustesUtils {

    // Status global para controle de atualizações
    var pendingRestartReason: String? = null
    var pendingDataRefresh: Boolean = false

    @Composable
    fun RestartAppDialog(reason: String?, onDismiss: () -> Unit) {
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
                Button(onClick = { 
                    // No Compose, precisamos de uma referência à Activity para reiniciar
                }) {
                    Text(stringResource(R.string.reiniciar_agora))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    /**
     * Exibe o diálogo de reinício em atividades baseadas em Views.
     */
    fun showRestartDialog(activity: Activity, reason: String?) {
        val title = when (reason) {
            Ajustes.REASON_DB_RESTORE -> activity.getString(R.string.dica_restaura_bd_titulo)
            Ajustes.REASON_PREFERENCES_CHANGED -> activity.getString(R.string.dica_restart_app_titulo)
            else -> activity.getString(R.string.atencao)
        }
        val message = when (reason) {
            Ajustes.REASON_DB_RESTORE -> activity.getString(R.string.dica_restaura_bd_completa)
            Ajustes.REASON_PREFERENCES_CHANGED -> activity.getString(R.string.dica_restart_app_message)
            else -> activity.getString(R.string.dica_texto_reinicio)
        }

        AlertDialog.Builder(activity, R.style.TemaDialogo)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.reiniciar_agora) { _, _ ->
                restartApplication(activity)
            }
            .setNegativeButton(R.string.cancelar) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Reinicia a aplicação a partir da atividade atual de forma robusta.
     */
    fun restartApplication(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            activity.startActivity(intent)
        }
        activity.finish()
    }

    /**
     * Verifica se há atualizações pendentes e as processa.
     * Deve ser chamado no onResume das atividades.
     */
    fun checkPendingUpdates(activity: Activity, onRefresh: () -> Unit) {
        pendingRestartReason?.let { reason ->
            showRestartDialog(activity, reason)
            pendingRestartReason = null
        }
        if (pendingDataRefresh) {
            onRefresh()
            pendingDataRefresh = false
        }
    }
}
