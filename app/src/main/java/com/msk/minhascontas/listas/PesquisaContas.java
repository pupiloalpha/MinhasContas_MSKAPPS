package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.Toolbar;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("NewApi")
public class PesquisaContas extends AppCompatActivity {

    private DBContas dbContasPesquisadas = new DBContas(this);
    private Calendar c = Calendar.getInstance();
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
    private String nomeBuscado, nomeConta;
    private int conta;
    private ActionMode.Callback alteraUmaConta = new ActionMode.Callback() {

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

            switch (item.getItemId()) {
                case R.id.botao_editar:

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
                    break;
                case R.id.botao_excluir:

                    if (idConta != 0) {
                        dbContasPesquisadas.open();
                        String nomeContaExcluir = dbContasPesquisadas
                                .mostraNomeConta(idConta);
                        int qtRepeteConta = dbContasPesquisadas
                                .quantasContasPorNome(nomeContaExcluir);
                        String dataAntiga = dbContasPesquisadas
                                .mostraCodigoConta(idConta);
                        long ii = dbContasPesquisadas
                                .mostraPrimeiraRepeticaoConta(
                                        nomeContaExcluir,
                                        qtRepeteConta, dataAntiga);
                        dataAntiga = dbContasPesquisadas.mostraCodigoConta(ii);
                        dbContasPesquisadas.atualizaDataContas(
                                nomeContaExcluir, dataAntiga,
                                qtRepeteConta);

                        if (qtRepeteConta == 1) {
                            // Exclui a unica conta
                            dbContasPesquisadas
                                    .excluiConta(idConta);
                            Toast.makeText(
                                    PesquisaContas.this
                                            .getApplicationContext(),
                                    getResources()
                                            .getString(
                                                    R.string.dica_conta_excluida,
                                                    nomeContaExcluir),
                                    Toast.LENGTH_SHORT).show();
                            idConta = 0;
                        } else {
                            // Exclui todas as repeticoes da conta
                            Dialogo();
                        }
                        dbContasPesquisadas.close();
                        buscaContas.notifyDataSetChanged();
                        MontaLista();
                        setResult(RESULT_OK, null);
                    }
                    break;
                case R.id.botao_pagar:
                    if (idConta != 0) {

                        dbContasPesquisadas.open();
                        String pg = dbContasPesquisadas
                                .mostraPagamentoConta(idConta);
                        if (pg.equals("paguei")) {
                            dbContasPesquisadas
                                    .alteraPagamentoConta(idConta,
                                            "falta");
                        } else {
                            dbContasPesquisadas
                                    .alteraPagamentoConta(idConta,
                                            "paguei");
                        }
                        dbContasPesquisadas.close();
                        idConta = 0;

                        buscaContas.notifyDataSetChanged();
                        MontaLista();
                        setResult(RESULT_OK, null);
                    }
                    break;
                case R.id.botao_lembrete:
                    if (idConta != 0) {
                        dbContasPesquisadas.open();
                        int[] dmaConta = dbContasPesquisadas
                                .mostraDMAConta(idConta);
                        int dia = dmaConta[0];
                        int mes = dmaConta[1];
                        int ano = dmaConta[2];

                        double valorConta = dbContasPesquisadas
                                .mostraValorConta(idConta);
                        String nomeContaCalendario = r.getString(
                                R.string.dica_evento,
                                dbContasPesquisadas
                                        .mostraNomeConta(idConta));
                        dbContasPesquisadas.close();
                        c.set(ano, mes, dia);
                        Intent evento = new Intent(
                                Intent.ACTION_EDIT);
                        evento.setType("vnd.android.cursor.item/event");
                        evento.putExtra(Events.TITLE,
                                nomeContaCalendario);
                        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(r.getConfiguration().locale);
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
                    break;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            buscaContas.marcaConta(conta, false);
        }
    };
    private OnItemClickListener toqueSimples = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adaptView, View v, int posicao,
                                long arg3) {

            dbContasPesquisadas.open();
            contasParaLista.moveToPosition(posicao);
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            dbContasPesquisadas.close();

            if (!alteraContas) {
                if (mActionMode == null) {
                    buscaContas.limpaSelecao();
                    contas = new ArrayList<Long>();
                    buscaContas.marcaConta(posicao, true);
                    mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                    lastView = v;
                    conta = posicao;
                } else {
                    buscaContas.marcaConta(conta, false);
                    buscaContas.marcaConta(posicao, true);
                    if (posicao != conta) {
                        mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                        lastView = v;
                        conta = posicao;
                    } else {
                        mActionMode.finish();
                        MontaLista();
                    }
                }
            } else {

                if (contas.size() != 0) {

                    if (contas.contains(idConta)) {
                        if (!primeiraConta) {
                            contas.remove(idConta);
                            buscaContas.marcaConta(posicao, false);
                            //v.setSelected(false);
                        } else {
                            primeiraConta = false;
                        }

                    } else {
                        contas.add(idConta);
                        buscaContas.marcaConta(posicao, true);
                        //v.setSelected(true);
                    }

                    if (contas.size() == 0) {
                        mActionMode.finish();
                        MontaLista();
                    }

                    if (contas.size() != 0)
                        mActionMode.setTitle(r.getQuantityString(R.plurals.selecao,
                                contas.size(), contas.size()));
                }

            }
        }

    };
    private ActionMode.Callback alteraVariasContas = new ActionMode.Callback() {
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

            switch (item.getItemId()) {
                case R.id.botao_pagar:
                    if (contas.size() != 0) {
                        dbContasPesquisadas.open();
                        for (int i = 0; i < contas.size(); i++) {
                            String pg = dbContasPesquisadas
                                    .mostraPagamentoConta(contas.get(i));
                            if (pg.equals("paguei")) {
                                dbContasPesquisadas.alteraPagamentoConta(
                                        contas.get(i), "falta");
                            } else {
                                dbContasPesquisadas.alteraPagamentoConta(
                                        contas.get(i), "paguei");
                            }
                        }
                        dbContasPesquisadas.close();
                    }
                    setResult(RESULT_OK, null);
                    mode.finish();
                    break;
                case R.id.botao_excluir:
                    if (contas.size() != 0) {
                        dbContasPesquisadas.open();
                        for (int i = 0; i < contas.size(); i++) {
                            dbContasPesquisadas.excluiConta(contas.get(i));
                        }
                        dbContasPesquisadas.close();
                    }
                    mode.finish();
                    break;
            }
            MontaLista();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            buscaContas.limpaSelecao();
            contas = new ArrayList<Long>();
            alteraContas = false;
        }
    };
    private AdapterView.OnItemLongClickListener toqueLongo = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int posicao, long id) {

            // Limpa a barra de titulo
            if (mActionMode != null)
                mActionMode.finish();

            // Limpa a selecao de contas
            buscaContas.limpaSelecao();
            contas = new ArrayList<Long>();
            alteraContas = true;
            primeiraConta = true;

            // Busca informacoes da conta no DB
            dbContasPesquisadas.open();
            contasParaLista.moveToPosition(posicao);
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            dbContasPesquisadas.close();

            // Seleciona conta
            contas.add(idConta);
            buscaContas.marcaConta(posicao, true);

            // Mostra selecao na barra de titulo
            mActionMode = PesquisaContas.this
                    .startSupportActionMode(alteraVariasContas);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pesquisa_conta);
        r = getResources();
        dbContasPesquisadas.open();
        iniciar();
        usarActionBar();
        MontaAutoCompleta();
        lastView = null;
        listaContas.setOnItemClickListener(toqueSimples);
        listaContas.setOnItemLongClickListener(toqueLongo);
    }

    private void iniciar() {

        listaContas = findViewById(R.id.lvContasPesquisadas);
        resultado = findViewById(R.id.tvSemResultados);
        listaContas.setEmptyView(resultado);
        nomeContaBuscar = findViewById(R.id.acNomeContaBusca);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void MontaAutoCompleta() {
        dbContasPesquisadas.open();
        ArrayAdapter completa;

        if (dbContasPesquisadas.quantasContas() != 0) {
            completa = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line,
                    dbContasPesquisadas.mostraNomeContas());
        } else {
            completa = new ArrayAdapter(this,
                    android.R.layout.simple_dropdown_item_1line, getResources()
                    .getStringArray(R.array.NomeConta));
        }
        dbContasPesquisadas.close();
        nomeContaBuscar.setAdapter(completa);
    }

    private void MontaLista() {
        dbContasPesquisadas.open();
        contasParaLista = dbContasPesquisadas.buscaContasPorNome(nomeBuscado);
        int i = contasParaLista.getCount();
        dbContasPesquisadas.close();
        if (i >= 0) {

            int posicao = listaContas.getFirstVisiblePosition();
            buscaContas = new AdaptaListaPesquisa(this, contasParaLista);

            listaContas.setAdapter(buscaContas);
            listaContas.setEmptyView(resultado);
            listaContas.setSelection(posicao);
        }
        contas = new ArrayList<Long>();
        alteraContas = false;
        primeiraConta = false;
        buscaContas.limpaSelecao();
    }

    private void Dialogo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getString(R.string.dica_menu_exclusao));

        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dbContasPesquisadas.open();
                        String nomeContaExcluir = dbContasPesquisadas
                                .mostraNomeConta(idConta);
                        String dataContaExcluir = dbContasPesquisadas
                                .mostraCodigoConta(idConta);
                        switch (id) {
                            case 0:
                                dbContasPesquisadas.excluiConta(idConta);
                                break;
                            case 1:
                                int[] repete = dbContasPesquisadas
                                        .mostraRepeticaoConta(idConta);
                                int nr = repete[1];
                                dbContasPesquisadas.excluiSerieContaPorNome(
                                        nomeContaExcluir, dataContaExcluir, nr);
                                break;
                            case 2:
                                dbContasPesquisadas.excluiContaPorNome(
                                        nomeContaExcluir, dataContaExcluir);
                                break;
                        }
                        Toast.makeText(
                                PesquisaContas.this.getApplicationContext(),
                                getResources().getString(
                                        R.string.dica_conta_excluida,
                                        nomeContaExcluir), Toast.LENGTH_SHORT)
                                .show();

                        buscaContas.notifyDataSetChanged();
                        MontaLista();
                        MontaAutoCompleta();
                        dbContasPesquisadas.close();
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pesquisa_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbContasPesquisadas.close();
                finish();
                break;
            case R.id.botao_pesquisar:
                if (nomeContaBuscar.getText().toString().equals(""))
                    nomeBuscado = " ";
                else {
                    nomeBuscado = nomeContaBuscar.getText().toString();
                }
                MontaLista();
                MontaAutoCompleta();
                nomeContaBuscar.setText("");
                idConta = 0;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            dbContasPesquisadas.open();
            buscaContas.notifyDataSetChanged();
            MontaLista();
            idConta = 0;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        dbContasPesquisadas.close();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        setResult(RESULT_OK, null);
        dbContasPesquisadas.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        dbContasPesquisadas.open();
        super.onResume();
    }

}
