package com.msk.minhascontas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.msk.minhascontas.db.ContasContract;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;
import com.msk.minhascontas.resumos.BaseResumoFragment;
import com.msk.minhascontas.resumos.ResumoCategoriaDiario;
import com.msk.minhascontas.resumos.ResumoCategoriaMensal;
import com.msk.minhascontas.resumos.ResumoTipoDiario;
import com.msk.minhascontas.resumos.ResumoTipoMensal;
import com.msk.minhascontas.viewmodel.ContasViewModel;
import com.msk.minhascontas.viewmodel.ContasViewModel.DateState;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Activity principal da aplicação "Minhas Contas".
 * Gerencia a navegação entre resumos, preferências do usuário,
 * bloqueio de aplicativo e interação com outras atividades.
 */
public class MinhasContas extends AppCompatActivity {

    private static final String TAG = "MinhasContas";

    // --- Constantes ---
    public static final int START_PAGE = 200; // Posição inicial do ViewPager para navegação infinita simulada

    // CHAVE DE ENTRADA (O que MinhasContas envia para as filhas)
    public static final String KEY_PAGINA = "nrPagina";
    public static final String KEY_MES = "mes";
    public static final String KEY_ANO = "ano";

    // CHAVES DE RETORNO (Uma para cada Activity que precisa sincronizar a posição)
    public static final String RETURN_KEY_PAGINA = "nr_pagina";

    // --- Variáveis Membro ---
    private DBContas dbContas;
    private ContasViewModel contasViewModel;
    private ViewPager2 mViewPager;

    // Variáveis de estado de data/posição (cache para acesso síncrono, são atualizadas pelo ViewModel)
    private int mes; // Mês (0-11)
    private int ano; // Ano
    private int nrPagina; // Posição atual do ViewPager

    // Preferências de usuário internas da Activity
    private boolean autobkup = true;
    private boolean bloqueioApp = false;
    private boolean atualizaPagamento = false;

