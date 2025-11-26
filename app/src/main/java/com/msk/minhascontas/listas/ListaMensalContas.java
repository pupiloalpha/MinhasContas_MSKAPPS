package com.msk.minhascontas.listas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.db.ContasContract;
import com.msk.minhascontas.db.DBContas;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

public class ListaMensalContas extends Fragment {

    // region 1. Campos (Fields)
    // =============================================================================================
    public static ActionMode mActionMode = null;
    public ArrayList<Long> contas = new ArrayList<>(); // Armazena os IDs das contas selecionadas

    private DBContas dbContasDoMes;
    private final Calendar c = Calendar.getInstance();
    private Resources res;
    private SharedPreferences buscaPreferencias = null;
    private NumberFormat dinheiro;

    // Elementos da UI
    private TextView semContas;
    private RecyclerView listaContas;

    // Variáveis de estado
    private int mes, ano, conta, tipo, filtro;
    private long idConta = 0;
    private String ordemListaDeContas, nomeConta;
    private AdaptaListaMensalRC buscaContas;
    private Cursor contasParaLista = null;
    private boolean alteraContas = false;
    private boolean primeiraConta = false;
    private double valorConta = 0.0D;
    // endregion

    // region 2. Método de Fábrica Estático (Static Factory Method)
    // =============================================================================================
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
    // endregion

    // region 3. Métodos de Ciclo de Vida (Lifecycle Methods)
    // =============================================================================================
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
        ordemListaDeContas = buscaPreferencias.getString("ordem", ContasContract.Colunas.COLUNA_NOME_CONTA + " ASC");
        buscaPreferencias
                .registerOnSharedPreferenceChangeListener(preferencias);

        Bundle bundle = getArguments();
        if (bundle != null) {
            Log.d("MINHAS_CONTAS", "ListaMensalContas.onCreateView: Obtendo argumentos do bundle.");
            ano = bundle.getInt("ano");
            mes = bundle.getInt("mes");
            tipo = bundle.getInt("tipo");
            filtro = bundle.getInt("filtro");
            Log.d("MINHAS_CONTAS", "ListaMensalContas.onCreateView: mes=" + mes + ", ano=" + ano + ", tipo=" + tipo + ", filtro=" + filtro);

        } else {
            // IMPORTANT: If no arguments are passed, ensure default month is 1-indexed
            Calendar c = Calendar.getInstance();
            mes = c.get(Calendar.MONTH) + 1; // Correct to 1-indexed for database
            ano = c.get(Calendar.YEAR);
            tipo = -1; // Default to no type filter
            filtro = -1; // Default to no class/payment filter
            Log.d("MINHAS_CONTAS", "ListaMensalContas.onCreateView: Bundle NULO. Usando defaults: mes=" + mes + ", ano=" + ano + ", tipo=" + tipo + ", filtro=" + filtro);
        }

