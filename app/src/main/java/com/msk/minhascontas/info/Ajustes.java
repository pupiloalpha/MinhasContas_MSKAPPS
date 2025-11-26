package com.msk.minhascontas.info;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.tarefas.BarraProgresso;
import com.msk.minhascontas.tarefas.BarraProgresso;
import com.msk.minhascontas.tarefas.ExportarExcelTarefa;
import com.msk.minhascontas.tarefas.ImportarExcelTarefa;
import java.util.Calendar; // Para obter o mês e ano atual

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Activity que exibe a tela de Ajustes do aplicativo.
 * Gerencia as preferências do usuário, backup/restauração e exportação/importação de dados.
 */
public class Ajustes extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajustes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.menu_ajustes);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.conteudo_ajustes, new AjustesFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Retorna à Activity anterior (MinhasContas)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragmento que contém a lógica das preferências de ajuste.
     */
    public static class AjustesFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private static final String TAG = "AjustesFragment";

        // --- Variáveis Membro ---
        private DBContas dbMinhasContas;
        private String currentBackupFolderUriString; // URI da pasta de backup persistida
        // private Uri excelFileUri; // Não mais necessário, pois é passado diretamente para a tarefa

        // --- ActivityResultLaunchers para Storage Access Framework (SAF) ---

        // Launcher para selecionar um diretório para backup/restauração (ACTION_OPEN_DOCUMENT_TREE)
        private final ActivityResultLauncher<Intent> safDirectoryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri treeUri = result.getData().getData();
                        if (treeUri != null && isAdded()) {
                            // Persiste a permissão para que o app possa acessar o diretório no futuro
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );

                            SharedPreferences sharedPref = requireContext().getSharedPreferences("backup", Context.MODE_PRIVATE);
                            SharedPreferences.Editor edit = sharedPref.edit();
                            edit.putString("backup_uri", treeUri.toString());
                            edit.apply();
                            currentBackupFolderUriString = treeUri.toString();

                            updateBackupLocationSummary(); // Atualiza o sumário da preferência
                            Toast.makeText(requireContext(), getString(R.string.backup_location_set, DocumentFile.fromTreeUri(requireContext(), treeUri).getName()), Toast.LENGTH_LONG).show();

                            // Notifica o BackupManager do Android para agendar um backup, se auto-backup estiver ativo
                            new BackupManager(requireContext()).dataChanged();
                            Log.d(TAG, "Diretório de backup SAF selecionado e permissão persistida: " + treeUri.toString());
                        }
                    } else {
                        Log.d(TAG, "Seleção de diretório SAF cancelada ou falhou.");
                    }
                }
        );

        // Launcher para CRIAR um novo documento Excel para exportação (ACTION_CREATE_DOCUMENT)
        private final ActivityResultLauncher<String> exportadorExcelLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/vnd.ms-excel"), // MIME type para .xls
                uri -> {
                    if (uri != null) {
                        iniciaExportacaoExcel(uri); // Inicia a tarefa de exportação
                        Log.d(TAG, "URI para exportação de Excel obtida: " + uri.toString());
                    } else {
                        Toast.makeText(requireContext(), R.string.dica_erro_exporta_excel, Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Criação de documento para exportação de Excel cancelada ou falhou.");
                    }
                }
        );

        // Launcher para ABRIR um documento Excel para importação (ACTION_OPEN_DOCUMENT)
        private final ActivityResultLauncher<String[]> importadorExcelLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(), // Permite selecionar múltiplos, mas pegamos o primeiro
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        iniciaImportacaoExcel(uris.get(0)); // Inicia a tarefa de importação com o primeiro URI
                        Log.d(TAG, "URI para importação de Excel obtida: " + uris.get(0).toString());
                    } else {
                        Toast.makeText(requireContext(), R.string.dica_erro_importacao_cancelada, Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Seleção de documento para importação de Excel cancelada ou falhou.");
                    }
                }
        );;


        // --- Métodos de Ciclo de Vida do Fragment ---

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferencias, rootKey);
            if (!isAdded()) {
                Log.e(TAG, "onCreatePreferences: Fragmento não anexado. Não é possível inicializar preferências.");
                return;
            }

            dbMinhasContas = DBContas.getInstance(requireContext());
            SharedPreferences sharedPref = requireContext().getSharedPreferences("backup", Context.MODE_PRIVATE);
            currentBackupFolderUriString = sharedPref.getString("backup_uri", "");

            setAppVersionSummary();
            updateBackupLocationSummary();
            setupPreferenceListeners();
            setupPasswordRecoveryPreference();
            setupExportImportPreferences(); // Configura os listeners para exportar/importar
        }


        // --- Implementação de Preference.OnPreferenceClickListener ---

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            String key = preference.getKey();

            if (key.equals(getString(R.string.pref_key_bkup_select_folder))) {
                openSafDirectoryPicker();
                return true;
            } else if (key.equals(getString(R.string.pref_key_bkup))) {
                executeManualBackup();
                return true;
            } else if (key.equals(getString(R.string.pref_key_restaura))) {
                executeManualRestore();
                return true;
            } else if (key.equals(getString(R.string.pref_key_apagatudo))) {
                showDeleteAllAccountsDialog();
                return true;
            } else if (key.equals(getString(R.string.pref_key_exportar))) {
                // Abre o seletor de arquivos do sistema para escolher o nome e local do arquivo
                Calendar cal = Calendar.getInstance();
                String nomeArquivo = "MinhasContas_Exportacao_"
                        + cal.get(Calendar.YEAR) + "_"
                        + (cal.get(Calendar.MONTH) + 1) + ".xls"; // .xls por conta da biblioteca JXL
                exportadorExcelLauncher.launch(nomeArquivo);
                Log.d(TAG, "onPreferenceClick: Lançando seletor para exportação de Excel com nome: " + nomeArquivo);
                return true;
            } else if (key.equals(getString(R.string.pref_key_importar))) {
            // CORREÇÃO: Usar um array de MIME types para suportar XLS, XLSX e Google Sheets
            // NOTA: A biblioteca JXL só suporta XLS. O suporte a XLSX requer migração para Apache POI.
            String[] mimeTypes = {
                    "application/vnd.ms-excel",                                    // .xls
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                    "application/vnd.google-apps.spreadsheet",                     // Google Sheets
                    "application/octet-stream"                                     // Outros tipos genéricos para compatibilidade
            };
            importadorExcelLauncher.launch(mimeTypes); // Abre seletor com múltiplos tipos MIME
            Log.d(TAG, "onPreferenceClick: Lançando seletor para importação de Excel (XLS, XLSX, Google Sheets).");
            return true;
        }
            return false;
        }

        // --- Implementação de Preference.OnPreferenceChangeListener ---

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key.equals(getString(R.string.pref_key_senha))) {
                preference.setSummary(getString(R.string.pref_descricao_senha_definida));
                Log.d(TAG, "onPreferenceChange: Senha alterada.");
            }
            // Outras lógicas de onPreferenceChange se aplicáveis
            return true;
        }


        // --- Métodos de Inicialização e Configuração de Preferências ---

        private void setAppVersionSummary() {
            String versionName = "N/A";
            try {
                PackageInfo pinfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
                versionName = pinfo.versionName;
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Falha ao obter informações do pacote para o nome da versão.", e);
            }
            Preference versionPref = findPreference(getString(R.string.pref_key_versao));
            if (versionPref != null) {
                versionPref.setSummary(versionName);
                Log.d(TAG, "Sumário da versão do aplicativo definido: " + versionName);
            }
        }

        private void updateBackupLocationSummary() {
            Preference backupLocationPref = findPreference(getString(R.string.pref_key_bkup_select_folder));
            if (backupLocationPref != null && !currentBackupFolderUriString.isEmpty()) {
                Uri uri = Uri.parse(currentBackupFolderUriString);
                DocumentFile documentFile = DocumentFile.fromTreeUri(requireContext(), uri);
                String displayName = (documentFile != null && documentFile.getName() != null) ? documentFile.getName() : uri.getLastPathSegment();
                backupLocationPref.setSummary(getString(R.string.pref_descricao_bkup_local_chosen) + ": " + displayName);
                Log.d(TAG, "Sumário do local de backup atualizado: " + displayName);
            } else if (backupLocationPref != null) {
                backupLocationPref.setSummary(R.string.pref_descricao_bkup_select_folder); // Reseta para o sumário padrão se não houver URI
            }
        }

        private void setupPreferenceListeners() {
            findAndSetClickListener(getString(R.string.pref_key_bkup_select_folder));
            findAndSetClickListener(getString(R.string.pref_key_bkup));
            findAndSetClickListener(getString(R.string.pref_key_restaura));
            findAndSetClickListener(getString(R.string.pref_key_apagatudo));
            findAndSetClickListener(getString(R.string.pref_key_exportar));
            findAndSetClickListener(getString(R.string.pref_key_importar)); // NOVO: Listener para importar

            findAndSetChangeListener(getString(R.string.pref_key_acesso));
            findAndSetChangeListener(getString(R.string.pref_key_categoria));
            findAndSetChangeListener(getString(R.string.pref_key_pagamento));
            findAndSetChangeListener(getString(R.string.pref_key_resumo));
            findAndSetChangeListener(getString(R.string.pref_key_saldo));
            findAndSetChangeListener(getString(R.string.pref_key_senha));
            findAndSetChangeListener(getString(R.string.pref_key_auto_bkup));
            Log.d(TAG, "Listeners de preferências configurados.");
        }

        private void setupPasswordRecoveryPreference() {
            EditTextPreference respostaSecretaPref = findPreference("resposta_secreta");
            if (respostaSecretaPref != null) {
                respostaSecretaPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String novoValor = newValue.toString();
                    String resumo = novoValor.isEmpty() ?
                            getString(R.string.dica_resposta_secreta_ajustes_summary) :
                            getString(R.string.resposta_definida);
                    preference.setSummary(resumo);
                    Log.d(TAG, "Sumário da resposta secreta atualizado.");
                    return true;
                });
                String respostaSalva = respostaSecretaPref.getText();
                if (respostaSalva != null && !respostaSalva.isEmpty()) {
                    respostaSecretaPref.setSummary(getString(R.string.resposta_definida));
                }
                respostaSecretaPref.setOnBindEditTextListener(editText ->
                        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
                );
                Log.d(TAG, "Preferência de recuperação de senha configurada.");
            }
        }

        private void setupExportImportPreferences() {
            // A lógica de click para exportar e importar foi movida para onPreferenceClick
            // Aqui apenas garantimos que os listeners de click estão definidos via setupPreferenceListeners
        }


        // --- Métodos de Backup e Restauração (SAF) ---

        /**
         * Abre o seletor de diretórios do Storage Access Framework (SAF) para o usuário escolher um local de backup.
         */
        private void openSafDirectoryPicker() {
            if (!isAdded()) return;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            if (!currentBackupFolderUriString.isEmpty()) {
                Uri initialUri = Uri.parse(currentBackupFolderUriString);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, initialUri);
                }
            }
            safDirectoryPickerLauncher.launch(intent);
            Log.d(TAG, "Lançando SAF Directory Picker.");
        }

        /**
         * Executa o processo de backup manual usando o diretório SAF escolhido.
         */
        private void executeManualBackup() {
            if (!isAdded()) return;
            if (currentBackupFolderUriString.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_backup_location_not_set), Toast.LENGTH_LONG).show();
                Log.w(TAG, "Backup manual falhou: Local de backup SAF não definido.");
                return;
            }
            Uri backupUri = Uri.parse(currentBackupFolderUriString);
            copiaBD(backupUri);
            copiaSharedPreferences(backupUri);
            Toast.makeText(requireContext(), getString(R.string.backup_complete), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Backup manual concluído.");
        }

        /**
         * Executa o processo de restauração manual usando o diretório SAF escolhido.
         */
        private void executeManualRestore() {
            if (!isAdded()) return;
            if (currentBackupFolderUriString.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_restore_location_not_set), Toast.LENGTH_LONG).show();
                Log.w(TAG, "Restauração manual falhou: Local de restauração SAF não definido.");
                return;
            }
            Uri restoreUri = Uri.parse(currentBackupFolderUriString);
            restauraBD(restoreUri);
            restauraSharedPreferences(restoreUri);
            Log.d(TAG, "Restauração manual concluída.");
            // Toasts já são mostrados por restauraBD e restauraSharedPreferences
        }

        /**
         * Copia o banco de dados interno do aplicativo para o URI de backup SAF especificado.
         * @param backupTreeUri O URI do diretório para salvar o banco de dados.
         */
        private void copiaBD(Uri backupTreeUri) {
            if (!isAdded()) return;
            try {
                DocumentFile backupDir = DocumentFile.fromTreeUri(requireContext(), backupTreeUri);
                if (backupDir == null || !backupDir.canWrite()) {
                    Toast.makeText(requireContext(), getString(R.string.erro_backup_perm_write), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Backup do Banco de Dados Falhou: Não é possível gravar no URI " + backupTreeUri);
                    return;
                }

                File currentDB = requireContext().getDatabasePath("minhas_contas");
                if (currentDB.exists()) {
                    DocumentFile backupDBFile = backupDir.findFile("minhas_contas.db");
                    if (backupDBFile == null || !backupDBFile.exists()) {
                        backupDBFile = backupDir.createFile("application/vnd.sqlite3", "minhas_contas.db");
                    }
                    if (backupDBFile == null) {
                        Log.e(TAG, "Falha ao criar ou encontrar arquivo de backup do BD via SAF.");
                        Toast.makeText(requireContext(), R.string.erro_backup_bd_saf, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try (FileInputStream fis = new FileInputStream(currentDB);
                         OutputStream fos = requireContext().getContentResolver().openOutputStream(backupDBFile.getUri())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                        Log.d(TAG, "Backup do Banco de Dados para SAF concluído com sucesso: " + backupDBFile.getUri());
                    }
                } else {
                    Log.e(TAG, "Banco de dados atual 'minhas_contas' não existe internamente para backup.");
                    Toast.makeText(requireContext(), R.string.erro_backup_nao_encontrado_ou_ilegivel, Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Backup do Banco de Dados Falhou!", e);
                Toast.makeText(requireContext(), R.string.erro_backup_perm_write, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Restaura o banco de dados a partir do URI de backup SAF especificado.
         * @param restoreTreeUri O URI do diretório contendo o backup do banco de dados.
         */
        private void restauraBD(Uri restoreTreeUri) {
            if (!isAdded()) return;
            try {
                DocumentFile restoreDir = DocumentFile.fromTreeUri(requireContext(), restoreTreeUri);
                if (restoreDir == null || !restoreDir.canRead()) {
                    Toast.makeText(requireContext(), R.string.erro_restore_perm_read, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Restauração do Banco de Dados Falhou: Não é possível ler do URI " + restoreTreeUri);
                    return;
                }

                DocumentFile backupDBFile = restoreDir.findFile("minhas_contas.db");
                if (backupDBFile == null || !backupDBFile.exists() || !backupDBFile.canRead()) {
                    Toast.makeText(requireContext(), R.string.erro_backup_nao_encontrado_ou_ilegivel, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Restauração do Banco de Dados Falhou: Backup do BD não encontrado ou ilegível em " + restoreTreeUri + "/minhas_contas.db");
                    return;
                }

                File currentDB = requireContext().getDatabasePath("minhas_contas");
                if (currentDB.getParentFile() != null) {
                    currentDB.getParentFile().mkdirs(); // Cria diretórios pais se não existirem
                }

                try (InputStream fis = requireContext().getContentResolver().openInputStream(backupDBFile.getUri());
                     FileOutputStream fos = new FileOutputStream(currentDB)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    Log.d(TAG, "Restauração do Banco de Dados de SAF concluída com sucesso: " + backupDBFile.getUri());
                    Toast.makeText(requireContext(), R.string.dica_restaura_bd, Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Restauração do Banco de Dados Falhou!", e);
                Toast.makeText(requireContext(), R.string.erro_restore_perm_read, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Copia todos os arquivos SharedPreferences do armazenamento interno do aplicativo
         * para o URI de backup SAF especificado.
         * @param backupTreeUri O URI do diretório para salvar os arquivos SharedPreferences.
         */
        private void copiaSharedPreferences(Uri backupTreeUri) {
            if (!isAdded()) return;
            try {
                DocumentFile backupDir = DocumentFile.fromTreeUri(requireContext(), backupTreeUri);
                if (backupDir == null || !backupDir.canWrite()) {
                    Toast.makeText(requireContext(), getString(R.string.erro_backup_perm_write), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Backup das Preferências Falhou: Não é possível gravar no URI " + backupTreeUri);
                    return;
                }

                File sharedPrefsDir = new File(requireContext().getApplicationInfo().dataDir, "shared_prefs");

                if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory()) {
                    File[] sharedPrefsFiles = sharedPrefsDir.listFiles((dir, name) -> name.endsWith(".xml"));

                    if (sharedPrefsFiles != null) {
                        for (File prefFile : sharedPrefsFiles) {
                            DocumentFile backupFile = backupDir.findFile(prefFile.getName());
                            if (backupFile == null || !backupFile.exists()) {
                                backupFile = backupDir.createFile("application/xml", prefFile.getName());
                            }
                            if (backupFile == null) {
                                Log.e(TAG, "Falha ao criar arquivo de backup das Preferências via SAF: " + prefFile.getName());
                                continue;
                            }

                            try (FileInputStream fis = new FileInputStream(prefFile);
                                 OutputStream fos = requireContext().getContentResolver().openOutputStream(backupFile.getUri())) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, len);
                                }
                                fos.flush();
                                Log.d(TAG, "Preferência compartilhada copiada para SAF: " + prefFile.getName());
                            }
                        }
                    } else {
                        Log.d(TAG, "Nenhum arquivo XML de preferência compartilhada encontrado internamente.");
                    }
                } else {
                    Log.d(TAG, "Diretório shared_prefs não existe internamente.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Backup das Preferências Falhou!", e);
                Toast.makeText(requireContext(), R.string.erro_backup_prefs_geral, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Restaura todos os arquivos SharedPreferences do URI de backup SAF especificado
         * para o armazenamento interno do aplicativo.
         * @param restoreTreeUri O URI do diretório contendo os arquivos SharedPreferences de backup.
         */
        private void restauraSharedPreferences(Uri restoreTreeUri) {
            if (!isAdded()) return;
            try {
                DocumentFile restoreDir = DocumentFile.fromTreeUri(requireContext(), restoreTreeUri);
                if (restoreDir == null || !restoreDir.canRead()) {
                    Toast.makeText(requireContext(), R.string.erro_restore_perm_read, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Restauração das Preferências Falhou: Não é possível ler do URI " + restoreTreeUri);
                    return;
                }

                File sharedPrefsDir = new File(requireContext().getApplicationInfo().dataDir, "shared_prefs");
                if (!sharedPrefsDir.exists()) {
                    sharedPrefsDir.mkdirs();
                }

                DocumentFile[] backupFiles = restoreDir.listFiles();
                if (backupFiles != null) {
                    for (DocumentFile backupFile : backupFiles) {
                        if (backupFile.isFile() && backupFile.getName().endsWith(".xml") && backupFile.canRead()) {
                            File prefFile = new File(sharedPrefsDir, backupFile.getName());
                            try (InputStream fis = requireContext().getContentResolver().openInputStream(backupFile.getUri());
                                 FileOutputStream fos = new FileOutputStream(prefFile)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, len);
                                }
                                fos.flush();
                                Log.d(TAG, "Preferência compartilhada restaurada de SAF: " + backupFile.getName());
                            }
                        }
                    }
                    Toast.makeText(requireContext(), getString(R.string.dica_restaura_prefs) + " " + getString(R.string.dica_restart_app), Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "Nenhum arquivo XML de preferência compartilhada encontrado no diretório SAF: " + restoreTreeUri);
                }
            } catch (IOException e) {
                Log.e(TAG, "Restauração das Preferências Falhou!", e);
                Toast.makeText(requireContext(), R.string.erro_restore_prefs_geral, Toast.LENGTH_SHORT).show();
            }
        }


        // --- Métodos de Exportação e Importação de Excel ---

        /**
         * Inicia a exportação para Excel, chamando a tarefa assíncrona BarraProgresso.
         * @param excelFileUri O URI de destino do arquivo Excel.
         */
        private void iniciaExportacaoExcel(Uri excelFileUri) {
            if (!isAdded()) {
                Log.e(TAG, "Fragment não anexado. Cancelando exportação de Excel.");
                return;
            }
            Context context = requireContext();

            // Obter o mês e ano atual para exportação
            // NOTA: Em Calendar, JANUARY é 0. No DBMinhasContas, Janeiro é 1.
            Calendar c = Calendar.getInstance();
            int mesExportacao = c.get(Calendar.MONTH) + 1;
            int anoExportacao = c.get(Calendar.YEAR);

            // 1. Instanciar a Tarefa com os parâmetros de exportação
            ExportarExcelTarefa tarefa = new ExportarExcelTarefa(excelFileUri, mesExportacao, anoExportacao);

            // 2. Executar a BarraProgresso, que agora gerencia o AsyncTask
            // O Toast de resultado será tratado dentro do onPostExecute da BarraProgresso.
            new BarraProgresso(context, tarefa).execute();
            Log.d(TAG, "Iniciando exportação de Excel para: " + excelFileUri.toString());
        }

        /**
         * Inicia a importação de dados de um arquivo Excel para o banco de dados.
         * @param excelFileUri O URI do arquivo Excel (.xls) a ser lido.
         */
        private void iniciaImportacaoExcel(Uri excelFileUri) {
            if (!isAdded()) {
                Log.e(TAG, "Fragment não anexado. Cancelando importação de Excel.");
                return;
            }
            Context context = requireContext();

            // 1. Instanciar a Tarefa com a URI
            // Assumindo que ImportarExcelTarefa recebe apenas a URI
            ImportarExcelTarefa tarefa = new ImportarExcelTarefa(excelFileUri);

            // 2. Executar a BarraProgresso, que agora gerencia o AsyncTask
            new BarraProgresso(context, tarefa).execute();
            Log.d(TAG, "Iniciando importação de Excel de: " + excelFileUri.toString());
        }


        // --- Métodos de Diálogo ---

        /**
         * Exibe um diálogo de confirmação antes de apagar todas as contas.
         */
        private void showDeleteAllAccountsDialog() {
            if (!isAdded()) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.titulo_exclui_tudo)
                    .setMessage(R.string.texto_exclui_tudo)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        if (isAdded()) {
                            dbMinhasContas.deleteAllContas();
                            Toast.makeText(requireContext(), getString(R.string.dica_exclusao_bd), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Todas as contas foram excluídas do banco de dados.");
                        }
                    })
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
            Log.d(TAG, "Diálogo de confirmação 'Apagar Tudo' exibido.");
        }


        // --- Métodos Auxiliares ---

        private void findAndSetClickListener(String key) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setOnPreferenceClickListener(this);
            }
        }

        private void findAndSetChangeListener(String key) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setOnPreferenceChangeListener(this);
            }
        }
    }
}