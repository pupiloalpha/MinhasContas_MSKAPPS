package com.msk.minhascontas.resumos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class ResumoMensal extends Fragment implements View.OnClickListener {


    public static final String ANO_PAGINA = "ano_pagina";
    public static final String MES_PAGINA = "mes_pagina";

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
    private int mes, ano;
    private double[] valores, valoresDesp, valoresRec, valoresSaldo,
            valoresAplicados;
    // ELEMENTOS DAS PAGINAS
    private View rootView;

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static ResumoMensal newInstance(int mes, int ano) {
        ResumoMensal fragment = new ResumoMensal();
        Bundle args = new Bundle();
        args.putInt(ANO_PAGINA, ano);
        args.putInt(MES_PAGINA, mes);
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
        rootView = inflater.inflate(R.layout.conteudo_resumos, container, false);
        Bundle args = getArguments();

        mes = args.getInt(MES_PAGINA);
        ano = args.getInt(ANO_PAGINA);

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
        // PREENCHE AS LINHAS DA TABELA

        // VALORES DE RECEITAS
        if (dbContas.quantasContasPorTipo(receita, 0, mes, ano) > 0)
            valores[0] = dbContas.somaContas(receita, 0, mes, ano);
        else
            valores[0] = 0.0D;

        // VALOR RECEITAS RECEBIDAS
        if (dbContas.quantasContasPagasPorTipo(receita, "paguei", 0, mes, ano) > 0)
            valoresRec[0] = dbContas.somaContasPagas(receita, "paguei", 0, mes,
                    ano);
        else
            valoresRec[0] = 0.0D;
        // VALOR RECEITAS A RECEBAR
        if (dbContas.quantasContasPagasPorTipo(receita, "falta", 0, mes, ano) > 0)
            valoresRec[1] = dbContas.somaContasPagas(receita, "falta", 0, mes,
                    ano);
        else
            valoresRec[1] = 0.0D;

        // VALORES DE DESPESAS
        if (dbContas.quantasContasPorTipo(despesa, 0, mes, ano) > 0)
            valores[1] = dbContas.somaContas(despesa, 0, mes, ano);
        else
            valores[1] = 0.0D;
        // VALOR CONTAS PAGAS
        if (dbContas.quantasContasPagasPorTipo(despesa, "paguei", 0, mes, ano) > 0)
            valoresDesp[0] = dbContas.somaContasPagas(despesa, "paguei", 0,
                    mes, ano);
        else
            valoresDesp[0] = 0.0D;
        // VALOR CONTAS A PAGAR
        if (dbContas.quantasContasPagasPorTipo(despesa, "falta", 0, mes, ano) > 0)
            valoresDesp[1] = dbContas.somaContasPagas(despesa, "falta", 0, mes,
                    ano);
        else
            valoresDesp[1] = 0.0D;

        // VALORES DAS CATEGORIAS DE DESPESAS
        for (int i = 0; i < despesas.length; i++) {
            if (dbContas.quantasContasPorClasse(despesas[i], 0, mes,
                    ano) > 0)
                valoresDesp[i + 2] = dbContas.somaContasPorClasse(
                        despesas[i], 0, mes, ano);
            else
                valoresDesp[i + 2] = 0.0D;
        }

        // VALORES DE APLICACOES
        if (dbContas.quantasContasPorTipo(aplicacao, 0, mes, ano) > 0)
            valores[2] = dbContas.somaContas(aplicacao, 0, mes, ano);
        else
            valores[2] = 0.0D;

        for (int j = 0; j < aplicacoes.length; j++) {
            if (dbContas.quantasContasPorClasse(aplicacoes[j], 0,
                    mes, ano) > 0)
                valoresAplicados[j] = dbContas.somaContasPorClasse(
                        aplicacoes[j], 0, mes, ano);
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
        if (dbContas.quantasContasPorTipo(receita, 0, mes_anterior,
                ano_anterior) > 0)
            r = dbContas.somaContas(receita, 0, mes_anterior, ano_anterior);

        double d = 0.0D; // DESPESA MES ANTERIOR
        if (dbContas.quantasContasPorTipo(despesa, 0, mes_anterior,
                ano_anterior) > 0)
            d = dbContas.somaContas(despesa, 0, mes_anterior, ano_anterior);

        double s = r - d; // SALDO MES ANTERIOR
        if (dbContas.quantasContasPorMes(mes_anterior, ano_anterior) > 0)
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


    @Override
    public void onClick(View v) {


        dados_mes.putInt("mes", mes);
        dados_mes.putInt("ano", ano);

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
