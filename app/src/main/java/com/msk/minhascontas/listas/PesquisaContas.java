package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context; // Adicionado para InputMethodManager
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler; // Adicionado
import android.os.Looper; // Adicionado
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.text.Editable; // Adicionado
import android.text.TextWatcher; // Adicionado
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager; // Adicionado
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.ContasContract;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.Conta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PesquisaContas extends AppCompatActivity {

    // --- CONSTANTES ---
    private static final String TAG = "PesquisaContas"; // Tag para logs
    private static final long SEARCH_DELAY_MS = 300; // Atraso em milissegundos para o debounce da pesquisa

    // --- DEPENDÊNCIAS / RECURSOS ---
    private DBContas dbContasPesquisadas;
    private final Calendar c = Calendar.getInstance();
    private Resources r;

    // --- ELEMENTOS DE UI ---
    private RecyclerView listaContas;
    private TextView resultado;
    private AppCompatAutoCompleteTextView nomeContaBuscar;

    // --- ADAPTADORES E CURSORES ---
    private AdaptaListaPesquisaRC buscaContas;
    private Cursor contasParaLista = null;

    // --- ESTADO E VARIÁVEIS DA ATIVIDADE ---
    private ActionMode mActionMode;
    private long idConta = 0;
    private final ArrayList<Long> contas = new ArrayList<>();
    private boolean alteraContas = false; // Indica se estamos em modo de multi-seleção
    private boolean primeiraConta = false; // Auxiliar para o primeiro item selecionado no multi-seleção
    private String nomeConta; // Usado para o título do ActionMode de seleção única
    private int conta; // Mantido para rastrear a posição no modo de seleção única
    private int nrPagina; // Recebe a posição da página para retornar

    // --- Debounce Mechanism ---
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // --- MÉTODOS DE LIFECYCLE DA ATIVIDADE ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pesquisa_conta);
        r = getResources();
        dbContasPesquisadas = DBContas.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nrPagina = extras.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE);
            Log.d(TAG, "Posição recebida (KEY_PAGINA): " + nrPagina);
        } else {
            nrPagina = MinhasContas.START_PAGE;
        }

        iniciar();
        usarActionBar();
        MontaAutoCompleta();

        listaContas.setLayoutManager(new LinearLayoutManager(this));

        // Adiciona listener para filtrar a lista em tempo real enquanto o usuário digita
        nomeContaBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Não é necessário implementar aqui para este caso
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancela qualquer execução de pesquisa pendente para evitar múltiplas chamadas rápidas
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                // Agenda uma nova pesquisa com um pequeno atraso (debounce)
                searchRunnable = () -> {
                    MontaLista(s.toString());
                };
                handler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Não é necessário implementar aqui para este caso
            }
        });

        // Adiciona listener para clique no item do autocomplete
        // Modificado para cancelar o debounce e chamar MontaLista imediatamente
        nomeContaBuscar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Cancela qualquer pesquisa pendente do TextWatcher e executa imediatamente
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                MontaLista(nomeContaBuscar.getText().toString());
                // Esconde o teclado após a seleção de um item
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(nomeContaBuscar.getWindowToken(), 0);
                }
            }
        });

        // Adiciona listener para ação "Done" do teclado
        // Modificado para cancelar o debounce e chamar MontaLista imediatamente
        nomeContaBuscar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    // Cancela qualquer pesquisa pendente do TextWatcher e executa imediatamente
                    if (searchRunnable != null) {
                        handler.removeCallbacks(searchRunnable);
                    }
                    MontaLista(nomeContaBuscar.getText().toString());
                    // Esconde o teclado após a pesquisa
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });

        // Carrega a lista inicial (sem filtro se o campo estiver vazio, ou com filtro se houver texto inicial)
        MontaLista(nomeContaBuscar.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            // Recarrega a lista com o filtro atual
            MontaLista(nomeContaBuscar.getText().toString());
            idConta = 0;
        }
    }

    // Necessário para evitar que a superclasse chame finish() antes,
    // e para gerenciar o comportamento de volta com gestos no Android 13+.
    @SuppressLint({"MissingSuperCall", "GestureBackNavigation"})
    @Override
    public void onBackPressed() {
        // Se houver um ActionMode ativo, finalize-o em vez de sair da tela
        if (mActionMode != null) {
            mActionMode.finish();
            return; // Consome o evento de back para não sair da Activity
        }
        // Se não houver ActionMode, retorna normalmente
        RetornaPosicaoParaSincronizacao();
        super.onBackPressed();
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
            RetornaPosicaoParaSincronizacao();
            finish();
            return true;
        } else if (itemId == R.id.botao_pesquisar) {
            // Cancela qualquer pesquisa pendente do TextWatcher e executa imediatamente
            if (searchRunnable != null) {
                handler.removeCallbacks(searchRunnable);
            }
            MontaLista(nomeContaBuscar.getText().toString());
            idConta = 0;
            // Esconde o teclado após a pesquisa manual
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(nomeContaBuscar.getWindowToken(), 0);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- MÉTODOS DE INICIALIZAÇÃO E CONFIGURAÇÃO ---

    private void iniciar() {
        listaContas = findViewById(R.id.lvContasPesquisadas);
        resultado = findViewById(R.id.tvSemResultados);
        nomeContaBuscar = findViewById(R.id.acNomeContaBusca);
    }

    private void usarActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void MontaAutoCompleta() {
        Set<String> accountNames = new HashSet<>();
        try (Cursor cursor = dbContasPesquisadas.getContasByFilter(new DBContas.ContaFilter(), null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
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

    // --- MÉTODOS DE LÓGICA PRINCIPAL E MANIPULAÇÃO DE DADOS ---

    private void MontaLista(String filterName) {
        if (contasParaLista != null) {
            contasParaLista.close();
        }

        DBContas.ContaFilter filter = new DBContas.ContaFilter();
        if (filterName != null && !filterName.trim().isEmpty()) {
            filter.setNome(filterName);
        }

        contasParaLista = dbContasPesquisadas.getContasByFilter(filter, null);

        if (buscaContas == null) {
            buscaContas = new AdaptaListaPesquisaRC(this, contasParaLista);
            buscaContas.setOnItemClickListener(itemClickListener);
            listaContas.setAdapter(buscaContas);
        } else {
            buscaContas.swapCursor(contasParaLista);
        }

        // Tratamento manual para Empty View (RecyclerView não suporta setEmptyView diretamente)
        if (contasParaLista.getCount() == 0) {
            listaContas.setVisibility(View.GONE);
            resultado.setVisibility(View.VISIBLE);
        } else {
            listaContas.setVisibility(View.VISIBLE);
            resultado.setVisibility(View.GONE);
        }

        contas.clear();
        alteraContas = false;
        primeiraConta = false;
        if (mActionMode != null) { // Garante que o ActionMode é finalizado ao recarregar a lista
            mActionMode.finish();
        }
        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
    }

    /**
     * Lógica de toque simples (seleção individual ou toggle de multi-seleção).
     */
    private void handleItemClick(long id, int position) {
        Cursor cursor = buscaContas.getItem(position);
        if (cursor != null) {
            idConta = id;
            nomeConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);

            if (!alteraContas) {
                // Modo de seleção individual (alteraUmaConta)
                if (mActionMode == null) {
                    // Inicia a seleção
                    buscaContas.limpaSelecao();
                    contas.clear();
                    buscaContas.marcaConta(id, true);

                    mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                    conta = position;
                } else {
                    // Trocando ou desativando a seleção
                    long oldId = buscaContas.getItemId(conta);
                    if (oldId != -1) { // Verifica se oldId é válido
                        buscaContas.marcaConta(oldId, false);
                    }


                    if (position != conta) {
                        // Nova seleção
                        buscaContas.marcaConta(id, true);
                        // Reinicia o ActionMode para atualizar o título, se necessário.
                        // Poderíamos também apenas atualizar o título do modo existente.
                        mActionMode.finish(); // Finaliza o anterior para iniciar um novo com o item correto
                        mActionMode = PesquisaContas.this.startSupportActionMode(alteraUmaConta);
                        conta = position;
                    } else {
                        // Clicando no já selecionado: finaliza
                        mActionMode.finish();
                    }
                }
            } else {
                // Modo de multi-seleção (alteraVariasContas)
                if (contas.contains(idConta)) {
                    // Deseleciona
                    // primeiraconta é uma flag auxiliar para não desmarcar o item inicial ao primeiro clique.
                    // Isso pode ser simplificado, se a intenção é apenas que o primeiro item possa ser desmarcado normalmente
                    // após o modo de seleção ser iniciado.
                    if (primeiraConta && contas.size() == 1 && contas.get(0) == idConta) {
                        // Se é o primeiro item e único na seleção, não faz nada no primeiro "desclique"
                        primeiraConta = false; // Permite desmarcar na próxima tentativa
                    } else {
                        contas.remove(idConta);
                        buscaContas.marcaConta(idConta, false);
                        primeiraConta = false; // Qualquer outra desmarcação, a flag não importa mais
                    }
                } else {
                    // Seleciona
                    contas.add(idConta);
                    buscaContas.marcaConta(idConta, true);
                    primeiraConta = false; // Uma vez que mais de um item é selecionado, a flag não é mais relevante
                }

                if (contas.isEmpty()) {
                    mActionMode.finish();
                }

                if (!contas.isEmpty() && mActionMode != null) {
                    mActionMode.setTitle(r.getQuantityString(R.plurals.selecao,
                            contas.size(), contas.size()));
                }
            }
        }
    }

    /**
     * Lógica de toque longo (inicia o modo de multi-seleção).
     */
    private boolean handleItemLongClick(long id, int position) {
        // Se já existe um ActionMode ativo, finalize-o antes de iniciar um novo
        if (mActionMode != null) {
            mActionMode.finish();
        }

        // Prepara para o modo de multi-seleção
        if (buscaContas != null) {
            buscaContas.limpaSelecao();
        }
        contas.clear();
        alteraContas = true;
        primeiraConta = true; // Marca que este é o primeiro item da multi-seleção

        // Busca informações da conta no DB
        Cursor cursor = buscaContas.getItem(position);
        if (cursor != null) {
            idConta = id;
            nomeConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);

            // Seleciona a conta clicada
            contas.add(idConta);
            if (buscaContas != null) {
                buscaContas.marcaConta(idConta, true);
            }

            // Inicia o ActionMode de multi-seleção
            mActionMode = PesquisaContas.this.startSupportActionMode(alteraVariasContas);

            // Atualiza o título do ActionMode com a contagem
            if (mActionMode != null) { // Verificação adicional para garantir que mActionMode não é nulo
                mActionMode.setTitle(r.getQuantityString(R.plurals.selecao,
                        contas.size(), contas.size()));
            }
        }
        return true;
    }

    @SuppressWarnings("unused") // Mantendo o método Dialogo, caso seja usado em outro lugar
    private void Dialogo() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getString(R.string.dica_menu_exclusao));

        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try (Cursor cursor = dbContasPesquisadas.getContaPeloId(idConta)) {
                            if (cursor.moveToFirst()) {
                                String nomeContaExcluir = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
                                String codigoConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_CODIGO_CONTA);
                                int nr = getColumnInt(cursor, ContasContract.Colunas.COLUNA_NR_REPETICAO_CONTA);

                                if (id == 0) { // Delete only this account
                                    dbContasPesquisadas.deleteContaById(idConta);
                                } else if (id == 1) { // Delete this and future recurring accounts
                                    dbContasPesquisadas.deleteContasRecorrentes(
                                            codigoConta, nr, DBContas.TipoExclusao.DESTA_EM_DIANTE);
                                } else if (id == 2) { // Delete all recurring accounts with the same code
                                    dbContasPesquisadas.deleteContasRecorrentes(
                                            codigoConta, 1, DBContas.TipoExclusao.TODAS_AS_REPETICOES);
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
                        // Recarrega a lista com o filtro atual
                        MontaLista(nomeContaBuscar.getText().toString());
                        MontaAutoCompleta();
                        idConta = 0;
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // --- MÉTODOS DE RETORNO / SINCRONIZAÇÃO ---

    private void RetornaPosicaoParaSincronizacao() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, nrPagina);
        setResult(RESULT_OK, returnIntent);
        Log.d(TAG, "Retornando posição: " + nrPagina);
    }

    // --- CALLBACKS DO ACTION MODE ---

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
                    Intent app = new Intent("com.msk.minhascontas.EDITACONTA");
                    app.putExtras(nova);
                    PesquisaContas.this.startActivityForResult(app, 1);
                }
                setResult(RESULT_OK, null);
            } else if (itemId == R.id.botao_excluir) {
                if (idConta != 0) {
                    // Usando o diálogo de exclusão, que lida com recorrentes
                    // O método Dialogo() já existe e é adequado para seleção única
                    Dialogo();
                }
            } else if (itemId == R.id.botao_pagar) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasPesquisadas.getContaPeloId(idConta)) {
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
                            dbContasPesquisadas.updateConta(contaToUpdate);
                        }
                    }
                    idConta = 0;
                    MontaLista(nomeContaBuscar.getText().toString());
                    setResult(RESULT_OK, null);
                }
            } else if (itemId == R.id.botao_lembrete) {
                if (idConta != 0) {
                    try (Cursor cursor = dbContasPesquisadas.getContaPeloId(idConta)) {
                        if (cursor.moveToFirst()) {
                            int dia = getColumnInt(cursor, ContasContract.Colunas.COLUNA_DIA_DATA_CONTA);
                            int mes = getColumnInt(cursor, ContasContract.Colunas.COLUNA_MES_DATA_CONTA);
                            int ano = getColumnInt(cursor, ContasContract.Colunas.COLUNA_ANO_DATA_CONTA);
                            double valorConta = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
                            String nomeContaCalendario = r.getString(
                                    R.string.dica_evento,
                                    getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA));

                            // Mês no Calendar é de 0-11, então subtraímos 1 do mês do DB (que deve ser 1-12)
                            c.set(ano, mes - 1, dia); // Ajuste aqui para o mês

                            Intent evento = new Intent(Intent.ACTION_EDIT);
                            evento.setType("vnd.android.cursor.item/event");
                            evento.putExtra(Events.TITLE, nomeContaCalendario);

                            Locale current;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                current = r.getConfiguration().getLocales().get(0);
                            } else {
                                current = r.getConfiguration().locale;
                            }
                            NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

                            evento.putExtra(Events.DESCRIPTION, r
                                    .getString(R.string.dica_calendario, dinheiro.format(valorConta)));

                            evento.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, c.getTimeInMillis());
                            evento.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, c.getTimeInMillis());

                            evento.putExtra(Events.ACCESS_LEVEL, Events.ACCESS_PRIVATE);
                            evento.putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY);
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
                buscaContas.limpaSelecao();
            }
            // Garante que o estado de multi-seleção é desativado
            alteraContas = false;
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
                    for (long id : contas) {
                        try (Cursor cursor = dbContasPesquisadas.getContaPeloId(id)) {
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
                                dbContasPesquisadas.updateConta(contaToUpdate);
                            }
                        }
                    }
                }
                setResult(RESULT_OK, null);
                mode.finish();
            } else if (itemId == R.id.botao_excluir) {
                if (!contas.isEmpty()) {
                    // TODO: Implementar diálogo de exclusão recorrente para multi-seleção, se necessário.
                    // Atualmente, a exclusão de várias contas apenas exclui as ocorrências individuais.
                    // O diálogo original (Dialogo()) tratava de exclusão recorrente para uma única conta.
                    // Se você quiser o diálogo de exclusão recorrente para cada item selecionado,
                    // precisará iterar sobre 'contas' e chamar o Dialogo() ou uma lógica similar para cada 'id'.
                    // Por enquanto, ele apenas exclui as ocorrências individuais.
                    new AlertDialog.Builder(PesquisaContas.this)
                            .setTitle(R.string.confirmar_exclusao_multipla_titulo)
                            .setMessage(r.getQuantityString(R.plurals.confirmar_exclusao_multipla_mensagem, contas.size(), contas.size()))
                            .setPositiveButton(R.string.sim, (dialog, which) -> {
                                for (long id : contas) {
                                    dbContasPesquisadas.deleteConta(id); // Exclui a ocorrência individual
                                }
                                Toast.makeText(PesquisaContas.this.getApplicationContext(),
                                        r.getQuantityString(R.plurals.dica_contas_excluidas, contas.size(), contas.size()),
                                        Toast.LENGTH_SHORT).show();
                                MontaLista(nomeContaBuscar.getText().toString()); // Atualiza a lista
                                mode.finish(); // Finaliza o ActionMode
                            })
                            .setNegativeButton(R.string.nao, null)
                            .show();
                    return true; // Retorna true para consumir o clique e aguardar a ação do diálogo
                }
            }
            MontaLista(nomeContaBuscar.getText().toString()); // Atualiza a lista após as operações
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (buscaContas != null) {
                buscaContas.limpaSelecao();
            }
            contas.clear();
            alteraContas = false; // Garante que o estado de multi-seleção é desativado
            primeiraConta = false; // Reseta a flag
        }
    };

    // --- LISTENERS ---

    private final AdaptaListaPesquisaRC.OnItemClickListener itemClickListener = new AdaptaListaPesquisaRC.OnItemClickListener() {
        @Override
        public void onItemClick(long id, int position) {
            handleItemClick(id, position);
        }

        @Override
        public boolean onItemLongClick(long id, int position) {
            return handleItemLongClick(id, position);
        }
    };

    // --- MÉTODOS AUXILIARES PARA CURSOR ---

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