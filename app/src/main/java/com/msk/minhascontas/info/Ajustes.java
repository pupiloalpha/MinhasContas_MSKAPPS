package com.msk.minhascontas.info;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.ExportarExcel;

import java.util.Calendar;

public class Ajustes extends PreferenceActivity implements
        OnPreferenceClickListener, OnPreferenceChangeListener {

    Cursor cursor = null;
    Toolbar toolbar;
    DBContas dbMinhasContas = new DBContas(this);
    ExportarExcel excel = new ExportarExcel();
    BackupManager android;

    private Preference backup, restaura, apagatudo, versao, exportar;
    private EditTextPreference senha;
    private CheckBoxPreference acesso, pagamento, resumo, saldo, autobkup;
    private PreferenceScreen prefs;
    private String chave, nrVersao, pastaBackUp;
    private PackageInfo info;
    private Resources r = null;
    // VARIAVEIS UTILIZADAS
    private int ano, erro, categorias, ajusteReceita;
    private String[] jan, fev, mar, abr, mai, jun, jul, ago, set, out, nov,
            dez, colunas, linhas, valores;
    private double dvalor0, dvalor1;
    private String despesa, receita, aplicacao;
    private Cursor despesas, receitas, aplicacoes;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferencias);

        prefs = getPreferenceScreen();

        r = getResources();

        // ELEMENTOS DAS PREFERENCIAS QUE SERAO UTILIZADOS
        exportar = (Preference) prefs.findPreference(r
                .getString(R.string.pref_key_exportar));
        backup = (Preference) prefs.findPreference(r
                .getString(R.string.pref_key_bkup));
        restaura = (Preference) prefs.findPreference(r
                .getString(R.string.pref_key_restaura));
        apagatudo = (Preference) prefs.findPreference(r
                .getString(R.string.pref_key_apagatudo));
        versao = (Preference) prefs.findPreference(r
                .getString(R.string.pref_key_versao));
        senha = (EditTextPreference) prefs.findPreference(r
                .getString(R.string.pref_key_senha));
        acesso = (CheckBoxPreference) prefs.findPreference(r
                .getString(R.string.pref_key_acesso));
        pagamento = (CheckBoxPreference) prefs.findPreference(r
                .getString(R.string.pref_key_pagamento));
        resumo = (CheckBoxPreference) prefs.findPreference(r
                .getString(R.string.pref_key_resumo));
        saldo = (CheckBoxPreference) prefs.findPreference(r
                .getString(R.string.pref_key_saldo));
        autobkup = (CheckBoxPreference) prefs.findPreference(r
                .getString(R.string.pref_key_auto_bkup));

        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        nrVersao = info.versionName;

        versao.setSummary(r.getString(R.string.pref_descricao_versao, nrVersao));

        if (acesso.isChecked()) {
            acesso.setSummary(R.string.pref_descricao_acesso_negado);
        } else {
            acesso.setSummary(R.string.pref_descricao_acesso_livre);
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

        if (!pastaBackUp.equals("")){

            backup.setSummary(pastaBackUp);
        }

        toolbar.setTitle(getTitle());

        // CRIA O METODO QUE SERA CHAMADO QUANDO CLICAR
        backup.setOnPreferenceClickListener(this);
        restaura.setOnPreferenceClickListener(this);
        apagatudo.setOnPreferenceClickListener(this);
        exportar.setOnPreferenceClickListener(this);
        acesso.setOnPreferenceClickListener(this);
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
            abrePasta();
        }
        if (chave.equals("restaura")) {
            // Restaura DB
            new BarraProgresso(this, getResources().getString(
                    R.string.dica_titulo_barra), getResources().getString(
                    R.string.dica_barra_recupera), 100, 10).execute();
            dbMinhasContas.open();
            dbMinhasContas.restauraBD(pastaBackUp);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.dica_restaura_bd), Toast.LENGTH_SHORT)
                    .show();
            dbMinhasContas.close();
        }
        if (chave.equals("apagatudo")) {
            // Apaga banco de dados
            // USUARIO ESCOLHE TRANSFORMAR APLICACAO EM DESPESA
            new AlertDialog.Builder(new ContextThemeWrapper(this,
                    R.style.TemaDialogo))
                    .setTitle(R.string.titulo_exclui_tudo)
                    .setMessage(R.string.texto_exclui_tudo)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface pDialogo,
                                                    int pInt) {
                                    dbMinhasContas.open();
                                    dbMinhasContas.excluiTodasAsContas();
                                    dbMinhasContas.excluiTodasAsCategorias();
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

            new BarraProgresso(this, getResources().getString(
                    R.string.dica_titulo_barra), getResources().getString(
                    R.string.dica_barra_exporta), 100, 10).execute();

            CriaArquivoExcel();

            if (erro == 0) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.dica_exporta_excel),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.dica_erro_exporta_excel),
                        Toast.LENGTH_SHORT).show();
            }
        }
        if (chave.equals("acesso")) {

            if (acesso.isChecked()) {
                acesso.setSummary(R.string.pref_descricao_acesso_negado);
            } else {
                acesso.setSummary(R.string.pref_descricao_acesso_livre);
            }

        }
        if (chave.equals("pagamento")) {

            if (pagamento.isChecked()) {
                pagamento.setSummary(R.string.pref_descricao_autopagamento);
            } else {
                pagamento.setSummary(R.string.pref_descricao_editapagamento);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio, Toast.LENGTH_SHORT).show();
        }

        if (chave.equals("resumo")) {

            if (resumo.isChecked()) {
                resumo.setSummary(R.string.pref_descricao_resumo_mensal);
            } else {
                resumo.setSummary(R.string.pref_descricao_resumo_diario);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio, Toast.LENGTH_SHORT).show();
        }

        if (chave.equals("saldo")) {

            if (saldo.isChecked()) {
                saldo.setSummary(R.string.pref_descricao_saldo_somado);
            } else {
                saldo.setSummary(R.string.pref_descricao_saldo_real);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio, Toast.LENGTH_SHORT).show();
        }

        if (chave.equals("autobkup")) {

            if (autobkup.isChecked()) {
                autobkup.setSummary(R.string.pref_descricao_auto_bkup_sim);
            } else {
                autobkup.setSummary(R.string.pref_descricao_auto_bkup_nao);
            }
            Toast.makeText(getApplicationContext(),
                    R.string.dica_texto_reinicio, Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK, null);
        return false;
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.ajustes, new LinearLayout(this), false);
        toolbar = (Toolbar) contentView.findViewById(R.id.msk_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back_white);
        toolbar.setBackgroundColor(Color.parseColor("#FF2B2B2B"));
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

    private void CriaArquivoExcel() {
        // COLOCA VALORES DE DADOS NOS VETORES
        dbMinhasContas.open();
        ano = Calendar.getInstance().get(Calendar.YEAR);
        jan = SaldoMensal(0, ano);
        fev = SaldoMensal(1, ano);
        mar = SaldoMensal(2, ano);
        abr = SaldoMensal(3, ano);
        mai = SaldoMensal(4, ano);
        jun = SaldoMensal(5, ano);
        jul = SaldoMensal(6, ano);
        ago = SaldoMensal(7, ano);
        set = SaldoMensal(8, ano);
        out = SaldoMensal(9, ano);
        nov = SaldoMensal(10, ano);
        dez = SaldoMensal(11, ano);
        dbMinhasContas.close();
        colunas = r.getStringArray(R.array.MesesDoAno);

        NomeLinhas(); // DEFINE O NOME DAS LINHAS DA TABELA

        // CRIA O ARQUIVO EXCEL
        erro = excel.CriaExcel(r.getString(R.string.planilha, ano), jan, fev,
                mar, abr, mai, jun, jul, ago, set, out, nov, dez, colunas,
                linhas, pastaBackUp);

    }

    private String[] SaldoMensal(int mes, int ano) {

        // DEFINE OS NOMES DA LINHAS DA TABELA
        dbMinhasContas.open();
        despesa = getResources().getString(R.string.linha_despesa);
        despesas = dbMinhasContas.buscaCategoriaPorTipo(despesa);
        receita = getResources().getString(R.string.linha_receita);
        receitas = dbMinhasContas.buscaCategoriaPorTipo(receita);
        aplicacao = getResources().getString(R.string.linha_aplicacoes);
        aplicacoes = dbMinhasContas.buscaCategoriaPorTipo(aplicacao);

        // AJUSTE QUANDO EXISTE APENAS UMA RECEITA
        if (receitas.getCount() > 1)
            ajusteReceita = receitas.getCount();
        else
            ajusteReceita = 0;

        categorias = despesas.getCount() + ajusteReceita
                + aplicacoes.getCount() + 7;
        valores = new String[categorias];

        // PREENCHE OS VALORES DE DESPESAS
        if (dbMinhasContas.quantasContasPorTipo(despesa, 0, mes, ano) > 0) {
            valores[0] = String.format("%.2f",
                    dbMinhasContas.somaContas(despesa, 0, mes, ano));
            dvalor0 = dbMinhasContas.somaContas(despesa, 0, mes, ano);
        } else {
            valores[0] = String.format("%.2f", 0.0D);
            dvalor0 = 0.0D;
        }
        for (int i = 0; i < despesas.getCount(); i++) {
            despesas.moveToPosition(i);
            if (dbMinhasContas.quantasContasPorClasse(despesas.getString(1), 0,
                    mes, ano) > 0)
                valores[i + 1] = String.format(
                        "%.2f",
                        dbMinhasContas.somaContasPorClasse(
                                despesas.getString(1), 0, mes, ano));
            else
                valores[i + 1] = String.format("%.2f", 0.0D);
        }
        // VALORES DE RECEITAS
        if (dbMinhasContas.quantasContasPorTipo(receita, 0, mes, ano) > 0) {
            valores[despesas.getCount() + 1] = String.format("%.2f",
                    dbMinhasContas.somaContas(receita, 0, mes, ano));
            dvalor1 = dbMinhasContas.somaContas(receita, 0, mes, ano);
        } else {
            valores[despesas.getCount() + 1] = String.format("%.2f", 0.0D);
            dvalor1 = 0.0D;
        }
        if (receitas.getCount() > 1)
            for (int j = 0; j < receitas.getCount(); j++) {
                receitas.moveToPosition(j);
                if (dbMinhasContas.quantasContasPorClasse(
                        receitas.getString(1), 0, mes, ano) > 0)
                    valores[j + despesas.getCount() + 2] = String.format(
                            "%.2f",
                            dbMinhasContas.somaContasPorClasse(
                                    receitas.getString(1), 0, mes, ano));
                else
                    valores[j + despesas.getCount() + 2] = String.format(
                            "%.2f", 0.0D);
            }
        // VALORES DE APLICACOES
        if (dbMinhasContas.quantasContasPorTipo(aplicacao, 0, mes, ano) > 0)
            valores[despesas.getCount() + ajusteReceita + 2] = String.format(
                    "%.2f", dbMinhasContas.somaContas(aplicacao, 0, mes, ano));
        else
            valores[despesas.getCount() + ajusteReceita + 2] = String.format(
                    "%.2f", 0.0D);
        for (int k = 0; k < aplicacoes.getCount(); k++) {
            aplicacoes.moveToPosition(k);
            if (dbMinhasContas.quantasContasPorClasse(aplicacoes.getString(1),
                    0, mes, ano) > 0)
                valores[k + despesas.getCount() + ajusteReceita + 3] = String
                        .format("%.2f",
                                dbMinhasContas.somaContasPorClasse(
                                        aplicacoes.getString(1), 0, mes, ano));
            else
                valores[k + despesas.getCount() + ajusteReceita + 3] = String
                        .format("%.2f", 0.0D);

        }

        // VALOR DO SALDO MENSAL
        valores[categorias - 4] = String.format("%.2f", dvalor1 - dvalor0);

        // VALOR CONTAS PAGAS
        if (dbMinhasContas.quantasContasPagasPorTipo(despesa, "paguei", 0, mes,
                ano) > 0) {
            valores[categorias - 3] = String.format("%.2f", dbMinhasContas
                    .somaContasPagas(despesa, "paguei", 0, mes, ano));
            dvalor1 = dbMinhasContas.somaContasPagas(despesa, "paguei", 0, mes,
                    ano);
        } else {
            valores[categorias - 3] = String.format("%.2f", 0.0D);
            dvalor1 = 0.0D;
        }

        // VALOR CONTAS A PAGAR
        if (dbMinhasContas.quantasContasPagasPorTipo(despesa, "falta", 0, mes,
                ano) > 0)
            valores[categorias - 2] = String.format("%.2f", dbMinhasContas
                    .somaContasPagas(despesa, "falta", 0, mes, ano));
        else
            valores[categorias - 2] = String.format("%.2f", 0.0D);

        // VALOR DO SALDO ATUAL
        valores[categorias - 1] = String.format("%.2f", dvalor0 - dvalor1);

        dbMinhasContas.close();

        return valores;
    }

    private void NomeLinhas() {

        // DEFINE OS NOMES DA LINHAS DA TABELA
        dbMinhasContas.open();
        despesa = getResources().getString(R.string.linha_despesa);
        despesas = dbMinhasContas.buscaCategoriaPorTipo(despesa);
        receita = getResources().getString(R.string.linha_receita);
        receitas = dbMinhasContas.buscaCategoriaPorTipo(receita);
        aplicacao = getResources().getString(R.string.linha_aplicacoes);
        aplicacoes = dbMinhasContas.buscaCategoriaPorTipo(aplicacao);

        // AJUSTE QUANDO EXISTE APENAS UMA RECEITA
        if (receitas.getCount() > 1)
            ajusteReceita = receitas.getCount();
        else
            ajusteReceita = 0;

        categorias = despesas.getCount() + ajusteReceita
                + aplicacoes.getCount() + 7;
        linhas = new String[categorias];

        // PREENCHE AS LINHAS DA TABELA
        linhas[0] = despesa;
        for (int i = 0; i < despesas.getCount(); i++) {
            despesas.moveToPosition(i);
            linhas[i + 1] = despesas.getString(1);
        }
        // VALORES DE RECEITAS
        linhas[despesas.getCount() + 1] = receita;
        if (receitas.getCount() > 1)
            for (int j = 0; j < receitas.getCount(); j++) {
                receitas.moveToPosition(j);
                linhas[j + despesas.getCount() + 2] = receitas.getString(1);
            }
        // VALORES DE APLICACOES
        linhas[despesas.getCount() + ajusteReceita + 2] = aplicacao;
        for (int k = 0; k < aplicacoes.getCount(); k++) {
            aplicacoes.moveToPosition(k);
            linhas[k + despesas.getCount() + ajusteReceita + 3] = aplicacoes
                    .getString(1);
        }

        // VALOR DO SALDO MENSAL
        linhas[categorias - 4] = getResources().getString(R.string.linha_saldo);

        // VALOR CONTAS PAGAS E A PAGAR
        linhas[categorias - 3] = getResources()
                .getString(R.string.resumo_pagas);

        linhas[categorias - 2] = getResources().getString(
                R.string.resumo_faltam);

        // VALOR DO SALDO ATUAL
        linhas[categorias - 1] = getResources()
                .getString(R.string.resumo_saldo);

        dbMinhasContas.close();

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
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    public void abrePasta()
    {
        startActivityForResult(new Intent(this, EscolhePasta.class), 111);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case 111:

                if (resultCode == RESULT_OK){

                    if (data != null){

                        Bundle extras = data.getExtras();
                        String path = (String) extras.get(EscolhePasta.CHOSEN_DIRECTORY);

                        try {

                            // Cria um Backup do Banco de Dados
                            dbMinhasContas.open();
                            dbMinhasContas.copiaBD(path);
                            android = new BackupManager(getApplicationContext());
                            android.dataChanged();
                            Toast.makeText(getApplicationContext(), getString(R.string.dica_copia_bd), Toast.LENGTH_SHORT).show();
                            dbMinhasContas.close();

                        } catch (Exception e) {

                            Log.e("Seleção de arquivos","Deu erro!!!", e);
                        }
                    }
                }

                break;
        }


    }
}
