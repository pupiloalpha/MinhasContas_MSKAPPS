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
import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.DBContas.Colunas;

import java.text.NumberFormat;
import java.util.Locale;

public class ResumoTipoDiario extends Fragment implements View.OnClickListener {

    public static final String ANO_PAGINA = "ano_pagina";
    public static final String MES_PAGINA = "mes_pagina";
    public static final String DIA_PAGINA = "dia_pagina";

    // BARRA NO TOPO DO APLICATIVO
    private final Bundle dados_mes = new Bundle();

    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas;

    // OPCOES DE AJUSTE
    private SharedPreferences buscaPreferencias = null;

    // ELEMENTOS UTILIZADOS EM TELA
    private TextView valorDesp, valorRec, valorAplic, valorSaldo,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorFundos, valorPoupanca, valorPrevidencia, valorDespFixa, valorDespVar,
            valorPrestacoes, valorReceber, valorRecebido;

    private LinearLayout aplic, desp, rec, sald;

    // VARIAEIS UTILIZADAS
    private int dia, mes, ano;
    private double[] valores, valoresDesp, valoresRec, valoresSaldo,
            valoresAplicados;

    // ELEMENTOS DAS PAGINAS
    private View rootView;

    public ResumoTipoDiario() {
    }

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static ResumoTipoDiario newInstance(int dia, int mes, int ano) {
        ResumoTipoDiario fragment = new ResumoTipoDiario();
        Bundle args = new Bundle();
        args.putInt(ANO_PAGINA, ano);
        args.putInt(MES_PAGINA, mes);
        args.putInt(DIA_PAGINA, dia);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbContas = DBContas.getInstance(context);
        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // COLOCA OS MESES NA TELA
        rootView = inflater.inflate(R.layout.resumo_por_tipo, container, false);
        Bundle args = getArguments();

        if (args != null) {
            dia = args.getInt(DIA_PAGINA);
            mes = args.getInt(MES_PAGINA);
            ano = args.getInt(ANO_PAGINA);
        }

        // DEFINE OS ELEMENTOS QUE SERAO EXIBIDOS
        Iniciar();

        // CALCULA OS VALORES QUE SERAO EXIBIDOS
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
        valorDespFixa = rootView
                .findViewById(R.id.tvValorDespFixa);
        valorDespVar = rootView
                .findViewById(R.id.tvValorDespVar);
        valorPrestacoes = rootView
                .findViewById(R.id.tvValorPrestacoes);
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

        valorDespFixa.setText(dinheiro.format(valoresDesp[3]));
        valorDespVar.setText(dinheiro.format(valoresDesp[4]));
        valorPrestacoes.setText(dinheiro.format(valoresDesp[5]));

        valorReceber.setText(dinheiro.format(valoresRec[1]));
        valorRecebido.setText(dinheiro.format(valoresRec[0]));

        valorFundos.setText(dinheiro.format(valoresAplicados[0]));
        valorPoupanca.setText(dinheiro.format(valoresAplicados[1]));
        valorPrevidencia.setText(dinheiro.format(valoresAplicados[2]));

        valorSaldoAtual.setText(dinheiro.format(valoresSaldo[0]));
        valorSaldoAnterior.setText(dinheiro.format(valoresSaldo[1]));

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
        } else {
            //valorSaldo.setTextColor(Color.parseColor("#2B2B2B"));
        }
    }

    private void Saldo() {

        // DEFINE OS NOMES DA LINHAS DA TABELA

        valores = new double[4];
        valoresDesp = new double[6];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];

        // PREENCHE AS LINHAS DA TABELA

        // VALORES DE RECEITAS
        valores[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1));

        // VALOR RECEITAS RECEBIDAS
        valoresRec[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR RECEITAS A RECEBAR
        valoresRec[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DE DESPESAS
        valores[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0));

        // VALOR CONTAS PAGAS
        valoresDesp[0] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR CONTAS A PAGAR
        valoresDesp[1] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DAS CATEGORIAS DE DESPESAS
        for (int i = 0; i < 4; i++) {
            valoresDesp[i + 2] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(0).setClasse(i));
        }

        // VALORES DE APLICACOES
        valores[2] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(2));

        for (int j = 0; j < 3; j++) {
            valoresAplicados[j] = getSumForFilter(new ContaFilter().setDia(dia).setMes(mes).setAno(ano).setTipo(2).setClasse(j));
        }

        // VALOR DO SALDO MENSAL
        valoresSaldo[0] = valores[0] - valores[1];

        // VALOR DO SALDO ATUAL

        valores[3] = valores[0] - valoresDesp[0];

        // VALOR DO SALDO DO MES ANTERIOR

        int mes_anterior = mes - 1; // DEFINE MES ANTERIOR
        int ano_anterior = ano;
        if (mes_anterior < 0) {
            mes_anterior = 11;
            ano_anterior = ano_anterior - 1;
        }
        double r = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(1)); // RECEITA MES ANTERIOR

        double d = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(0)); // DESPESA MES ANTERIOR

        double s = r - d; // SALDO MES ANTERIOR
        try (Cursor contasCursor = dbContas.getContasByFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior), null)) {
            if (contasCursor != null && contasCursor.getCount() > 0)
                valoresSaldo[1] = s;
            else
                valoresSaldo[1] = 0.0D;
        }

        // VALOR DO SALDO ATUAL
        boolean somaSaldo = buscaPreferencias.getBoolean("saldo", false);
        if (somaSaldo) {
            valores[3] = valoresRec[0] - valoresDesp[0]
                    + valoresSaldo[1];
        } else {
            valores[3] = valoresRec[0] - valoresDesp[0];
        }
    }

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
        startActivityForResult(mostra_resumo, 0);
    }

    @Override
    public void onResume() {
        // CALCULA OS VALORES QUE SERAO EXIBIDOS
        Saldo();
        InsereValores();
        super.onResume();
    }

}
