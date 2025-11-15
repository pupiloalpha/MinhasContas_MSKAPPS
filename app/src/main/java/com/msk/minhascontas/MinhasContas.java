package com.msk.minhascontas;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.info.Ajustes;
import com.msk.minhascontas.resumos.ResumoCategoriaDiario;
import com.msk.minhascontas.resumos.ResumoCategoriaMensal;
import com.msk.minhascontas.resumos.ResumoTipoDiario;
import com.msk.minhascontas.resumos.ResumoTipoMensal;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class MinhasContas extends AppCompatActivity {

    private static final int START_PAGE = 200;
    private static final String CURRENT_VIEW_PAGER_POSITION_KEY = "current_view_pager_position";

    private DBContas dbContas;

    private ViewPager2 mViewPager;
    private String[] Meses;
    private int dia, mes, ano, nrPagina;

    private boolean autobkup = true, resumoMensal = true;
    private boolean bloqueioApp = false, atualizaPagamento = false, resumoCategoria = false;

    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    int currentPage = mViewPager.getCurrentItem();
                    Objects.requireNonNull(mViewPager.getAdapter()).notifyItemChanged(currentPage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbContas = DBContas.getInstance(this);

        PreferenciasUsuario();
        if (bloqueioApp) {
            Dialogo();
        }
        setContentView(R.layout.pagina_resumos);

        Resources res = getResources();
        Meses = getResources().getStringArray(R.array.MesResumido);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PermissaoSD();

        Paginas mPaginas = new Paginas(getSupportFragmentManager(), getLifecycle());

        mViewPager = findViewById(R.id.paginas);
        mViewPager.setAdapter(mPaginas);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCurrentDate(position);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tablayout);

        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(mPaginas.getPageTitle(position))
        ).attach();

        ImageButton addConta = findViewById(R.id.ibfab);
        addConta.setOnClickListener(v -> {
            setResult(RESULT_OK, null);
            Intent intent = new Intent("com.msk.minhascontas.NOVACONTA");
            someActivityResultLauncher.launch(intent);
        });

        // Restore ViewPager2 position if available
        if (savedInstanceState != null) {
            int savedPosition = savedInstanceState.getInt(CURRENT_VIEW_PAGER_POSITION_KEY, START_PAGE);
            mViewPager.setCurrentItem(savedPosition, false);
            updateCurrentDate(savedPosition);
        } else {
            mViewPager.setCurrentItem(START_PAGE, false);
            updateCurrentDate(START_PAGE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_VIEW_PAGER_POSITION_KEY, mViewPager.getCurrentItem());
    }

    private void updateCurrentDate(int position) {
        Calendar currentCalendar = Calendar.getInstance();
        int positionOffset = position - START_PAGE;

        if (resumoMensal) {
            currentCalendar.add(Calendar.MONTH, positionOffset);
        } else {
            currentCalendar.add(Calendar.DAY_OF_YEAR, positionOffset);
        }
        dia = currentCalendar.get(Calendar.DAY_OF_MONTH);
        mes = currentCalendar.get(Calendar.MONTH);
        ano = currentCalendar.get(Calendar.YEAR);
        nrPagina = positionOffset;
    }

    private void PreferenciasUsuario() {

        SharedPreferences buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(this);
        autobkup = buscaPreferencias.getBoolean("autobkup", true);
        resumoMensal = buscaPreferencias.getBoolean("resumo", true);
        resumoCategoria = buscaPreferencias.getBoolean("categoria", false);
        bloqueioApp = buscaPreferencias.getBoolean("acesso", false);
        atualizaPagamento = buscaPreferencias.getBoolean("pagamento", false);
        String ordem = buscaPreferencias.getString("ordem", "");
        if (ordem.equals("tipo_conta DESC, classificacao DESC") || ordem.equals("tipo_conta ASC, classificacao ASC")) {
            //PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
            //PreferenceManager.setDefaultValues(this, R.xml.preferencias, true);
        }
    }

    private void Dialogo() {

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
            } else {
                edit.setHint(getString(R.string.senha_errada));
                edit.setHintTextColor(Color.RED);
                edit.setText("");
            }
        });

        // NOVO: Adiciona listener ao TextView de recuperação de senha
        TextView tvEsqueciSenha = dialogo.findViewById(R.id.tvEsqueciSenha);
        tvEsqueciSenha.setOnClickListener(v -> mostrarDialogoRecuperacaoSenha(dialogo)); // Chama a nova função

        AppCompatCheckBox cb = dialogo.findViewById(R.id.cbMostraSenha);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                edit.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                edit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        });
        dialogo.show();
        dialogo.setOnCancelListener(dialog -> finish());
    }

    // NOVO MÉTODO PARA GERENCIAR O FLUXO DE RECUPERAÇÃO
    private void mostrarDialogoRecuperacaoSenha(Dialog dialogoBloqueio) {
        SharedPreferences buscaPreferencias = PreferenceManager.getDefaultSharedPreferences(this);
        // Recupera a pergunta e a resposta do SharedPreferences
        String perguntaId = buscaPreferencias.getString("pergunta_seguranca_id", null);
        String respostaSalva = buscaPreferencias.getString("resposta_secreta", null);

        if (perguntaId == null || respostaSalva == null || respostaSalva.isEmpty()) {
            Toast.makeText(this, getString(R.string.erro_recuperacao_nao_configurada), Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Mostrar a Pergunta de Segurança
        String[] perguntas = getResources().getStringArray(R.array.perguntas_seguranca); // Array de perguntas no strings.xml
        String pergunta = perguntas[Integer.parseInt(perguntaId)]; // Assumindo que perguntaId é o índice do array

        final AppCompatEditText inputResposta = new AppCompatEditText(this);
        inputResposta.setHint(R.string.dica_resposta_secreta);

        new AlertDialog.Builder(this)
                .setTitle(R.string.recuperacao_senha)
                .setMessage(pergunta)
                .setView(inputResposta)
                .setPositiveButton(R.string.confirmar, (dialogRec, which) -> {
                    String respostaDigitada = inputResposta.getText() != null ? inputResposta.getText().toString().trim() : "";

                    // 2. Verificar a Resposta
                    if (respostaDigitada.equalsIgnoreCase(respostaSalva)) {
                        // 3. Resposta correta: Iniciar redefinição de senha
                        mostrarDialogoNovaSenha(dialogoBloqueio);
                    } else {
                        Toast.makeText(this, getString(R.string.senha_errada), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // NOVO MÉTODO PARA REDEFINIR A SENHA
    private void mostrarDialogoNovaSenha(Dialog dialogoBloqueio) {
        // Implemente um AlertDialog com dois campos de texto para Nova Senha e Confirmação
        // Exemplo simplificado:
        final AppCompatEditText inputNovaSenha = new AppCompatEditText(this);
        inputNovaSenha.setHint(R.string.nova_senha);
        inputNovaSenha.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.redefinir_senha)
                .setView(inputNovaSenha)
                .setPositiveButton(R.string.salvar, (dialogNovaSenha, which) -> {
                    String novaSenha = inputNovaSenha.getText() != null ? inputNovaSenha.getText().toString() : "";

                    if (!novaSenha.isEmpty()) {
                        // Salvar a nova senha em SharedPreferences
                        PreferenceManager.getDefaultSharedPreferences(this).edit()
                                .putString("senha", novaSenha)
                                .apply();

                        Toast.makeText(this, getString(R.string.senha_redefinida_sucesso), Toast.LENGTH_LONG).show();

                        // Fechar o diálogo de bloqueio inicial, permitindo o acesso
                        dialogoBloqueio.dismiss();
                    } else {
                        Toast.makeText(this, getString(R.string.erro_senha_vazia), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void AjustesBD() {
        dbContas.confirmaPagamentos();
        dbContas.ajustaRepeticoesContas();

        Calendar c = Calendar.getInstance();
        if (atualizaPagamento) {
            // Replaced deprecated 'atualizaPagamentoContas' with 'atualizaPagamentoContasVencidas'
            dbContas.atualizaPagamentoContasVencidas(c.get(Calendar.DAY_OF_MONTH) + 1, c.get(Calendar.MONTH), c.get(Calendar.YEAR));
        }
    }

    private void PermissaoSD() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        else {
            AjustesBD();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AjustesBD();
        }
        else {
            Toast.makeText(getApplicationContext(), getString(R.string.permissao_negada), Toast.LENGTH_SHORT).show();
        }
    }

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
        } else if (itemId == R.id.menu_sobre) {
            startActivity(new Intent("com.msk.minhascontas.SOBRE"));
        } else if (itemId == R.id.botao_pesquisar) {
            startActivity(new Intent("com.msk.minhascontas.BUSCACONTA"));
        } else if (itemId == R.id.botao_enviar) {

            StringBuilder texto = new StringBuilder(getString(R.string.app_name) + " "
                    + Meses[mes] + "/" + ano + ":");

            // Refactored to use getContasByFilter
            DBContas.ContaFilter filter = new DBContas.ContaFilter()
                    .setMes(mes)
                    .setAno(ano);

            try (Cursor cursor = dbContas.getContasByFilter(filter, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        // Using column names for better readability and maintainability
                        String nomeConta = getColumnString(cursor, DBContas.Colunas.COLUNA_NOME_CONTA);
                        double valorConta = getColumnDouble(cursor, DBContas.Colunas.COLUNA_VALOR_CONTA);
                        texto.append(" ").append(nomeConta).append(" - ").append(valorConta);
                    } while (cursor.moveToNext());
                }
            }

            Intent envelope = new Intent(Intent.ACTION_SEND);
            envelope.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.app_name));
            envelope.putExtra(Intent.EXTRA_TEXT, texto.toString());
            envelope.setType("text/plain");
            startActivity(Intent.createChooser(envelope,
                    getString(R.string.titulo_grafico)
                            + " "
                            + Meses[mes]
                            + "/" + ano + ":"));
        } else if (itemId == R.id.botao_graficos) {
            Bundle dados_mes = new Bundle();
            dados_mes.putInt("dia", dia);
            dados_mes.putInt("mes", mes);
            dados_mes.putInt("ano", ano);
            dados_mes.putInt("nr", nrPagina);
            Intent graficos = new Intent("com.msk.minhascontas.graficos.MEUSGRAFICOS");
            graficos.putExtras(dados_mes);
            startActivity(graficos);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (autobkup) {
            BackupManager.dataChanged("com.msk.minhascontas");
        }
        super.onDestroy();
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
            int offset = position - START_PAGE;
            Fragment fragment;

            if (resumoMensal) {
                cal.add(Calendar.MONTH, offset);
                fragment = resumoCategoria ? ResumoCategoriaMensal.newInstance(mes, ano, offset) : ResumoTipoMensal.newInstance(mes, ano, offset);
            } else {
                cal.add(Calendar.DAY_OF_YEAR, offset);
                fragment = resumoCategoria ? ResumoCategoriaDiario.newInstance(dia, mes, ano) : ResumoTipoDiario.newInstance(dia, mes, ano);
            }
            mFragmentos.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return START_PAGE * 2;
        }

        public CharSequence getPageTitle(int position) {
            Calendar cal = Calendar.getInstance();
            int offset = position - START_PAGE;

            if (resumoMensal) {
                cal.add(Calendar.MONTH, offset);

                return Meses[cal.get(Calendar.MONTH)] + "/" + String.valueOf(cal.get(Calendar.YEAR)).substring(2);
            }
            else {
                cal.add(Calendar.DAY_OF_YEAR, offset);
                return cal.get(Calendar.DAY_OF_MONTH) + "/" + Meses[cal.get(Calendar.MONTH)];
            }
        }

        public Fragment getFragment(int position) {
            return mFragmentos.get(position);
        }
    }

    // Helper methods to safely retrieve data from Cursor
    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getString(columnIndex) : null;
    }

    private double getColumnDouble(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return (columnIndex >= 0) ? cursor.getDouble(columnIndex) : 0.0D;
    }
}
