package com.msk.minhascontas.resumos;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import static com.msk.minhascontas.db.ContasContract.*;

import java.text.NumberFormat;

// Estende a classe base
public class ResumoTipoDiario extends BaseResumoFragment {

    private static final String TAG = "ResumoTipoDiario";

    public static final String DIA_PAGINA = "dia_pagina";

    // Campos de View (TextViews)
    private TextView valorDesp, valorRec, valorAplic, valorSaldo,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorDespFixa, valorDespVar, valorPrestacoes,
            valorFundos, valorPoupanca, valorPrevidencia,
            valorReceber, valorRecebido;

    public ResumoTipoDiario() {
    }

    public static ResumoTipoDiario newInstance(int dia, int mes, int ano) {
        ResumoTipoDiario fragment = new ResumoTipoDiario();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        args.putInt(DIA_PAGINA, dia); // Usando a constante
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inicializa dia, mes e ano a partir dos argumentos
        Bundle args = getArguments();
        if (args != null) {
            dia = args.getInt(DIA_PAGINA, 0);
            mes = args.getInt("mes", 0);
            ano = args.getInt("ano", 0);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDadosAtualizados() {
        // Implementação vazia, mantida como no original
    }

    @Override
    protected int getLayoutResId() {
        // Assumindo que o layout é o mesmo do mensal, mas filtrado por dia
        return R.layout.resumo_por_tipo;
    }

    @Override
    protected void initializeArrays() {
        valores = new double[4];
        valoresDesp = new double[6];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];
    }

    @Override
    protected void iniciarViews(View view) {
        // Inicializa as TextViews
        valorPago = view.findViewById(R.id.valor_desp_paga);
        valorPagar = view.findViewById(R.id.valor_desp_pagar);
        valorDespFixa = view.findViewById(R.id.valor_desp_fixa);
        valorDespVar = view.findViewById(R.id.valor_desp_var);
        valorPrestacoes = view.findViewById(R.id.valor_prestacoes);
        valorCartao = view.findViewById(R.id.valor_cartao_credito);
        valorReceber = view.findViewById(R.id.valor_receber);
        valorRecebido = view.findViewById(R.id.valor_recebido);
        valorFundos = view.findViewById(R.id.valor_fundos);
        valorPoupanca = view.findViewById(R.id.valor_poupancas);
        valorPrevidencia = view.findViewById(R.id.valor_previdencias);
        valorSaldoAtual = view.findViewById(R.id.valor_saldo_atual);
        valorSaldoAnterior = view.findViewById(R.id.valor_saldo_anterior);
        valorDesp = view.findViewById(R.id.valor_despesas);
        valorRec = view.findViewById(R.id.valor_receitas);
        valorAplic = view.findViewById(R.id.valor_aplicacoes);
        valorSaldo = view.findViewById(R.id.valor_saldo);

        // Initialize MaterialCardView fields from BaseResumoFragment
        layoutAplicacoes = view.findViewById(R.id.resumo_aplicacoes);
        layoutDespesas = view.findViewById(R.id.resumo_despesas);
        layoutReceitas = view.findViewById(R.id.resumo_receitas);
        layoutSaldo = view.findViewById(R.id.resumo_saldo);
    }

    @Override
    protected ContaFilter getContaFilter() {
        // Filtro Diário: garante que o cálculo inclua o dia
        return new ContaFilter().setDia(dia).setMes(mes).setAno(ano);
    }

    /**
     * MÉTODO REFATORADO: Simplificada a lógica de Saldo Anterior usando getSumForFilter().
     */
    @Override
    protected void saldo() {

        // VALORES DE RECEITAS (Total) - TIPO_RECEITA
        valores[0] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_RECEITA));

