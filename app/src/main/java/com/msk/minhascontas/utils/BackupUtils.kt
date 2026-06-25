package com.msk.minhascontas.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object BackupUtils {
    private const val TAG = "BackupManager"

    fun copiaBD(context: Context, backupTreeUri: Uri) {
        try {
            val backupDir = DocumentFile.fromTreeUri(context, backupTreeUri) ?: return
            
            // 1. Backup do Banco Legado
            val currentDB = context.getDatabasePath("minhas_contas")
            if (currentDB.exists()) {
                val backupDBFile = backupDir.findFile("minhas_contas.db") ?: backupDir.createFile("application/vnd.sqlite3", "minhas_contas.db")
                if (backupDBFile != null) {
                    FileInputStream(currentDB).use { fis ->
                        context.contentResolver.openOutputStream(backupDBFile.uri)?.use { fos ->
                            fis.copyTo(fos)
                        }
                    }
                }
            }

            // 2. Backup do Banco Room
            val roomDB = context.getDatabasePath("minhas_contas_room_db")
            if (roomDB.exists()) {
                val backupRoomFile = backupDir.findFile("minhas_contas_room.db") ?: backupDir.createFile("application/vnd.sqlite3", "minhas_contas_room.db")
                if (backupRoomFile != null) {
                    FileInputStream(roomDB).use { fis ->
                        context.contentResolver.openOutputStream(backupRoomFile.uri)?.use { fos ->
                            fis.copyTo(fos)
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
            
            // 1. Restaura Banco Legado
            val backupDBFile = restoreDir.findFile("minhas_contas.db")
            if (backupDBFile != null) {
                val currentDB = context.getDatabasePath("minhas_contas")
                context.contentResolver.openInputStream(backupDBFile.uri)?.use { fis ->
                    FileOutputStream(currentDB).use { fos ->
                        fis.copyTo(fos)
                    }
                }
            }

            // 2. Restaura Banco Room
            val backupRoomFile = restoreDir.findFile("minhas_contas_room.db")
            if (backupRoomFile != null) {
                val roomDB = context.getDatabasePath("minhas_contas_room_db")
                context.contentResolver.openInputStream(backupRoomFile.uri)?.use { fis ->
                    FileOutputStream(roomDB).use { fos ->
                        fis.copyTo(fos)
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
