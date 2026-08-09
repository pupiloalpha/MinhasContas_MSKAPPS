package com.minhascontas.app.ui.resumo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.utils.LabelUtils
import com.msk.minhascontas.viewmodel.ResumoViewModel
import com.msk.minhascontas.viewmodel.TipoFiltroResumo

@Composable
fun ResumoPorTipoScreen(
    viewModel: ResumoViewModel,
    tipoFiltro: TipoFiltroResumo,
    mes: Int,
    ano: Int,
    dia: Int = 1,
    onCardClick: (tipoResumo: String) -> Unit = {}
) {
    val state by viewModel.tipoState.collectAsState()
    val context = LocalContext.current

    // Obtém rótulos dinâmicos de classes para Despesas (Tipo 0)
    val labelDespFixa = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_FIXA)
    val labelDespVar = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_VARIAVEL)
    val labelCartao = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_CARTAO)
    val labelPrestacoes = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_PRESTACOES)

    // Obtém rótulos dinâmicos de classes para Aplicações (Tipo 2)
    val labelFundos = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_FUNDOS)
    val labelPoupanca = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_POUPANCA)
    val labelPrevidencia = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL)

    // Atualiza os filtros no ViewModel sempre que houver mudança nos parâmetros de data/filtro
    LaunchedEffect(tipoFiltro, mes, ano, dia) {
        viewModel.setFiltro(mes = mes, ano = ano, dia = dia, tipoFiltro = tipoFiltro)
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card de Receitas
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_receita_color),
                    primaryColor = colorResource(id = R.color.receita_color),
                    titulo = stringResource(R.string.linha_receita),
                    valorTotal = state.valorReceitasTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("RECEITAS") }
                ) { primaryColor ->
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_recebidas),
                        rightValue = stringResource(R.string.resumo_areceber),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorRecebido,
                        rightValue = state.valorReceber,
                        textColor = primaryColor
                    )
                }
            }

            // Card de Despesas com Rótulos Dinâmicos
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_despesa_color),
                    primaryColor = colorResource(id = R.color.despesa_color),
                    titulo = stringResource(R.string.linha_despesa),
                    valorTotal = state.valorDespesasTotal,
                    cornerRadius = 8.dp,
                    onClick = { onCardClick("DESPESAS") }
                ) { primaryColor ->
                    // Pagas vs Faltam
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_pagas),
                        rightValue = stringResource(R.string.resumo_faltam),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorDespPaga,
                        rightValue = state.valorDespPagar,
                        textColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorResource(id = R.color.cinza_claro))
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    // Fixas vs Variáveis
                    RowTwoColumns(
                        leftValue = labelDespFixa.ifBlank { stringResource(R.string.linha_despFixa) },
                        rightValue = labelDespVar.ifBlank { stringResource(R.string.linha_despVar) },
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorDespFixa,
                        rightValue = state.valorDespVar,
                        textColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Cartão vs Prestações
                    RowTwoColumns(
                        leftValue = labelCartao.ifBlank { stringResource(R.string.linha_cartao) },
                        rightValue = labelPrestacoes.ifBlank { stringResource(R.string.linha_prestacoes) },
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorCartaoCredito,
                        rightValue = state.valorPrestacoes,
                        textColor = primaryColor
                    )
                }
            }

            // Card de Aplicações com Rótulos Dinâmicos
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_aplicacao_color),
                    primaryColor = colorResource(id = R.color.aplicacao_color),
                    titulo = stringResource(R.string.linha_aplicacoes),
                    valorTotal = state.valorAplicacoesTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("APLICACOES") }
                ) { primaryColor ->
                    RowThreeColumns(
                        leftValue = labelFundos.ifBlank { stringResource(R.string.linha_fundos) },
                        centerValue = labelPoupanca.ifBlank { stringResource(R.string.linha_poupanca) },
                        rightValue = labelPrevidencia.ifBlank { stringResource(R.string.linha_previdencia) },
                        isHeader = true
                    )
                    RowThreeColumns(
                        leftValue = state.valorFundos,
                        centerValue = state.valorPoupancas,
                        rightValue = state.valorPrevidencias,
                        textColor = primaryColor
                    )
                }
            }

            // Card de Saldo
            item {
                val corDespesa = colorResource(id = R.color.despesa_color)
                val corReceita = colorResource(id = R.color.receita_color)
                val corOnSurfaceOriginal = colorResource(id = R.color.on_surface)

                // Cores dinâmicas dos valores numéricos de saldo (vermelho se negativo, verde se positivo/zero)
                val valSaldoTotal = (state.valorSaldoTotal as? Number)?.toDouble() ?: 0.0
                val corSaldoTotal = if (valSaldoTotal < 0) corDespesa else corReceita

                val valSaldoAtual = (state.valorSaldoAtual as? Number)?.toDouble() ?: 0.0
                val corSaldoAtual = if (valSaldoAtual < 0) corDespesa else corReceita

                val valSaldoAnterior = (state.valorSaldoAnterior as? Number)?.toDouble() ?: 0.0
                val corSaldoAnterior = if (valSaldoAnterior < 0) corDespesa else corReceita

                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_saldo_color),
                    primaryColor = corOnSurfaceOriginal,
                    titulo = stringResource(R.string.linha_saldo),
                    valorTotal = state.valorSaldoTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("SALDO") },
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    lineColor = corOnSurfaceOriginal,
                    totalValueColor = corSaldoTotal
                ) { _ ->
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_saldo),
                        rightValue = stringResource(R.string.resumo_mes_anterior),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorSaldoAtual,
                        rightValue = state.valorSaldoAnterior,
                        leftColor = corSaldoAtual,
                        rightColor = corSaldoAnterior
                    )
                }
            }
        }
    }
}

