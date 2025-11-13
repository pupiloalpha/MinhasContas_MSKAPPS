
package com.msk.minhascontas.resumos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter; // Added import for ContaFilter
import com.msk.minhascontas.db.DBContas.Colunas; // Added import for Colunas

import java.text.NumberFormat;
import java.util.Locale;

public class ResumoCategoriaDiario extends Fragment implements View.OnClickListener {

    public static final String ANO_PAGINA = "ano_pagina";
    public static final String MES_PAGINA = "mes_pagina";
    public static final String DIA_PAGINA = "dia_pagina";

    private final Bundle dados_mes = new Bundle();

    private DBContas dbContas;

    private SharedPreferences buscaPreferencias = null;

    private TextView valorDesp, valorRec, valorAplic, valorSaldo, valorBanco,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorFundos, valorPoupanca, valorPrevidencia, vAlimentacao, vEducacao,
            vMoradia, vTransporte, vSaude, vOutros, valorReceber, valorRecebido;

    private LinearLayout aplic, desp, rec, sald;

    private int dia, mes, ano;
    private double[] valores, valoresDesp, valoresRec, valoresSaldo,
            valoresAplicados;

    private View rootView;

    public ResumoCategoriaDiario() {
    }

    public static ResumoCategoriaDiario newInstance(int dia, int mes, int ano) {
        ResumoCategoriaDiario fragment = new ResumoCategoriaDiario();
        Bundle args = new Bundle();
        args.putInt(ANO_PAGINA, ano);
        args.putInt(MES_PAGINA, mes);
        args.putInt(DIA_PAGINA, dia);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbContas = DBContas.getInstance(context);
        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.resumo_por_categoria, container, false);
        Bundle args = getArguments();

        if (args != null) {
            dia = args.getInt(DIA_PAGINA);
            mes = args.getInt(MES_PAGINA);
            ano = args.getInt(ANO_PAGINA);
        }

        Iniciar();

        Saldo();

        InsereValores();

        aplic.setOnClickListener(this);
        desp.setOnClickListener(this);
        rec.setOnClickListener(this);
        sald.setOnClickListener(this);

