package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;

import java.util.ArrayList;
import java.util.Calendar;

@SuppressLint("InflateParams")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ListaMensalContas extends AppCompatActivity implements View.OnClickListener {

    public ArrayList<Long> contas = new ArrayList<Long>();
    DBContas dbContasDoMes = new DBContas(this);
    Calendar c = Calendar.getInstance();
    // BARRA NO TOPO DO APLICATIVO
    ActionMode mActionMode = null;
    ColorDrawable cor;
    Resources r;
    SharedPreferences buscaPreferencias = null;
    // ELEMENTOS DA TELA
    private ImageButton antes, depois, addConta;
    private TextView lista, semContas;
    private ListView listaContas;
    private View lastView;
    // VARIAVEIS UTILIZADAS
    private String[] MESES;
    private int dia, mes, ano, conta;
    private int[] dmaConta;
    private long idConta = 0;
    private String ordemListaDeContas, nomeConta, tipo;
    private double valorConta, valores;
    private AdaptaListaMensal buscaContas;
    private Cursor contasParaLista = null;
    private boolean alteraContas = false;
    private boolean primeiraConta = false;

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.contas_do_mes);

        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        ordemListaDeContas = buscaPreferencias.getString("ordem", "conta ASC");
        buscaPreferencias
                .registerOnSharedPreferenceChangeListener(preferencias);

        // Recupera o mes e o ano da lista anterior
        Bundle localBundle = getIntent().getExtras();
        ano = localBundle.getInt("ano");
        mes = localBundle.getInt("mes");
        tipo = localBundle.getString("tipo");
        r = getResources();
        dbContasDoMes.open();
        iniciar();
        usarActionBar();

        lista.setText(new StringBuilder().append(MESES[mes]).append("/")
                .append(ano));

        MontaLista();

        // Metodos de click em cada um dos itens da tela
        lastView = null;
        listaContas.setOnItemClickListener(toqueSimples);
        listaContas.setOnItemLongClickListener(toqueLongo);
        antes.setOnClickListener(this);
        depois.setOnClickListener(this);

        addConta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.NOVACONTA"), 1);
            }
        });

    }

    private OnSharedPreferenceChangeListener preferencias = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (key.equals("ordem")) {
                dbContasDoMes.open();
                AtualizaActionBar();
                MontaLista();
            }

        }
    };


    private void iniciar() {
        listaContas = ((ListView) findViewById(R.id.lvContasCriadas));
        antes = ((ImageButton) findViewById(R.id.ibMesAntes));
        depois = ((ImageButton) findViewById(R.id.ibMesDepois));
        lista = ((TextView) findViewById(R.id.tvMesLista));
        semContas = (TextView) findViewById(R.id.tvSemContas);
        MESES = getResources().getStringArray(R.array.MesesDoAno);

        addConta = (ImageButton) findViewById(R.id.ibfab);

    }

    @SuppressLint("InflateParams")
    private void MontaLista() {

        dbContasDoMes.open();

        contasParaLista = dbContasDoMes.buscaTodasDoMes(mes, ano,
                buscaPreferencias.getString("ordem", ordemListaDeContas));

        if (tipo.equals("receitas")) {
            contasParaLista = dbContasDoMes.buscaContasTipoDoMes(mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                    getResources().getString(R.string.linha_receita));
        }
        if (tipo.equals("despesas")) {
            contasParaLista = dbContasDoMes.buscaContasTipoDoMes(mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                    getResources().getString(R.string.linha_despesa));
        }

        if (tipo.equals("aplicacoes")) {
            contasParaLista = dbContasDoMes.buscaContasTipoDoMes(mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                    getResources().getString(R.string.linha_aplicacoes));
        }

        int n = contasParaLista.getCount();

        dbContasDoMes.close();

        if (n >= 0) {

            int posicao = listaContas.getFirstVisiblePosition();
            String[] prestacao = r.getStringArray(R.array.TipoDespesa);
            String[] semana = r.getStringArray(R.array.Semana);
            buscaContas = new AdaptaListaMensal(this, contasParaLista,
                    prestacao, semana);
            listaContas.setAdapter(buscaContas);
            listaContas.setEmptyView(semContas);
            listaContas.setSelection(posicao);

        }

        contas = new ArrayList<Long>();
        primeiraConta = false;
        alteraContas = false;
        buscaContas.limpaSelecao();

    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {

            case R.id.ibMesAntes:
                mes = (-1 + this.mes);
                if (mes < 0) {
                    mes = 11;
                    ano = (-1 + this.ano);
                }
                break;

            case R.id.ibMesDepois:
                mes = (1 + this.mes);
                if (mes > 11) {
                    mes = 0;
                    ano = (1 + this.ano);
                }
                break;
        }
        lista.setText(new StringBuilder().append(MESES[mes]).append("/")
                .append(ano));
        AtualizaActionBar();
        MontaLista();
        if (mActionMode != null)
            mActionMode.finish();

        setResult(RESULT_OK, null);
    }

    OnItemClickListener toqueSimples = new OnItemClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int posicao,
                                long arg3) {

            dbContasDoMes.open();
            contasParaLista.moveToPosition(posicao);
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            dbContasDoMes.close();

            if (alteraContas == false) {
                if (mActionMode == null) {
                    buscaContas.limpaSelecao();
                    contas = new ArrayList<Long>();
                    buscaContas.marcaConta(posicao, true);
                } else {
                    buscaContas.marcaConta(conta, false);
                    buscaContas.marcaConta(posicao, true);
                }
                mActionMode = ListaMensalContas.this
                        .startSupportActionMode(alteraUmaConta);
                lastView = v;
                conta = posicao;

            } else {

                if (contas.size() != 0) {

                    if (contas.contains(idConta)) {
                        if (primeiraConta == false) {
                            contas.remove(idConta);
                            buscaContas.marcaConta(posicao, false);

                        } else {
                            primeiraConta = false;
                        }

                    } else {
                        contas.add(idConta);
                        buscaContas.marcaConta(posicao, true);

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

    ActionMode.Callback alteraUmaConta = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_lista, menu);
            mode.setTitle(nomeConta);
            addConta.setVisibility(View.GONE);

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
                        Bundle localBundle = new Bundle();
                        localBundle.putLong("id", idConta);
                        Intent localIntent = new Intent(
                                "com.msk.minhascontas.EDITACONTA");
                        localIntent.putExtras(localBundle);
                        startActivityForResult(localIntent, 1);
                    }
                    setResult(RESULT_OK, null);
                    mode.finish();
                    break;
                case R.id.botao_pagar:
                    if (idConta != 0) {

                        dbContasDoMes.open();

                        String pg = dbContasDoMes
                                .mostraPagamentoConta(idConta);
                        if (pg.equals("paguei")) {
                            dbContasDoMes.alteraPagamentoConta(
                                    idConta, "falta");
                        } else {
                            dbContasDoMes.alteraPagamentoConta(
                                    idConta, "paguei");
                        }
                        dbContasDoMes.close();

                    }
                    setResult(RESULT_OK, null);
                    mode.finish();
                    break;
                case R.id.botao_excluir:
                    if (idConta != 0) {
                        dbContasDoMes.open();
                        String nomeContaExcluir = dbContasDoMes
                                .mostraNomeConta(idConta);
                        int qtRepeteConta = dbContasDoMes
                                .quantasContasPorNome(nomeContaExcluir);
                        long ii = dbContasDoMes
                                .mostraPrimeiraRepeticaoConta(
                                        nomeContaExcluir,
                                        qtRepeteConta);
                        String dataAntiga = dbContasDoMes
                                .mostraDataConta(ii);
                        dbContasDoMes.atualizaDataContas(
                                nomeContaExcluir, dataAntiga,
                                qtRepeteConta);

                        if (qtRepeteConta == 1) {
                            // Exclui a unica conta
                            dbContasDoMes.excluiConta(idConta);
                            Toast.makeText(
                                    getApplicationContext(),
                                    getResources()
                                            .getString(
                                                    R.string.dica_conta_excluida,
                                                    nomeContaExcluir),
                                    Toast.LENGTH_SHORT).show();
                            idConta = 0;
                        } else {
                            // Exclui uma ou mais repeticoes da
                            // conta
                            Dialogo();
                        }
                        dbContasDoMes.close();
                    }
                    mode.finish();
                    break;
                case R.id.botao_lembrete:
                    if (idConta != 0) {
                        dbContasDoMes.open();
                        dmaConta = dbContasDoMes
                                .mostraDMAConta(idConta);
                        dia = dmaConta[0];
                        mes = dmaConta[1];
                        ano = dmaConta[2];

                        valorConta = dbContasDoMes
                                .mostraValorConta(idConta);
                        String nomeContaCalendario = r.getString(
                                R.string.dica_evento, dbContasDoMes
                                        .mostraNomeConta(idConta));
                        dbContasDoMes.close();
                        c.set(ano, mes, dia);
                        Intent evento = new Intent(
                                Intent.ACTION_EDIT);
                        evento.setType("vnd.android.cursor.item/event");
                        evento.putExtra(Events.TITLE,
                                nomeContaCalendario);
                        evento.putExtra(Events.DESCRIPTION, r
                                .getString(
                                        R.string.dica_calendario,
                                        String.format("%.2f",
                                                valorConta)));

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
                        startActivity(evento);
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
            AtualizaActionBar();
            //lastView.setBackgroundColor(Color.WHITE);
            buscaContas.marcaConta(conta, false);
            addConta.setVisibility(View.VISIBLE);
        }
    };

    private void Dialogo() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.TemaDialogo));

        // set title
        dialogoBuilder.setTitle(getString(R.string.dica_menu_exclusao));

        // set dialog message
        dialogoBuilder.setItems(R.array.TipoAjusteConta,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dbContasDoMes.open();
                        String nomeContaExcluir = dbContasDoMes
                                .mostraNomeConta(idConta);
                        String dmaContaExcluir = dbContasDoMes
                                .mostraDataConta(idConta);
                        switch (id) {
                            case 0:
                                dbContasDoMes.excluiConta(idConta);
                                break;
                            case 1:
                                int[] repete = dbContasDoMes
                                        .mostraRepeticaoConta(idConta);
                                int nr = repete[1];
                                dbContasDoMes.excluiSerieContaPorNome(
                                        nomeContaExcluir, dmaContaExcluir, nr);
                                break;
                            case 2:
                                dbContasDoMes.excluiContaPorNome(nomeContaExcluir,
                                        dmaContaExcluir);
                                break;
                        }
                        Toast.makeText(
                                getApplicationContext(),
                                getResources().getString(
                                        R.string.dica_conta_excluida,
                                        nomeContaExcluir), Toast.LENGTH_SHORT)
                                .show();
                        buscaContas.notifyDataSetChanged();
                        dbContasDoMes.close();
                        MontaLista();
                        idConta = 0;
                        nomeConta = " ";
                        setResult(RESULT_OK, null);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = dialogoBuilder.create();
        // show it
        alertDialog.show();
    }

    AdapterView.OnItemLongClickListener toqueLongo = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int posicao, long id) {

            // Limpa a barra de titulo
            if (mActionMode != null)
                mActionMode.finish();

            // Limpa a selecao de contas
            buscaContas.limpaSelecao();
            contas = new ArrayList<Long>();
            primeiraConta = true;
            alteraContas = true;

            // Busca informacoes da conta no DB
            dbContasDoMes.open();
            contasParaLista.moveToPosition(posicao);
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            dbContasDoMes.close();

            // Seleciona conta
            contas.add(idConta);
            buscaContas.marcaConta(posicao, true);

            // Mostra selecao na barra de titulo
            mActionMode = ListaMensalContas.this
                    .startSupportActionMode(alteraVariasContas);

            return false;
        }
    };

    ActionMode.Callback alteraVariasContas = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_contas, menu);
            addConta.setVisibility(View.GONE);

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

                        dbContasDoMes.open();

                        for (int i = 0; i < contas.size(); i++) {

                            String pg = dbContasDoMes
                                    .mostraPagamentoConta(contas.get(i));
                            if (pg.equals("paguei")) {
                                dbContasDoMes.alteraPagamentoConta(
                                        contas.get(i), "falta");
                            } else {
                                dbContasDoMes.alteraPagamentoConta(
                                        contas.get(i), "paguei");
                            }
                        }

                        dbContasDoMes.close();

                    }
                    setResult(RESULT_OK, null);
                    mode.finish();
                    break;
                case R.id.botao_excluir:

                    if (contas.size() != 0) {

                        dbContasDoMes.open();

                        for (int i = 0; i < contas.size(); i++) {
                            dbContasDoMes.excluiConta(idConta);
                        }

                        dbContasDoMes.close();
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
            AtualizaActionBar();
            addConta.setVisibility(View.VISIBLE);
            buscaContas.limpaSelecao();
            contas = new ArrayList<Long>();
            alteraContas = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            dbContasDoMes.open();
            AtualizaActionBar();
            MontaLista();

        }
    }

    @SuppressLint("NewApi")
    private void usarActionBar() {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        AtualizaActionBar();

    }

    private void AtualizaActionBar() {
        dbContasDoMes.open();
        if (tipo.equals("receitas")) {
            if (dbContasDoMes.quantasContasPorTipo(
                    r.getString(R.string.linha_receita), 0, mes, ano) > 0)
                valores = dbContasDoMes.somaContas(
                        r.getString(R.string.linha_receita), 0, mes, ano);
            else
                valores = 0.0D;

            cor = new ColorDrawable(Color.parseColor("#FF0099CC"));
            getSupportActionBar().setBackgroundDrawable(cor);
            getSupportActionBar().setTitle(R.string.linha_receita);
            getSupportActionBar().setSubtitle(r.getString(R.string.dica_dinheiro,
                    String.format("%.2f", valores)));

        }

        if (tipo.equals("despesas")) {
            if (dbContasDoMes.quantasContasPorTipo(
                    r.getString(R.string.linha_despesa), 0, mes, ano) > 0)
                valores = dbContasDoMes.somaContas(
                        r.getString(R.string.linha_despesa), 0, mes, ano);
            else
                valores = 0.0D;
            getSupportActionBar().setTitle(R.string.linha_despesa);
            getSupportActionBar().setSubtitle(r.getString(R.string.dica_dinheiro,
                    String.format("%.2f", valores)));
            cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
            getSupportActionBar().setBackgroundDrawable(cor);

        }

        if (tipo.equals("aplicacoes")) {
            if (dbContasDoMes.quantasContasPorTipo(
                    r.getString(R.string.linha_aplicacoes), 0, mes, ano) > 0)
                valores = dbContasDoMes.somaContas(
                        r.getString(R.string.linha_aplicacoes), 0, mes, ano);
            else
                valores = 0.0D;
            getSupportActionBar().setTitle(R.string.linha_aplicacoes);
            getSupportActionBar().setSubtitle(r.getString(R.string.dica_dinheiro,
                    String.format("%.2f", valores)));
            cor = new ColorDrawable(Color.parseColor("#FF669900"));
            getSupportActionBar().setBackgroundDrawable(cor);

        }
        // dbContasDoMes.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (tipo.equals("aplicacoes") || tipo.equals("despesas")) {

            getMenuInflater().inflate(R.menu.barra_botoes_filtra_lista, menu);
        } else {

            getMenuInflater().inflate(R.menu.barra_botoes_lista, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_OK, null);
                dbContasDoMes.close();
                finish();
                break;
            case R.id.menu_ajustes:
                setResult(RESULT_OK, null);
                startActivityForResult(new Intent(this, Ajustes.class), 0);
                break;
            case R.id.menu_sobre:
                setResult(RESULT_OK, null);
                startActivity(new Intent("com.msk.minhascontas.SOBRE"));
                break;
            case R.id.botao_pesquisar:
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.BUSCACONTA"), 2);
                break;
            case R.id.botao_filtrar:

                FiltroContas();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void FiltroContas() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.TemaDialogo));

        // set title
        dialogoBuilder.setTitle(getString(R.string.titulo_filtro));

        if (tipo.equals("despesas")) {

            // set dialog message
            dialogoBuilder.setItems(R.array.TipoDespesa,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dbContasDoMes.open();
                            // DEFINE CONTEUDO LISTA COM FILTRO
                            String[] classes = getResources().getStringArray(R.array.TipoDespesa);
                            contasParaLista = dbContasDoMes.buscaContasClasseDoMes(mes, ano,
                                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                                    getResources().getString(R.string.linha_despesa), classes[id]);

                            String[] prestacao = r.getStringArray(R.array.TipoDespesa);
                            String[] semana = r.getStringArray(R.array.Semana);
                            buscaContas = new AdaptaListaMensal(getApplication(), contasParaLista,
                                    prestacao, semana);
                            listaContas.setAdapter(buscaContas);

                            // DEFINE TITULO LISTA COM FILTRO
                            if (dbContasDoMes.quantasContasPorClasse(
                                    classes[id], 0, mes, ano) > 0)
                                valores = dbContasDoMes.somaContasPorClasse(
                                        classes[id], 0, mes, ano);
                            else
                                valores = 0.0D;
                            getSupportActionBar().setTitle(classes[id]);
                            getSupportActionBar().setSubtitle(r.getString(R.string.dica_dinheiro,
                                    String.format("%.2f", valores)));

                            dbContasDoMes.close();
                            listaContas.setEmptyView(semContas);

                        }
                    });
        }

        if (tipo.equals("aplicacoes")) {
            // set dialog message
            dialogoBuilder.setItems(R.array.GraficoAplicacoes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            dbContasDoMes.open();
                            // define conteudo lista com filtro
                            String[] classes = getResources().getStringArray(R.array.GraficoAplicacoes);
                            contasParaLista = dbContasDoMes.buscaContasClasseDoMes(mes, ano,
                                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                                    getResources().getString(R.string.linha_aplicacoes), classes[id]);

                            String[] prestacao = r.getStringArray(R.array.TipoDespesa);
                            String[] semana = r.getStringArray(R.array.Semana);
                            buscaContas = new AdaptaListaMensal(getApplication(), contasParaLista,
                                    prestacao, semana);

                            listaContas.setAdapter(buscaContas);

                            // DEFINE TITULO LISTA COM FILTRO
                            if (dbContasDoMes.quantasContasPorClasse(
                                    classes[id], 0, mes, ano) > 0)
                                valores = dbContasDoMes.somaContasPorClasse(
                                        classes[id], 0, mes, ano);
                            else
                                valores = 0.0D;
                            getSupportActionBar().setTitle(classes[id]);
                            getSupportActionBar().setSubtitle(r.getString(R.string.dica_dinheiro,
                                    String.format("%.2f", valores)));

                            dbContasDoMes.close();
                            listaContas.setEmptyView(semContas);

                        }
                    });
        }

        // create alert dialog
        AlertDialog alertDialog = dialogoBuilder.create();
        // show it
        alertDialog.show();
    }


    protected void onRestart() {
        dbContasDoMes.open();
        AtualizaActionBar();
        // MontaLista();
        super.onRestart();
    }

    protected void onResume() {
        dbContasDoMes.open();
        AtualizaActionBar();
        // MontaLista();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        dbContasDoMes.close();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        setResult(RESULT_OK, null);
        dbContasDoMes.close();
        super.onDestroy();
    }

}