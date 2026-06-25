package com.msk.minhascontas.features.ai

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.msk.minhascontas.BuildConfig
import com.msk.minhascontas.db.ContasContract
import kotlinx.coroutines.*
import kotlin.collections.iterator

class TransactionClassifier(private val context: Context) {

    companion object {
        private const val TAG = "TransactionClassifier"
    }

    // Dicionários por idioma (expansíveis)
    // As palavras-chave devem ser armazenadas em minúsculas e sem acentos para melhor correspondência
    private val keywords = mapOf(
        "pt" to mapOf(
            "receita" to listOf(
                "salario", "rendimento", "deposito", "transferencia recebida", "credito", "pagamento recebido",
                "remuneracao", "vencimentos", "proventos", "pix recebido", "recebido", "reembolso", "estorno", 
                "venda", "lucro", "dividendo", "jcp", "bonificacao",
            ),
            "despesa" to listOf(
                "pagamento", "compra", "conta", "fatura", "cartao", "uber", "ifood", "mercado",
                "taxa", "tarifa", "juros", "anuidade", "mensalidade", "pay", "pagto", "debito", "boleto", 
                "transferencia enviada", "pix enviado", "saque", "iof", "seguro", "desc", "ajuste",
            ),
            "aplicacao" to listOf(
                "investimento", "aplicacao", "resgate", "cdb", "poupanca", "lci", "lca", "tesouro", 
                "fii", "acoes", "bolsa", "renda variavel", "previdencia",
            ),
            "classes" to mapOf(
                ContasContract.CLASSE_DESPESA_CARTAO to listOf(
                    "visa", "mastercard", "itaucard", "nubank", "digio", "elo", "hipercard", "amex", "american express", 
                    "credito", "fatura", "itau", "bradesco", "santander", "banco", "card"
                ),
                ContasContract.CLASSE_DESPESA_FIXA to listOf(
                    "aluguel", "condominio", "mensalidade", "assinatura", "streaming", "seguro", "plano"
                )
            ),
            "categorias" to mapOf(
                ContasContract.CATEGORIA_ALIMENTACAO to listOf(
                    "mercado", "supermercado", "feira", "restaurante", "ifood", "comida", "rest", "bar", "cafe", 
                    "padaria", "confeitaria", "lanche", "pizza", "burger", "super", "hiper", "atacad", 
                    "mcdonalds", "bk", "starbucks", "pad", "conf", "gastronomia", "sams", "carrefour", "pao de acucar",
                    "extra", "assai", "rappi", "ze delivery", "atacadista", "mercearia", "sacolao"
                ),
                ContasContract.CATEGORIA_TRANSPORTE to listOf(
                    "uber", "99", "taxi", "combustivel", "estacionamento", "onibus", "metro", "posto", "gasol", 
                    "comb", "estac", "pedagio", "auto", "oficina", "pecas", "shell", "ipiranga",
                    "petrobras", "rodov", "estac", "garagem", "mecanico", "abastec", "sem parar", "veloe", "conectcar"
                ),
                ContasContract.CATEGORIA_LAZER to listOf(
                    "cinema", "show", "netflix", "spotify", "viagem", "hobby", "stream", "game", "jogos", "clube", 
                    "event", "turism", "hotel", "pousada", "decolar", "airbnb", "steam", "psn", "xbox",
                    "ingresso", "teatro", "museu", "barzinho", "sympla", "eventim", "bilheteria"
                ),
                ContasContract.CATEGORIA_MORADIA to listOf(
                    "aluguel", "condominio", "luz", "agua", "gas", "iptu", "ene", "energ", "eletr", "sane", "tel", 
                    "inter", "fibra", "net", "vivo", "tim", "claro", "oi", "sky", "copasa", "cemig", "sabesp", "enel",
                    "energia", "telefone", "internet", "tv", "reforma", "manutencao", "cemig", "leroy merlin", "c c", "telhanorte"
                ),
                ContasContract.CATEGORIA_SAUDE to listOf(
                    "farmacia", "medico", "dentista", "plano de saude", "exame", "hosp", "clinica", "odonto", 
                    "laborat", "drog", "drogasil", "raia", "pacheco", "saude", "odontologico", "hospital",
                    "consulta", "remedio", "medicamento", "araujo", "paguemenos", "unimed", "bradesco saude"
                ),
                ContasContract.CATEGORIA_EDUCACAO to listOf(
                    "curso", "faculdade", "livro", "material escolar", "ingles", "esc", "univ", "fac", "colegio", 
                    "mensal", "matr", "escola", "educa", "treinamento", "workshop", "pos graduacao"
                ),
                ContasContract.CATEGORIA_VESTUARIO to listOf(
                    "roupa", "calcado", "loja", "vestuario", "renner", "riachuelo", "cea", "zara", "nike", "adidas",
                    "tenis", "moda", "boutique", "acessorios", "caedu", "lupo", "scala"
                ),
                ContasContract.CATEGORIA_INVESTIMENTOS to listOf(
                    "investimento", "aplicacao", "resgate", "cdb", "poupanca", "lci", "lca", "tesouro", 
                    "fii", "acoes", "bolsa", "renda variavel", "previdencia", "tesouro direto", "cdi"
                )
            )
        ),
        "en" to mapOf(
            "receita" to listOf("salary", "income", "deposit", "transfer received", "credit", "payment received"),
            "despesa" to listOf("payment", "purchase", "bill", "invoice", "card", "uber", "doordash", "grocery", "tax", "fee"),
            "aplicacao" to listOf("investment", "withdrawal", "cdb", "savings", "bond", "treasury", "stocks"),
            "categorias" to mapOf(
                ContasContract.CATEGORIA_ALIMENTACAO to listOf("grocery", "supermarket", "restaurant", "food delivery", "meal", "mcdonalds", "starbucks"),
                ContasContract.CATEGORIA_TRANSPORTE to listOf("uber", "taxi", "fuel", "parking", "bus", "subway", "gas station"),
                ContasContract.CATEGORIA_LAZER to listOf("movie", "concert", "netflix", "spotify", "travel", "hobby", "hotel", "airbnb"),
                ContasContract.CATEGORIA_MORADIA to listOf("rent", "condo fee", "electricity", "water", "gas", "property tax", "internet", "phone"),
                ContasContract.CATEGORIA_SAUDE to listOf("pharmacy", "doctor", "dentist", "health plan", "exam", "hospital"),
                ContasContract.CATEGORIA_EDUCACAO to listOf("course", "college", "book", "school supplies", "school"),
                ContasContract.CATEGORIA_VESTUARIO to listOf("clothes", "shoes", "apparel", "clothing", "nike", "zara"),
                ContasContract.CATEGORIA_INVESTIMENTOS to listOf("investment", "savings", "bond", "stocks", "treasury", "crypto")
            )
        ),
        "fr" to mapOf(
            "receita" to listOf("salaire", "revenu", "depot", "virement recu", "credit", "paiement recu"),
            "despesa" to listOf("paiement", "achat", "facture", "carte", "uber", "deliveroo", "courses", "taxe", "frais"),
            "aplicacao" to listOf("investissement", "retrait", "cdb", "epargne", "bon d'etat"),
            "categorias" to mapOf(
                ContasContract.CATEGORIA_ALIMENTACAO to listOf("courses", "supermarche", "restaurant", "livraison repas"),
                ContasContract.CATEGORIA_TRANSPORTE to listOf("uber", "taxi", "carburant", "stationnement", "bus", "metro"),
                ContasContract.CATEGORIA_LAZER to listOf("cinema", "concert", "netflix", "spotify", "voyage"),
                ContasContract.CATEGORIA_MORADIA to listOf("loyer", "charges", "electricite", "eau", "gaz", "taxe fonciere"),
                ContasContract.CATEGORIA_SAUDE to listOf("pharmacie", "medecin", "dentiste", "mutuelle", "examen"),
                ContasContract.CATEGORIA_EDUCACAO to listOf("cours", "universite", "livre", "fournitures scolaires"),
                ContasContract.CATEGORIA_VESTUARIO to listOf("vetements", "chaussures", "habillement"),
                ContasContract.CATEGORIA_INVESTIMENTOS to listOf("investissement", "epargne", "actions", "bourse")
            )
        ),
        "es" to mapOf(
            "receita" to listOf("salario", "ingreso", "deposito", "transferencia recibida", "credito", "pago recibido"),
            "despesa" to listOf("pago", "compra", "factura", "tarjeta", "uber", "pedidosya", "mercado", "tasa", "cuota"),
            "aplicacao" to listOf("inversion", "retiro", "cdb", "ahorro", "bono", "tesoro"),
            "categorias" to mapOf(
                ContasContract.CATEGORIA_ALIMENTACAO to listOf("mercado", "supermercado", "restaurante", "comida a domicilio"),
                ContasContract.CATEGORIA_TRANSPORTE to listOf("uber", "taxi", "combustible", "estacionamiento", "autobus", "metro"),
                ContasContract.CATEGORIA_LAZER to listOf("cine", "concierto", "netflix", "spotify", "viaje"),
                ContasContract.CATEGORIA_MORADIA to listOf("alquiler", "condominio", "luz", "agua", "gas", "impuesto"),
                ContasContract.CATEGORIA_SAUDE to listOf("farmacia", "medico", "dentista", "seguro salud", "examen"),
                ContasContract.CATEGORIA_EDUCACAO to listOf("curso", "universidad", "libro", "material escolar"),
                ContasContract.CATEGORIA_VESTUARIO to listOf("ropa", "zapatos", "vestimenta"),
                ContasContract.CATEGORIA_INVESTIMENTOS to listOf("inversion", "ahorro", "acciones", "bolsa")
            )
        )
    )

