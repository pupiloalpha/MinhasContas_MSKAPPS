package com.msk.minhascontas.db

import android.database.Cursor
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.msk.minhascontas.db.ContasContract.Colunas

/**
 * POJO (Plain Old Java Object) que representa a entidade Conta, convertido para Kotlin.
 * Esta classe encapsula os dados de uma conta, melhorando a coesão
 * e a manutenibilidade do código.
 */
@Entity(
    tableName = Colunas.TABELA_NOME,
    indices = [
        Index(
            value = [
                Colunas.COLUNA_NOME_CONTA,
                Colunas.COLUNA_DIA_DATA_CONTA,
                Colunas.COLUNA_MES_DATA_CONTA,
                Colunas.COLUNA_ANO_DATA_CONTA,
                Colunas.COLUNA_VALOR_CONTA,
                Colunas.COLUNA_CODIGO_CONTA
            ],
            unique = true
        )
    ]
)
data class Conta(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID)
    var idConta: Long = 0,

    @ColumnInfo(name = Colunas.COLUNA_NOME_CONTA)
    var nome: String = "",

    @ColumnInfo(name = Colunas.COLUNA_TIPO_CONTA)
    var tipo: Int = 0,

    @ColumnInfo(name = Colunas.COLUNA_CLASSE_CONTA)
    var classeConta: Int = 0,

    @ColumnInfo(name = Colunas.COLUNA_CATEGORIA_CONTA)
    var categoria: Int = 0,

    @ColumnInfo(name = Colunas.COLUNA_DIA_DATA_CONTA)
    var dia: Int = 1,

    @ColumnInfo(name = Colunas.COLUNA_MES_DATA_CONTA)
    var mes: Int = 1,

    @ColumnInfo(name = Colunas.COLUNA_ANO_DATA_CONTA)
    var ano: Int = 2000,

    @ColumnInfo(name = Colunas.COLUNA_VALOR_CONTA)
    var valor: Double = 0.0,

    @ColumnInfo(name = Colunas.COLUNA_PAGOU_CONTA)
    var pagamento: String = "",

    @ColumnInfo(name = Colunas.COLUNA_QT_REPETICOES_CONTA)
    var qtRepete: Int = 1,

    @ColumnInfo(name = Colunas.COLUNA_NR_REPETICAO_CONTA)
    var nRepete: Int = 1,

    @ColumnInfo(name = Colunas.COLUNA_INTERVALO_CONTA)
    var intervalo: Int = 0,

    @ColumnInfo(name = Colunas.COLUNA_CODIGO_CONTA)
    var codigo: String = "",

    @ColumnInfo(name = Colunas.COLUNA_VALOR_JUROS)
    var valorJuros: Double = 0.0
) {

    /**
     * Construtor secundário para compatibilidade com o código Java que não utiliza valorJuros.
     */
    @Ignore
    constructor(
        idConta: Long, nome: String, tipo: Int, classeConta: Int, categoria: Int,
        dia: Int, mes: Int, ano: Int, valor: Double, pagamento: String,
        qtRepete: Int, nRepete: Int, intervalo: Int, codigo: String
    ) : this(
        idConta, nome, tipo, classeConta, categoria, dia, mes, ano, valor,
        pagamento, qtRepete, nRepete, intervalo, codigo, 0.0
    )

    /**
     * Construtor sem argumentos para compatibilidade (ex: ImportarExcel.java).
     */
    @Ignore
    constructor() : this(0)

    companion object {
        /**
         * Cria um objeto Conta a partir de um Cursor.
         * O uso de @JvmStatic mantém a compatibilidade com chamadas estáticas em Java.
         */
        @JvmStatic
        fun fromCursor(cursor: Cursor?): Conta? {
            if (cursor == null || cursor.isClosed) {
                return null
            }

            return try {
                Conta(
                    idConta = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
                    nome = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA)),
                    tipo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA)),
                    classeConta = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA)),
                    categoria = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA)),
                    dia = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA)),
                    mes = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA)),
                    ano = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA)),
                    valor = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA)),
                    pagamento = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA)),
                    qtRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA)),
                    nRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA)),
                    intervalo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA)),
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA)),
                    valorJuros = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS))
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    /**
     * Classe Builder para construção flexível de objetos Conta, mantendo compatibilidade com Java.
     */
    class Builder(
        private val nome: String,
        private val valor: Double,
        private val dia: Int,
        private val mes: Int,
        private val ano: Int,
        private val codigo: String
    ) {
        private var idConta: Long = 0
        private var tipo: Int = 0
        private var classeConta: Int = 0
        private var categoria: Int = 0
        private var pagamento: String = "falta"
        private var qtRepete: Int = 1
        private var nRepete: Int = 1
        private var intervalo: Int = 300
        private var valorJuros: Double = 0.0

        fun setIdConta(idConta: Long) = apply { this.idConta = idConta }
        fun setTipo(tipo: Int) = apply { this.tipo = tipo }
        fun setClasseConta(classeConta: Int) = apply { this.classeConta = classeConta }
        fun setCategoria(categoria: Int) = apply { this.categoria = categoria }
        fun setPagamento(pagamento: String) = apply { this.pagamento = pagamento }
        fun setQtRepete(qtRepete: Int) = apply { this.qtRepete = qtRepete }
        fun setNRepete(nRepete: Int) = apply { this.nRepete = nRepete }
        fun setIntervalo(intervalo: Int) = apply { this.intervalo = intervalo }
        fun setValorJuros(valorJuros: Double) = apply { this.valorJuros = valorJuros }

        fun build(): Conta = Conta(
            idConta = idConta,
            nome = nome,
            tipo = tipo,
            classeConta = classeConta,
            categoria = categoria,
            dia = dia,
            mes = mes,
            ano = ano,
            valor = valor,
            pagamento = pagamento,
            qtRepete = qtRepete,
            nRepete = nRepete,
            intervalo = intervalo,
            codigo = codigo,
            valorJuros = valorJuros
        )
    }
}
