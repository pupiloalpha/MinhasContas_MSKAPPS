package com.msk.minhascontas;

import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;
import com.msk.minhascontas.resumos.ResumoDiario;
import com.msk.minhascontas.resumos.ResumoMensal;

import java.util.Calendar;

public class MinhasContas extends AppCompatActivity implements
        OnPageChangeListener {

    private static int[] diaConta, mesConta, anoConta;
    private final Context contexto = this;
    private final Calendar c = Calendar.getInstance();
    // CLASSE DO BANCO DE DADOS
    private DBContas dbContas = new DBContas(this);
    // ELEMENTOS DA TELA
    private Paginas mPaginas;
    private ViewPager mViewPager;
    private Resources r;
    private SharedPreferences buscaPreferencias = null;
    private ActionBar actionBar;
    // VARIAVEIS DO APLICATIVO
    private Boolean autobkup = true;
    private Boolean resumoMensal = true;
    private Boolean bloqueioApp = false;
    private Boolean atualizaPagamento = false;
    private String senhaUsuario, tipo, categoria;
    private String[] despesas, receitas;
    private String[] Meses;
    private int dia, mes, ano, paginas, nrPagina;

    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenciasUsuario();
        if (bloqueioApp) {
            Dialogo();
        }
        setContentView(R.layout.pagina_resumos);

        r = getResources();
        ImageButton addConta = (ImageButton) findViewById(R.id.ibfab);
        addConta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                startActivityForResult(
                        new Intent("com.msk.minhascontas.NOVACONTA"), 1);
            }
        });

        // AJUSTES DO BANCO DE DADOS
        dbContas.open();
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        // PEGA O ANO ATUAL PARA DEFINIR A PRIMEIRA TELA
        ano = c.get(Calendar.YEAR);
        mes = c.get(Calendar.MONTH);
        dia = c.get(Calendar.DAY_OF_MONTH);
        AjustesBD();
        dia = c.get(Calendar.DAY_OF_MONTH);

        if (resumoMensal) {
            // PAGINA CONTENDO MESES
            nrPagina = 12;
            paginas = 24;
            ListaMesesAnos();
        } else {
            // PAGINA CONTENDO DIAS
            nrPagina = c.get(Calendar.DAY_OF_YEAR) - 1;
            if (Math.IEEEremainder(ano, 4.0D) == 0)
                paginas = 366;
            else
                paginas = 365;
            ListaDiasMesesAno();
        }

        Meses = getResources().getStringArray(R.array.MesResumido);

        // Cria o adaptador que chama o fragmento para cada tela
        mPaginas = new Paginas(getSupportFragmentManager());

        // Define o ViewPager e as telas do adaptador.
        mViewPager = (ViewPager) findViewById(R.id.paginas);

        mViewPager.setAdapter(mPaginas);
        mViewPager.getAdapter().notifyDataSetChanged();

        // Define a barra de titulo e as tabs
        usarActionBar();

        // DEFINE O MES QUE APARECERA NA TELA QUANDO ABRIR
        mViewPager.setCurrentItem(nrPagina);

        // RECOMNHECE MUDANCA DE PAGINA
        mViewPager.setOnPageChangeListener(this);

    }

    private void PreferenciasUsuario() {

        buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(this);
        autobkup = buscaPreferencias.getBoolean("autobkup", true);
        resumoMensal = buscaPreferencias.getBoolean("resumo", true);
        bloqueioApp = buscaPreferencias.getBoolean("acesso", false);
        atualizaPagamento = buscaPreferencias.getBoolean("pagamento", false);
        senhaUsuario = buscaPreferencias.getString("senha", "");
    }

    private void Dialogo() {

        final Dialog dialogo = new Dialog(contexto, R.style.TemaBloqueio);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(R.layout.tela_bloqueio);
        dialogo.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialogo.getWindow().setLayout(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        dialogo.setCanceledOnTouchOutside(false);

        final AppCompatEditText edit = (AppCompatEditText) dialogo.findViewById(R.id.etSenha);

        Button ok = (Button) dialogo.findViewById(R.id.bEntra);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (senhaUsuario.equals(edit.getText().toString())) {

                    dialogo.dismiss();

                } else {

                    edit.setHint(r.getString(R.string.senha_errada));
                    edit.setHintTextColor(Color.RED);
                    edit.setText("");

                }

            }
        });

        AppCompatCheckBox cb = (AppCompatCheckBox) dialogo.findViewById(R.id.cbMostraSenha);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // checkbox status is changed from uncheck to checked.
                if (!isChecked) {
                    // show password
                    edit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // hide password
                    edit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        dialogo.show();

        dialogo.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }

    private void AjustesBD() {
        // Atualiza pagamentos
        if (atualizaPagamento) {

            dia = dia + 1;

            // db.open();
            dbContas.atualizaPagamentoContas(dia, mes, ano);
            // db.close();
        }

    }

    private void usarActionBar() {

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        // Define a navegaçao por tabs
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // show the given tab
                mViewPager.setCurrentItem(tab.getPosition());

            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab

            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event

            }
        };

        String[] MesCompleto = getResources().getStringArray(R.array.MesesDoAno);

        String title;
        // Adiciona o nome dos meses nas tabs
        for (int i = 0; i < paginas; i++) {

            if (resumoMensal) {

                if (isTablet(this)) {
                    title = "  " + MesCompleto[mesConta[i]] + "/" + (anoConta[i]) % 100 + "  ";
                } else {
                    title = "  " + Meses[mesConta[i]] + "/" + (anoConta[i]) % 100 + "  ";
                }

            } else {
                String s = "" + diaConta[i];
                if (diaConta[i] < 10)
                    s = "0" + diaConta[i];
                title = "  " + s + "/" + Meses[mesConta[i]] + "/" + (anoConta[i]) % 100 + "  ";
            }
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(title)
                            .setTabListener(tabListener));
        }
        actionBar.setSelectedNavigationItem(nrPagina);

    }

    private void ListaMesesAnos() {

        // DEFINE OS MESES E ANOS QUE APARECERAM NA TELA
        mesConta = new int[paginas];
        anoConta = new int[paginas];
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

    private void ListaDiasMesesAno() {

        // DEFINE OS MESES E ANOS QUE APARECERAM NA TELA
        diaConta = new int[paginas];
        mesConta = new int[paginas];
        anoConta = new int[paginas];
        int d = 1;
        int u = 0;
        int n = c.get(Calendar.YEAR);

        for (int i = 0; i < diaConta.length; i++) {

            if (Math.IEEEremainder(n, 4.0D) != 0 && u == 1 && d > 28) {
                d = 1;
                u = 2;
            } else if (Math.IEEEremainder(n, 4.0D) == 0 && u == 1 && d > 29) {
                d = 1;
                u = 2;
            } else {

                if (u == 0 || u == 2 || u == 4 || u == 6 || u == 7 || u == 9
                        || u == 11) {
                    if (d > 31) {
                        d = 1;
                        u = u + 1;
                    }
                } else {
                    if (d > 30) {
                        d = 1;
                        u = u + 1;
                    }
                }

            }

            if (u > 11) {
                d = 1;
                u = 0;
                n = n + 1;
            }

            diaConta[i] = d;
            mesConta[i] = u;
            anoConta[i] = n;
            d++;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.barra_botoes_inicio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        mes = mesConta[mViewPager.getCurrentItem()];
        ano = anoConta[mViewPager.getCurrentItem()];

        switch (item.getItemId()) {

            case R.id.menu_ajustes:
                startActivityForResult(new Intent(this, Ajustes.class), 0);
                break;
            case R.id.menu_sobre:
                startActivity(new Intent("com.msk.minhascontas.SOBRE"));
                break;
            case R.id.botao_pesquisar:
                startActivityForResult(
                        new Intent("com.msk.minhascontas.BUSCACONTA"), 2);
                break;
            case R.id.botao_enviar:

                dbContas.open();
                String aplicacoes = dbContas.mostraContasPorTipo(getResources()
                        .getString(R.string.linha_aplicacoes), mes, ano);
                String despesas = dbContas.mostraContasPorTipo(getResources()
                        .getString(R.string.linha_despesa), mes, ano);
                String receitas = dbContas.mostraContasPorTipo(getResources()
                        .getString(R.string.linha_receita), mes, ano);

                String texto = getResources().getString(R.string.app_name) + " "
                        + Meses[mes] + "/" + ano + "\n" + receitas + "\n"
                        + despesas + "\n" + aplicacoes;

                Intent envelope = new Intent("android.intent.action.SEND");
                envelope.putExtra("android.intent.extra.SUBJECT", getResources()
                        .getString(R.string.app_name));
                envelope.putExtra("android.intent.extra.TEXT", texto);
                envelope.setType("*/*");
                startActivity(Intent.createChooser(envelope, getResources()
                        .getString(R.string.titulo_grafico)
                        + " "
                        + Meses[mes]
                        + "/" + ano + ":"));

                break;
            case R.id.botao_graficos:

                Bundle dataGrafico = new Bundle();
                dataGrafico.putInt("mes", mes);
                dataGrafico.putInt("ano", ano);
                Intent intentGrafico = new Intent(
                        "com.msk.minhascontas.graficos.MEUSGRAFICOS");
                intentGrafico.putExtras(dataGrafico);
                startActivity(intentGrafico);

                break;

        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
        // NAO FAZ NADA
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // NAO FAZ NADA
    }

    @Override
    public void onPageSelected(int arg0) {
        // NAO FAZ NADA
        actionBar.setSelectedNavigationItem(arg0);
        nrPagina = arg0;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mPaginas = new Paginas(getSupportFragmentManager());
            mViewPager.setAdapter(mPaginas);
            mViewPager.setCurrentItem(nrPagina);
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
        mPaginas = new Paginas(getSupportFragmentManager());
        mViewPager.setAdapter(mPaginas);
        mViewPager.setCurrentItem(nrPagina);
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        dbContas.open();

        int i = dbContas.quantasContas();

        if (autobkup && i != 0) {

            SharedPreferences sharedPref = getSharedPreferences("backup", Context.MODE_PRIVATE);
            String pastaBackUp = sharedPref.getString("backup", "");
            dbContas.copiaBD(pastaBackUp);

            BackupManager android = new BackupManager(getApplicationContext());
            android.dataChanged();

        }

        dbContas.close();

        super.onDestroy();
    }

    /**
     * CLASSE QUE GERENCIA OS FRAGMENTOS
     * <p/>
     * *
     */
    public class Paginas extends FragmentStatePagerAdapter {

        public Paginas(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {

            // DEFINE PAGINA NA TELA
            if (resumoMensal)
                return ResumoMensal.newInstance(mesConta[i], anoConta[i]);
            else
                return ResumoDiario.newInstance(diaConta[i], mesConta[i],
                        anoConta[i]);
        }

        @Override
        public int getCount() {
            // QUANTIDADE DE PAGINAS QUE SERAO MOSTRADAS
            return paginas;
        }

    }

}
