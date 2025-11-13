package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.msk.minhascontas.R;
import com.msk.minhascontas.info.BarraProgresso;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

// New imports
import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.DBContas.Colunas;
import com.msk.minhascontas.db.DBContas.TipoAtualizacao;
import com.msk.minhascontas.db.DBContas.TipoExclusao;

import static com.msk.minhascontas.db.DBContas.PAGAMENTO_FALTA;
import static com.msk.minhascontas.db.DBContas.PAGAMENTO_PAGO;


@SuppressLint("NewApi")
public class EditarConta extends AppCompatActivity implements
        View.OnClickListener, RadioGroup.OnCheckedChangeListener,
        AdapterView.OnItemClickListener {

    private static Button data;
    private static int dia, mes, ano;
    private DBContas dbContaParaEditar;
    private AlertDialog dialogo;
    // ELEMENTOS DA TELA
    private TextInputEditText nome;
    private AutoCompleteTextView classificaConta, contaCategoria, intervaloRepete;
    private AppCompatEditText valor, etPrestacoes; // Renamed from 'prestacoes' to 'etPrestacoes'
    private RadioGroup tipo;
    private AppCompatRadioButton rec, desp, aplic;
    private AppCompatCheckBox pagamento;
    private LinearLayout categoria;
    // VARIAVEIS UTILIZADAS
    private double valorNovoConta;
    private long idConta;
    private int tipoConta, classeConta, categoriaConta,
            qtPrest, // Renamed from 'qtPrestacoes' to 'qtPrest'
            intervalo, qtConta, nr, altera;
    private String novoPagouConta, novoNomeConta;
    private Resources res;
    private Conta conta;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.edita_conta);
        Bundle localBundle = getIntent().getExtras();
        if (localBundle != null) {
            idConta = localBundle.getLong("id");
        }
        res = getResources();
        dbContaParaEditar = DBContas.getInstance(this);
        PegaConta();
        ConfereRepeticaoConta(); // Moved after PegaConta()
        Iniciar();
        usarActionBar();
        MostraDados();
        classificaConta.setOnItemClickListener(this);
        contaCategoria.setOnItemClickListener(this);
        intervaloRepete.setOnItemClickListener(this);
        pagamento.setOnClickListener(this);
        data.setOnClickListener(this);
        tipo.setOnCheckedChangeListener(this);
    }

    private void ConfereRepeticaoConta() {
        // CONFERE SE A CONTA REPETE
        // Using getContasByFilter to check for repetitions of the current account
        if (conta != null && conta.getCodigo() != null) {
            ContaFilter filter = new ContaFilter().setCodigoConta(conta.getCodigo());
            try (Cursor c = dbContaParaEditar.getContasByFilter(filter, null)) {
                qtConta = c.getCount();
            }
            if (qtConta > 1) {
                Dialogo();
            }
        } else {
            // If 'conta' or its 'codigo' is null, it might be a new account or an error
            // For now, assume no repetitions to edit if data is not available
            qtConta = 0;
        }
    }

    private void Iniciar() {
        toolbar = findViewById(R.id.toolbar);
        data = findViewById(R.id.etDataConta);
        valor = findViewById(R.id.etValorNovo);
        tipo = findViewById(R.id.rgTipoContaModificada);
        rec = findViewById(R.id.rRecContaModificada);
        desp = findViewById(R.id.rDespContaModificada);
        aplic = findViewById(R.id.rAplicContaModificada);
        etPrestacoes = findViewById(R.id.etPrestacoes); // Corrected to match layout ID
        classificaConta = findViewById(R.id.spClasseConta);
        contaCategoria = findViewById(R.id.spCategoriaConta);
        intervaloRepete = findViewById(R.id.spRepeticoes);
        categoria = findViewById(R.id.layout_categoria);

        nome = findViewById(R.id.acNomeContaModificada);

        pagamento = findViewById(R.id.cbPagamento);
        pagamento.setVisibility(View.GONE);
    }

    private void PegaConta() {
        try (Cursor cursor = dbContaParaEditar.getContaById(idConta)) { // Using getContaById
            if (cursor != null && cursor.moveToFirst()) {
                // Use getColumnIndexOrThrow for robustness
                conta = new Conta(
                        cursor.getLong(cursor.getColumnIndexOrThrow(Colunas._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA)),
                        // COLUNA_CATEGORIA_CONTA is TEXT in DBContas, but int in Conta class. Parse it.
                        Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS)) // Added valorJuros
                );

                dia = conta.getDia();
                mes = conta.getMes();
                ano = conta.getAno();
                tipoConta = conta.getTipo();
                classeConta = conta.getClasseConta();
                categoriaConta = conta.getCategoria();
                novoPagouConta = conta.getPagamento();
                intervalo = conta.getIntervalo();
                qtPrest = conta.getQtRepete();
            }
        } catch (Exception e) {
            // Handle the exception, e.g., log it or show a user message
            e.printStackTrace();
            Toast.makeText(this, "Error loading account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Finish activity if account cannot be loaded to prevent further issues
            finish();
        }
    }

    private void MostraDados() {
        if (conta == null) {
            // If conta is null, there was an error loading it, so we can't display data.
            // This case should be handled by PegaConta() and finishing the activity.
            return;
        }

        nome.setText(conta.getNome());

        Calendar c = Calendar.getInstance();
        c.set(conta.getAno(), conta.getMes(), conta.getDia());
        Locale current = res.getConfiguration().getLocales().get(0);
        DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
        data.setText(dataFormato.format(c.getTime()));

        valor.setText(String.format(Locale.US, "%.2f", conta.getValor()));
        pagamento.setChecked(false);

        if (PAGAMENTO_PAGO.equals(conta.getPagamento())) { // Using constant
            pagamento.setChecked(true);
        }

        ArrayAdapter<String> categoriasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.CategoriaConta));
        ArrayAdapter<String> intervalosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.repete_conta));

        // Update adapters and selection based on account type
        configurarAdaptadoresSpinner(conta.getTipo()); // Pass current account type to configure

        contaCategoria.setAdapter(categoriasAdapter);
        // Category is an integer index, so use the index directly
        if (categoriaConta >= 0 && categoriaConta < categoriasAdapter.getCount()) {
            contaCategoria.setText(categoriasAdapter.getItem(categoriaConta), false);
        }
        intervaloRepete.setAdapter(intervalosAdapter);

        etPrestacoes.setText(String.valueOf(qtPrest)); // Use the correct ID

        // Set repetition interval text based on numeric value
        setIntervaloRepeticaoTexto(intervalo, intervalosAdapter);
    }

    // Helper method to configure adapters and visibility based on account type
    private void configurarAdaptadoresSpinner(int tipoContaAtual) {
        ArrayAdapter<String> classesContasAdapter;
        Resources res = getResources();

        if (tipoContaAtual == 0) { // Expense
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoDespesa));
            rec.setChecked(false);
            aplic.setChecked(false);
            desp.setChecked(true);
            pagamento.setText(R.string.dica_pagamento);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.VISIBLE);
        } else if (tipoContaAtual == 1) { // Revenue
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoReceita));
            rec.setChecked(true);
            aplic.setChecked(false);
            desp.setChecked(false);
            pagamento.setText(R.string.dica_recebe);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.GONE);
        } else { // Application (tipoContaAtual == 2)
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoAplicacao));
            rec.setChecked(false);
            aplic.setChecked(true);
            desp.setChecked(false);
            pagamento.setVisibility(View.GONE);
            categoria.setVisibility(View.GONE);
        }
        // Set adapter for account classification
        classificaConta.setAdapter(classesContasAdapter);
        // Set initial selection of account class
        if (classeConta >= 0 && classeConta < classesContasAdapter.getCount()) {
            classificaConta.setText(classesContasAdapter.getItem(classeConta), false);
        }
    }

    // Helper method to set repetition spinner text
    private void setIntervaloRepeticaoTexto(int intervaloValor, ArrayAdapter<String> adapter) {
        String textoIntervalo = "";
        switch (intervaloValor) {
            case 101: // Daily
                if (adapter.getCount() > 0) textoIntervalo = adapter.getItem(0);
                break;
            case 107: // Weekly
                if (adapter.getCount() > 1) textoIntervalo = adapter.getItem(1);
                break;
            case 300: // Monthly
                if (adapter.getCount() > 2) textoIntervalo = adapter.getItem(2);
                break;
            case 3650: // Annually
                if (adapter.getCount() > 3) textoIntervalo = adapter.getItem(3);
                break;
        }
        if (!textoIntervalo.isEmpty()) {
            intervaloRepete.setText(textoIntervalo, false);
        }
    }

    @Override
    public void onClick(View paramView) {
        int viewId = paramView.getId();
        if (viewId == R.id.etDataConta) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (viewId == R.id.cbPagamento) {
            if (pagamento.isChecked()) {
                novoPagouConta = PAGAMENTO_PAGO; // Using constant
            } else {
                novoPagouConta = PAGAMENTO_FALTA; // Using constant
            }
        }
    }

    private void Dialogo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.dica_menu_edicao));
        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                (dialog, id) -> {
                    if (id == 0) { // Edit only this account
                        nr = 0; // Indicates only the current account will be modified
                    } else if (id == 1) { // Edit this account and future ones
                        nr = conta.getnRepete(); // Sets the repetition number from which future ones will be altered
                    } else if (id == 2) { // Edit all repetitions
                        nr = 1; // Indicates all repetitions will be altered
                    }
                    dialogo.dismiss();
                });
        dialogo = alertDialogBuilder.create();
        dialogo.show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // Update account type based on RadioGroup selection
        if (checkedId == R.id.rDespContaModificada) {
            tipoConta = 0;
        } else if (checkedId == R.id.rRecContaModificada) { // Corrected condition
            tipoConta = 1;
        } else if (checkedId == R.id.rAplicContaModificada) {
            tipoConta = 2;
        }
        // Reconfigure adapters and field visibility based on the new type
        configurarAdaptadoresSpinner(tipoConta);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edita_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_OK, null);
            finish();
            return true;
        } else if (itemId == R.id.menu_edita) {
            ConfereAlteracoesConta();

            if (valorNovoConta == 0.0D) { // Check for invalid value
                Toast.makeText(this, "Invalid account value.", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Update current account data
            conta.setNome(novoNomeConta);
            conta.setTipo(tipoConta);
            conta.setClasseConta(classeConta);
            conta.setCategoria(categoriaConta);
            conta.setDia(dia);
            conta.setMes(mes);
            conta.setAno(ano);
            conta.setValor(valorNovoConta);
            conta.setPagamento(novoPagouConta);
            conta.setQtRepete(qtPrest);
            conta.setIntervalo(intervalo);
            // valorJuros is not updated here; add if needed
            // conta.setValorJuros(valorJurosNovo); // If there's a new juros value

            if (nr == 0) { // Edit only this account
                dbContaParaEditar.updateConta(conta);
            } else { // Edit this and future accounts or all accounts
                TipoAtualizacao tipoAtualizacao;
                TipoExclusao tipoExclusao;

                if (nr == 1) { // All repetitions
                    tipoExclusao = TipoExclusao.TODAS;
                    tipoAtualizacao = TipoAtualizacao.TODAS;
                } else { // This and future repetitions (nr = conta.getnRepete())
                    tipoExclusao = TipoExclusao.ESTA_E_FUTURAS;
                    tipoAtualizacao = TipoAtualizacao.ESTA_E_FUTURAS;
                }

                // First, delete existing series from the starting point
                dbContaParaEditar.deleteContasRecorrentes(conta.getCodigo(), nr, tipoExclusao);

                // Then, insert new series based on the updated 'conta' details
                // The 'conta' object passed to insertContasRecorrentes should represent the first new repetition.
                // Reset nRepete to 1 for the first insertion in the new series.
                conta.setnRepete(1);
                dbContaParaEditar.insertContasRecorrentes(conta, qtPrest, intervalo);

                // Progress bar for regeneration
                if (qtPrest > 1) {
                    new BarraProgresso(this, getResources().getString(
                            R.string.dica_titulo_barra), getResources().getString(
                            R.string.dica_barra_altera), qtPrest, 0, "mskapp").execute();
                }
            }

            altera = 1; // Signal that the account was altered
            setResult(RESULT_OK, null);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void ConfereAlteracoesConta() {

        if (!nome.getText().toString().isEmpty()) {
            novoNomeConta = nome.getText().toString();
        } else {
            novoNomeConta = res.getString(R.string.sem_nome);
        }

        String valorStr = valor.getText().toString().trim();
        if (!valorStr.isEmpty()) {
            try {
                valorNovoConta = Double.parseDouble(valorStr);
            } catch (NumberFormatException e) {
                valorNovoConta = 0.0; // Invalid value
                Toast.makeText(this, "Invalid value for account amount. Setting to 0.", Toast.LENGTH_SHORT).show();
            }
        } else {
            valorNovoConta = 0.0;
        }

        String prestacoesStr = etPrestacoes.getText().toString().trim();
        if (!prestacoesStr.isEmpty()) {
            try {
                qtPrest = Integer.parseInt(prestacoesStr);
            } catch (NumberFormatException e) {
                qtPrest = 1; // If invalid, treat as a single installment
                Toast.makeText(this, "Invalid value for installments. Setting to 1.", Toast.LENGTH_SHORT).show();
            }
        } else {
            qtPrest = 1; // If empty, treat as a single installment
        }

        // If the account is edited as a single instance (nr == 0),
        // and the original number of repetitions was greater than 1,
        // adjust the number of repetitions to 1 for this specific account.
        if (nr == 0) {
            conta.setnRepete(1);
            conta.setQtRepete(1); // Set total repetitions to 1 for a single account
        }
    }

    // This method is no longer strictly necessary with the refactoring using updateContasRecorrentes
    // and deleteContasRecorrentes followed by insertContasRecorrentes. Its logic has been moved
    // and adapted into onOptionsItemSelected. The method can be removed or left as a placeholder.
    private void ModificaContas() {
        // The logic for modifying recurring accounts has been moved to the onOptionsItemSelected method.
        // This method can be safely removed if no other part of the code explicitly calls it.
        /* Example of how the logic would be adapted:
        TipoAtualizacao tipoAtualizacao;
        TipoExclusao tipoExclusao;

        if (nr == 1) { // Todas as repetições
            tipoAtualizacao = TipoAtualizacao.TODAS;
            tipoExclusao = TipoExclusao.TODAS;
        } else { // Esta e futuras repetições
            tipoAtualizacao = TipoAtualizacao.ESTA_E_FUTURAS;
            tipoExclusao = TipoExclusao.ESTA_E_FUTURAS;
        }

        // Excluir a série existente a partir do ponto de partida
        dbContaParaEditar.deleteContasRecorrentes(conta.getCodigo(), nr, tipoExclusao);

        // Inserir a nova série com base nos detalhes da conta atualizada
        conta.setnRepete(1); // Resetar para 1 para a primeira inserção na nova série
        dbContaParaEditar.insertContasRecorrentes(conta, qtPrest, intervalo);

        if (qtPrest > 1) {
            new BarraProgresso(this, getResources().getString(
                    R.string.dica_titulo_barra), getResources().getString(
                    R.string.dica_barra_altera), qtPrest, 0, "mskapp").execute();
        }
        */
    }

    private void usarActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.titulo_editar);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int parentId = parent.getId();
        if (parentId == R.id.spClasseConta) {
            classeConta = position; // Update classeConta variable
            // Adjust visibility of other fields based on selected class (if necessary)
            if (tipoConta == 0) { // Expense
                // Example: If the class is specific for installments
                if (position == 1) { // Assuming class index 1 is for installments
                    etPrestacoes.setText(String.valueOf(120)); // Set a default value
                    qtPrest = 120;
                }
                // Logic to show/hide interest can be added here if it depends on the class
            } else if (tipoConta == 2) { // Application
                pagamento.setVisibility(View.GONE); // Applications generally don't have payment status
            } else { // Revenue
                pagamento.setText(R.string.dica_recebe);
                pagamento.setVisibility(View.VISIBLE);
            }
        } else if (parentId == R.id.spCategoriaConta) {
            categoriaConta = position; // Update categoriaConta variable
        } else if (parentId == R.id.spRepeticoes) {
            // Update interval value based on spinner selection
            switch (position) {
                case 0: // Daily
                    intervalo = 101;
                    break;
                case 1: // Weekly
                    intervalo = 107;
                    break;
                case 2: // Monthly
                    intervalo = 300;
                    break;
                case 3: // Annually
                    intervalo = 3650;
                    break;
            }
        }
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
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        finish();
        super.onBackPressed();
    }

    // DatePicker Fragment
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Create a DatePickerDialog with the current date
            return new DatePickerDialog(getActivity(), this, ano, mes, dia);
        }

        // Callback when the user selects a date
        public void onDateSet(DatePicker view, int mAno, int mMes, int mDia) {
            // Update global date variables
            ano = mAno;
            mes = mMes; // Month is 0-indexed in Calendar, but DatePickerDialog returns correctly
            dia = mDia;

            // Update the date button/field display
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().getLocales().get(0);
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            EditarConta.data.setText(dataFormato.format(dateCalendar.getTime())); // Access 'data' statically
        }
    }
}