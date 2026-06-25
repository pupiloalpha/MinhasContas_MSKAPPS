package com.msk.minhascontas.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MetaFinanceiraDao {
    @Query("SELECT * FROM metas_financeiras ORDER BY prioridade ASC")
    fun getAllMetas(): LiveData<List<MetaFinanceira>>

    @Query("SELECT * FROM metas_financeiras WHERE ativa = 1 ORDER BY prioridade ASC")
    fun getMetasAtivas(): LiveData<List<MetaFinanceira>>

    @Query("SELECT * FROM metas_financeiras WHERE id = :id")
    suspend fun getMetaById(id: Long): MetaFinanceira?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaFinanceira): Long

    @Update
    suspend fun updateMeta(meta: MetaFinanceira)

    @Delete
    suspend fun deleteMeta(meta: MetaFinanceira)

    @Query("UPDATE metas_financeiras SET valorAtual = :novoValor WHERE id = :id")
    suspend fun atualizarProgresso(id: Long, novoValor: Double)

    @Query("SELECT * FROM metas_financeiras WHERE codigoVinculo = :codigo LIMIT 1")
    suspend fun getMetaByCodigo(codigo: String): MetaFinanceira?
}
