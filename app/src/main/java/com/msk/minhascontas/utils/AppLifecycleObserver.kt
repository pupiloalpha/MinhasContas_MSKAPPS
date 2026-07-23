package com.msk.minhascontas.utils

import android.app.backup.BackupManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager

class AppLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        // O aplicativo foi para o background (usuário saiu ou minimizou)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Usaremos uma chave padrão para o backup automático, se não existir assume true para segurança
        if (prefs.getBoolean("backup_automatico_enabled", true)) {
            Log.d("AppLifecycleObserver", "Iniciando checkpoint e backup automático...")
            
            // 1. Consolida os bancos de dados (Checkpoint)
            BackupUtils.flushDatabases(context)
            
            // 2. Notifica o BackupManager do Android que os dados mudaram
            BackupManager(context).dataChanged()
        }
    }
}
