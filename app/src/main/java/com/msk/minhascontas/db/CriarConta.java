package com.msk.minhascontas.db;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.msk.minhascontas.R;
import com.msk.minhascontas.info.AlertaCalendario;
import com.msk.minhascontas.info.BarraProgresso;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CriarConta extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private static final int LER_AGENDA = 444;
    // ELEMENTOS DA TELA
    private static Button dataConta;
    // VARIAVEIS UTILIZADAS
    private static int dia, mes, ano, diaRepete, mesRepete, anoRepete;
    private TextInputLayout juros;
    private AppBarLayout titulo;
    private AppCompatAutoCompleteTextView nomeConta;
    private AppCompatEditText repeteConta, valorConta, jurosConta;
    private RadioGroup tipo;
    private AppCompatCheckBox parcelarConta, pagamento, lembrete;
    private AppCompatSpinner classificaConta, categoriaConta, intervaloRepete;
    private LinearLayout cb, categoria;
    private Resources res;
    private DBContas dbNovasContas = new DBContas(this);
    private int contaTipo, contaClasse, contaCategoria, qtRepete, intervalo, nr;
    private String contaData, contaNome, contaPaga;
    private double contaValor, valorJuros;

    @SuppressWarnings("rawtypes")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cria_conta);
        res = getResources();
        dbNovasContas.open();
        iniciar();
        usarActionBar();
        DataDeHoje();
        MostraClasseConta();
        tipo.setOnCheckedChangeListener(this);
        parcelarConta.setVisibility(View.GONE);
        dataConta.setOnClickListener(this);
        pagamento.setOnClickListener(this);
        classificaConta.setOnItemSelectedListener(this);
        categoriaConta.setOnItemSelectedListener(this);
        intervaloRepete.setOnItemSelectedListener(this);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void iniciar() {
        titulo = findViewById(R.id.aplBarra);
        valorConta = findViewById(R.id.etValorNovaConta);
        jurosConta = findViewById(R.id.etJurosNovaConta);
        repeteConta = findViewById(R.id.etRepeticoes);
        tipo = findViewById(R.id.rgTipoNovaConta);
        categoria = findViewById(R.id.layout_categoria);
        cb = findViewById(R.id.layout_pagamento);
        parcelarConta = findViewById(R.id.cbValorParcelar);
        pagamento = findViewById(R.id.cbPagamento);
        lembrete = findViewById(R.id.cbLembrete);
        dataConta = findViewById(R.id.etData);
        juros = findViewById(R.id.layout_juros);
        classificaConta = findViewById(R.id.spClasseConta);
        categoriaConta = findViewById(R.id.spCategoriaConta);
        categoriaConta.setSelection(7);
        nomeConta = findViewById(R.id.acNomeNovaConta);
        ArrayAdapter completa = new ArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, getResources()
                .getStringArray(R.array.NomeConta));
        nomeConta.setAdapter(completa);

        intervaloRepete = findViewById(R.id.spRepeticoes);
        intervaloRepete.setSelection(2);
        contaTipo = contaClasse = 0;
        contaCategoria = 7;
        contaPaga = "falta";
        intervalo = 300;
        nr = 0;
    }

    private void DataDeHoje() {
        Locale current = res.getConfiguration().locale;
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, current);
        Calendar c = Calendar.getInstance();
        dia = c.get(Calendar.DAY_OF_MONTH);
        mes = c.get(Calendar.MONTH);
        ano = c.get(Calendar.YEAR);
        dataConta.setText(df.format(c.getTime()));
        diaRepete = dia;
        anoRepete = ano;
    }

    @Override
    public void onCheckedChanged(RadioGroup tipos, int tipoId) {

        switch (tipoId) {

            case R.id.rDespNovaConta:
                contaTipo = 0;
                pagamento.setVisibility(View.VISIBLE);
                pagamento.setText(R.string.dica_pagamento);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.VISIBLE);
                categoria.setVisibility(View.VISIBLE);
                break;
            case R.id.rRecNovaConta:
                contaTipo = 1;
                pagamento.setVisibility(View.VISIBLE);
                pagamento.setText(R.string.dica_recebe);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.VISIBLE);
                categoria.setVisibility(View.GONE);
                break;
            case R.id.rAplicNovaConta:
                contaTipo = 2;
                pagamento.setVisibility(View.GONE);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.GONE);
                categoria.setVisibility(View.GONE);
                break;
        }
        MostraClasseConta();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void MostraClasseConta() {
        int i = 0;
        ArrayAdapter classesContas;

        if (contaTipo == 1) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoReceita));
        } else if (contaTipo == 2) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoAplicacao));
            i = 1;
        } else {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoDespesa));
        }
        classesContas
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classificaConta.setAdapter(classesContas);
        classificaConta.setSelection(i);
        // dbNovasContas.close();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View paramView) {

        switch (paramView.getId()) {
            case R.id.etData:
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), "datePicker");
                break;
            case R.id.cbPagamento:
                if (pagamento.isChecked()) {
                    contaPaga = "paguei";
                } else {
                    contaPaga = "falta";
                }
                break;
        }
    }

    private void ConfereDadosConta() {

        if (nomeConta.getText().toString().equals(""))
            contaNome = res.getString(R.string.sem_nome);
        else {
            contaNome = nomeConta.getText().toString();
        }
        String nomeConta1 = contaNome;
        String nomeConta2 = contaNome;
        dbNovasContas.open();
        int a = dbNovasContas.quantasContasPorNomeNoDia(nomeConta1, dia, mes,
                ano);
        int b = 1;

        if (a != 0) {
            while (a != 0) {
                nomeConta2 = nomeConta1 + b;
                a = dbNovasContas.quantasContasPorNomeNoDia(nomeConta2, dia,
                        mes, ano);
                b = b + 1;
            }
            contaNome = nomeConta2;
        }

        if (!repeteConta.getText().toString().equals(""))
            qtRepete = Integer.parseInt(repeteConta.getText().toString());
        else
            qtRepete = 1;

        if (!jurosConta.getText().toString().equals("")) {
            valorJuros = Double.parseDouble(jurosConta.getText().toString());
            valorJuros = valorJuros / 100;
        } else {
            valorJuros = 0.0D;
        }
    }

    private void ArmazenaDadosConta() {

        if (!valorConta.getText().toString().equals("")) {
            Double valorPrestacao;
            if (parcelarConta.isChecked()) {
                valorPrestacao = Double.parseDouble(valorConta.getText()
                        .toString());
                valorPrestacao = valorPrestacao / qtRepete;
                contaValor = valorPrestacao;

                if (contaClasse == 2 && qtRepete > 1 && valorJuros != 0)
                    contaValor = contaValor
                            * ((valorJuros) / (1.0D - (1 / (Math.pow(
                            (valorJuros + 1.0D), qtRepete)))));
            } else {
                contaValor = Double.valueOf(valorConta.getText().toString());
            }

            contaData = dataConta.getText().toString();

            if (repeteConta.getText().toString().equals("")
                    || repeteConta.getText().toString().equals("0")) {

                dbNovasContas.geraConta(contaNome, contaTipo, contaClasse,
                        contaCategoria, dia, mes, ano, contaValor, contaPaga,
                        qtRepete, 1, intervalo, contaData);
            } else {
                // Metodo para repetir conta
                dbNovasContas.geraConta(contaNome, contaTipo, contaClasse,
                        contaCategoria, dia, mes, ano, contaValor, contaPaga,
                        qtRepete, 1, intervalo, contaData);

                Calendar data = Calendar.getInstance();
                data.set(ano, mes, dia);

                for (int i = 1; i < qtRepete; i++) {

                    int nRepete = i + 1;

                    if (intervalo == 300) { // Repeticao mensal
                        data.add(Calendar.MONTH, 1);
                    } else if (intervalo == 3650) { // Repeticao anual
                        data.add(Calendar.YEAR, 1);
                    } else { // Repeticao diaria ou semanal
                        data.add(Calendar.DATE, intervalo - 100);
                    }

                    diaRepete = data.get(Calendar.DAY_OF_MONTH);
                    mesRepete = data.get(Calendar.MONTH);
                    anoRepete = data.get(Calendar.YEAR);

                    if (contaClasse == 0 || contaClasse == 2)
                        contaValor = contaValor * (1.0D + valorJuros);

                    dbNovasContas
                            .geraConta(contaNome, contaTipo, contaClasse, contaCategoria,
                                    diaRepete, mesRepete, anoRepete, contaValor, contaPaga,
                                    qtRepete, nRepete, intervalo, contaData);
                }
            }
        } else {
            nr = 0;
        }
    }

    private void AdicionaLembrete() {

        int permEscrever = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        int permLer = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);

        if (permEscrever != PackageManager.PERMISSION_GRANTED && permLer != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CALENDAR)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR}, LER_AGENDA);
            }
        } else {
            // CRIA LEMBRETE E ALERTA NO CALENDARIO
            AlertaCalendario
                    .adicionarEventoNoCalendario(
                            getContentResolver(),
                            this.getString(R.string.dica_evento, contaNome),
                            this.getString(R.string.dica_calendario,
                                    String.format(Locale.US, "%.2f", contaValor)), dia, mes,
                            ano, true, qtRepete, intervalo);
            setResult(RESULT_OK, null);
            dbNovasContas.close();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // CRIA LEMBRETE E ALERTA NO CALENDARIO
            AlertaCalendario
                    .adicionarEventoNoCalendario(
                            getContentResolver(),
                            this.getString(R.string.dica_evento, contaNome),
                            this.getString(R.string.dica_calendario,
                                    String.format(Locale.US, "%.2f", contaValor)), dia, mes,
                            ano, true, qtRepete, intervalo);
            setResult(RESULT_OK, null);
            dbNovasContas.close();
            finish();
        }
    }

    private void CriaAplicacao() {

        // USUARIO ESCOLHE TRANSFORMAR APLICACAO EM DESPESA
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_despesa_saque)
                .setMessage(R.string.texto_despesa_saque)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface pDialogo,
                                                int pInt) {
                                dbNovasContas.geraConta(contaNome,
                                        0, 1, 7, dia, mes, ano,
                                        contaValor, contaPaga, qtRepete, 1,
                                        intervalo, contaData);
                                // dbNovasContas.close();
                                if (lembrete.isChecked()) {
                                    AdicionaLembrete();
                                } else {
                                    finish();
                                }
                            }
                        })
                .setNegativeButton(R.string.cancelar,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface pDialogo,
                                                int pInt) {
                                pDialogo.dismiss();
                                if (lembrete.isChecked()) {
                                    AdicionaLembrete();
                                } else {
                                    finish();
                                }
                            }
                        }).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> spinner, View itemsp,
                               int posicao, long paramLong) {

        ColorDrawable cor;
        if (spinner.getId() == R.id.spClasseConta) {
            if (contaTipo == 0) {
                contaClasse = posicao;
                cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FFCC0000"));
                juros.setVisibility(View.VISIBLE);
                pagamento.setText(R.string.dica_pagamento);
                pagamento.setVisibility(View.VISIBLE);
                if (posicao == 0 || posicao == 3)
                    parcelarConta.setVisibility(View.VISIBLE);
                else
                    parcelarConta.setVisibility(View.GONE);

                if (posicao == 1) {
                    repeteConta.setText(120 + "");
                    qtRepete = 120;
                } else {
                    repeteConta.setText("");
                }
            } else if (contaTipo == 1) {
                contaClasse = posicao;
                cor = new ColorDrawable(Color.parseColor("#FF0099CC"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FF0099CC"));
                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);
                juros.setVisibility(View.GONE);
                repeteConta.setText("");
                parcelarConta.setVisibility(View.GONE);
            } else {
                contaClasse = posicao;
                cor = new ColorDrawable(Color.parseColor("#FF669900"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FF669900"));
                juros.setVisibility(View.VISIBLE);
                repeteConta.setText("");
                pagamento.setVisibility(View.GONE);
                parcelarConta.setVisibility(View.GONE);
            }
            parcelarConta.setChecked(false);
        } else if (spinner.getId() == R.id.spCategoriaConta) {
            contaCategoria = posicao;
        } else {
            switch (posicao) {
                case 0: // Diariamente
                    intervalo = 101;
                    break;
                case 1: // Semanalmente
                    intervalo = 107;
                    break;
                case 2: // Mensalmente
                    intervalo = 300;
                    break;
                case 3: // Anualmente
                    intervalo = 3650;
                    break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> paramAdapterView) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cria_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbNovasContas.close();
                finish();
                return true;
            case R.id.menu_cria:

                nr = 1;
                ConfereDadosConta();
                ArmazenaDadosConta();
                setResult(RESULT_OK, null);

                if (qtRepete > 1) {
                    new BarraProgresso(this, getResources().getString(
                            R.string.dica_titulo_barra), getResources().getString(
                            R.string.dica_barra_progresso), qtRepete, 0, "mskapp").execute();
                }

                if (contaTipo == 2) {
                    CriaAplicacao();
                } else {
                    if (lembrete.isChecked()) {
                        AdicionaLembrete();
                    } else {
                        finish();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void usarActionBar() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_cancel_white);
        getSupportActionBar().setTitle("");
        ColorDrawable cor;
        cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
        getSupportActionBar().setBackgroundDrawable(cor);
        titulo.setBackgroundColor(Color.parseColor("#FFCC0000"));
    }

    @Override
    protected void onDestroy() {

        if (nr == 1) {
            Toast.makeText(
                    getApplicationContext(),
                    String.format(
                            getResources()
                                    .getString(R.string.dica_conta_criada),
                            contaNome), Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, null);
        dbNovasContas.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        dbNovasContas.open();
        super.onResume();
    }

    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new DatePickerDialog(getActivity(), this, ano, mes, dia);
        }

        public void onDateSet(DatePicker view, int mAno, int mMes, int mDia) {
            ano = mAno;
            mes = mMes;
            dia = mDia;
            Calendar data = Calendar.getInstance();
            data.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().locale;
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            dataConta.setText(dataFormato.format(data.getTime()));
            diaRepete = dia;
            anoRepete = ano;
        }
    }
}