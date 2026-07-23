package com.msk.minhascontas.db

import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.msk.minhascontas.R
import com.msk.minhascontas.db.DBContas.ContaFilter
import com.msk.minhascontas.db.DBContas.TipoAtualizacao
import com.msk.minhascontas.db.DBContas.TipoExclusao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Repositório que centraliza o acesso aos dados de Contas.
 * Atualmente utiliza o DBContas (SQLite tradicional), mas está preparado
 * para ser migrado para Room sem impactar a interface do usuário.
 */
class ContasRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val dbContas: DBContas = DBContas.getInstance(context)
    private val appDatabase: AppDatabase = AppDatabase.getDatabase(context)
    private val metasRepository = MetasRepository(context)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Mutex para evitar condições de corrida em importações automáticas
    private val importMutex = kotlinx.coroutines.sync.Mutex()
    // Cache de meses processados para evitar IO repetitivo na mesma sessão
    private val mesesProcessadosCache = mutableSetOf<String>()

    init {
        checkAndMigrateLegacyData()
    }

    private fun checkAndMigrateLegacyData() {
        repositoryScope.launch {
            try {
                // 1. Verifica se o Room está vazio
                val countRoom = appDatabase.contaDao().count()

                if (countRoom == 0) {
                    // 2. Se o Room estiver vazio, verifica se o Legado tem dados
                    val countLegacy = dbContas.quantasContas()
                    if (countLegacy > 0) {
                        Log.i(TAG, "Detectado banco legado com $countLegacy contas. Iniciando migração automática para Room...")
                        
                        val legacyContas = dbContas.getAllContasDetalhado()
                        if (legacyContas.isNotEmpty()) {
                            // 3. Insere no Room
                            appDatabase.contaDao().insertAll(legacyContas)
                            Log.i(TAG, "Migração para Room concluída com sucesso ($countLegacy registros).")
                        }
                    } else {
                        Log.d(TAG, "Banco legado vazio. Nenhuma migração necessária.")
                    }
                } else {
                    Log.d(TAG, "Room já contém dados ($countRoom registros). Ignorando migração.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro durante a migração do legado para Room", e)
            }
        }
    }

    companion object {
        private const val TAG = "ContasRepository"
        @Volatile
        private var instance: ContasRepository? = null

        @JvmStatic
        fun getInstance(context: Context): ContasRepository {
            return instance ?: synchronized(this) {
                instance ?: ContasRepository(context).also { instance = it }
            }
        }
    }

    // --- Operações de Leitura ---

    suspend fun getConta(id: Long): Conta? {
        return appDatabase.contaDao().getContaById(id)
    }

    suspend fun getSugestoesContas(): List<Conta> {
        return withContext(Dispatchers.IO) {
            appDatabase.contaDao().getUniqueContasByName()
        }
    }

    /**
     * Retorna um Cursor para uma conta específica.
     * Útil para compatibilidade com adaptadores Legados (CursorAdapter).
     */
    fun getContaCursor(id: Long): Cursor? {
        return dbContas.getContaPeloId(id)
    }

    suspend fun getContas(filter: ContaFilter?, order: String?): List<Conta> {
        val whereClause = filter?.buildWhereClause() ?: ""
        val whereArgs = filter?.buildWhereArgs() ?: emptyArray<String>()

        // Tratamento robusto para evitar comandos SQL inválidos ou injeções de sintaxe
        val orderByClean = if (!order.isNullOrBlank()) " ORDER BY $order" else ""

        val queryStr = StringBuilder("SELECT * FROM ").append(ContasContract.Colunas.TABELA_NOME)
        if (whereClause.isNotBlank()) {
            queryStr.append(" WHERE ").append(whereClause)
        }
        queryStr.append(orderByClean)

        val query = SimpleSQLiteQuery(queryStr.toString(), whereArgs)
        return appDatabase.contaDao().getContasFilteredSync(query)
    }

    /**
     * Retorna um Cursor para uma lista de contas filtradas.
     * Útil para compatibilidade com adaptadores Legados (CursorAdapter).
     */
    fun getContasCursor(filter: ContaFilter?, order: String?): Cursor? {
        return dbContas.getContasByFilter(filter, order)
    }

    suspend fun getContasDoMes(mes: Int, ano: Int, tipo: Int, filtro: ContaFilter?): List<Conta> {
        val f = filtro ?: ContaFilter()
        f.setMes(mes).setAno(ano)
        if (tipo != -1) f.setTipo(tipo)
        
        val whereClause = f.buildWhereClause()
        val whereArgs = f.buildWhereArgs()
        val orderBy = "${ContasContract.Colunas.COLUNA_DIA_DATA_CONTA} ASC, ${ContasContract.Colunas.COLUNA_VALOR_CONTA} DESC"
        val queryStr = "SELECT * FROM ${ContasContract.Colunas.TABELA_NOME}" +
                (if (whereClause.isNotBlank()) " WHERE $whereClause" else "") +
                " ORDER BY $orderBy"

        val query = SimpleSQLiteQuery(queryStr, whereArgs)
        return appDatabase.contaDao().getContasFilteredSync(query)
    }

    suspend fun somaValoresPorFiltro(ano: Int, mes: Int, tipo: Int, classe: Int, categoria: Int, status: String?, diaFim: Int = -1): Double {
        val filter = ContaFilter()
            .setAno(ano)
            .setMes(mes)
            .setTipo(tipo)
            .setClasse(classe)
            .setCategoria(categoria)
            .setPagamento(status)
            .setDiaFim(diaFim)
        
        return calcularTotalMensal(mes, ano, tipo, filter)
    }

    suspend fun calcularTotalMensal(mes: Int, ano: Int, tipo: Int, filtro: ContaFilter?): Double {
        val f = filtro ?: ContaFilter()
        f.setMes(mes).setAno(ano).setTipo(tipo)

        val whereClause = f.buildWhereClause() ?: ""
        val whereArgs = f.buildWhereArgs() ?: emptyArray<String>()

        val queryStr = StringBuilder("SELECT SUM(")
            .append(ContasContract.Colunas.COLUNA_VALOR_CONTA)
            .append(") FROM ")
            .append(ContasContract.Colunas.TABELA_NOME)

        if (whereClause.isNotBlank()) {
            queryStr.append(" WHERE ").append(whereClause)
        }

        val query = SimpleSQLiteQuery(queryStr.toString(), whereArgs)
        return appDatabase.contaDao().getSumFilteredSync(query) ?: 0.0
    }

    /**
     * Retorna um Flow com o total somado baseado no filtro, usando Room.
     */
    fun calcularTotalFlow(mes: Int, ano: Int, tipo: Int, filtro: ContaFilter?): Flow<Double> {
        val f = filtro ?: ContaFilter()
        f.setMes(mes).setAno(ano).setTipo(tipo)
        
        val whereClause = f.buildWhereClause()
        val whereArgs = f.buildWhereArgs()
        
        val queryStr = "SELECT SUM(${ContasContract.Colunas.COLUNA_VALOR_CONTA}) FROM ${ContasContract.Colunas.TABELA_NOME}" +
                (if (whereClause.isNotBlank()) " WHERE $whereClause" else "")

        val query = SimpleSQLiteQuery(queryStr, whereArgs)
        return appDatabase.contaDao().getSumFiltered(query).map { it ?: 0.0 }
    }

    suspend fun somaAplicacoesAnteriores(dia: Int, mes: Int, ano: Int, isMonthly: Boolean, classe: Int): Double {
        return if (isMonthly) {
            appDatabase.contaDao().sumPreviousMonthsByClass(ano, mes, ContasContract.TIPO_APLICACAO, classe)
        } else {
            appDatabase.contaDao().sumPreviousDaysByClass(ano, mes, dia, ContasContract.TIPO_APLICACAO, classe)
        } ?: 0.0
    }

    suspend fun somaSaldoAnterior(dia: Int, mes: Int, ano: Int, isMonthly: Boolean): Double {
        val sums = if (isMonthly) {
            appDatabase.contaDao().sumPreviousMonthsGrouped(ano, mes)
        } else {
            appDatabase.contaDao().sumPreviousDaysGrouped(ano, mes, dia)
        }
        
        val recTotal = sums.find { it.tipo == ContasContract.TIPO_RECEITA }?.total ?: 0.0
        val despTotal = sums.find { it.tipo == ContasContract.TIPO_DESPESA }?.total ?: 0.0
        
        return recTotal - despTotal
    }

    suspend fun somaValoresNoPeriodo(diaInicio: Int, diaFim: Int, mes: Int, ano: Int, tipo: Int, classe: Int, categoria: Int, status: String?): Double {
        return appDatabase.contaDao().sumInPeriod(diaInicio, diaFim, mes, ano, tipo, classe, categoria, status) ?: 0.0
    }

    suspend fun getMediaCategoriaUltimosMeses(categoria: Int, mesesAtras: Int): Double {
        val cal = Calendar.getInstance()
        val endYearMonth = cal.get(Calendar.YEAR) * 12 + (cal.get(Calendar.MONTH) + 1)
        cal.add(Calendar.MONTH, -mesesAtras)
        val startYearMonth = cal.get(Calendar.YEAR) * 12 + (cal.get(Calendar.MONTH) + 1)

        val sums = appDatabase.contaDao().getMonthlySums(categoria, startYearMonth, endYearMonth - 1)
        val filteredSums = sums.filter { it.total > 0 }
        
        return if (filteredSums.isEmpty()) 0.0 else filteredSums.sumOf { it.total } / filteredSums.size
    }

    /**
     * Coleta os valores do resumo financeiro para um determinado mês e ano.
     * Otimizado para usar Room e evitar IO redundante.
     */
    suspend fun coletaDadosResumo(context: Context, mes: Int, ano: Int): Array<String?> {
        val res = context.resources
        val ptBr = java.util.Locale("pt", "BR")
        val dinheiro = java.text.NumberFormat.getCurrencyInstance(ptBr)
        
        val despesasCategorias = res.getStringArray(R.array.TipoDespesa)
        val receitasCategorias = res.getStringArray(R.array.TipoReceita)
        val aplicacoesCategorias = res.getStringArray(R.array.TipoAplicacao)

        val ajusteReceita = if (receitasCategorias.size > 1) receitasCategorias.size else 0
        val numLinhasResumo = despesasCategorias.size + ajusteReceita + aplicacoesCategorias.size + 9
        val valores = arrayOfNulls<String>(numLinhasResumo)
        var indice = 0

        // Busca todos os totais por categoria em uma única query
        val sumsDespesa = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_DESPESA)
            .associate { it.categoria to it.total }
        val sumsReceita = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_RECEITA)
            .associate { it.categoria to it.total }

        // --- DESPESAS ---
        valores[indice++] = ""
        for (i in despesasCategorias.indices) {
            valores[indice++] = dinheiro.format(sumsDespesa[i] ?: 0.0)
        }

        // --- RECEITAS ---
        valores[indice++] = ""
        if (receitasCategorias.size > 1) {
            for (i in receitasCategorias.indices) {
                valores[indice++] = dinheiro.format(sumsReceita[i] ?: 0.0)
            }
        }

        // --- APLICAÇÕES ---
        valores[indice++] = ""
        var totalAplicacoes = 0.0
        for (i in aplicacoesCategorias.indices) {
            // Para aplicações, a classe_conta é o que define a categoria no resumo legado
            val valor = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_APLICACAO, i, -1, null) ?: 0.0
            valores[indice++] = dinheiro.format(valor)
            totalAplicacoes += valor
        }

        // --- TOTAIS ---
        val totalDPaga = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO) ?: 0.0
        val totalDPendente = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PENDENTE) ?: 0.0
        val totalRRecebida = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO) ?: 0.0
        val totalRPendente = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PENDENTE) ?: 0.0

        valores[indice++] = dinheiro.format(totalRRecebida - totalDPaga)
        valores[indice++] = dinheiro.format(totalDPaga)
        valores[indice++] = dinheiro.format(totalDPendente)
        valores[indice++] = dinheiro.format(totalRRecebida)
        valores[indice++] = dinheiro.format(totalRPendente)
        valores[indice] = dinheiro.format(totalAplicacoes)

        return valores
    }

    fun getNomeLinhas(context: Context): Array<String?> {
        return dbContas.NomeLinhas(context) ?: emptyArray<String?>()
    }

    fun getListaContasCompletaCursor(mes: Int, ano: Int): Cursor? {
        return dbContas.listaContasCompleta(mes, ano)
    }

    /**
     * Retorna um Flow de contas baseado no filtro e ordenação, usando Room.
     */
    fun getContasFlow(filter: ContaFilter?, order: String?): Flow<List<Conta>> {
        val whereClause = filter?.buildWhereClause() ?: ""
        val whereArgs = filter?.buildWhereArgs() ?: emptyArray<String>()
        val orderBy = if (order.isNullOrBlank()) "" else " ORDER BY $order"
        val queryStr = "SELECT * FROM ${ContasContract.Colunas.TABELA_NOME}" +
                (if (whereClause.isNotBlank()) " WHERE $whereClause" else "") +
                orderBy

        val query = SimpleSQLiteQuery(queryStr, whereArgs)
        return appDatabase.contaDao().getContasFiltered(query)
    }

    /**
     * Importa contas fixas de um mês para o outro com proteção contra duplicidade.
     */
    suspend fun importarFixasDeMesAnterior(mes: Int, ano: Int): Int {
        return importMutex.withLock {
            val chaveMes = "$mes-$ano"
            
            // 1. Verificação rápida em cache (Idempotência)
            if (mesesProcessadosCache.contains(chaveMes)) return@withLock 0

            // 2. Verifica se já existem fixas no mês atual para evitar duplicidade física
            val filtroAtual = ContaFilter()
                .setMes(mes)
                .setAno(ano)
                .setClasse(ContasContract.CLASSE_DESPESA_FIXA)
            
            val fixasAtuais = getContas(filtroAtual, null)
            if (fixasAtuais.isNotEmpty()) {
                mesesProcessadosCache.add(chaveMes)
                return@withLock 0 
            }

            val cal = Calendar.getInstance()
            cal.set(ano, mes - 1, 1)
            cal.add(Calendar.MONTH, -1)
            
            val mesAnt = cal.get(Calendar.MONTH) + 1
            val anoAnt = cal.get(Calendar.YEAR)

            // 3. Busca fixas do mês anterior
            val filtroAnt = ContaFilter()
                .setMes(mesAnt)
                .setAno(anoAnt)
                .setClasse(ContasContract.CLASSE_DESPESA_FIXA)
            
            val fixasAnteriores = getContas(filtroAnt, null)
            if (fixasAnteriores.isEmpty()) return@withLock 0

            // 4. Clona os registros
            val novasContas = fixasAnteriores.map { antiga ->
                Conta.Builder(antiga.nome, antiga.valor, antiga.dia, mes, ano, java.util.UUID.randomUUID().toString())
                    .setTipo(antiga.tipo)
                    .setClasseConta(antiga.classeConta)
                    .setCategoria(antiga.categoria)
                    .setPagamento(ContasContract.STATUS_PENDENTE)
                    .setQtRepete(1)
                    .setNRepete(1)
                    .setIntervalo(0)
                    .setValorJuros(antiga.valorJuros)
                    .build()
            }

            val inseridos = inserirContasEmMassa(novasContas)
            if (inseridos > 0) {
                mesesProcessadosCache.add(chaveMes)
            }
            inseridos
        }
    }

    /**
     * Retorna a soma de gastos (Despesas) agrupados por categoria para um período.
     * Otimizado para realizar uma única consulta ao banco via Room.
     */
    suspend fun getGastosPorCategoria(mes: Int, ano: Int): Map<Int, Double> {
        val despesasMap = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_DESPESA)
            .associate { it.categoria to it.total }

        val aplicacoesTotal = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_APLICACAO)
            .sumOf { it.total }

        val result = mutableMapOf<Int, Double>()
        for (i in 0..7) {
            result[i] = despesasMap[i] ?: 0.0
        }
        result[8] = aplicacoesTotal
        return result
    }

    fun cursorToListaContas(cursor: Cursor?): MutableList<Conta> {
        return (dbContas.cursorToListaContas(cursor) ?: mutableListOf<Conta>()) as MutableList<Conta>
    }

    // --- Operações de Escrita ---

    suspend fun salvarConta(conta: Conta): Long {
        return withContext(Dispatchers.IO) {
            val id = dbContas.geraConta(conta)
            if (id > 0) {
                conta.idConta = id
                appDatabase.contaDao().insert(conta)
            }
            id
        }
    }

    suspend fun salvarContasRecorrentes(conta: Conta, qtRepeticoes: Int, intervalo: Int) {
        withContext(Dispatchers.IO) {
            dbContas.geraContasRecorrentes(conta, qtRepeticoes, intervalo)
            val filter = ContaFilter().setCodigoConta(conta.codigo)
            val series = dbContas.getContas(filter, null) ?: emptyList()
            appDatabase.contaDao().insertAll(series)
        }
    }

    suspend fun inserirContasEmMassa(contas: List<Conta>): Int {
        return withContext(Dispatchers.IO) {
            // Filtrar contas que já podem existir para evitar duplicidade lógica no banco
            val contasParaInserir = contas.filter { nova ->
                !existeNoBanco(nova)
            }

            if (contasParaInserir.isEmpty()) return@withContext 0

            val result = dbContas.inserirContasEmMassa(contasParaInserir)
            if (result > 0) {
                appDatabase.contaDao().insertAll(contasParaInserir)
            }
            result
        }
    }

    /**
     * Verifica se uma conta com as mesmas características básicas já existe.
     */
    suspend fun existeNoBanco(conta: Conta): Boolean {
        val filtro = DBContas.ContaFilter()
            .setNome(conta.nome)
            .setDia(conta.dia)
            .setMes(conta.mes)
            .setAno(conta.ano)
        
        val existentes = getContas(filtro, null)
        return existentes.any { it.valor == conta.valor }
    }

    /**
     * Conta quantas das contas fornecidas já existem no banco de dados.
     */
    suspend fun contarDuplicados(contas: List<Conta>): Int {
        var duplicados = 0
        contas.forEach { 
            if (existeNoBanco(it)) duplicados++
        }
        return duplicados
    }

    suspend fun atualizarConta(conta: Conta): Boolean {
        return withContext(Dispatchers.IO) {
            val result = dbContas.alteraConta(conta)
            if (result) {
                appDatabase.contaDao().update(conta)
            }
            result
        }
    }

    /**
     * Atualiza múltiplas contas de uma vez.
     * @param ids Lista de IDs das contas a serem alteradas.
     * @param novosValores Mapa com nome da coluna e novo valor.
     * @return Número de contas atualizadas.
     */
    suspend fun atualizarContasEmMassa(ids: List<Long>, novosValores: Map<String, Any?>): Int {
        return withContext(Dispatchers.IO) {
            val contentValues = android.content.ContentValues()
            novosValores.forEach { (key, value) ->
                when (value) {
                    is String -> contentValues.put(key, value)
                    is Int -> contentValues.put(key, value)
                    is Double -> contentValues.put(key, value)
                    is Long -> contentValues.put(key, value)
                    is Boolean -> contentValues.put(key, value)
                    null -> contentValues.putNull(key)
                }
            }

            val result = dbContas.atualizarContasEmMassa(ids, contentValues)
            if (result > 0) {
                // Sincroniza o Room com os dados atualizados do banco legado para as contas afetadas
                val contasAtualizadas = mutableListOf<Conta>()
                for (id in ids) {
                    val c = dbContas.getConta(id)
                    if (c != null) contasAtualizadas.add(c)
                }
                if (contasAtualizadas.isNotEmpty()) {
                    appDatabase.contaDao().insertAll(contasAtualizadas)
                }
            }
            result
        }
    }

    suspend fun atualizarContasRecorrentes(conta: Conta, tipo: TipoAtualizacao) {
        withContext(Dispatchers.IO) {
            dbContas.alteraContasRecorrentes(conta, tipo)
            val filter = ContaFilter().setCodigoConta(conta.codigo)
            val series = dbContas.getContas(filter, null) ?: emptyList()
            appDatabase.contaDao().insertAll(series)
        }
    }

    suspend fun atualizarPagamento(id: Long, status: String): Int {
        return withContext(Dispatchers.IO) {
            val result = dbContas.updateContaPagamento(id, status)
            if (result > 0) {
                val conta = dbContas.getConta(id)
                conta?.let { 
                    appDatabase.contaDao().update(it)
                    if (!it.codigo.isNullOrEmpty()) {
                        val meta = metasRepository.metas.value?.find { m -> m.codigoVinculo == it.codigo }
                        meta?.let { m -> atualizarProgressoRealMeta(m) }
                    }
                }
            }
            result
        }
    }

    suspend fun atualizarPagamentoContas(dia: Int, mes: Int, ano: Int): Int {
        return withContext(Dispatchers.IO) {
            val result = dbContas.atualizaPagamentoContas(dia, mes, ano)
            if (result > 0) {
                // Sincroniza o Room com os dados atualizados do banco legado
                val todas = dbContas.getAllContasDetalhado()
                appDatabase.contaDao().insertAll(todas)
            }
            result
        }
    }

    suspend fun confirmaPagamentos(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = dbContas.confirmaPagamentos()
            if (result) {
                val todas = dbContas.getAllContasDetalhado()
                appDatabase.contaDao().insertAll(todas)
            }
            result
        }
    }

    suspend fun ajustaRepeticoesContas(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = dbContas.ajustaRepeticoesContas()
            if (result) {
                val todas = dbContas.getAllContasDetalhado()
                appDatabase.contaDao().insertAll(todas)
            }
            result
        }
    }

    // --- Operações de Exclusão ---

    suspend fun excluirConta(id: Long): Int {
        return withContext(Dispatchers.IO) {
            val result = dbContas.deleteConta(id)
            if (result > 0) {
                val conta = appDatabase.contaDao().getContaById(id)
                conta?.let { appDatabase.contaDao().delete(it) }
            }
            result
        }
    }

    suspend fun excluirContasRecorrentes(id: Long, codigo: String, nr: Int, tipo: TipoExclusao): Boolean {
        return withContext(Dispatchers.IO) {
            val result = dbContas.deletarContasRecorrentes(id, codigo, nr, tipo)
            if (result) {
                // Sincroniza a exclusão no Room para evitar "contas zumbis"
                when (tipo) {
                    TipoExclusao.SOMENTE_ESTA -> {
                        val conta = appDatabase.contaDao().getContaById(id)
                        conta?.let { appDatabase.contaDao().delete(it) }
                    }
                    TipoExclusao.DESTA_EM_DIANTE -> {
                        appDatabase.contaDao().deleteByCodigoFrom(codigo, nr)
                    }
                    TipoExclusao.TODAS_AS_REPETICOES -> {
                        appDatabase.contaDao().deleteByCodigo(codigo)
                    }
                }

                val filter = ContaFilter().setCodigoConta(codigo)
                val restantes = dbContas.getContas(filter, null) ?: emptyList()
                appDatabase.contaDao().insertAll(restantes)
            }
            result
        }
    }

    suspend fun excluirTudo() {
        withContext(Dispatchers.IO) {
            dbContas.deleteAllContas()
            appDatabase.contaDao().deleteAll()
        }
    }

    /**
     * Verifica se existem séries de contas que terminam no mês especificado e gera notificação.
     * Melhoria: Adicionado filtro de pagamento para evitar alertas precoces.
     */
    fun verificarFimDeSeries(mes: Int, ano: Int) {
        repositoryScope.launch {
            val filter = ContaFilter().setMes(mes).setAno(ano)
            val contas = dbContas.getContas(filter, null) ?: emptyList()
            
            contas.filter { 
                it.nRepete == it.qtRepete && 
                it.qtRepete > 1 && 
                it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO 
            }.forEach { conta ->
                dbContas.addNotificacao(
                    appContext.getString(R.string.msg_fim_serie_titulo),
                    appContext.getString(R.string.msg_fim_serie_corpo, conta.nome),
                    "fim_serie|${conta.idConta}"
                )
            }
        }
    }

    /**
     * Renova uma série de contas que chegou ao fim, criando uma nova série
     * com as mesmas configurações para o período seguinte.
     */
    fun renovarSerie(idConta: Long) {
        repositoryScope.launch {
            val contaBase = dbContas.getConta(idConta) ?: return@launch

            val cal = Calendar.getInstance()
            cal.set(contaBase.ano, contaBase.mes - 1, contaBase.dia)

            // Calcula a próxima data com base no intervalo
            when (contaBase.intervalo) {
                300 -> cal.add(Calendar.MONTH, 1) // Mensal
                3650 -> cal.add(Calendar.YEAR, 1) // Anual
                else -> cal.add(Calendar.DATE, if (contaBase.intervalo > 100) contaBase.intervalo - 100 else 1)
            }

            // Cria a base para a nova série mantendo configurações, mas com nova data
            val novaSerieBase = Conta.Builder(
                contaBase.nome,
                contaBase.valor,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR),
                java.util.UUID.randomUUID().toString()
            ).apply {
                setTipo(contaBase.tipo)
                setClasseConta(contaBase.classeConta)
                setCategoria(contaBase.categoria)
                setPagamento(DBContas.PAGAMENTO_FALTA)
                setValorJuros(contaBase.valorJuros)
            }.build()

            // Gera a nova sequência de repetições
            dbContas.geraContasRecorrentes(novaSerieBase, contaBase.qtRepete, contaBase.intervalo)
        }
    }

    // --- Operações de Metas Financeiras (Coach 20%) ---

    fun getMetasAtivas(): LiveData<List<MetaFinanceira>> {
        return metasRepository.metas
    }

    fun getMetasSincrono(): List<MetaFinanceira> {
        return metasRepository.getAllGoals()
    }

    fun getMetaById(id: String): MetaFinanceira? {
        return metasRepository.metas.value?.find { it.id == id }
    }

    suspend fun salvarMeta(meta: MetaFinanceira): String {
        metasRepository.saveGoal(meta)
        return meta.id
    }

    suspend fun excluirMeta(meta: MetaFinanceira) {
        metasRepository.deleteGoal(meta.id)
    }

    /**
     * Atualiza o progresso real de uma meta consultando as contas vinculadas no banco.
     */
    suspend fun atualizarProgressoRealMeta(meta: MetaFinanceira) {
        val codigo = meta.codigoVinculo ?: return
        
        val filter = ContaFilter().setCodigoConta(codigo)
        val contasVinculadas = dbContas.getContas(filter, null) ?: emptyList()
        
        val totalRealizado = contasVinculadas
            .filter { it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO }
            .sumOf { it.valor }
            
        metasRepository.saveGoal(meta.copy(valorAtual = totalRealizado))
    }
}