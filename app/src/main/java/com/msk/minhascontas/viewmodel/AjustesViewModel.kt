package com.msk.minhascontas.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.lifecycle.viewModelScope
import com.msk.minhascontas.db.Conta
import kotlinx.coroutines.launch
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.pdf.ContaImportada
import com.msk.minhascontas.features.pdf.ImportSummary
import com.msk.minhascontas.features.pdf.ImportarPDF
import com.msk.minhascontas.features.excel.ImportarExcel
import com.msk.minhascontas.utils.AjustesUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AjustesViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val backupPref: SharedPreferences = application.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val repository: ContasRepository = ContasRepository.getInstance(application)

    private val _appVersion = MutableLiveData<String>()
    val appVersion: LiveData<String> = _appVersion

    private val _backupLocation = MutableLiveData<String>()
    val backupLocation: LiveData<String> = _backupLocation

    private val _importSummary = MutableStateFlow<ImportSummary?>(null)
    val importSummary: StateFlow<ImportSummary?> = _importSummary.asStateFlow()

    private val _importState = MutableStateFlow(ImportState.IDLE)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _pdfProgress = MutableStateFlow(Pair(-1, -1))
    val pdfProgress: StateFlow<Pair<Int, Int>> = _pdfProgress.asStateFlow()

    enum class ImportState {
        IDLE, READING, ANALYZING, SUCCESS, ERROR
    }

    init {
        loadAppVersion()
        loadBackupLocation()
    }

    private fun loadAppVersion() {
        try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            _appVersion.value = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            _appVersion.value = "N/A"
        }
    }

    fun loadBackupLocation() {
        val uriString = backupPref.getString("backup_uri", "")
        if (!uriString.isNullOrEmpty()) {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
            _backupLocation.value = documentFile?.name ?: uri.lastPathSegment ?: ""
        } else {
            _backupLocation.value = ""
        }
    }

    fun getPreference(key: String, defaultValue: Boolean): Boolean {
        return sharedPref.getBoolean(key, defaultValue)
    }

    fun setPreference(key: String, value: Boolean) {
        sharedPref.edit().putBoolean(key, value).commit()
    }

    fun getPreference(key: String, defaultValue: String): String {
        return sharedPref.getString(key, defaultValue) ?: defaultValue
    }

    fun setPreference(key: String, value: String) {
        sharedPref.edit().putString(key, value).commit()
    }

    fun excluirTudo(callback: () -> Unit) {
        viewModelScope.launch {
            repository.excluirTudo()
            callback()
        }
    }

    fun lerPDF(uri: Uri) {
        viewModelScope.launch {
            _importError.value = null
            _pdfProgress.value = Pair(0, 0)
            _importState.value = ImportState.READING
            
            try {
                val importador = ImportarPDF()
                val result = importador.lerPDF(getApplication(), uri) { atual, total ->
                    _pdfProgress.value = Pair(atual, total)
                }
                
                if (result == null) {
                    _importError.value = getApplication<Application>().getString(com.msk.minhascontas.R.string.dica_erro_importacao_pdf_falhou)
                    _importState.value = ImportState.ERROR
                    return@launch
                }

                if (result.contas.isEmpty()) {
                    _importError.value = getApplication<Application>().getString(com.msk.minhascontas.R.string.dica_importacao_vazia)
                    _importState.value = ImportState.ERROR
                    return@launch
                }

                // Etapa 2: Analisar duplicados
                _importState.value = ImportState.ANALYZING
                val duplicados = repository.contarDuplicados(result.contas.map { it.conta })
                
                val finalResult = result.copy(totalDuplicados = duplicados)
                _importSummary.value = finalResult
                _importState.value = ImportState.SUCCESS
            } catch (t: Throwable) {
                _importError.value = t.message ?: getApplication<Application>().getString(com.msk.minhascontas.R.string.erro_desconhecido)
                _importState.value = ImportState.ERROR
            }
        }
    }

    fun lerExcel(uri: Uri) {
        viewModelScope.launch {
            _importError.value = null
            _pdfProgress.value = Pair(0, 0)
            _importState.value = ImportState.READING
            
            try {
                val importador = ImportarExcel()
                val result = importador.lerExcel(getApplication(), uri) { atual, total ->
                    _pdfProgress.value = Pair(atual, total)
                }
                
                if (result == null) {
                    _importError.value = getApplication<Application>().getString(com.msk.minhascontas.R.string.dica_erro_importacao_excel_falhou)
                    _importState.value = ImportState.ERROR
                    return@launch
                }

                if (result.contas.isEmpty()) {
                    _importError.value = getApplication<Application>().getString(com.msk.minhascontas.R.string.import_no_records)
                    _importState.value = ImportState.ERROR
                    return@launch
                }

                // Etapa 2: Analisar duplicados
                _importState.value = ImportState.ANALYZING
                val duplicados = repository.contarDuplicados(result.contas.map { it.conta })
                
                val finalResult = result.copy(totalDuplicados = duplicados)
                _importSummary.value = finalResult
                _importState.value = ImportState.SUCCESS
            } catch (t: Throwable) {
                _importError.value = t.message ?: getApplication<Application>().getString(com.msk.minhascontas.R.string.erro_desconhecido)
                _importState.value = ImportState.ERROR
            }
        }
    }

    fun confirmarImportacao(contas: List<ContaImportada>, gerarFuturas: Boolean, onFinish: (Int) -> Unit) {
        viewModelScope.launch {
            // Usa o importador de PDF para a persistência, pois a lógica é idêntica
            val importador = ImportarPDF()
            val result = importador.confirmarImportacao(getApplication(), contas, gerarFuturas)
            AjustesUtils.pendingDataRefresh = true
            _importSummary.value = null
            _importState.value = ImportState.IDLE
            onFinish(result)
        }
    }

    fun cancelarImportacao() {
        _importSummary.value = null
        _importState.value = ImportState.IDLE
    }

    fun confirmarLimpezaEDuplicados() {
        _importSummary.value = _importSummary.value?.copy(totalDuplicados = 0)
    }
}