    private val geminiModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Normaliza uma string removendo acentos e convertendo para minúsculas.
     */
    private fun normalizar(texto: String): String {
        return texto.lowercase()
            .replace("[áàâãä]".toRegex(), "a")
            .replace("[éèêë]".toRegex(), "e")
            .replace("[íìîï]".toRegex(), "i")
            .replace("[óòôõö]".toRegex(), "o")
            .replace("[úùûü]".toRegex(), "u")
            .replace("ç", "c")
            .replace("ñ", "n")
            .replace("[^a-z0-9 ]".toRegex(), " ") // Remove caracteres especiais
            .replace(Regex("\\s+"), " ")          // Normaliza espaços
            .trim()
    }

    /**
     * Classifica uma transação com base na descrição e no idioma.
     * @param descricao Texto da descrição
     * @param tipoPadrao Tipo sugerido (ex: baseado no sinal do valor)
     * @param idioma Código ISO 639-1 (pt, en, fr, es)
     * @param usarIA Se deve tentar IA quando as regras falharem
     * @return ClassificacaoResult(tipo, classe, categoria)
     */
    suspend fun classificar(
        descricao: String,
        tipoPadrao: Int? = null,
        idioma: String = getCurrentLanguage(),
        usarIA: Boolean = true
    ): ClassificacaoResult {
        val normalizedDesc = normalizar(descricao)
        val dict = keywords[idioma] ?: keywords["pt"]!!

        // 1. Tentar regras locais
        val tipoLocal = detectarTipo(normalizedDesc, dict) ?: tipoPadrao
        val categoriaLocal = detectarCategoria(normalizedDesc, dict)
        val classeLocal = detectarClasse(normalizedDesc, tipoLocal ?: ContasContract.TIPO_DESPESA, dict)

        // Se encontrou algo específico (não é OUTROS) ou se temos o tipo, retornamos
        if ((tipoLocal != null) && (categoriaLocal != null && categoriaLocal != ContasContract.CATEGORIA_OUTROS)) {
            return ClassificacaoResult(
                tipo = tipoLocal,
                classe = classeLocal,
                categoria = categoriaLocal
            )
        }

        // 2. Se a IA é permitida, classificar via Gemini (usando o tipoPadrao como dica)
        if (usarIA) {
            val resultIA = classificarComIA(descricao, tipoLocal ?: tipoPadrao, idioma)
            // Se a IA falhou em retornar algo útil, usamos o que temos do local
            if (resultIA.categoria != ContasContract.CATEGORIA_OUTROS || tipoLocal != null) {
                val tipoFinal = tipoLocal ?: resultIA.tipo
                // Priorizamos a classe local se ela detectar algo específico (como Cartão),
                // caso contrário usamos o que a IA sugeriu.
                val classeFinal = if (classeLocal != ContasContract.CLASSE_DESPESA_VARIAVEL) {
                    classeLocal
                } else {
                    resultIA.classe
                }
                
                return resultIA.copy(
                    tipo = tipoFinal,
                    classe = classeFinal
                )
            }
        }

        // Fallback final
        val tipoFinal = tipoLocal ?: tipoPadrao ?: ContasContract.TIPO_DESPESA
        return ClassificacaoResult(
            tipo = tipoFinal,
            classe = classeLocal,
            categoria = categoriaLocal ?: ContasContract.CATEGORIA_OUTROS
        )
    }

