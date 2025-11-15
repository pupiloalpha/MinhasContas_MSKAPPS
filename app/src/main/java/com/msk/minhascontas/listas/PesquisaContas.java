package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.Toolbar;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.Conta; // Added import for Conta

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("NewApi")
public class PesquisaContas extends AppCompatActivity {

    private DBContas dbContasPesquisadas;
    private final Calendar c = Calendar.getInstance();
    private Resources r;
    private ActionMode mActionMode;
    // ELEMENTOS DA TELA
    private ListView listaContas;
    private View lastView;
    private TextView resultado;
    private AppCompatAutoCompleteTextView nomeContaBuscar;
    // ELEMENTOS PARA MONTAR A LISTA
    private AdaptaListaPesquisa buscaContas;
    private Cursor contasParaLista = null;
    @SuppressWarnings("rawtypes")
    // VARIAVEIS
    private long idConta = 0;
    private ArrayList<Long> contas = new ArrayList<Long>();
    private boolean alteraContas = false;
    private boolean primeiraConta = false;
    private String nomeBuscado; // Removed nomeConta from here
    private String nomeConta; // Re-declared nomeConta to avoid confusion with the selected item name
    private int conta;
    private final ActionMode.Callback alteraUmaConta = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_lista, menu);
            mode.setTitle(nomeConta);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.botao_editar) {

                if (idConta != 0) {
                    Bundle nova = new Bundle();
                    nova.putLong("id", idConta);
                    Intent app = new Intent(
                            "com.msk.minhascontas.EDITACONTA");
                    app.putExtras(nova);
                    PesquisaContas.this.startActivityForResult(
                            app, 1);
                }
                setResult(RESULT_OK, null);
            } else if (itemId == R.id.botao_excluir) {

                if (idConta != 0) {
                    try (Cursor cursor = dbContasPesquisadas.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            String codigoConta = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                            int nrRepete = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);
                            dbContasPesquisadas.deleteContasRecorrentes(codigoConta, nrRepete, DBContas.TipoExclusao.APENAS_ESTA);
                        }
                    }
                    if (buscaContas != null) {
                        buscaContas.notifyDataSetChanged();
                    }
                    MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
                    setResult(RESULT_OK, null);
                }
            } else if (itemId == R.id.botao_pagar) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasPesquisadas.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            long currentId = getColumnLong(cursor, DBContas.Colunas._ID);
                            String currentNome = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                            int currentTipo = getColumnInt(cursor, DBContas.Colunas.COLUNA_TIPO_CONTA);
                            int currentClasse = getColumnInt(cursor, DBContas.Colunas.COLUNA_CLASSE_CONTA);
                            int currentCategoria = getColumnInt(cursor, DBContas.Colunas.COLUNA_CATEGORIA_CONTA);
                            int currentDia = getColumnInt(cursor, DBContas.Colunas.COLUNA_DIA_DATA_CONTA);
                            int currentMes = getColumnInt(cursor, DBContas.Colunas.COLUNA_MES_DATA_CONTA);
                            int currentAno = getColumnInt(cursor, DBContas.Colunas.COLUNA_ANO_DATA_CONTA);
                            double currentValor = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_CONTA);
                            String currentPagamento = getColumnString(cursor, DBContas.Colunas.COLUNA_PAGOU_CONTA);
                            int currentQtRepeticoes = getColumnInt(cursor, DBContas.Colunas.COLUNA_QT_REPETICOES_CONTA);
                            int currentNrRepeticao = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);
                            int currentIntervalo = getColumnInt(cursor, DBContas.Colunas.COLUNA_INTERVALO_CONTA);
                            String currentCodigo = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                            double currentValorJuros = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_JUROS);

                            Conta contaToUpdate = new Conta(currentId, currentNome, currentTipo, currentClasse, currentCategoria,
                                    currentDia, currentMes, currentAno, currentValor, currentPagamento,
                                    currentQtRepeticoes, currentNrRepeticao, currentIntervalo, currentCodigo, currentValorJuros);

                            if (DBContas.PAGAMENTO_PAGO.equals(currentPagamento)) {
                                contaToUpdate.setPagamento(DBContas.PAGAMENTO_FALTA);
                            } else {
                                contaToUpdate.setPagamento(DBContas.PAGAMENTO_PAGO);
                            }
                            dbContasPesquisadas.updateConta(contaToUpdate);
                        }
                    }
                    idConta = 0;

                    if (buscaContas != null) {
                        buscaContas.notifyDataSetChanged();
                    }
                    MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
                    setResult(RESULT_OK, null);
                }
            } else if (itemId == R.id.botao_lembrete) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasPesquisadas.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            int dia = getColumnInt(cursor, DBContas.Colunas.COLUNA_DIA_DATA_CONTA);
                            int mes = getColumnInt(cursor, DBContas.Colunas.COLUNA_MES_DATA_CONTA);
                            int ano = getColumnInt(cursor, DBContas.Colunas.COLUNA_ANO_DATA_CONTA);
                            double valorConta = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_CONTA);
                            String nomeContaCalendario = r.getString(
                                    R.string.dica_evento,
                                    getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA));

                            c.set(ano, mes, dia);
                            Intent evento = new Intent(
                                    Intent.ACTION_EDIT);
                            evento.setType("vnd.android.cursor.item/event");
                            evento.putExtra(Events.TITLE,
                                    nomeContaCalendario);
                            NumberFormat dinheiro = NumberFormat.getCurrencyInstance(r.getConfiguration().getLocales().get(0));
                            evento.putExtra(Events.DESCRIPTION, r
                                    .getString(
                                            R.string.dica_calendario,
                                            dinheiro.format(valorConta)));

                            evento.putExtra(
                                    CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    c.getTimeInMillis());
                            evento.putExtra(
                                    CalendarContract.EXTRA_EVENT_END_TIME,
                                    c.getTimeInMillis());

                            evento.putExtra(Events.ACCESS_LEVEL,
                                    Events.ACCESS_PRIVATE);
                            evento.putExtra(Events.AVAILABILITY,
                                    Events.AVAILABILITY_BUSY);
                            setResult(RESULT_OK, null);
                            startActivity(evento);
                        }
                    }
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (buscaContas != null) {
                buscaContas.marcaConta(conta, false);
            }
        }
    };
    private final OnItemClickListener toqueSimples = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adaptView, View v, int posicao,
                                long arg3) {

            if (contasParaLista != null && contasParaLista.moveToPosition(posicao)) {
                idConta = contasParaLista.getLong(0);
                nomeConta = contasParaLista.getString(1);

                if (!alteraContas) {
                    if (mActionMode == null) {
                        if (buscaContas != null) {
                            buscaContas.limpaSelecao();
                        }
                        contas = new ArrayList<>();
                        if (buscaContas != null) {
                            buscaContas.marcaConta(posicao, true);
                        }
                        mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                        lastView = v;
                        conta = posicao;
                    } else {
                        if (buscaContas != null) {
                            buscaContas.marcaConta(conta, false);
                            buscaContas.marcaConta(posicao, true);
                        }
                        if (posicao != conta) {
                            mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                            lastView = v;
                            conta = posicao;
                        } else {
                            mActionMode.finish();
                            MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
                        }
                    }
                } else {

                    if (!contas.isEmpty()) {

                        if (contas.contains(idConta)) {
                            if (!primeiraConta) {
                                contas.remove(idConta);
                                if (buscaContas != null) {
                                    buscaContas.marcaConta(posicao, false);
                                }
                                //v.setSelected(false);
                            } else {
                                primeiraConta = false;
                            }

                        } else {
                            contas.add(idConta);
                            if (buscaContas != null) {
                                buscaContas.marcaConta(posicao, true);
                            }
                            //v.setSelected(true);
                        }

                        if (contas.isEmpty()) {
                            mActionMode.finish();
                            MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
                        }

                        if (!contas.isEmpty())
                            mActionMode.setTitle(r.getQuantityString(R.plurals.selecao,
                                    contas.size(), contas.size()));
                    }

                }
            }
        }

    };
    private final ActionMode.Callback alteraVariasContas = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_contas, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.botao_pagar) {
                if (!contas.isEmpty()) {
                    for (int i = 0; i < contas.size(); i++) {
                        long id = contas.get(i);
                        try (Cursor cursor = dbContasPesquisadas.getContaById(id)) {
                            if (cursor.moveToFirst()) {
                                long currentId = getColumnLong(cursor, DBContas.Colunas._ID);
                                String currentNome = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                                int currentTipo = getColumnInt(cursor, DBContas.Colunas.COLUNA_TIPO_CONTA);
                                int currentClasse = getColumnInt(cursor, DBContas.Colunas.COLUNA_CLASSE_CONTA);
                                int currentCategoria = getColumnInt(cursor, DBContas.Colunas.COLUNA_CATEGORIA_CONTA);
                                int currentDia = getColumnInt(cursor, DBContas.Colunas.COLUNA_DIA_DATA_CONTA);
                                int currentMes = getColumnInt(cursor, DBContas.Colunas.COLUNA_MES_DATA_CONTA);
                                int currentAno = getColumnInt(cursor, DBContas.Colunas.COLUNA_ANO_DATA_CONTA);
                                double currentValor = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_CONTA);
                                String currentPagamento = getColumnString(cursor, DBContas.Colunas.COLUNA_PAGOU_CONTA);
                                int currentQtRepeticoes = getColumnInt(cursor, DBContas.Colunas.COLUNA_QT_REPETICOES_CONTA);
                                int currentNrRepeticao = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);
                                int currentIntervalo = getColumnInt(cursor, DBContas.Colunas.COLUNA_INTERVALO_CONTA);
                                String currentCodigo = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                                double currentValorJuros = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_JUROS);

                                Conta contaToUpdate = new Conta(currentId, currentNome, currentTipo, currentClasse, currentCategoria,
                                        currentDia, currentMes, currentAno, currentValor, currentPagamento,
                                        currentQtRepeticoes, currentNrRepeticao, currentIntervalo, currentCodigo, currentValorJuros);

                                if (DBContas.PAGAMENTO_PAGO.equals(currentPagamento)) {
                                    contaToUpdate.setPagamento(DBContas.PAGAMENTO_FALTA);
                                } else {
                                    contaToUpdate.setPagamento(DBContas.PAGAMENTO_PAGO);
                                }
                                dbContasPesquisadas.updateConta(contaToUpdate);
                            }
                        }
                    }
                }
                setResult(RESULT_OK, null);
                mode.finish();
            } else if (itemId == R.id.botao_excluir) {
                if (!contas.isEmpty()) {
                    for (int i = 0; i < contas.size(); i++) {
                        dbContasPesquisadas.deleteContaById(contas.get(i));
                    }
                }
                mode.finish();
            }
            MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (buscaContas != null) {
                buscaContas.limpaSelecao();
            }
            contas = new ArrayList<>();
            alteraContas = false;
        }
    };
    private final AdapterView.OnItemLongClickListener toqueLongo = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int posicao, long id) {

            // Limpa a barra de titulo
            if (mActionMode != null)
                mActionMode.finish();

            // Limpa a selecao de contas
            if (buscaContas != null) {
                buscaContas.limpaSelecao();
            }
            contas = new ArrayList<>();
            alteraContas = true;
            primeiraConta = true;

            // Busca informacoes da conta no DB
            if (contasParaLista != null && contasParaLista.moveToPosition(posicao)) {
                idConta = contasParaLista.getLong(0);
                nomeConta = contasParaLista.getString(1);

                // Seleciona conta
                contas.add(idConta);
                if (buscaContas != null) {
                    buscaContas.marcaConta(posicao, true);
                }

                // Mostra selecao na barra de titulo
                mActionMode = PesquisaContas.this
                        .startSupportActionMode(alteraVariasContas);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pesquisa_conta);
        r = getResources();
        dbContasPesquisadas = DBContas.getInstance(this);
        iniciar();
        usarActionBar();
        MontaAutoCompleta();
        lastView = null;
        listaContas.setOnItemClickListener(toqueSimples);
        listaContas.setOnItemLongClickListener(toqueLongo);

        // Initial list load, no filter applied
        MontaLista(null);

        // Add listener for autocomplete item click
        nomeContaBuscar.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MontaLista(nomeContaBuscar.getText().toString());
            }
        });

        // Add listener for keyboard "Done" action
        nomeContaBuscar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    MontaLista(nomeContaBuscar.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void iniciar() {

        listaContas = findViewById(R.id.lvContasPesquisadas);
        resultado = findViewById(R.id.tvSemResultados);
        listaContas.setEmptyView(resultado);
        nomeContaBuscar = findViewById(R.id.acNomeContaBusca);
    }

    private void MontaAutoCompleta() {
        // Fetch all distinct account names from the database
        Set<String> accountNames = new HashSet<>();
        try (Cursor cursor = dbContasPesquisadas.getContasByFilter(new DBContas.ContaFilter(), null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                    if (name != null) {
                        accountNames.add(name);
                    }
                } while (cursor.moveToNext());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(accountNames));
        nomeContaBuscar.setAdapter(adapter);
    }

    private void MontaLista(String filterName) {
        if (contasParaLista != null) {
            contasParaLista.close();
            contasParaLista = null; // Ensure it's null after closing
        }

        DBContas.ContaFilter filter = new DBContas.ContaFilter();
        if (filterName != null && !filterName.trim().isEmpty()) {
            filter.setNomeConta(filterName);
        }

        contasParaLista = dbContasPesquisadas.getContasByFilter(filter, null);
        int i = contasParaLista.getCount();
        if (i >= 0) {

            int posicao = listaContas.getFirstVisiblePosition();
            buscaContas = new AdaptaListaPesquisa(this, contasParaLista);

            listaContas.setAdapter(buscaContas);
            listaContas.setEmptyView(resultado);
            listaContas.setSelection(posicao);
        }
        contas = new ArrayList<>();
        alteraContas = false;
        primeiraConta = false;
        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
    }

    private void Dialogo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getString(R.string.dica_menu_exclusao));

        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try (Cursor cursor = dbContasPesquisadas.getContaById(idConta)) {
                            if (cursor.moveToFirst()) {
                                String nomeContaExcluir = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                                String codigoConta = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                                int nr = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);

                                if (id == 0) { // Delete only this account
                                    dbContasPesquisadas.deleteContaById(idConta);
                                } else if (id == 1) { // Delete this and future recurring accounts
                                    dbContasPesquisadas.deleteContasRecorrentes(
                                            codigoConta, nr, DBContas.TipoExclusao.ESTA_E_FUTURAS);
                                } else if (id == 2) { // Delete all recurring accounts with the same code
                                    dbContasPesquisadas.deleteContasRecorrentes(
                                            codigoConta, 1, DBContas.TipoExclusao.TODAS);
                                }
                                Toast.makeText(
                                        PesquisaContas.this.getApplicationContext(),
                                        getResources().getString(
                                                R.string.dica_conta_excluida,
                                                nomeContaExcluir), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }

                        if (buscaContas != null) {
                            buscaContas.notifyDataSetChanged();
                        }
                        MontaLista(nomeContaBuscar.getText().toString()); // Pass current search term
                        MontaAutoCompleta();
                        idConta = 0;
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    @SuppressLint("NewApi")
    private void usarActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pesquisa_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_OK, null);
            finish();
        } else if (itemId == R.id.botao_pesquisar) {
            // Now the search button will filter the list based on the text in nomeContaBuscar
            MontaLista(nomeContaBuscar.getText().toString());
            // We don't clear the text here, as the user might want to refine the search.
            // If the user wants to see all accounts, they can clear the text and press search again.
            idConta = 0;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (mActionMode != null) {
                mActionMode.finish(); // Dismiss any active action mode
            }
            if (buscaContas != null) {
                buscaContas.notifyDataSetChanged();
            }
            MontaLista(nomeContaBuscar.getText().toString()); // Refresh list with current filter
            idConta = 0;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        super.onBackPressed();
    }

    // Helper methods to safely retrieve data from Cursor
    private int getColumnInt(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getInt(columnIndex) : 0;
    }

    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getString(columnIndex) : null;
    }

    private double getColumnDouble(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getDouble(columnIndex) : 0.0D;
    }

    private long getColumnLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getLong(columnIndex) : 0L;
    }
}