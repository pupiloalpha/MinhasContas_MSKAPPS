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

    @RawQuery
    suspend fun getContasFilteredSync(query: SupportSQLiteQuery): List<Conta>

    @RawQuery
    suspend fun getSumFilteredSync(query: SupportSQLiteQuery): Double?

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
    
    @Query("SELECT * FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano")
    fun getContasByMonth(mes: Int, ano: Int): Flow<List<Conta>>

    @Query("SELECT categoria_conta as categoria, SUM(valor_conta) as total FROM contasListadas WHERE mes_data = :mes AND ano_data = :ano AND tipo_conta = :tipo GROUP BY categoria_conta")
    suspend fun getSumByCategorySync(mes: Int, ano: Int, tipo: Int): List<CategorySum>

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

    @Query("SELECT tipo_conta as tipo, SUM(valor_conta) as total FROM contasListadas WHERE (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes)) AND tipo_conta IN (0, 1) GROUP BY tipo_conta")
    suspend fun sumPreviousMonthsGrouped(ano: Int, mes: Int): List<TypeSum>

    @Query("SELECT tipo_conta as tipo, SUM(valor_conta) as total FROM contasListadas WHERE (ano_data < :ano OR (ano_data = :ano AND mes_data < :mes) OR (ano_data = :ano AND mes_data = :mes AND dia_data < :dia)) AND tipo_conta IN (0, 1) GROUP BY tipo_conta")
    suspend fun sumPreviousDaysGrouped(ano: Int, mes: Int, dia: Int): List<TypeSum>

    data class CategorySum(val categoria: Int, val total: Double)
    data class MonthSum(val yearMonth: Int, val total: Double)
    data class TypeSum(val tipo: Int, val total: Double)
}
