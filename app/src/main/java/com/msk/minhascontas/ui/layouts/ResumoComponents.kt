package com.minhascontas.app.ui.resumo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

fun Double.toCurrencyFormat(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

/**
 * Formata valores numéricos em moeda respeitando o Locale e idioma atual do dispositivo.
 */
fun formatarMoeda(valor: Any?): String {
    if (valor == null) return ""
    return when (valor) {
        is Number -> {
            val formatador = NumberFormat.getCurrencyInstance(Locale.getDefault())
            formatador.format(valor.toDouble())
        }
        else -> valor.toString()
    }
}

// ============================================================================
// COMPONENTES DE LAYOUT DOS CARDS DE RESUMO
// ============================================================================

@Composable
fun CardBaseResumo(
    backgroundColor: Color,
    primaryColor: Color,
    titulo: String,
    valorTotal: Any?,
    cornerRadius: Dp,
    onClick: (() -> Unit)? = null,
    titleColor: Color = primaryColor,
    lineColor: Color = primaryColor,
    totalValueColor: Color = primaryColor,
    content: @Composable (primaryColor: Color) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    modifier = Modifier.weight(1.4f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = formatarMoeda(valorTotal),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = totalValueColor,
                    modifier = Modifier.weight(1.3f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(lineColor)
            )

            content(primaryColor)
        }
    }
}

@Composable
fun RowTwoColumns(
    leftValue: Any?,
    rightValue: Any?,
    isHeader: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    leftColor: Color = textColor,
    rightColor: Color = textColor
) {
    val textStyle = MaterialTheme.typography.bodyMedium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatarMoeda(leftValue),
            style = textStyle,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else leftColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            text = formatarMoeda(rightValue),
            style = textStyle,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else rightColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RowThreeColumns(
    leftValue: Any?,
    centerValue: Any?,
    rightValue: Any?,
    isHeader: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val textStyle = MaterialTheme.typography.bodyMedium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatarMoeda(leftValue),
            style = textStyle,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else textColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            text = formatarMoeda(centerValue),
            style = textStyle,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else textColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = formatarMoeda(rightValue),
            style = textStyle,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else textColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}