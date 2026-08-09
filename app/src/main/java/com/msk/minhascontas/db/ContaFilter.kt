package com.msk.minhascontas.db

import com.msk.minhascontas.db.ContasContract.Colunas
import java.io.Serializable

/**
 * Builder class to create filters for 'contas' (accounts/bills).
 * Useful for searching recurring series or accounts with specific criteria.
 * This class is {@link Serializable} to allow passing via Bundle between Android components.
 */
class ContaFilter : Serializable {
    @JvmField var codigoConta: String? = null
    @JvmField var nrRepeticaoMin: Int = -1
    @JvmField var nrRepeticaoMax: Int = -1
    @JvmField var dia: Int = -1
    @JvmField var diaFim: Int = -1
    @JvmField var mes: Int = -1
    @JvmField var ano: Int = -1
    @JvmField var nome: String? = null
    @JvmField var tipo: Int = -1
    @JvmField var classe: Int = -1
    @JvmField var categoria: Int = -1
    @JvmField var pagamento: String? = null
    @JvmField var categoriasIn: List<Int>? = null
    @JvmField var classesIn: List<Int>? = null
    @JvmField var tiposIn: List<Int>? = null
    @JvmField var valorGlobal: Double? = null
    @JvmField var diaGlobal: Int? = null
    @JvmField var mesGlobal: Int? = null
    @JvmField var anoGlobal: Int? = null
    @JvmField var isPesquisaGlobal: Boolean = false

    // Manual setters for chaining (Fluent API)
    fun setCodigoConta(codigo: String?): ContaFilter { this.codigoConta = codigo; return this }
    fun setNome(nome: String?): ContaFilter { this.nome = nome; return this }
    fun setNrRepeticaoMin(nr: Int): ContaFilter { this.nrRepeticaoMin = nr; return this }
    fun setNrRepeticaoMax(nr: Int): ContaFilter { this.nrRepeticaoMax = nr; return this }
    fun setDia(dia: Int): ContaFilter { this.dia = dia; return this }
    fun setDiaFim(diaFim: Int): ContaFilter { this.diaFim = diaFim; return this }
    fun setMes(mes: Int): ContaFilter { this.mes = mes; return this }
    fun setAno(ano: Int): ContaFilter { this.ano = ano; return this }
    fun setTipo(tipo: Int): ContaFilter { this.tipo = tipo; return this }
    fun setClasse(classe: Int): ContaFilter { this.classe = classe; return this }
    fun setCategoria(categoria: Int): ContaFilter { this.categoria = categoria; return this }
    fun setCategoriasIn(categorias: List<Int>?): ContaFilter { this.categoriasIn = categorias; return this }
    fun setClassesIn(classes: List<Int>?): ContaFilter { this.classesIn = classes; return this }
    fun setTiposIn(tipos: List<Int>?): ContaFilter { this.tiposIn = tipos; return this }
    fun setValorGlobal(valor: Double?): ContaFilter { this.valorGlobal = valor; return this }
    fun setDiaGlobal(dia: Int?): ContaFilter { this.diaGlobal = dia; return this }
    fun setMesGlobal(mes: Int?): ContaFilter { this.mesGlobal = mes; return this }
    fun setAnoGlobal(ano: Int?): ContaFilter { this.anoGlobal = ano; return this }
    fun setPesquisaGlobal(isGlobal: Boolean): ContaFilter { this.isPesquisaGlobal = isGlobal; return this }
    fun setPagamento(pagamento: String?): ContaFilter { this.pagamento = pagamento; return this }

    fun setFiltroData(ano: Int, mes: Int, dia: Int): ContaFilter {
        this.ano = ano
        this.mes = mes
        this.dia = dia
        return this
    }

    fun buildWhereClause(): String {
        val clauses = mutableListOf<String>()
        val textFilters = mutableListOf<String>()

        if (isPesquisaGlobal) {
            if (!nome.isNullOrEmpty()) {
                textFilters.add("${Colunas.COLUNA_NOME_CONTA} LIKE ?")
            }
            categoriasIn?.let { if (it.isNotEmpty()) textFilters.add("${Colunas.COLUNA_CATEGORIA_CONTA} IN (${makePlaceholders(it.size)})") }
            classesIn?.let { if (it.isNotEmpty()) textFilters.add("${Colunas.COLUNA_CLASSE_CONTA} IN (${makePlaceholders(it.size)})") }
            tiposIn?.let { if (it.isNotEmpty()) textFilters.add("${Colunas.COLUNA_TIPO_CONTA} IN (${makePlaceholders(it.size)})") }
            valorGlobal?.let { textFilters.add("${Colunas.COLUNA_VALOR_CONTA} = ?") }
            diaGlobal?.let { textFilters.add("${Colunas.COLUNA_DIA_DATA_CONTA} = ?") }
            mesGlobal?.let { textFilters.add("${Colunas.COLUNA_MES_DATA_CONTA} = ?") }
            anoGlobal?.let { textFilters.add("${Colunas.COLUNA_ANO_DATA_CONTA} = ?") }

            if (textFilters.isNotEmpty()) {
                clauses.add("(" + textFilters.joinToString(" OR ") + ")")
            }
        } else {
            if (!nome.isNullOrEmpty()) {
                clauses.add("${Colunas.COLUNA_NOME_CONTA} LIKE ?")
            }
            if (tipo != -1) {
                clauses.add("${Colunas.COLUNA_TIPO_CONTA} = ?")
            }
            if (classe != -1) {
                clauses.add("${Colunas.COLUNA_CLASSE_CONTA} = ?")
            }
            if (categoria != -1) {
                clauses.add("${Colunas.COLUNA_CATEGORIA_CONTA} = ?")
            }
        }

        codigoConta?.let { clauses.add("${Colunas.COLUNA_CODIGO_CONTA} = ?") }
        if (nrRepeticaoMin > 0) clauses.add("${Colunas.COLUNA_NR_REPETICAO_CONTA} >= ?")
        if (nrRepeticaoMax > 0) clauses.add("${Colunas.COLUNA_NR_REPETICAO_CONTA} <= ?")

        if (dia > 0 && diaFim > 0) {
            clauses.add("${Colunas.COLUNA_DIA_DATA_CONTA} BETWEEN ? AND ?")
        } else if (dia > 0) {
            clauses.add("${Colunas.COLUNA_DIA_DATA_CONTA} = ?")
        } else if (diaFim > 0) {
            clauses.add("${Colunas.COLUNA_DIA_DATA_CONTA} <= ?")
        }

        if (mes in 1..12) clauses.add("${Colunas.COLUNA_MES_DATA_CONTA} = ?")
        if (ano > 0) clauses.add("${Colunas.COLUNA_ANO_DATA_CONTA} = ?")
        if (!pagamento.isNullOrEmpty()) clauses.add("${Colunas.COLUNA_PAGOU_CONTA} = ?")

        return clauses.joinToString(" AND ")
    }

