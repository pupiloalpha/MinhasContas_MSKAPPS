package com.msk.minhascontas.db

/*
BackupHelper.kt

O que é
- Utilitário Kotlin para exportar e importar o arquivo SQLite do app usando
  o Storage Access Framework (SAF). Projetado para ser compatível com Scoped Storage
  em Android 10+ sem necessidade de permissões de armazenamento legadas.

Principais funções
- getDatabaseFile(context): File
  - Retorna o File que aponta para o banco do app (context.getDatabasePath(DB_NAME)).
  - Observação: DB_NAME aqui é "minhas_contas" — tenha certeza que corresponde
    ao nome que a aplicação efetivamente usa (ajuste se o arquivo tiver extensão).
- createExportIntent(suggestedFileName): Intent
  - Cria um Intent ACTION_CREATE_DOCUMENT para que o usuário selecione local/arquivo de destino.
- createImportIntent(): Intent
  - Cria um Intent ACTION_OPEN_DOCUMENT para o usuário escolher um backup a ser importado.
- exportToUri(context, destUri): Boolean
  - Copia os bytes do arquivo de DB para o Uri (destino selecionado pelo usuário).
  - Retorna true em sucesso. Lança IOException em falhas I/O.
- importFromUri(context, srcUri): Boolean
  - Copia os bytes de srcUri (arquivo selecionado) para um arquivo temporário na cache
    e então substitui (rename/copy fallback) o arquivo do DB do app. Retorna true em sucesso.
  - ATENÇÃO: quem chama deve fechar qualquer conexão aberta ao DB (Room/SQLiteOpenHelper)
    antes de invocar importFromUri, para evitar conflitos de lock.
- createFileInTree(context, parentTreeUri, filename): Uri?
  - Cria um arquivo dentro de uma árvore aberta pelo usuário (ACTION_OPEN_DOCUMENT_TREE).

Como usar (Fluxo recomendado)
1. Exportação:
   - Chamar createExportIntent("minhas_contas_backup.db") e lançar com Activity Result API.
   - No onActivityResult/ActivityResult callback, obter uri e:
       AppDatabase.getInstance(context).close() // garantir DB fechado
       BackupHelper.exportToUri(context, uri)
       // reopen DB se necessário

2. Importação:
   - Chamar createImportIntent() e obter uri selecionado no callback.
   - Fechar DB (AppDatabase.getInstance(context).close()), então:
       BackupHelper.importFromUri(context, uri)
   - Reabrir DB (AppDatabase.getInstance(context)).

Boas práticas e observações
- Não tente abrir/escrever diretamente em /sdcard ou usar Environment.getExternalStorageDirectory()
  em dispositivos modernos. Use SAF como implementado aqui.
- Para permitir acesso persistente a um URI retornado (por exemplo, salvar backups recorrentes):
    context.contentResolver.takePersistableUriPermission(uri, flags)
- Para garantir reabertura correta do Room após importação, adicione no AppDatabase um método
  para fechar e resetar o singleton (por exemplo closeAndReset()).
- Teste o fluxo em dispositivos com Android 6, 10, 11+ para validar diferenças de comportamento.
- Importante: sempre mantenha um backup antes de substituir o banco em produção.
*/

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper utilities to export and import the app SQLite database using the
 * Storage Access Framework (Scoped Storage) so it works on modern Android
 * (Android 10+ with scoped storage) as well as older devices when using
 * the system file picker.
 *
 * Important:
 * - Before calling importFromUri(...) you MUST close any open database
 *   connections (Room/AppDatabase) to avoid file locking issues. The helper
 *   does not attempt to close or manage DB instances.
 * - The helper copies bytes between the database file (context.getDatabasePath(DB_NAME))
 *   and the URI provided by the SAF. Use createExportIntent(...) and
 *   createImportIntent() to obtain URIs from the system picker.
 */
object BackupHelper {
    private const val TAG = "BackupHelper"
    private const val DB_NAME = "minhas_contas"

