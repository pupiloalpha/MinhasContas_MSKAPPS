package com.msk.minhascontas.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.sqlite.db.SimpleSQLiteQuery
import com.msk.minhascontas.db.AppDatabase
import com.msk.minhascontas.db.DBContas
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object BackupUtils {
    private const val TAG = "BackupManager"

    fun flushDatabases(context: Context) {
        try {
            // 1. Banco Legado
            val dbLegado = DBContas.getInstance(context).database
            dbLegado?.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.use { it.moveToFirst() }

            // 2. Banco Room
            val roomDb = AppDatabase.getDatabase(context)
            roomDb.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { it.moveToFirst() }
            Log.d(TAG, "Database checkpoint completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing database checkpoint", e)
        }
    }

    fun copiaBD(context: Context, backupTreeUri: Uri) {
        try {
            val backupDir = DocumentFile.fromTreeUri(context, backupTreeUri) ?: return
            
            val dbsToBackup = listOf("minhas_contas", "minhas_contas_room_db")
            
            for (dbName in dbsToBackup) {
                val dbFiles = listOf(dbName, "$dbName-wal", "$dbName-shm")
                for (fileName in dbFiles) {
                    val currentFile = context.getDatabasePath(fileName)
                    if (currentFile.exists()) {
                        val backupFile = backupDir.findFile(fileName) ?: backupDir.createFile("application/octet-stream", fileName)
                        backupFile?.let {
                            FileInputStream(currentFile).use { fis ->
                                context.contentResolver.openOutputStream(it.uri)?.use { fos ->
                                    fis.copyTo(fos)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro backup BD", e)
        }
    }

    fun restauraBD(context: Context, restoreTreeUri: Uri) {
        try {
            val restoreDir = DocumentFile.fromTreeUri(context, restoreTreeUri) ?: return
            
            val dbsToRestore = listOf("minhas_contas", "minhas_contas_room_db")
            
            for (dbName in dbsToRestore) {
                // Remove todos os arquivos do banco de dados existente
                val dbPath = context.getDatabasePath(dbName).path
                File(dbPath).delete()
                File("$dbPath-wal").delete()
                File("$dbPath-shm").delete()
                
                // Restaura apenas os arquivos que existirem no backup
                val dbFiles = listOf(dbName, "$dbName-wal", "$dbName-shm")
                for (fileName in dbFiles) {
                    val backupFile = restoreDir.findFile(fileName)
                    if (backupFile != null) {
                        val targetFile = context.getDatabasePath(fileName)
                        context.contentResolver.openInputStream(backupFile.uri)?.use { fis ->
                            FileOutputStream(targetFile).use { fos ->
                                fis.copyTo(fos)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro restaura BD", e)
        }
    }

    fun copiaSharedPreferences(context: Context, backupTreeUri: Uri) {
        try {
            val backupDir = DocumentFile.fromTreeUri(context, backupTreeUri) ?: return
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles { _, name -> name.endsWith(".xml") }?.forEach { prefFile ->
                    val backupFile = backupDir.findFile(prefFile.name) ?: backupDir.createFile("application/xml", prefFile.name)
                    if (backupFile != null) {
                        FileInputStream(prefFile).use { fis ->
                            context.contentResolver.openOutputStream(backupFile.uri)?.use { fos ->
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

    fun restauraSharedPreferences(context: Context, restoreTreeUri: Uri) {
        try {
            val restoreDir = DocumentFile.fromTreeUri(context, restoreTreeUri) ?: return
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (!sharedPrefsDir.exists()) sharedPrefsDir.mkdirs()
            restoreDir.listFiles().filter { it.isFile && it.name?.endsWith(".xml") == true }.forEach { backupFile ->
                val prefFile = File(sharedPrefsDir, backupFile.name ?: "")
                context.contentResolver.openInputStream(backupFile.uri)?.use { fis ->
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
