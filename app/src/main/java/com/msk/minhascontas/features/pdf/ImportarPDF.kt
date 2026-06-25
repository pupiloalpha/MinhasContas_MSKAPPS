package com.msk.minhascontas.features.pdf

import android.content.Context
import android.net.Uri
import android.util.Log
import com.msk.minhascontas.features.ai.TransactionClassifier
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.*

class ImportarPDF {

    companion object {
        private const val TAG = "ImportarPDF"
        private const val KEY_ITAU = "itau"
        private const val KEY_BB = "brasil"
        private const val KEY_INTER = "inter"
    }

    suspend fun lerPDF(
        context: Context,
        uri: Uri,
        onProgress: ((Int, Int) -> Unit)? = null
    ): MutableList<Conta> = withContext(Dispatchers.IO) {
        val contas = mutableListOf<Conta>()
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            PDFBoxResourceLoader.init(context)
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext contas
            document = PDDocument.load(inputStream)

            val stripper = PDFTextStripper().apply {
                sortByPosition = true
                startPage = 1
                endPage = document.numberOfPages
            }
            val fullText = stripper.getText(document)

            if (fullText.length < 500) {
                Log.w(TAG, "Texto extraído muito pequeno. PDF pode ser imagem.")
            }
            val lowerText = fullText.lowercase(Locale.getDefault())

            val bank = when {
                lowerText.contains(KEY_ITAU) || lowerText.contains("itaú") -> "itau"
                lowerText.contains(KEY_BB) || lowerText.contains("banco do brasil") || lowerText.contains(" extrato bb ") -> "bb"
                lowerText.contains(KEY_INTER) || lowerText.contains("banco inter") -> "inter"
                lowerText.contains("caixa") || lowerText.contains("cef") -> "caixa"
                else -> "generic"
            }
            Log.d(TAG, "Banco detectado: $bank")

            val isFatura = (lowerText.contains("fatura") || lowerText.contains("cartão") || lowerText.contains("cartao")) &&
                    (lowerText.contains("vencimento") || lowerText.contains("limite") ||
                            lowerText.contains("total a pagar") || lowerText.contains("melhor dia") ||
                            lowerText.contains("pagamento mínimo") || lowerText.contains("detalhamento") ||
                            lowerText.contains("compras nacionais"))

            var transactions = when {
                isFatura -> parseFatura(fullText)
                bank == "itau" -> parseItau(fullText)
                bank == "bb" -> parseBB(fullText)
                bank == "inter" -> parseInter(fullText)
                bank == "caixa" -> parseCaixa(fullText)
                else -> parseGeneric(fullText)
            }

            // Fallback: Se o parser específico falhou, tenta o genérico
            if (transactions.isEmpty() && (bank != "generic" || isFatura)) {
                Log.d(TAG, "Parser específico ($bank) falhou ou não encontrou transações. Tentando parser genérico...")
                transactions = parseGeneric(fullText)
            }

            val total = transactions.size
            var processed = 0
            val usarIA = true

            if (usarIA && onProgress != null) {
                withContext(Dispatchers.Main) {
                    onProgress(0, total)
                }
            }

            val classifier = TransactionClassifier(context)

            for ((dataStr, descRaw, valorRaw) in transactions) {
                if (descRaw.isBlank() || valorRaw == 0.0) {
                    processed++
                    onProgress?.invoke(processed, total)
                    continue
                }

                val descLimpa = limparDescricao(descRaw)
                val valorAbsoluto = kotlin.math.abs(valorRaw)

                val tipoForcado = if (valorRaw > 0) ContasContract.TIPO_RECEITA else ContasContract.TIPO_DESPESA

                val classificacao = classifier.classificar(
                    descricao = descLimpa,
                    tipoPadrao = tipoForcado,
                    usarIA = usarIA
                )

                val calendar = parseData(dataStr) ?: Calendar.getInstance()

                val descLower = descLimpa.lowercase(Locale.getDefault())
                val ehCartao = isFatura || descLower.contains("cartao") || descLower.contains("cartão") ||
                        descLower.contains("compra no") || descLower.contains("apple pay") || descLower.contains("google pay")

                val conta = Conta().apply {
                    nome = descLimpa.take(100)
                    valor = valorAbsoluto
                    tipo = classificacao.tipo

                    classeConta = if (ehCartao && classificacao.tipo == ContasContract.TIPO_DESPESA) {
                        ContasContract.CLASSE_DESPESA_CARTAO
                    } else {
                        classificacao.classe
                    }

                    categoria = classificacao.categoria
                    dia = calendar[Calendar.DAY_OF_MONTH]
                    mes = calendar[Calendar.MONTH] + 1
                    ano = calendar[Calendar.YEAR]
                    pagamento = ContasContract.STATUS_PENDENTE
                    qtRepete = 1
                    nRepete = 1
                }
                contas.add(conta)
                processed++
                onProgress?.invoke(processed, total)
            }

            Log.d(TAG, "Total de transações importadas: ${contas.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar PDF", e)
        } finally {
            document?.close()
            inputStream?.close()
        }
        return@withContext contas
    }

    // --------------------------------------------------------------
    // PARSER FATURA DE CARTÃO DE CRÉDITO (Otimizado)
    // --------------------------------------------------------------
    private fun parseFatura(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()
        val lines = text.lines()

        val monthMap = mapOf(
            "JAN" to 1, "FEV" to 2, "MAR" to 3, "ABR" to 4, "MAI" to 5, "JUN" to 6,
            "JUL" to 7, "AGO" to 8, "SET" to 9, "OUT" to 10, "NOV" to 11, "DEZ" to 12
        )

        // Aceita valores com prefixo R$ opcional (ex: "R$ 50,00", "R$-3.694,65")
        val regex1 = Regex("""(\d{2}/\d{2})\s+(.+?)\s+(?:R\$?\s*)?([+-]?\s*[\d.]{1,10},\d{2})""", RegexOption.IGNORE_CASE)
        val regex2 = Regex("""(\d{2})\s+([A-Z]{3})\s+(.+?)\s+(?:R\$?\s*)?([+-]?\s*[\d.]{1,10},\d{2})""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() ||
                trimmed.contains("Total a pagar", ignoreCase = true) ||
                trimmed.contains("Limite", ignoreCase = true) ||
                trimmed.contains("Subtotal", ignoreCase = true) ||
                trimmed.contains("Total da Fatura", ignoreCase = true)) continue

            var data: String? = null
            var desc: String? = null
            var valorFinal = 0.0

            val m1 = regex1.find(trimmed)
            if (m1 != null) {
                data = m1.groupValues[1]
                desc = m1.groupValues[2].trim()
                val valorStr = m1.groupValues[3]
                val valor = valorStr.replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                valorFinal = -valor
            } else {
                val m2 = regex2.find(trimmed)
                if (m2 != null) {
                    val dia = m2.groupValues[1]
                    val mesNome = m2.groupValues[2].uppercase()
                    val mes = monthMap[mesNome]
                    if (mes != null) {
                        data = "$dia/${mes.toString().padStart(2, '0')}"
                        desc = m2.groupValues[3].trim()
                        val valorStr = m2.groupValues[4]
                        val valor = valorStr.replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                        valorFinal = -valor
                    }
                }
            }

            if (data != null && desc != null && valorFinal != 0.0) {
                // Remove a sigla do país ("BR") que costuma vir no final das faturas BB/Ourocard
                desc = desc.replace(Regex("""\s+BR$""", RegexOption.IGNORE_CASE), "").trim()

                // Se a descrição indicar um crédito ou pagamento, invertemos de volta para positivo
                if (desc.contains("PGTO", ignoreCase = true) ||
                    desc.contains("PAGAMENTO", ignoreCase = true) ||
                    desc.contains("CREDITO", ignoreCase = true) ||
                    desc.contains("ESTORNO", ignoreCase = true)) {
                    valorFinal = kotlin.math.abs(valorFinal)
                }

                result.add(Triple(data, desc, valorFinal))
            }
        }
        return result
    }

    // --------------------------------------------------------------
    // PARSER BANCO DO BRASIL
    // --------------------------------------------------------------
    private fun parseBB(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()
        val lines = text.lines()

        val regexPrincipal = Regex(
            """(\d{2}/\d{2}(?:/\d{4})?)\s+(?:\d+\s+\d+\s+)?(.+?)\s+(\d{1,3}(?:\.\d{3})*,\d{2})\s*\(\s*([+-])\s*\)""",
            RegexOption.IGNORE_CASE
        )

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() ||
                trimmed.contains("Saldo do dia", ignoreCase = true) ||
                trimmed.contains("SALDO", ignoreCase = true) ||
                trimmed.contains("Saldo Anterior", ignoreCase = true)) {
                continue
            }

            val match = regexPrincipal.find(trimmed)
            if (match != null) {
                val data = match.groupValues[1]
                val descricaoRaw = match.groupValues[2].trim()
                val valorStr = match.groupValues[3]
                val sinal = match.groupValues[4]

                val valor = valorStr.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                if (valor == 0.0) continue

                val valorFinal = if (sinal == "-") -valor else valor
                val descricao = limparDescricaoBB(descricaoRaw)

                if (descricao.isNotBlank()) {
                    result.add(Triple(data, descricao, valorFinal))
                }
            }
        }
        return result
    }

    private fun limparDescricaoBB(desc: String): String {
        return desc
            .replace(Regex("""\b(Lote|Documento|Histórico|Valor|Rende Fácil)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[./:,\-–—+()"]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(100)
    }

    // --------------------------------------------------------------
    // PARSER BANCO ITAÚ
    // --------------------------------------------------------------
    private fun parseItau(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()
        val lines = text.lines()

        // Regex mais flexível: Removeu ^ e $, aceita data dd/mm ou dd/mm/aaaa, e sinal opcional
        val regex = Regex(
            """(\d{2}/\d{2}(?:/\d{4})?)\s+(.+?)\s+([+-]?\s*[\d.]{1,10},\d{2})\s*([+-])?"""
        )

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.contains("SALDO DO DIA", ignoreCase = true) ||
                trimmedLine.contains("SALDO ANTERIOR", ignoreCase = true) ||
                trimmedLine.contains("SALDO FINAL", ignoreCase = true)) continue

            val match = regex.find(trimmedLine)
            if (match != null) {
                val data = match.groupValues[1]
                val desc = match.groupValues[2].trim()
                var valorStr = match.groupValues[3].replace(" ", "").replace(".", "").replace(",", ".")
                val sinalFinal = match.groupValues[4]

                val isNegativePrefix = valorStr.startsWith("-")
                if (isNegativePrefix) valorStr = valorStr.substring(1)

                val valor = valorStr.toDoubleOrNull() ?: 0.0
                val valorFinal = if (isNegativePrefix || sinalFinal == "-") -valor else valor

                if (desc.isNotBlank() && valorFinal != 0.0) {
                    result.add(Triple(data, desc, valorFinal))
                }
            }
        }
        return result
    }

    // --------------------------------------------------------------
    // PARSER BANCO INTER
    // --------------------------------------------------------------
    private fun parseInter(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()
        val lines = text.lines()
        var currentDate: String? = null

        val monthMap = mapOf(
            "janeiro" to 1, "fevereiro" to 2, "março" to 3, "abril" to 4,
            "maio" to 5, "junho" to 6, "julho" to 7, "agosto" to 8,
            "setembro" to 9, "outubro" to 10, "novembro" to 11, "dezembro" to 12,
            "jan" to 1, "fev" to 2, "mar" to 3, "abr" to 4, "mai" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "set" to 9, "out" to 10, "nov" to 11, "dez" to 12
        )

        val regexTransacao = Regex("""^(.*?)\s*([+-]?)\s*R\$?\s*(\d{1,3}(?:\.\d{3})*,\d{2})\s*$""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.contains("Saldo do dia", ignoreCase = true)) continue

            val dateMatch1 = Regex("""(\d{1,2})\s+(?:de\s+)?(\w+)\s+(?:de\s+)?(\d{4})""", RegexOption.IGNORE_CASE).find(trimmed)
            if (dateMatch1 != null) {
                val dia = dateMatch1.groupValues[1].padStart(2, '0')
                val mesNome = dateMatch1.groupValues[2].lowercase()
                val ano = dateMatch1.groupValues[3]
                val mes = monthMap[mesNome]
                if (mes != null) {
                    currentDate = "$dia/${mes.toString().padStart(2, '0')}/$ano"
                    continue
                }
            }

            val dateMatch2 = Regex("""^(\d{2}/\d{2}/\d{4})$""").find(trimmed)
            if (dateMatch2 != null) {
                currentDate = dateMatch2.groupValues[1]
                continue
            }

            if (currentDate != null) {
                val match = regexTransacao.find(trimmed)
                if (match != null) {
                    var descricao = match.groupValues[1]
                    val sinalStr = match.groupValues[2]
                    val valorStr = match.groupValues[3].replace(".", "").replace(",", ".")

                    val valorNum = valorStr.toDoubleOrNull() ?: 0.0
                    val sinal = if (sinalStr == "-") -1 else 1

                    descricao = descricao
                        .replace(Regex("""^["']|["']$"""), "")
                        .replace(Regex("""Cp\s*:\s*[\d-]+""", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("""\s+"""), " ")
                        .trim()

                    if (valorNum != 0.0) {
                        result.add(Triple(currentDate, descricao, valorNum * sinal))
                    }
                }
            }
        }
        return result
    }

    // --------------------------------------------------------------
    // PARSER CAIXA ECONÔMICA FEDERAL
    // --------------------------------------------------------------
    private fun parseCaixa(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()
        val lines = text.lines()

        val regex = Regex(
            """(\d{2}/\d{2}(?:/\d{4})?)\s+(.+?)\s+(\d{1,3}(?:\.\d{3})*,\d{2})\s+([CD])""",
            RegexOption.IGNORE_CASE
        )

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.contains("SALDO", ignoreCase = true)) continue

            val match = regex.find(trimmed)
            if (match != null) {
                val data = match.groupValues[1]
                val desc = match.groupValues[2].trim()
                val valorStr = match.groupValues[3]
                val tipoCD = match.groupValues[4].uppercase()

                val valor = valorStr.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                if (valor == 0.0) continue

                val valorFinal = if (tipoCD == "D") -valor else valor

                result.add(Triple(data, desc, valorFinal))
            }
        }
        return result
    }

    // --------------------------------------------------------------
    // PARSER GENÉRICO
    // --------------------------------------------------------------
    private fun parseGeneric(text: String): List<Triple<String, String, Double>> {
        val result = mutableListOf<Triple<String, String, Double>>()

        val regex = Regex(
            """(\d{2}/\d{2}(?:/\d{4})?)\s+(.+?)\s+([+-]?\s*\d{1,3}(?:\.\d{3})*,\d{2})\s*([CD])?""",
            RegexOption.IGNORE_CASE
        )

        regex.findAll(text).forEach { match ->
            var data = match.groupValues[1]
            val desc = match.groupValues[2].trim()
            var valorStr = match.groupValues[3].replace(" ", "").replace(".", "").replace(",", ".")
            val sufixoCD = match.groupValues.getOrNull(4)?.uppercase() ?: ""

            val isNegativeSinal = valorStr.startsWith("-")
            val isPositiveSinal = valorStr.startsWith("+")

            if (isNegativeSinal || isPositiveSinal) {
                valorStr = valorStr.substring(1)
            }

            val valor = valorStr.toDoubleOrNull() ?: 0.0
            var valorFinal = if (isNegativeSinal) -valor else valor

            if (sufixoCD == "D") {
                valorFinal = -kotlin.math.abs(valor)
            } else if (sufixoCD == "C") {
                valorFinal = kotlin.math.abs(valor)
            }

            val descLower = desc.lowercase()
            if (descLower.contains("saldo") || descLower.contains("disponível") || descLower.contains("bloqueado")) {
                return@forEach
            }

            if (data.split("/").size == 2) {
                data += "/${Calendar.getInstance()[Calendar.YEAR]}"
            }
            if (valorFinal != 0.0) result.add(Triple(data, desc, valorFinal))
        }
        return result
    }

    // --------------------------------------------------------------
    // AUXILIARES
    // --------------------------------------------------------------
    private fun limparDescricao(desc: String): String {
        return desc
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\d{2}/\\d{2}/\\d{4}"), "")
            .replace(Regex("\\d{2}/\\d{2}"), "")
            .replace(Regex("\\d{5,}"), "")
            .trim()
    }

    private fun parseData(dataStr: String): Calendar? {
        val partes = dataStr.split("/")
        if (partes.size < 2) return null
        val cal = Calendar.getInstance()
        return try {
            val dia = partes[0].toInt()
            val mes = partes[1].toInt()
            var ano = if (partes.size > 2) partes[2].toInt() else cal[Calendar.YEAR]
            if (ano < 100) ano += 2000
            cal.set(ano, mes - 1, dia)
            cal
        } catch (ignored: Exception) {
            null
        }
    }

    private data class Triple<A, B, C>(val first: A, val second: B, val third: C)
}