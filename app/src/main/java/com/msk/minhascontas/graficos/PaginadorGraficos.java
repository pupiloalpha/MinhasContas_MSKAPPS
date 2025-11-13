package com.msk.minhascontas.graficos;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;

import java.util.Calendar;
import java.util.HashMap;

public class PaginadorGraficos extends AppCompatActivity {

    private static final int BUSCA_CONTA = 111;
    private static final int CONFIGURACOES = 222;
    private static final int CRIA_CONTA = 333;
    private static int[] mesConta, anoConta;
    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas;
    // ELEMENTOS DA TELA
    private Paginas mPaginas;
    private ViewPager2 mViewPager;
    private Resources res;
    // VARIAVEIS DO APLICATIVO
    private String[] Meses;
    private int paginas, nrPagina;

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pagina_graficos);

        res = getResources();
        dbContas = DBContas.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // AJUSTES DO BANCO DE DADOS
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        // PAGINA CONTENDO MESES
        Bundle localBundle = getIntent().getExtras();
        int nrPaginaOffsetFromMinhasContas = 0; // Default para 0 (mês atual)
        if (localBundle != null) {
            // O 'nr' é o offset da MinhasContas.java (0 para o mês atual)
            nrPaginaOffsetFromMinhasContas = localBundle.getInt("nr", 0);
        }

        paginas = 120; // PaginadorGraficos gerencia 120 páginas
        ListaMesesAnos(); // Popula mesConta e anoConta

        Meses = getResources().getStringArray(R.array.MesResumido);

        // Cria o adaptador que chama o fragmento para cada tela
        mPaginas = new Paginas(this);

        // Define o ViewPager e as telas do adaptador.
        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);

        // Define a barra de titulo e as tabs
        TabLayout tabLayout = findViewById(R.id.tablayout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();

        ImageButton addConta = findViewById(R.id.ibfab);
        addConta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.NOVACONTA"), CRIA_CONTA);
            }
        });

        // DEFINE O MES QUE APARECERA NA TELA QUANDO ABRIR
        // Calcula a página correta para PaginadorGraficos baseado no offset de MinhasContas
        // O índice 60 corresponde ao mês/ano atual na lista de 120 meses de PaginadorGraficos
        int currentMonthIndexInPaginadorGraficos = 60;
        int targetPage = currentMonthIndexInPaginadorGraficos + nrPaginaOffsetFromMinhasContas;

        // Garante que a targetPage esteja dentro dos limites do adaptador de PaginadorGraficos
        if (targetPage < 0) {
            targetPage = 0;
        } else if (targetPage >= paginas) { // 'paginas' é 120
            targetPage = paginas - 1;
        }
        mViewPager.setCurrentItem(targetPage);
        this.nrPagina = targetPage; // Atualiza a variável nrPagina da classe PaginadorGraficos
    }

    private void ListaMesesAnos() {

        // DEFINE OS MESES E ANOS QUE APARECERAM NA TELA
        mesConta = new int[paginas];
        anoConta = new int[paginas];
        Calendar c = Calendar.getInstance();
        int u = c.get(Calendar.MONTH);
        int n = c.get(Calendar.YEAR) - 5;
        for (int i = 0; i < mesConta.length; i++) {

            if (u > 11) {
                u = 0;
                n = n + 1;
            }
            mesConta[i] = u;
            anoConta[i] = n;
            u++;
        }
    }

    private void AtualizaGrafico() {
        // A atualização é feita automaticamente pelo adapter.
        // O método getFragment não existe no FragmentStateAdapter.
        // nrPagina = mViewPager.getCurrentItem(); // Não é necessário, já é atualizado no onCreate
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
            finish();
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

        public Paginas(AppCompatActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int i) {
            return GraficoMensal.newInstance(mesConta[i], anoConta[i]);
        }

        @Override
        public int getItemCount() {
            return paginas;
        }

        public CharSequence getPageTitle(int i) {

            String[] MesCompleto = res.getStringArray(R.array.MesesDoAno);
            String title;

            if (isTablet(getApplicationContext())) {
                title = "  " + MesCompleto[mesConta[i]] + "/" + (anoConta[i]) % 100 + "  ";
            } else {
                title = "  " + Meses[mesConta[i]] + "/" + (anoConta[i]) % 100 + "  ";
            }

            return title;
        }
    }
}