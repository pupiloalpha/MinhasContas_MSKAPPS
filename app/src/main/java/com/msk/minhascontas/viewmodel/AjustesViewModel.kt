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
import com.google.firebase.auth.FirebaseAuth
import com.msk.minhascontas.db.ContasRepository

class AjustesViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val backupPref: SharedPreferences = application.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val repository: ContasRepository = ContasRepository.getInstance(application)

    private val _appVersion = MutableLiveData<String>()
    val appVersion: LiveData<String> = _appVersion

    private val _backupLocation = MutableLiveData<String>()
    val backupLocation: LiveData<String> = _backupLocation

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> = _userEmail

    init {
        loadAppVersion()
        loadBackupLocation()
        updateUserInfo()
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

    fun updateUserInfo() {
        _userEmail.value = FirebaseAuth.getInstance().currentUser?.email
    }

    fun getPreference(key: String, defaultValue: Boolean): Boolean {
        return sharedPref.getBoolean(key, defaultValue)
    }

    fun setPreference(key: String, value: Boolean) {
        sharedPref.edit().putBoolean(key, value).apply()
    }

    fun getPreference(key: String, defaultValue: String): String {
        return sharedPref.getString(key, defaultValue) ?: defaultValue
    }

    fun setPreference(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    fun excluirTudo(callback: () -> Unit) {
        repository.excluirTudo()
        callback()
    }

    fun syncAllToCloud() {
        repository.syncAllToCloud()
    }

    fun downloadFromCloud(callback: (Int) -> Unit) {
        repository.downloadContasFromCloud { callback(it) }
    }
}