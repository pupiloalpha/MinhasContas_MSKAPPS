package com.msk.minhascontas.listas;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Classe Abstrata que estende RecyclerView.Adapter para fornecer
 * funcionalidades de manipulação de Cursor.
 * Essa classe será a base para Adapters que carregam dados do SQLite.
 *
 * @param <VH> O ViewHolder específico para esta classe.
 */
public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private Cursor mCursor;
    private int mRowIdColumn;

    public CursorRecyclerViewAdapter(Cursor cursor) {
        mCursor = cursor;
        // Otimização: Permite que o RecyclerView saiba que o ID de cada item
        // é estável (baseado na coluna _id do banco de dados).
        setHasStableIds(true);
        swapCursor(cursor);
    }

    /**
     * Move o Cursor para a posição correta antes de chamar o onBind abstrato.
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (!isDataValid(mCursor)) {
            throw new IllegalStateException("O Cursor não é válido/existe.");
        }

        // Move o Cursor para a posição solicitada
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Não foi possível mover o Cursor para a posição: " + position);
        }

        // Chama o método abstrato que será implementado pelas classes concretas
        onBindViewHolder(holder, mCursor);
    }

    /**
     * Método abstrato a ser implementado por subclasses, onde a lógica de
     * preenchimento do ViewHolder com os dados do Cursor deve ocorrer.
     */
    public abstract void onBindViewHolder(VH holder, Cursor cursor);

    /**
     * Retorna o número de itens, baseado no Cursor.
     */
    @Override
    public int getItemCount() {
        if (isDataValid(mCursor)) {
            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * Retorna a ID do item (a ID da linha do banco de dados).
     */
    @Override
    public long getItemId(int position) {
        if (isDataValid(mCursor) && mCursor.moveToPosition(position)) {
            // Retorna o valor da coluna _ID (row ID)
            return mCursor.getLong(mRowIdColumn);
        }
        return RecyclerView.NO_ID;
    }

    /**
     * Troca o Cursor atual por um novo. Fecha o antigo se for diferente e válido.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null; // Nenhuma mudança
        }

        Cursor oldCursor = mCursor;
        if (oldCursor != null) {
            oldCursor.close(); // Fecha o Cursor antigo para liberar o recurso
        }

        mCursor = newCursor;
        if (mCursor != null) {
            // Tenta obter o índice da coluna _ID (necessário para getItemId)
            mRowIdColumn = mCursor.getColumnIndexOrThrow("_id");
            // Notifica o Adapter para desenhar a nova lista
            notifyDataSetChanged();
        } else {
            mRowIdColumn = -1;
            // Notifica o Adapter que a lista está vazia
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    /**
     * Verifica se o Cursor é válido (não nulo e não fechado).
     */
    public boolean isDataValid(Cursor cursor) {
        return cursor != null && !cursor.isClosed();
    }

    /**
     * Retorna o Cursor atual.
     */
    public Cursor getCursor() {
        return mCursor;
    }

    /**
     * Retorna o item na posição específica.
     */
    public Cursor getItem(int position) {
        if (isDataValid(mCursor) && mCursor.moveToPosition(position)) {
            return mCursor;
        }
        return null;
    }
}