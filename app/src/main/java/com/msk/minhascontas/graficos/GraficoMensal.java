package com.msk.minhascontas.graficos;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PieChartView;

public class GraficoMensal extends Fragment {

    // GRAFICOS E DADOS
    private List<SliceValue> values;
    private SliceValue arcValue;
    private PieChartView gcontas, greceitas, gpagamentos;
    private PieChartData data;
    private LineChartView gsaldo;
    private ColumnChartView gdespesas, gcategortias, gaplicacoes;
    private ColumnChartData dados;

    // ELEMENTOS DA TELA
    private View rootView;
    private TextView semcontas;
    private LinearLayout grafcontas, grafdesp, grafcat, grafrec, grafpag, grafaplic, grafsaldo;

    // VARIAVEIS
    private DBContas dbContasFeitas;
    private String[] MESES;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dbContasFeitas = new DBContas(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // COLOCA OS MESES NA TELA
        rootView = inflater.inflate(R.layout.graficos_do_mes, container, false);

        // Recupera o mes e o ano da lista anterior
        Bundle localBundle = getArguments();
        ano = localBundle.getInt("ano");
        mes = localBundle.getInt("mes");

        Iniciar();
        Locale current = getResources().getConfiguration().locale;
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

        values = new ArrayList<SliceValue>();
        roleta = new int[]{Color.parseColor("#33B5E5"),
                Color.parseColor("#AA66CC"), Color.parseColor("#99CC00"),
                Color.parseColor("#FFBB33"), Color.parseColor("#FF4444"),
                Color.parseColor("#0099CC"), Color.parseColor("#9933CC"),
                Color.parseColor("#669900"), Color.parseColor("#FF8800"),
                Color.parseColor("#CC0000")};
    }

