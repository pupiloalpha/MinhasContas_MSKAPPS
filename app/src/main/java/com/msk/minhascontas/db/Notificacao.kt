package com.msk.minhascontas.db

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.msk.minhascontas.db.ContasContract.Notificacoes

/**
 * Entidade Room que representa uma notificação no sistema.
 */
@Entity(tableName = Notificacoes.TABELA_NOME)
data class Notificacao(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID)
    var id: Long = 0,

    @ColumnInfo(name = Notificacoes.COLUNA_TITULO)
    var titulo: String = "",

    @ColumnInfo(name = Notificacoes.COLUNA_MENSAGEM)
    var mensagem: String = "",

    @ColumnInfo(name = Notificacoes.COLUNA_DATA)
    var dataCriacao: Long = System.currentTimeMillis(),

    @ColumnInfo(name = Notificacoes.COLUNA_LIDA)
    var lida: Boolean = false,

    @ColumnInfo(name = Notificacoes.COLUNA_TIPO)
    var tipo: String? = null
)
