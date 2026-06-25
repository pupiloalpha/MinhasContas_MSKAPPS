package com.msk.minhascontas.features.info

import android.app.backup.BackupManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.msk.minhascontas.R
import com.msk.minhascontas.db.AppDatabase
import com.msk.minhascontas.features.auth.LoginGoogle
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.features.planos.PersonalizarCategorias
import com.msk.minhascontas.features.planos.PlanejamentoFinanceiro
import com.msk.minhascontas.tarefas.BarraProgresso
import com.msk.minhascontas.tarefas.ExportarExcelTarefa
import com.msk.minhascontas.tarefas.ImportarBancoAntigoTarefa
import com.msk.minhascontas.tarefas.ImportarExcelTarefa
import com.msk.minhascontas.tarefas.ImportarPDFTarefa
import com.msk.minhascontas.ui.AjustesScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.AjustesUtils
import com.msk.minhascontas.viewmodel.AjustesViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

class Ajustes : ComponentActivity() {

    private lateinit var viewModel: AjustesViewModel

    private var isLoading by mutableStateOf(false)
    private var loadingMessage by mutableStateOf("")

    private lateinit var safDirectoryPickerLauncher: ActivityResultLauncher<Uri?>
    private lateinit var exportadorExcelLauncher: ActivityResultLauncher<String>
    private lateinit var importadorExcelLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var importadorPdfLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var importadorBancoAntigoLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var googleLoginLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val RESULT_RESTART_REQUIRED = 1001
        const val EXTRA_RESTART_REASON = "restart_reason"
        const val REASON_DB_RESTORE = "db_restore"
        const val REASON_PREFERENCES_CHANGED = "preferences_changed"
        private const val TAG = "AjustesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[AjustesViewModel::class.java]

        initLaunchers()

