package com.msk.minhascontas.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
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
    
    // States for labels
    val classLabels = remember(currentTipo, revertCount) {
        val arrayResId = when (currentTipo) {
            ContasContract.TIPO_DESPESA -> R.array.TipoDespesa
            ContasContract.TIPO_RECEITA -> R.array.TipoReceita
            ContasContract.TIPO_APLICACAO -> R.array.TipoAplicacao
            else -> 0
        }
        val size = if (arrayResId != 0) context.resources.getStringArray(arrayResId).size else 0
        val list = mutableStateListOf<String>()
        for (i in 0 until size) {
            list.add(LabelUtils.getClasseLabel(context, currentTipo, i))
        }
        list
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
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_personalizar_categorias)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Salva classes
                        classLabels.forEachIndexed { index, label ->
                            LabelUtils.setClasseLabel(context, currentTipo, index, label)
                        }
                        // Salva categorias (apenas para despesa, como no original)
                        if (currentTipo == ContasContract.TIPO_DESPESA) {
                            categoryLabels.forEachIndexed { index, label ->
                                LabelUtils.setCategoriaLabel(context, index, label)
                            }
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
            // Seletor de Tipo Modernizado
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
            
            classLabels.forEachIndexed { index, label ->
                OutlinedTextField(
                    value = label,
                    onValueChange = { classLabels[index] = it },
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
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.confirmar_reverter_titulo)) },
            text = { Text(stringResource(R.string.confirmar_reverter_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        LabelUtils.revertToDefault(context)
                        revertCount++
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = secondaryColor)
                ) {
                    Text(stringResource(R.string.sim))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = secondaryColor)
                ) {
                    Text(stringResource(R.string.nao))
                }
            }
        )
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
