package com.msk.minhascontas.db;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.info.AlertaCalendario;
import com.msk.minhascontas.info.BarraProgresso;

import java.util.Calendar;
import java.util.Locale;

public class CriarConta extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private static final int LER_AGENDA = 444;
    // ELEMENTOS DA TELA
    private static Button dataConta;
    // VARIAVEIS UTILIZADAS
    private static int dia, mes, ano;
    private final Calendar c = Calendar.getInstance();
    private Button criaNovaConta, cancela;
    private TextInputLayout juros;
    private AppCompatAutoCompleteTextView nomeConta;
    private AppCompatEditText repeteConta, valorConta, jurosConta;
    private RadioGroup tipo;
    private AppCompatCheckBox parcelarConta, pagamento, lembrete;
    private AppCompatSpinner classificaConta, intervaloRepete;
    private LinearLayout cb;
    private Resources r;
    private DBContas dbNovasContas = new DBContas(this);
    private int diaRepete, mesRepete, anoRepete;
    private int qtRepete, intervalo, nr;
    private String contaClasse, contaData, contaNome, contaPaga, contaTipo;
    private double contaValor, valorJuros;
    private String[] despesas, receitas, aplicacoes;

    @SuppressWarnings("rawtypes")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cria_conta);
        r = getResources();
        dbNovasContas.open();
        iniciar();
        usarActionBar();
        DataDeHoje(dia, mes, ano);
        MostraClasseConta();
        tipo.setOnCheckedChangeListener(this);
        parcelarConta.setVisibility(View.GONE);
        dataConta.setOnClickListener(this);
        criaNovaConta.setOnClickListener(this);
        cancela.setOnClickListener(this);
        pagamento.setOnClickListener(this);
        classificaConta.setOnItemSelectedListener(this);
        intervaloRepete.setOnItemSelectedListener(this);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void iniciar() {
        nomeConta = ((AppCompatAutoCompleteTextView) findViewById(R.id.acNomeNovaConta));
        valorConta = ((AppCompatEditText) findViewById(R.id.etValorNovaConta));
        jurosConta = (AppCompatEditText) findViewById(R.id.etJurosNovaConta);
        repeteConta = ((AppCompatEditText) findViewById(R.id.etRepeticoes));
        criaNovaConta = ((Button) findViewById(R.id.ibNovaConta));
        cancela = (Button) findViewById(R.id.ibCancelar);
        tipo = ((RadioGroup) findViewById(R.id.rgTipoNovaConta));
        cb = (LinearLayout) findViewById(R.id.layout_pagamento);
        parcelarConta = ((AppCompatCheckBox) findViewById(R.id.cbValorParcelar));
        pagamento = ((AppCompatCheckBox) findViewById(R.id.cbPagamento));
        lembrete = (AppCompatCheckBox) findViewById(R.id.cbLembrete);
        dataConta = ((Button) findViewById(R.id.etData));
        juros = (TextInputLayout) findViewById(R.id.layout_juros);
        classificaConta = ((AppCompatSpinner) findViewById(R.id.spClasseConta));
        intervaloRepete = ((AppCompatSpinner) findViewById(R.id.spRepeticoes));
        intervaloRepete.setSelection(2);
        ArrayAdapter completa = new ArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, getResources()
                .getStringArray(R.array.NomeConta));
        nomeConta.setAdapter(completa);
        dia = c.get(Calendar.DAY_OF_MONTH);
        mes = c.get(Calendar.MONTH);
        ano = c.get(Calendar.YEAR);
        contaTipo = r.getString(R.string.linha_despesa);
        contaClasse = r.getString(R.string.linha_cartao);
        contaPaga = "falta";
        intervalo = 300;
        despesas = r.getStringArray(R.array.TipoDespesa);
        receitas = r.getStringArray(R.array.TipoReceita);
        aplicacoes = r.getStringArray(R.array.TipoAplicacao);
        nr = 0;
    }

    private void DataDeHoje(int dia, int mes, int ano) {

        dataConta.setText(dia + "/" + (mes + 1) + "/" + ano);
        diaRepete = dia;
        anoRepete = ano;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void MostraClasseConta() {
        int i = 0;
        ArrayAdapter classesContas;

        if (contaTipo.equals(r.getString(R.string.linha_receita))) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoReceita));

        } else if (contaTipo.equals(r.getString(R.string.linha_aplicacoes))) {
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

    @Override
    public void onCheckedChanged(RadioGroup tipos, int tipoId) {

        switch (tipoId) {

            case R.id.rDespNovaConta:
                contaTipo = r.getString(R.string.linha_despesa);
                pagamento.setVisibility(View.VISIBLE);
                pagamento.setText(R.string.dica_pagamento);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.VISIBLE);
                break;
            case R.id.rRecNovaConta:
                contaTipo = r.getString(R.string.linha_receita);
                pagamento.setVisibility(View.VISIBLE);
                pagamento.setText(R.string.dica_recebe);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.VISIBLE);
                break;
            case R.id.rAplicNovaConta:
                contaTipo = r.getString(R.string.linha_aplicacoes);
                pagamento.setVisibility(View.GONE);
                parcelarConta.setVisibility(View.GONE);
                cb.setVisibility(View.GONE);
                break;
        }

        MostraClasseConta();

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View paramView) {

        switch (paramView.getId()) {

            case R.id.etData:

                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), "datePicker");
                DataDeHoje(dia, mes, ano);

                break;
            case R.id.cbPagamento:
                if (pagamento.isChecked()) {
                    contaPaga = "paguei";
                } else {
                    contaPaga = "falta";
                }
                break;
            case R.id.ibNovaConta:

                nr = 1;

                ConfereDadosConta();

                ArmazenaDadosConta();

                setResult(RESULT_OK, null);

                if (qtRepete > 1) {
                    new BarraProgresso(this, getResources().getString(
                            R.string.dica_titulo_barra), getResources().getString(
                            R.string.dica_barra_progresso), qtRepete, 0, "mskapp").execute();
                }

                if (contaTipo.equals(r.getString(R.string.linha_aplicacoes))) {
                    CriaAplicacao();
                } else {
                    if (lembrete.isChecked()) {
                        AdicionaLembrete();
                    } else {
                        finish();
                    }
                }

                break;
            case R.id.ibCancelar:
                setResult(RESULT_OK, null);
                finish();
                break;
        }
    }

    private void ConfereDadosConta() {

        if (nomeConta.getText().toString().equals(""))
            contaNome = r.getString(R.string.sem_nome);
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

                String[] prestacao = r.getStringArray(R.array.TipoDespesa);

                if (contaClasse.equals(prestacao[2]) && qtRepete > 1
                        && valorJuros != 0)
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
                        contaPaga, contaData, dia, mes, ano, contaValor,
                        qtRepete, 1, intervalo);
            } else {
                // Metodo para repetir conta
                dbNovasContas.geraConta(contaNome, contaTipo, contaClasse,
                        contaPaga, contaData, dia, mes, ano, contaValor,
                        qtRepete, 1, intervalo);

                Calendar data = Calendar.getInstance();
                data.set(ano, mes, dia);

                for (int i = 1; i < qtRepete; i++) {

                    int nRepete = i + 1;

                    if (intervalo == 300) { // Repeticao mensal
                        data.add(Calendar.DATE, 30);
                    } else if (intervalo == 3650) { // Repeticao anual
                        data.add(Calendar.YEAR, 1);
                    } else { // Repeticao diaria ou semanal
                        data.add(Calendar.DATE, intervalo - 100);
                    }

                    diaRepete = data.get(Calendar.DAY_OF_MONTH);
                    mesRepete = data.get(Calendar.MONTH);
                    anoRepete = data.get(Calendar.YEAR);

                    String[] aplicacao = r.getStringArray(R.array.TipoReceita);

                    if (contaClasse.equals(aplicacao[0])
                            || contaClasse.equals(aplicacao[2]))
                        contaValor = contaValor * (1.0D + valorJuros);

                    dbNovasContas
                            .geraConta(contaNome, contaTipo, contaClasse,
                                    contaPaga, contaData, diaRepete, mesRepete,
                                    anoRepete, contaValor, qtRepete, nRepete,
                                    intervalo);
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
                                String[] despesas = r
                                        .getStringArray(R.array.TipoDespesa);
                                // dbNovasContas.open();
                                dbNovasContas.geraConta(contaNome,
                                        r.getString(R.string.linha_despesa),
                                        despesas[1], contaPaga, contaData, dia,
                                        mes, ano, contaValor, qtRepete, 1,
                                        intervalo);
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

        if (spinner.getId() == R.id.spClasseConta) {
            if (contaTipo.equals(r.getString(R.string.linha_despesa))) {

                contaClasse = despesas[posicao];

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
            } else if (contaTipo.equals(r.getString(R.string.linha_receita))) {

                contaClasse = receitas[posicao];
                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);
                juros.setVisibility(View.GONE);
                repeteConta.setText("");
                parcelarConta.setVisibility(View.GONE);
            } else {

                contaClasse = aplicacoes[posicao];
                juros.setVisibility(View.VISIBLE);
                repeteConta.setText("");
                pagamento.setVisibility(View.GONE);
                parcelarConta.setVisibility(View.GONE);
            }

            parcelarConta.setChecked(false);

        }

        if (spinner.getId() == R.id.spRepeticoes) {

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbNovasContas.close();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void usarActionBar() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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

            dataConta.setText(dia + "/" + (mes + 1) + "/" + ano);
        }
    }

}
