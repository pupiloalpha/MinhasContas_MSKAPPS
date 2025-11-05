package com.msk.minhascontas.db

import android.content.Context

/**
 * Repositório que expõe operações similares às oferecidas por DBContas,
 * mas implementadas com Room. Vá completando métodos conforme for migrando o app.
 */
class RoomContasRepository private constructor(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.contaDao()

    companion object {
        @Volatile
        private var INSTANCE: RoomContasRepository? = null

        fun getInstance(context: Context): RoomContasRepository {
            return INSTANCE ?: synchronized(this) {
                val inst = RoomContasRepository(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }

    fun geraConta(
        nome: String,
        tipo: Int?,
        classeConta: Int?,
        categoria: String?,
        dia: Int,
        mes: Int,
        ano: Int,
        valor: Double,
        pagamento: String?,
        qtRepete: Int,
        nRepete: Int,
        intervalo: Int,
        codigo: String?
    ): Long {
        val conta = Conta(
            nome = nome,
            tipoConta = tipo,
            classeConta = classeConta,
            categoriaConta = categoria,
            dia = dia,
            mes = mes,
            ano = ano,
            valor = valor,
            pagou = pagamento,
            qtRepeticoes = qtRepete,
            nrRepeticao = nRepete,
            intervalo = intervalo,
            codigo = codigo
        )
        return dao.insert(conta)
    }

    fun alteraDadosConta(conta: Conta): Int {
        return dao.update(conta)
    }

    fun alteraNomeContas(nomeNovo: String, nomeAntigo: String, codigo: String, nrRepete: Int): Int {
        // Delegar para query: atualizar todas as contas com mesmo nome/codigo e nr_repeticao > nrRepete-1
        return dao.updateNomeForSeries(nomeNovo, nomeAntigo, codigo, nrRepete - 1)
    }

    fun alteraTipoContas(tipo: Int, classeConta: Int, categoria: String, nomeAntigo: String, codigo: String, nrRepete: Int): Int {
        return dao.updateTipoForSeries(tipo, classeConta, categoria, nomeAntigo, codigo, nrRepete - 1)
    }

    fun alteraValorContas(valor: Double, pagamento: String, nomeAntigo: String, codigo: String, nrRepete: Int): Int {
        return dao.updateValorForSeries(valor, pagamento, nomeAntigo, codigo, nrRepete - 1)
    }

    fun alteraPagamentoConta(idConta: Long, pagamento: String): Int {
        return dao.updatePagamentoById(idConta, pagamento)
    }

    fun atualizaDataContas(nome: String, codigo: String, nr: Int): Int {
        return dao.atualizaDataContas(nome, codigo, nr)
    }

    fun atualizaPagamentoContas(dia: Int, mes: Int, ano: Int): Int {
        // As alterações do Java marcavam várias linhas com lógica OR; aqui fazemos a versão simples:
        val updated1 = dao.atualizaPagamentoContasDiaMesAno(dia, mes, ano)
        // also mark earlier years as 'paguei' if ano < given
        val updated2 = dao.atualizaPagamentoContasEarlierYears(ano)
        return updated1 + updated2
    }

    fun confirmaPagamentos(): Boolean {
        val countNotPaguei = dao.countNotPaguei()
        if (countNotPaguei > 0) {
            dao.confirmaPagamentosSetFalta()
            return true
        }
        return true
    }

    fun ajustaRepeticoesContas(): Boolean {
        val count = dao.countIntervaloLessThan(32)
        if (count > 0) {
            dao.ajustaRepeticoesContasSet(300, 31)
        }
        return true
    }

    fun excluiConta(idConta: Long): Int = dao.deleteById(idConta)

    fun excluiContaPorNome(nome: String, codigo: String): Int = dao.deleteByNomeAndCodigo(nome, codigo)

    fun excluiSerieContaPorNome(nome: String, codigo: String, nrRepete: Int): Int = dao.deleteSeriesByNomeAndCodigoAfter(nome, codigo, nrRepete - 1)

    fun excluiTodasAsContas(): Int = dao.deleteAll()

    fun buscaUmaConta(idConta: Long): Conta? = dao.getById(idConta)

    fun buscaContas(dia: Int, mes: Int, ano: Int): List<Conta> {
        return if (dia != 0) dao.getByDayMonthYear(dia, mes, ano) else dao.getByMonthYear(mes, ano)
    }

    fun buscaContasTipo(dia: Int, mes: Int, ano: Int, tipo: Int): List<Conta> {
        return if (dia != 0) dao.getByDayMonthYearTipo(dia, mes, ano, tipo) else dao.getByMonthYearTipo(mes, ano, tipo)
    }

    fun buscaContasTipoPagamento(dia: Int, mes: Int, ano: Int, tipo: Int, pagamento: String): List<Conta> {
        return if (dia != 0) dao.getByDayMonthYearTipoPagamento(dia, mes, ano, tipo, pagamento) else dao.getByMonthYearTipoPagamento(mes, ano, tipo, pagamento)
    }

    fun buscaContasClasse(dia: Int, mes: Int, ano: Int, tipo: Int, classe: Int): List<Conta> {
        return if (dia != 0) dao.getByDayMonthYearClasse(dia, mes, ano, tipo, classe) else dao.getByMonthYearClasse(mes, ano, tipo, classe)
    }

    fun buscaContasCategoria(dia: Int, mes: Int, ano: Int, categoria: String): List<Conta> {
        return if (dia != 0) dao.getByDayMonthYearCategoria(dia, mes, ano, categoria) else dao.getByMonthYearCategoria(mes, ano, categoria)
    }

    fun buscaContasPorNome(nome: String): List<Conta> = dao.getByNome(nome)

    fun mostraContasPorTipo(nome: String, tipo: Int, mes: Int, ano: Int): String {
        val contas = dao.getByMonthYearTipo(mes, ano, tipo)
        val sb = StringBuilder()
        sb.append(nome).append(" do mês:\n")
        for (c in contas) {
            sb.append(String.format("%s %s;\n", java.text.NumberFormat.getCurrencyInstance().format(c.valor), c.nome))
        }
        return sb.toString()
    }

    fun mostraNomeConta(idConta: Long): String? = dao.getById(idConta)?.nome

    fun mostraDMAConta(idConta: Long): IntArray? {
        val list = dao.getDMAById(idConta)
        return if (list.isNotEmpty()) intArrayOf(list[0], list[1], list[2]) else null
    }

    fun mostraValorConta(idConta: Long): Double = dao.getValorById(idConta) ?: 0.0

    fun mostraPagamentoConta(idConta: Long): String? = dao.getPagouById(idConta)

    fun mostraRepeticaoConta(idConta: Long): IntArray? {
        val c = dao.getById(idConta) ?: return null
        return intArrayOf(c.qtRepeticoes, c.nrRepeticao, c.intervalo)
    }

    fun mostraPrimeiraRepeticaoConta(nome: String, qtRepete: Int, codigo: String): Long? = dao.primeiraRepeticaoId(nome, qtRepete, codigo)

    fun mostraCodigoConta(idConta: Long): String? = dao.getCodigoById(idConta)

    fun mostraNomeContas(): List<String> = dao.getDistinctNomes()

    fun quantasContas(): Int = dao.countAll()

    fun quantasContasPagasPorTipo(tipo: Int, pagamento: String, dia: Int, mes: Int, ano: Int): Int {
        return if (dia == 0) dao.countPagasByTipo(tipo, pagamento, mes, ano)
        else dao.countPagasByTipoBeforeDay(tipo, pagamento, dia - 1, mes, ano)
    }

    fun quantasContasPorClasse(classe: Int, dia: Int, mes: Int, ano: Int): Int {
        return if (dia == 0) dao.countByClasseMesAno(classe, mes, ano) else dao.countByClaseBeforeDay(classe, dia + 1, mes, ano)
    }

    fun quantasContasPorMes(mes: Int, ano: Int): Int = dao.countByMesAno(mes, ano)

    fun quantasContasPorTipo(tipo: Int, dia: Int, mes: Int, ano: Int): Int {
        return if (dia == 0) dao.countByTipoMesAno(tipo, mes, ano) else dao.countByTipoBeforeDay(tipo, dia + 1, mes, ano)
    }

    fun quantasContasPorNome(nome: String): Int = dao.countByNome(nome)

    fun quantasRepeticoesDaConta(nome: String, codigo: String): Int = dao.countRepeticoesDaConta(nome, codigo)

    fun quantasContasPorNomeNoDia(nome: String, dia: Int, mes: Int, ano: Int): Int = dao.countByNomeAndDia(nome, dia, mes, ano)

    fun somaContas(tipo: Int, dia: Int, mes: Int, ano: Int): Double {
        return if (dia == 0) dao.somaContasPorTipoMesAno(tipo, mes, ano) ?: 0.0
        else dao.somaContasPorTipoBeforeDay(tipo, dia + 1, mes, ano) ?: 0.0
    }

    fun somaContasPagas(tipo: Int, pagamento: String, dia: Int, mes: Int, ano: Int): Double {
        return if (dia == 0) dao.somaContasPagas(tipo, pagamento, mes, ano) ?: 0.0
        else dao.somaContasPagasBeforeDay(tipo, pagamento, dia + 1, mes, ano) ?: 0.0
    }

    fun somaContasPorClasse(classe: Int, dia: Int, mes: Int, ano: Int): Double {
        return if (dia == 0) dao.somaContasPorClasseMesAno(classe, mes, ano) ?: 0.0
        else dao.somaContasPorClasseBeforeDay(classe, dia + 1, mes, ano) ?: 0.0
    }

    fun somaContasPorCategoria(categoria: String, dia: Int, mes: Int, ano: Int): Double {
        return if (dia == 0) dao.somaContasPorCategoriaMesAno(categoria, mes, ano) ?: 0.0
        else dao.somaContasPorCategoriaBeforeDay(categoria, dia + 1, mes, ano) ?: 0.0
    }
}
