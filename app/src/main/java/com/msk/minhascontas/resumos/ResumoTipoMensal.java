package com.msk.minhascontas.resumos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;

import java.text.NumberFormat;
import java.util.Locale;

public class ResumoTipoMensal extends Fragment implements View.OnClickListener {


    public static final String ANO_PAGINA = "ano_pagina";
    public static final String MES_PAGINA = "mes_pagina";
    public static final String NR_PAGINA = "nr_pagina";

    // BARRA NO TOPO DO APLICATIVO
    private Bundle dados_mes = new Bundle();

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
    private int mes, ano, nrPagina;
    private double[] valores, valoresDesp, valoresRec, valoresSaldo,
            valoresAplicados;
    // ELEMENTOS DAS PAGINAS
    private View rootView;

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static ResumoTipoMensal newInstance(int mes, int ano, int nr) {
        ResumoTipoMensal fragment = new ResumoTipoMensal();
        Bundle args = new Bundle();
        args.putInt(ANO_PAGINA, ano);
        args.putInt(MES_PAGINA, mes);
        args.putInt(NR_PAGINA, nr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dbContas = new DBContas(activity);
        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // COLOCA OS MESES NA TELA
        rootView = inflater.inflate(R.layout.resumo_por_tipo, container, false);
        Bundle args = getArguments();

        mes = args.getInt(MES_PAGINA);
        ano = args.getInt(ANO_PAGINA);
        nrPagina = args.getInt(NR_PAGINA);

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

        valorPago = ((TextView) rootView
                .findViewById(R.id.tvValorDespPaga));
        valorPagar = ((TextView) rootView
                .findViewById(R.id.tvValorDespPagar));
        valorDespFixa = ((TextView) rootView
                .findViewById(R.id.tvValorDespFixa));
        valorDespVar = ((TextView) rootView
                .findViewById(R.id.tvValorDespVar));
        valorPrestacoes = ((TextView) rootView
                .findViewById(R.id.tvValorPrestacoes));
        valorCartao = ((TextView) rootView
                .findViewById(R.id.tvValorCartaoCredito));
        valorReceber = ((TextView) rootView
                .findViewById(R.id.tvValorReceber));
        valorRecebido = ((TextView) rootView
                .findViewById(R.id.tvValorRecebido));
        valorFundos = ((TextView) rootView
                .findViewById(R.id.tvValorFundos));
        valorPoupanca = ((TextView) rootView
                .findViewById(R.id.tvValorPoupancas));
        valorPrevidencia = ((TextView) rootView
                .findViewById(R.id.tvValorPrevidencias));
        valorSaldoAtual = ((TextView) rootView
                .findViewById(R.id.tvValorSaldoAtual));
        valorSaldoAnterior = ((TextView) rootView
                .findViewById(R.id.tvValorSaldoAnterior));
        valorDesp = ((TextView) rootView
                .findViewById(R.id.tvValorDespesas));
        valorRec = ((TextView) rootView
                .findViewById(R.id.tvValorReceitas));
        valorAplic = ((TextView) rootView
                .findViewById(R.id.tvValorAplicacoes));
        valorSaldo = ((TextView) rootView
                .findViewById(R.id.tvValorSaldo));

        aplic = (LinearLayout) rootView.findViewById(R.id.l_aplicacoes);
        desp = (LinearLayout) rootView.findViewById(R.id.l_despesas);
        rec = (LinearLayout) rootView.findViewById(R.id.l_receitas);
        sald = (LinearLayout) rootView.findViewById(R.id.l_saldo);

    }

    private void InsereValores() {

        Locale current = getResources().getConfiguration().locale;
        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

        // INSERE OS VALORES EM CADA ITEtM

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
        }
        if (valoresSaldo[1] < 0.0D) {
            valorSaldoAnterior.setTextColor(Color.parseColor("#CC0000"));
        }

        if (valores[3] < 0.0D) {
            valorSaldo.setTextColor(Color.parseColor("#CC0000"));
        }
    }

    private void Saldo() {

        // DEFINE OS NOMES DA LINHAS DA TABELA

        dbContas.open();
        String despesa = getResources().getString(R.string.linha_despesa);
        String[] despesas = getResources().getStringArray(R.array.TipoDespesa);
        String receita = getResources().getString(R.string.linha_receita);
        String aplicacao = getResources().getString(R.string.linha_aplicacoes);
        String[] aplicacoes = getResources().getStringArray(R.array.TipoAplicacao);

        valores = new double[4];
        valoresDesp = new double[6];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];

