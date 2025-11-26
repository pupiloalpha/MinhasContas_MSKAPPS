package com.msk.minhascontas.resumos;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import java.util.Calendar;
import android.content.Context; // Importação adicionada para getContext()

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Adicionado para anotações de segurança

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import static com.msk.minhascontas.db.ContasContract.*;

import java.text.NumberFormat;
import java.util.Locale;

// Estende a classe base
public class ResumoTipoMensal extends BaseResumoFragment {

    private static final String TAG = "ResumoTipoMensal"; // Tag para logs

    // Campos de View (TextViews)
    private TextView valorDesp, valorRec, valorAplic, valorSaldo,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorDespFixa, valorDespVar, valorPrestacoes,
            valorFundos, valorPoupanca, valorPrevidencia,
            valorReceber, valorRecebido;

    public ResumoTipoMensal() {
    }

    public static ResumoTipoMensal newInstance(int nrPagina, int mes, int ano) {
        ResumoTipoMensal fragment = new ResumoTipoMensal();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        args.putInt("nr_pagina", nrPagina);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inicializa mes, ano e nrPagina a partir dos argumentos
        Bundle args = getArguments();
        if (args != null) {
            mes = args.getInt("mes", 0);
            ano = args.getInt("ano", 0);
            // nrPagina = args.getInt("nr_pagina", 0); // Não usado diretamente em getContaFilter para o mensal
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDadosAtualizados() {
        // Implementação vazia, mantida como no original
    }

    @Override
    protected int getLayoutResId() {
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
        // Filtro Mensal: Filtra por mês e ano (dia é nulo)
        return new ContaFilter().setMes(mes).setAno(ano);
    }

    /**
     * MÉTODO REFATORADO: Simplificada a lógica de Saldo Anterior usando getSumForFilter().
     */
    @Override
    protected void saldo() {

        // VALORES DE RECEITAS (Total) - TIPO_RECEITA
        valores[0] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_RECEITA));

        // VALOR RECEITAS RECEBIDAS (TIPO_RECEITA, Pago)
        valoresRec[0] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_RECEITA)
                .setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR RECEITAS A RECEBER (TIPO_RECEITA, Falta)
        valoresRec[1] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_RECEITA)
                .setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DE DESPESAS (Total) - TIPO_DESPESA
        valores[1] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_DESPESA));

        // VALOR CONTAS PAGAS (TIPO_DESPESA, Pago)
        valoresDesp[0] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_DESPESA)
                .setPagamento(DBContas.PAGAMENTO_PAGO));

        // VALOR CONTAS A PAGAR (TIPO_DESPESA, Falta)
        valoresDesp[1] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_DESPESA)
                .setPagamento(DBContas.PAGAMENTO_FALTA));

        // VALORES DAS CLASSES DE DESPESAS (0 a 3)
        for (int i = 0; i < 4; i++) {
            valoresDesp[i + 2] = getSumForFilter(new ContaFilter()
                    .setMes(mes).setAno(ano).setTipo(TIPO_DESPESA).setClasse(i));
        }

        // VALORES DE APLICACOES (Total) - TIPO_APLICACAO
        valores[2] = getSumForFilter(new ContaFilter()
                .setMes(mes).setAno(ano).setTipo(TIPO_APLICACAO));

        // VALORES DAS CLASSES DE APLICACOES (0 a 2)
        for (int j = 0; j < 3; j++) {
            valoresAplicados[j] = getSumForFilter(new ContaFilter()
                    .setMes(mes).setAno(ano).setTipo(TIPO_APLICACAO).setClasse(j));
        }

        // VALOR DO SALDO MENSAL (Receitas Totais - Despesas Totais)
        valoresSaldo[0] = valores[0] - valores[1];

        // --- Lógica de Saldo Anterior (Usando Calendar) ---
        Calendar cal = Calendar.getInstance();
        // O mês armazenado (this.mes) é 1-based (Janeiro=1). Calendar.MONTH é 0-based.
        cal.set(ano, mes - 1, 1); // Define a data para o primeiro dia do mês/ano atual.
        cal.add(Calendar.MONTH, -1); // Volta um mês.

        int mes_anterior = cal.get(Calendar.MONTH) + 1; // Converte de volta para 1-based.
        int ano_anterior = cal.get(Calendar.YEAR);
        // -----------------------------------------------------------------

        // 1. Calcula Receitas Totais do mês anterior (TIPO_RECEITA)
        double r = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(TIPO_RECEITA));

        // 2. Calcula Despesas Totais do mês anterior (TIPO_DESPESA)
        double d = getSumForFilter(new ContaFilter().setMes(mes_anterior).setAno(ano_anterior).setTipo(TIPO_DESPESA));

        // 3. O saldo anterior é a diferença. (Apenas o saldo do mês anterior)
        valoresSaldo[1] = r - d;

        // VALOR DO SALDO ATUAL: (Receitas Recebidas - Contas Pagas)
        valores[3] = valoresRec[0] - valoresDesp[0];

        // Soma Saldo Anterior (Opção de Preferências)
        boolean somaSaldo = buscaPreferencias.getBoolean("saldo", false);
        if (somaSaldo) {
            valores[3] = valoresRec[0] - valoresDesp[0] + valoresSaldo[1];
        }
    }

    /**
     * PONTO CRÍTICO: Inserção de valores na UI com proteção de ciclo de vida.
     */
    @Override
    protected void insereValores() {
        // **INÍCIO DA PROTEÇÃO CONTRA IllegalStateException**
        Context context = getContext();
        if (context == null || !isAdded()) {
            Log.w(TAG, "insereValores abortado: Fragment não anexado.");
            return;
        }
        // **FIM DA PROTEÇÃO**

        NumberFormat dinheiro = getCurrencyFormat();

        // INSERE OS VALORES EM CADA ITEM
        valorPago.setText(dinheiro.format(valoresDesp[0]));
        valorPagar.setText(dinheiro.format(valoresDesp[1]));

        // Mapeamento das Classes de Conta (0-3) para as TextViews de TIPO
        valorCartao.setText(dinheiro.format(valoresDesp[2]));
        valorDespFixa.setText(dinheiro.format(valoresDesp[3]));
        valorDespVar.setText(dinheiro.format(valoresDesp[4]));
        valorPrestacoes.setText(dinheiro.format(valoresDesp[5]));

        // Receitas
        valorReceber.setText(dinheiro.format(valoresRec[1]));
        valorRecebido.setText(dinheiro.format(valoresRec[0]));

        // Aplicações
        valorFundos.setText(dinheiro.format(valoresAplicados[0]));
        valorPoupanca.setText(dinheiro.format(valoresAplicados[1]));
        valorPrevidencia.setText(dinheiro.format(valoresAplicados[2]));

        // Exibição dos totais e saldos
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