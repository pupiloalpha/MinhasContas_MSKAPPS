package com.msk.minhascontas.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contas")
data class Conta(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0L,

    @ColumnInfo(name = "conta")
    val nome: String,

    @ColumnInfo(name = "tipo_conta")
    val tipoConta: Int?,

    @ColumnInfo(name = "classe_conta")
    val classeConta: Int?,

    // original schema declares categoria as TEXT; keep it as String to avoid read issues
    @ColumnInfo(name = "categoria_conta")
    val categoriaConta: String?,

    @ColumnInfo(name = "dia_data")
    val dia: Int,

    @ColumnInfo(name = "mes_data")
    val mes: Int,

    @ColumnInfo(name = "ano_data")
    val ano: Int,

    @ColumnInfo(name = "valor")
    val valor: Double,

    @ColumnInfo(name = "pagou")
    val pagou: String?,

    @ColumnInfo(name = "qt_repeticoes")
    val qtRepeticoes: Int,

    @ColumnInfo(name = "nr_repeticao")
    val nrRepeticao: Int,

    @ColumnInfo(name = "dia_repeticao")
    val intervalo: Int,

    @ColumnInfo(name = "codigo")
    val codigo: String?
)