        return rootView;
    }

    private void Iniciar() {

        valorPago = rootView
                .findViewById(R.id.tvValorDespPaga);
        valorPagar = rootView
                .findViewById(R.id.tvValorDespPagar);
        valorBanco = rootView
                .findViewById(R.id.tvValorBanco);
        vAlimentacao = rootView
                .findViewById(R.id.tvValorAlimentacao);
        vEducacao = rootView
                .findViewById(R.id.tvValorEducacao);
        vMoradia = rootView
                .findViewById(R.id.tvValorMoradia);
        vSaude = rootView
                .findViewById(R.id.tvValorSaude);
        vTransporte = rootView
                .findViewById(R.id.tvValorTransporte);
        vOutros = rootView
                .findViewById(R.id.tvValorOutros);
        valorCartao = rootView
                .findViewById(R.id.tvValorCartaoCredito);
        valorReceber = rootView
                .findViewById(R.id.tvValorReceber);
        valorRecebido = rootView
                .findViewById(R.id.tvValorRecebido);
        valorFundos = rootView
                .findViewById(R.id.tvValorFundos);
        valorPoupanca = rootView
                .findViewById(R.id.tvValorPoupancas);
        valorPrevidencia = rootView
                .findViewById(R.id.tvValorPrevidencias);
        valorSaldoAtual = rootView
                .findViewById(R.id.tvValorSaldoAtual);
        valorSaldoAnterior = rootView
                .findViewById(R.id.tvValorSaldoAnterior);
        valorDesp = rootView
                .findViewById(R.id.tvValorDespesas);
        valorRec = rootView
                .findViewById(R.id.tvValorReceitas);
        valorAplic = rootView
                .findViewById(R.id.tvValorAplicacoes);
        valorSaldo = rootView
                .findViewById(R.id.tvValorSaldo);

        aplic = rootView.findViewById(R.id.l_aplicacoes);
        desp = rootView.findViewById(R.id.l_despesas);
        rec = rootView.findViewById(R.id.l_receitas);
        sald = rootView.findViewById(R.id.l_saldo);
    }

    private void InsereValores() {

        Locale current = Locale.getDefault();
        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

        valorPago.setText(dinheiro.format(valoresDesp[0]));
        valorPagar.setText(dinheiro.format(valoresDesp[1]));
        valorCartao.setText(dinheiro.format(valoresDesp[2]));

        vAlimentacao.setText(dinheiro.format(valoresDesp[3]));
        vEducacao.setText(dinheiro.format(valoresDesp[4]));
        vMoradia.setText(dinheiro.format(valoresDesp[6]));
        vSaude.setText(dinheiro.format(valoresDesp[7]));
        vTransporte.setText(dinheiro.format(valoresDesp[8]));
        vOutros.setText(dinheiro.format(valoresDesp[10]));

        valorReceber.setText(dinheiro.format(valoresRec[1]));
        valorRecebido.setText(dinheiro.format(valoresRec[0]));

        valorFundos.setText(dinheiro.format(valoresAplicados[0]));
        valorPoupanca.setText(dinheiro.format(valoresAplicados[1]));
        valorPrevidencia.setText(dinheiro.format(valoresAplicados[2]));

        valorSaldoAtual.setText(dinheiro.format(valoresSaldo[0]));
        valorSaldoAnterior.setText(dinheiro.format(valoresSaldo[1]));
        valorBanco.setText(dinheiro.format(valores[3]));

        valorRec.setText(dinheiro.format(valores[0]));
        valorDesp.setText(dinheiro.format(valores[1]));
        valorAplic.setText(dinheiro.format(valores[2]));
        valorSaldo.setText(dinheiro.format(valores[3]));

        if (valoresSaldo[0] < 0.0D) {
            valorSaldoAtual.setTextColor(Color.parseColor("#CC0000"));
        } else {
            valorSaldoAtual.setTextColor(Color.parseColor("#669900"));
        }
        if (valoresSaldo[1] < 0.0D) {
            valorSaldoAnterior.setTextColor(Color.parseColor("#CC0000"));
        } else {
            valorSaldoAnterior.setTextColor(Color.parseColor("#669900"));
        }
        if (valores[3] < 0.0D) {
            valorSaldo.setTextColor(Color.parseColor("#CC0000"));
            valorBanco.setTextColor(Color.parseColor("#CC0000"));
        } else {
            valorBanco.setTextColor(Color.parseColor("#669900"));
        }
    }

    private void Saldo() {

        valores = new double[4];
        valoresDesp = new double[11];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];

        valores[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1));

        valoresRec[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_PAGO));

        valoresRec[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_FALTA));

        valores[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0));

        valoresDesp[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_PAGO));

        valoresDesp[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_FALTA));

        valoresDesp[2] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setClasse(0));

        for (int i = 0; i < 8; i++) {
            valoresDesp[i + 3] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setCategoria(String.valueOf(i)));
        }

        valoresDesp[10] = valoresDesp[5] + valoresDesp[9] + valoresDesp[10];

        valores[2] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(2));

        for (int j = 0; j < 3; j++) {
            valoresAplicados[j] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(2).setClasse(j));
        }

        valoresSaldo[0] = valores[0] - valores[1];

        valores[3] = valores[0] - valoresDesp[0];

        int mes_anterior = mes - 1;
        int ano_anterior = ano;
        if (mes_anterior < 0) {
            mes_anterior = 11;
            ano_anterior = ano_anterior - 1;
        }
        double r = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(1));

        double d = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(0));

        double s = r - d;
        // The original code checks contas.getCount() > 0, which implies if there are any accounts for the previous month,
        // then consider the balance. Otherwise, it's 0.0D. getSumForFilter returns 0.0D if no accounts are found,
        // so this logic needs to be handled.
        // If the original `buscaContas` was just to check existence, we need to check if getContasByFilter returns anything.
        // For simplicity and to match the original logic, if there are any accounts, the previous month's balance is 's'.
        // Otherwise, it's 0.0D.
        try (Cursor contasCursor = dbContas.getContasByFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior), null)) {
            if (contasCursor != null && contasCursor.getCount() > 0)
                valoresSaldo[1] = s;
            else
                valoresSaldo[1] = 0.0D;
        }


        boolean somaSaldo = buscaPreferencias.getBoolean("saldo", false);
        if (somaSaldo) {
            valores[3] = valoresRec[0] - valoresDesp[0]
                    + valoresSaldo[1];
        } else {
            valores[3] = valoresRec[0] - valoresDesp[0];
        }
    }

    // New helper method to get sum for a given filter
    private double getSumForFilter(DBContas.ContaFilter filter) {
        double sum = 0.0D;
        try (Cursor cursor = dbContas.getContasByFilter(filter, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sum += cursor.getDouble(cursor.getColumnIndexOrThrow(DBContas.Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        }
        return sum;
    }

    @Override
    public void onClick(View v) {

        dados_mes.putInt("mes", mes);
        dados_mes.putInt("ano", ano);
        dados_mes.putInt("nr", 12);

        int viewId = v.getId();
        if (viewId == R.id.l_saldo) {
            dados_mes.putInt("tipo", -1);
        } else if (viewId == R.id.l_aplicacoes) {
            dados_mes.putInt("tipo", 2);
        } else if (viewId == R.id.l_despesas) {
            dados_mes.putInt("tipo", 0);
        } else if (viewId == R.id.l_receitas) {
            dados_mes.putInt("tipo", 1);
        }

        Intent mostra_resumo = new Intent("com.msk.minhascontas.CONTASDOMES");
        mostra_resumo.putExtras(dados_mes);
        requireActivity().startActivity(mostra_resumo);
    }

    @Override
    public void onResume() {
        Saldo();
        InsereValores();
        super.onResume();
    }
}
