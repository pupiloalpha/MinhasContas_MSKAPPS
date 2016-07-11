package com.msk.minhascontas.graficos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;

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

public class GraficoMensalContas extends AppCompatActivity implements OnClickListener {

    // GRAFICOS E DADOS
    private List<SliceValue> values;
    private SliceValue arcValue;
    private PieChartView gcontas, greceitas, gpagamentos;
    private PieChartData data;
    private LineChartView gsaldo;
    private ColumnChartView gdespesas, gaplicacoes;
    private ColumnChartData dados;
    // ELEMENTOS DA TELA
    private TextView mesGraficos, semcontas;
    private ImageButton mesVolta, mesFrente, addConta;
    private LinearLayout grafcontas, grafdesp, grafrec, grafpag, grafaplic, grafsaldo;

    // VARIAVEIS
    private DBContas dbContasFeitas = new DBContas(this);
    private String[] MESES;
    private int ano, mes, contas;
    private double vaplic, vdesp, vrec,
            vsaldo, vdesppg, vdespnpg, vrecarec, vrecrec;
    private String despesa, receita, aplicacao;
    private float[] valores;
    private String[] series, despesas, aplicacoes;
    private int[] roleta, cores;
    private NumberFormat dinheiro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graficos_do_mes);
        Iniciar();
        usarActionBar();
        Locale current = getResources().getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);

        Bundle envelope = getIntent().getExtras();
        ano = envelope.getInt("ano");
        mes = envelope.getInt("mes");
        mesGraficos.setText(MESES[mes] + "/" + ano);

        Saldo();
        mesVolta.setOnClickListener(this);
        mesFrente.setOnClickListener(this);

        AtualizaGrafico();

        addConta.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.NOVACONTA"), 1);
            }
        });
    }

    private void Iniciar() {

        gcontas = (PieChartView) findViewById(R.id.grafico_contas);
        gdespesas = (ColumnChartView) findViewById(R.id.grafico_despesas);
        greceitas = (PieChartView) findViewById(R.id.grafico_receitas);
        gpagamentos = (PieChartView) findViewById(R.id.grafico_pagamentos);
        gaplicacoes = (ColumnChartView) findViewById(R.id.grafico_aplicacoes);
        gsaldo = (LineChartView) findViewById(R.id.grafico_saldo);

        grafcontas = (LinearLayout) findViewById(R.id.layout_grafico_contas);
        grafdesp = (LinearLayout) findViewById(R.id.layout_grafico_despesas);
        grafaplic = (LinearLayout) findViewById(R.id.layout_grafico_aplicacoes);
        grafrec = (LinearLayout) findViewById(R.id.layout_grafico_receitas);
        grafpag = (LinearLayout) findViewById(R.id.layout_grafico_pagamentos);
        grafsaldo = (LinearLayout) findViewById(R.id.layout_grafico_saldo);

        mesGraficos = (TextView) findViewById(R.id.tvMesAno);
        semcontas = (TextView) findViewById(R.id.tvSemGrafico);
        mesVolta = (ImageButton) findViewById(R.id.ibMesVolta);
        mesFrente = (ImageButton) findViewById(R.id.ibMesFrente);
        MESES = getResources().getStringArray(R.array.MesesDoAno);
        values = new ArrayList<SliceValue>();
        roleta = new int[]{Color.parseColor("#33B5E5"),
                Color.parseColor("#AA66CC"), Color.parseColor("#99CC00"),
                Color.parseColor("#FFBB33"), Color.parseColor("#FF4444"),
                Color.parseColor("#0099CC"), Color.parseColor("#9933CC"),
                Color.parseColor("#669900"), Color.parseColor("#FF8800"),
                Color.parseColor("#CC0000")};
        addConta = (ImageButton) findViewById(R.id.ibfab);

    }

    private void Saldo() {
        dbContasFeitas.open();

        contas = dbContasFeitas.quantasContasPorMes(mes, ano);

        if (contas != 0)
            semcontas.setVisibility(View.GONE);
        else
            semcontas.setVisibility(View.VISIBLE);

        // DADOS DAS DESPESAS
        despesa = getResources().getString(R.string.linha_despesa);
        despesas = getResources().getStringArray(R.array.TipoDespesa);
        vdesp = 0.0D;
        // Valores de despesas
        if (dbContasFeitas.quantasContasPorTipo(despesa, 0, mes, ano) > 0)
            vdesp = dbContasFeitas.somaContas(despesa, 0, mes, ano);

        // DADOS DAS RECEITAS
        receita = getResources().getString(R.string.linha_receita);
        vrec = 0.0D;
        // Valores de receitas
        if (dbContasFeitas.quantasContasPorTipo(receita, 0, mes, ano) > 0)
            vrec = dbContasFeitas.somaContas(receita, 0, mes, ano);

        // DADOS DAS APLICACOES
        aplicacao = getResources().getString(R.string.linha_aplicacoes);
        aplicacoes = getResources().getStringArray(R.array.TipoAplicacao);
        vaplic = 0.0D;
        if (dbContasFeitas.quantasContasPorTipo(aplicacao, 0, mes, ano) > 0)
            vaplic = dbContasFeitas.somaContas(aplicacao, 0, mes, ano);

        vsaldo = (vrec - vdesp);

        vdesppg = 0.0D;
        vdespnpg = 0.0D;
        // Valores de despesas pagas

        if (dbContasFeitas.quantasContasPagasPorTipo(despesa, "paguei", 0, mes,
                ano) > 0)
            vdesppg = dbContasFeitas.somaContasPagas(despesa, "paguei", 0, mes,
                    ano);

        if (dbContasFeitas.quantasContasPagasPorTipo(despesa, "falta", 0, mes,
                ano) > 0)
            vdespnpg = dbContasFeitas.somaContasPagas(despesa, "falta", 0, mes,
                    ano);

        vrecrec = 0.0D;
        vrecarec = 0.0D;
        // Valores de receitas recebidas

        if (dbContasFeitas.quantasContasPagasPorTipo(receita, "paguei", 0, mes,
                ano) > 0)
            vrecrec = dbContasFeitas.somaContasPagas(receita, "paguei", 0, mes,
                    ano);

        if (dbContasFeitas.quantasContasPagasPorTipo(receita, "falta", 0, mes,
                ano) > 0)
            vrecarec = dbContasFeitas.somaContasPagas(receita, "falta", 0, mes,
                    ano);

        dbContasFeitas.close();

    }

    @Override
    public void onClick(View graphView) {
        switch (graphView.getId()) {

            case R.id.ibMesVolta:
                mes = (-1 + mes);
                if (mes < 0) {
                    mes = 11;
                    ano = (-1 + ano);
                }
                mesGraficos.setText(MESES[mes] + "/" + ano);
                Saldo();
                break;
            case R.id.ibMesFrente:
                mes = (1 + mes);
                if (mes > 11) {
                    mes = 0;
                    ano = (1 + ano);
                }
                mesGraficos.setText(MESES[mes] + "/" + ano);
                Saldo();
                break;
        }
        AtualizaGrafico();
        MostraGraficos();
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
            grafpag.setVisibility(View.GONE);
        } else {
            grafdesp.setVisibility(View.VISIBLE);
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
                .getDisplayMetrics().scaledDensity, 36));

        data.setCenterText2(dinheiro.format(vsaldo));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));

        gcontas.setPieChartData(data);
    }

    private void GraficoAplicacoes() {

        dbContasFeitas.open();
        aplicacoes = getResources().getStringArray(R.array.TipoAplicacao);

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

            if (dbContasFeitas.quantasContasPorClasse(aplicacoes[i],
                    0, mes, ano) > 0)
                valores[i] = (float) dbContasFeitas.somaContasPorClasse(
                        aplicacoes[i], 0, mes, ano);
            else
                valores[i] = (float) 0.0D;
        }

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
        despesas = getResources().getStringArray(R.array.TipoDespesa);

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
            if (dbContasFeitas.quantasContasPorClasse(despesas[i], 0,
                    mes, ano) > 0)
                valores[i] = (float) dbContasFeitas.somaContasPorClasse(
                        despesas[i], 0, mes, ano);
            else
                valores[i] = (float) 0.0D;
        }

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
                .getDisplayMetrics().scaledDensity, 36));

        data.setCenterText2(dinheiro.format(vdesp));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));

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
                .getDisplayMetrics().scaledDensity, 36));

        data.setCenterText2(dinheiro.format(vrec));
        data.setCenterText2FontSize(ChartUtils.px2sp(getResources()
                .getDisplayMetrics().scaledDensity, 42));

        greceitas.setPieChartData(data);
    }

    private void GraficoSaldo() {

        List<PointValue> valuesPos = new ArrayList<PointValue>();
        List<PointValue> valuesNeg = new ArrayList<PointValue>();
        List<PointValue> values = new ArrayList<PointValue>();
        dbContasFeitas.open();
        for (int i = 1; i < 32; i++) {
            // Valores de despesas
            if (dbContasFeitas.quantasContasPorTipo(despesa, i, mes, ano) > 0)
                vdesp = dbContasFeitas.somaContas(despesa, i, mes, ano);
            else
                vdesp = 0.0D;
            // Valores de receitas
            if (dbContasFeitas.quantasContasPorTipo(receita, i, mes, ano) > 0)
                vrec = dbContasFeitas.somaContas(receita, i, mes, ano);
            else
                vrec = 0.0D;
            vsaldo = (vrec - vdesp);

            values.add(new PointValue(i, (float) vsaldo));

            if (vsaldo < 0.0D) {
                valuesNeg.add(new PointValue(i, (float) vsaldo));
            } else {
                valuesPos.add(new PointValue(i, (float) vsaldo));
            }

        }
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


    @SuppressLint("NewApi")
    private void usarActionBar() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.barra_botoes_lista, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                break;
            case R.id.menu_ajustes:
                startActivityForResult(new Intent(this, Ajustes.class), 121212);
                break;
            case R.id.menu_sobre:
                startActivity(new Intent("com.msk.minhascontas.SOBRE"));
                break;
            case R.id.botao_pesquisar:
                startActivityForResult(
                        new Intent("com.msk.minhascontas.BUSCACONTA"), 2424242);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
