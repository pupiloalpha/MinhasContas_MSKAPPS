package com.msk.minhascontas.db

import androidx.room.*
import com.msk.minhascontas.db.ContasContract.Notificacoes
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM ${Notificacoes.TABELA_NOME} ORDER BY data_criacao DESC")
    fun getAll(): Flow<List<Notificacao>>

    @Query("SELECT * FROM ${Notificacoes.TABELA_NOME} WHERE lida = 0 ORDER BY data_criacao DESC")
    fun getUnread(): Flow<List<Notificacao>>

    @Query("SELECT * FROM ${Notificacoes.TABELA_NOME} WHERE lida = 0 ORDER BY data_criacao DESC")
    suspend fun getUnreadSync(): List<Notificacao>

    @Query("SELECT COUNT(*) FROM ${Notificacoes.TABELA_NOME} WHERE lida = 0")
    suspend fun countUnread(): Int

    @Query("SELECT * FROM ${Notificacoes.TABELA_NOME} WHERE titulo = :titulo AND mensagem = :mensagem AND date(data_criacao/1000, 'unixepoch', 'localtime') = date('now', 'localtime') LIMIT 1")
    suspend fun findDuplicateToday(titulo: String, mensagem: String): Notificacao?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notificacao: Notificacao): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notificacoes: List<Notificacao>)

    @Update
    suspend fun update(notificacao: Notificacao)

    @Query("UPDATE ${Notificacoes.TABELA_NOME} SET lida = 1 WHERE _id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE ${Notificacoes.TABELA_NOME} SET lida = 1 WHERE lida = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM ${Notificacoes.TABELA_NOME}")
    suspend fun deleteAll()

    @Query("DELETE FROM ${Notificacoes.TABELA_NOME} WHERE lida = 1 AND data_criacao < :timestamp")
    suspend fun deleteOldRead(timestamp: Long)

    @Query("DELETE FROM ${Notificacoes.TABELA_NOME} WHERE data_criacao < :timestamp")
    suspend fun deleteVeryOld(timestamp: Long)
}