    private fun gerarClassePadrao(tipo: Int): Int {
        return when (tipo) {
            ContasContract.TIPO_DESPESA -> ContasContract.CLASSE_DESPESA_VARIAVEL
            ContasContract.TIPO_APLICACAO -> ContasContract.CLASSE_APLICACAO_OUTRAS
            else -> 0
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun detectarClasse(desc: String, tipo: Int, dict: Map<String, Any>): Int {
        if (tipo != ContasContract.TIPO_DESPESA) return gerarClassePadrao(tipo)
        
        val classMap = dict["classes"] as? Map<Int, List<String>> ?: return gerarClassePadrao(tipo)
        for ((classId, palavras) in classMap) {
            if (palavras.any { desc.contains(it) }) return classId
        }
        return gerarClassePadrao(tipo)
    }

    @Suppress("UNCHECKED_CAST")
    private fun detectarTipo(desc: String, dict: Map<String, Any>): Int? {
        val receitaList = dict["receita"] as? List<String> ?: emptyList()
        if (receitaList.any { desc.contains(it) }) return ContasContract.TIPO_RECEITA

        val despesaList = dict["despesa"] as? List<String> ?: emptyList()
        if (despesaList.any { desc.contains(it) }) return ContasContract.TIPO_DESPESA

        val aplicacaoList = dict["aplicacao"] as? List<String> ?: emptyList()
        if (aplicacaoList.any { desc.contains(it) }) return ContasContract.TIPO_APLICACAO

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun detectarCategoria(desc: String, dict: Map<String, Any>): Int? {
        val catMap = dict["categorias"] as? Map<Int, List<String>> ?: return null
        var bestCat: Int? = null
        var minIndex = Int.MAX_VALUE

        for ((catId, palavras) in catMap) {
            for (p in palavras) {
                val idx = desc.indexOf(p)
                if (idx != -1 && idx < minIndex) {
                    minIndex = idx
                    bestCat = catId
                }
            }
        }
        return bestCat
    }

    private suspend fun classificarComIA(descricao: String, tipoSugerido: Int?, idioma: String): ClassificacaoResult {
        return withContext(Dispatchers.IO) {
            try {
                val tipoHint = when (tipoSugerido) {
                    ContasContract.TIPO_RECEITA -> "receita"
                    ContasContract.TIPO_APLICACAO -> "aplicacao"
                    ContasContract.TIPO_DESPESA -> "despesa"
                    else -> "desconhecido"
                }

                val prompt = """
                    Você é um classificador financeiro especialista. Analise a transação e retorne JSON.
                    
                    Descrição: "$descricao"
                    Dica de Tipo: $tipoHint
                    Idioma: $idioma
                    
                    Regras de prioridade:
                    1. "Posto [Nome]" ou "Combustivel" -> categoria: transporte.
                    2. "Mercado", "Supermercado" -> categoria: alimentacao.
                    3. Termos como VISA, MASTER, ITAUCARD, CARD, BANCO -> classe: cartao.
                    4. Aluguel, Condominio, Assinatura -> classe: fixa.
                    
                    Responda APENAS com JSON:
                    {
                      "tipo": "receita" | "despesa" | "aplicacao",
                      "classe": "cartao" | "fixa" | "variavel" | "prestacoes" | "outras",
                      "categoria": "alimentacao" | "transporte" | "lazer" | "moradia" | "saude" | "educacao" | "vestuario" | "investimentos" | "outros"
                    }
                """.trimIndent()
                val response = geminiModel.generateContent(prompt)
                val json = response.text?.trim() ?: "{}"
                
                // Regex mais flexíveis para extrair os valores do JSON
                val tipoStr = Regex("""(?i)"tipo"\s*:\s*"(\w+)"""").find(json)?.groupValues?.get(1)?.lowercase()
                val classeStr = Regex("""(?i)"classe"\s*:\s*"(\w+)"""").find(json)?.groupValues?.get(1)?.lowercase()
                val catStr = Regex("""(?i)"categoria"\s*:\s*"(\w+)"""").find(json)?.groupValues?.get(1)?.lowercase()

                val tipo = when (tipoStr) {
                    "receita" -> ContasContract.TIPO_RECEITA
                    "aplicacao" -> ContasContract.TIPO_APLICACAO
                    else -> ContasContract.TIPO_DESPESA
                }

                val classe = when (classeStr) {
                    "cartao" -> ContasContract.CLASSE_DESPESA_CARTAO
                    "fixa" -> ContasContract.CLASSE_DESPESA_FIXA
                    "prestacoes" -> ContasContract.CLASSE_DESPESA_PRESTACOES
                    "outras" -> if (tipo == ContasContract.TIPO_APLICACAO) ContasContract.CLASSE_APLICACAO_OUTRAS else 0
                    else -> if (tipo == ContasContract.TIPO_DESPESA) ContasContract.CLASSE_DESPESA_VARIAVEL else 0
                }
                
                val categoria = when (catStr) {
                    "alimentacao" -> ContasContract.CATEGORIA_ALIMENTACAO
                    "transporte" -> ContasContract.CATEGORIA_TRANSPORTE
                    "lazer" -> ContasContract.CATEGORIA_LAZER
                    "moradia" -> ContasContract.CATEGORIA_MORADIA
                    "saude" -> ContasContract.CATEGORIA_SAUDE
                    "educacao" -> ContasContract.CATEGORIA_EDUCACAO
                    "vestuario" -> ContasContract.CATEGORIA_VESTUARIO
                    "investimentos" -> ContasContract.CATEGORIA_INVESTIMENTOS
                    else -> ContasContract.CATEGORIA_OUTROS
                }
                
                val tipoFinal = tipoSugerido ?: tipo
                ClassificacaoResult(tipoFinal, if (tipoSugerido != null) gerarClassePadrao(tipoSugerido) else classe, categoria)
            } catch (e: Exception) {
                Log.e(TAG, "Erro na classificação por IA: ${e.message}")
                val fallbackTipo = tipoSugerido ?: ContasContract.TIPO_DESPESA
                ClassificacaoResult(fallbackTipo, gerarClassePadrao(fallbackTipo), ContasContract.CATEGORIA_OUTROS)
            }
        }
    }

    private fun getCurrentLanguage(): String {
        return context.resources.configuration.locales[0].language
    }

    data class ClassificacaoResult(
        val tipo: Int,
        val classe: Int,
        val categoria: Int
    )
}