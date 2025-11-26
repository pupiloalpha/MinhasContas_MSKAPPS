   package com.msk.minhascontas.graficos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.msk.minhascontas.viewmodel.ContasViewModel.DateState;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class PaginadorGraficos extends AppCompatActivity {

    private static final String TAG = "PaginadorGraficos";
    private static final int START_PAGE = MinhasContas.START_PAGE;
    private static final int BUSCA_CONTA = 111;
    private static final int CONFIGURACOES = 222;

    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas;
    // ELEMENTOS DA TELA
    private Paginas mPaginas;
    private ViewPager2 mViewPager;
    private Resources res;
    // NOVO: ViewModel para gerenciar a posição e a data
    private ContasViewModel contasViewModel;

    // VARIÁVEIS DE DATA ATUAL (para ActionBar, etc.)
    private int mes, ano, nrPagina;

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_graficos);

        dbContas = DBContas.getInstance(this);

        // 1. Inicializa o ViewModel (Escopo da Activity)
        contasViewModel = new ViewModelProvider(this).get(ContasViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        res = getResources();

        Bundle extras = getIntent().getExtras();
        int initialPosition = START_PAGE;

        if (extras != null) {
            // O ViewModel recalcula mes/ano com base na posição
            initialPosition = extras.getInt(MinhasContas.KEY_PAGINA, START_PAGE);
            Log.d(TAG, "Posição inicial recebida (KEY_PAGINA): " + initialPosition);
        }

        if (savedInstanceState != null) {
            initialPosition = savedInstanceState.getInt(MinhasContas.KEY_PAGINA, initialPosition);
            Log.d(TAG, "Posição restaurada via savedInstanceState: " + initialPosition);
        }

        // 2. Sincroniza a posição inicial com o ViewModel
        contasViewModel.setViewPagerPosition(initialPosition);

        // 3. Obtém o estado inicial do ViewModel para inicializar as variáveis de cache (mes/ano/nrPagina)
        DateState dateState = contasViewModel.getCurrentDateState().getValue();
        if (dateState != null) {
            this.mes = dateState.mes;
            this.ano = dateState.ano;
            this.nrPagina = dateState.nrPagina;
        } else {
            this.nrPagina = initialPosition;
        }
        initialPosition = this.nrPagina; // Garante que a posição final inicializada é usada no ViewPager

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        Paginas mPaginas = new Paginas(getSupportFragmentManager(), getLifecycle());

        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        // Garante a inicialização na posição sincronizada
        mViewPager.setCurrentItem(initialPosition, false);
        Log.d(TAG, "ViewPager inicializado em: " + initialPosition);

        // 4. Observa o LiveData para manter o cache (mes/ano/nrPagina) sincronizado com o ViewModel
        contasViewModel.getCurrentDateState().observe(this, newDateState -> {
            this.mes = newDateState.mes;
            this.ano = newDateState.ano;
            this.nrPagina = newDateState.nrPagina;
            // Atualiza o subtítulo via Observer
            Objects.requireNonNull(getSupportActionBar()).setSubtitle(mPaginas.getPageTitle(newDateState.nrPagina));
        });

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 5. Notifica o ViewModel sobre a mudança de posição
                contasViewModel.setViewPagerPosition(position);
                // O LiveData Observer cuidará de atualizar o cache (mes/ano/nrPagina) e o subtítulo.
            }
        });

        TabLayout tabLayout = findViewById(R.id.tablayout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // **MUDANÇA: Salva a posição atual sob a chave KEY_PAGINA para restauração**
        outState.putInt(MinhasContas.KEY_PAGINA, mViewPager.getCurrentItem());
    }

    private void AtualizaGrafico() {
        // A atualização é feita automaticamente pelo adapter.
    }

    // MÉTODO DE RETORNO (Alterado para usar a constante centralizada)
    private void prepararResultadoParaRetorno() {
        Log.d(TAG, "onBackPressed: Retornando posição: " + mViewPager.getCurrentItem());
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, mViewPager.getCurrentItem());
        setResult(RESULT_OK, returnIntent);
    }

    // MÉTODO DE RETORNO AO PRESSIONAR "VOLTAR" (Não alterado)
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        prepararResultadoParaRetorno();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.barra_botoes_lista, menu);
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
            startActivityForResult(new Intent(this, Ajustes.class), CONFIGURACOES);
        } else if (itemId == R.id.menu_sobre) {
            startActivity(new Intent("com.msk.minhascontas.SOBRE"));
        } else if (itemId == R.id.botao_pesquisar) {
            startActivityForResult(
                    new Intent("com.msk.minhascontas.BUSCACONTA"), BUSCA_CONTA);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            AtualizaGrafico();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AtualizaGrafico();
    }

    public class Paginas extends FragmentStateAdapter {

        // HashMap para manter referências aos fragmentos, como em PaginadorListas
        private final HashMap<Integer, Fragment> mFragmentos = new HashMap<>();

        public Paginas(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            // NOVO: Usa o método estático do ViewModel (isMonthlySummary = true)
            DateState dateState = ContasViewModel.calculateDateState(position, true);

            // Fragment é instanciado com os dados do DateState
            Fragment fragment = GraficoMensal.newInstance(dateState.mes, dateState.ano);
            mFragmentos.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            // Usa o range grande de PaginadorListas para uniformidade.
            return START_PAGE * 2;
        }

        public CharSequence getPageTitle(int position) {

            // OBTÉM OS MESES DO VIEWMODEL
            String[] MesesResumidos = contasViewModel.getStringMonths();
            String[] MesesCompletos = res.getStringArray(R.array.MesesDoAno);

            // NOVO: Usa o método estático do ViewModel para calcular a data
            DateState dateState = ContasViewModel.calculateDateState(position, true);

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