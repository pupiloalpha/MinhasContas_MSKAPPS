package com.msk.minhascontas.info;

import android.Manifest;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.ExportarExcel;

public class Ajustes extends PreferenceActivity implements
        OnPreferenceClickListener, OnPreferenceChangeListener {

    // VARIAVEIS UTILIZADAS
    private static final int ABRE_PASTA = 666;
    private static final int ABRE_ARQUIVO = 777;
    private static final int CRIA_EXCEL = 888;
    private Toolbar toolbar;
    private DBContas dbMinhasContas = new DBContas(this);
    private ExportarExcel excel = new ExportarExcel();
    // ELEMENTOS DA TELA
    private PreferenceScreen prefs;
    private Preference backup, restaura, apagatudo, versao, exportar;
    private EditTextPreference senha;
    private CheckBoxPreference acesso, categoria, pagamento, resumo, saldo, autobkup;
    private String chave, pastaBackUp;
    private PackageInfo info;
    private Resources res = null;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferencias);

        res = getResources();

        prefs = getPreferenceScreen();

        // ELEMENTOS DAS PREFERENCIAS QUE SERAO UTILIZADOS
        exportar = (Preference) prefs.findPreference(res
                .getString(R.string.pref_key_exportar));
        backup = (Preference) prefs.findPreference(res
                .getString(R.string.pref_key_bkup));
        restaura = (Preference) prefs.findPreference(res
                .getString(R.string.pref_key_restaura));
        apagatudo = (Preference) prefs.findPreference(res
                .getString(R.string.pref_key_apagatudo));
        versao = (Preference) prefs.findPreference(res
                .getString(R.string.pref_key_versao));
        senha = (EditTextPreference) prefs.findPreference(res
                .getString(R.string.pref_key_senha));
        acesso = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_acesso));
        categoria = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_categoria));
        pagamento = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_pagamento));
        resumo = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_resumo));
        saldo = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_saldo));
        autobkup = (CheckBoxPreference) prefs.findPreference(res
                .getString(R.string.pref_key_auto_bkup));

        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        String nrVersao = info.versionName;

        versao.setSummary(res.getString(R.string.pref_descricao_versao, nrVersao));

        if (acesso.isChecked()) {
            acesso.setSummary(R.string.pref_descricao_acesso_negado);
        } else {
            acesso.setSummary(R.string.pref_descricao_acesso_livre);
        }

        if (categoria.isChecked()) {
            categoria.setSummary(R.string.pref_descricao_categoria);
        } else {
            categoria.setSummary(R.string.pref_descricao_sem_categoria);
        }
        if (pagamento.isChecked()) {
            pagamento.setSummary(R.string.pref_descricao_autopagamento);
        } else {
            pagamento.setSummary(R.string.pref_descricao_editapagamento);
        }

        if (resumo.isChecked()) {
            resumo.setSummary(R.string.pref_descricao_resumo_mensal);
        } else {
            resumo.setSummary(R.string.pref_descricao_resumo_diario);
        }

        if (saldo.isChecked()) {
            saldo.setSummary(R.string.pref_descricao_saldo_somado);
        } else {
            saldo.setSummary(R.string.pref_descricao_saldo_real);
        }

        if (autobkup.isChecked()) {
            autobkup.setSummary(R.string.pref_descricao_auto_bkup_sim);
        } else {
            autobkup.setSummary(R.string.pref_descricao_auto_bkup_nao);
        }

        SharedPreferences sharedPref = getSharedPreferences("backup", Context.MODE_PRIVATE);
        pastaBackUp = sharedPref.getString("backup", "");

        if (!pastaBackUp.equals("")) {

            backup.setSummary(pastaBackUp);
        }

        toolbar.setTitle(getTitle());

        // CRIA O METODO QUE SERA CHAMADO QUANDO CLICAR
        backup.setOnPreferenceClickListener(this);
        restaura.setOnPreferenceClickListener(this);
        apagatudo.setOnPreferenceClickListener(this);
        exportar.setOnPreferenceClickListener(this);
        acesso.setOnPreferenceClickListener(this);
        categoria.setOnPreferenceClickListener(this);
        pagamento.setOnPreferenceClickListener(this);
        resumo.setOnPreferenceClickListener(this);
        saldo.setOnPreferenceClickListener(this);
        senha.setOnPreferenceChangeListener(this);
        autobkup.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference itemPref) {

        chave = itemPref.getKey();

        if (chave.equals("backup")) {
            // Seleciona pasta para backup
            if (Build.VERSION.SDK_INT >= 23)
                PermissaoSD(ABRE_PASTA);
            else
                abrePasta(ABRE_PASTA);
        }
        if (chave.equals("restaura")) {
            // Seleciona o arquivo backup
            if (Build.VERSION.SDK_INT >= 23)
                PermissaoSD(ABRE_ARQUIVO);
            else
                abrePasta(ABRE_ARQUIVO);
        }
        if (chave.equals("apagatudo")) {
            // Apaga banco de dados
            // USUARIO ESCOLHE TRANSFORMAR APLICACAO EM DESPESA
            new AlertDialog.Builder(this)
                    .setTitle(R.string.titulo_exclui_tudo)
                    .setMessage(R.string.texto_exclui_tudo)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface pDialogo,
                                                    int pInt) {
                                    dbMinhasContas.open();
                                    dbMinhasContas.excluiTodasAsContas();
                                    Toast.makeText(
                                            getApplicationContext(),
                                            getString(R.string.dica_exclusao_bd),
                                            Toast.LENGTH_SHORT).show();
                                    dbMinhasContas.close();
                                }
                            })
                    .setNegativeButton(R.string.cancelar,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface pDialogo,
                                                    int pInt) {
                                    pDialogo.dismiss();
                                }
                            }).show();
        }
        if (chave.equals("excel")) {
            // EXPORTA TESTES PARA EXCEL
            if (Build.VERSION.SDK_INT >= 23)
                PermissaoSD(CRIA_EXCEL);
            else
                abrePasta(CRIA_EXCEL);
        }
        if (chave.equals("acesso")) {
            if (acesso.isChecked()) {
                acesso.setSummary(R.string.pref_descricao_acesso_negado);
            } else {
                acesso.setSummary(R.string.pref_descricao_acesso_livre);
            }
        }
        if (chave.equals("categoria")) {
            if (categoria.isChecked()) {
                categoria.setSummary(R.string.pref_descricao_categoria);
            } else {
                categoria.setSummary(R.string.pref_descricao_sem_categoria);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio,
                    Toast.LENGTH_SHORT).show();
        }
        if (chave.equals("pagamento")) {
            if (pagamento.isChecked()) {
                pagamento.setSummary(R.string.pref_descricao_autopagamento);
            } else {
                pagamento.setSummary(R.string.pref_descricao_editapagamento);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio,
                    Toast.LENGTH_SHORT).show();
        }
        if (chave.equals("resumo")) {
            if (resumo.isChecked()) {
                resumo.setSummary(R.string.pref_descricao_resumo_mensal);
            } else {
                resumo.setSummary(R.string.pref_descricao_resumo_diario);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio,
                    Toast.LENGTH_SHORT).show();
        }
        if (chave.equals("saldo")) {
            if (saldo.isChecked()) {
                saldo.setSummary(R.string.pref_descricao_saldo_somado);
            } else {
                saldo.setSummary(R.string.pref_descricao_saldo_real);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio,
                    Toast.LENGTH_SHORT).show();
        }
        if (chave.equals("autobkup")) {
            if (autobkup.isChecked()) {
                autobkup.setSummary(R.string.pref_descricao_auto_bkup_sim);
            } else {
                autobkup.setSummary(R.string.pref_descricao_auto_bkup_nao);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio,
                    Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, null);
        return false;
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.ajustes, new LinearLayout(this), false);
        toolbar = (Toolbar) contentView.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back_white);
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFFFF"));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        ViewGroup contentWrapper = (ViewGroup) contentView
                .findViewById(R.id.conteudo_ajustes);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);
        getWindow().setContentView(contentView);
    }

    public void abrePasta(int nr) {

        if (nr == ABRE_PASTA) {
            Bundle envelope = new Bundle();
            envelope.putString("tipo", "");
            Intent atividade = new Intent(this, EscolhePasta.class);
            atividade.putExtras(envelope);
            startActivityForResult(atividade, nr);
        }

        if (nr == ABRE_ARQUIVO) {
            Bundle envelope = new Bundle();
            envelope.putString("tipo", "minhas_contas");
            Intent atividade = new Intent(this, EscolhePasta.class);
            atividade.putExtras(envelope);
            startActivityForResult(atividade, nr);
        }

        if (nr == CRIA_EXCEL) {
            new BarraProgresso(this, getResources().getString(
                    R.string.dica_titulo_barra), getResources().getString(
                    R.string.dica_barra_exporta), 100, 10, pastaBackUp).execute();
        }
    }

    private void PermissaoSD(int nr) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, nr);
            }
        } else {
            abrePasta(nr);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay! Do the
            abrePasta(requestCode);
        } else {
            // permission denied, boo! Disable the
            Toast.makeText(getApplicationContext(), getString(R.string.titulo_senha), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference itemPref, Object novaSenha) {
        chave = itemPref.getKey();
        if (chave.equals("senha")) {
            senha.setText(novaSenha.toString());
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ABRE_PASTA:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        String path = (String) extras.get(EscolhePasta.CHOSEN_DIRECTORY);

                        SharedPreferences sharedPref = getSharedPreferences("backup", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString("backup", path);
                        edit.commit();

                        pastaBackUp = sharedPref.getString("backup", "");
                        if (!pastaBackUp.equals("")) {
                            backup.setSummary(pastaBackUp);
                        }
                        // Cria um Backup do Banco de Dados
                        dbMinhasContas.open();
                        dbMinhasContas.copiaBD(pastaBackUp);
                        BackupManager android = new BackupManager(getApplicationContext());
                        android.dataChanged();
                        Toast.makeText(getApplicationContext(), getString(R.string.dica_copia_bd), Toast.LENGTH_SHORT).show();
                        dbMinhasContas.close();
                    }
                }
                break;
            case ABRE_ARQUIVO:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        String path = (String) extras.get(EscolhePasta.CHOSEN_DIRECTORY);

                        // Restaura DB
                        dbMinhasContas.open();
                        dbMinhasContas.restauraBD(path);
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.dica_restaura_bd), Toast.LENGTH_SHORT)
                                .show();
                        dbMinhasContas.close();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        setResult(RESULT_OK, null);
        super.onDestroy();
    }
}