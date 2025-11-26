package com.msk.minhascontas.info;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by msk on 31/05/16.
 */
public class BackupAgent extends BackupAgentHelper {

    private static final String DB_NAME = "minhas_contas";
    // A unique key for the FileBackupHelper.
    static final String FILES_BACKUP_KEY = "my_db_backup";
    static final String PREFS_BACKUP_KEY = "my_prefs_backup"; // New key for shared preferences
    private static final String TAG = "MinhasContasBackup"; // Define a TAG


    @Override
    public void onCreate() {
        super.onCreate(); // Always call super.onCreate()
        Log.d(TAG, "BackupAgent onCreate called.");
        // 1. For the database
        DbBackupHelper dbs = new DbBackupHelper(this, DB_NAME);
        addHelper(FILES_BACKUP_KEY, dbs);
        Log.d(TAG, "DbBackupHelper added for DB: " + DB_NAME);

        // 2. For SharedPreferences (assuming you use the default shared preferences file)
        // The default shared preferences file name is usually "<your_package_name>_preferences.xml"
        String defaultPrefsFileName = getPackageName() + "_preferences";
        SharedPreferencesBackupHelper prefsHelper = new SharedPreferencesBackupHelper(this, defaultPrefsFileName);
        addHelper(PREFS_BACKUP_KEY, prefsHelper);
        Log.d(TAG, "SharedPreferenceBackupHelper added for default preferences: " + defaultPrefsFileName);
    }

    // Removed the getFilesDir() override. FileBackupHelper, when provided
    // with an absolute path via DbBackupHelper, does not need this override.
    // Overriding it here could cause incorrect behavior for generic FileBackupHelper
    // instances if they were to assume files are in this directory.

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        Log.d(TAG, "onBackup called.");
        synchronized (DB_NAME) {
            super.onBackup(oldState, data, newState);
        }
        Log.d(TAG, "onBackup completed.");
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Log.d(TAG, "onRestore called. App Version Code: " + appVersionCode);
        synchronized (DB_NAME) {
            super.onRestore(data, appVersionCode, newState);
        }
        Log.d(TAG, "onRestore completed.");
    }

    public class DbBackupHelper extends FileBackupHelper {
        public DbBackupHelper(Context ctx, String dbName) {
            super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
            Log.d(TAG, "DbBackupHelper initialized for path: " + ctx.getDatabasePath(dbName).getAbsolutePath());
        }
    }
}