        res = requireActivity().getResources();
        // Acesso moderno ao Locale
        Locale current;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            current = res.getConfiguration().getLocales().get(0);
        } else {
            current = res.getConfiguration().locale;
        }
        dinheiro = NumberFormat.getCurrencyInstance(current);

        listaContas = rootView.findViewById(R.id.lvContasCriadas);
        listaContas.setLayoutManager(new LinearLayoutManager(requireContext()));

        semContas = rootView.findViewById(R.id.tvSemContas);

        MontaLista();

        return rootView;
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
    // endregion

    // region 4. Métodos Públicos/Package-Private
    // =============================================================================================
    public void updateFilter(int newFiltro) {
        this.filtro = newFiltro;
        MontaLista();
    }

    public void refreshLista() {
        MontaLista();
    }
    // endregion

    // region 5. Callbacks de ActionMode
    // =============================================================================================
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
                    try (Cursor cursor = dbContasDoMes.getContaPeloId(idConta)) {
                        if (cursor.moveToFirst()) {
                            long currentId = getColumnLong(cursor, ContasContract.Colunas._ID);
                            String currentNome = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
                            int currentTipo = getColumnInt(cursor, ContasContract.Colunas.COLUNA_TIPO_CONTA);
                            int currentClasse = getColumnInt(cursor, ContasContract.Colunas.COLUNA_CLASSE_CONTA);
                            int currentCategoria = getColumnInt(cursor, ContasContract.Colunas.COLUNA_CATEGORIA_CONTA);
                            int currentDia = getColumnInt(cursor, ContasContract.Colunas.COLUNA_DIA_DATA_CONTA);
                            int currentMes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_MES_DATA_CONTA);
                            int currentAno = getColumnInt(cursor, ContasContract.Colunas.COLUNA_ANO_DATA_CONTA);
                            double currentValor = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
                            String currentPagamento = getColumnString(cursor, ContasContract.Colunas.COLUNA_PAGOU_CONTA);
                            int currentQtRepeticoes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_QT_REPETICOES_CONTA);
                            int currentNrRepeticao = getColumnInt(cursor, ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA);
                            int currentIntervalo = getColumnInt(cursor, ContasContract.Colunas.COLUNA_INTERVALO_CONTA);
                            String currentCodigo = getColumnString(cursor, ContasContract.Colunas.COLUNA_CODIGO_CONTA);
                            double currentValorJuros = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_JUROS);

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
                    try (Cursor cursor = dbContasDoMes.getContaPeloId(idConta)) {
                        if (cursor.moveToFirst()) {
                            String codigoConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_CODIGO_CONTA);
                            int nrRepete = getColumnInt(cursor, ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA);
                            dbContasDoMes.deleteContasRecorrentes(codigoConta, nrRepete, DBContas.TipoExclusao.SOMENTE_ESTA);
                        }
                    }
                }
                mode.finish();
            } else if (itemId == R.id.botao_lembrete) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasDoMes.getContaPeloId(idConta)) {
                        if (cursor.moveToFirst()) {
                            int dia = getColumnInt(cursor, ContasContract.Colunas.COLUNA_DIA_DATA_CONTA);
                            mes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_MES_DATA_CONTA);
                            ano = getColumnInt(cursor, ContasContract.Colunas.COLUNA_ANO_DATA_CONTA);
                            double valorConta = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
                            String nomeContaCalendario = res.getString(
                                    R.string.dica_evento, getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA));
                            c.set(ano, mes, dia);
                            Intent evento = new Intent(
                                    Intent.ACTION_EDIT);
                            evento.setType("vnd.android.cursor.item/event");
                            evento.putExtra(Events.TITLE,
                                    nomeContaCalendario);

                            // Acesso moderno ao Locale
                            Locale current;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                current = res.getConfiguration().getLocales().get(0);
                            } else {
                                current = res.getConfiguration().locale;
                            }
                            NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

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
            if (buscaContas != null) {
                // Limpa todas as seleções para garantir o estado visual correto
                buscaContas.limpaSelecao();
            }
            if (PaginadorListas.addConta != null) {
                PaginadorListas.addConta.setVisibility(View.GONE);
            }
            if (getActivity() instanceof PaginadorListas) {
                ((PaginadorListas) getActivity()).AtualizaActionBar();
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
                    for (long id : contas) { // Iteração moderna
                        try (Cursor cursor = dbContasDoMes.getContaPeloId(id)) {
                            if (cursor.moveToFirst()) {
                                long currentId = getColumnLong(cursor, ContasContract.Colunas._ID);
                                String currentNome = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
                                int currentTipo = getColumnInt(cursor, ContasContract.Colunas.COLUNA_TIPO_CONTA);
                                int currentClasse = getColumnInt(cursor, ContasContract.Colunas.COLUNA_CLASSE_CONTA);
                                int currentCategoria = getColumnInt(cursor, ContasContract.Colunas.COLUNA_CATEGORIA_CONTA);
                                int currentDia = getColumnInt(cursor, ContasContract.Colunas.COLUNA_DIA_DATA_CONTA);
                                int currentMes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_MES_DATA_CONTA);
                                int currentAno = getColumnInt(cursor, ContasContract.Colunas.COLUNA_ANO_DATA_CONTA);
                                double currentValor = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
                                String currentPagamento = getColumnString(cursor, ContasContract.Colunas.COLUNA_PAGOU_CONTA);
                                int currentQtRepeticoes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_QT_REPETICOES_CONTA);
                                int currentNrRepeticao = getColumnInt(cursor, ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA);
                                int currentIntervalo = getColumnInt(cursor, ContasContract.Colunas.COLUNA_INTERVALO_CONTA);
                                String currentCodigo = getColumnString(cursor, ContasContract.Colunas.COLUNA_CODIGO_CONTA);
                                double currentValorJuros = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_JUROS);

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
                    Iterator<Long> iterator = contas.iterator(); // Uso de Iterator para evitar ConcurrentModificationException
                    while (iterator.hasNext()) {
                        dbContasDoMes.deleteContaById(iterator.next());
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
                // Limpa todas as seleções para garantir o estado visual correto
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
    // endregion

    // region 6. Listeners (Ouvintes)
    // =============================================================================================
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencias = (sharedPreferences, key) -> {
        if ("ordem".equals(key)) {
            MontaLista();
        }
    };
    // endregion

    // region 7. Lógica Principal (Core Logic Methods)
    // =============================================================================================
    private void MontaLista() {

        String ordem = buscaPreferencias.getString("ordem", ordemListaDeContas);
        // Em ListaMensalContas.java, no início de MontaLista()
        Log.d("MINHAS_CONTAS", "MontaLista() - Iniciando busca. Mes=" + mes + ", Ano=" + ano + ", Tipo=" + tipo + ", Filtro=" + filtro + ", Ordem=" + ordem);

        if (contasParaLista != null) {
            contasParaLista.close();
        }

        DBContas.ContaFilter filter = new DBContas.ContaFilter();
        filter.setMes(mes)
                .setAno(ano);

        if (tipo != -1) {
            filter.setTipo(tipo);
            if (filtro >= 0) {
                if (tipo == 0 && filtro == 4) { // Despesa Payment status "falta"
                    filter.setPagamento(DBContas.PAGAMENTO_FALTA);
                } else if (tipo == 0 && filtro == 5) { // Despesa Payment status "paguei"
                    filter.setPagamento(DBContas.PAGAMENTO_PAGO);
                } else if (tipo == 1 && filtro == 3) { // Receita Payment status "falta"
                    filter.setPagamento(DBContas.PAGAMENTO_FALTA);
                } else if (tipo == 1 && filtro == 4) { // Receita Payment status "paguei"
                    filter.setPagamento(DBContas.PAGAMENTO_PAGO);

                } else { // Filter by class
                    filter.setClasse(filtro);
                }
            }
        }
        contasParaLista = dbContasDoMes.getContasByFilter(filter, ordem);

        if (contasParaLista == null) {
            Log.e("MINHAS_CONTAS", "MontaLista() - ERRO: Cursor retornado é NULL!");
        } else {
            Log.d("MINHAS_CONTAS", "MontaLista() - Cursor retornado com " + contasParaLista.getCount() + " registros.");
        }


        if (contasParaLista != null && contasParaLista.getCount() >= 0) {

            if (buscaContas == null) {
                // Primeira inicialização: cria o Adapter e define o Listener
                buscaContas = new AdaptaListaMensalRC(requireContext(), contasParaLista);

                // Configuração do Listener de clique/toque longo para o RecyclerView
                buscaContas.setOnItemClickListener(new AdaptaListaMensalRC.OnItemClickListener() {
                    @Override
                    public void onItemClick(long id, int position) {
                        handleItemClick(id, position);
                    }

                    @Override
                    public boolean onItemLongClick(long id, int position) {
                        return handleItemLongClick(id, position);
                    }
                });

                listaContas.setAdapter(buscaContas);
            } else {
                // Atualização: apenas troca o Cursor
                buscaContas.swapCursor(contasParaLista);
            }

            // Tratamento manual para Empty View (RecyclerView não suporta setEmptyView diretamente)
            if (contasParaLista.getCount() == 0) {
                Log.d("MINHAS_CONTAS", "MontaLista() - Nenhum registro encontrado. Exibindo tvSemContas.");

                listaContas.setVisibility(View.GONE);
                semContas.setVisibility(View.VISIBLE);
            } else {
                Log.d("MINHAS_CONTAS", "MontaLista() - Registros encontrados. Exibindo listaContas.");

                listaContas.setVisibility(View.VISIBLE);
                semContas.setVisibility(View.GONE);
            }
        }

        contas = new ArrayList<>();
        primeiraConta = false;
        alteraContas = false;
        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
    }
    // endregion

    // region 8. Handlers de Interação (Interaction Handlers)
    // =============================================================================================
    /**
     * Lógica de toque simples (seleção individual ou toggle de multi-seleção).
     */
    private void handleItemClick(long id, int position) {
        Cursor cursor = buscaContas.getItem(position);
        if (cursor != null) {
            idConta = id;
            nomeConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
            double vConta = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);

            if (!alteraContas) {
                // Modo de seleção individual (alteraUmaConta)
                if (mActionMode == null) {
                    // Inicia a seleção
                    buscaContas.limpaSelecao();
                    contas.clear();
                    buscaContas.marcaConta(id, true);

                    AppCompatActivity act = (AppCompatActivity) getActivity();
                    if (act != null) {
                        mActionMode = act.startSupportActionMode(alteraUmaConta);
                    }
                    conta = position;
                } else {
                    // Trocando ou desativando a seleção
                    long oldId = buscaContas.getItemId(conta);
                    buscaContas.marcaConta(oldId, false);

                    if (position != conta) {
                        // Nova seleção
                        buscaContas.marcaConta(id, true);
                        AppCompatActivity act = (AppCompatActivity) getActivity();
                        if (act != null) {
                            mActionMode = act.startSupportActionMode(alteraUmaConta);
                        }
                        conta = position;
                    } else {
                        // Clicando no já selecionado: finaliza
                        mActionMode.finish();
                        // MontaLista(); // MontaLista é chamado em onDestroyActionMode
                    }
                }
            } else {
                // Modo de multi-seleção (alteraVariasContas)
                if (contas.contains(idConta)) {
                    // Deseleciona
                    if (!primeiraConta) {
                        contas.remove(idConta);
                        buscaContas.marcaConta(idConta, false);
                        valorConta = valorConta - vConta;
                    } else {
                        primeiraConta = false;
                        valorConta = valorConta + vConta; // Já foi adicionado no toque longo
                    }
                } else {
                    // Seleciona
                    contas.add(idConta);
                    buscaContas.marcaConta(idConta, true);
                    valorConta = valorConta + vConta;
                }

                if (contas.isEmpty()) {
                    mActionMode.finish();
                    // MontaLista(); // MontaLista é chamado em onDestroyActionMode
                }

                if (!contas.isEmpty() && mActionMode != null) {
                    mActionMode.setTitle(res.getQuantityString(R.plurals.selecao,
                            contas.size(), contas.size()));
                    if (tipo != -1)
                        mActionMode.setSubtitle(dinheiro.format(valorConta));
                }
            }
        }
    }

    /**
     * Lógica de toque longo (inicia o modo de multi-seleção).
     */
    private boolean handleItemLongClick(long id, int position) {

        if (mActionMode != null)
            mActionMode.finish();

        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
        contas = new ArrayList<>();
        primeiraConta = true;
        alteraContas = true;

        Cursor cursor = buscaContas.getItem(position);
        if (cursor != null) {
            idConta = id;
            nomeConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
            double vConta = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
            valorConta = vConta;

            contas.add(idConta);
            if (buscaContas != null) {
                buscaContas.marcaConta(idConta, true);
            }

            AppCompatActivity act = (AppCompatActivity) getActivity();
            if (act != null) {
                mActionMode = act.startSupportActionMode(alteraVariasContas);
                mActionMode.setTitle(res.getQuantityString(R.plurals.selecao, contas.size(), contas.size()));
                if (tipo != -1)
                    mActionMode.setSubtitle(dinheiro.format(valorConta));
            }
        }
        return true;
    }
    // endregion

    // region 9. Métodos de UI/Diálogo
    // =============================================================================================
    @SuppressWarnings("unused")
    private void Dialogo() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(requireActivity());
        dialogoBuilder.setTitle(getString(R.string.dica_menu_exclusao));
        dialogoBuilder.setItems(R.array.TipoAjusteConta,
                (dialog, id) -> {
                    try (Cursor cursor = dbContasDoMes.getContaPeloId(idConta)) {
                        if (cursor.moveToFirst()) {
                            String nomeContaExcluir = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
                            String codigoConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_CODIGO_CONTA);
                            if (id == 0) { // Delete only this account
                                dbContasDoMes.deleteContaById(idConta);
                            } else if (id == 1) { // Delete this and future recurring accounts
                                int nr = getColumnInt(cursor, ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA);
                                dbContasDoMes.deleteContasRecorrentes(codigoConta, nr, DBContas.TipoExclusao.DESTA_EM_DIANTE);
                            } else if (id == 2) { // Delete all recurring accounts with the same code
                                dbContasDoMes.deleteContasRecorrentes(codigoConta, 1, DBContas.TipoExclusao.TODAS_AS_REPETICOES);
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
                        buscaContas.swapCursor(dbContasDoMes.getContasByFilter(new DBContas.ContaFilter()
                                .setMes(mes)
                                .setAno(ano), buscaPreferencias.getString("ordem", ordemListaDeContas)));
                    }
                    MontaLista();
                    idConta = 0;
                    nomeConta = " ";
                });
        AlertDialog alertDialog = dialogoBuilder.create();
        alertDialog.show();
    }
    // endregion

    // region 10. Métodos Auxiliares de Cursor (Cursor Helper Methods)
    // =============================================================================================
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
    // endregion
}