        setContent {
            MinhasContasTheme {
                AjustesScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onNavigateToPersonalizarCategorias = {
                        startActivity(Intent(this, PersonalizarCategorias::class.java))
                    },
                    onNavigateToPlanejamento = {
                        startActivity(Intent(this, PlanejamentoFinanceiro::class.java))
                    },
                    onGoogleLoginClick = {
                        googleLoginLauncher.launch(Intent(this, LoginGoogle::class.java))
                    },
                    onSyncCloudDownload = { executeCloudDownload() },
                    onSyncCloudUpload = { executeCloudUpload() },
                    onSelectBackupFolder = { openSafDirectoryPicker() },
                    onExecuteBackup = { executeManualBackup() },
                    onExecuteRestore = { executeManualRestore() },
                    onExportExcel = {
                        val cal = Calendar.getInstance()
                        val nomeArquivo =
                            "${getString(R.string.export_filename_prefix)}${cal.get(Calendar.YEAR)}_${
                                cal.get(Calendar.MONTH) + 1
                            }.xlsx"
                        exportadorExcelLauncher.launch(nomeArquivo)
                    },
                    onImportExcel = {
                        val mimeTypes = arrayOf(
                            "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.google-apps.spreadsheet",
                            "application/octet-stream"
                        )
                        importadorExcelLauncher.launch(mimeTypes)
                    },
                    onImportPDF = {
                        val mimeTypes = arrayOf("application/pdf")
                        importadorPdfLauncher.launch(mimeTypes)
                    },
                    onImportOldDB = {
                        val mimeTypes = arrayOf(
                            "application/x-sqlite3",
                            "application/octet-stream",
                            "application/vnd.sqlite3"
                        )
                        importadorBancoAntigoLauncher.launch(mimeTypes)
                    },
                    onDeleteAll = {
                        viewModel.excluirTudo {
                            Toast.makeText(
                                this,
                                getString(R.string.dica_exclusao_bd),
                                Toast.LENGTH_SHORT
                            ).show()
                            AjustesUtils.pendingRestartReason = REASON_DB_RESTORE
                            val resultIntent = Intent()
                            resultIntent.putExtra(EXTRA_RESTART_REASON, REASON_DB_RESTORE)
                            setResult(RESULT_RESTART_REQUIRED, resultIntent)
                        }
                    },
                    onLogout = { executeLogout() },
                    isNotificationServiceEnabled = isNotificationServiceEnabled(),
                    onOpenNotificationSettings = {
                        val intent =
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        startActivity(intent)
                    },
                    onPreferenceChanged = {
                        Toast.makeText(
                            this,
                            getString(R.string.dica_restart_app),
                            Toast.LENGTH_LONG
                        ).show()
                        AjustesUtils.pendingRestartReason = REASON_PREFERENCES_CHANGED
                        val resultIntent = Intent()
                        resultIntent.putExtra(EXTRA_RESTART_REASON, REASON_PREFERENCES_CHANGED)
                        setResult(RESULT_RESTART_REQUIRED, resultIntent)
                    },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage
                )
            }
        }
    }

    private fun initLaunchers() {
        safDirectoryPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri: Uri? ->
            if (treeUri != null) {
                contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val sharedPrefBackup = getSharedPreferences("backup", MODE_PRIVATE)
                sharedPrefBackup.edit().putString("backup_uri", treeUri.toString()).apply()
                viewModel.loadBackupLocation()
                Toast.makeText(this, getString(R.string.backup_location_set, DocumentFile.fromTreeUri(this, treeUri)?.name), Toast.LENGTH_LONG).show()
                BackupManager(this).dataChanged()
            }
        }

        exportadorExcelLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri: Uri? ->
            if (uri != null) iniciaExportacaoExcel(uri)
        }

        importadorExcelLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) iniciaImportacaoExcel(uri)
        }

        importadorPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) iniciaImportacaoPDF(uri)
        }

        importadorBancoAntigoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) iniciaImportacaoBancoAntigo(uri)
        }

        googleLoginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) viewModel.updateUserInfo()
        }
    }

    private fun executeLogout() {
        FirebaseAuth.getInstance().signOut()
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@Ajustes)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar estado de credenciais", e)
            }
            viewModel.updateUserInfo()
        }
    }

    // --- Métodos de Lógica (Migrados do Fragment) ---

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun executeCloudDownload() {
        isLoading = true
        loadingMessage = getString(R.string.msg_cloud_downloading)

        viewModel.downloadFromCloud { inseridas ->
            runOnUiThread {
                isLoading = false
                if (inseridas >= 0) {
                    Toast.makeText(this, getString(R.string.msg_cloud_sync_success, inseridas), Toast.LENGTH_LONG).show()
                    AjustesUtils.pendingRestartReason = REASON_DB_RESTORE
                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_RESTART_REASON, REASON_DB_RESTORE)
                    setResult(RESULT_RESTART_REQUIRED, resultIntent)
                } else {
                    Toast.makeText(this, getString(R.string.msg_cloud_download_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun executeCloudUpload() {
        isLoading = true
        loadingMessage = getString(R.string.msg_uploading_data)
        viewModel.syncAllToCloud()
        Handler(Looper.getMainLooper()).postDelayed({
            isLoading = false
            Toast.makeText(this, R.string.google_upload_success, Toast.LENGTH_SHORT).show()
        }, 2000)
    }

    private fun openSafDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val uriString = getSharedPreferences("backup", MODE_PRIVATE).getString("backup_uri", "")
        if (!uriString.isNullOrEmpty()) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriString.toUri())
        }
        safDirectoryPickerLauncher.launch(null)
    }

    private fun executeManualBackup() {
        val uriString = getSharedPreferences("backup", MODE_PRIVATE).getString("backup_uri", "")
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_backup_location_not_set), Toast.LENGTH_LONG).show()
            return
        }
        val backupUri = uriString.toUri()
        copiaBD(backupUri)
        copiaSharedPreferences(backupUri)
        Toast.makeText(this, getString(R.string.backup_complete), Toast.LENGTH_SHORT).show()
    }

    private fun executeManualRestore() {
        val uriString = getSharedPreferences("backup", MODE_PRIVATE).getString("backup_uri", "")
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_restore_location_not_set), Toast.LENGTH_LONG).show()
            return
        }
        val restoreUri = uriString.toUri()
        DBContas.getInstance(this).close()
        AppDatabase.closeDatabase()
        restauraBD(restoreUri)
        restauraSharedPreferences(restoreUri)

        if (FirebaseAuth.getInstance().currentUser != null) {
            DBContas.getInstance(this).open()
            viewModel.syncAllToCloud()
            Toast.makeText(this, getString(R.string.msg_syncing_cloud), Toast.LENGTH_SHORT).show()
        }

        AjustesUtils.pendingRestartReason = REASON_DB_RESTORE
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_RESTART_REASON, REASON_DB_RESTORE)
        setResult(RESULT_RESTART_REQUIRED, resultIntent)
    }

    private fun iniciaExportacaoExcel(uri: Uri) {
        val c = Calendar.getInstance()
        val tarefa = ExportarExcelTarefa(uri, c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
        BarraProgresso(this, tarefa).execute()
    }

    private fun iniciaImportacaoExcel(uri: Uri) {
        BarraProgresso(this, ImportarExcelTarefa(uri)).execute()
    }

    private fun iniciaImportacaoPDF(uri: Uri) {
        BarraProgresso(this, ImportarPDFTarefa(uri)).execute()
    }

    private fun iniciaImportacaoBancoAntigo(uri: Uri) {
        BarraProgresso(this, ImportarBancoAntigoTarefa(uri)).execute()
    }

    // --- Funções de Cópia e Restauração de Arquivos ---

    private fun copiaBD(backupTreeUri: Uri) {
        try {
            val backupDir = DocumentFile.fromTreeUri(this, backupTreeUri) ?: return
            
            // 1. Banco Legado
            val currentDB = getDatabasePath("minhas_contas")
            if (currentDB.exists()) {
                val backupDBFile = backupDir.findFile("minhas_contas.db") ?: backupDir.createFile("application/vnd.sqlite3", "minhas_contas.db")
                backupDBFile?.let { file ->
                    FileInputStream(currentDB).use { fis ->
                        contentResolver.openOutputStream(file.uri)?.use { fos -> fis.copyTo(fos) }
                    }
                }
            }

            // 2. Banco Room
            val roomDB = getDatabasePath("minhas_contas_room_db")
            if (roomDB.exists()) {
                val backupRoomFile = backupDir.findFile("minhas_contas_room.db") ?: backupDir.createFile("application/vnd.sqlite3", "minhas_contas_room.db")
                backupRoomFile?.let { file ->
                    FileInputStream(roomDB).use { fis ->
                        contentResolver.openOutputStream(file.uri)?.use { fos -> fis.copyTo(fos) }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro backup BD", e)
        }
    }

    private fun restauraBD(restoreTreeUri: Uri) {
        try {
            val restoreDir = DocumentFile.fromTreeUri(this, restoreTreeUri) ?: return
            
            // 1. Restaura Banco Legado
            restoreDir.findFile("minhas_contas.db")?.let { backupDBFile ->
                val currentDB = getDatabasePath("minhas_contas")
                currentDB.parentFile?.mkdirs()
                contentResolver.openInputStream(backupDBFile.uri)?.use { fis ->
                    FileOutputStream(currentDB).use { fos -> fis.copyTo(fos) }
                }
            }

            // 2. Restaura Banco Room
            restoreDir.findFile("minhas_contas_room.db")?.let { backupRoomFile ->
                val roomDB = getDatabasePath("minhas_contas_room_db")
                roomDB.parentFile?.mkdirs()
                contentResolver.openInputStream(backupRoomFile.uri)?.use { fis ->
                    FileOutputStream(roomDB).use { fos -> fis.copyTo(fos) }
                }
            }

            Toast.makeText(this, R.string.dica_restaura_bd, Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(TAG, "Erro restaura BD", e)
        }
    }

    private fun copiaSharedPreferences(backupTreeUri: Uri) {
        try {
            val backupDir = DocumentFile.fromTreeUri(this, backupTreeUri) ?: return
            val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles { _, name -> name.endsWith(".xml") }?.forEach { prefFile ->
                    val backupFile = backupDir.findFile(prefFile.name) ?: backupDir.createFile("application/xml", prefFile.name)
                    if (backupFile != null) {
                        FileInputStream(prefFile).use { fis ->
                            contentResolver.openOutputStream(backupFile.uri)?.use { fos ->
                                fis.copyTo(fos)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro backup prefs", e)
        }
    }

    private fun restauraSharedPreferences(restoreTreeUri: Uri) {
        try {
            val restoreDir = DocumentFile.fromTreeUri(this, restoreTreeUri) ?: return
            val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
            if (!sharedPrefsDir.exists()) sharedPrefsDir.mkdirs()
            restoreDir.listFiles().filter { it.isFile && it.name?.endsWith(".xml") == true }.forEach { backupFile ->
                val prefFile = File(sharedPrefsDir, backupFile.name ?: "")
                contentResolver.openInputStream(backupFile.uri)?.use { fis ->
                    FileOutputStream(prefFile).use { fos ->
                        fis.copyTo(fos)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro restaura prefs", e)
        }
    }
}
