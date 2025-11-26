package com.msk.minhascontas.resumos;

import android.content.Context; // Adicionado para checagem de contexto
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log; // Adicionado para logs de segurança
import java.text.NumberFormat;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import static com.msk.minhascontas.db.ContasContract.*;

// Estende a nova classe base
public class ResumoCategoriaDiario extends BaseResumoFragment {

    private static final String TAG = "ResumoCategoriaDiario"; // Tag para logs

    public static final String DIA_PAGINA = "dia_pagina";

    // Campos de View
    private TextView valorDesp, valorRec, valorAplic, valorSaldo, valorBanco,
            valorPagar, valorPago, valorCartao, valorSaldoAtual, valorSaldoAnterior,
            valorFundos, valorPoupanca, valorPrevidencia, vAlimentacao, vEducacao,
            vMoradia, vTransporte, vSaude, vOutros, valorReceber, valorRecebido;

    public ResumoCategoriaDiario() {
    }

    public static ResumoCategoriaDiario newInstance(int dia, int mes, int ano) {
        ResumoCategoriaDiario fragment = new ResumoCategoriaDiario();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        args.putInt("dia", dia);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        if (args != null) {
            // Acessando o valor correto usando a constante
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
        return R.layout.resumo_por_categoria;
    }

    @Override
    protected void initializeArrays() {
        valores = new double[4];
        // O array valoresDesp tem 10 posições (Pago, Pagar + 8 Classes: 0 a 7)
        valoresDesp = new double[10];
        valoresRec = new double[2];
        valoresSaldo = new double[2];
        valoresAplicados = new double[3];
    }

    @Override
    protected void iniciarViews(View view) {
        // Localiza as views
        valorPago = view.findViewById(R.id.valor_desp_paga);
        valorPagar = view.findViewById(R.id.valor_desp_pagar);
        valorBanco = view.findViewById(R.id.valor_banco);

        vAlimentacao = view.findViewById(R.id.valor_alimentacao);
        vEducacao = view.findViewById(R.id.valor_educacao);
        vMoradia = view.findViewById(R.id.valor_moradia);
        vTransporte = view.findViewById(R.id.valor_transporte);
        vSaude = view.findViewById(R.id.valor_saude);
        vOutros = view.findViewById(R.id.valor_outros);
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
        // Filtro Diário inclui dia, mês e ano
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

        // VALORES DAS CATEGORIAS DE DESPESAS (8 categorias: 0 a 7)
        // OBS: valoresDesp[2] até valoresDesp[10] serão usados para as 8 categorias.
        for (int i = 0; i < 10; i++) {
            // Filtra Despesas (TIPO_DESPESA) por Categoria (i)
            valoresDesp[i + 2] = getSumForFilter(new ContaFilter()
                    .setDia(dia).setMes(mes).setAno(ano).setTipo(TIPO_DESPESA).setCategoria(i));
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
            valores[3] = valoresRec[0] - valoresDesp[0] + valoresSaldo[1];
        }
    }

    /**
     * MÉTODO PROTEGIDO: Adicionada checagem robusta do ciclo de vida do Fragment.
     */
    @Override
    protected void insereValores() {
        // INÍCIO DA PROTEÇÃO CONTRA IllegalStateException
        Context context = getContext();
        if (context == null || !isAdded()) {
            Log.w(TAG, "insereValores abortado: Fragment não anexado.");
            return;
        }
        // FIM DA PROTEÇÃO

        NumberFormat dinheiro = getCurrencyFormat();

        // INSERE OS VALORES EM CADA ITEM
        valorPago.setText(dinheiro.format(valoresDesp[0]));
        valorPagar.setText(dinheiro.format(valoresDesp[1]));

        // Mapeamento das Categorias de Conta (valoresDesp[2] até [9])
        // Categoria 0: Alimentação
        vAlimentacao.setText(dinheiro.format(valoresDesp[2]));
        // Categoria 1: Educação
        vEducacao.setText(dinheiro.format(valoresDesp[3]));
        // Categoria 3: Moradia
        vMoradia.setText(dinheiro.format(valoresDesp[5]));
        // Categoria 4: Saúde
        vSaude.setText(dinheiro.format(valoresDesp[6]));
        // Categoria 5: Transporte
        vTransporte.setText(dinheiro.format(valoresDesp[7]));
        // Categoria 7: Outros
        // Coloca os valores de lazer (2) e vestuário (6) dentro de outros (7)
        vOutros.setText(dinheiro.format(valoresDesp[4] + valoresDesp[8] + valoresDesp[9]));

        // Outros campos fixos na UI
        valorCartao.setText(dinheiro.format(0.0D)); // Não calculamos por categoria aqui.
        valorBanco.setText(dinheiro.format(0.0D)); // Não calculamos por categoria aqui.

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
        valorDesp.setText(dinheiro.format(valores[1])); // Despesas Totais
        valorRec.setText(dinheiro.format(valores[0])); // Receitas Totais
        valorAplic.setText(dinheiro.format(valores[2])); // Aplicações Totais
        valorSaldo.setText(dinheiro.format(valores[3])); // Saldo Final

        if (valores[3] < 0) {
            valorSaldo.setTextColor(Color.RED);
        } else {
            valorSaldo.setTextColor(Color.BLACK);
        }
    }
}