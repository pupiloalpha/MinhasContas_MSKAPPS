package com.msk.minhascontas.features.listas

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

/**
 * Classe Abstrata que estende RecyclerView.Adapter para fornecer
 * funcionalidades de manipulação de Cursor.
 * Essa classe será a base para Adapters que carregam dados do SQLite.
 * 
 * @param <VH> O ViewHolder específico para esta classe.
</VH> */
abstract class CursorRecyclerViewAdapter<VH : RecyclerView.ViewHolder>
    (
    /**
     * Retorna o Cursor atual.
     */
    var cursor: Cursor?
) : RecyclerView.Adapter<VH>() {
    private var mRowIdColumn = 0

    init {
        // Otimização: Permite que o RecyclerView saiba que o ID de cada item
        // é estável (baseado na coluna _id do banco de dados).
        setHasStableIds(true)
        swapCursor(cursor)
    }

    /**
     * Move o Cursor para a posição correta antes de chamar o onBind abstrato.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        check(isDataValid(this.cursor)) { "O Cursor não é válido/existe." }

        val currentCursor = this.cursor!!
        // Move o Cursor para a posição solicitada
        check(currentCursor.moveToPosition(position)) { "Não foi possível mover o Cursor para a posição: $position" }

        // Chama o método abstrato que será implementado pelas classes concretas
        onBindViewHolder(holder, currentCursor)
    }

    /**
     * Método abstrato a ser implementado por subclasses, onde a lógica de
     * preenchimento do ViewHolder com os dados do Cursor deve ocorrer.
     */
    abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    /**
     * Retorna o número de itens, baseado no Cursor.
     */
    override fun getItemCount(): Int {
        return if (isDataValid(this.cursor)) {
            cursor!!.count
        } else 0
    }

    /**
     * Retorna a ID do item (a ID da linha do banco de dados).
     */
    override fun getItemId(position: Int): Long {
        if (isDataValid(this.cursor) && cursor!!.moveToPosition(position)) {
            // Retorna o valor da coluna _ID (row ID)
            return cursor!!.getLong(mRowIdColumn)
        }
        return RecyclerView.NO_ID
    }

    /**
     * Troca o Cursor atual por um novo. Fecha o antigo se for diferente e válido.
     */
    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === this.cursor) {
            return null // Nenhuma mudança
        }

        val oldCursor = this.cursor
        oldCursor?.close() // Fecha o Cursor antigo para liberar o recurso

        this.cursor = newCursor
        if (this.cursor != null) {
            // Tenta obter o índice da coluna _ID (necessário para getItemId)
            mRowIdColumn = cursor!!.getColumnIndexOrThrow("_id")
            // Notifica o Adapter para desenhar a nova lista
            notifyDataSetChanged()
        } else {
            mRowIdColumn = -1
            // Notifica o Adapter que a lista está vazia
            notifyDataSetChanged()
        }
        return oldCursor
    }

    /**
     * Verifica se o Cursor é válido (não nulo e não fechado).
     */
    fun isDataValid(cursor: Cursor?): Boolean {
        return (cursor != null) && !cursor.isClosed
    }

    /**
     * Retorna o item na posição específica.
     */
    fun getItem(position: Int): Cursor? {
        if (isDataValid(this.cursor) && cursor!!.moveToPosition(position)) {
            return this.cursor
        }
        return null
    }
}