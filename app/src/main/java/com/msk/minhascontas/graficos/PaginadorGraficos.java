package com.msk.minhascontas.graficos;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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
    private DBContas dbContas = new DBContas(this);
    // ELEMENTOS DA TELA
    private Paginas mPaginas;
    private ViewPager mViewPager;
    private Resources res;
    // VARIAVEIS DO APLICATIVO
    private String tipo, filtro;
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // AJUSTES DO BANCO DE DADOS
        dbContas.open();
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        // PAGINA CONTENDO MESES
        Bundle localBundle = getIntent().getExtras();
        nrPagina = localBundle.getInt("nr");
        paginas = 120;
        ListaMesesAnos();

        Meses = getResources().getStringArray(R.array.MesResumido);

        // Cria o adaptador que chama o fragmento para cada tela
        mPaginas = new Paginas(getSupportFragmentManager());

        // Define o ViewPager e as telas do adaptador.
        mViewPager = (ViewPager) findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        mViewPager.getAdapter().notifyDataSetChanged();

        // Define a barra de titulo e as tabs
        int normalColor = Color.parseColor("#90FFFFFF");
        int selectedColor = res.getColor(android.R.color.white);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setTabTextColors(normalColor, selectedColor);
        tabLayout.setupWithViewPager(mViewPager);

        ImageButton addConta = (ImageButton) findViewById(R.id.ibfab);
        addConta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.NOVACONTA"), CRIA_CONTA);
            }
        });

        // DEFINE O MES QUE APARECERA NA TELA QUANDO ABRIR
        mViewPager.setCurrentItem(nrPagina);
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
        nrPagina = mViewPager.getCurrentItem();
        Fragment current = mPaginas.getFragment(nrPagina);
        if (current != null) {
            current.onResume();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.barra_botoes_lista, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                break;
            case R.id.menu_ajustes:
                startActivityForResult(new Intent(this, Ajustes.class), CONFIGURACOES);
                break;
            case R.id.menu_sobre:
                startActivity(new Intent("com.msk.minhascontas.SOBRE"));
                break;
            case R.id.botao_pesquisar:
                startActivityForResult(
                        new Intent("com.msk.minhascontas.BUSCACONTA"), BUSCA_CONTA);
                break;
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
    protected void onPause() {
        dbContas.close();
        super.onPause();
    }

    @Override
    protected void onResume() {
        dbContas.open();
        AtualizaGrafico();
        super.onResume();
    }

    /**
     * CLASSE QUE GERENCIA OS FRAGMENTOS
     */
    public class Paginas extends FragmentStatePagerAdapter {

        HashMap<Integer, String> tags = new HashMap<Integer, String>();

        public Paginas(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            // DEFINE PAGINA NA TELA
            return GraficoMensal.newInstance(mesConta[i], anoConta[i]);
        }

        @Override
        public int getCount() {
            // QUANTIDADE DE PAGINAS QUE SERAO MOSTRADAS
            return paginas;
        }

        @Override
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

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if (obj instanceof Fragment) {
                Fragment f = (Fragment) obj;
                String tag = f.getTag();
                tags.put(position, tag);
            }
            return obj;
        }

        public Fragment getFragment(int position) {
            String tag = tags.get(position);
            if (tag == null)
                return null;
            return getSupportFragmentManager().findFragmentByTag(tag);
        }
    }
}