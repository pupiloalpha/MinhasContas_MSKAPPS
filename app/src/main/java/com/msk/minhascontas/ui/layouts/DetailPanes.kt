package com.msk.minhascontas.ui.layouts

import android.app.Activity
import android.app.backup.BackupManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.features.listas.ListaMensalContasScreen
import com.msk.minhascontas.tarefas.BarraProgresso
import com.msk.minhascontas.tarefas.ExportarExcelTarefa
import com.msk.minhascontas.tarefas.ImportarBancoAntigoTarefa
import com.msk.minhascontas.ui.AjustesScreen
import com.msk.minhascontas.ui.CriarContaScreen
import com.msk.minhascontas.utils.DetailDestination
import com.msk.minhascontas.viewmodel.AjustesViewModel
import com.msk.minhascontas.viewmodel.ContasViewModel
import com.msk.minhascontas.viewmodel.CriarContaViewModel
import java.util.Calendar

@Composable
fun ContasDetailPane(
    dest: DetailDestination.Contas,
    dateState: ContasViewModel.DateState?,
    isMonthly: Boolean,
    position: Int,
    repository: ContasRepository,
    selectedIds: Set<Long> = emptySet(),
    onSelectionChange: (Set<Long>) -> Unit = {},
    onEditContaRequest: ((List<Long>) -> Unit)? = null,
    onCoachClick: ((String) -> Unit)? = null,
    onListaAtualizada: (() -> Unit)? = null
) {
    val mes = dateState?.mes ?: 1
    val ano = dateState?.ano ?: 2026
    val dia = if (isMonthly) 0 else (dateState?.dia ?: 0)

    ListaMensalContasScreen(
        mes = mes,
        ano = ano,
        dia = dia,
        tipo = dest.tipo,
        filtro = dest.filtro,
        categoria = dest.categoria,
        repository = repository,
        selectedIds = selectedIds,
        onSelectionChange = onSelectionChange,
        onEditarConta = onEditContaRequest,
        onCoachClick = onCoachClick,
        onListaAtualizada = onListaAtualizada
    )
}

@Composable
fun CriarContaDetailPane(dateState: ContasViewModel.DateState?, onBack: () -> Unit, onSuccess: () -> Unit) {
    val criarContaViewModel: CriarContaViewModel = viewModel()
    CriarContaScreen(
        initialMes = dateState?.mes ?: -1,
        initialAno = dateState?.ano ?: -1,
        onBack = onBack,
        onSuccess = onSuccess,
        viewModel = criarContaViewModel
    )
}

@Suppress("DEPRECATION")
@Composable
fun AjustesDetailPane(
    onBack: () -> Unit,
    onPreferenceChanged: () -> Unit,
    onNavigateToPersonalizarCategorias: () -> Unit,
    onNavigateToPlanejamento: () -> Unit,
    onRestartReasonChange: (String?) -> Unit,
    isNotificationServiceEnabled: () -> Boolean,
    executeManualBackup: () -> Unit,
    executeManualRestore: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AjustesViewModel = viewModel()
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }

    val safLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.getSharedPreferences("backup", Context.MODE_PRIVATE).edit { putString("backup_uri", uri.toString()) }
            viewModel.loadBackupLocation()
            val folderName = DocumentFile.fromTreeUri(context, uri)?.name ?: ""
            Toast.makeText(context, context.applicationContext.getString(R.string.backup_location_set, folderName), Toast.LENGTH_LONG).show()
            BackupManager(context).dataChanged()
        }
    }

    val excelExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) BarraProgresso(context as Activity, ExportarExcelTarefa(uri, Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR))).execute()
    }
    val excelImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.lerExcel(uris.first())
    }
    val pdfImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.lerPDF(uris.first())
    }
    val dbImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) BarraProgresso(context as Activity, ImportarBancoAntigoTarefa(uris.first())).execute()
    }

    AjustesScreen(
        viewModel = viewModel, onBackClick = onBack,
        onNavigateToPersonalizarCategorias = onNavigateToPersonalizarCategorias,
        onNavigateToPlanejamento = onNavigateToPlanejamento,
        onSelectBackupFolder = { safLauncher.launch(null) }, onExecuteBackup = executeManualBackup, onExecuteRestore = executeManualRestore,
        onExportExcel = { excelExportLauncher.launch("${context.applicationContext.getString(R.string.export_filename_prefix)}${Calendar.getInstance().get(Calendar.YEAR)}.xlsx") },
        onImportExcel = { excelImportLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
        onImportPDF = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
        onImportOldDB = { dbImportLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream")) },
        onDeleteAll = { viewModel.excluirTudo { Toast.makeText(context, context.applicationContext.getString(R.string.dica_exclusao_bd), Toast.LENGTH_SHORT).show(); onRestartReasonChange(Ajustes.REASON_DB_RESTORE) } },
        isNotificationServiceEnabled = isNotificationServiceEnabled(),
        onOpenNotificationSettings = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) },
        onPreferenceChanged = { Toast.makeText(context, context.applicationContext.getString(R.string.dica_restart_app), Toast.LENGTH_LONG).show(); onRestartReasonChange(Ajustes.REASON_PREFERENCES_CHANGED); onPreferenceChanged() },
        isLoading = isLoading, loadingMessage = loadingMessage
    )
}