    private void Saldo() {
        dbContasFeitas.open();
        Cursor somador = null;

        contas = dbContasFeitas.quantasContasPorMes(mes, ano);
        if (contas != 0)
            semcontas.setVisibility(View.GONE);
        else
            semcontas.setVisibility(View.VISIBLE);

        // DADOS DAS DESPESAS
        vdesp = 0.0D;
        // Valores de despesas
        somador = dbContasFeitas.buscaContasTipo(0, mes, ano, null, 0);
        if (somador.getCount() > 0)
            vdesp = SomaContas(somador);

        // DADOS DAS RECEITAS
        vrec = 0.0D;
        // Valores de receitas
        somador = dbContasFeitas.buscaContasTipo(0, mes, ano, null, 1);
        if (somador.getCount() > 0)
            vrec = SomaContas(somador);

        // DADOS DAS APLICACOES
        vaplic = 0.0D;
        somador = dbContasFeitas.buscaContasTipo(0, mes, ano, null, 2);
        if (somador.getCount() > 0)
            vaplic = SomaContas(somador);

        vsaldo = (vrec - vdesp);

        vdesppg = 0.0D;
        vdespnpg = 0.0D;

        // Valores de despesas pagas
        somador = dbContasFeitas.buscaContasTipoPagamento(0, mes, ano, null, 0, "paguei");
        if (somador.getCount() > 0)
            vdesppg = SomaContas(somador);

        somador = dbContasFeitas.buscaContasTipoPagamento(0, mes, ano, null, 0, "falta");
        if (somador.getCount() > 0)
            vdespnpg = SomaContas(somador);

        vrecrec = 0.0D;
        vrecarec = 0.0D;
        // Valores de receitas recebidas
        somador = dbContasFeitas.buscaContasTipoPagamento(0, mes, ano, null, 1, "paguei");
        if (somador.getCount() > 0)
            vrecrec = SomaContas(somador);

        somador = dbContasFeitas.buscaContasTipoPagamento(0, mes, ano, null, 1, "falta");
        if (somador.getCount() > 0)
            vrecarec = SomaContas(somador);

        somador.close();
        dbContasFeitas.close();
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
            d += cursor.getDouble(8);
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

        values = new ArrayList<SliceValue>();
        for (int i = 0; i < 3; i++) {
            arcValue = new SliceValue(valores[i], cores[i]);
            arcValue.setLabel(dinheiro.format(valores[i]));
            arcValue.setSliceSpacing(5);
            values.add(arcValue);
        }

        data = new PieChartData(values);

        data.setHasLabels(true);
        data.setHasLabelsOnlyForSelected(false);
        data.setHasLabelsOutside(false);
        data.setHasCenterCircle(true);

        data.setCenterText1(this.getString(R.string.resumo_saldo));
        data.setCenterText1FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 38));
        data.setCenterText1Color(Color.parseColor("#696969"));

        data.setCenterText2(dinheiro.format(vsaldo));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));
        data.setCenterText2Color(Color.parseColor("#696969"));

        gcontas.setPieChartData(data);
    }

    private void GraficoAplicacoes() {

        dbContasFeitas.open();
        Cursor somador = null;
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

            somador = dbContasFeitas.buscaContasClasse(0, mes, ano, null, 2, i);
            if (somador.getCount() > 0)
                valores[i] = (float) SomaContas(somador);
            else
                valores[i] = (float) 0.0D;
        }

        somador.close();
        dbContasFeitas.close();

        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> valorColuna;

        for (int i = 0; i < aplicacoes.length; ++i) {
            valorColuna = new ArrayList<SubcolumnValue>();
            SubcolumnValue sc;
            for (int j = 0; j < 1; ++j) {
                sc = new SubcolumnValue(valores[i], cores[i]);
                sc.setLabel(dinheiro.format(valores[i]));
                valorColuna.add(sc);
            }
            Column column = new Column(valorColuna);
            column.setHasLabels(true);
            columns.add(column);
        }

        dados = new ColumnChartData(columns);

        Axis axisY = new Axis().setHasLines(true);
        axisY.setName(getResources().getString(R.string.dica_valor)).setMaxLabelChars(6);
        axisY.setTextColor(getResources().getColor(R.color.cinza));
        axisY.setAutoGenerated(true);
        dados.setAxisYLeft(axisY);

        gaplicacoes.setColumnChartData(dados);
    }

    private void GraficoDespesas() {

        dbContasFeitas.open();
        Cursor somador = null;
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
            somador = dbContasFeitas.buscaContasClasse(0, mes, ano, null, 0, i);
            if (somador.getCount() > 0)
                valores[i] = (float) SomaContas(somador);
            else
                valores[i] = (float) 0.0D;
        }

        somador.close();
        dbContasFeitas.close();

        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> valorColuna;

        for (int i = 0; i < despesas.length; ++i) {
            valorColuna = new ArrayList<SubcolumnValue>();
            SubcolumnValue sc;
            for (int j = 0; j < 1; ++j) {
                sc = new SubcolumnValue(valores[i], cores[i]);
                sc.setLabel(dinheiro.format(valores[i]));
                valorColuna.add(sc);
            }
            Column column = new Column(valorColuna);
            column.setHasLabels(true);
            columns.add(column);
        }

        dados = new ColumnChartData(columns);

        Axis axisY = new Axis().setHasLines(true);
        axisY.setName(getResources().getString(R.string.dica_valor)).setMaxLabelChars(4);
        axisY.setTextColor(getResources().getColor(R.color.cinza));
        axisY.setAutoGenerated(true);
        dados.setAxisYLeft(axisY);

        gdespesas.setColumnChartData(dados);
    }

    private void GraficoCategorias() {

        dbContasFeitas.open();
        Cursor somador = null;
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
            somador = dbContasFeitas.buscaContasCategoria(0, mes, ano, null, i);
            if (somador.getCount() > 0)
                valores[i] = (float) SomaContas(somador);
            else
                valores[i] = (float) 0.0D;
        }

        somador.close();
        dbContasFeitas.close();

        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> valorColuna;

        for (int i = 0; i < categorias.length; ++i) {
            valorColuna = new ArrayList<SubcolumnValue>();
            SubcolumnValue sc;
            for (int j = 0; j < 1; ++j) {
                sc = new SubcolumnValue(valores[i], cores[i]);
                sc.setLabel(dinheiro.format(valores[i]));
                valorColuna.add(sc);
            }
            Column column = new Column(valorColuna);
            column.setHasLabels(true);
            columns.add(column);
        }

        dados = new ColumnChartData(columns);

        Axis axisY = new Axis().setHasLines(true);
        axisY.setName(getResources().getString(R.string.dica_valor)).setMaxLabelChars(4);
        axisY.setTextColor(getResources().getColor(R.color.cinza));
        axisY.setAutoGenerated(true);
        dados.setAxisYLeft(axisY);

        gcategortias.setColumnChartData(dados);
    }

    private void GraficoPagamentos() {

        valores = new float[]{(float) vdespnpg, (float) vdesppg};
        series = getResources().getStringArray(R.array.GraficoPagamentos);
        cores = new int[]{Color.parseColor("#FF4444"),
                Color.parseColor("#FFBB33")};

        values = new ArrayList<SliceValue>();

        arcValue = new SliceValue(valores[0], cores[0]);
        arcValue.setLabel(dinheiro.format(valores[0]));
        arcValue.setSliceSpacing(5);
        values.add(arcValue);

        arcValue = new SliceValue(valores[1], cores[1]);
        arcValue.setLabel(dinheiro.format(valores[1]));
        arcValue.setSliceSpacing(5);
        values.add(arcValue);

        data = new PieChartData(values);

        data.setHasLabels(true);
        data.setHasLabelsOnlyForSelected(false);
        data.setHasLabelsOutside(false);
        data.setHasCenterCircle(true);

        data.setCenterText1(this.getString(R.string.linha_despesa));
        data.setCenterText1FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 38));
        data.setCenterText1Color(Color.parseColor("#696969"));

        data.setCenterText2(dinheiro.format(vdesp));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));
        data.setCenterText2Color(Color.parseColor("#696969"));

        gpagamentos.setPieChartData(data);
    }

    private void GraficoReceitas() {

        valores = new float[]{(float) vrecrec, (float) vrecarec};
        series = new String[]{getResources().getString(R.string.resumo_recebidas),
                getResources().getString(R.string.resumo_areceber)};
        cores = new int[]{getResources().getColor(R.color.azul),
                getResources().getColor(R.color.roxo)};

        values = new ArrayList<SliceValue>();

        arcValue = new SliceValue(valores[0], cores[0]);
        arcValue.setLabel(dinheiro.format(valores[0]));
        arcValue.setSliceSpacing(5);
        values.add(arcValue);

        arcValue = new SliceValue(valores[1], cores[1]);
        arcValue.setLabel(dinheiro.format(valores[1]));
        arcValue.setSliceSpacing(5);
        values.add(arcValue);

        data = new PieChartData(values);

        data.setHasLabels(true);
        data.setHasLabelsOnlyForSelected(false);
        data.setHasLabelsOutside(false);
        data.setHasCenterCircle(true);

        data.setCenterText1(this.getString(R.string.linha_receita));
        data.setCenterText1FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 38));
        data.setCenterText1Color(Color.parseColor("#696969"));

        data.setCenterText2(dinheiro.format(vrec));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));
        data.setCenterText2Color(Color.parseColor("#696969"));

        greceitas.setPieChartData(data);
    }

    private void GraficoSaldo() {

        List<PointValue> valuesPos = new ArrayList<PointValue>();
        List<PointValue> valuesNeg = new ArrayList<PointValue>();
        List<PointValue> values = new ArrayList<PointValue>();

        dbContasFeitas.open();
        Cursor somaReceitas = dbContasFeitas.buscaContasTipo(0, mes, ano, "dia_data ASC", 1);
        Cursor somaDespesas = dbContasFeitas.buscaContasTipo(0, mes, ano, "dia_data ASC", 0);

        // GERADOR DE DADOS PARA O GRAFICO DE SALDO
        if (somaDespesas.getCount() > 0 || somaReceitas.getCount() > 0) {
            vsaldo = 0.0D;
            for (int i = 1; i < 32; i++) {
                vrec = 0.0D;
                somaReceitas.moveToFirst();
                while (!somaReceitas.isAfterLast()) {
                    if (somaReceitas.getInt(5) < i + 1)
                        vrec = vrec + somaReceitas.getDouble(8);
                    somaReceitas.moveToNext();
                }
                vdesp = 0.0D;
                somaDespesas.moveToFirst();
                while (!somaDespesas.isAfterLast()) {
                    if (somaDespesas.getInt(5) < i + 1)
                        vdesp = vdesp + somaDespesas.getDouble(8);
                    somaDespesas.moveToNext();
                }
                vsaldo = (vrec - vdesp);
                values.add(new PointValue(i, (float) vsaldo));

                if (vsaldo < 0.0D) {
                    valuesNeg.add(new PointValue(i, (float) vsaldo));
                } else {
                    valuesPos.add(new PointValue(i, (float) vsaldo));
                }
            }
        }

        somaReceitas.close();
        somaDespesas.close();
        dbContasFeitas.close();

        //Linhas do grafico
        List<Line> lines = new ArrayList<Line>();
        Line line = new Line(values).setColor(getResources().getColor(R.color.cinza_claro)).setCubic(true);
        line.setHasPoints(false);
        line.setFilled(true);
        line.setStrokeWidth(2);
        lines.add(line);

        line = new Line(valuesPos).setColor(getResources().getColor(R.color.verde)).setCubic(true);
        line.setHasLines(false);
        line.setHasPoints(true);
        lines.add(line);

        line = new Line(valuesNeg).setColor(getResources().getColor(R.color.vermelho_claro)).setCubic(true);
        line.setHasLines(false);
        line.setHasPoints(true);
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);
        Axis axis = new Axis().setHasLines(true);
        axis.setName(getResources().getString(R.string.dica_valor)).setMaxLabelChars(5);
        axis.setTextColor(getResources().getColor(R.color.cinza));
        axis.setAutoGenerated(true);
        data.setAxisYLeft(axis);

        axis = new Axis().setHasLines(false);
        axis.setName(getResources().getString(R.string.dica_vencimento));
        axis.setTextColor(getResources().getColor(R.color.cinza));
        axis.setAutoGenerated(true);
        data.setAxisXBottom(axis);

        gsaldo.setLineChartData(data);
    }

    @Override
    public void onResume() {
        Saldo();
        AtualizaGrafico();
        MostraGraficos();
        super.onResume();
    }
}
