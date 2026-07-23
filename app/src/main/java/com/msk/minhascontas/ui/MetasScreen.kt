package com.msk.minhascontas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ProgressoCategoria
import com.msk.minhascontas.viewmodel.MetasViewModel
import java.text.NumberFormat

/**
 * Tela que exibe o progresso das metas financeiras por categoria.
 */
@Composable
fun MetasScreen(
    mes: Int,
    ano: Int,
    dia: Int,
    onCategoryClick: (tipo: Int, filtro: Int) -> Unit = { _, _ -> },
    viewModel: MetasViewModel = viewModel()
) {
    val progressos by viewModel.getProgressoMensal(mes, ano, dia).observeAsState(emptyList())

    if (progressos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.ai_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            items(progressos, key = { it.index }) { progresso ->
                ProgressoCategoriaCard(
                    progresso = progresso,
                    onClick = {
                        // Se for a categoria 8 (Investimentos/Dívidas), mostramos todos os tipos (-1)
                        // Caso contrário, focamos em Despesas (Tipo 0) filtradas pela categoria.
                        val actualTipo = if (progresso.index == 8) -1 else ContasContract.TIPO_DESPESA
                        onCategoryClick(actualTipo, progresso.index)
                    }
                )
            }
        }
    }
}

/**
 * Card que exibe o progresso individual de uma categoria.
 */
@Composable
fun ProgressoCategoriaCard(
    progresso: ProgressoCategoria,
    onClick: () -> Unit = {}
) {
    val currencyFormat = NumberFormat.getCurrencyInstance()
    
    // Calcula a porcentagem de uso do orçamento
    val porcentagem = if (progresso.valorPlanejado > 0) {
        (progresso.valorReal / progresso.valorPlanejado).toFloat()
    } else if (progresso.valorReal > 0) {
        2f // Indica que ultrapassou (pois não havia plano)
    } else {
        0f
    }
    
    val isOverBudget = progresso.valorReal > progresso.valorPlanejado && progresso.valorPlanejado > 0
    val containerColor = colorResource(progresso.corRes)
    val contentColor = colorResource(progresso.onCorRes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progresso.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Surface(
                    color = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else containerColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${(porcentagem * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { porcentagem.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else contentColor,
                trackColor = containerColor.copy(alpha = 0.4f),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.valor),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(progresso.valorReal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.total_planejado).replace(":", ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(progresso.valorPlanejado),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
