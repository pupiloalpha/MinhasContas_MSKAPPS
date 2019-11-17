package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
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
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.AppBarLayout;
import com.msk.minhascontas.R;
import com.msk.minhascontas.info.BarraProgresso;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

@SuppressLint("NewApi")
public class EditarConta extends AppCompatActivity implements
        View.OnClickListener, RadioGroup.OnCheckedChangeListener,
        OnItemSelectedListener {

    private static Button data;
    private static int dia, mes, ano;
    private DBContas dbContaParaEditar = new DBContas(this);
    private AlertDialog dialogo;
    // ELEMENTOS DA TELA
    private AppCompatAutoCompleteTextView nome;
    private AppCompatEditText valor, prestacoes;
    private RadioGroup tipo;
    private AppCompatRadioButton rec, desp, aplic;
    private AppCompatCheckBox pagamento;
    private AppCompatSpinner classificaConta, contaCategoria, intervaloRepete;
    private AppBarLayout titulo;
    private LinearLayout categoria;
    // VARIAVEIS UTILIZADAS
    private double valorConta, valorNovoConta;
    private long idConta, idConta1;
    private int tipoConta, classeConta, categoriaConta,
            anoPrest, diaVenc, mesPrest, nPrest, qtPrest,
            intervalo, qtConta, nr, altera;
    private int[] repeteConta, dataConta;
    private String nomeConta, pagouConta, codigoConta,
            novoPagouConta, novoNomeConta;
    private Resources res;
    @SuppressWarnings("rawtypes")
    private ArrayAdapter autocompleta, classesContas;

    @Override
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.edita_conta);
        Bundle localBundle = getIntent().getExtras();
        idConta = localBundle.getLong("id");
        res = getResources();
        dia = mes = ano = altera = qtConta = nr = 0;
        dbContaParaEditar.open();
        PegaConta();
        ConfereRepeticaoConta();
        Iniciar();
        usarActionBar();
        MostraDados();
        classificaConta.setOnItemSelectedListener(this);
        contaCategoria.setOnItemSelectedListener(this);
        intervaloRepete.setOnItemSelectedListener(this);
        pagamento.setOnClickListener(this);
        data.setOnClickListener(this);
        tipo.setOnCheckedChangeListener(this);
    }

    private void ConfereRepeticaoConta() {
        // CONFERE SE A CONTA REPETE
        qtConta = dbContaParaEditar.quantasRepeticoesDaConta(nomeConta, codigoConta);
        if (qtConta > 1) {
            Dialogo();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void Iniciar() {
        titulo = findViewById(R.id.aplBarra);
        data = findViewById(R.id.etDataConta);
        valor = findViewById(R.id.etValorNovo);
        tipo = findViewById(R.id.rgTipoContaModificada);
        rec = findViewById(R.id.rRecContaModificada);
        desp = findViewById(R.id.rDespContaModificada);
        aplic = findViewById(R.id.rAplicContaModificada);
        prestacoes = findViewById(R.id.etPrestacoes);
        classificaConta = findViewById(R.id.spClasseConta);
        contaCategoria = findViewById(R.id.spCategoriaConta);
        intervaloRepete = findViewById(R.id.spRepeticoes);
        categoria = findViewById(R.id.layout_categoria);

        autocompleta = new ArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, getResources()
                .getStringArray(R.array.NomeConta));
        nome = findViewById(R.id.acNomeContaModificada);
        nome.setAdapter(autocompleta);

        pagamento = findViewById(R.id.cbPagamento);
        pagamento.setVisibility(View.GONE);
    }

    private void PegaConta() {
        Cursor cursor = dbContaParaEditar.buscaUmaConta(idConta);
        cursor.moveToFirst();
        nomeConta = cursor.getString(1);
        tipoConta = cursor.getInt(2);
        classeConta = cursor.getInt(3);
        categoriaConta = cursor.getInt(4);
        dia = cursor.getInt(5);
        mes = cursor.getInt(6);
        ano = cursor.getInt(7);
        dataConta = new int[]{dia, mes, ano};
        valorConta = cursor.getDouble(8);
        pagouConta = cursor.getString(9);
        qtPrest = cursor.getInt(10);
        nPrest = cursor.getInt(11);
        intervalo = cursor.getInt(12);
        repeteConta = new int[]{qtPrest, nPrest, intervalo};
        codigoConta = cursor.getString(13);
        novoPagouConta = pagouConta;
        idConta1 = dbContaParaEditar.mostraPrimeiraRepeticaoConta(nomeConta,
                qtPrest, codigoConta);
        String dataAntiga = dbContaParaEditar.mostraCodigoConta(idConta1);
        dbContaParaEditar.atualizaDataContas(nomeConta, dataAntiga, qtPrest);
        codigoConta = dataAntiga;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void MostraDados() {
        nome.setText(nomeConta);

        Calendar c = Calendar.getInstance();
        c.set(ano, mes, dia);
        Locale current = res.getConfiguration().locale;
        DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
        data.setText(dataFormato.format(c.getTime()));

        valor.setText(String.format(Locale.US, "%.2f", valorConta));
        pagamento.setChecked(false);

        if (pagouConta.equals("paguei"))
            pagamento.setChecked(true);

        ColorDrawable cor;
        if (tipoConta == 0) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoDespesa));
            rec.setChecked(false);
            aplic.setChecked(false);
            desp.setChecked(true);
            cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
            getSupportActionBar().setBackgroundDrawable(cor);
            titulo.setBackgroundColor(Color.parseColor("#FFCC0000"));
            pagamento.setText(R.string.dica_pagamento);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.VISIBLE);
        } else if (tipoConta == 1) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoReceita));
            rec.setChecked(true);
            aplic.setChecked(false);
            desp.setChecked(false);
            cor = new ColorDrawable(Color.parseColor("#FF0099CC"));
            getSupportActionBar().setBackgroundDrawable(cor);
            titulo.setBackgroundColor(Color.parseColor("#FF0099CC"));
            pagamento.setText(R.string.dica_recebe);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.GONE);
        } else {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoAplicacao));
            rec.setChecked(false);
            aplic.setChecked(true);
            desp.setChecked(false);
            pagamento.setVisibility(View.GONE);
            categoria.setVisibility(View.GONE);
            cor = new ColorDrawable(Color.parseColor("#FF669900"));
            getSupportActionBar().setBackgroundDrawable(cor);
            titulo.setBackgroundColor(Color.parseColor("#FF669900"));
        }

        classesContas
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classificaConta.setAdapter(classesContas);
        classificaConta.setSelection(classeConta);
        contaCategoria.setSelection(categoriaConta);

        prestacoes.setText(qtPrest + "");

        if (intervalo == 101)
            intervaloRepete.setSelection(0);
        else if (intervalo == 107)
            intervaloRepete.setSelection(1);
        else if (intervalo == 3650)
            intervaloRepete.setSelection(3);
        else if (intervalo == 300)
            intervaloRepete.setSelection(2);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View paramView) {

        switch (paramView.getId()) {
            case R.id.etDataConta:
                DialogFragment newFragment = new EditarConta.DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), "datePicker");
                break;
            case R.id.cbPagamento:
                if (pagamento.isChecked()) {
                    novoPagouConta = "paguei";
                } else {
                    novoPagouConta = "falta";
                }
                break;
        }
    }

    private void Dialogo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.dica_menu_edicao));

        // set dialog message
        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        switch (id) {
                            case 0: // Edita uma conta
                                nr = 0;
                                break;
                            case 1: // Edita uma conta e as repeticoes posteriores
                                nr = nPrest;
                                break;
                            case 2: // Edita todas as repeticoes
                                nr = 1;
                                break;
                        }
                        dialogo.dismiss();
                    }

                });
        // create alert dialog
        dialogo = alertDialogBuilder.create();
        // show it
        dialogo.show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        ColorDrawable cor;
        switch (checkedId) {
            case R.id.rDespContaModificada:
                tipoConta = 0;
                cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FFCC0000"));
                pagamento.setVisibility(View.VISIBLE);
                categoria.setVisibility(View.VISIBLE);
                break;
            case R.id.rRecContaModificada:
                tipoConta = 1;
                cor = new ColorDrawable(Color.parseColor("#FF0099CC"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FF0099CC"));
                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);
                categoria.setVisibility(View.GONE);
                break;
            case R.id.rAplicContaModificada:
                tipoConta = 2;
                cor = new ColorDrawable(Color.parseColor("#FF669900"));
                getSupportActionBar().setBackgroundDrawable(cor);
                titulo.setBackgroundColor(Color.parseColor("#FF669900"));
                pagamento.setVisibility(View.GONE);
                categoria.setVisibility(View.GONE);
                break;
        }
        MostraDados();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edita_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbContaParaEditar.close();
                finish();
                return true;
            case R.id.menu_edita:
                ConfereAlteracoesConta();
                if (nr == 0 && valorNovoConta != 0.0D) {
                    dbContaParaEditar.alteraDadosConta(idConta, novoNomeConta,
                            tipoConta, classeConta, categoriaConta,
                            diaVenc, mesPrest, anoPrest, valorNovoConta, novoPagouConta,
                            qtPrest, nPrest, intervalo, codigoConta);
                } else {
                    if (qtPrest != repeteConta[0] || repeteConta[2] != intervalo
                            || dia != dataConta[0] || mes != dataConta[1]
                            || ano != dataConta[2]) {
                        ModificaContas();
                    } else {
                        AtualizaDadosContas();
                    }
                    if (qtPrest > 1) {
                        new BarraProgresso(this, getResources().getString(
                                R.string.dica_titulo_barra), getResources().getString(
                                R.string.dica_barra_altera), qtPrest, 0, "mskapp").execute();
                    }
                }
                altera = 1;
                setResult(RESULT_OK, null);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void ConfereAlteracoesConta() {

        // Confere se foi digitado um nome
        if (!nome.getText().toString().equals("")) {
            novoNomeConta = nome.getText().toString();
        } else
            novoNomeConta = res.getString(R.string.sem_nome);

        // Confere o nome digitado com o nome da conta
        if (!nomeConta.equals(novoNomeConta)) {

            String nomeConta1 = novoNomeConta;
            String nomeConta2 = novoNomeConta;

            int a = dbContaParaEditar.quantasContasPorNomeNoDia(nomeConta1,
                    dia, mes, ano);
            int b = 1;
            if (a != 0) {
                while (a != 0) {
                    nomeConta2 = nomeConta1 + b;
                    a = dbContaParaEditar.quantasContasPorNomeNoDia(nomeConta2,
                            dia, mes, ano);
                    b = b + 1;
                }
                novoNomeConta = nomeConta2;
            }
        }

        // Confere o valor digitado com o valor da conta
        if (!valor.getText().toString().equals(""))
            valorNovoConta = Double.parseDouble(valor.getText().toString());
        else
            valorNovoConta = 0.0D;

        // Atualiza data para contas
        diaVenc = dia;
        mesPrest = mes;
        anoPrest = ano;

        // Confere se alterou as repeticoes
        if (!prestacoes.getText().toString().equals(""))
            qtPrest = Integer.parseInt(prestacoes.getText().toString());
        else
            qtPrest = 0;

        if (nr == 1 && nPrest != 1) {
            nPrest = 1;
        }
    }

    private void AtualizaDadosContas() {
        dbContaParaEditar.alteraTipoContas(tipoConta, classeConta, categoriaConta, nomeConta, codigoConta, nPrest);
        if (valorConta != valorNovoConta) {
            dbContaParaEditar.alteraValorContas(valorNovoConta, pagouConta, nomeConta, codigoConta, nPrest);
        }
        if (!nomeConta.equals(novoNomeConta)) {
            dbContaParaEditar.alteraNomeContas(novoNomeConta, nomeConta, codigoConta, nPrest);
        }
    }

    private void ModificaContas() {

        Calendar data = Calendar.getInstance();
        // Metodo para obter data da primeira conta da repeticao
        int[] dma = dbContaParaEditar.mostraDMAConta(idConta1);
        if (nPrest == 1)
            data.set(dma[2], dma[1], dma[0]);
        else
            data.set(ano, mes, dia);

        dbContaParaEditar.excluiSerieContaPorNome(nomeConta, codigoConta, nPrest);

        // Metodo para repetir conta
        for (int i = nPrest; i <= qtPrest; i++) {

            nPrest = i;
            diaVenc = data.get(Calendar.DAY_OF_MONTH);
            mesPrest = data.get(Calendar.MONTH);
            anoPrest = data.get(Calendar.YEAR);

            dbContaParaEditar.geraConta(novoNomeConta, tipoConta, classeConta,
                    categoriaConta, diaVenc, mesPrest, anoPrest, valorNovoConta,
                    novoPagouConta, qtPrest, nPrest, intervalo, codigoConta);

            // CORRECAO DAS DATAS PARA OS INTERVALOS DE REPETICAO
            if (intervalo == 300) { // Repeticao mensal
                data.add(Calendar.MONTH, 1);
            } else if (intervalo == 3650) { // Repeticao anual
                data.add(Calendar.YEAR, 1);
            } else { // Repeticao diaria ou semanal
                data.add(Calendar.DATE, intervalo - 100);
            }
        }
    }

    private void usarActionBar() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_cancel_white);
    }

    @Override
    public void onItemSelected(AdapterView<?> spinner, View arg1, int posicao,
                               long arg3) {

        switch (spinner.getId()) {
            case R.id.spClasseConta:
                if (tipoConta == 0) {
                    classeConta = posicao;
                    if (posicao == 1) {
                        prestacoes.setText(120 + "");
                        qtPrest = 120;
                    }
                } else if (tipoConta == 2) {
                    classeConta = posicao;
                    pagamento.setVisibility(View.GONE);
                } else {
                    classeConta = posicao;
                    pagamento.setText(R.string.dica_recebe);
                    pagamento.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.spCategoriaConta:
                categoriaConta = posicao;
                break;
            case R.id.spRepeticoes:
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
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    protected void onDestroy() {
        if (altera == 1) {
            Toast.makeText(
                    getApplicationContext(),
                    String.format(
                            getResources().getString(
                                    R.string.dica_conta_alterada),
                            novoNomeConta), Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, null);
        dbContaParaEditar.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        dbContaParaEditar.open();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        dbContaParaEditar.close();
        finish();
        super.onBackPressed();
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
            Calendar c = Calendar.getInstance();
            c.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().locale;
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            data.setText(dataFormato.format(c.getTime()));
        }
    }
}
