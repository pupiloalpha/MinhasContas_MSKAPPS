package com.msk.minhascontas.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContaDao {
    @Query("SELECT * FROM contas ORDER BY conta ASC")
    fun getAll(): List<Conta>

    @Query("SELECT * FROM contas WHERE _id = :id LIMIT 1")
    fun getById(id: Long): Conta?

    @Insert
    fun insert(conta: Conta): Long

    @Update
    fun update(conta: Conta): Int

    @Delete
    fun delete(conta: Conta): Int

    // Basic selects used by repository
    @Query("SELECT * FROM contas WHERE dia_data = :dia AND mes_data = :mes AND ano_data = :ano")
    fun getByDayMonthYear(dia: Int, mes: Int, ano: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE mes_data = :mes AND ano_data = :ano")
    fun getByMonthYear(mes: Int, ano: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE dia_data = :dia AND mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo")
    fun getByDayMonthYearTipo(dia: Int, mes: Int, ano: Int, tipo: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo")
    fun getByMonthYearTipo(mes: Int, ano: Int, tipo: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE dia_data = :dia AND mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo AND pagou = :pagamento")
    fun getByDayMonthYearTipoPagamento(dia: Int, mes: Int, ano: Int, tipo: Int, pagamento: String): List<Conta>

    @Query("SELECT * FROM contas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo AND pagou = :pagamento")
    fun getByMonthYearTipoPagamento(mes: Int, ano: Int, tipo: Int, pagamento: String): List<Conta>

    @Query("SELECT * FROM contas WHERE dia_data = :dia AND mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo AND classe_conta = :classe")
    fun getByDayMonthYearClasse(dia: Int, mes: Int, ano: Int, tipo: Int, classe: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo AND classe_conta = :classe")
    fun getByMonthYearClasse(mes: Int, ano: Int, tipo: Int, classe: Int): List<Conta>

    @Query("SELECT * FROM contas WHERE dia_data = :dia AND mes_data = :mes AND ano_data = :ano AND tipo_conta = 0 AND categoria_conta = :categoria")
    fun getByDayMonthYearCategoria(dia: Int, mes: Int, ano: Int, categoria: String): List<Conta>

    @Query("SELECT * FROM contas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = 0 AND categoria_conta = :categoria")
    fun getByMonthYearCategoria(mes: Int, ano: Int, categoria: String): List<Conta>

    @Query("SELECT * FROM contas WHERE conta = :nome ORDER BY ano_data ASC, mes_data ASC, dia_data ASC")
    fun getByNome(nome: String): List<Conta>

    @Query("SELECT SUM(valor) FROM contas WHERE tipo_conta = :tipo AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorTipoMesAno(tipo: Int, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE tipo_conta = :tipo AND pagou = :pagamento AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPagas(tipo: Int, pagamento: String, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE classe_conta = :classe AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorClasseMesAno(classe: Int, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE categoria_conta = :categoria AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorCategoriaMesAno(categoria: String, mes: Int, ano: Int): Double?

    @Query("UPDATE contas SET pagou = :pagamento WHERE _id = :id")
    fun updatePagamentoById(id: Long, pagamento: String): Int

    @Query("UPDATE contas SET codigo = :codigo WHERE conta = :nome AND qt_repeticoes = :qt")
    fun atualizaDataContas(nome: String, codigo: String, qt: Int): Int

    @Query("UPDATE contas SET pagou = 'paguei' WHERE dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun atualizaPagamentoContasDiaMesAno(dia: Int, mes: Int, ano: Int): Int

    // mark earlier years as paguei (mimic original: 'OR ano_data < ano')
    @Query("UPDATE contas SET pagou = 'paguei' WHERE ano_data < :ano")
    fun atualizaPagamentoContasEarlierYears(ano: Int): Int

    @Query("UPDATE contas SET pagou = 'falta' WHERE pagou != 'paguei'")
    fun confirmaPagamentosSetFalta(): Int

    @Query("SELECT COUNT(*) FROM contas WHERE pagou != 'paguei'")
    fun countNotPaguei(): Int

    @Query("SELECT COUNT(*) FROM contas WHERE dia_repeticao < :limite")
    fun countIntervaloLessThan(limite: Int): Int

    @Query("UPDATE contas SET dia_repeticao = :novoIntervalo WHERE dia_repeticao <= :limite")
    fun ajustaRepeticoesContasSet(novoIntervalo: Int, limite: Int): Int

    @Query("DELETE FROM contas WHERE _id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM contas WHERE conta = :nome AND codigo = :codigo")
    fun deleteByNomeAndCodigo(nome: String, codigo: String): Int

    @Query("DELETE FROM contas WHERE conta = :nome AND codigo = :codigo AND nr_repeticao > :nr")
    fun deleteSeriesByNomeAndCodigoAfter(nome: String, codigo: String, nr: Int): Int

    @Query("DELETE FROM contas")
    fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM contas")
    fun countAll(): Int

    @Query("SELECT COUNT(*) FROM contas WHERE tipo_conta = :tipo AND pagou = :pagamento AND mes_data = :mes AND ano_data = :ano")
    fun countPagasByTipo(tipo: Int, pagamento: String, mes: Int, ano: Int): Int

    // helper to count 'before day' where original code used dia-1
    @Query("SELECT COUNT(*) FROM contas WHERE tipo_conta = :tipo AND pagou = :pagamento AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun countPagasByTipoBeforeDay(tipo: Int, pagamento: String, dia: Int, mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE classe_conta = :classe AND mes_data = :mes AND ano_data = :ano")
    fun countByClasseMesAno(classe: Int, mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE classe_conta = :classe AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun countByClaseBeforeDay(classe: Int, dia: Int, mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE mes_data = :mes AND ano_data = :ano")
    fun countByMesAno(mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE tipo_conta = :tipo AND mes_data = :mes AND ano_data = :ano")
    fun countByTipoMesAno(tipo: Int, mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE tipo_conta = :tipo AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun countByTipoBeforeDay(tipo: Int, dia: Int, mes: Int, ano: Int): Int

    @Query("SELECT COUNT(*) FROM contas WHERE conta = :nome")
    fun countByNome(nome: String): Int

    @Query("SELECT COUNT(*) FROM contas WHERE conta = :nome AND codigo = :codigo")
    fun countRepeticoesDaConta(nome: String, codigo: String): Int

    @Query("SELECT COUNT(*) FROM contas WHERE conta = :nome AND dia_data = :dia AND mes_data = :mes AND ano_data = :ano")
    fun countByNomeAndDia(nome: String, dia: Int, mes: Int, ano: Int): Int

    @Query("SELECT _id FROM contas WHERE conta = :nome AND qt_repeticoes = :qt AND codigo = :codigo LIMIT 1")
    fun primeiraRepeticaoId(nome: String, qt: Int, codigo: String): Long?

    @Query("SELECT codigo FROM contas WHERE _id = :id LIMIT 1")
    fun getCodigoById(id: Long): String?

    @Query("SELECT pagou FROM contas WHERE _id = :id LIMIT 1")
    fun getPagouById(id: Long): String?

    @Query("SELECT valor FROM contas WHERE _id = :id LIMIT 1")
    fun getValorById(id: Long): Double?

    // returns three ints in result list [dia, mes, ano] (use with caution)
    @Query("SELECT dia_data, mes_data, ano_data FROM contas WHERE _id = :id LIMIT 1")
    fun getDMAById(id: Long): List<Int>

    @Query("SELECT DISTINCT conta FROM contas ORDER BY conta ASC")
    fun getDistinctNomes(): List<String>

    // helper update queries for series modifications (name/type/valor)
    @Query("UPDATE contas SET conta = :novoNome WHERE conta = :nomeAntigo AND codigo = :codigo AND nr_repeticao > :nr")
    fun updateNomeForSeries(novoNome: String, nomeAntigo: String, codigo: String, nr: Int): Int

    @Query("UPDATE contas SET tipo_conta = :tipo, classe_conta = :classe, categoria_conta = :categoria WHERE conta = :nomeAntigo AND codigo = :codigo AND nr_repeticao > :nr")
    fun updateTipoForSeries(tipo: Int, classe: Int, categoria: String, nomeAntigo: String, codigo: String, nr: Int): Int

    @Query("UPDATE contas SET valor = :valor, pagou = :pagamento WHERE conta = :nomeAntigo AND codigo = :codigo AND nr_repeticao > :nr")
    fun updateValorForSeries(valor: Double, pagamento: String, nomeAntigo: String, codigo: String, nr: Int): Int

    @Query("SELECT SUM(valor) FROM contas WHERE tipo_conta = :tipo AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorTipoBeforeDay(tipo: Int, dia: Int, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE tipo_conta = :tipo AND pagou = :pagamento AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPagasBeforeDay(tipo: Int, pagamento: String, dia: Int, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE classe_conta = :classe AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorClasseBeforeDay(classe: Int, dia: Int, mes: Int, ano: Int): Double?

    @Query("SELECT SUM(valor) FROM contas WHERE categoria_conta = :categoria AND dia_data < :dia AND mes_data = :mes AND ano_data = :ano")
    fun somaContasPorCategoriaBeforeDay(categoria: String, dia: Int, mes: Int, ano: Int): Double?
}
