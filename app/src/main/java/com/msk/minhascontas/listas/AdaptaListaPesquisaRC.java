package com.msk.minhascontas.listas;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.msk.minhascontas.R;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter para RecyclerView que exibe os resultados da pesquisa,
 * herdando a gestão do Cursor de CursorRecyclerViewAdapter.
 */
public class AdaptaListaPesquisaRC extends CursorRecyclerViewAdapter<AdaptaListaPesquisaRC.ViewHolder> {

    private final Resources res;
    private final String[] semana;
    private final String[] categoriaConta;
    private final NumberFormat dinheiro;
    private final DateFormat dataFormato;
    // Usamos um Set para IDs de contasListadas selecionadas para maior eficiência
    private final Set<Long> selecoes = new HashSet<>();

    // Constantes para cores (Carregadas via ContextCompat)
    private final int RED_COLOR;
    private final int GREEN_COLOR;
    private final int BLUE_COLOR;
    private final int SELECTION_COLOR;

    // Interface para manipular cliques (necessário no RecyclerView)
    private OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onItemClick(long id, int position);
        boolean onItemLongClick(long id, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView nome;
        public final TextView categoria;
        public final TextView data;
        public final TextView dia;
        public final TextView valor;
        public final ImageView pagamento;

        public ViewHolder(View view) {
            super(view);
            // Referências de layout da linha_pesquisa
            nome = view.findViewById(R.id.tvNomeContaCriada);
            categoria = view.findViewById(R.id.tvNomeCategoria);
            data = view.findViewById(R.id.tvDataContaCriada);
            dia = view.findViewById(R.id.tvDiaContaCriada);
            valor = view.findViewById(R.id.tvValorContaCriada);
            pagamento = view.findViewById(R.id.ivPagamento);
        }
    }

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------

    public AdaptaListaPesquisaRC(Context context, Cursor cursor) {
        super(cursor);

        res = context.getResources();

        // 1. Inicialização das cores via ContextCompat
        RED_COLOR = ContextCompat.getColor(context, R.color.despesa_color);
        GREEN_COLOR = ContextCompat.getColor(context, R.color.aplicacao_color);
        BLUE_COLOR = ContextCompat.getColor(context, R.color.receita_color);
        SELECTION_COLOR = ContextCompat.getColor(context, R.color.linha_selecionada);

        // 2. Obtendo o Locale de forma moderna
        Locale current;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            current = res.getConfiguration().getLocales().get(0);
        } else {
            current = res.getConfiguration().locale;
        }

        dinheiro = NumberFormat.getCurrencyInstance(current);
        dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
        semana = res.getStringArray(R.array.Semana);
        categoriaConta = res.getStringArray(R.array.CategoriaConta);
    }

    // -------------------------------------------------------------------------
    // Implementação de RecyclerView.Adapter (Criação de View)
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout da linha
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.linha_pesquisa, parent, false);

        final ViewHolder holder = new ViewHolder(view);

        // Configura os Listeners de clique
        view.setOnClickListener(v -> {
            if (itemClickListener != null) {
                // Notifica o clique com a ID do banco de dados e a posição
                itemClickListener.onItemClick(getItemId(holder.getAdapterPosition()), holder.getAdapterPosition());
            }
        });

        view.setOnLongClickListener(v -> {
            if (itemClickListener != null) {
                // Notifica o clique longo
                return itemClickListener.onItemLongClick(getItemId(holder.getAdapterPosition()), holder.getAdapterPosition());
            }
            return false;
        });

        return holder;
    }

    // -------------------------------------------------------------------------
    // Implementação de CursorRecyclerViewAdapter (Preenchimento de Dados)
    // -------------------------------------------------------------------------

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        // Cursor já está na posição correta
        long idConta = getItemId(holder.getAdapterPosition());

        // Recuperar dados do Cursor
        String nomeconta = cursor.getString(1);
        int tipo = cursor.getInt(2);
        int classe = cursor.getInt(3);
        int categ = cursor.getInt(4);
        int d = cursor.getInt(5);
        int m = cursor.getInt(6);
        int a = cursor.getInt(7);
        String status = cursor.getString(9);
        int qtRepeticoes = cursor.getInt(10);
        int nrRepeticao = cursor.getInt(11);

        // Nome (com ajuste para recorrentes)
        holder.nome.setText(nomeconta);

        if (qtRepeticoes > 1 && (classe == 0 || classe == 3) && tipo == 0) {
            String nomeRecorrente = String.format(Locale.getDefault(), "%s %d/%d",
                    nomeconta, nrRepeticao, qtRepeticoes);
            holder.nome.setText(nomeRecorrente);
        }

        // Data e Dia da Semana
        Calendar c = Calendar.getInstance();
        c.set(a, m, d);
        int s = c.get(Calendar.DAY_OF_WEEK);
        holder.data.setText(dataFormato.format(c.getTime()));
        holder.dia.setText(semana[s - 1]);

        // Categoria e Classe
        String[] classeConta;
        if (tipo == 0) { // Despesa
            classeConta = res.getStringArray(R.array.TipoDespesa);
            holder.categoria.setText(String.format("%s | %s", classeConta[classe], categoriaConta[categ]));
        } else if (tipo == 1) { // Receita
            classeConta = res.getStringArray(R.array.TipoReceita);
            holder.categoria.setText(classeConta[classe]);
        } else { // Aplicação (tipo == 2)
            classeConta = res.getStringArray(R.array.TipoAplicacao);
            holder.categoria.setText(classeConta[classe]);
        }

        // Valor e Status de Pagamento/Cor
        holder.pagamento.setVisibility(View.INVISIBLE);
        holder.valor.setText(dinheiro.format(cursor.getDouble(8)));

        // Uso das constantes de cor
        if (tipo == 0) { // Despesa
            holder.valor.setTextColor(RED_COLOR);
            if ("paguei".equals(status))
                holder.pagamento.setVisibility(View.VISIBLE);
        } else if (tipo == 2) { // Aplicação
            holder.valor.setTextColor(GREEN_COLOR);
        } else { // Receita
            holder.valor.setTextColor(BLUE_COLOR);
            if ("paguei".equals(status))
                holder.pagamento.setVisibility(View.VISIBLE);
        }

        // Estado de Seleção
        if (selecoes.contains(idConta)) {
            holder.itemView.setBackgroundColor(SELECTION_COLOR);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // -------------------------------------------------------------------------
    // Métodos de Seleção
    // -------------------------------------------------------------------------

    /**
     * Marca/Desmarca uma conta pelo seu ID no banco de dados.
     * Atualiza apenas o item afetado (melhor performance).
     */
    public void marcaConta(long idConta, Boolean seleciona) {
        int posicao = -1;

        // Localiza a posição do item com o ID dado
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndexOrThrow("_id");
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                if (cursor.getLong(idColumnIndex) == idConta) {
                    posicao = i;
                    break;
                }
            }
        }

        if (seleciona) {
            selecoes.add(idConta);
        } else {
            selecoes.remove(idConta);
        }

        // Se a posição foi encontrada, atualiza apenas aquele item
        if (posicao != -1) {
            notifyItemChanged(posicao);
        }
    }

    /**
     * Limpa todas as seleções.
     */
    public void limpaSelecao() {
        if (!selecoes.isEmpty()) {
            selecoes.clear();
            // Notifica que toda a lista mudou para resetar os fundos
            notifyDataSetChanged();
        }
    }

    /**
     * Retorna o Set de IDs de contasListadas selecionadas.
     */
    public Set<Long> getSelecoes() {
        return selecoes;
    }
}