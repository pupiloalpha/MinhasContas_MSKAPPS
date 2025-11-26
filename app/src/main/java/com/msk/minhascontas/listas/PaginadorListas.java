package com.msk.minhascontas.listas;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.msk.minhascontas.MinhasContas;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;
import com.msk.minhascontas.viewmodel.ContasViewModel;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class PaginadorListas extends AppCompatActivity {

    private static final String TAG = "PaginadorListas";
    private static final int START_PAGE = MinhasContas.START_PAGE;
    public static ImageButton addConta;
    private DBContas dbContas;
    private ViewPager2 mViewPager;
    private Paginas mPaginas;
    private Resources res;
    private NumberFormat dinheiro;
    private String[] classes;

    // NOVO: ViewModel para gerenciar a posição e a data
    private ContasViewModel contasViewModel;
    private int mes, ano, tipo, filtro, nrPagina;

    // ActivityResultLauncher para CriarConta.java ou Ajustes.java
    private final ActivityResultLauncher<Intent> mPaginaListaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Após criar ou editar uma conta, o fragmento atual precisa recarregar
                    int currentPage = mViewPager.getCurrentItem();
                    // 1. Encontra o Fragment ativo na posição atual.
                    Fragment fragment = mPaginas.getFragment(currentPage);
                    // 2. Chama o método público de atualização no Fragment
                    if (fragment instanceof ListaMensalContas) {
                        ((ListaMensalContas) fragment).refreshLista();
                        // 3. Atualiza o Action Bar para recalcular a soma com os novos dados
                        AtualizaActionBar();
                    }
                    // O retorno da posição para MinhasContas será feito em onBackPressed/finish,
                    Log.d(TAG, "Resultado OK recebido. Forçando recarga do fragmento na posição: " + currentPage);
                }
            });

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_lista_mensal);

        // 1. Inicializa o ViewModel (Escopo da Activity)
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        res = getResources();
        Locale current = res.getConfiguration().getLocales().get(0);
        dinheiro = NumberFormat.getCurrencyInstance(current);

        dbContas = DBContas.getInstance(this);


        Bundle localBundle = getIntent().getExtras();
        int tipoFromMinhasContas = 0;
        int initialPosition = START_PAGE; // Default para a página atual (mês atual)

        if (localBundle != null) {
            tipoFromMinhasContas = localBundle.getInt("tipo", 0);
            int receivedPosition = localBundle.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE);

            if (receivedPosition >= 0) {
                initialPosition = receivedPosition;
                Log.d(TAG, "SINCRONIA: Target Page set by KEY_PAGINA: " + initialPosition);
            } else {
                Log.d(TAG, "SINCRONIA: Usando START_PAGE padrão: " + START_PAGE);
            }
        }

        if (savedInstanceState != null) {
            initialPosition = savedInstanceState.getInt(MinhasContas.KEY_PAGINA, initialPosition);
            Log.d(TAG, "Posição restaurada via savedInstanceState: " + initialPosition);
        }
        this.tipo = tipoFromMinhasContas;

        // 2. Sincroniza a posição inicial com o ViewModel
        contasViewModel.setViewPagerPosition(initialPosition);

        // 3. Obtém o estado inicial do ViewModel para inicializar as variáveis de cache (mes/ano/nrPagina)
        ContasViewModel.DateState dateState = contasViewModel.getCurrentDateState().getValue();
        if (dateState != null) {
            this.mes = dateState.mes;
            this.ano = dateState.ano;
            this.nrPagina = dateState.nrPagina;
        } else {
            // Fallback para o caso de ViewModel estar nulo no init (improvável, mas seguro)
            this.nrPagina = initialPosition;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        int toolbarColor = 0;
        if (tipo == 1) {
            toolbarColor = ContextCompat.getColor(this, R.color.receita_color);
        } else if (tipo == 0) {
            toolbarColor = ContextCompat.getColor(this, R.color.despesa_color);
        } else if (tipo == 2) {
            toolbarColor = ContextCompat.getColor(this, R.color.aplicacao_color);
        } else if (tipo == -1) {
            toolbarColor = ContextCompat.getColor(this, R.color.primary);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(toolbarColor));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        if (tipo == -1) filtro = -2;
        else filtro = -1;

        addConta = findViewById(R.id.ibfab);
        addConta.setOnClickListener(v -> {
            Intent intent = new Intent("com.msk.minhascontas.NOVACONTA");
            // Passa a posição atual do ViewPager local para a CriarConta
            intent.putExtra(MinhasContas.KEY_PAGINA, nrPagina);
            mPaginaListaLauncher.launch(intent);
        });

        mPaginas = new Paginas(getSupportFragmentManager(), getLifecycle());

        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);

        if (nrPagina < 0) {
            nrPagina = 0;
        } else if (nrPagina >= mPaginas.getItemCount()) {
            nrPagina = mPaginas.getItemCount() - 1;
        }

        // --- APLICAÇÃO DA POSIÇÃO CALCULADA OU RECEBIDA ---
        mViewPager.setCurrentItem(nrPagina, false);

        // 4. Observa o LiveData para manter o cache (mes/ano/nrPagina) sincronizado com o ViewModel
        contasViewModel.getCurrentDateState().observe(this, newDateState -> {
            this.mes = newDateState.mes;
            this.ano = newDateState.ano;
            this.nrPagina = newDateState.nrPagina;
            AtualizaActionBar();
        });

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 5. Notifica o ViewModel sobre a mudança de posição
                contasViewModel.setViewPagerPosition(position);
                // O LiveData Observer cuidará de chamar AtualizaActionBar()
                if (ListaMensalContas.mActionMode != null) ListaMensalContas.mActionMode.finish();
            }
        });

        TabLayout tabLayout = findViewById(R.id.tablayout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();
        // Chama pela primeira vez após setar a posição inicial e o cache (mes/ano/nrPagina)
        AtualizaActionBar();
    }

    // MÉTODO CHAVE: Prepara a Intent de retorno para a atividade chamadora.
    private void prepararResultadoParaRetorno() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.getCurrentItem());
        setResult(RESULT_OK, resultIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MinhasContas.KEY_PAGINA, mViewPager.getCurrentItem());
    }

    // Chama o retorno ao pressionar o botão de voltar do dispositivo
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        prepararResultadoParaRetorno();
        super.onBackPressed();
    }

    private void FiltroContas() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(this);

        dialogoBuilder.setTitle(getString(R.string.titulo_filtro));

        if (tipo == 0) {
            classes = res.getStringArray(R.array.FiltroDespesa);
            dialogoBuilder.setItems(classes,
                    (dialog, id) -> {
                        if (id < 6) {
                            filtro = id;
                        } else {
                            filtro = -1;
                        }
                        aplicarFiltroAoFragmentoAtual();
                    });
        }
        if (tipo == 1) {
            classes = res.getStringArray(R.array.FiltroReceita);
            dialogoBuilder.setItems(classes,
                    (dialog, id) -> {
                        if (id < 5) {
                            filtro = id;
                        } else {
                            filtro = -1;
                        }
                        aplicarFiltroAoFragmentoAtual();
                    });
        }

        if (tipo == 2) {
            classes = res.getStringArray(R.array.FiltroAplicacao);
            dialogoBuilder.setItems(classes,
                    (dialog, id) -> {
                        if (id < 3) {
                            filtro = id;
                        }
                        else {
                            filtro = -1;
                        }
                        aplicarFiltroAoFragmentoAtual();
                    });
        }
        AlertDialog alertDialog = dialogoBuilder.create();
        alertDialog.show();
    }

    private void aplicarFiltroAoFragmentoAtual() {
        int currentPage = mViewPager.getCurrentItem();
        Fragment fragment = mPaginas.getFragment(currentPage);
        // Chama o método no fragmento para aplicar o filtro e recarregar a lista
        if (fragment instanceof ListaMensalContas) {
            ((ListaMensalContas) fragment).updateFilter(filtro);
        }
        else {
            Log.e(TAG, "Fragmento atual não é ListaMensalContas, não foi possível aplicar o filtro.");
        }
        AtualizaActionBar();
    }

    private void MontaLista() {
        // O método MontaLista no fragmento é chamado implicitamente via onResume, mas para garantir
        aplicarFiltroAoFragmentoAtual();
    }

    public void AtualizaActionBar() {

        double valores = 0.0D;
        classes = null;

        // NOVO: Obtém o array de meses do ViewModel
        String[] mesesArray = contasViewModel.getStringMonths();

        if (getSupportActionBar() == null) return;

        if (tipo == 1) {
            getSupportActionBar().setTitle(res.getString(R.string.linha_receita));
            classes = res.getStringArray(R.array.FiltroReceita);
        } else if (tipo == 0) {
            classes = res.getStringArray(R.array.FiltroDespesa);
            getSupportActionBar().setTitle(res.getString(R.string.linha_despesa));
        } else if (tipo == 2) {
            classes = res.getStringArray(R.array.FiltroAplicacao);
            getSupportActionBar().setTitle(res.getString(R.string.linha_aplicacoes));
        } else if (tipo == -1) {
            getSupportActionBar().setTitle(res.getString(R.string.app_name));
        }


        if (filtro >= 0) {
            if (tipo == 0 && filtro == 4) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, DBContas.PAGAMENTO_FALTA)) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 0 && filtro == 5) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, DBContas.PAGAMENTO_PAGO)) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 1 && filtro == 3) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, DBContas.PAGAMENTO_FALTA)) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 1 && filtro == 4) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, DBContas.PAGAMENTO_PAGO)) {
                    valores = SomaContas(somador);
                }
            } else {
                try (Cursor somador = dbContas.buscaContasClasse(0, mes, ano, null, tipo, filtro)) {
                    valores = SomaContas(somador);
                }
            }
            if (classes != null && filtro < classes.length) {
                getSupportActionBar().setTitle(classes[filtro]);
            } else {
                getSupportActionBar().setTitle(classes[0]);
            }
            getSupportActionBar().setSubtitle(dinheiro.format(valores));
        } else if (filtro == -1) {
            try (Cursor somador = dbContas.buscaContasTipo(0, mes, ano, null, tipo)) {
                valores = SomaContas(somador);
            }
            getSupportActionBar().setSubtitle(dinheiro.format(valores));
        }

    }

    private double SomaContas(Cursor cursor) {
        double total = 0.0D;
        if (cursor != null && cursor.moveToFirst()) {
            // Assumindo que a COLUNA_VALOR_CONTA é a 8ª coluna (índice 8)
            int valorColunaIndex = 8;
            do {
                total += cursor.getDouble(valorColunaIndex);
            } while (cursor.moveToNext());
        }
        return total;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (tipo == -1) {
            getMenuInflater().inflate(R.menu.barra_botoes_lista, menu);
        } else {
            getMenuInflater().inflate(R.menu.barra_botoes_filtra_lista, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // **MUDANÇA: Deve retornar a posição atual antes de finalizar**
            Log.d(TAG, "Home/Up pressionado: Retornando posição: " + mViewPager.getCurrentItem());
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.getCurrentItem());
            setResult(RESULT_OK, returnIntent);
            finish();
            return true;
        } else if (itemId == R.id.menu_ajustes) {
            Intent intent = new Intent(this, Ajustes.class);
            mPaginaListaLauncher.launch(intent);
        } else if (itemId == R.id.menu_sobre) {
            startActivity(new Intent("com.msk.minhascontas.SOBRE"));
        } else if (itemId == R.id.botao_pesquisar) {
            Intent intent = new Intent("com.msk.minhascontas.BUSCACONTA");
            // Passa a posição atual do ViewPager para a PesquisaContas.java
            intent.putExtra(MinhasContas.KEY_PAGINA, mViewPager.getCurrentItem());
            mPaginaListaLauncher.launch(intent);
            return true;
        } else if (itemId == R.id.botao_filtrar) {
            FiltroContas();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MontaLista();
    }

    public class Paginas extends FragmentStateAdapter {

        private final HashMap<Integer, Fragment> mFragmentos = new HashMap<>();

        public Paginas(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // NOVO: Usa o método estático do ViewModel (isMonthlySummary = true)
            ContasViewModel.DateState dateState = ContasViewModel.calculateDateState(position, true);

            // Fragment é instanciado com os dados do DateState
            Fragment fragment = ListaMensalContas.newInstance(dateState.mes, dateState.ano, tipo, filtro);
            mFragmentos.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return START_PAGE * 2;
        }

        public CharSequence getPageTitle(int position) {

            // OBTÉM OS MESES DO VIEWMODEL
            String[] MesesResumidos = contasViewModel.getStringMonths();
            String[] MesesCompletos = res.getStringArray(R.array.MesesDoAno);

            // NOVO: Usa o método estático do ViewModel para calcular a data
            ContasViewModel.DateState dateState = ContasViewModel.calculateDateState(position, true);

            int mesIndex = dateState.mes;
            String anoAbreviado = String.valueOf(dateState.ano).substring(2);
            String title;

            if (isTablet(getApplicationContext())) {
                title = "  " + MesesCompletos[mesIndex] + "/" + anoAbreviado + "  ";
            } else {
                title = "  " + MesesResumidos[mesIndex] + "/" + anoAbreviado + "  ";
            }
            return title;
        }

        public Fragment getFragment(int position) {
            return mFragmentos.get(position);
        }
    }
}