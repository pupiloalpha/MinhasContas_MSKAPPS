package com.msk.minhascontas.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {
    @Query("SELECT * FROM contasListadas ORDER BY ano_data DESC, mes_data DESC, dia_data DESC")
    fun getAllContas(): Flow<List<Conta>>

    @RawQuery(observedEntities = [Conta::class])
    fun getContasFiltered(query: SupportSQLiteQuery): Flow<List<Conta>>

    @RawQuery(observedEntities = [Conta::class])
    fun getSumFiltered(query: SupportSQLiteQuery): Flow<Double?>

    @RawQuery(observedEntities = [Conta::class])
    suspend fun getContasFilteredSync(query: SupportSQLiteQuery): List<Conta>

    @RawQuery(observedEntities = [Conta::class])
    suspend fun getSumFilteredSync(query: SupportSQLiteQuery): Double?

    @Query("SELECT * FROM contasListadas GROUP BY nome_conta ORDER BY _id DESC")
    suspend fun getUniqueContasByName(): List<Conta>

    @Query("SELECT * FROM contasListadas WHERE _id = :id")
    suspend fun getContaById(id: Long): Conta?

    @Query("SELECT COUNT(*) FROM contasListadas")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conta: Conta): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contas: List<Conta>)

    @Update
    suspend fun update(conta: Conta)

    @Delete
    suspend fun delete(conta: Conta)

    @Query("DELETE FROM contasListadas")
    suspend fun deleteAll()

    @Query("DELETE FROM contasListadas WHERE codigo = :codigo")
    suspend fun deleteByCodigo(codigo: String)

    @Query("DELETE FROM contasListadas WHERE codigo = :codigo AND nr_repeticao >= :nr")
    suspend fun deleteByCodigoFrom(codigo: String, nr: Int)

    @Query("UPDATE contasListadas SET pagou_conta = :pago WHERE (ano_data < :ano) OR (ano_data = :ano AND mes_data < :mes) OR (ano_data = :ano AND mes_data = :mes AND dia_data < :dia) AND pagou_conta = :falta")
    suspend fun updatePastDueToPaid(ano: Int, mes: Int, dia: Int, pago: String, falta: String): Int

    @Query("UPDATE contasListadas SET pagou_conta = :falta WHERE pagou_conta != :pago")
    suspend fun resetNonPaidToPending(pago: String, falta: String): Int

    @Query("UPDATE contasListadas SET intervalo_conta = 300 WHERE intervalo_conta <= 31")
    suspend fun adjustIntervals(): Int

    @Query("SELECT * FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano ORDER BY dia_data ASC, nome_conta ASC")
    suspend fun getContasByMonthSync(mes: Int, ano: Int): List<Conta>

    /**
     * Soma de valores por categoria para um tipo específico (0=Despesa, 1=Receita, 2=Aplicação)
     */
    @Query("SELECT categoria_conta as categoria, SUM(valor_conta) as total FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo GROUP BY categoria_conta")
    suspend fun getSumByCategorySync(mes: Int, ano: Int, tipo: Int): List<CategorySum>

    /**
     * Soma por categoria filtrando por tipo de conta e status de pagamento opcional.
     */
    @Query("SELECT categoria_conta as categoria, SUM(valor_conta) as total FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo AND (:status IS NULL OR pagou_conta = :status) GROUP BY categoria_conta")
    suspend fun getSumByCategoryAndStatusSync(mes: Int, ano: Int, tipo: Int, status: String?): List<CategorySum>

    /**
     * Agrupa por categoria e status de pagamento para suporte visual nos gráficos.
     */
    @Query("SELECT categoria_conta as categoria, pagou_conta as status, SUM(valor_conta) as total FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo GROUP BY categoria_conta, pagou_conta")
    suspend fun getSumByCategoryGroupedByStatusSync(mes: Int, ano: Int, tipo: Int): List<CategoryStatusSum>

    @Query("SELECT SUM(valor_conta) FROM contasListadas WHERE tipo_conta = :tipo AND (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes))")
    suspend fun sumPreviousMonths(ano: Int, mes: Int, tipo: Int): Double?

    @Query("SELECT SUM(valor_conta) FROM contasListadas WHERE tipo_conta = :tipo AND (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes) OR (ano_data = :ano AND mes_data = :mes AND dia_data < :dia))")
    suspend fun sumPreviousDays(ano: Int, mes: Int, dia: Int, tipo: Int): Double?

    @Query("SELECT SUM(valor_conta) FROM contasListadas WHERE tipo_conta = :tipo AND (classe_conta = :classe OR :classe = -1) AND (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes))")
    suspend fun sumPreviousMonthsByClass(ano: Int, mes: Int, tipo: Int, classe: Int): Double?

    @Query("SELECT SUM(valor_conta) FROM contasListadas WHERE tipo_conta = :tipo AND (classe_conta = :classe OR :classe = -1) AND (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes) OR (ano_data = :ano AND mes_data = :mes AND dia_data < :dia))")
    suspend fun sumPreviousDaysByClass(ano: Int, mes: Int, dia: Int, tipo: Int, classe: Int): Double?

    @Query("SELECT SUM(valor_conta) FROM contasListadas WHERE ano_data = :ano AND mes_data = :mes AND dia_data BETWEEN :diaInicio AND :diaFim AND (tipo_conta = :tipo OR :tipo = -1) AND (classe_conta = :classe OR :classe = -1) AND (categoria_conta = :categoria OR :categoria = -1) AND (pagou_conta = :status OR :status IS NULL)")
    suspend fun sumInPeriod(diaInicio: Int, diaFim: Int, mes: Int, ano: Int, tipo: Int, classe: Int, categoria: Int, status: String?): Double?

    @Query("SELECT (ano_data * 12 + mes_data) as yearMonth, SUM(valor_conta) as total FROM contasListadas WHERE categoria_conta = :categoria AND tipo_conta = 0 AND (ano_data * 12 + mes_data) BETWEEN :startYearMonth AND :endYearMonth GROUP BY yearMonth")
    suspend fun getMonthlySums(categoria: Int, startYearMonth: Int, endYearMonth: Int): List<MonthSum>

    @Query("SELECT (ano_data * 12 + mes_data) as yearMonth, SUM(valor_conta) as total FROM contasListadas WHERE tipo_conta = :tipo AND (ano_data * 12 + mes_data) BETWEEN :startYearMonth AND :endYearMonth GROUP BY yearMonth")
    suspend fun getMonthlySumsByType(tipo: Int, startYearMonth: Int, endYearMonth: Int): List<MonthSum>

    @Query("SELECT tipo_conta as tipo, SUM(valor_conta) as total FROM contasListadas WHERE (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes)) AND tipo_conta IN (0, 1) GROUP BY tipo_conta")
    suspend fun sumPreviousMonthsGrouped(ano: Int, mes: Int): List<TypeSum>

    @Query("SELECT tipo_conta as tipo, SUM(valor_conta) as total FROM contasListadas WHERE (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes) OR (ano_data = :ano AND mes_data = :mes AND dia_data < :dia)) AND tipo_conta IN (0, 1) GROUP BY tipo_conta")
    suspend fun sumPreviousDaysGrouped(ano: Int, mes: Int, dia: Int): List<TypeSum>

    data class CategorySum(val categoria: Int, val total: Double)
    data class CategoryStatusSum(val categoria: Int, val status: String, val total: Double)
    data class MonthSum(val yearMonth: Int, val total: Double)
    data class TypeSum(val tipo: Int, val total: Double)
}