        // VALOR RECEITAS RECEBIDAS (TIPO_RECEITA, Pago)
        valoresRec[0] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_RECEITA)
                .setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR RECEITAS A RECEBER (TIPO_RECEITA, Falta)
        valoresRec[1] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_RECEITA)
                .setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DE DESPESAS (Total) - TIPO_DESPESA
        valores[1] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_DESPESA));

        // VALOR CONTAS PAGAS (TIPO_DESPESA, Pago)
        valoresDesp[0] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_DESPESA)
                .setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR CONTAS A PAGAR (TIPO_DESPESA, Falta)
        valoresDesp[1] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_DESPESA)
                .setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DAS CLASSES DE DESPESAS (0 a 3)
        for (int i = 0; i < 4; i++) {
            valoresDesp[i + 2] = getSumForFilter(new ContaFilter()
                    .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_DESPESA).setClasse(i));
        }

        // VALORES DE APLICACOES (Total) - TIPO_APLICACAO
        valores[2] = getSumForFilter(new ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_APLICACAO));

        // VALORES DAS CLASSES DE APLICACOES (0 a 2)
        for (int j = 0; j < 3; j++) {
            valoresAplicados[j] = getSumForFilter(new ContaFilter()
                    .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_APLICACAO).setClasse(j));
        }

        // VALOR DO SALDO DIÁRIO (Receitas Totais - Despesas Totais do DIA)
        valoresSaldo[0] = valores[0] - valores[1];

        // --- Lógica de Saldo Anterior Diário (Usando Calendar) ---
        Calendar cal = Calendar.getInstance();
        // O mês é 1-based (Janeiro=1). Calendar.MONTH é 0-based.
        cal.set(ano, mes - 1, dia); // Define a data para o dia/mês/ano atual.
        cal.add(Calendar.DAY_OF_MONTH, -1); // Volta um dia.

        int dia_anterior = cal.get(Calendar.DAY_OF_MONTH);
        int mes_anterior = cal.get(Calendar.MONTH) + 1; // Converte de volta para 1-based.
        int ano_anterior = cal.get(Calendar.YEAR);
        // -----------------------------------------------------------------

        // 1. Calcula Receitas Totais do dia anterior
        double r = getSumForFilter(new ContaFilter().setDia(dia_anterior).setMes(mes_anterior).setAno(ano_anterior).setTipo(TIPO_RECEITA));

        // 2. Calcula Despesas Totais do dia anterior
        double d = getSumForFilter(new ContaFilter().setDia(dia_anterior).setMes(mes_anterior).setAno(ano_anterior).setTipo(TIPO_DESPESA));

        // 3. O saldo anterior é a diferença. (Apenas o saldo do dia anterior)
        valoresSaldo[1] = r - d;

        // VALOR DO SALDO ATUAL/DO DIA (Receitas Recebidas - Contas Pagas)
        valores[3] = valoresRec[0] - valoresDesp[0];

        // Soma Saldo Anterior (Opção de Preferências)
        boolean somaSaldo = buscaPreferencias.getBoolean("saldo", false);
        if (somaSaldo) {
            // Se a preferência estiver ativa, o Saldo Total passa a ser Saldo Atual + Saldo do Dia Anterior
            valores[3] = valoresRec[0] - valoresDesp[0] + valoresSaldo[1];
        }
    }

    /**
     * PONTO CRÍTICO: Inserção de valores na UI com proteção de ciclo de vida.
     */
    @Override
    protected void insereValores() {
        Context context = getContext();
        if (context == null || !isAdded()) {
            Log.w(TAG, "insereValores abortado: Fragment não anexado.");
            return;
        }

        NumberFormat dinheiro = getCurrencyFormat();

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
        if (valoresSaldo[0] < 0) {
            valorSaldoAtual.setTextColor(Color.RED);
        } else {
            valorSaldoAtual.setTextColor(Color.BLACK);
        }

        valorSaldoAnterior.setText(dinheiro.format(valoresSaldo[1]));
        valorDesp.setText(dinheiro.format(valores[1]));
        valorRec.setText(dinheiro.format(valores[0]));
        valorAplic.setText(dinheiro.format(valores[2]));
        valorSaldo.setText(dinheiro.format(valores[3]));

        if (valores[3] < 0) {
            valorSaldo.setTextColor(Color.RED);
        } else {
            valorSaldo.setTextColor(Color.BLACK);
        }
    }
}