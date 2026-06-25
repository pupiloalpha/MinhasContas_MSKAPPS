package com.msk.minhascontas.features.excel

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.msk.minhascontas.features.ai.TransactionClassifier
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasContract.Colunas
import org.apache.poi.ss.usermodel.*
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.iterator
import kotlin.math.abs

/**
 * Classe de serviço responsável pela leitura, validação e conversão de dados
 * de um arquivo Excel (.xls ou .xlsx) para uma lista de objetos Conta.
 */
class ImportarExcel {

    internal class ColumnMapping {
        enum class ImportMode {
            FULL_DB_MATCH,
            BASIC_MATCH,
            INVALID
        }

        var mode: ImportMode = ImportMode.INVALID
        var headerRowIndex: Int = -1
        val colIndexMap: MutableMap<String, Int> = HashMap()

        // Armazena colunas extras de texto para concatenação
        val textCols: MutableList<Int> = mutableListOf()
    }

    private val ALIASES_NOME = listOf("NOME", "DESCRICAO", "HISTORICO", "DETALHE", "CONTA", "NOME_CONTA", "ITEM", "TRANSACAO", "DESC", "NOME DA CONTA", "HISTORY", "NAME", "DESCRIPTION")
    private val ALIASES_VALOR = listOf("VALOR", "PRECO", "QUANTIA", "VALOR_CONTA", "MONTANTE", "TOTAL", "VAL", "IMPORTE", "VALOR TRANSACAO", "AMOUNT", "VALUE", "PRICE", "COST")
    private val ALIASES_DATA = listOf("DATA", "VENCIMENTO", "EMISSAO", "DIA", "DATA_CONTA", "FECHA", "DT VENC", "DATA MOV", "MOVIMENTACAO", "DATE", "WHEN", "TIMESTAMP")
    private val ALIASES_STATUS = listOf("STATUS", "PAGO", "PAGUEI", "SITUACAO", "PAGAMENTO", "PAID", "STATE")

