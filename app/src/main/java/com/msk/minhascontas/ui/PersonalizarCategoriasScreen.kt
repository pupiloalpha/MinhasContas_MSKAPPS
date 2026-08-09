package com.msk.minhascontas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.ui.layouts.MCAlertDialog
import com.msk.minhascontas.utils.LabelUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizarCategoriasScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var currentTipo by remember { mutableIntStateOf(ContasContract.TIPO_DESPESA) }
    var revertCount by remember { mutableIntStateOf(0) }

    // Lista de tipos gerenciados
    val tipos = listOf(
        ContasContract.TIPO_DESPESA,
        ContasContract.TIPO_RECEITA,
        ContasContract.TIPO_APLICACAO
    )

    // Mantém o estado dos rótulos de classes de TODOS os tipos em memória
    val allClassesLabels = remember(revertCount) {
        val map = mutableStateMapOf<Int, androidx.compose.runtime.snapshots.SnapshotStateList<String>>()

        for (tipo in tipos) {
            val arrayResId = when (tipo) {
                ContasContract.TIPO_DESPESA -> R.array.TipoDespesa
                ContasContract.TIPO_RECEITA -> R.array.TipoReceita
                ContasContract.TIPO_APLICACAO -> R.array.TipoAplicacao
                else -> 0
            }
            val size = if (arrayResId != 0) context.resources.getStringArray(arrayResId).size else 0
            val list = mutableStateListOf<String>()
            for (i in 0 until size) {
                list.add(LabelUtils.getClasseLabel(context, tipo, i))
            }
            map[tipo] = list
        }
        map
    }

    val categoryLabels = remember(revertCount) {
        val size = context.resources.getStringArray(R.array.CategoriaConta).size
        val list = mutableStateListOf<String>()
        for (i in 0 until size) {
            list.add(LabelUtils.getCategoriaLabel(context, i))
        }
        list
    }

    var showResetDialog by remember { mutableStateOf(false) }

    val primaryColor = when (currentTipo) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.azul)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.verde)
        else -> colorResource(R.color.vermelho)
    }

    val secondaryColor = when (currentTipo) {
        ContasContract.TIPO_RECEITA -> colorResource(R.color.receita_color)
        ContasContract.TIPO_APLICACAO -> colorResource(R.color.aplicacao_color)
        else -> colorResource(R.color.despesa_color)
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_personalizar_categorias)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Salva as classes de TODOS os tipos alterados
                        allClassesLabels.forEach { (tipo, labels) ->
                            labels.forEachIndexed { index, label ->
                                LabelUtils.setClasseLabel(context, tipo, index, label)
                            }
                        }
                        // Salva as categorias
                        categoryLabels.forEachIndexed { index, label ->
                            LabelUtils.setCategoriaLabel(context, index, label)
                        }

                        onSaved()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.salvar))
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Seletor de Tipo
            TipoContaSelector(
                selectedType = currentTipo,
                onTypeSelected = { currentTipo = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.dica_spinner),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Exibe a lista do tipo selecionado atualmente na tela
            val currentClassLabels = allClassesLabels[currentTipo]
            currentClassLabels?.forEachIndexed { index, label ->
                OutlinedTextField(
                    value = label,
                    onValueChange = { currentClassLabels[index] = it },
                    label = { Text("${stringResource(R.string.classe)} $index") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryColor,
                        focusedLabelColor = secondaryColor,
                        cursorColor = secondaryColor
                    )
                )
            }

            if (currentTipo == ContasContract.TIPO_DESPESA) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.titulo_categoria),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                categoryLabels.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = label,
                        onValueChange = { categoryLabels[index] = it },
                        label = { Text("${stringResource(R.string.categoria_label)} $index") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = secondaryColor,
                            focusedLabelColor = secondaryColor,
                            cursorColor = secondaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryColor),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(secondaryColor))
            ) {
                Text(stringResource(R.string.reverter_padrao).uppercase())
            }
        }
    }

    if (showResetDialog) {
        MCAlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = stringResource(R.string.confirmar_reverter_titulo),
            text = stringResource(R.string.confirmar_reverter_msg),
            confirmLabel = stringResource(R.string.sim),
            onConfirm = {
                LabelUtils.revertToDefault(context)
                revertCount++
                showResetDialog = false
            },
            dismissLabel = stringResource(R.string.nao)
        )
    }
}