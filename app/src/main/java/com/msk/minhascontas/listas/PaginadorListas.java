package com.msk.minhascontas.listas;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
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

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class PaginadorListas extends AppCompatActivity {

    private static final int BUSCA_CONTA = 111;
    private static final int CONFIGURACOES = 222;
    private static final int CRIA_CONTA = 333;
    private static int[] mesConta, anoConta;
    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas = new DBContas(this);
    // ELEMENTOS DA TELA
    private Paginas mPaginas;
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private Resources res;
    private NumberFormat dinheiro;
    // VARIAVEIS DO APLICATIVO
    private String tipo, filtro;
    private String[] Meses, classes;
    private int mes, ano, paginas, nrPagina;

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pagina_lista_mensal);

        res = getResources();
        Locale current = res.getConfiguration().locale;
        dinheiro = NumberFormat.getCurrencyInstance(current);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // AJUSTES DO BANCO DE DADOS
        dbContas.open();
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        // PEGA O ANO ATUAL PARA DEFINIR A PRIMEIRA TELA
        Bundle localBundle = getIntent().getExtras();
        ano = localBundle.getInt("ano");
        mes = localBundle.getInt("mes");
        tipo = localBundle.getString("tipo");
        filtro = "";

        // PAGINA CONTENDO MESES
        nrPagina = 12;
        paginas = 24;
        ListaMesesAnos();

        Meses = getResources().getStringArray(R.array.MesResumido);

        // Cria o adaptador que chama o fragmento para cada tela
        mPaginas = new Paginas(getSupportFragmentManager());

        // Define o ViewPager e as telas do adaptador.
        mViewPager = (ViewPager) findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        mViewPager.getAdapter().notifyDataSetChanged();
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                nrPagina = position;
                AtualizaActionBar();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (ListaMensalContas.mActionMode != null) ListaMensalContas.mActionMode.finish();
            }
        });

        // Define a barra de titulo e as tabs
        int normalColor = Color.parseColor("#90FFFFFF");
        int selectedColor = res.getColor(android.R.color.white);

        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setTabTextColors(normalColor, selectedColor);
        tabLayout.setupWithViewPager(mViewPager);

        AtualizaActionBar();

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
        int n = c.get(Calendar.YEAR) - 1;
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

    private void FiltroContas() {
        AlertDialog.Builder dialogoBuilder = new AlertDialog.Builder(this);

        // set title
        dialogoBuilder.setTitle(getString(R.string.titulo_filtro));

        if (tipo.equals(res.getString(R.string.linha_despesa))) {

            classes = res.getStringArray(R.array.FiltroDespesa);

            // set dialog message
            dialogoBuilder.setItems(classes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // DEFINE CONTEUDO LISTA COM FILTRO
                            if (id < 4) {
                                filtro = classes[id];
                            } else if (id == 4) {
                                filtro = "falta";
                            } else if (id == 5) {
                                filtro = "paguei";
                            } else {
                                filtro = "";
                            }
                            nrPagina = mViewPager.getCurrentItem();
                            mPaginas = new Paginas(getSupportFragmentManager());
                            mViewPager.setAdapter(mPaginas);
                            mViewPager.setCurrentItem(nrPagina);
                            MontaLista();
                        }
                    });
        }

        if (tipo.equals(res.getString(R.string.linha_aplicacoes))) {

            classes = res.getStringArray(R.array.FiltroAplicacao);

            // set dialog message
            dialogoBuilder.setItems(classes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // DEFINE CONTEUDO LISTA COM FILTRO
                            if (id < 3) {
                                filtro = classes[id];
                            } else {
                                filtro = "";
                            }
                            nrPagina = mViewPager.getCurrentItem();
                            mPaginas = new Paginas(getSupportFragmentManager());
                            mViewPager.setAdapter(mPaginas);
                            mViewPager.setCurrentItem(nrPagina);
                            AtualizaActionBar();
                            MontaLista();
                        }
                    });
        }

        // create alert dialog
        AlertDialog alertDialog = dialogoBuilder.create();
        // show it
        alertDialog.show();
    }

    private void MontaLista() {
        nrPagina = mViewPager.getCurrentItem();
        Fragment current = mPaginas.getFragment(nrPagina);
        if (current != null) {
            current.onResume();
        }
        AtualizaActionBar();
    }

    public void AtualizaActionBar() {

        mes = mesConta[nrPagina];
        ano = anoConta[nrPagina];

        dbContas.open();

        ColorDrawable cor;

        if (tipo.equals(res.getString(R.string.linha_receita))) {
            cor = new ColorDrawable(Color.parseColor("#FF0099CC"));
            getSupportActionBar().setBackgroundDrawable(cor);
            tabLayout.setBackgroundColor(Color.parseColor("#FF0099CC"));
            tabLayout.setTabTextColors(Color.parseColor("#90FFFFFF"), res.getColor(R.color.branco));

        } else if (tipo.equals(res.getString(R.string.linha_despesa))) {
            cor = new ColorDrawable(Color.parseColor("#FFCC0000"));
            getSupportActionBar().setBackgroundDrawable(cor);
            tabLayout.setBackgroundColor(Color.parseColor("#FFCC0000"));
            tabLayout.setTabTextColors(Color.parseColor("#90FFFFFF"), res.getColor(R.color.branco));

        } else if (tipo.equals(res.getString(R.string.linha_aplicacoes))) {
            cor = new ColorDrawable(Color.parseColor("#FF669900"));
            getSupportActionBar().setBackgroundDrawable(cor);
            tabLayout.setBackgroundColor(Color.parseColor("#FF669900"));
            tabLayout.setTabTextColors(Color.parseColor("#90FFFFFF"), res.getColor(R.color.branco));
        }

        double valores;
        if (!filtro.equals("")) {

            // DEFINE TITULO LISTA COM FILTRO
            if (dbContas.quantasContasPorClasse(
                    filtro, 0, mes, ano) > 0)
                valores = dbContas.somaContasPorClasse(
                        filtro, 0, mes, ano);
            else
                valores = 0.0D;

            if (filtro.equals("paguei") || filtro.equals("falta")) {
                if (dbContas.quantasContasPagasPorTipo(tipo,
                        filtro, 0, mes, ano) > 0)
                    valores = dbContas.somaContasPagas(tipo,
                            filtro, 0, mes, ano);
                else
                    valores = 0.0D;
            }

            if (filtro.equals("paguei")) {
                getSupportActionBar().setTitle(res.getString(R.string.resumo_pagas));
            } else if (filtro.equals("falta")) {
                getSupportActionBar().setTitle(res.getString(R.string.resumo_faltam));
            } else {
                getSupportActionBar().setTitle(filtro);
            }
            getSupportActionBar().setSubtitle(dinheiro.format(valores));
        } else {
            // DEFINE TITULO LISTA SEM FILTRO
            if (dbContas.quantasContasPorTipo(
                    tipo, 0, mes, ano) > 0)
                valores = dbContas.somaContas(
                        tipo, 0, mes, ano);
            else
                valores = 0.0D;

            if (!tipo.equals("todas")) {
                getSupportActionBar().setTitle(tipo);
                getSupportActionBar().setSubtitle(dinheiro.format(valores));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (tipo.equals(res.getString(R.string.linha_despesa))
                || tipo.equals(res.getString(R.string.linha_aplicacoes))) {

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
                dbContas.close();
                finish();
                break;
            case R.id.menu_ajustes:
                setResult(RESULT_OK, null);
                startActivityForResult(new Intent(this, Ajustes.class), CONFIGURACOES);
                break;
            case R.id.menu_sobre:
                setResult(RESULT_OK, null);
                startActivity(new Intent("com.msk.minhascontas.SOBRE"));
                break;
            case R.id.botao_pesquisar:
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.BUSCACONTA"), BUSCA_CONTA);
                break;
            case R.id.botao_filtrar:
                FiltroContas();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            MontaLista();
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
        MontaLista();
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
            return ListaMensalContas.newInstance(mesConta[i], anoConta[i], tipo, filtro);
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