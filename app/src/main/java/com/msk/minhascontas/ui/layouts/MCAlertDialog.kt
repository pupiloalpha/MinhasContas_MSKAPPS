package com.msk.minhascontas.ui.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.ui.theme.MinhasContasDialogTheme

/**
 * Diálogo de alerta padrão unificado para o aplicativo.
 */
@Composable
fun MCAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String? = null,
    confirmLabel: String = stringResource(android.R.string.ok),
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = stringResource(R.string.cancelar),
    onDismiss: (() -> Unit)? = onDismissRequest,
    content: @Composable (() -> Unit)? = null
) {
    MinhasContasDialogTheme {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(title) },
            text = {
                if (content != null) {
                    content()
                } else if (text != null) {
                    Text(text)
                }
            },
            confirmButton = {
                if (onConfirm != null) {
                    Button(onClick = onConfirm) {
                        Text(confirmLabel)
                    }
                }
            },
            dismissButton = {
                if (dismissLabel != null && onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissLabel)
                    }
                }
            }
        )
    }
}

/**
 * Diálogo de seleção em lista unificado.
 */
@Composable
fun MCListDialog(
    onDismissRequest: () -> Unit,
    title: String,
    entries: Array<String>,
    values: Array<String>,
    currentValue: String,
    onValueSelected: (String) -> Unit
) {
    MCAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        onConfirm = null, // Sem botão de confirmação, seleciona ao clicar
        dismissLabel = stringResource(R.string.cancelar),
        onDismiss = onDismissRequest,
        content = {
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
        }
    )
}
