package com.msk.minhascontas.features.servicos

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

class NotificationImportService : NotificationListenerService() {

    private val TAG = "NotificationImport"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isEnabled = prefs.getBoolean("notificacao_ativa", false)

        if (!isEnabled || sbn == null) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val packageName = sbn.packageName

        Log.d(TAG, "Notification received from $packageName: $title - $text")

        // Lista de pacotes de bancos comuns (exemplo)
        val bankPackages = listOf(
            "com.nu.production", // Nubank
            "com.itau", // Itaú
            "br.com.inter.android", // Inter
            "br.com.bb.android", // Banco do Brasil
            "com.santander.app", // Santander
            "br.com.bradesco.next" // Next
        )

        // Se quiser restringir a apenas bancos conhecidos, descomente a linha abaixo
        if (!bankPackages.any { packageName.contains(it) }) {
             Log.d(TAG, "Ignorando notificação de pacote não bancário: $packageName")
             // return // Por enquanto vamos deixar processar tudo para facilitar testes, mas em prod seria bom filtrar
        }

        val conta = parseNotification(title, text)
        if (conta != null) {
            val repository = ContasRepository.getInstance(this)
            repository.salvarConta(conta)
            
            val currencyFormat = NumberFormat.getCurrencyInstance()
            Log.d(TAG, "Conta criada a partir da notificação: ${conta.nome} - ${currencyFormat.format(conta.valor)}")
        }
    }

    private fun parseNotification(title: String, text: String): Conta? {
        val fullText = "$title $text"
        
        // Obter símbolo da moeda local (ex: R$, $, €)
        val currencyFormat = NumberFormat.getCurrencyInstance()
        val currencySymbol = currencyFormat.currency?.symbol ?: ""
        val escapedSymbol = if (currencySymbol.isNotEmpty()) Pattern.quote(currencySymbol) else "\\$|R\\$|€|£"

        // Regex flexível para o símbolo da localidade e valores (suporta . ou , como separadores)
        val valuePattern = Pattern.compile("$escapedSymbol\\s?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2}))")
        val matcher = valuePattern.matcher(fullText)

        if (matcher.find()) {
            val valorCapturado = matcher.group(1) ?: return null
            
            // Tenta converter o valor baseado nos separadores encontrados
            val valor = try {
                val lastDot = valorCapturado.lastIndexOf('.')
                val lastComma = valorCapturado.lastIndexOf(',')
                
                if (lastComma > lastDot) {
                    // Formato 1.234,56 (BR/EU)
                    valorCapturado.replace(".", "").replace(",", ".").toDouble()
                } else if (lastDot > lastComma) {
                    // Formato 1,234.56 (US)
                    valorCapturado.replace(",", "").toDouble()
                } else {
                    // Sem separadores complexos, apenas trata a vírgula como ponto decimal se for o único
                    valorCapturado.replace(",", ".").toDouble()
                }
            } catch (e: Exception) {
                null
            } ?: return null

            // Tenta extrair um nome significativo
            var nome = getString(R.string.notificacao_nome_padrao)
            
            // Padrões comuns de bancos em diversos idiomas (PT, EN, ES, FR)
            val patterns = listOf(
                Pattern.compile("(?:em|at|en|dans)\\s+([^.]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:para|to|a|pour)\\s+([^.]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:no|in|en el|dans le)\\s+([^.]+)", Pattern.CASE_INSENSITIVE)
            )

            for (p in patterns) {
                val m = p.matcher(text)
                if (m.find()) {
                    nome = m.group(1)?.trim() ?: nome
                    break
                }
            }

            val calendar = Calendar.getInstance()
            return Conta.Builder(
                nome,
                valor,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR),
                UUID.randomUUID().toString()
            )
            .setTipo(ContasContract.TIPO_DESPESA)
            .setClasseConta(ContasContract.CLASSE_DESPESA_VARIAVEL)
            .setPagamento(ContasContract.STATUS_PAGO_RECEBIDO) // Geralmente se já notificou, já foi pago
            .build()
        }

        return null
    }
}
