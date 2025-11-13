package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.Conta; // Added import for Conta

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

@SuppressLint("InflateParams")
@RequiresApi(android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ListaMensalContas extends Fragment {

    public static ActionMode mActionMode = null;
    public ArrayList<Long> contas = new ArrayList<>();
    private DBContas dbContasDoMes;
    private final Calendar c = Calendar.getInstance();
    // BARRA NO TOPO DO APLICATIVO
    private Resources res;
    private SharedPreferences buscaPreferencias = null;
    private NumberFormat dinheiro;

    // ELEMENTOS DA TELA
    private TextView semContas;
    private ListView listaContas;

    // VARIAVEIS UTILIZADAS
    private int mes, ano, conta, tipo, filtro;
    private long idConta = 0;
    private String ordemListaDeContas, nomeConta;
    private AdaptaListaMensal buscaContas;
    private Cursor contasParaLista = null;
    private boolean alteraContas = false;
    private boolean primeiraConta = false;
    private double valorConta = 0.0D;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencias = (sharedPreferences, key) -> {
        if ("ordem".equals(key)) {
            MontaLista();
        }
    };

    private final ActionMode.Callback alteraUmaConta = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_altera_lista, menu);
            mode.setTitle(nomeConta);
            if (PaginadorListas.addConta != null) {
                PaginadorListas.addConta.setVisibility(View.GONE);
            }
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
                    Bundle localBundle = new Bundle();
                    localBundle.putLong("id", idConta);
                    Intent localIntent = new Intent(
                            "com.msk.minhascontas.EDITACONTA");
                    localIntent.putExtras(localBundle);
                    startActivity(localIntent);
                }
                mode.finish();
            } else if (itemId == R.id.botao_pagar) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasDoMes.getContaById(idConta)) {
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
                            dbContasDoMes.updateConta(contaToUpdate);
                        }
                    }
                }
                mode.finish();
            } else if (itemId == R.id.botao_excluir) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasDoMes.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            String codigoConta = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                            int nrRepete = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);
                            dbContasDoMes.deleteContasRecorrentes(codigoConta, nrRepete, DBContas.TipoExclusao.APENAS_ESTA);
                        }
                    }
                }
                mode.finish();
            } else if (itemId == R.id.botao_lembrete) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasDoMes.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            int dia = getColumnInt(cursor, DBContas.Colunas.COLUNA_DIA_DATA_CONTA);
                            mes = getColumnInt(cursor, DBContas.Colunas.COLUNA_MES_DATA_CONTA);
                            ano = getColumnInt(cursor, DBContas.Colunas.COLUNA_ANO_DATA_CONTA);
                            double valorConta = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_CONTA);
                            String nomeContaCalendario = res.getString(
                                    R.string.dica_evento, getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA));
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
                    }
                }
                mode.finish();
            }
            MontaLista();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (buscaContas != null && conta < buscaContas.getCount()) {
                buscaContas.marcaConta(conta, false);
            }
            if (PaginadorListas.addConta != null) {
                PaginadorListas.addConta.setVisibility(View.GONE);
            }
            if (getActivity() instanceof PaginadorListas) {
                ((PaginadorListas) getActivity()).AtualizaActionBar();
            }
        }
    };
    private final AdapterView.OnItemClickListener toqueSimples = (arg0, v, posicao, arg3) -> {

        if (contasParaLista != null && contasParaLista.moveToPosition(posicao)) {
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            double vConta = contasParaLista.getDouble(8);

            if (!alteraContas) {
                if (mActionMode == null) {
                    buscaContas.limpaSelecao();
                    contas = new ArrayList<>();
                    buscaContas.marcaConta(posicao, true);
                    AppCompatActivity act = (AppCompatActivity) getActivity();
                    if (act != null) {
                        mActionMode = act.startSupportActionMode(alteraUmaConta);
                    }
                    conta = posicao;
                } else {
                    buscaContas.marcaConta(conta, false);
                    buscaContas.marcaConta(posicao, true);
                    if (posicao != conta) {
                        AppCompatActivity act = (AppCompatActivity) getActivity();
                        if (act != null) {
                            mActionMode = act.startSupportActionMode(alteraUmaConta);
                        }
                        conta = posicao;
                    } else {
                        mActionMode.finish();
                        MontaLista();
                    }
                }
            } else {
                if (!contas.isEmpty()) {
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

                    if (contas.isEmpty()) {
                        mActionMode.finish();
                        MontaLista();
                    }

                    if (!contas.isEmpty()) {
                        mActionMode.setTitle(res.getQuantityString(R.plurals.selecao,
                                contas.size(), contas.size()));
                        if (tipo != -1)
                            mActionMode.setSubtitle(dinheiro.format(valorConta));
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
            if (PaginadorListas.addConta != null) {
                PaginadorListas.addConta.setVisibility(View.GONE);
            }
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
                        try (Cursor cursor = dbContasDoMes.getContaById(id)) {
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
                                dbContasDoMes.updateConta(contaToUpdate);
                            }
                        }
                    }
                }
                mode.finish();
            } else if (itemId == R.id.botao_excluir) {
                if (!contas.isEmpty()) {
                    for (int i = 0; i < contas.size(); i++) {
                        dbContasDoMes.deleteContaById(contas.get(i));
                    }
                }
                mode.finish();
            }
            MontaLista();
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
            valorConta = 0.0D;
            if (PaginadorListas.addConta != null) {
                PaginadorListas.addConta.setVisibility(View.VISIBLE);
            }
            if (getActivity() instanceof PaginadorListas) {
                ((PaginadorListas) getActivity()).AtualizaActionBar();
            }
        }
    };
    private final AdapterView.OnItemLongClickListener toqueLongo = (parent, view, posicao, id) -> {

        if (mActionMode != null)
            mActionMode.finish();

        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
        contas = new ArrayList<>();
        primeiraConta = true;
        alteraContas = true;

        if (contasParaLista != null && contasParaLista.moveToPosition(posicao)) {
            idConta = contasParaLista.getLong(0);
            nomeConta = contasParaLista.getString(1);
            contas.add(idConta);
            if (buscaContas != null) {
                buscaContas.marcaConta(posicao, true);
            }

            AppCompatActivity act = (AppCompatActivity) getActivity();
            if (act != null) {
                mActionMode = act.startSupportActionMode(alteraVariasContas);
            }
        }
        return false;
    };

    public static ListaMensalContas newInstance(int mes, int ano, int tipo, int filtro) {
        ListaMensalContas fragment = new ListaMensalContas();
        Bundle args = new Bundle();
        args.putInt("ano", ano);
        args.putInt("mes", mes);
        args.putInt("tipo", tipo);
        args.putInt("filtro", filtro);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbContasDoMes = DBContas.getInstance(context);
        buscaPreferencias = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.contas_do_mes, container, false);
        ordemListaDeContas = buscaPreferencias.getString("ordem", "nome ASC");
        buscaPreferencias
                .registerOnSharedPreferenceChangeListener(preferencias);

        Bundle bundle = getArguments();
        if (bundle != null) {
            ano = bundle.getInt("ano");
            mes = bundle.getInt("mes");
            tipo = bundle.getInt("tipo");
            filtro = bundle.getInt("filtro");
        }

        res = requireActivity().getResources();
        Locale current = res.getConfiguration().getLocales().get(0);
        dinheiro = NumberFormat.getCurrencyInstance(current);

        listaContas = rootView.findViewById(R.id.lvContasCriadas);
        semContas = rootView.findViewById(R.id.tvSemContas);

        MontaLista();

        listaContas.setOnItemClickListener(toqueSimples);
        listaContas.setOnItemLongClickListener(toqueLongo);
        return rootView;
    }

    private void MontaLista() {
        String ordem = buscaPreferencias.getString("ordem", ordemListaDeContas);

        if (contasParaLista != null) {
            contasParaLista.close();
        }

        DBContas.ContaFilter filter = new DBContas.ContaFilter()
                .setMes(mes)
                .setAno(ano);

        if (tipo != -1) {
            filter.setTipo(tipo);
            if (filtro >= 0) {
                if (filtro == 4) { // Payment status "falta"
                    filter.setPagamento(DBContas.PAGAMENTO_FALTA);
                } else if (filtro == 5) { // Payment status "paguei"
                    filter.setPagamento(DBContas.PAGAMENTO_PAGO);
                } else { // Filter by class
                    filter.setClasse(filtro);
                }
            }
        }
        contasParaLista = dbContasDoMes.getContasByFilter(filter, ordem); // Replaced all buscaContas methods

        if (contasParaLista.getCount() >= 0) {
            int posicao = listaContas.getFirstVisiblePosition();
            buscaContas = new AdaptaListaMensal(getActivity(), contasParaLista);
            listaContas.setAdapter(buscaContas);
            listaContas.setEmptyView(semContas);
            listaContas.setSelection(posicao);
        }

        contas = new ArrayList<>();
        primeiraConta = false;
        alteraContas = false;
        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
    }

    @SuppressWarnings("unused")
    private void Dialogo() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(requireActivity());
        dialogoBuilder.setTitle(getString(R.string.dica_menu_exclusao));
        dialogoBuilder.setItems(R.array.TipoAjusteConta,
                (dialog, id) -> {
                    try (Cursor cursor = dbContasDoMes.getContaById(idConta)) {
                        if (cursor.moveToFirst()) {
                            String nomeContaExcluir = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                            String codigoConta = getColumnString(cursor, DBContas.Colunas.COLUNA_CODIGO_CONTA);
                            if (id == 0) { // Delete only this account
                                dbContasDoMes.deleteContaById(idConta);
                            } else if (id == 1) { // Delete this and future recurring accounts
                                int nr = getColumnInt(cursor, DBContas.Colunas.COLUNA_NR_REPETICAO_CONTA);
                                dbContasDoMes.deleteContasRecorrentes(codigoConta, nr, DBContas.TipoExclusao.ESTA_E_FUTURAS);
                            } else if (id == 2) { // Delete all recurring accounts with the same code
                                dbContasDoMes.deleteContasRecorrentes(codigoConta, 1, DBContas.TipoExclusao.TODAS);
                            }
                            Toast.makeText(
                                    getActivity(),
                                    getResources().getString(
                                            R.string.dica_conta_excluida,
                                            nomeContaExcluir), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    if (buscaContas != null) {
                        buscaContas.notifyDataSetChanged();
                    }
                    MontaLista();
                    idConta = 0;
                    nomeConta = " ";
                });
        AlertDialog alertDialog = dialogoBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        MontaLista();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (contasParaLista != null) {
            contasParaLista.close();
        }
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