    suspend fun lerExcel(
        context: Context,
        uri: Uri?,
        onProgress: ((Int, Int) -> Unit)? = null
    ): MutableList<Conta>? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null
        var inputStream: InputStream? = null
        var workbook: Workbook? = null
        val todasContas = mutableListOf<Conta>()

        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)

            // Identifica se é um arquivo virtual (como Google Sheets)
            val isVirtual = mimeType == "application/vnd.google-apps.spreadsheet" ||
                    (DocumentsContract.isDocumentUri(context, uri) && isVirtualFile(context, uri))

            inputStream = if (isVirtual) {
                try {
                    // Tenta exportar o arquivo virtual do Google Drive para o formato XLSX
                    contentResolver.openTypedAssetFileDescriptor(
                        uri,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        null
                    )?.createInputStream()
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao exportar para XLSX, tentando XLS legado: ${e.message}")
                    try {
                        // Fallback para o formato XLS (Excel 97-2003) se o XLSX falhar
                        contentResolver.openTypedAssetFileDescriptor(
                            uri,
                            "application/vnd.ms-excel",
                            null
                        )?.createInputStream()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Não foi possível exportar o arquivo do Google Sheets: ${e2.message}")
                        null
                    }
                }
            } else {
                try {
                    contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao abrir stream de arquivo comum: ${e.message}")
                    null
                }
            }

            if (inputStream == null) {
                Log.e(TAG, "InputStream nulo para o arquivo: $uri")
                return@withContext null
            }

            // Envolve em BufferedInputStream para suportar mark/reset exigido pelo POI em alguns streams
            val bis = BufferedInputStream(inputStream)
            workbook = WorkbookFactory.create(bis)

            val totalSheets = workbook.numberOfSheets
            var totalLinhasGlobal = 0

            for (i in 0 until totalSheets) {
                totalLinhasGlobal += workbook.getSheetAt(i).lastRowNum + 1
            }

            var processadasGlobal = 0

            for (i in 0 until totalSheets) {
                val sheet = workbook.getSheetAt(i)
                val sheetNameNorm = normalizarHeader(sheet.sheetName)

                // Pular aba de RESUMO gerada pelo ExportarExcel
                if (sheetNameNorm == "RESUMO") {
                    processadasGlobal += sheet.lastRowNum + 1
                    continue
                }

                val contasDaAba = processarPlanilha(context, sheet) {
                    processadasGlobal++
                    onProgress?.invoke(processadasGlobal, totalLinhasGlobal)
                }

                if (!contasDaAba.isNullOrEmpty()) {
                    todasContas.addAll(contasDaAba)
                }
            }

            if (todasContas.isEmpty()) return@withContext null
            return@withContext todasContas
        } catch (e: Exception) {
            Log.e(TAG, "Erro na importação de Excel: ${e.message}", e)
            return@withContext null
        } finally {
            try { workbook?.close(); inputStream?.close() } catch (e: Exception) {}
        }
    }

    private suspend fun processarPlanilha(
        context: Context,
        sheet: Sheet,
        onRowProcessed: (() -> Unit)? = null
    ): List<Conta>? = withContext(Dispatchers.IO) {
        val mapping = identificarColunas(sheet)
        if (mapping.mode == ColumnMapping.ImportMode.INVALID) {
            repeat(sheet.lastRowNum + 1) { onRowProcessed?.invoke() }
            return@withContext null
        }

        val startRow = mapping.headerRowIndex + 1
        repeat(mapping.headerRowIndex + 1) { onRowProcessed?.invoke() }

        val totalRows = sheet.lastRowNum - startRow + 1
        if (totalRows <= 0) return@withContext null

        val classifier = TransactionClassifier(context)
        val contas = mutableListOf<Conta>()
        val sheetNameNorm = normalizarHeader(sheet.sheetName)

        for (i in startRow..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: run {
                onRowProcessed?.invoke()
                continue
            }

            val colNomeIdx = mapping.colIndexMap[Colunas.COLUNA_NOME_CONTA] ?: -1
            val colValorIdx = mapping.colIndexMap[Colunas.COLUNA_VALOR_CONTA] ?: -1

            // Concatenação de múltiplas colunas de texto para enriquecer a descrição
            val nomeParts = mutableListOf<String>()
            if (colNomeIdx != -1) {
                val cell = if (colNomeIdx >= 0) row.getCell(colNomeIdx) else null
                val valBase = getCellValueAsString(cell).trim()
                if (valBase.isNotEmpty()) nomeParts.add(valBase)
            }
            mapping.textCols.forEach { idx ->
                if (idx != colNomeIdx && idx >= 0) {
                    val part = getCellValueAsString(row.getCell(idx)).trim()
                    if (part.isNotEmpty() && !nomeParts.contains(part)) nomeParts.add(part)
                }
            }
            val nome = nomeParts.joinToString(" - ").take(150)

            val valorCell = if (colValorIdx >= 0) row.getCell(colValorIdx) else null
            val valorStr = getCellValueAsString(valorCell)
            val valorNum = parseDouble(valorStr, 0.0)

            var dataCalendar: Calendar? = if (mapping.mode == ColumnMapping.ImportMode.FULL_DB_MATCH) {
                val dia = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_DIA_DATA_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_DIA_DATA_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 1
                val mes = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_MES_DATA_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_MES_DATA_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 1
                val ano = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_ANO_DATA_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_ANO_DATA_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 2000
                Calendar.getInstance().apply { set(ano, mes - 1, dia) }
            } else {
                getDateFromCell(sheet, mapping.colIndexMap["DATA_COMPLETA"], i) ?: Calendar.getInstance()
            }

            if (nome.isBlank() || valorNum == 0.0 || dataCalendar == null) {
                onRowProcessed?.invoke()
                continue
            }

            val sinalNegativo = valorNum < 0
            val valorAbsoluto = abs(valorNum)

            var tipoFinal = -1
            var classeFinal = -1
            var categoriaFinal = -1
            var statusFinal = ContasContract.STATUS_PENDENTE
            var qtRepeteFinal = 1
            var nRepeteFinal = 1
            var intervaloFinal = 0
            var codigoFinal = UUID.randomUUID().toString()
            var jurosFinal = 0.0

            var idFinal = 0L

            if (mapping.mode == ColumnMapping.ImportMode.FULL_DB_MATCH) {
                idFinal = getCellValueAsString(if ((mapping.colIndexMap["_id"] ?: -1) >= 0) row.getCell(mapping.colIndexMap["_id"]!!) else null).toDoubleOrNull()?.toLong() ?: 0L
                tipoFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_TIPO_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_TIPO_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: -1
                classeFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_CLASSE_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_CLASSE_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: -1
                categoriaFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_CATEGORIA_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_CATEGORIA_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: -1
                statusFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_PAGOU_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_PAGOU_CONTA]!!) else null)
                qtRepeteFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_QT_REPETICOES_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_QT_REPETICOES_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 1
                nRepeteFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_NR_REPETICAO_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_NR_REPETICAO_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 1
                intervaloFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_INTERVALO_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_INTERVALO_CONTA]!!) else null).toDoubleOrNull()?.toInt() ?: 0
                codigoFinal = getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_CODIGO_CONTA] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_CODIGO_CONTA]!!) else null).ifBlank { UUID.randomUUID().toString() }
                jurosFinal = parseDouble(getCellValueAsString(if ((mapping.colIndexMap[Colunas.COLUNA_VALOR_JUROS] ?: -1) >= 0) row.getCell(mapping.colIndexMap[Colunas.COLUNA_VALOR_JUROS]!!) else null), 0.0)
            }

            // Inferência lógica por Sinais, Aba e Palavras-Chave
            if (tipoFinal == -1 || mapping.mode != ColumnMapping.ImportMode.FULL_DB_MATCH) {
                val infoInferida = inferirTipoeClasse(nome, sheetNameNorm, sinalNegativo)
                tipoFinal = infoInferida.first

                if (classeFinal == -1) classeFinal = infoInferida.second

                val colStatusIdx = mapping.colIndexMap[Colunas.COLUNA_PAGOU_CONTA] ?: -1
                if (colStatusIdx != -1) {
                    val cell = if (colStatusIdx >= 0) row.getCell(colStatusIdx) else null
                    val statusRaw = getCellValueAsString(cell).lowercase()
                    statusFinal = if (statusRaw.matches(Regex(".*(pago|sim|yes|ok|paguei|recebido).*"))) {
                        ContasContract.STATUS_PAGO_RECEBIDO
                    } else {
                        ContasContract.STATUS_PENDENTE
                    }
                }
            }

            // Fallback para o classificador IA se a categoria ainda for desconhecida
            if (categoriaFinal == -1) {
                val classificacao = classifier.classificar(descricao = nome, tipoPadrao = tipoFinal, usarIA = true)
                if (classeFinal == -1 || classeFinal == 0) classeFinal = classificacao.classe
                categoriaFinal = classificacao.categoria
            }

            contas.add(Conta().apply {
                this.idConta = idFinal
                this.nome = nome
                this.valor = valorAbsoluto
                this.tipo = tipoFinal
                this.classeConta = classeFinal
                this.categoria = categoriaFinal
                this.dia = dataCalendar[Calendar.DAY_OF_MONTH]
                this.mes = dataCalendar[Calendar.MONTH] + 1
                this.ano = dataCalendar[Calendar.YEAR]
                this.pagamento = statusFinal
                this.qtRepete = qtRepeteFinal
                this.nRepete = nRepeteFinal
                this.intervalo = intervaloFinal
                this.codigo = codigoFinal
                this.valorJuros = jurosFinal
            })
            onRowProcessed?.invoke()
        }
        return@withContext contas
    }

    internal fun identificarColunas(sheet: Sheet): ColumnMapping {
        val mapping = ColumnMapping()
        if (sheet.physicalNumberOfRows < 1) return mapping

        // Checagem rápida de aba DADOS nativa do ExportarExcel
        val sheetNameNorm = normalizarHeader(sheet.sheetName)
        val isNativeAba = sheetNameNorm == "DADOS"

        val maxRowsToScan = minOf(sheet.physicalNumberOfRows, 50)
        for (rowIndex in 0 until maxRowsToScan) {
            val row = sheet.getRow(rowIndex) ?: continue
            val excelHeaders = mutableMapOf<String, Int>()
            
            // 1. Coleta todos os textos da linha e normaliza
            for (i in 0 until row.lastCellNum.toInt()) {
                val cell = row.getCell(i) ?: continue
                val headerText = normalizarHeader(getCellValueAsString(cell))
                if (headerText.isNotEmpty()) {
                    excelHeaders[headerText] = i
                }
            }

            if (excelHeaders.isEmpty()) continue

            // 2. Tenta mapear o máximo de colunas conhecidas (DB_FULL_ALIASES)
            val fullMap = mutableMapOf<String, Int>()
            var foundEssentialCount = 0
            val essentialFields = listOf(
                Colunas.COLUNA_NOME_CONTA,
                Colunas.COLUNA_VALOR_CONTA
            )

            for (field in DB_FULL_ALIASES.keys) {
                val aliases = DB_FULL_ALIASES[field] ?: continue
                var foundIdx = -1
                for (alias in aliases) {
                    val normAlias = normalizarHeader(alias)
                    // Match exato ou parcial no header
                    if (excelHeaders.containsKey(normAlias)) {
                        foundIdx = excelHeaders[normAlias]!!
                        break
                    }
                    // Tenta busca parcial se for um alias longo
                    for ((header, idx) in excelHeaders) {
                        if (header.contains(normAlias) || normAlias.contains(header)) {
                            foundIdx = idx
                            break
                        }
                    }
                    if (foundIdx != -1) break
                }
                
                if (foundIdx != -1) {
                    fullMap[field] = foundIdx
                    if (essentialFields.contains(field)) foundEssentialCount++
                }
            }

            // 3. Critério de Aceitação: Deve ter pelo menos Nome e Valor
            if (foundEssentialCount >= 2) {
                // Verifica se temos campos suficientes para o modo FULL
                val missingEssentialForFull = listOf(
                    Colunas.COLUNA_TIPO_CONTA,
                    Colunas.COLUNA_DIA_DATA_CONTA
                ).any { !fullMap.containsKey(it) }

                if (!missingEssentialForFull || isNativeAba) {
                    mapping.mode = ColumnMapping.ImportMode.FULL_DB_MATCH
                } else {
                    mapping.mode = ColumnMapping.ImportMode.BASIC_MATCH
                    // Garante mapeamento de data se não estiver no fullMap mas estiver via alias genérico
                    if (!fullMap.containsKey(Colunas.COLUNA_DIA_DATA_CONTA) && !fullMap.containsKey("DATA_COMPLETA")) {
                         val foundDataCol = findColumnByAliases(excelHeaders, ALIASES_DATA)
                         if (foundDataCol != -1) fullMap["DATA_COMPLETA"] = foundDataCol
                    }
                }
                
                mapping.headerRowIndex = rowIndex
                mapping.colIndexMap.putAll(fullMap)
                return mapping
            }
        }

        return identificarPorHeuristica(sheet)
    }

    /**
     * Verifica se um documento é virtual (ex: Google Docs, Sheets).
     */
    private fun isVirtualFile(context: Context, uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(context, uri)) return false
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val flags = cursor.getInt(0)
                    (flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0
                } else false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun normalizarHeader(text: String): String {
        return text.uppercase()
            .replace("[ÁÀÂÃ]".toRegex(), "A")
            .replace("[ÉÈÊ]".toRegex(), "E")
            .replace("[ÍÌÎ]".toRegex(), "I")
            .replace("[ÓÒÔÕ]".toRegex(), "O")
            .replace("[ÚÙÛ]".toRegex(), "U")
            .replace("Ç", "C")
            .replace(".", "")
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun findColumnByAliases(headers: Map<String, Int>, aliases: List<String>): Int {
        for (alias in aliases) {
            val normAlias = alias.uppercase()
            if (headers.containsKey(normAlias)) return headers[normAlias]!!
            for (header in headers.keys) {
                if (header.contains(normAlias)) return headers[header]!!
            }
        }
        return -1
    }

    private fun isEssentialField(field: String): Boolean {
        return field == Colunas.COLUNA_NOME_CONTA ||
                field == Colunas.COLUNA_VALOR_CONTA ||
                field == Colunas.COLUNA_DIA_DATA_CONTA ||
                field == Colunas.COLUNA_TIPO_CONTA
    }

    private fun identificarPorHeuristica(sheet: Sheet): ColumnMapping {
        val mapping = ColumnMapping()
        val numRows = minOf(sheet.physicalNumberOfRows, 100)
        if (numRows < 1) return mapping

        val sampleRows = (0 until numRows).mapNotNull { sheet.getRow(it) }
        val numCols = sampleRows.maxOfOrNull { it.lastCellNum.toInt() } ?: 0
        if (numCols == 0) return mapping

        val dateScore = IntArray(numCols)
        val numberScore = IntArray(numCols)
        val textScore = IntArray(numCols)

        for (row in sampleRows) {
            for (j in 0 until minOf(numCols, row.lastCellNum.toInt())) {
                val cell = row.getCell(j) ?: continue
                if (isLikelyDate(cell)) dateScore[j]++
                else if (isLikelyNumber(cell)) numberScore[j]++
                else if (getCellValueAsString(cell).trim().length > 3) textScore[j]++
            }
        }

        var bestDataCol = dateScore.indices.maxByOrNull { dateScore[it] } ?: -1
        if (bestDataCol != -1 && dateScore[bestDataCol] < 2) bestDataCol = -1

        var bestValorCol = numberScore.indices.filter { it != bestDataCol }.maxByOrNull { numberScore[it] } ?: -1
        if (bestValorCol != -1 && numberScore[bestValorCol] < 2) bestValorCol = -1

        val textCols = mutableListOf<Int>()
        for (j in 0 until numCols) {
            if (j != bestDataCol && j != bestValorCol && textScore[j] >= 2) {
                textCols.add(j)
            }
        }

        if (textCols.isNotEmpty() && bestValorCol != -1) {
            val bestNomeCol = textCols.first()
            mapping.colIndexMap[Colunas.COLUNA_NOME_CONTA] = bestNomeCol
            mapping.colIndexMap[Colunas.COLUNA_VALOR_CONTA] = bestValorCol
            if (bestDataCol != -1) mapping.colIndexMap["DATA_COMPLETA"] = bestDataCol

            mapping.textCols.addAll(textCols)
            mapping.mode = ColumnMapping.ImportMode.BASIC_MATCH
            mapping.headerRowIndex = -1
        }
        return mapping
    }

    private fun isLikelyDate(cell: Cell): Boolean {
        if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return true
        if (cell.cellType == CellType.STRING) {
            val s = cell.stringCellValue
            return s.matches(".*\\d{2}[/-]\\d{2}[/-]\\d{2,4}.*".toRegex())
        }
        return false
    }

    private fun isLikelyNumber(cell: Cell): Boolean {
        if (cell.cellType == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) return true
        if (cell.cellType == CellType.STRING) {
            val s = cell.stringCellValue.replace(",", ".").replace("[^0-9.]".toRegex(), "")
            return s.isNotEmpty() && s.toDoubleOrNull() != null
        }
        return false
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) cell.dateCellValue.toString() else cell.numericCellValue.toString()
                CellType.FORMULA -> {
                    val evaluator = cell.sheet.workbook.creationHelper.createFormulaEvaluator()
                    val cv = evaluator.evaluate(cell)
                    if (cv.cellType == CellType.NUMERIC) cv.numberValue.toString() else cv.stringValue ?: ""
                }
                else -> ""
            }
        } catch (e: Exception) { "" }
    }

    private fun parseDouble(value: String, defaultValue: Double): Double {
        if (value.isEmpty()) return defaultValue
        try {
            var clean = value.replace("R$", "").replace("$", "").trim()
            if (clean.contains(",") && clean.contains(".")) {
                clean = clean.replace(".", "").replace(",", ".")
            } else if (clean.contains(",")) {
                clean = clean.replace(",", ".")
            }
            clean = clean.replace("[^0-9.-]".toRegex(), "")
            return clean.toDouble()
        } catch (e: Exception) {
            return try {
                NumberFormat.getInstance(Locale.forLanguageTag("pt-BR")).parse(value.trim())?.toDouble() ?: defaultValue
            } catch (e2: Exception) {
                defaultValue
            }
        }
    }

    private fun getDateFromCell(sheet: Sheet, col: Int?, row: Int): Calendar? {
        if (col == null || col < 0) return null
        val cell = sheet.getRow(row)?.getCell(col) ?: return null
        val calendar = Calendar.getInstance()

        if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            calendar.time = cell.dateCellValue
            return calendar
        }

        val s = getCellValueAsString(cell).trim()
        val formats = listOf("dd/MM/yyyy", "dd/MM/yy", "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                sdf.isLenient = false
                calendar.time = sdf.parse(s)!!
                return calendar
            } catch (e: Exception) {}
        }

        try {
            val p = s.split("[^0-9]+".toRegex()).filter { it.isNotEmpty() }
            if (p.size >= 3) {
                val d = p[0].toInt()
                val m = p[1].toInt()
                var y = p[2].toInt()
                if (y < 100) y += 2000
                if (d in 1..31 && m in 1..12) {
                    calendar.set(y, m - 1, d)
                    return calendar
                }
            }
        } catch (e: Exception) {}
        return null
    }

    /**
     * Função auxiliar para inferir Tipo e Classe baseado no nome, aba e sinal do valor.
     * Retorna um Pair(Tipo, Classe).
     */
    private fun inferirTipoeClasse(nome: String, aba: String, isNegativo: Boolean): Pair<Int, Int> {
        val textoAlvo = "$nome $aba".lowercase()

        // Aplicação / Investimento
        if (textoAlvo.matches(Regex(".*(aplicacao|investimento|cdb|selic|acao|ações|poupanca|fundo).*"))) {
            return Pair(ContasContract.TIPO_APLICACAO, ContasContract.CLASSE_APLICACAO_OUTRAS)
        }

        // Receita Fixa / Variável
        if (textoAlvo.matches(Regex(".*(receita|salario|ganho|entrada|rendimento|honorario|venda).*")) || aba.contains("receita")) {
            return Pair(ContasContract.TIPO_RECEITA, 1) // 1 como 'Receita Variável/Fixa' genérica
        }

        // Despesa Fixa / Variável
        if (textoAlvo.matches(Regex(".*(despesa|gasto|compra|saida|pagamento|boleto|fatura).*")) || aba.contains("despesa") || isNegativo) {
            return Pair(ContasContract.TIPO_DESPESA, ContasContract.CLASSE_DESPESA_VARIAVEL)
        }

        // Fallback padrão pelo sinal
        return Pair(if (isNegativo) ContasContract.TIPO_DESPESA else ContasContract.TIPO_RECEITA, 0)
    }

    companion object {
        private const val TAG = "ImportarExcel"

        private val DB_FULL_ALIASES: Map<String, List<String>> = mapOf(
            Colunas.COLUNA_NOME_CONTA to listOf("NOME", "NOME_CONTA", "DESCRICAO", "HISTORICO"),
            Colunas.COLUNA_TIPO_CONTA to listOf("TIPO", "TIPO_CONTA"),
            Colunas.COLUNA_CLASSE_CONTA to listOf("CLASSE", "CLASSE_CONTA"),
            Colunas.COLUNA_CATEGORIA_CONTA to listOf("CATEGORIA", "CATEGORIA_CONTA"),
            Colunas.COLUNA_DIA_DATA_CONTA to listOf("DIA", "DIA_DATA"),
            Colunas.COLUNA_MES_DATA_CONTA to listOf("MES", "MES_DATA", "MÊS"),
            Colunas.COLUNA_ANO_DATA_CONTA to listOf("ANO", "ANO_DATA"),
            Colunas.COLUNA_VALOR_CONTA to listOf("VALOR", "VALOR_CONTA"),
            Colunas.COLUNA_PAGOU_CONTA to listOf("PAGAMENTO", "PAGOU_CONTA", "STATUS"),
            Colunas.COLUNA_QT_REPETICOES_CONTA to listOf("QT_REPETICOES", "QT_REPETE", "QT REPET", "QUANTIDADE"),
            Colunas.COLUNA_NR_REPETICAO_CONTA to listOf("NR_REPETICAO", "N_REPETE", "N REPET", "NUMERO"),
            Colunas.COLUNA_INTERVALO_CONTA to listOf("INTERVALO", "INTERVALO_CONTA"),
            Colunas.COLUNA_CODIGO_CONTA to listOf("CODIGO", "CODIGO_CONTA"),
            Colunas.COLUNA_VALOR_JUROS to listOf("VALOR_JUROS", "JUROS"),
            "_id" to listOf("ID", "IDENTIFICADOR", "PK"),
            "DATA_COMPLETA" to listOf("DATA", "VENCIMENTO")
        )
    }
}