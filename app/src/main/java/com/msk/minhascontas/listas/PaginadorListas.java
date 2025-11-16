package com.msk.minhascontas.listas;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class PaginadorListas extends AppCompatActivity {

    private static final int START_PAGE = 200;
    public static ImageButton addConta;
    private DBContas dbContas;
    private ViewPager2 mViewPager;
    private Resources res;
    private NumberFormat dinheiro;
    private String[] Meses, classes;
    private int mes, ano, tipo, filtro, nrPagina;

    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    int currentPage = mViewPager.getCurrentItem();
                    Objects.requireNonNull(mViewPager.getAdapter()).notifyItemChanged(currentPage);
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

        Bundle localBundle = getIntent().getExtras();
        int nrPaginaOffsetFromMinhasContas = 0; // Default offset to 0 (current month)
        int tipoFromMinhasContas = 0; // Default type
        if (localBundle != null) {
            nrPaginaOffsetFromMinhasContas = localBundle.getInt("nr", 0);
            tipoFromMinhasContas = localBundle.getInt("tipo", 0);
        }
        this.tipo = tipoFromMinhasContas; // Assign to class member

        setContentView(R.layout.pagina_lista_mensal);

        res = getResources();
        Locale current = res.getConfiguration().getLocales().get(0);
        dinheiro = NumberFormat.getCurrencyInstance(current);

        dbContas = DBContas.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int toolbarColor = 0;
        if (tipo == 1) {
            toolbarColor = ContextCompat.getColor(this, R.color.receita_color);
        } else if (tipo == 0) {
            toolbarColor = ContextCompat.getColor(this, R.color.despesa_color);
        } else if (tipo == 2) {
            toolbarColor = ContextCompat.getColor(this, R.color.aplicacao_color);
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

        Meses = getResources().getStringArray(R.array.MesResumido);

        Paginas mPaginas = new Paginas(getSupportFragmentManager(), getLifecycle());

        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCurrentDate(position);
                if (ListaMensalContas.mActionMode != null) ListaMensalContas.mActionMode.finish();
            }
        });

        TabLayout tabLayout = findViewById(R.id.tablayout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();

        addConta = findViewById(R.id.ibfab);
        addConta.setOnClickListener(v -> {
            setResult(RESULT_OK, null);
            Intent intent = new Intent("com.msk.minhascontas.NOVACONTA");
            someActivityResultLauncher.launch(intent);
        });

        // Corretamente calcula a página alvo para PaginadorListas
        // PaginadorListas também usa START_PAGE como seu ponto central, então a página absoluta
        // deve ser START_PAGE + o offset recebido de MinhasContas.
        int targetPage = START_PAGE + nrPaginaOffsetFromMinhasContas;

        mViewPager.setCurrentItem(targetPage, false); // Define o ViewPager para a página absoluta calculada
        this.nrPagina = targetPage; // Atualiza o membro da classe nrPagina
        updateCurrentDate(this.nrPagina); // Chama updateCurrentDate com a página absoluta correta
    }

    private void updateCurrentDate(int position) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.add(Calendar.MONTH, position - START_PAGE);
        mes = currentCalendar.get(Calendar.MONTH);
        ano = currentCalendar.get(Calendar.YEAR);
        nrPagina = position;
        AtualizaActionBar();
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
                        int currentPage = mViewPager.getCurrentItem();
                        Fragment fragment = ((Paginas) Objects.requireNonNull(mViewPager.getAdapter())).getFragment(currentPage);
                        if (fragment instanceof ListaMensalContas) {
                            ((ListaMensalContas) fragment).updateFilter(filtro);
                        }
                        AtualizaActionBar();
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
                        int currentPage = mViewPager.getCurrentItem();
                        Fragment fragment = ((Paginas) Objects.requireNonNull(mViewPager.getAdapter())).getFragment(currentPage);
                        if (fragment instanceof ListaMensalContas) {
                            ((ListaMensalContas) fragment).updateFilter(filtro);
                        }
                        AtualizaActionBar();
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
                        int currentPage = mViewPager.getCurrentItem();
                        Fragment fragment = ((Paginas) Objects.requireNonNull(mViewPager.getAdapter())).getFragment(currentPage);
                        if (fragment instanceof ListaMensalContas) {
                            ((ListaMensalContas) fragment).updateFilter(filtro);
                        }
                        AtualizaActionBar();
                    });
        }
        AlertDialog alertDialog = dialogoBuilder.create();
        alertDialog.show();
    }

    private void MontaLista() {
        int currentPage = mViewPager.getCurrentItem();
        Fragment fragment = ((Paginas) Objects.requireNonNull(mViewPager.getAdapter())).getFragment(currentPage);
        if (fragment instanceof ListaMensalContas) {
            ((ListaMensalContas) fragment).updateFilter(filtro);
        }
        AtualizaActionBar();
    }

    public void AtualizaActionBar() {

        double valores = 0.0D;
        classes = null;

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
        }

        if (filtro >= 0) {

            if (tipo == 0 && filtro == 4) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, "falta")) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 0 && filtro == 5) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, "paguei")) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 1 && filtro == 3) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, "falta")) {
                    valores = SomaContas(somador);
                }
            } else if (tipo == 1 && filtro == 4) {
                try (Cursor somador = dbContas.buscaContasTipoPagamento(0, mes, ano, null, tipo, "paguei")) {
                    valores = SomaContas(somador);
                }
            } else {
                try (Cursor somador = dbContas.buscaContasClasse(0, mes, ano, null, tipo, filtro)) {
                    valores = SomaContas(somador);
                }
            }
            if (classes != null && filtro < classes.length) {
                getSupportActionBar().setTitle(classes[filtro]);
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
            do {
                total += cursor.getDouble(8);
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
            setResult(RESULT_OK, null);
            finish();
        } else if (itemId == R.id.menu_ajustes) {
            Intent intent = new Intent(this, Ajustes.class);
            someActivityResultLauncher.launch(intent);
        } else if (itemId == R.id.menu_sobre) {
            startActivity(new Intent("com.msk.minhascontas.SOBRE"));
        } else if (itemId == R.id.botao_pesquisar) {
            Intent intent = new Intent("com.msk.minhascontas.BUSCACONTA");
            someActivityResultLauncher.launch(intent);
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
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, position - START_PAGE);
            Fragment fragment = ListaMensalContas.newInstance(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), tipo, filtro);
            mFragmentos.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return START_PAGE * 2;
        }

        public CharSequence getPageTitle(int position) {
            String[] MesCompleto = res.getStringArray(R.array.MesesDoAno);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, position - START_PAGE);
            if (isTablet(getApplicationContext())) {
                return "  " + MesCompleto[cal.get(Calendar.MONTH)] + "/" + String.valueOf(cal.get(Calendar.YEAR)).substring(2) + "  ";
            } else {
                return "  " + Meses[cal.get(Calendar.MONTH)] + "/" + String.valueOf(cal.get(Calendar.YEAR)).substring(2) + "  ";
            }
        }

        public Fragment getFragment(int position) {
            return mFragmentos.get(position);
        }
    }
}
