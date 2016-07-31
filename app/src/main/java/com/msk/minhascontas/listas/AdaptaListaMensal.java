package com.msk.minhascontas.listas;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.msk.minhascontas.R;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AdaptaListaMensal extends CursorAdapter {

    private LayoutInflater inflater;
    private String[] prestacao, semana;
    private HashMap<Integer, Boolean> selecoes = new HashMap<Integer, Boolean>();
    private NumberFormat dinheiro;

    @SuppressWarnings("deprecation")
    public AdaptaListaMensal(Context context, Cursor c, String[] array, String[] array1) {
        super(context, c);
        inflater = LayoutInflater.from(context);
        prestacao = array;
        semana = array1;
        Locale current = context.getResources().getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.linha_conta_nova, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nome = ((TextView) view.findViewById(R.id.tvNomeContaCriada));
        TextView categoria = ((TextView) view.findViewById(R.id.tvNomeCategoria));
        TextView data = ((TextView) view.findViewById(R.id.tvDataContaCriada));
        TextView dia = ((TextView) view.findViewById(R.id.tvDiaContaCriada));
        TextView valor = ((TextView) view.findViewById(R.id.tvValorContaCriada));
        ImageView pagamento = ((ImageView) view.findViewById(R.id.ivPagamento));

        int i = cursor.getInt(10);
        String status = cursor.getString(4);
        int classe = cursor.getInt(3);
        int tipo = cursor.getInt(2);
        String nomeconta = cursor.getString(1);
        int d = cursor.getInt(6);
        int m = cursor.getInt(7);
        int a = cursor.getInt(8);

        nome.setText(nomeconta);

        if ((i > 1) && classe == 0 && tipo == 0) {
            String str4 = nomeconta + " " + cursor.getInt(11) + "/" + i;
            nome.setText(str4);
        }

        if ((i > 1) && classe == 3 && tipo == 0) {
            String str4 = nomeconta + " " + cursor.getInt(11) + "/" + i;
            nome.setText(str4);
        }

        data.setText("" + d);

        Calendar c = Calendar.getInstance();
        c.set(a, m, d);
        int s = c.get(Calendar.DAY_OF_WEEK);

        dia.setText(semana[s - 1]);

        categoria.setText(classe);

        pagamento.setVisibility(View.INVISIBLE);

        valor.setText(dinheiro.format(cursor.getDouble(9)));

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
