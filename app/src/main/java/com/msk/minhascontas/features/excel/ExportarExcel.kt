package com.msk.minhascontas.features.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.MetaFinanceira
import com.msk.minhascontas.utils.LabelUtils
import org.apache.poi.ss.usermodel.CreationHelper
import java.util.Locale
import org.apache.poi.ss.usermodel.DataFormat
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.io.OutputStream

// Apache POI Imports
class ExportarExcel {
    // ELEMENTOS QUE ESCREVEM O ARQUIVO EM EXCEL
    private var arquivoExcel: XSSFWorkbook? = null
    private var planilhaResumo: XSSFSheet? = null // Renamed for clarity (previously 'planilha')

    // FONTE PARA TEXTO
    private var arial10font: XSSFFont? = null
    private var times16font: XSSFFont? = null

    // FORMATO DE NUMERO
    private var dataFormat: DataFormat? = null // Used to get custom number formats

    // FORMATADORES DE CELULAS COM TEXTO, DATA e NUMEROS
    private var integerFormat: XSSFCellStyle? = null
    private var arial10format: XSSFCellStyle? = null
    private var decDuasCasasFontFormat: XSSFCellStyle? = null
    private var titleFormat: XSSFCellStyle? = null

    // Nomes das colunas da aba DADOS, para uso interno e centralização
    private val colunasDados = arrayOf<String?>(
        "ID", "Nome", "Tipo", "Classe", "Categoria", "Dia", "Mês", "Ano",
        "Valor", "Status", "Qt. Repet.", "N. Repet.", "Intervalo", "Código", "Juros"
    )

    // INFORMACOES QUE ALIMENTAM AS PLANILHAS
    private var erro = 0

