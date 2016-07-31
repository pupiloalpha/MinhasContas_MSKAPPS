package com.msk.minhascontas.listas;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.msk.minhascontas.R;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AdaptaListaPesquisa extends CursorAdapter {

    private LayoutInflater inflater;
    private String[] semana;
    private HashMap<Integer, Boolean> selecoes = new HashMap<Integer, Boolean>();
    private NumberFormat dinheiro;
    private DateFormat dataFormato;
    private Resources res = null;

    @SuppressWarnings("deprecation")
    public AdaptaListaPesquisa(Context context, Cursor c, String[] array) {
        super(context, c);
        inflater = LayoutInflater.from(context);
        semana = array;
        res = context.getResources();
        Locale current = res.getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);
        dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.linha_pesquisa, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nome = ((TextView) view.findViewById(R.id.tvNomeContaCriada));
        TextView categoria = ((TextView) view.findViewById(R.id.tvNomeCategoria));
        TextView data = ((TextView) view.findViewById(R.id.tvDataContaCriada));
        TextView dia = ((TextView) view.findViewById(R.id.tvDiaContaCriada));
        TextView valor = ((TextView) view.findViewById(R.id.tvValorContaCriada));
        ImageView pagamento = ((ImageView) view.findViewById(R.id.ivPagamento));

        String nomeconta = cursor.getString(1);
        int tipo = cursor.getInt(2);
        int classe = cursor.getInt(3);
        int categ = cursor.getInt(4);
        int d = cursor.getInt(5);
        int m = cursor.getInt(6);
        int a = cursor.getInt(7);
        String status = cursor.getString(9);
        int i = cursor.getInt(10);
        nome.setText(nomeconta);

        if ((i > 1) && classe == 0 && tipo == 0) {
            String str4 = nomeconta + " " + cursor.getInt(11) + "/" + i;
            nome.setText(str4);
        }

        if ((i > 1) && classe == 3 && tipo == 0) {
            String str4 = nomeconta + " " + cursor.getInt(11) + "/" + i;
            nome.setText(str4);
        }

        Calendar c = Calendar.getInstance();
        c.set(a, m, d);
        int s = c.get(Calendar.DAY_OF_WEEK);
        data.setText(dataFormato.format(c.getTime()));

        dia.setText(semana[s - 1]);
        String[] classeConta;
        String[] categoriaConta = res.getStringArray(R.array.CategoriaConta);

        if (tipo == 0) {
            classeConta = res.getStringArray(R.array.TipoDespesa);
            categoria.setText(classeConta[classe] + " | " + categoriaConta[categ]);
        } else if (tipo == 1) {
            classeConta = res.getStringArray(R.array.TipoReceita);
            categoria.setText(classeConta[classe]);
        } else {
            classeConta = res.getStringArray(R.array.TipoAplicacao);
            categoria.setText(classeConta[classe]);
        }

        pagamento.setVisibility(View.INVISIBLE);

        valor.setText(dinheiro.format(cursor.getDouble(8)));

        if (tipo == 0) {
            valor.setTextColor(Color.parseColor("#CC0000"));
            if (status.equals("paguei"))
                pagamento.setVisibility(View.VISIBLE);
        } else if (tipo == 2) {
            valor.setTextColor(Color.parseColor("#669900"));
        } else {
            valor.setTextColor(Color.parseColor("#0099CC"));
            if (status.equals("paguei"))
                pagamento.setVisibility(View.VISIBLE);
        }
    }

    public void marcaConta(Integer i, Boolean b) {
        if (b)
            selecoes.put(i, b);
        else
            selecoes.remove(i);
        notifyDataSetChanged();
    }

    public void limpaSelecao() {
        selecoes = new HashMap<Integer, Boolean>();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (selecoes.get(position) != null)
            view.setBackgroundColor(Color.parseColor("#FFC5E1A5"));
        else
            view.setBackgroundColor(Color.WHITE);
        return view;
    }
}
