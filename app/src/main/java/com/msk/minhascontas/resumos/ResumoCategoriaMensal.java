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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.DBContas.Colunas;

import java.text.NumberFormat;

public class ResumoCategoriaMensal extends Fragment implements View.OnClickListener {

    public static final String ANO_PAGINA = "ano_pagina";
    public static final String MES_PAGINA = "mes_pagina";
    public static final String NR_PAGINA = "nr_pagina";

    // BARRA NO TOPO DO APLICATIVO
    private final Bundle dados_mes = new Bundle();

    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas;

    // OPCOES DE AJUSTE
    private SharedPreferences buscaPreferencias = null;

    // ELEMENTOS UTILIZADOS EM TELA
    private TextView valorDesp, valorRec, valorAplic, valorSaldo, valorBanco,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorFundos, valorPoupanca, valorPrevidencia, vAlimentacao, vEducacao,
            vMoradia, vTransporte, vSaude, vOutros, valorReceber, valorRecebido;

    private LinearLayout aplic, desp, rec, sald;

    // VARIAEIS UTILIZADAS
    private int mes, ano, nrPagina;
    private double[] valores, valoresDesp, valoresRec, valoresSaldo,
            valoresAplicados;
    // ELEMENTOS DAS PAGINAS
    private View rootView;

    private final ActivityResultLauncher<Intent> mostraResumoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle the result if needed
            });

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static ResumoCategoriaMensal newInstance(int mes, int ano, int nr) {
        ResumoCategoriaMensal fragment = new ResumoCategoriaMensal();
        Bundle args = new Bundle();
        args.putInt(ANO_PAGINA, ano);
        args.putInt(MES_PAGINA, mes);
        args.putInt(NR_PAGINA, nr);
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
        // COLOCA OS MESES NA TELA
        rootView = inflater.inflate(R.layout.resumo_por_categoria, container, false);
        Bundle args = getArguments();

        if (args != null) {
            mes = args.getInt(MES_PAGINA);
            ano = args.getInt(ANO_PAGINA);
            nrPagina = args.getInt(NR_PAGINA);
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

        NumberFormat dinheiro = NumberFormat.getCurrencyInstance();

        // INSERE OS VALORES EM CADA ITEtM

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
             //valorSaldo.setTextColor(Color.parseColor("#2B2B2B"));
            valorBanco.setTextColor(Color.parseColor("#669900"));
        }
    }

    private void Saldo() {

        // DEFINE OS NOMES DA LINHAS DA TABELA

        valores = new double[4];
        valoresDesp = new double[11];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];

        // PREENCHE AS LINHAS DA TABELA

        // VALORES DE RECEITAS
        valores[0] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1));

        // VALOR RECEITAS RECEBIDAS
        valoresRec[0] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR RECEITAS A RECEBAR
        valoresRec[1] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1).setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DE DESPESAS
        valores[1] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0));

        // VALOR CONTAS PAGAS
        valoresDesp[0] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR CONTAS A PAGAR
        valoresDesp[1] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALOR CARTAO DE CREDITO
        valoresDesp[2] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setClasse(0));

        // VALORES DAS CATEGORIAS DE DESPESAS
        for (int i = 0; i < 8; i++) {
            valoresDesp[i + 3] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setCategoria(String.valueOf(i)));
        }
        // VALOR DA CATEGORIA OUTROS
        valoresDesp[10] = valoresDesp[5] + valoresDesp[9] + valoresDesp[10];

        // VALORES DE APLICACOES
        valores[2] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(2));

        for (int j = 0; j < 3; j++) {
            valoresAplicados[j] = getSumForFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(2).setClasse(j));
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
        dados_mes.putInt("nr", nrPagina);
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
        mostraResumoLauncher.launch(mostra_resumo);
    }

    @Override
    public void onResume() {
        // CALCULA OS VALORES QUE SERAO EXIBIDOS
        Saldo();
        InsereValores();
        super.onResume();
    }
}
