package com.msk.minhascontas.graficos;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.ContasContract.Colunas;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.msk.minhascontas.db.DBContas.PAGAMENTO_FALTA;
import static com.msk.minhascontas.db.DBContas.PAGAMENTO_PAGO;

public class GraficoMensal extends Fragment {

    // GRAFICOS E DADOS
    private PieChart gcontas, greceitas, gpagamentos;
    private LineChart gsaldo;
    private BarChart gdespesas, gcategortias, gaplicacoes;

    // ELEMENTOS DA TELA
    private View rootView;
    private TextView semcontas;
    private LinearLayout grafcontas, grafdesp, grafcat, grafrec, grafpag, grafaplic, grafsaldo;

    // VARIAVEIS
    private DBContas dbContasFeitas;
    private int ano, mes, contas;
    private double vaplic, vdesp, vrec,
            vsaldo, vdesppg, vdespnpg, vrecarec, vrecrec;
    private float[] valores;
    private String[] series;
    private int[] roleta, cores;
    private NumberFormat dinheiro;


    public static GraficoMensal newInstance(int mes, int ano) {
        GraficoMensal fragment = new GraficoMensal();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbContasFeitas = DBContas.getInstance(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // COLOCA OS MESES NA TELA
        rootView = inflater.inflate(R.layout.graficos_do_mes, container, false);

        // Recupera o mes e o ano da lista anterior
        Bundle localBundle = getArguments();
        if (localBundle != null) {
            ano = localBundle.getInt("ano");
            mes = localBundle.getInt("mes");
        }


        Iniciar();
        Locale current = getResources().getConfiguration().getLocales().get(0);
        dinheiro = NumberFormat.getCurrencyInstance(current);
        Saldo();
        AtualizaGrafico();
        MostraGraficos();

        return rootView;
    }

    private void Iniciar() {

        gcontas = rootView.findViewById(R.id.grafico_contas);
        gdespesas = rootView.findViewById(R.id.grafico_despesas);
        gcategortias = rootView.findViewById(R.id.grafico_categorias);
        greceitas = rootView.findViewById(R.id.grafico_receitas);
        gpagamentos = rootView.findViewById(R.id.grafico_pagamentos);
        gaplicacoes = rootView.findViewById(R.id.grafico_aplicacoes);
        gsaldo = rootView.findViewById(R.id.grafico_saldo);

        grafcontas = rootView.findViewById(R.id.layout_grafico_contas);
        grafdesp = rootView.findViewById(R.id.layout_grafico_despesas);
        grafcat = rootView.findViewById(R.id.layout_grafico_categorias);
        grafaplic = rootView.findViewById(R.id.layout_grafico_aplicacoes);
        grafrec = rootView.findViewById(R.id.layout_grafico_receitas);
        grafpag = rootView.findViewById(R.id.layout_grafico_pagamentos);
        grafsaldo = rootView.findViewById(R.id.layout_grafico_saldo);

        semcontas = rootView.findViewById(R.id.tvSemGrafico);

        roleta = new int[]{Color.parseColor("#33B5E5"),
                Color.parseColor("#AA66CC"), Color.parseColor("#99CC00"),
                Color.parseColor("#FFBB33"), Color.parseColor("#FF4444"),
                Color.parseColor("#0099CC"), Color.parseColor("#9933CC"),
                Color.parseColor("#669900"), Color.parseColor("#FF8800"),
                Color.parseColor("#CC0000")};
    }

    private void Saldo() {
        ContaFilter baseFilter = new ContaFilter().setMes(mes).setAno(ano);

        try (Cursor cursor = dbContasFeitas.getContasByFilter(baseFilter, null)) {
            contas = cursor.getCount();
        }

        if (contas != 0)
            semcontas.setVisibility(View.GONE);
        else
            semcontas.setVisibility(View.VISIBLE);

        // DADOS DAS DESPESAS
        vdesp = 0.0D;
        // Valores de despesas
        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0), null)) {
            if (somador.getCount() > 0)
                vdesp = SomaContas(somador);
        }

        // DADOS DAS RECEITAS
        vrec = 0.0D;
        // Valores de receitas
        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1), null)) {
            if (somador.getCount() > 0)
                vrec = SomaContas(somador);
        }

        // DADOS DAS APLICACOES
        vaplic = 0.0D;
        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(2), null)) {
            if (somador.getCount() > 0)
                vaplic = SomaContas(somador);
        }

        vsaldo = (vrec - vdesp);

        vdesppg = 0.0D;
        vdespnpg = 0.0D;

        // Valores de despesas pagas
        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setPagamento(PAGAMENTO_PAGO), null)) {
            if (somador.getCount() > 0)
                vdesppg = SomaContas(somador);
        }

        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setPagamento(PAGAMENTO_FALTA), null)) {
            if (somador.getCount() > 0)
                vdespnpg = SomaContas(somador);
        }

        vrecrec = 0.0D;
        vrecarec = 0.0D;
        // Valores de receitas recebidas
        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1).setPagamento(PAGAMENTO_PAGO), null)) {
            if (somador.getCount() > 0)
                vrecrec = SomaContas(somador);
        }

        try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1).setPagamento(PAGAMENTO_FALTA), null)) {
            if (somador.getCount() > 0)
                vrecarec = SomaContas(somador);
        }
    }

    private double SomaContas(Cursor cursor) {
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        int valorColumnIndex = cursor.getColumnIndex(Colunas.COLUNA_VALOR_CONTA);
        if (valorColumnIndex == -1) { // Handle case where column might not be found
            return d;
        }

        for (int j = 0; ; j++) {
            if (j >= i) {
                return d;
            }
            d += cursor.getDouble(valorColumnIndex);
            cursor.moveToPrevious();
        }
    }

    private void MostraGraficos() {

        if (vdesp + vrec + vaplic == 0 || contas == 0) {
            grafcontas.setVisibility(View.GONE);
            grafsaldo.setVisibility(View.GONE);
        } else {
            grafcontas.setVisibility(View.VISIBLE);
            grafsaldo.setVisibility(View.VISIBLE);
        }
        if (vdesp == 0 || contas == 0) {
            grafdesp.setVisibility(View.GONE);
            grafcat.setVisibility(View.GONE);
            grafpag.setVisibility(View.GONE);
        } else {
            grafdesp.setVisibility(View.VISIBLE);
            grafcat.setVisibility(View.VISIBLE);
            grafpag.setVisibility(View.VISIBLE);
        }

        if (vrec == 0 || contas == 0)
            grafrec.setVisibility(View.GONE);
        else
            grafrec.setVisibility(View.VISIBLE);

        if (vaplic == 0 || contas == 0)
            grafaplic.setVisibility(View.GONE);
        else
            grafaplic.setVisibility(View.VISIBLE);
    }

    private void AtualizaGrafico() {
        GraficoContas();
        GraficoDespesas();
        GraficoCategorias();
        GraficoAplicacoes();
        GraficoPagamentos();
        GraficoReceitas();
        GraficoSaldo();
    }

    private void GraficoContas() {
        // Valores e cores do Grafico
        valores = new float[]{(float) vdesp, (float) vrec, (float) vaplic};
        series = getResources().getStringArray(R.array.GraficoContas);
        cores = new int[]{Color.parseColor("#FF4444"),
                Color.parseColor("#33B5E5"), Color.parseColor("#99CC00")};

        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            entries.add(new PieEntry(valores[i], series[i]));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);

        PieData data = new PieData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gcontas.setData(data);
        gcontas.setCenterText(this.getString(R.string.resumo_saldo) + "\n" + dinheiro.format(vsaldo));
        gcontas.setCenterTextSize(18f);
        gcontas.setCenterTextColor(Color.parseColor("#696969"));
        gcontas.invalidate();
    }

    private void GraficoAplicacoes() {

        String[] aplicacoes = getResources().getStringArray(R.array.TipoAplicacao);

        valores = new float[aplicacoes.length];
        series = new String[aplicacoes.length];
        cores = new int[aplicacoes.length];
        roleta = new int[]{Color.parseColor("#33B5E5"),
                Color.parseColor("#AA66CC"), Color.parseColor("#99CC00"),
                Color.parseColor("#FFBB33"), Color.parseColor("#FF4444"),
                Color.parseColor("#0099CC"), Color.parseColor("#9933CC"),
                Color.parseColor("#669900"), Color.parseColor("#FF8800"),
                Color.parseColor("#CC0000")};

        for (int i = 0; i < aplicacoes.length; i++) {
            series[i] = aplicacoes[i];
            if (i > 9) {
                cores[i] = roleta[i - 9];
            } else {
                cores[i] = roleta[i];
            }

            try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(2).setClasse(i), null)) {
                if (somador.getCount() > 0)
                    valores[i] = (float) SomaContas(somador);
                else
                    valores[i] = (float) 0.0D;
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < aplicacoes.length; ++i) {
            entries.add(new BarEntry(i, valores[i]));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.BLACK);

        BarData data = new BarData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gaplicacoes.setData(data);
        gaplicacoes.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < series.length) {
                    return series[index];
                }
                return ""; // Return empty string or a default if out of bounds
            }
        });
        gaplicacoes.invalidate();
    }

    private void GraficoDespesas() {

        String[] despesas = getResources().getStringArray(R.array.TipoDespesa);

        valores = new float[despesas.length];
        series = new String[despesas.length];
        cores = new int[despesas.length];
        roleta = new int[]{Color.parseColor("#FFBB33"),
                Color.parseColor("#FF4444"), Color.parseColor("#AA66CC"),
                Color.parseColor("#FF8800"), Color.parseColor("#9933CC"),
                Color.parseColor("#CC0000"), Color.parseColor("#0099CC"),
                Color.parseColor("#669900"), Color.parseColor("#33B5E5"),
                Color.parseColor("#99CC00")};

        for (int i = 0; i < despesas.length; i++) {
            series[i] = despesas[i];
            if (i > 9) {
                cores[i] = roleta[i - 9];
            } else {
                cores[i] = roleta[i];
            }
            try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0).setClasse(i), null)) {
                if (somador.getCount() > 0)
                    valores[i] = (float) SomaContas(somador);
                else
                    valores[i] = (float) 0.0D;
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < despesas.length; ++i) {
            entries.add(new BarEntry(i, valores[i]));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.BLACK);

        BarData data = new BarData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gdespesas.setData(data);
        gdespesas.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < series.length) {
                    return series[index];
                }
                return "";
            }
        });
        gdespesas.invalidate();
    }

    private void GraficoCategorias() {

        String[] categorias = getResources().getStringArray(R.array.CategoriaConta);

        valores = new float[categorias.length];
        series = new String[categorias.length];
        cores = new int[categorias.length];
        roleta = new int[]{Color.parseColor("#FF4444"),
                Color.parseColor("#AA66CC"), Color.parseColor("#FFBB33"),
                Color.parseColor("#0099CC"), Color.parseColor("#99CC00"),
                Color.parseColor("#FF8800"), Color.parseColor("#9E9D24"),
                Color.parseColor("#696969")};

        for (int i = 0; i < categorias.length; i++) {
            series[i] = categorias[i];
            if (i > 9) {
                cores[i] = roleta[i - 9];
            } else {
                cores[i] = roleta[i];
            }
            try (Cursor somador = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setCategoria(i), null)) {
                if (somador.getCount() > 0)
                    valores[i] = (float) SomaContas(somador);
                else
                    valores[i] = (float) 0.0D;
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < categorias.length; ++i) {
            entries.add(new BarEntry(i, valores[i]));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.BLACK);

        BarData data = new BarData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gcategortias.setData(data);
        gcategortias.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < series.length) {
                    return series[index];
                }
                return ""; // Return empty string or a default if out of bounds
            }
        });
        gcategortias.invalidate();
    }

    private void GraficoPagamentos() {

        valores = new float[]{(float) vdespnpg, (float) vdesppg};
        series = getResources().getStringArray(R.array.GraficoPagamentos);
        cores = new int[]{Color.parseColor("#FF4444"),
                Color.parseColor("#FFBB33")};

        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            entries.add(new PieEntry(valores[i], series[i]));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);

        PieData data = new PieData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gpagamentos.setData(data);
        gpagamentos.setCenterText(this.getString(R.string.linha_despesa) + "\n" + dinheiro.format(vdesp));
        gpagamentos.setCenterTextSize(18f);
        gpagamentos.setCenterTextColor(Color.parseColor("#696969"));
        gpagamentos.invalidate();
    }

    private void GraficoReceitas() {

        valores = new float[]{(float) vrecrec, (float) vrecarec};
        series = new String[]{getResources().getString(R.string.resumo_recebidas),
                getResources().getString(R.string.resumo_areceber)};
        cores = new int[]{ContextCompat.getColor(requireContext(), R.color.azul),
                ContextCompat.getColor(requireContext(), R.color.roxo)};

        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            entries.add(new PieEntry(valores[i], series[i]));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(cores);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);

        PieData data = new PieData(set);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        greceitas.setData(data);
        greceitas.setCenterText(this.getString(R.string.linha_receita) + "\n" + dinheiro.format(vrec));
        greceitas.setCenterTextSize(18f);
        greceitas.setCenterTextColor(Color.parseColor("#696969"));
        greceitas.invalidate();
    }

    private void GraficoSaldo() {

        List<Entry> valuesPos = new ArrayList<>();
        List<Entry> valuesNeg = new ArrayList<>();
        List<Entry> values = new ArrayList<>();

        try (Cursor somaReceitas = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(1), Colunas.COLUNA_DIA_DATA_CONTA + " ASC");
             Cursor somaDespesas = dbContasFeitas.getContasByFilter(new ContaFilter().setMes(mes).setAno(ano).setTipo(0), Colunas.COLUNA_DIA_DATA_CONTA + " ASC")) {

            int diaColumnIndexReceitas = somaReceitas.getColumnIndex(Colunas.COLUNA_DIA_DATA_CONTA);
            int valorColumnIndexReceitas = somaReceitas.getColumnIndex(Colunas.COLUNA_VALOR_CONTA);
            int diaColumnIndexDespesas = somaDespesas.getColumnIndex(Colunas.COLUNA_DIA_DATA_CONTA);
            int valorColumnIndexDespesas = somaDespesas.getColumnIndex(Colunas.COLUNA_VALOR_CONTA);

            // GERADOR DE DADOS PARA O GRAFICO DE SALDO
            if ((somaDespesas.getCount() > 0 && diaColumnIndexDespesas != -1 && valorColumnIndexDespesas != -1) ||
                (somaReceitas.getCount() > 0 && diaColumnIndexReceitas != -1 && valorColumnIndexReceitas != -1)) {
                vsaldo = 0.0D;
                for (int i = 1; i < 32; i++) {
                    vrec = 0.0D;
                    somaReceitas.moveToFirst();
                    while (!somaReceitas.isAfterLast()) {
                        if (diaColumnIndexReceitas != -1 && somaReceitas.getInt(diaColumnIndexReceitas) < i + 1)
                            vrec = vrec + somaReceitas.getDouble(valorColumnIndexReceitas);
                        somaReceitas.moveToNext();
                    }
                    vdesp = 0.0D;
                    somaDespesas.moveToFirst();
                    while (!somaDespesas.isAfterLast()) {
                        if (diaColumnIndexDespesas != -1 && somaDespesas.getInt(diaColumnIndexDespesas) < i + 1)
                            vdesp = vdesp + somaDespesas.getDouble(valorColumnIndexDespesas);
                        somaDespesas.moveToNext();
                    }
                    vsaldo = (vrec - vdesp);
                    values.add(new Entry(i, (float) vsaldo));

                    if (vsaldo < 0.0D) {
                        valuesNeg.add(new Entry(i, (float) vsaldo));
                    } else {
                        valuesPos.add(new Entry(i, (float) vsaldo));
                    }
                }
            }
        }

        //Linhas do grafico
        LineDataSet set1 = new LineDataSet(values, "");
        set1.setColor(ContextCompat.getColor(requireContext(), R.color.cinza_claro));
        set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setDrawFilled(true);
        set1.setFillColor(ContextCompat.getColor(requireContext(), R.color.cinza_claro));

        LineDataSet set2 = new LineDataSet(valuesPos, "");
        set2.setColor(ContextCompat.getColor(requireContext(), R.color.verde));
        set2.setDrawCircles(true);
        set2.setCircleColor(ContextCompat.getColor(requireContext(), R.color.verde));

        LineDataSet set3 = new LineDataSet(valuesNeg, "");
        set3.setColor(ContextCompat.getColor(requireContext(), R.color.vermelho_claro));
        set3.setDrawCircles(true);
        set3.setCircleColor(ContextCompat.getColor(requireContext(), R.color.vermelho_claro));

        LineData data = new LineData(set1, set2, set3);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return dinheiro.format(value);
            }
        });

        gsaldo.setData(data);
        gsaldo.invalidate();
    }

    @Override
    public void onResume() {
        Saldo();
        AtualizaGrafico();
        MostraGraficos();
        super.onResume();
    }
}