    /**
     * Returns the File object that points to the database used by Room/SQLiteOpenHelper.
     * Note: the actual filename on disk may include an extension; this uses the
     * same name the app used when creating/opening the database.
     */
    fun getDatabaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

    /**
     * Builds an Intent to ask the user to choose a destination file for export.
     * Use startActivityForResult or the Activity Result API and then pass the
     * returned uri to exportToUri(...).
     *
     * suggestedFileName is optional (e.g. "minhas_contas_backup.db").
     */
    fun createExportIntent(suggestedFileName: String? = null): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, suggestedFileName ?: "minhas_contas_backup.db")
        }
        return intent
    }

    /**
     * Builds an Intent to ask the user to choose a backup file to import.
     */
    fun createImportIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        return intent
    }

    /**
     * Exports the current database file to the destination Uri (obtained via SAF).
     * The destination Uri should be writable (returned from ACTION_CREATE_DOCUMENT).
     *
     * Returns true on success.
     */
    @Throws(IOException::class)
    fun exportToUri(context: Context, destUri: Uri): Boolean {
        val dbFile = getDatabaseFile(context)
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file does not exist: ${dbFile.absolutePath}")
            return false
        }

        val resolver: ContentResolver = context.contentResolver
        resolver.openOutputStream(destUri)?.use { outStream ->
            FileInputStream(dbFile).use { input ->
                input.copyTo(outStream)
                outStream.flush()
            }
        } ?: run {
            Log.w(TAG, "Unable to open output stream for uri: $destUri")
            return false
        }

        Log.i(TAG, "Database exported to: $destUri")
        return true
    }

    /**
     * Imports database bytes from the provided source Uri into the application's
     * database file. IMPORTANT: caller must close any open DB connection before
     * invoking this method.
     *
     * The method first copies the content to a temporary file in the app cache
     * and then atomically replaces the DB file (best-effort). This reduces the
     * chance of leaving a partially written DB file.
     */
    @Throws(IOException::class)
    fun importFromUri(context: Context, srcUri: Uri): Boolean {
        val resolver: ContentResolver = context.contentResolver
        val dbFile = getDatabaseFile(context)

        // Copy to temp file first
        val temp = File(context.cacheDir, "${DB_NAME}.import.tmp")
        resolver.openInputStream(srcUri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        } ?: run {
            Log.w(TAG, "Unable to open input stream for uri: $srcUri")
            return false
        }

        // Ensure parent dirs exist
        val parent = dbFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        // Replace the target DB file. Caller SHOULD have closed DB connections.
        try {
            // Remove existing DB file (best-effort)
            if (dbFile.exists()) {
                if (!dbFile.delete()) {
                    Log.w(TAG, "Could not delete existing DB file at ${dbFile.absolutePath}")
                }
            }

            // Move temp to final location
            if (!temp.renameTo(dbFile)) {
                // fallback: copy bytes
                FileInputStream(temp).use { input ->
                    FileOutputStream(dbFile).use { out ->
                        input.copyTo(out)
                        out.fd.sync()
                    }
                }
                if (!temp.delete()) {
                    Log.w(TAG, "Could not delete temp file: ${temp.absolutePath}")
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to replace DB file: ${ex.message}")
            // Clean up temp
            if (temp.exists()) temp.delete()
            throw IOException("Failed to import DB: ${ex.message}", ex)
        }

        Log.i(TAG, "Database imported from: $srcUri -> ${dbFile.absolutePath}")
        return true
    }

    /**
     * Utility to create a Document URI that points to a file inside a chosen
     * tree (e.g. when user granted access to a directory). This is optional
     * and can help when you want to create a backup file inside a picked folder.
     *
     * parentTreeUri must be a Uri returned by ACTION_OPEN_DOCUMENT_TREE.
     * filename is the suggested child name.
     */
    fun createFileInTree(context: Context, parentTreeUri: Uri, filename: String): Uri? {
        try {
            // Create a new document under the selected tree
            val mime = "application/octet-stream"
            val created = DocumentsContract.createDocument(context.contentResolver, parentTreeUri, mime, filename)
            return created
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to create file in tree: ${ex.message}")
            return null
        }
    }
}
