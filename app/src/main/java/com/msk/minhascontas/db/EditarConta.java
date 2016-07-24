package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.info.BarraProgresso;

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
    private Button modifica, cancela;
    private RadioGroup tipo;
    private AppCompatRadioButton rec, desp, aplic;
    private AppCompatCheckBox pagamento;
    private AppCompatSpinner classificaConta, intervaloRepete;
    // VARIAVEIS UTILIZADAS
    private double valorConta, valorNovoConta;
    private long idConta, idConta1;
    private int anoPrest, diaVenc, mesPrest, nPrest, qtPrest,
            intervalo, qtConta, nr, altera;
    private int[] dmaConta, repeteConta;
    private String classeConta, dataConta, nomeConta, tipoConta, pagouConta,
            novoPagouConta, novoNomeConta;
    private String[] dadosConta, despesas, receitas, aplicacoes;
    private Resources r;
    @SuppressWarnings("rawtypes")
    private ArrayAdapter autocompleta, classesContas;

    @Override
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.edita_conta);
        Bundle localBundle = getIntent().getExtras();
        idConta = localBundle.getLong("id");
        r = getResources();
        dbContaParaEditar.open();
        Iniciar();
        usarActionBar();
        PegaConta();
        MostraDados();
        altera = 0;
        ConfereRepeticaoConta();

        classificaConta.setOnItemSelectedListener(this);
        intervaloRepete.setOnItemSelectedListener(this);
        pagamento.setOnClickListener(this);
        data.setOnClickListener(this);
        modifica.setOnClickListener(this);
        cancela.setOnClickListener(this);
        tipo.setOnCheckedChangeListener(this);

    }

    private void ConfereRepeticaoConta() {
        // CONFERE SE A CONTA REPETE

        qtConta = 0;
        nr = 0;
        dbContaParaEditar.open();
        qtConta = dbContaParaEditar.quantasContasPorNome(nomeConta);
        // dbContaParaEditar.close();

        if (qtConta > 1) {
            Dialogo();
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void Iniciar() {
        nome = (AppCompatAutoCompleteTextView) findViewById(R.id.acNomeContaModificada);
        pagamento = (AppCompatCheckBox) findViewById(R.id.cbPagamento);
        data = (Button) findViewById(R.id.etDataConta);
        valor = (AppCompatEditText) findViewById(R.id.etValorNovo);
        tipo = (RadioGroup) findViewById(R.id.rgTipoContaModificada);
        rec = (AppCompatRadioButton) findViewById(R.id.rRecContaModificada);
        desp = (AppCompatRadioButton) findViewById(R.id.rDespContaModificada);
        aplic = (AppCompatRadioButton) findViewById(R.id.rAplicContaModificada);
        prestacoes = (AppCompatEditText) findViewById(R.id.etPrestacoes);
        modifica = (Button) findViewById(R.id.ibModificaConta);
        cancela = (Button) findViewById(R.id.ibCancelar);
        classificaConta = (AppCompatSpinner) findViewById(R.id.spClassificaConta);
        intervaloRepete = (AppCompatSpinner) findViewById(R.id.spRepeticoes);
        autocompleta = new ArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, getResources()
                .getStringArray(R.array.NomeConta));
        nome.setAdapter(autocompleta);
        pagamento.setVisibility(View.GONE);
        dia = mes = ano = 0;
        despesas = r.getStringArray(R.array.TipoDespesa);
        receitas = r.getStringArray(R.array.TipoReceita);
        aplicacoes = r.getStringArray(R.array.TipoAplicacao);
    }

    private void PegaConta() {
        dbContaParaEditar.open();
        dmaConta = dbContaParaEditar.mostraDMAConta(idConta);
        dia = dmaConta[0];
        mes = dmaConta[1];
        ano = dmaConta[2];
        dadosConta = dbContaParaEditar.mostraDadosConta(idConta);
        nomeConta = dadosConta[0];
        tipoConta = dadosConta[1];
        classeConta = dadosConta[2];
        pagouConta = dadosConta[3];
        novoPagouConta = pagouConta;
        dataConta = dadosConta[4];
        valorConta = dbContaParaEditar.mostraValorConta(idConta);
        repeteConta = dbContaParaEditar.mostraRepeticaoConta(idConta);

        qtPrest = repeteConta[0];
        nPrest = repeteConta[1];
        intervalo = repeteConta[2];
        idConta1 = dbContaParaEditar.mostraPrimeiraRepeticaoConta(nomeConta,
                qtPrest);
        String dataAntiga = dbContaParaEditar.mostraDataConta(idConta1);
        dbContaParaEditar.atualizaDataContas(nomeConta, dataAntiga, qtPrest);
        dataConta = dataAntiga;
        // dbContaParaEditar.close();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void MostraDados() {
        nome.setText(nomeConta);
        String str = (dia + "/" + (1 + mes) + "/" + ano);
        data.setText(str);
        valor.setText(String.format("%.2f", valorConta).replace(",", "."));
        pagamento.setChecked(false);

        if (pagouConta.equals("paguei"))
            pagamento.setChecked(true);

        int i = 0;
        // dbContaParaEditar.open();
        if (tipoConta.equals(r.getString(R.string.linha_despesa))) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoDespesa));

            for (int j = 0; j < despesas.length; j++) {
                if (dadosConta[2].equals(despesas[j]))
                    i = j;
            }

            rec.setChecked(false);
            aplic.setChecked(false);
            desp.setChecked(true);
            pagamento.setText(R.string.dica_pagamento);
            pagamento.setVisibility(View.VISIBLE);

        } else if (tipoConta.equals(r.getString(R.string.linha_receita))) {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoReceita));

            for (int j = 0; j < receitas.length; j++) {
                if (dadosConta[2].equals(receitas[j]))
                    i = j;
            }

            rec.setChecked(true);
            aplic.setChecked(false);
            desp.setChecked(false);

            pagamento.setText(R.string.dica_recebe);
            pagamento.setVisibility(View.VISIBLE);

        } else {
            classesContas = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.TipoAplicacao));
            i = 1;

            for (int j = 0; j < aplicacoes.length; j++) {
                if (dadosConta[2].equals(aplicacoes[j]))
                    i = j;
            }

            rec.setChecked(false);
            aplic.setChecked(true);
            desp.setChecked(false);
            pagamento.setVisibility(View.GONE);

        }

        classesContas
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classificaConta.setAdapter(classesContas);
        classificaConta.setSelection(i);

        // dbContaParaEditar.close();

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
                String str = (dia + "/" + (1 + mes) + "/" + ano);
                data.setText(str);

                break;
            case R.id.cbPagamento:
                if (pagamento.isChecked()) {
                    novoPagouConta = "paguei";
                } else {
                    novoPagouConta = "falta";
                }
                break;
            case R.id.ibModificaConta:

                ConfereAlteracoesConta();

                if (nr == 0) {
                    ModificaUmaConta();
                }
                if (qtPrest != repeteConta[0] || repeteConta[2] != intervalo
                        || nr > 0) {
                    //ModificaUmaConta();
                    ModificaContas();
                }
                if (qtPrest > 1 && nr > 0) {
                    new BarraProgresso(this, getResources().getString(
                            R.string.dica_titulo_barra), getResources().getString(
                            R.string.dica_barra_altera), qtPrest, 0).execute();
                }
                altera = 1;
                setResult(RESULT_OK, null);
                finish();
                break;
            case R.id.ibCancelar:
                setResult(RESULT_OK, null);
                finish();
                break;
        }

    }

    private void ConfereAlteracoesConta() {

        // Confere se foi digitado um nome
        if (!nome.getText().toString().equals("")) {
            novoNomeConta = nome.getText().toString();
        } else
            novoNomeConta = r.getString(R.string.sem_nome);

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
            valorNovoConta = 0;

        // Atualiza data para contas
        diaVenc = dia;
        mesPrest = mes;
        anoPrest = ano;

        // Confere se alterou as repeticoes
        if (!prestacoes.getText().toString().equals(""))
            qtPrest = Integer.parseInt(prestacoes.getText().toString());
        else
            qtPrest = 0;

    }

    private void ModificaContas() {

        if (nr == 1 && nPrest != 1) {

            // Metodo para obter data da primeira conta da repeticao
            int[] dma = dbContaParaEditar.mostraDMAConta(idConta1);
            diaVenc = dma[0];
            mesPrest = dma[1];
            anoPrest = dma[2];
            nPrest = 1;
        }

        dbContaParaEditar.excluiSerieContaPorNome(nomeConta, dataConta, nPrest);

        // Metodo para repetir conta
        for (int i = nPrest; i <= qtPrest; i++) {

            nPrest = i;

            dbContaParaEditar.geraConta(novoNomeConta, tipoConta, classeConta,
                    novoPagouConta, dataConta, diaVenc, mesPrest, anoPrest,
                    valorNovoConta, qtPrest, nPrest, intervalo);

            // CORRECAO DAS DATAS PARA OS INTERVALOS DE REPETICAO
            if (intervalo == 300) { // Repeticao mensal

                mesPrest = (1 + mesPrest);

                if (mesPrest > 11) {
                    mesPrest = 0;
                    anoPrest = (1 + anoPrest);
                }

            } else if (intervalo == 3650) { // Repeticao anual
                anoPrest = anoPrest + 1;
            } else { // Repeticao diaria ou semanal
                diaVenc = diaVenc + (intervalo - 100);
            }

            ConfereDiaMes();
        }

    }

    private void ConfereDiaMes() {

        // Fevereiro ano normal
        if (diaVenc > 28 && mesPrest == 1
                && (int) Math.IEEEremainder(anoPrest, 4.0D) != 0) {
            diaVenc = diaVenc - 28;
            mesPrest = 2;
        } else if (diaVenc > 29 && mesPrest == 1
                && (int) Math.IEEEremainder(anoPrest, 4.0D) == 0) {
            diaVenc = diaVenc - 29;
            mesPrest = 2;
        } else if (diaVenc > 30) {
            if (mesPrest == 3 || mesPrest == 5 || mesPrest == 6
                    || mesPrest == 8 || mesPrest == 10) {
                diaVenc = diaVenc - 30;
                mesPrest = mesPrest + 1;

            }
        } else if (diaVenc > 31)
            if (mesPrest == 0 || mesPrest == 2 || mesPrest == 4
                    || mesPrest == 7 || mesPrest == 9 || mesPrest == 11) {
                diaVenc = diaVenc - 31;
                mesPrest = mesPrest + 1;
                if (mesPrest > 11) {
                    mesPrest = 0;
                    anoPrest = (1 + anoPrest);
                }
            }

    }

    private void ModificaUmaConta() {

        // Modifica uma conta ou uma repeti��o da conta

        dbContaParaEditar.alteraNomeConta(idConta, novoNomeConta);

        // Altera o valor da Conta
        if (valorConta != valorNovoConta || !pagouConta.equals(novoPagouConta)) {
            dbContaParaEditar.alteraValorConta(idConta, valorNovoConta,
                    novoPagouConta);
        }

        // Atualiza tipo, classe e data da conta
        dbContaParaEditar.alteraTipoConta(idConta, tipoConta);
        dbContaParaEditar.alteraClasseConta(idConta, classeConta);

        // Altera Data Contas
        if (dmaConta[0] != diaVenc || dmaConta[1] != mesPrest
                || dmaConta[2] != anoPrest) {

            dbContaParaEditar.alteraDataConta(idConta, dataConta, diaVenc,
                    mesPrest, anoPrest);
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
                                // dbContaParaEditar.open();
                                int[] repete = dbContaParaEditar
                                        .mostraRepeticaoConta(idConta);
                                // dbContaParaEditar.close();
                                nr = repete[1];

                                break;
                            case 2: // Edita todas as repeticoes
                                nr = 1;
                                break;
                        }

                        dialogo.dismiss();
                        // ModificaDadosConta();
                        // setResult(RESULT_OK, null);
                        // finish();

                    }

                });

        // create alert dialog
        dialogo = alertDialogBuilder.create();
        // show it
        dialogo.show();

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        switch (checkedId) {

            case R.id.rDespContaModificada:
                tipoConta = r.getString(R.string.linha_despesa);
                pagamento.setVisibility(View.VISIBLE);
                break;
            case R.id.rRecContaModificada:
                tipoConta = r.getString(R.string.linha_receita);

                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);

                break;
            case R.id.rAplicContaModificada:
                tipoConta = r.getString(R.string.linha_aplicacoes);
                pagamento.setVisibility(View.GONE);

                break;
        }
        MostraDados();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbContaParaEditar.close();
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
    public void onItemSelected(AdapterView<?> spinner, View arg1, int posicao,
                               long arg3) {

        if (spinner.getId() == R.id.spClassificaConta) {
            if (tipoConta.equals(r.getString(R.string.linha_despesa))) {
                classeConta = despesas[posicao];
                if (posicao == 1) {
                    prestacoes.setText(120 + "");
                    qtPrest = 120;
                }
            } else if (tipoConta.equals(r.getString(R.string.linha_aplicacoes))) {
                classeConta = aplicacoes[posicao];
                tipoConta = r.getString(R.string.linha_aplicacoes);
                pagamento.setVisibility(View.GONE);

            } else {
                classeConta = receitas[posicao];
                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);
            }

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

            String str = (dia + "/" + (1 + mes) + "/" + ano);
            data.setText(str);
        }
    }
}