    private fun makePlaceholders(count: Int): String {
        return if (count < 1) "" else Array(count) { "?" }.joinToString(",")
    }

    fun buildWhereArgs(): Array<String> {
        val args = mutableListOf<String>()

        if (isPesquisaGlobal) {
            if (!nome.isNullOrEmpty()) args.add("%$nome%")
            categoriasIn?.forEach { args.add(it.toString()) }
            classesIn?.forEach { args.add(it.toString()) }
            tiposIn?.forEach { args.add(it.toString()) }
            valorGlobal?.let { args.add(it.toString()) }
            diaGlobal?.let { args.add(it.toString()) }
            mesGlobal?.let { args.add(it.toString()) }
            anoGlobal?.let { args.add(it.toString()) }
        } else {
            if (!nome.isNullOrEmpty()) args.add("%$nome%")
            if (tipo != -1) args.add(tipo.toString())
            if (classe != -1) args.add(classe.toString())
            if (categoria != -1) args.add(categoria.toString())
        }

        codigoConta?.let { args.add(it) }
        if (nrRepeticaoMin > 0) args.add(nrRepeticaoMin.toString())
        if (nrRepeticaoMax > 0) args.add(nrRepeticaoMax.toString())

        if (dia > 0 && diaFim > 0) {
            args.add(dia.toString())
            args.add(diaFim.toString())
        } else if (dia > 0) {
            args.add(dia.toString())
        } else if (diaFim > 0) {
            args.add(diaFim.toString())
        }

        if (mes in 1..12) args.add(mes.toString())
        if (ano > 0) args.add(ano.toString())
        if (!pagamento.isNullOrEmpty()) args.add(pagamento!!)

        return args.toTypedArray()
    }

    fun getSelection(): String? {
        val selectionParts = mutableListOf<String>()
        if (mes != -1) selectionParts.add("${Colunas.COLUNA_MES_DATA_CONTA} = ?")
        if (ano != -1) selectionParts.add("${Colunas.COLUNA_ANO_DATA_CONTA} = ?")
        if (tipo != -1) selectionParts.add("${Colunas.COLUNA_TIPO_CONTA} = ?")
        if (classe != -1) selectionParts.add("${Colunas.COLUNA_CLASSE_CONTA} = ?")
        if (categoria != -1) selectionParts.add("${Colunas.COLUNA_CATEGORIA_CONTA} = ?")
        if (!pagamento.isNullOrEmpty()) selectionParts.add("${Colunas.COLUNA_PAGOU_CONTA} = ?")
        if (dia > 0 && diaFim > 0) {
            selectionParts.add("${Colunas.COLUNA_DIA_DATA_CONTA} BETWEEN ? AND ?")
        } else if (dia > 0) {
            selectionParts.add("${Colunas.COLUNA_DIA_DATA_CONTA} = ?")
        } else if (diaFim > 0) {
            selectionParts.add("${Colunas.COLUNA_DIA_DATA_CONTA} <= ?")
        }
        return if (selectionParts.isEmpty()) null else selectionParts.joinToString(" AND ")
    }

    fun getSelectionArgs(): Array<String> {
        val args = mutableListOf<String>()
        if (mes != -1) args.add(mes.toString())
        if (ano != -1) args.add(ano.toString())
        if (tipo != -1) args.add(tipo.toString())
        if (classe != -1) args.add(classe.toString())
        if (categoria != -1) args.add(categoria.toString())
        if (!pagamento.isNullOrEmpty()) args.add(pagamento!!)
        if (dia > 0 && diaFim > 0) {
            args.add(dia.toString())
            args.add(diaFim.toString())
        } else if (dia > 0) {
            args.add(dia.toString())
        } else if (diaFim > 0) {
            args.add(diaFim.toString())
        }
        return args.toTypedArray()
    }
}