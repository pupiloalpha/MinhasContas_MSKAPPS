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

    @Override
    public void onCreate(){
        FileBackupHelper dbs = new FileBackupHelper(this, DB_NAME);
        addHelper("dbs", dbs);
    }

    @Override
    public File getFilesDir(){
        File path = getDatabasePath(DB_NAME);
        return path.getParentFile();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        synchronized (DB_NAME) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Log.d("ConnectBot.BackupAgent", "onRestore called");

        synchronized (DB_NAME) {
            Log.d("ConnectBot.BackupAgent", "onRestore in-lock");

            super.onRestore(data, appVersionCode, newState);
        }
    }

    public class DbBackupHelper extends FileBackupHelper {
        public DbBackupHelper(Context ctx, String dbName) {
            super(ctx, ctx.getDatabasePath(dbName).getAbsolutePath());
        }
    }

}