@Composable
fun ResumoPorCategoriaScreen(
    viewModel: ResumoViewModel,
    tipoFiltro: TipoFiltroResumo,
    mes: Int,
    ano: Int,
    dia: Int = 1,
    onCardClick: (tipoResumo: String) -> Unit = {}
) {
    val state by viewModel.categoriaState.collectAsState()
    val context = LocalContext.current

    // Obtém rótulos dinâmicos de Categorias de Despesas (índices correspondentes do banco de dados)
    val labelAlimentacao = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_ALIMENTACAO)
    val labelEducacao = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_EDUCACAO)
    val labelMoradia = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_MORADIA)
    val labelSaude = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_SAUDE)
    val labelTransporte = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_TRANSPORTE)
    val labelOutros = LabelUtils.getCategoriaLabel(context, ContasContract.CATEGORIA_OUTROS)

    // Obtém rótulos dinâmicos de classes para Aplicações
    val labelFundos = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_FUNDOS)
    val labelPoupanca = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_POUPANCA)
    val labelPrevidencia = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL)

    // Obtém rótulo dinâmico para Cartão de Crédito
    val labelCartao = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_CARTAO)

    // Atualiza os filtros no ViewModel sempre que houver mudança nos parâmetros
    LaunchedEffect(tipoFiltro, mes, ano, dia) {
        viewModel.setFiltro(mes = mes, ano = ano, dia = dia, tipoFiltro = tipoFiltro)
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Card de Receitas
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_receita_color),
                    primaryColor = colorResource(id = R.color.receita_color),
                    titulo = stringResource(R.string.linha_receita),
                    valorTotal = state.valorReceitasTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("RECEITAS") }
                ) { primaryColor ->
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_recebidas),
                        rightValue = stringResource(R.string.resumo_areceber),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorRecebido,
                        rightValue = state.valorReceber,
                        textColor = primaryColor
                    )
                }
            }

            // 2. Card de Despesas por Categorias (AJUSTADO COM NOMES DINÂMICOS)
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_despesa_color),
                    primaryColor = colorResource(id = R.color.despesa_color),
                    titulo = stringResource(R.string.linha_despesa),
                    valorTotal = state.valorDespesasTotal,
                    cornerRadius = 8.dp,
                    onClick = { onCardClick("DESPESAS") }
                ) { primaryColor ->
                    // Pagas vs Faltam
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_pagas),
                        rightValue = stringResource(R.string.resumo_faltam),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorDespPaga,
                        rightValue = state.valorDespPagar,
                        textColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorResource(id = R.color.cinza_claro))
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    // Linha 1 de Categorias: Alimentação | Educação | Moradia
                    RowThreeColumns(
                        leftValue = labelAlimentacao.ifBlank { stringResource(R.string.dica_alimentacao) },
                        centerValue = labelEducacao.ifBlank { stringResource(R.string.dica_educacao) },
                        rightValue = labelMoradia.ifBlank { stringResource(R.string.dica_moradia) },
                        isHeader = true
                    )
                    RowThreeColumns(
                        leftValue = state.valorAlimentacao,
                        centerValue = state.valorEducacao,
                        rightValue = state.valorMoradia,
                        textColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Linha 2 de Categorias: Saúde | Transporte | Outros
                    RowThreeColumns(
                        leftValue = labelSaude.ifBlank { stringResource(R.string.dica_saude) },
                        centerValue = labelTransporte.ifBlank { stringResource(R.string.dica_transporte) },
                        rightValue = labelOutros.ifBlank { stringResource(R.string.dica_outros) },
                        isHeader = true
                    )
                    RowThreeColumns(
                        leftValue = state.valorSaude,
                        centerValue = state.valorTransporte,
                        rightValue = state.valorOutros,
                        textColor = primaryColor
                    )
                }
            }

            // 3. Card de Aplicações
            item {
                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_aplicacao_color),
                    primaryColor = colorResource(id = R.color.aplicacao_color),
                    titulo = stringResource(R.string.linha_aplicacoes),
                    valorTotal = state.valorAplicacoesTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("APLICACOES") }
                ) { primaryColor ->
                    RowThreeColumns(
                        leftValue = labelFundos.ifBlank { stringResource(R.string.linha_fundos) },
                        centerValue = labelPoupanca.ifBlank { stringResource(R.string.linha_poupanca) },
                        rightValue = labelPrevidencia.ifBlank { stringResource(R.string.linha_previdencia) },
                        isHeader = true
                    )
                    RowThreeColumns(
                        leftValue = state.valorFundos,
                        centerValue = state.valorPoupancas,
                        rightValue = state.valorPrevidencias,
                        textColor = primaryColor
                    )
                }
            }

            // 4. Card de Saldo (AJUSTADO PARA O RÓTULO DE CARTÃO)
            item {
                val corDespesa = colorResource(id = R.color.despesa_color)
                val corReceita = colorResource(id = R.color.receita_color)
                val corOnSurfaceOriginal = colorResource(id = R.color.on_surface)

                val valSaldoTotal = (state.valorSaldoTotal as? Number)?.toDouble() ?: 0.0
                val corSaldoTotal = if (valSaldoTotal < 0) corDespesa else corReceita

                val valSaldoAtual = (state.valorSaldoAtual as? Number)?.toDouble() ?: 0.0
                val corSaldoAtual = if (valSaldoAtual < 0) corDespesa else corReceita

                val valSaldoAnterior = (state.valorSaldoAnterior as? Number)?.toDouble() ?: 0.0
                val corSaldoAnterior = if (valSaldoAnterior < 0) corDespesa else corReceita

                val valBanco = (state.valorBanco as? Number)?.toDouble() ?: 0.0
                val corBanco = if (valBanco > 0) corReceita else corOnSurfaceOriginal

                val valCartao = (state.valorCartaoCredito as? Number)?.toDouble() ?: 0.0
                val corCartao = if (valCartao > 0) corDespesa else corOnSurfaceOriginal

                CardBaseResumo(
                    backgroundColor = colorResource(id = R.color.card_saldo_color),
                    primaryColor = corOnSurfaceOriginal,
                    titulo = stringResource(R.string.linha_saldo),
                    valorTotal = state.valorSaldoTotal,
                    cornerRadius = 12.dp,
                    onClick = { onCardClick("SALDO") },
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    lineColor = corOnSurfaceOriginal,
                    totalValueColor = corSaldoTotal
                ) { _ ->
                    // Linha 1: Saldo Atual x Mês Anterior
                    RowTwoColumns(
                        leftValue = stringResource(R.string.resumo_saldo),
                        rightValue = stringResource(R.string.resumo_mes_anterior),
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorSaldoAtual,
                        rightValue = state.valorSaldoAnterior,
                        leftColor = corSaldoAtual,
                        rightColor = corSaldoAnterior
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorResource(id = R.color.cinza))
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    // Linha 2: Banco x Cartão de Crédito
                    RowTwoColumns(
                        leftValue = stringResource(R.string.dica_banco),
                        rightValue = labelCartao.ifBlank { stringResource(R.string.linha_cartao) },
                        isHeader = true
                    )
                    RowTwoColumns(
                        leftValue = state.valorBanco,
                        rightValue = state.valorCartaoCredito,
                        leftColor = corBanco,
                        rightColor = corCartao
                    )
                }
            }
        }
    }
}