    // --- ActivityResultLauncher para Comunicação entre Activities ---
    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int currentPage = mViewPager.getCurrentItem();
                int positionToSync = currentPage; // Valor padrão para sincronização

                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.hasExtra(RETURN_KEY_PAGINA)) {
                        positionToSync = data.getIntExtra(RETURN_KEY_PAGINA, currentPage);
                        Log.d(TAG, "Sincronização via Launcher: Posição retornada: " + positionToSync);
                    } else {
                        String activityName = (data != null && data.getComponent() != null) ? data.getComponent().getClassName() : "Unknown Activity";
                        Log.w(TAG, "Sincronização via Launcher: '" + activityName + "' não retornou RETURN_KEY_PAGINA. Recarregando página atual.");
                    }
                    // Chama o método para sincronizar a posição do ViewPager e refrescar o fragmento
                    syncViewPagerPositionAndRefresh(positionToSync);
                } else {
                    String activityName = (result.getData() != null && result.getData().getComponent() != null) ? result.getData().getComponent().getClassName() : "Unknown Activity";
                    Log.w(TAG, "Resultado da Activity filha '" + activityName + "' não foi OK. Posição e estado mantidos.");
                }
            });


    // --- Métodos de Ciclo de Vida da Activity ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity iniciada.");

        // Inicializa o banco de dados e o ViewModel
        dbContas = DBContas.getInstance(this);
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        // Carrega preferências do usuário e verifica bloqueio do aplicativo
        loadUserPreferencesAndCheckAppLock();

        // Configura o layout da Activity
        setContentView(R.layout.pagina_resumos);

        // Configura a barra de ferramentas
        setupToolbar();

        // Verifica e solicita permissões (e realiza ajustes no BD após permissão)
        checkAndRequestPermissions();

        // Configura o ViewPager e o TabLayout
        setupViewPagerAndTabLayout();

        // Configura o Floating Action Button (FAB)
        setupFabClickListener();

        // Observa o LiveData do ViewModel para inicializar e restaurar a UI
        observeViewModelDateState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity retomada.");
        // Implementar lógica de refresh se necessário ao retornar para a Activity.
        // O ViewModel.observe no onCreate já lida com a restauração de estado, mas
        // um refresh explícito pode ser necessário se os dados puderem mudar
        // em background ou por outras apps (menos comum).
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity pausada.");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: Estado da Activity salvo.");
        // O ViewModel já é responsável por manter o estado da UI,
        // então não é necessário salvar dados adicionais do ViewPager aqui.
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Activity destruída.");
        if (autobkup) {
            BackupManager.dataChanged("com.msk.minhascontas");
            Log.d(TAG, "onDestroy: Backup de dados solicitado.");
        }
        super.onDestroy();
    }

    // --- Métodos de Inicialização da UI ---

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "setupToolbar: Toolbar configurada.");
    }

    private void setupViewPagerAndTabLayout() {
        Paginas mPaginas = new Paginas(getSupportFragmentManager(), getLifecycle());

        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Apenas notifica o ViewModel sobre a nova posição.
                contasViewModel.setViewPagerPosition(position);
                Log.d(TAG, "setupViewPagerAndTabLayout: ViewModel atualizado (via swipe): " + position);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tablayout);
        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();
        Log.d(TAG, "setupViewPagerAndTabLayout: ViewPager e TabLayout configurados.");
    }

    private void setupFabClickListener() {
        ImageButton addConta = findViewById(R.id.ibfab);
        addConta.setOnClickListener(v -> {
            Intent intent = new Intent("com.msk.minhascontas.NOVACONTA");
            // Usa as variáveis de cache preenchidas pelo ViewModel
            intent.putExtra(KEY_PAGINA, nrPagina);
            intent.putExtra(KEY_MES, mes);
            intent.putExtra(KEY_ANO, ano);
            someActivityResultLauncher.launch(intent);
            Log.d(TAG, "setupFabClickListener: FAB clicado. Abrindo NovaConta com dados: Posição=" + nrPagina + ", Mês=" + mes + ", Ano=" + ano);
        });
    }

    private void observeViewModelDateState() {
        contasViewModel.getCurrentDateState().observe(this, dateState -> {
            // 1. Atualiza o ViewPager para a posição correta (se não for a posição atual)
            if (mViewPager.getCurrentItem() != dateState.nrPagina) {
                mViewPager.setCurrentItem(dateState.nrPagina, false);
            }

            // 2. Atualiza as variáveis de cache de estado para métodos síncronos da Activity
            this.mes = dateState.mes;
            this.ano = dateState.ano;
            this.nrPagina = dateState.nrPagina;

            Log.d(TAG, "observeViewModelDateState: Posição e Data restauradas via ViewModel (Observe): " + (dateState.mes + 1) + "/" + dateState.ano + ", Posição=" + dateState.nrPagina);

            // 3. GARANTIA VISUAL: Agenda a seleção da aba.
            mViewPager.post(() -> {
                TabLayout tabLayout = findViewById(R.id.tablayout);
                if (tabLayout != null) {
                    TabLayout.Tab tab = tabLayout.getTabAt(dateState.nrPagina);
                    if (tab != null) {
                        tab.select();
                        Log.d(TAG, "observeViewModelDateState: Visual (Observe): TabLayout selecionou a posição: " + dateState.nrPagina);
                    }
                }
            });
        });
    }


    // --- Preferências do Usuário ---

    private void loadUserPreferencesAndCheckAppLock() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        autobkup = prefs.getBoolean(getString(R.string.pref_key_auto_bkup), true);
        bloqueioApp = prefs.getBoolean(getString(R.string.pref_key_acesso), false);
        atualizaPagamento = prefs.getBoolean(getString(R.string.pref_key_pagamento), false);

        Log.d(TAG, "loadUserPreferencesAndCheckAppLock: Preferências carregadas: AutoBackup=" + autobkup + ", BloqueioApp=" + bloqueioApp + ", AtualizaPagamento=" + atualizaPagamento);

        if (bloqueioApp) {
            showAppLockDialog();
        }
    }


    // --- Segurança / Diálogos de Autenticação ---

    private void showAppLockDialog() {
        final Dialog dialogo = new Dialog(this);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.tela_bloqueio);
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        }
        dialogo.setCanceledOnTouchOutside(false);

        final AppCompatEditText edit = dialogo.findViewById(R.id.etSenha);
        SharedPreferences buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(this);
        String senhaUsuario = buscaPreferencias.getString("senha", "");

        Button ok = dialogo.findViewById(R.id.bEntra);
        ok.setOnClickListener(v -> {
            if (senhaUsuario.equals(edit.getText() != null ? edit.getText().toString() : null)) {
                dialogo.dismiss();
                Log.d(TAG, "showAppLockDialog: Senha correta, diálogo de bloqueio dispensado.");
            } else {
                edit.setHint(getString(R.string.senha_errada));
                edit.setHintTextColor(Color.RED);
                edit.setText("");
                Log.w(TAG, "showAppLockDialog: Tentativa de senha incorreta.");
            }
        });

        TextView tvEsqueciSenha = dialogo.findViewById(R.id.tvEsqueciSenha);
        tvEsqueciSenha.setOnClickListener(v -> showPasswordRecoveryDialog(dialogo));

        AppCompatCheckBox cb = dialogo.findViewById(R.id.cbMostraSenha);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                edit.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                edit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            edit.setSelection(edit.getText() != null ? edit.getText().length() : 0); // Mantém o cursor no final
        });
        dialogo.show();
        dialogo.setOnCancelListener(dialog -> {
            Log.d(TAG, "showAppLockDialog: Diálogo de bloqueio cancelado, finalizando Activity.");
            finish(); // Sai do aplicativo se o diálogo for cancelado
        });
        Log.d(TAG, "showAppLockDialog: Diálogo de bloqueio exibido.");
    }

    private void showPasswordRecoveryDialog(Dialog dialogoBloqueio) {
        SharedPreferences buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(this);
        String perguntaId = buscaPreferencias.getString("pergunta_seguranca_id", null);
        String respostaSalva = buscaPreferencias.getString("resposta_secreta", null);

        if (perguntaId == null || respostaSalva == null || respostaSalva.isEmpty()) {
            Toast.makeText(this, getString(R.string.erro_recuperacao_nao_configurada), Toast.LENGTH_LONG).show();
            Log.w(TAG, "showPasswordRecoveryDialog: Recuperação de senha não configurada.");
            return;
        }

        String[] perguntas = getResources().getStringArray(R.array.perguntas_seguranca);
        String pergunta = perguntas[Integer.parseInt(perguntaId)];

        final AppCompatEditText inputResposta = new AppCompatEditText(this);
        inputResposta.setHint(R.string.dica_resposta_secreta);

        new AlertDialog.Builder(this)
                .setTitle(R.string.recuperacao_senha)
                .setMessage(pergunta)
                .setView(inputResposta)
                .setPositiveButton(R.string.confirmar, (dialogRec, which) -> {
                    String respostaDigitada = inputResposta.getText() != null ? inputResposta.getText().toString().trim() : "";

                    if (respostaDigitada.equalsIgnoreCase(respostaSalva)) {
                        showNewPasswordDialog(dialogoBloqueio);
                        Log.d(TAG, "showPasswordRecoveryDialog: Resposta de segurança correta.");
                    } else {
                        Toast.makeText(this, getString(R.string.senha_errada), Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "showPasswordRecoveryDialog: Resposta de segurança incorreta.");
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
        Log.d(TAG, "showPasswordRecoveryDialog: Diálogo de recuperação de senha exibido.");
    }

    private void showNewPasswordDialog(Dialog dialogoBloqueio) {
        final AppCompatEditText inputNovaSenha = new AppCompatEditText(this);
        inputNovaSenha.setHint(R.string.nova_senha);
        inputNovaSenha.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.redefinir_senha)
                .setView(inputNovaSenha)
                .setPositiveButton(R.string.salvar, (dialogNovaSenha, which) -> {
                    String novaSenha = inputNovaSenha.getText() != null ? inputNovaSenha.getText().toString() : "";

                    if (!novaSenha.isEmpty()) {
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("senha", novaSenha)
                                .apply();

                        Toast.makeText(this, getString(R.string.senha_redefinida_sucesso), Toast.LENGTH_LONG).show();
                        dialogoBloqueio.dismiss();
                        Log.d(TAG, "showNewPasswordDialog: Senha redefinida com sucesso.");
                    } else {
                        Toast.makeText(this, getString(R.string.erro_senha_vazia), Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "showNewPasswordDialog: Nova senha vazia.");
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
        Log.d(TAG, "showNewPasswordDialog: Diálogo de nova senha exibido.");
    }

    // --- Permissões ---

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Se a permissão não foi concedida, solicita.
            // shouldShowRequestPermissionRationale pode ser usado para dar um contexto ao usuário.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            Log.d(TAG, "checkAndRequestPermissions: Solicitando permissão WRITE_EXTERNAL_STORAGE.");
        } else {
            // Permissão já concedida, realiza ajustes no BD.
            performDatabaseAdjustments();
            Log.d(TAG, "checkAndRequestPermissions: Permissão WRITE_EXTERNAL_STORAGE já concedida. Ajustes no BD realizados.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) { // Código de requisição para WRITE_EXTERNAL_STORAGE
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
                performDatabaseAdjustments();
                Log.d(TAG, "onRequestPermissionsResult: Permissão WRITE_EXTERNAL_STORAGE concedida. Ajustes no BD realizados.");
            } else {
                // Permissão negada
                Toast.makeText(getApplicationContext(), getString(R.string.permissao_negada), Toast.LENGTH_SHORT).show();
                Log.w(TAG, "onRequestPermissionsResult: Permissão WRITE_EXTERNAL_STORAGE negada.");
            }
        }
    }

    // --- Lógica de Banco de Dados / Negócio (Ajustes de Manutenção) ---

    private void performDatabaseAdjustments() {
        Log.d(TAG, "performDatabaseAdjustments: Iniciando ajustes no banco de dados.");
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        Calendar c = Calendar.getInstance();
        if (atualizaPagamento) {
            // Atualiza pagamentos para o dia seguinte ao dia atual do mês, no mês e ano atuais.
            dbContas.atualizaPagamentoContas(c.get(Calendar.DAY_OF_MONTH) + 1, c.get(Calendar.MONTH), c.get(Calendar.YEAR));
            Log.d(TAG, "performDatabaseAdjustments: Atualização de pagamentos agendada.");
        }
        Log.d(TAG, "performDatabaseAdjustments: Ajustes no banco de dados concluídos.");
    }

    // --- Sincronização da UI e Dados ---

    /**
     * Chamado por outras Activities ou eventos para sincronizar a posição do ViewPager
     * e forçar o refresh do fragmento atual.
     * @param returnedPosition A posição retornada pela Activity filha ou a posição a ser sincronizada.
     */
    public void syncViewPagerPositionAndRefresh(int returnedPosition) {
        // 1. Notifica o ViewModel da nova posição. O observe() no onCreate cuidará do resto.
        contasViewModel.setViewPagerPosition(returnedPosition);
        Log.d(TAG, "syncViewPagerPositionAndRefresh: ViewModel notificado com a posição: " + returnedPosition);

        // 2. Notifica o fragmento para recarregar o conteúdo (essencial para refresh de dados).
        Paginas adapter = (Paginas) mViewPager.getAdapter();
        if (adapter != null) {
            Fragment fragment = adapter.getFragment(returnedPosition);
            if (fragment instanceof BaseResumoFragment) {
                ((BaseResumoFragment) fragment).refreshData();
                Log.d(TAG, "syncViewPagerPositionAndRefresh: Fragmento na posição " + returnedPosition + " teve refreshData() chamado.");
            } else {
                Log.w(TAG, "syncViewPagerPositionAndRefresh: Fragmento na posição " + returnedPosition + " não é BaseResumoFragment ou é nulo. Não pode chamar refreshData().");
            }
        }
        Log.d(TAG, "Sincronização completa para posição: " + returnedPosition);
    }

    // --- Manipulação de Menu de Opções ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_botoes_inicio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_ajustes) {
            Intent intent = new Intent(this, Ajustes.class);
            someActivityResultLauncher.launch(intent);
            Log.d(TAG, "onOptionsItemSelected: Abrindo Ajustes.");
        } else if (itemId == R.id.menu_sobre) {
            startActivity(new Intent("com.msk.minhascontas.SOBRE"));
            Log.d(TAG, "onOptionsItemSelected: Abrindo Sobre.");
        } else if (itemId == R.id.botao_pesquisar) {
            Intent intent = new Intent("com.msk.minhascontas.BUSCACONTA");
            intent.putExtra(KEY_PAGINA, nrPagina); // Use nrPagina for consistency
            someActivityResultLauncher.launch(intent);
            Log.d(TAG, "onOptionsItemSelected: Abrindo BuscaConta. Posição atual: " + nrPagina);
            return true;
        } else if (itemId == R.id.botao_enviar) {
            // Obtém os meses do ViewModel para esta operação síncrona
            String[] mesesArray = contasViewModel.getStringMonths();
            // Usa 'mes' e 'ano' que ainda são variáveis de cache na Activity, preenchidas pelo LiveData.
            StringBuilder texto = new StringBuilder(getString(R.string.app_name) + " "
                    + mesesArray[mes] + "/" + ano + ":");

            DBContas.ContaFilter filter = new DBContas.ContaFilter()
                    .setMes(mes).setAno(ano);

            try (Cursor cursor = dbContas.getContasByFilter(filter, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String nomeConta = getColumnString(cursor, ContasContract.Colunas.COLUNA_NOME_CONTA);
                        double valorConta = getColumnDouble(cursor, ContasContract.Colunas.COLUNA_VALOR_CONTA);
                        texto.append(" ").append(nomeConta).append(" - ").append(valorConta);
                    } while (cursor.moveToNext());
                } else {
                    Log.d(TAG, "onOptionsItemSelected: Botão Enviar - Nenhum dado encontrado para o mês/ano selecionado.");
                    texto.append(" ").append(getString(R.string.dica_nenhuma_conta));
                }
            } catch (Exception e) {
                Log.e(TAG, "onOptionsItemSelected: Erro ao ler contas para envio: " + e.getMessage(), e);
                texto.append(" ").append(getString(R.string.erro_geral_bd));
            }

            Intent envelope = new Intent(Intent.ACTION_SEND);
            envelope.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            envelope.putExtra(Intent.EXTRA_TEXT, texto.toString());
            envelope.setType("text/plain");
            startActivity(Intent.createChooser(envelope, texto.toString()));
            Log.d(TAG, "onOptionsItemSelected: Botão Enviar - Compartilhando contas do mês " + (mes + 1) + "/" + ano);
        } else if (itemId == R.id.botao_graficos) {
            Bundle dados_mes = new Bundle();
            dados_mes.putInt("mes", mes);
            dados_mes.putInt("ano", ano);
            dados_mes.putInt(KEY_PAGINA, nrPagina); // Use nrPagina for consistency
            Intent graficos = new Intent("com.msk.minhascontas.graficos.MEUSGRAFICOS");
            graficos.putExtras(dados_mes);
            someActivityResultLauncher.launch(graficos);
            Log.d(TAG, "onOptionsItemSelected: Abrindo Gráficos com dados: Mês=" + mes + ", Ano=" + ano + ", Posição=" + nrPagina);
        }
        return super.onOptionsItemSelected(item);
    }


    // --- Métodos Auxiliares de Cursor ---

    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getString(columnIndex) : null;
    }

    private double getColumnDouble(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getDouble(columnIndex) : 0.0D;
    }


    // --- Classes Internas ---

    /**
     * Adaptador para o ViewPager2 que gerencia os fragmentos de resumo.
     */
    public class Paginas extends FragmentStateAdapter {

        private final HashMap<Integer, Fragment> mFragmentos = new HashMap<>();

        public Paginas(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Obtém o ViewState do ViewModel para decidir qual tipo de resumo criar
            ContasViewModel.ViewState viewState = contasViewModel.getViewState().getValue();
            boolean isMonthly = viewState == null || viewState.isMonthlySummary;
            boolean isCategory = viewState != null && viewState.isCategorySummary;

            DateState dateState = ContasViewModel.calculateDateState(position, isMonthly);
            Fragment fragment;

            if (isMonthly) {
                fragment = isCategory ?
                        ResumoCategoriaMensal.newInstance(dateState.mes, dateState.ano, position - START_PAGE) :
                        ResumoTipoMensal.newInstance(dateState.mes, dateState.ano, position - START_PAGE);
            } else {
                fragment = isCategory ?
                        ResumoCategoriaDiario.newInstance(dateState.dia, dateState.mes, dateState.ano) :
                        ResumoTipoDiario.newInstance(dateState.dia, dateState.mes, dateState.ano);
            }
            mFragmentos.put(position, fragment);
            Log.v(TAG, "Paginas.createFragment: Criado fragmento para posição " + position + ". Tipo: " + fragment.getClass().getSimpleName());
            return fragment;
        }

        @Override
        public int getItemCount() {
            return START_PAGE * 2; // Simula uma navegação quase infinita
        }

        /**
         * Retorna o título para a aba correspondente à posição do ViewPager.
         */
        public CharSequence getPageTitle(int position) {
            // OBTÉM OS MESES DO VIEWMODEL
            String[] mesesArray = contasViewModel.getStringMonths();

            ContasViewModel.ViewState viewState = contasViewModel.getViewState().getValue();
            boolean isMonthly = viewState == null || viewState.isMonthlySummary;

            DateState dateState = ContasViewModel.calculateDateState(position, isMonthly);

            if (isMonthly) {
                // Ex: "Jan/24"
                return mesesArray[dateState.mes] + "/" + String.valueOf(dateState.ano).substring(2);
            } else {
                // Ex: "10/Jan"
                return dateState.dia + "/" + mesesArray[dateState.mes];
            }
        }

        /**
         * Retorna a instância do fragmento para uma dada posição.
         * Útil para chamar métodos específicos do fragmento (ex: refreshData).
         */
        public Fragment getFragment(int position) {
            return mFragmentos.get(position);
        }
    }
}