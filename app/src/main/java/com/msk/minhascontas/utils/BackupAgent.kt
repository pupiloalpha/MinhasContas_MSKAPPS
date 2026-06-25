package com.msk.minhascontas.utils

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException

/**
 * Created by msk on 31/05/16.
 */
class BackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        super.onCreate() // Always call super.onCreate()
        Log.d(TAG, "BackupAgent onCreate called.")
        // 1. For the legacy database
        val dbs = DbBackupHelper(this, DB_NAME)
        addHelper(FILES_BACKUP_KEY, dbs)
        Log.d(TAG, "DbBackupHelper added for legacy DB: $DB_NAME")

        // 2. For the Room database
        val roomDbs = DbBackupHelper(this, ROOM_DB_NAME)
        addHelper(ROOM_FILES_BACKUP_KEY, roomDbs)
        Log.d(TAG, "DbBackupHelper added for Room DB: $ROOM_DB_NAME")

        // 3. For SharedPreferences
        val defaultPrefsFileName = packageName + "_preferences"
        val prefsHelper = SharedPreferencesBackupHelper(this, defaultPrefsFileName)
        addHelper(PREFS_BACKUP_KEY, prefsHelper)
        Log.d(TAG, "SharedPreferenceBackupHelper added for: $defaultPrefsFileName")
    }

    // Removed the getFilesDir() override. FileBackupHelper, when provided
    // with an absolute path via DbBackupHelper, does not need this override.
    // Overriding it here could cause incorrect behavior for generic FileBackupHelper
    // instances if they were to assume files are in this directory.
    @Throws(IOException::class)
    override fun onBackup(
        oldState: ParcelFileDescriptor?, data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        Log.d(TAG, "onBackup called.")
        synchronized(DB_NAME) {
            super.onBackup(oldState, data, newState)
        }
        Log.d(TAG, "onBackup completed.")
    }

    @Throws(IOException::class)
    override fun onRestore(
        data: BackupDataInput?, appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        Log.d(TAG, "onRestore called. App Version Code: " + appVersionCode)
        synchronized(DB_NAME) {
            super.onRestore(data, appVersionCode, newState)
        }
        Log.d(TAG, "onRestore completed.")
    }

    inner class DbBackupHelper(ctx: Context, dbName: String?) :
        FileBackupHelper(ctx, ctx.getDatabasePath(dbName).getAbsolutePath()) {
        init {
            Log.d(
                TAG,
                "DbBackupHelper initialized for path: " + ctx.getDatabasePath(dbName)
                    .getAbsolutePath()
            )
        }
    }

    companion object {
        private const val DB_NAME = "minhas_contas"
        private const val ROOM_DB_NAME = "minhas_contas_room_db"

        // Unique keys for the helpers
        const val FILES_BACKUP_KEY: String = "my_db_backup"
        const val ROOM_FILES_BACKUP_KEY: String = "my_room_db_backup"
        const val PREFS_BACKUP_KEY: String = "my_prefs_backup"
        private const val TAG = "MinhasContasBackup"
    }
}