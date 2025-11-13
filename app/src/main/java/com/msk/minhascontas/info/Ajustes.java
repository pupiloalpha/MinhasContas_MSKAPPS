package com.msk.minhascontas.info;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import missing Toolbar class
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.DBContas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class AjustesFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private static final String TAG = "AjustesFragment";
        private DBContas dbMinhasContas;
        private String pastaBackUp;

        private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted && isAdded()) {
                        openFolderPicker(true);
                    } else if (isAdded()) {
                        Toast.makeText(requireContext(), getString(R.string.titulo_permissao_negada), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && isAdded()) {
                            String path = extras.getString(EscolhePasta.CHOSEN_DIRECTORY);
                            if (path != null) {
                                SharedPreferences sharedPref = requireContext().getSharedPreferences("backup", Context.MODE_PRIVATE);
                                SharedPreferences.Editor edit = sharedPref.edit();
                                edit.putString("backup", path);
                                edit.apply();
                                pastaBackUp = path;
                                Preference backupPref = findPreference(getString(R.string.pref_key_bkup));
                                if (backupPref != null) {
                                    backupPref.setSummary(pastaBackUp);
                                }
                                copiaBD(pastaBackUp);
                                BackupManager android = new BackupManager(requireContext());
                                android.dataChanged();
                                Toast.makeText(requireContext(), getString(R.string.dica_copia_bd), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && isAdded()) {
                            String path = extras.getString(EscolhePasta.CHOSEN_DIRECTORY);
                            if (path != null) {
                                restauraBD(path);
                                Toast.makeText(requireContext(), getString(R.string.dica_restaura_bd), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferencias, rootKey);
            // Only access context if fragment is added, though onCreatePreferences is usually safe.
            if (isAdded()) {
                dbMinhasContas = DBContas.getInstance(requireContext());
                SharedPreferences sharedPref = requireContext().getSharedPreferences("backup", Context.MODE_PRIVATE);
                pastaBackUp = sharedPref.getString("backup", "");
            }

            findAndSetClickListener(getString(R.string.pref_key_bkup));
            findAndSetClickListener(getString(R.string.pref_key_restaura));
            findAndSetClickListener(getString(R.string.pref_key_apagatudo));
            findAndSetClickListener(getString(R.string.pref_key_exportar));

            findAndSetChangeListener(getString(R.string.pref_key_acesso));
            findAndSetChangeListener(getString(R.string.pref_key_categoria));
            findAndSetChangeListener(getString(R.string.pref_key_pagamento));
            findAndSetChangeListener(getString(R.string.pref_key_resumo));
            findAndSetChangeListener(getString(R.string.pref_key_saldo));
            findAndSetChangeListener(getString(R.string.pref_key_senha));
            findAndSetChangeListener(getString(R.string.pref_key_auto_bkup));

            Preference backupPref = findPreference(getString(R.string.pref_key_bkup));
            if (backupPref != null && !pastaBackUp.isEmpty()) {
                backupPref.setSummary(pastaBackUp);
            }
        }
        
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

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            String key = preference.getKey();

            if (key.equals(getString(R.string.pref_key_bkup))) {
                handleBackup();
                return true;
            }

            if (key.equals(getString(R.string.pref_key_restaura))) {
                handleRestore();
                return true;
            }

            if (key.equals(getString(R.string.pref_key_apagatudo))) {
                if (isAdded()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.titulo_exclui_tudo)
                            .setMessage(R.string.texto_exclui_tudo)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                if (isAdded()) {
                                    dbMinhasContas.deleteAllContas();
                                    Toast.makeText(requireContext(), getString(R.string.dica_exclusao_bd), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancelar, null)
                            .show();
                }
                return true;
            }

            return false;
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key.equals(getString(R.string.pref_key_senha))) {
                preference.setSummary(newValue.toString());
            }
            return true;
        }

        private void handleBackup() {
            if (isAdded()) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openFolderPicker(true);
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        }

        private void handleRestore() {
            if (isAdded()) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openFolderPicker(false);
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        }

        private void openFolderPicker(boolean isBackup) {
            if (isAdded()) {
                Intent intent = new Intent(requireContext(), EscolhePasta.class);
                if (isBackup) {
                    folderPickerLauncher.launch(intent);
                } else {
                    intent.putExtra("tipo", ".db"); // <--- ADD THIS LINE
                    filePickerLauncher.launch(intent);
                }
            }
        }

        private void copiaBD(String path) {
            if (isAdded()) {
                try {
                    File sd = new File(path);
                    if (sd.canWrite()) {
                        File currentDB = requireContext().getDatabasePath("minhas_contas");
                        File backupDB = new File(sd, "minhas_contas.db");

                        if (currentDB.exists()) {
                            try (FileInputStream fis = new FileInputStream(currentDB);
                                 FileOutputStream fos = new FileOutputStream(backupDB);
                                 FileChannel src = fis.getChannel();
                                 FileChannel dst = fos.getChannel()) {
                                dst.transferFrom(src, 0, src.size());
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Backup Failed!", e);
                    Toast.makeText(requireContext(), "Backup Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void restauraBD(String path) {
            if (isAdded()) {
                try {
                    File backupDB = new File(path);
                    File currentDB = requireContext().getDatabasePath("minhas_contas");

                    if (backupDB.exists()) {
                        try (FileInputStream fis = new FileInputStream(backupDB);
                             FileOutputStream fos = new FileOutputStream(currentDB);
                             FileChannel src = fis.getChannel();
                             FileChannel dst = fos.getChannel()) {
                            dst.transferFrom(src, 0, src.size());
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Restore Failed!", e);
                    Toast.makeText(requireContext(), "Restore Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}