package com.msk.minhascontas.info;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
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


    @Override
    public void onCreate() {
        // Use DbBackupHelper to correctly specify the database file path.
        // The FileBackupHelper now correctly targets the database file itself.
        DbBackupHelper dbs = new DbBackupHelper(this, DB_NAME);
        addHelper(FILES_BACKUP_KEY, dbs);
    }

    // Removed the getFilesDir() override. FileBackupHelper, when provided
    // with an absolute path via DbBackupHelper, does not need this override.
    // Overriding it here could cause incorrect behavior for generic FileBackupHelper
    // instances if they were to assume files are in this directory.

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        synchronized (DB_NAME) { // Synchronize on the DB_NAME constant
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Log.d("ConnectBot.BackupAgent", "onRestore called");

        synchronized (DB_NAME) { // Synchronize on the DB_NAME constant
            Log.d("ConnectBot.BackupAgent", "onRestore in-lock");

            super.onRestore(data, appVersionCode, newState);
        }
    }

    public class DbBackupHelper extends FileBackupHelper {
        public DbBackupHelper(Context ctx, String dbName) {
            // Correctly provide the absolute path to the database file.
            super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
        }
    }
}