        Cursor somador = null;

        // PREENCHE AS LINHAS DA TABELA

        // VALORES DE RECEITAS
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 1);
        if (somador.getCount() > 0)
            valores[0] = SomaContas(somador);
        else
            valores[0] = 0.0D;

        // VALOR RECEITAS RECEBIDAS
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 1, "paguei");
        if (somador.getCount() > 0)
            valoresRec[0] = SomaContas(somador);
        else
            valoresRec[0] = 0.0D;

        // VALOR RECEITAS A RECEBAR
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 1, "falta");
        if (somador.getCount() > 0)
            valoresRec[1] = SomaContas(somador);
        else
            valoresRec[1] = 0.0D;

        // VALORES DE DESPESAS
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 0);
        if (somador.getCount() > 0)
            valores[1] = SomaContas(somador);
        else
            valores[1] = 0.0D;

        // VALOR CONTAS PAGAS
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 0, "paguei");
        if (somador.getCount() > 0)
            valoresDesp[0] = SomaContas(somador);
        else
            valoresDesp[0] = 0.0D;

        // VALOR CONTAS A PAGAR
        somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, 0, "falta");
        if (somador.getCount() > 0)
            valoresDesp[1] = SomaContas(somador);
        else
            valoresDesp[1] = 0.0D;

        // VALORES DAS CATEGORIAS DE DESPESAS
        for (int i = 0; i < despesas.length; i++) {
            somador = dbContas.buscaContasClasse(0, mes, ano, null, 0, i);
            if (somador.getCount() > 0)
                valoresDesp[i + 2] = SomaContas(somador);
            else
                valoresDesp[i + 2] = 0.0D;
        }

        // VALORES DE APLICACOES
        somador = dbContas.buscaContasTipo(0, mes, ano, null, 2);
        if (somador.getCount() > 0)
            valores[2] = SomaContas(somador);
        else
            valores[2] = 0.0D;

        for (int j = 0; j < aplicacoes.length; j++) {
            somador = dbContas.buscaContasClasse(0, mes, ano, null, 2, j);
            if (somador.getCount() > 0)
                valoresAplicados[j] = SomaContas(somador);
            else
                valoresAplicados[j] = 0.0D;
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
        double r = 0.0D; // RECEITA MES ANTERIOR
        somador = dbContas.buscaContasTipo(0, mes_anterior, ano_anterior, null, 1);
        if (somador.getCount() > 0)
            r = SomaContas(somador);

        double d = 0.0D; // DESPESA MES ANTERIOR
        somador = dbContas.buscaContasTipo(0, mes_anterior, ano_anterior, null, 0);
        if (somador.getCount() > 0)
            d = SomaContas(somador);

        somador.close(); // FECHA O CURSOR DO SOMADOR

        double s = r - d; // SALDO MES ANTERIOR
        if (dbContas.buscaContas(0, mes_anterior, ano_anterior, null).getCount() > 0)
            valoresSaldo[1] = s;
        else
            valoresSaldo[1] = 0.0D;

        // VALOR DO SALDO ATUAL
        boolean somaSaldo = buscaPreferencias.getBoolean("saldo", false);

        if (somaSaldo) {
            valores[3] = valoresRec[0] - valoresDesp[0]
                    + valoresSaldo[1];
        } else {
            valores[3] = valoresRec[0] - valoresDesp[0];
        }
        dbContas.close();
    }

    private double SomaContas(Cursor cursor) {
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        for (int j = 0; ; j++) {
            if (j >= i) {
                cursor.close();
                return d;
            }
            d += cursor.getDouble(9);
            cursor.moveToPrevious();
        }
    }

    @Override
    public void onClick(View v) {


        dados_mes.putInt("mes", mes);
        dados_mes.putInt("ano", ano);
        dados_mes.putInt("nr", nrPagina);

        switch (v.getId()) {
            case R.id.l_saldo:
                dados_mes.putString("tipo", "todas");
                break;
            case R.id.l_aplicacoes:
                dados_mes.putString("tipo", getResources().getString(R.string.linha_aplicacoes));
                break;
            case R.id.l_despesas:
                dados_mes.putString("tipo", getResources().getString(R.string.linha_despesa));
                break;
            case R.id.l_receitas:
                dados_mes.putString("tipo", getResources().getString(R.string.linha_receita));
                break;
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
