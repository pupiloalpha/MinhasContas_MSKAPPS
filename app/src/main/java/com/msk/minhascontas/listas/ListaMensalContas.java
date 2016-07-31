package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

@SuppressLint("InflateParams")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ListaMensalContas extends Fragment {

    public static ActionMode mActionMode = null;
    public ArrayList<Long> contas = new ArrayList<Long>();
    private DBContas dbContasDoMes;
    private Calendar c = Calendar.getInstance();
    // BARRA NO TOPO DO APLICATIVO
    private Bundle dados_mes = new Bundle();
    private Resources res;
    private SharedPreferences buscaPreferencias = null;
    private NumberFormat dinheiro;

    // ELEMENTOS DA TELA
    private TextView semContas;
    private ListView listaContas;

    // VARIAVEIS UTILIZADAS
    private String[] prestacao, semana, classes;
    private int mes, ano, conta;
    private long idConta = 0;
    private String ordemListaDeContas, nomeConta, tipo, filtro;
    private AdaptaListaMensal buscaContas;
    private Cursor contasParaLista = null;
    private boolean alteraContas = false;
    private boolean primeiraConta = false;
    private double valorConta = 0.0D;
    private OnSharedPreferenceChangeListener preferencias = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (key.equals("ordem")) {
                dbContasDoMes.open();
                MontaLista();
            }
        }
    };

    private ActionMode.Callback alteraUmaConta = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_lista, menu);
            mode.setTitle(nomeConta);
            PaginadorListas.addConta.setVisibility(View.GONE);
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
                        startActivity(localIntent);
                    }
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
                    mode.finish();
                    break;
                case R.id.botao_excluir:
                    if (idConta != 0) {
                        dbContasDoMes.open();
                        String nomeContaExcluir = dbContasDoMes
                                .mostraNomeConta(idConta);
                        int qtRepeteConta = dbContasDoMes
                                .quantasContasPorNome(nomeContaExcluir);
                        String dataAntiga = dbContasDoMes
                                .mostraCodigoConta(idConta);
                        long ii = dbContasDoMes
                                .mostraPrimeiraRepeticaoConta(
                                        nomeContaExcluir,
                                        qtRepeteConta, dataAntiga);
                        dataAntiga = dbContasDoMes.mostraCodigoConta(ii);
                        dbContasDoMes.atualizaDataContas(
                                nomeContaExcluir, dataAntiga,
                                qtRepeteConta);

                        if (qtRepeteConta == 1) {
                            // Exclui a unica conta
                            dbContasDoMes.excluiConta(idConta);
                            Toast.makeText(
                                    getActivity(),
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
                        int[] dmaConta = dbContasDoMes
                                .mostraDMAConta(idConta);
                        int dia = dmaConta[0];
                        mes = dmaConta[1];
                        ano = dmaConta[2];
                        double valorConta = dbContasDoMes
                                .mostraValorConta(idConta);
                        String nomeContaCalendario = res.getString(
                                R.string.dica_evento, dbContasDoMes
                                        .mostraNomeConta(idConta));
                        dbContasDoMes.close();
                        c.set(ano, mes, dia);
                        Intent evento = new Intent(
                                Intent.ACTION_EDIT);
                        evento.setType("vnd.android.cursor.item/event");
                        evento.putExtra(Events.TITLE,
                                nomeContaCalendario);
                        evento.putExtra(Events.DESCRIPTION, res
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
            buscaContas.marcaConta(conta, false);
            PaginadorListas.addConta.setVisibility(View.VISIBLE);
            ((PaginadorListas) getActivity()).AtualizaActionBar();
        }
    };
    private OnItemClickListener toqueSimples = new OnItemClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int posicao,
                                long arg3) {

            dbContasDoMes.open();
            contasParaLista.moveToPosition(posicao);
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            double vConta = contasParaLista.getDouble(9);
            dbContasDoMes.close();

            if (!alteraContas) {
                if (mActionMode == null) {
                    buscaContas.limpaSelecao();
                    contas = new ArrayList<Long>();
                    buscaContas.marcaConta(posicao, true);
                    AppCompatActivity act = (AppCompatActivity) getActivity();
                    mActionMode = act.startSupportActionMode(alteraUmaConta);
                    conta = posicao;
                } else {
                    buscaContas.marcaConta(conta, false);
                    buscaContas.marcaConta(posicao, true);
                    if (posicao != conta) {
                        AppCompatActivity act = (AppCompatActivity) getActivity();
                        mActionMode = act.startSupportActionMode(alteraUmaConta);
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
                            valorConta = valorConta - vConta;
                        } else {
                            primeiraConta = false;
                            valorConta = valorConta + vConta;
                        }
                    } else {
                        contas.add(idConta);
                        buscaContas.marcaConta(posicao, true);
                        valorConta = valorConta + vConta;
                    }

                    if (contas.size() == 0) {
                        mActionMode.finish();
                        MontaLista();
                    }

                    if (contas.size() != 0) {
                        mActionMode.setTitle(res.getQuantityString(R.plurals.selecao,
                                contas.size(), contas.size()));
                        if (!tipo.equals("todas"))
                            mActionMode.setSubtitle(dinheiro.format(valorConta));
                    }
                }
            }
        }
    };
    private ActionMode.Callback alteraVariasContas = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_contas, menu);
            PaginadorListas.addConta.setVisibility(View.GONE);
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
                    mode.finish();
                    break;
                case R.id.botao_excluir:
                    if (contas.size() != 0) {
                        dbContasDoMes.open();
                        for (int i = 0; i < contas.size(); i++) {
                            dbContasDoMes.excluiConta(contas.get(i));
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
            buscaContas.limpaSelecao();
            contas = new ArrayList<Long>();
            alteraContas = false;
            valorConta = 0.0D;
            PaginadorListas.addConta.setVisibility(View.VISIBLE);
            ((PaginadorListas) getActivity()).AtualizaActionBar();
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
            AppCompatActivity act = (AppCompatActivity) getActivity();
            mActionMode = act.startSupportActionMode(alteraVariasContas);
            return false;
        }
    };

    public static ListaMensalContas newInstance(int mes, int ano, String tipo, String filtro) {
        ListaMensalContas fragment = new ListaMensalContas();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        args.putString("tipo", tipo);
        args.putString("filtro", filtro);
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
        dbContasDoMes = new DBContas(activity);
        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // COLOCA OS MESES NA TELA
        View rootView = inflater.inflate(R.layout.contas_do_mes, container, false);
        ordemListaDeContas = buscaPreferencias.getString("ordem", "conta ASC");
        buscaPreferencias
                .registerOnSharedPreferenceChangeListener(preferencias);

        // Recupera o mes e o ano da lista anterior
        Bundle bundle = getArguments();
        ano = bundle.getInt("ano");
        mes = bundle.getInt("mes");
        tipo = bundle.getString("tipo");
        filtro = bundle.getString("filtro");

        res = getActivity().getResources();
        Locale current = res.getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);
        dbContasDoMes.open();

        listaContas = ((ListView) rootView.findViewById(R.id.lvContasCriadas));
        semContas = (TextView) rootView.findViewById(R.id.tvSemContas);

        prestacao = res.getStringArray(R.array.TipoDespesa);
        semana = res.getStringArray(R.array.Semana);

        MontaLista();

        // Metodos de click em cada um dos itens da tela
        listaContas.setOnItemClickListener(toqueSimples);
        listaContas.setOnItemLongClickListener(toqueLongo);
        return rootView;
    }

    @SuppressLint("InflateParams")
    private void MontaLista() {

        dbContasDoMes.open();
        if (tipo.equals("todas")) {
            contasParaLista = dbContasDoMes.buscaContas(0, mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas));
        } else {
            contasParaLista = dbContasDoMes.buscaContasTipo(0, mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                    tipo);
        }

        if (!filtro.equals("")) {
            contasParaLista = dbContasDoMes.buscaContasClasse(0, mes, ano,
                    buscaPreferencias.getString("ordem", ordemListaDeContas),
                    tipo, filtro);

            if (filtro.equals("paguei") || filtro.equals("falta")) {
                contasParaLista = dbContasDoMes.buscaContasTipoPagamento(0, mes, ano,
                        buscaPreferencias.getString("ordem", ordemListaDeContas),
                        tipo, filtro);
            }
        }

        int n = contasParaLista.getCount();

        dbContasDoMes.close();
        if (n >= 0) {
            int posicao = listaContas.getFirstVisiblePosition();
            buscaContas = new AdaptaListaMensal(getActivity(), contasParaLista,
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

    private void Dialogo() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(getActivity());
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
                                .mostraCodigoConta(idConta);
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
                                getActivity(),
                                getResources().getString(
                                        R.string.dica_conta_excluida,
                                        nomeContaExcluir), Toast.LENGTH_SHORT)
                                .show();
                        buscaContas.notifyDataSetChanged();
                        dbContasDoMes.close();
                        MontaLista();
                        idConta = 0;
                        nomeConta = " ";
                    }
                });
        // create alert dialog
        AlertDialog alertDialog = dialogoBuilder.create();
        // show it
        alertDialog.show();
    }

    @Override
    public void onResume() {
        MontaLista();
        super.onResume();
    }
}
