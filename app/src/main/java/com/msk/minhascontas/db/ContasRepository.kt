package com.msk.minhascontas.db

import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.msk.minhascontas.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Repositório central com sincronização direta e fidedigna entre a Tela de Resumo e Gráficos.
 */
class ContasRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val dbContas: DBContas = DBContas.getInstance(context)
    private val notificationRepository = NotificationRepository.getInstance(context)
    private val appDatabase: AppDatabase = AppDatabase.getDatabase(context)
    private val metasRepository = MetasRepository(context)
    private val recurrenceManager = RecurrenceManager(appDatabase)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val importMutex = kotlinx.coroutines.sync.Mutex()
    private val mesesProcessadosCache = mutableSetOf<String>()

    init {
        repositoryScope.launch {
            LegacyMigrator(dbContas, appDatabase).checkAndMigrate()
        }
    }

    companion object {
        private const val TAG = "ContasRepository"
        const val PREF_DB_RESET_FLAG = "db_reset_flag"

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

    fun getContaCursor(id: Long): Cursor? {
        return dbContas.getContaPeloId(id)
    }

    suspend fun getContas(filter: ContaFilter?, order: String?): List<Conta> {
        val whereClause = filter?.buildWhereClause() ?: ""
        val whereArgs = filter?.buildWhereArgs() ?: emptyArray<String>()
        val orderByClean = if (!order.isNullOrBlank()) " ORDER BY $order" else ""

        val queryStr = StringBuilder("SELECT * FROM ").append(ContasContract.Colunas.TABELA_NOME)
        if (whereClause.isNotBlank()) {
            queryStr.append(" WHERE ").append(whereClause)
        }
        queryStr.append(orderByClean)

        val query = SimpleSQLiteQuery(queryStr.toString(), whereArgs)
        return appDatabase.contaDao().getContasFilteredSync(query)
    }

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

        val whereClause = f.buildWhereClause()
        val whereArgs = f.buildWhereArgs()

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

    suspend fun getMediaTipoUltimosMeses(tipo: Int, mesesAtras: Int): Double {
        val cal = Calendar.getInstance()
        val endYearMonth = cal.get(Calendar.YEAR) * 12 + (cal.get(Calendar.MONTH) + 1)
        cal.add(Calendar.MONTH, -mesesAtras)
        val startYearMonth = cal.get(Calendar.YEAR) * 12 + (cal.get(Calendar.MONTH) + 1)

        val sums = appDatabase.contaDao().getMonthlySumsByType(tipo, startYearMonth, endYearMonth - 1)
        val filteredSums = sums.filter { it.total > 0 }

        return if (filteredSums.isEmpty()) 0.0 else filteredSums.sumOf { it.total } / filteredSums.size
    }

    /**
     * Retorna a consolidação exata utilizada na Tela de Resumo Financeiro.
     */
    suspend fun getReportSummary(mes: Int, ano: Int): ReportSummary {
        val sumsDespesa = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_DESPESA)
            .associate { it.categoria to it.total }
        val sumsReceita = appDatabase.contaDao().getSumByCategorySync(mes, ano, ContasContract.TIPO_RECEITA)
            .associate { it.categoria to it.total }

        // Busca a lista completa de contas do mês para agrupar valores por dia
        val contasDoMes = getListaContasCompleta(mes, ano)

        val receitasPorDia = contasDoMes
            .filter { it.tipo == ContasContract.TIPO_RECEITA }
            .groupBy { it.dia }
            .mapValues { entry -> entry.value.sumOf { it.valor } }

        val despesasPorDia = contasDoMes
            .filter { it.tipo == ContasContract.TIPO_DESPESA }
            .groupBy { it.dia }
            .mapValues { entry -> entry.value.sumOf { it.valor } }

        val aplicacoesPorClasse = mutableMapOf<Int, Double>()
        var totalAplicacoes = 0.0
        for (i in 0..3) {
            val valor = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_APLICACAO, i, -1, null) ?: 0.0
            aplicacoesPorClasse[i] = valor
            totalAplicacoes += valor
        }

        val totalDPaga = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO) ?: 0.0
        val totalDPendente = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PENDENTE) ?: 0.0
        val totalRRecebida = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO) ?: 0.0
        val totalRPendente = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PENDENTE) ?: 0.0

        return ReportSummary(
            despesasPorCategoria = sumsDespesa,
            receitasPorCategoria = sumsReceita,
            aplicacoesPorClasse = aplicacoesPorClasse,
            receitasPorDia = receitasPorDia,
            despesasPorDia = despesasPorDia,
            totalDespesasPagas = totalDPaga,
            totalDespesasPendentes = totalDPendente,
            totalReceitasRecebidas = totalRRecebida,
            totalReceitasPendentes = totalRPendente,
            totalAplicacoes = totalAplicacoes,
            saldo = totalRRecebida - totalDPaga
        )
    }

    fun getNomeLinhas(context: Context): Array<String?> {
        val res = context.resources

        val despesa = res.getString(R.string.linha_despesa)
        val receita = res.getString(R.string.linha_receita)
        val aplicacao = res.getString(R.string.linha_aplicacoes)

        val despesas = Array(4) { i -> com.msk.minhascontas.utils.LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, i) }
        val receitas = Array(3) { i -> com.msk.minhascontas.utils.LabelUtils.getClasseLabel(context, ContasContract.TIPO_RECEITA, i) }
        val aplicacoes = Array(3) { i -> com.msk.minhascontas.utils.LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, i) }

        val ajusteReceita = if (receitas.size > 1) receitas.size else 0
        val numLinhasResumo = despesas.size + ajusteReceita + aplicacoes.size + 9
        val linhas = arrayOfNulls<String>(numLinhasResumo)
        var indice = 0

        linhas[indice++] = despesa
        System.arraycopy(despesas, 0, linhas, indice, despesas.size)
        indice += despesas.size

        linhas[indice++] = receita
        if (receitas.size > 1) {
            System.arraycopy(receitas, 0, linhas, indice, receitas.size)
            indice += receitas.size
        }

        linhas[indice++] = aplicacao
        System.arraycopy(aplicacoes, 0, linhas, indice, aplicacoes.size)
        indice += aplicacoes.size

        linhas[indice++] = res.getString(R.string.linha_saldo)
        linhas[indice++] = res.getString(R.string.resumo_pagas)
        linhas[indice++] = res.getString(R.string.resumo_faltam)
        linhas[indice++] = res.getString(R.string.resumo_recebidas)
        linhas[indice++] = res.getString(R.string.resumo_areceber)
        linhas[indice] = res.getString(R.string.linha_aplicacoes)

        return linhas
    }

    suspend fun getListaContasCompleta(mes: Int, ano: Int): List<Conta> {
        return withContext(Dispatchers.IO) {
            appDatabase.contaDao().getContasByMonthSync(mes, ano)
        }
    }

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

    suspend fun importarFixasDeMesAnterior(mes: Int, ano: Int): Int {
        return importMutex.withLock {
            val chaveMes = "$mes-$ano"
            if (mesesProcessadosCache.contains(chaveMes)) return@withLock 0

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

            val filtroAnt = ContaFilter()
                .setMes(mesAnt)
                .setAno(anoAnt)
                .setClasse(ContasContract.CLASSE_DESPESA_FIXA)

            val fixasAnteriores = getContas(filtroAnt, null)
            if (fixasAnteriores.isEmpty()) return@withLock 0

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
     * Retorna o mapa de gastos POR CATEGORIA fidedigno ao relatório de resumo financeiro.
     * Mapeia estritamente TIPO_DESPESA (0 a 7) garantindo consistência completa com a tela de Resumos.
     */
    suspend fun getGastosPorCategoria(mes: Int, ano: Int, status: String? = null): Map<Int, Double> {
        val despesasMap = appDatabase.contaDao().getSumByCategoryAndStatusSync(mes, ano, ContasContract.TIPO_DESPESA, status)
            .associate { it.categoria to it.total }

        val result = mutableMapOf<Int, Double>()
        for (i in 0..7) {
            result[i] = despesasMap[i] ?: 0.0
        }
        return result
    }

    /**
     * Retorna o mapa de receitas POR CATEGORIA garantindo sincronia total com o resumo financeiro.
     */
    suspend fun getReceitasPorCategoria(mes: Int, ano: Int, status: String? = null): Map<Int, Double> {
        val receitasMap = appDatabase.contaDao().getSumByCategoryAndStatusSync(mes, ano, ContasContract.TIPO_RECEITA, status)
            .associate { it.categoria to it.total }

        val result = mutableMapOf<Int, Double>()
        for (i in 0..3) {
            result[i] = receitasMap[i] ?: 0.0
        }
        return result
    }

    /**
     * Retorna os investimentos/aplicações organizados por classe exatamente como na tela de resumos.
     */
    suspend fun getAplicacoesPorClasse(mes: Int, ano: Int): Map<Int, Double> {
        val aplicacoesPorClasse = mutableMapOf<Int, Double>()
        for (i in 0..3) {
            val valor = appDatabase.contaDao().sumInPeriod(1, 31, mes, ano, ContasContract.TIPO_APLICACAO, i, -1, null) ?: 0.0
            aplicacoesPorClasse[i] = valor
        }
        return aplicacoesPorClasse
    }

    /**
     * Retorna o detalhamento agrupado por Categoria e Status (PAGO vs PENDENTE) para gráficos de despesas.
     */
    suspend fun getGastosDetalhadosPorCategoriaEStatus(mes: Int, ano: Int): List<ContaDao.CategoryStatusSum> {
        return appDatabase.contaDao().getSumByCategoryGroupedByStatusSync(mes, ano, ContasContract.TIPO_DESPESA)
    }

    fun cursorToListaContas(cursor: Cursor?): MutableList<Conta> {
        return (dbContas.cursorToListaContas(cursor) ?: mutableListOf<Conta>()) as MutableList<Conta>
    }

    // --- Operações de Escrita ---

    suspend fun salvarConta(conta: Conta): Long {
        return withContext(Dispatchers.IO) {
            val id = appDatabase.contaDao().insert(conta)
            if (id > 0) {
                conta.idConta = id
            }
            id
        }
    }

    suspend fun salvarContasRecorrentes(conta: Conta, qtRepeticoes: Int, intervalo: Int) {
        withContext(Dispatchers.IO) {
            val series = recurrenceManager.generateSeries(conta, qtRepeticoes, intervalo)
            appDatabase.contaDao().insertAll(series)
        }
    }

    suspend fun inserirContasEmMassa(contas: List<Conta>): Int {
        return withContext(Dispatchers.IO) {
            val contasParaInserir = contas.filter { nova ->
                !existeNoBanco(nova)
            }

            if (contasParaInserir.isEmpty()) return@withContext 0

            appDatabase.contaDao().insertAll(contasParaInserir)
            contasParaInserir.size
        }
    }

    suspend fun existeNoBanco(conta: Conta): Boolean {
        val filtro = ContaFilter()
            .setNome(conta.nome)
            .setDia(conta.dia)
            .setMes(conta.mes)
            .setAno(conta.ano)

        val existentes = getContas(filtro, null)
        return existentes.any { it.valor == conta.valor }
    }

    suspend fun contarDuplicados(contas: List<Conta>): Int {
        var duplicados = 0
        contas.forEach {
            if (existeNoBanco(it)) duplicados++
        }
        return duplicados
    }

    suspend fun atualizarConta(conta: Conta): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                appDatabase.contaDao().update(conta)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

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

    suspend fun atualizarContasRecorrentes(contaBase: Conta, tipoAtualizacao: TipoAtualizacao) {
        withContext(Dispatchers.IO) {
            if (tipoAtualizacao == TipoAtualizacao.SOMENTE_ESTA) {
                atualizarConta(contaBase)
                return@withContext
            }

            val filter = ContaFilter().setCodigoConta(contaBase.codigo)
            if (tipoAtualizacao == TipoAtualizacao.DESTA_EM_DIANTE) {
                filter.setNrRepeticaoMin(contaBase.nRepete)
            }

            val queryStr = "SELECT * FROM ${ContasContract.Colunas.TABELA_NOME} WHERE ${filter.buildWhereClause()} ORDER BY ${ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA} ASC"
            val query = SimpleSQLiteQuery(queryStr, filter.buildWhereArgs())
            val seriesParaAtualizar = appDatabase.contaDao().getContasFilteredSync(query)

            if (seriesParaAtualizar.isEmpty()) return@withContext

            val atualizadas = recurrenceManager.calculateUpdates(seriesParaAtualizar, contaBase, tipoAtualizacao)
            appDatabase.contaDao().insertAll(atualizadas)
        }
    }

    suspend fun atualizarPagamento(id: Long, status: String): Int {
        return withContext(Dispatchers.IO) {
            val conta = appDatabase.contaDao().getContaById(id)
            if (conta != null) {
                conta.pagamento = status
                appDatabase.contaDao().update(conta)

                if (!conta.codigo.isNullOrEmpty()) {
                    val meta = metasRepository.metas.value?.find { m -> m.codigoVinculo == conta.codigo }
                    meta?.let { m -> atualizarProgressoRealMeta(m) }
                }
                1
            } else 0
        }
    }

    suspend fun atualizarPagamentoContas(dia: Int, mes: Int, ano: Int): Int {
        return withContext(Dispatchers.IO) {
            appDatabase.contaDao().updatePastDueToPaid(ano, mes, dia, ContasContract.STATUS_PAGO_RECEBIDO, ContasContract.STATUS_PENDENTE)
        }
    }

    suspend fun confirmaPagamentos(): Boolean {
        return withContext(Dispatchers.IO) {
            appDatabase.contaDao().resetNonPaidToPending(ContasContract.STATUS_PAGO_RECEBIDO, ContasContract.STATUS_PENDENTE) > 0
        }
    }

    suspend fun ajustaRepeticoesContas(): Boolean {
        return withContext(Dispatchers.IO) {
            appDatabase.contaDao().adjustIntervals() > 0
        }
    }

    // --- Operações de Exclusão ---

    suspend fun excluirConta(id: Long): Int {
        return withContext(Dispatchers.IO) {
            val conta = appDatabase.contaDao().getContaById(id)
            if (conta != null) {
                appDatabase.contaDao().delete(conta)
                1
            } else 0
        }
    }

    suspend fun excluirContasRecorrentes(id: Long, codigo: String, nr: Int, tipo: TipoExclusao): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                when (tipo) {
                    TipoExclusao.SOMENTE_ESTA -> {
                        val conta = appDatabase.contaDao().getContaById(id)
                        conta?.let { appDatabase.contaDao().delete(it) }
                    }
                    TipoExclusao.DESTA_EM_DIANTE -> {
                        appDatabase.contaDao().deleteByCodigoFrom(codigo, nr)
                        val novoQt = nr - 1
                        if (novoQt > 0) {
                            val filter = ContaFilter().setCodigoConta(codigo).setNrRepeticaoMax(novoQt)
                            val queryStr = "SELECT * FROM ${ContasContract.Colunas.TABELA_NOME} WHERE ${filter.buildWhereClause()}"
                            val query = SimpleSQLiteQuery(queryStr, filter.buildWhereArgs())
                            val restantes = appDatabase.contaDao().getContasFilteredSync(query)
                            restantes.forEach { it.qtRepete = novoQt }
                            appDatabase.contaDao().insertAll(restantes)
                        }
                    }
                    TipoExclusao.TODAS_AS_REPETICOES -> {
                        appDatabase.contaDao().deleteByCodigo(codigo)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao excluir contas recorrentes no Room", e)
                false
            }
        }
    }

    suspend fun excluirTudo() {
        withContext(Dispatchers.IO) {
            appDatabase.contaDao().deleteAll()
            appDatabase.notificationDao().deleteAll()
        }
    }

    fun verificarFimDeSeries(mes: Int, ano: Int) {
        repositoryScope.launch {
            val filter = ContaFilter().setMes(mes).setAno(ano)
            val contas = getContas(filter, null)

            contas.filter {
                it.nRepete == it.qtRepete &&
                        it.qtRepete > 1 &&
                        it.pagamento == ContasContract.STATUS_PAGO_RECEBIDO
            }.forEach { conta ->
                notificationRepository.addNotificacao(
                    appContext.getString(R.string.msg_fim_serie_titulo),
                    appContext.getString(R.string.msg_fim_serie_corpo, conta.nome),
                    "fim_serie|${conta.idConta}"
                )
            }
        }
    }

    fun renovarSerie(idConta: Long) {
        repositoryScope.launch {
            val contaBase = dbContas.getConta(idConta) ?: return@launch

            val cal = Calendar.getInstance()
            cal.set(contaBase.ano, contaBase.mes - 1, contaBase.dia)

            when (contaBase.intervalo) {
                300 -> cal.add(Calendar.MONTH, 1)
                3650 -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.DATE, if (contaBase.intervalo > 100) contaBase.intervalo - 100 else 1)
            }

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

            dbContas.geraContasRecorrentes(novaSerieBase, contaBase.qtRepete, contaBase.intervalo)
        }
    }

    fun closeLegacy() {
        dbContas.close()
    }

    // --- Operações de Metas Financeiras ---

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

    suspend fun calcularProgressoRealDaMeta(meta: MetaFinanceira): Double {
        return withContext(Dispatchers.IO) {
            var progressoReal = 0.0

            if (!meta.codigoVinculo.isNullOrEmpty()) {
                val cal = Calendar.getInstance()
                val anoAtual = cal.get(Calendar.YEAR)
                val mesAtual = cal.get(Calendar.MONTH) + 1

                val queryStr = """
                    SELECT SUM(${ContasContract.Colunas.COLUNA_VALOR_CONTA}) 
                    FROM ${ContasContract.Colunas.TABELA_NOME} 
                    WHERE ${ContasContract.Colunas.COLUNA_CODIGO_CONTA} = ? 
                    AND ${ContasContract.Colunas.COLUNA_PAGOU_CONTA} = ?
                    AND (${ContasContract.Colunas.COLUNA_ANO_DATA_CONTA} < ? OR (${ContasContract.Colunas.COLUNA_ANO_DATA_CONTA} = ? AND ${ContasContract.Colunas.COLUNA_MES_DATA_CONTA} <= ?))
                """.trimIndent()
                val query = SimpleSQLiteQuery(queryStr, arrayOf<Any>(
                    meta.codigoVinculo,
                    ContasContract.STATUS_PAGO_RECEBIDO,
                    anoAtual, anoAtual, mesAtual
                ))
                progressoReal = appDatabase.contaDao().getSumFilteredSync(query) ?: 0.0
            }

            if ((meta.tipoMeta == MetaFinanceira.TIPO_RESERVA || meta.tipoMeta == MetaFinanceira.TIPO_INVESTIMENTO || meta.tipoMeta == MetaFinanceira.TIPO_APOSENTADORIA)) {
                if (!meta.codigoVinculo.isNullOrEmpty()) {
                    val cal = Calendar.getInstance()
                    val anoAtual = cal.get(Calendar.YEAR)
                    val mesAtual = cal.get(Calendar.MONTH) + 1

                    val queryStr = """
                        SELECT SUM(${ContasContract.Colunas.COLUNA_VALOR_CONTA}) 
                        FROM ${ContasContract.Colunas.TABELA_NOME} 
                        WHERE ${ContasContract.Colunas.COLUNA_CODIGO_CONTA} = ? 
                        AND (${ContasContract.Colunas.COLUNA_ANO_DATA_CONTA} < ? OR (${ContasContract.Colunas.COLUNA_ANO_DATA_CONTA} = ? AND ${ContasContract.Colunas.COLUNA_MES_DATA_CONTA} <= ?))
                    """.trimIndent()
                    val query = SimpleSQLiteQuery(queryStr, arrayOf<Any>(
                        meta.codigoVinculo,
                        anoAtual, anoAtual, mesAtual
                    ))
                    progressoReal = appDatabase.contaDao().getSumFilteredSync(query) ?: 0.0
                }
            }

            maxOf(progressoReal, meta.valorAtual)
        }
    }

    suspend fun atualizarProgressoRealMeta(meta: MetaFinanceira) {
        val totalRealizado = calcularProgressoRealDaMeta(meta)
        metasRepository.saveGoal(meta.copy(valorAtual = totalRealizado))
    }
}