    /**
     * Novo método principal de exportação para Excel.
     * Recebe os dados já processados (Resumo e Detalhado) e gerencia a criação das duas abas.
     *
     * @param context Contexto da aplicação.
     * @param outputUri URI de saída (onde o arquivo será gravado).
     * @param resumoLinhas Array de Strings com os nomes das linhas do Resumo.
     * @param resumoValores Array de Strings com os valores calculados do Resumo.
     * @param contasDetalhada Lista completa de contas para a aba DADOS.
     * @return 0 se sucesso, > 0 se erro.
     */
    fun CriaExcel(
        context: Context, outputUri: Uri?, resumoLinhas: Array<String?>,
        resumoValores: Array<String?>, contasDetalhada: MutableList<Conta>,
        metas: List<MetaFinanceira>
    ): Int {
        erro = 0 // Reseta o contador de erros
        var os: OutputStream? = null // Initialize OutputStream outside try to ensure it's closed

        try {
            // 1. Cria o XSSFWorkbook
            os = context.contentResolver.openOutputStream(outputUri!!)
            arquivoExcel = XSSFWorkbook()

            // 2. Configura os formatos de célula
            ConfiguraFormatos()

            // 3. Cria e preenche a Aba RESUMO
            planilhaResumo = arquivoExcel!!.createSheet("RESUMO") // Cria a aba RESUMO
            EscreveNomeColunas(
                colunasDados,
                resumoLinhas
            ) // Escreve os títulos das linhas e o cabeçalho "Valor"
            EscrevePlanilha(1, resumoValores) // Escreve os valores na Coluna 1 (B)

            // 3b. Cria e preenche a Aba RESUMO DETALHADO (Classes e Categorias)
            val planilhaResumoDet = arquivoExcel!!.createSheet("RESUMO_ANALISE")
            EscreveResumoCategorias(planilhaResumoDet, contasDetalhada, context)

            // 4. Cria e preenche a Aba METAS
            val planilhaMetas = arquivoExcel!!.createSheet("METAS")
            EscreveMetas(planilhaMetas, metas)
            EscrevePlanejamento(planilhaMetas, context, contasDetalhada)

            // 5. Cria e preenche a Aba DADOS (Original para Importação)
            val planilhaDados = arquivoExcel!!.createSheet("DADOS")
            EscreveDadosDetalhado(planilhaDados, contasDetalhada)

            // 6. Aba DADOS FORMATADOS (Para análise do usuário)
            val planilhaFormatada = arquivoExcel!!.createSheet("DADOS_ANALISE")
            EscreveDadosAnalise(planilhaFormatada, contasDetalhada, context)

            // 7. Escreve e Fecha o arquivo
            if (erro == 0) {
                arquivoExcel!!.write(os)
            }
            arquivoExcel!!.close()
        } catch (e: IOException) {
            Log.e("ExportarExcel", "Erro de I/O ao criar o arquivo: " + e.message)
            erro = erro + 1
        } catch (e: Exception) {
            Log.e("ExportarExcel", "Erro desconhecido em CriaExcel: " + e.message)
            erro = erro + 1
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    Log.e("ExportarExcel", "Erro ao fechar OutputStream: " + e.message)
                }
            }
        }
        return erro
    }

    /**
     * Define os XSSFFont e XSSFCellStyle que serão usados para
     * a formatação das células no Excel.
     */
    private fun ConfiguraFormatos() {
        try {
            // Criação do CreationHelper para DataFormat
            val createHelper: CreationHelper = arquivoExcel!!.getCreationHelper()
            dataFormat = createHelper.createDataFormat()

            // Fontes
            arial10font = arquivoExcel!!.createFont()
            arial10font!!.fontHeightInPoints = 10.toShort()
            arial10font!!.setFontName("Arial")

            times16font = arquivoExcel!!.createFont()
            times16font!!.fontHeightInPoints = 16.toShort()
            times16font!!.setFontName("Times New Roman") // Equivalente a Times no JXL
            times16font!!.bold = true

            // Formatadores de Célula
            arial10format = arquivoExcel!!.createCellStyle() // Texto Simples
            arial10format!!.setFont(arial10font)
            arial10format!!.setWrapText(true) // Permite quebra de linha
            arial10format!!.setAlignment(HorizontalAlignment.LEFT)

            integerFormat = arquivoExcel!!.createCellStyle() // Inteiro
            integerFormat!!.setFont(arial10font)
            integerFormat!!.setAlignment(HorizontalAlignment.LEFT)
            integerFormat!!.dataFormat = dataFormat!!.getFormat("0") // Formato para inteiro

            decDuasCasasFontFormat = arquivoExcel!!.createCellStyle() // Valor R$
            decDuasCasasFontFormat!!.setFont(arial10font)
            decDuasCasasFontFormat!!.setAlignment(HorizontalAlignment.LEFT)
            decDuasCasasFontFormat!!.dataFormat = dataFormat!!.getFormat("#,##0.00") // Formato para 2 casas decimais

            titleFormat = arquivoExcel!!.createCellStyle()
            titleFormat!!.setFont(times16font)
            titleFormat!!.setAlignment(HorizontalAlignment.LEFT)

            Log.i("ExportarExcel", "Formatos de célula configurados.")
        } catch (e: Exception) { // Generalize exception, POI specific exceptions are usually RuntimeException or IOException
            Log.e("ExportarExcel", "Erro ao configurar formatos: " + e.message)
        }
    }

    /**
     * Preenche a planilha de DADOS com todos os registros de contas detalhados.
     * Utiliza o array de Strings 'cabecalhos' para os títulos das colunas
     * e itera sobre a lista de POJOs Conta para preencher o conteúdo.
     *
     * @param planilhaDados O objeto XSSFSheet para a aba DADOS.
     * @param contas Lista de objetos Conta a serem escritos.
     */
    private fun EscreveDadosDetalhado(planilhaDados: XSSFSheet, contas: MutableList<Conta>) {
        // 1. Escreve os cabeçalhos das colunas (Linha 0)

        val headerRow: Row = planilhaDados.createRow(0) // Cria a linha de cabeçalho
        for (i in colunasDados.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(colunasDados[i])
            cell.cellStyle = arial10format // Aplica formato de texto simples
        }

        // 2. Escreve os dados das contas (A partir da Linha 1)
        for (rowNum in contas.indices) {
            val conta = contas.get(rowNum)
            val dataRow: Row =
                planilhaDados.createRow(rowNum + 1) // Cria uma nova linha para cada conta
            var col = 0

            // Coluna 0: ID
            val idCell = dataRow.createCell(col++)
            idCell.setCellValue(conta.idConta.toDouble())
            idCell.cellStyle = integerFormat

            // Coluna 1: Nome
            val nomeCell = dataRow.createCell(col++)
            nomeCell.setCellValue(conta.nome)
            nomeCell.cellStyle = arial10format

            // Coluna 2: Tipo
            val tipoCell = dataRow.createCell(col++)
            tipoCell.setCellValue(conta.tipo.toDouble())
            tipoCell.cellStyle = integerFormat

            // Coluna 3: Classe
            val classeCell = dataRow.createCell(col++)
            classeCell.setCellValue(conta.classeConta.toDouble())
            classeCell.cellStyle = integerFormat

            // Coluna 4: Categoria
            val categoriaCell = dataRow.createCell(col++)
            categoriaCell.setCellValue(conta.categoria.toDouble())
            categoriaCell.cellStyle = integerFormat

            // Coluna 5, 6, 7: Data (Dia, Mês, Ano)
            val diaCell = dataRow.createCell(col++)
            diaCell.setCellValue(conta.dia.toDouble())
            diaCell.cellStyle = integerFormat

            val mesCell = dataRow.createCell(col++)
            mesCell.setCellValue(conta.mes.toDouble())
            mesCell.cellStyle = integerFormat

            val anoCell = dataRow.createCell(col++)
            anoCell.setCellValue(conta.ano.toDouble())
            anoCell.cellStyle = integerFormat

            // Coluna 8: Valor
            val valorCell = dataRow.createCell(col++)
            valorCell.setCellValue(conta.valor)
            valorCell.cellStyle = decDuasCasasFontFormat

            // Coluna 9: Status (paguei/falta)
            val statusCell = dataRow.createCell(col++)
            statusCell.setCellValue(conta.pagamento)
            statusCell.cellStyle = arial10format

            // Colunas 10, 11, 12: Repetição e Intervalo
            val qtRepeteCell = dataRow.createCell(col++)
            qtRepeteCell.setCellValue(conta.qtRepete.toDouble())
            qtRepeteCell.cellStyle = integerFormat

            val nRepeteCell = dataRow.createCell(col++)
            nRepeteCell.setCellValue(conta.nRepete.toDouble())
            nRepeteCell.cellStyle = integerFormat

            val intervaloCell = dataRow.createCell(col++)
            intervaloCell.setCellValue(conta.intervalo.toDouble())
            intervaloCell.cellStyle = integerFormat

            // Coluna 13: Código
            val codigoCell = dataRow.createCell(col++)
            codigoCell.setCellValue(conta.codigo)
            codigoCell.cellStyle = arial10format

            // Coluna 14: Juros
            val jurosCell = dataRow.createCell(col)
            jurosCell.setCellValue(conta.valorJuros)
            jurosCell.cellStyle = decDuasCasasFontFormat
        }

        Log.i("ExportarExcel", "Aba DADOS escrita com " + contas.size + " registros.")
    }

    /**
     * Escreve uma aba com dados formatados e nomes amigáveis para análise do usuário.
     */
    private fun EscreveDadosAnalise(planilhaAnalise: XSSFSheet, contas: List<Conta>, context: Context) {
        val cabecalhos = arrayOf(
            "ID", "Nome", "Tipo", "Classe", "Categoria", "Data",
            "Valor", "Status", "Qt. Repet.", "N. Repet.", "Intervalo", "Código", "Juros"
        )

        val headerRow: Row = planilhaAnalise.createRow(0)
        for (i in cabecalhos.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(cabecalhos[i])
            cell.cellStyle = arial10format
        }

        for (rowNum in contas.indices) {
            val conta = contas[rowNum]
            val dataRow: Row = planilhaAnalise.createRow(rowNum + 1)
            var col = 0

            dataRow.createCell(col++).apply { setCellValue(conta.idConta.toDouble()); cellStyle = integerFormat }
            dataRow.createCell(col++).apply { setCellValue(conta.nome); cellStyle = arial10format }

            val tipoNome = when (conta.tipo) {
                0 -> "Despesa"
                1 -> "Receita"
                2 -> "Aplicação"
                else -> "Outros"
            }
            dataRow.createCell(col++).apply { setCellValue(tipoNome); cellStyle = arial10format }

            dataRow.createCell(col++).apply {
                setCellValue(LabelUtils.getClasseLabel(context, conta.tipo, conta.classeConta))
                cellStyle = arial10format
            }

            dataRow.createCell(col++).apply {
                setCellValue(LabelUtils.getCategoriaLabel(context, conta.categoria))
                cellStyle = arial10format
            }

            val dataStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", conta.dia, conta.mes, conta.ano)
            dataRow.createCell(col++).apply { setCellValue(dataStr); cellStyle = arial10format }

            dataRow.createCell(col++).apply { setCellValue(conta.valor); cellStyle = decDuasCasasFontFormat }
            dataRow.createCell(col++).apply { setCellValue(conta.pagamento); cellStyle = arial10format }
            dataRow.createCell(col++).apply { setCellValue(conta.qtRepete.toDouble()); cellStyle = integerFormat }
            dataRow.createCell(col++).apply { setCellValue(conta.nRepete.toDouble()); cellStyle = integerFormat }
            dataRow.createCell(col++).apply { setCellValue(conta.intervalo.toDouble()); cellStyle = integerFormat }
            dataRow.createCell(col++).apply { setCellValue(conta.codigo); cellStyle = arial10format }
            dataRow.createCell(col++).apply { setCellValue(conta.valorJuros); cellStyle = decDuasCasasFontFormat }
        }
    }

    /**
     * Escreve a aba de Metas Financeiras.
     */
    private fun EscreveMetas(planilhaMetas: XSSFSheet, metas: List<MetaFinanceira>) {
        val titleRow0 = planilhaMetas.createRow(0)
        titleRow0.createCell(0).apply { setCellValue("METAS ESTRATÉGICAS (COACH)"); cellStyle = titleFormat }

        val cabecalhos = arrayOf("Nome", "Tipo", "Objetivo", "Realizado", "Progresso (%)", "Status")
        val headerRow = planilhaMetas.createRow(1)
        for (i in cabecalhos.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(cabecalhos[i])
            cell.cellStyle = arial10format
        }

        for (i in metas.indices) {
            val meta = metas[i]
            val row = planilhaMetas.createRow(i + 2)
            var col = 0

            row.createCell(col++).apply { setCellValue(meta.nome); cellStyle = arial10format }

            val tipoStr = when (meta.tipoMeta) {
                0 -> "Dívida"
                1 -> "Reserva"
                2 -> "Investimento"
                3 -> "Aposentadoria"
                else -> "Outros"
            }
            row.createCell(col++).apply { setCellValue(tipoStr); cellStyle = arial10format }
            row.createCell(col++).apply { setCellValue(meta.valorObjetivo); cellStyle = decDuasCasasFontFormat }
            row.createCell(col++).apply { setCellValue(meta.valorAtual); cellStyle = decDuasCasasFontFormat }

            val progresso = if (meta.valorObjetivo > 0) (meta.valorAtual / meta.valorObjetivo) * 100 else 0.0
            row.createCell(col++).apply { setCellValue(progresso); cellStyle = decDuasCasasFontFormat }

            row.createCell(col++).apply { setCellValue(if (meta.ativa) "Ativa" else "Inativa"); cellStyle = arial10format }
        }
    }

    /**
     * Escreve o planejamento financeiro por categorias (Orçamento).
     */
    private fun EscrevePlanejamento(planilha: XSSFSheet, context: Context, contas: List<Conta>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val receitaReferencia = prefs.getFloat("plan_receita_referencia", 0.0f).toDouble()

        var rowNum = planilha.lastRowNum + 2
        planilha.createRow(rowNum++).createCell(0).apply { setCellValue("PLANEJAMENTO FINANCEIRO (ORÇAMENTO)"); cellStyle = titleFormat }

        val header = planilha.createRow(rowNum++)
        header.createCell(0).apply { setCellValue("Categoria"); cellStyle = arial10format }
        header.createCell(1).apply { setCellValue("Planejado (%)"); cellStyle = arial10format }
        header.createCell(2).apply { setCellValue("Valor Planejado"); cellStyle = arial10format }
        header.createCell(3).apply { setCellValue("Valor Real"); cellStyle = arial10format }

        // Agrega gastos reais do mês
        val gastosReais = mutableMapOf<Int, Double>()
        for (conta in contas) {
            if (conta.tipo == 0) {
                gastosReais[conta.categoria] = (gastosReais[conta.categoria] ?: 0.0) + conta.valor
            } else if (conta.tipo == 2) {
                gastosReais[8] = (gastosReais[8] ?: 0.0) + conta.valor // Category 8 is Investments
            }
        }

        for (i in 0..8) {
            val perc = prefs.getFloat("plan_perc_$i", -1.0f).toDouble()
            val defaultPerc = when(i) {
                0 -> 15.0; 1 -> 10.0; 2 -> 10.0; 3 -> 25.0; 4 -> 5.0; 5 -> 5.0; 6 -> 5.0; 7 -> 5.0; 8 -> 20.0
                else -> 0.0
            }
            val percFinal = if (perc >= 0) perc else defaultPerc
            
            val valorPlanejado = (percFinal / 100.0) * receitaReferencia
            val valorReal = gastosReais[i] ?: 0.0

            val row = planilha.createRow(rowNum++)
            row.createCell(0).setCellValue(LabelUtils.getCategoriaLabel(context, i))
            row.createCell(1).apply { setCellValue(percFinal); cellStyle = decDuasCasasFontFormat }
            row.createCell(2).apply { setCellValue(valorPlanejado); cellStyle = decDuasCasasFontFormat }
            row.createCell(3).apply { setCellValue(valorReal); cellStyle = decDuasCasasFontFormat }
        }
    }

    /**
     * Cria um resumo detalhado por categorias e classes.
     */
    private fun EscreveResumoCategorias(planilha: XSSFSheet, contas: List<Conta>, context: Context) {
        val categorias = mutableMapOf<Int, Double>()
        val classes = mutableMapOf<String, Double>() // Key: tipo_classe

        for (conta in contas) {
            if (conta.tipo == 0) { // Categorias geralmente interessam para despesas
                categorias[conta.categoria] = (categorias[conta.categoria] ?: 0.0) + conta.valor
            }
            val keyClasse = "${conta.tipo}_${conta.classeConta}"
            classes[keyClasse] = (classes[keyClasse] ?: 0.0) + conta.valor
        }

        var rowNum = 0

        // Seção Categorias
        val titleRow = planilha.createRow(rowNum++)
        titleRow.createCell(0).apply { setCellValue("RESUMO POR CATEGORIA (DESPESAS)"); cellStyle = titleFormat }
        
        val headCat = planilha.createRow(rowNum++)
        headCat.createCell(0).apply { setCellValue("Categoria"); cellStyle = arial10format }
        headCat.createCell(1).apply { setCellValue("Total"); cellStyle = arial10format }

        for (i in 0..8) {
            val total = categorias[i] ?: 0.0
            val row = planilha.createRow(rowNum++)
            row.createCell(0).setCellValue(LabelUtils.getCategoriaLabel(context, i))
            row.createCell(1).apply { setCellValue(total); cellStyle = decDuasCasasFontFormat }
        }

        rowNum += 2 // Espaço

        // Seção Classes
        planilha.createRow(rowNum++).createCell(0).apply { setCellValue("RESUMO POR CLASSES"); cellStyle = titleFormat }
        val tipos = arrayOf("DESPESAS", "RECEITAS", "APLICAÇÕES")
        for (t in 0..2) {
            planilha.createRow(rowNum++).createCell(0).apply { setCellValue(tipos[t]); cellStyle = titleFormat }
            for (c in 0..3) {
                val total = classes["${t}_$c"] ?: 0.0
                if (total > 0 || c == 0) {
                    val row = planilha.createRow(rowNum++)
                    row.createCell(0).setCellValue(LabelUtils.getClasseLabel(context, t, c))
                    row.createCell(1).apply { setCellValue(total); cellStyle = decDuasCasasFontFormat }
                }
            }
            rowNum++
        }
    }

    /**
     * Escreve os rótulos das linhas (nomes dos itens do resumo) na Coluna A e o cabeçalho "Valor" na Coluna B (para a aba RESUMO).
     * O parâmetro `colunas` (que é `colunasDados`) é ignorado para a geração de cabeçalhos no RESUMO,
     * pois a estrutura esperada é de "Item" na Coluna A e "Valor" na Coluna B.
     *
     * @param colunas Array de strings (originalmente `colunasDados`, mas ignorado aqui para cabeçalhos do RESUMO).
     * @param linhas Array de strings com os nomes das linhas do resumo (`resumoLinhas`).
     */
    private fun EscreveNomeColunas(colunas: Array<String?>?, linhas: Array<String?>) {
        // ESCREVE O ROTULO DE CADA COLUNA E LINHA DA PLANILHA (RESUMO)

        erro = 0

        try {
            // NOME DO ROTULO DA COLUNA DE VALORES NA ABA RESUMO (Coluna B, linha 0)
            var headerRow: Row? = planilhaResumo!!.getRow(0)
            if (headerRow == null) {
                headerRow = planilhaResumo!!.createRow(0)
            }
            val valueHeaderCell = headerRow.createCell(1) // Coluna B (index 1), linha 0
            valueHeaderCell.setCellValue("Valor") // Cabeçalho apropriado para a coluna de valores do resumo
            valueHeaderCell.cellStyle = arial10format

            Log.i("Excel Format", "Escreveu o nome da coluna para valores do resumo.")

            // NOME DOS ROTULOS DAS LINHAS DA PLANILHA (RESUMO) (Coluna A, a partir da linha 1)
            for (i in linhas.indices) {
                var dataRow: Row? = planilhaResumo!!.getRow(i + 1) // Pega a linha existente ou cria
                if (dataRow == null) {
                    dataRow = planilhaResumo!!.createRow(i + 1)
                }
                val labelCell = dataRow.createCell(0) // Coluna A (index 0), linha i+1
                labelCell.setCellValue(linhas[i])
                labelCell.cellStyle = arial10format
            }

            Log.i("Excel Format", "Escreveu o nome das linhas do resumo.")
        } catch (e: Exception) {
            Log.e("ExportarExcel", "Erro em EscreveNomeColunas (RESUMO): " + e.message)
            erro = erro + 1
        }
    }

    /**
     * Escreve os valores do resumo na coluna especificada (geralmente Coluna B para a aba RESUMO).
     * Tenta formatar os valores como números com 2 casas decimais se forem numéricos.
     *
     * @param nrColuna O número da coluna onde os valores serão escritos.
     * @param valor Array de Strings com os valores a serem escritos.
     */
    private fun EscrevePlanilha(nrColuna: Int, valor: Array<String?>) {
        // -------- ESCREVENDO NAS CELULAS DA PLANILHA (RESUMO) ------

        erro = 0
        try {
            for (i in valor.indices) {
                var dataRow: Row? = planilhaResumo!!.getRow(i + 1) // Pega a linha existente ou cria
                if (dataRow == null) {
                    dataRow = planilhaResumo!!.createRow(i + 1)
                }
                val contentCell = dataRow.createCell(nrColuna) // Escreve na coluna especificada

                // Tenta converter para double para aplicar formato numérico
                try {
                    // Substitui vírgula por ponto para parsing, comum em formatos numéricos brasileiros
                    val doubleValue = valor[i]!!.replace(",", ".").toDouble()
                    contentCell.setCellValue(doubleValue)
                    contentCell.cellStyle = decDuasCasasFontFormat
                } catch (e: NumberFormatException) {
                    // Se não for um número válido, escreve como texto
                    contentCell.setCellValue(valor[i])
                    contentCell.cellStyle = arial10format
                }
            }

            Log.i("Excel Format", "Escreveu os valores do resumo na coluna: " + nrColuna)
        } catch (e: Exception) {
            Log.e("ExportarExcel", "Erro em EscrevePlanilha (col " + nrColuna + "): " + e.message)
            erro = erro + 1
        }
    }
}