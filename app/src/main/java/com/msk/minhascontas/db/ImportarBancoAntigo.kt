package com.msk.minhascontas.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Classe responsável por abrir um arquivo de banco de dados (.db) externo,
 * ler os registros da tabela de contas e convertê-los em uma lista de objetos [Conta].
 * Suporta o mapeamento de colunas de versões antigas do aplicativo.
 */
class ImportarBancoAntigo {

    /**
     * Interface para acompanhamento do progresso da importação.
     */
    interface ProgressListener {
        fun onProgress(atual: Int, total: Int)
    }

    /**
     * Importa as contas de um banco de dados externo indicado pela [uri].
     *
     * @param context Contexto da aplicação.
     * @param uri URI do arquivo .db selecionado.
     * @param listener Callback opcional para progresso.
     * @return Uma lista de [Conta] ou null em caso de erro crítico.
     */
    suspend fun importar(context: Context, uri: Uri?, listener: ProgressListener?): List<Conta>? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null

        var tempFile: File? = null
        var db: SQLiteDatabase? = null
        val listaContas = mutableListOf<Conta>()

        try {
            // 1. Cria um arquivo temporário para trabalhar com o banco de dados
            tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.db")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 2. Abre o banco de dados em modo leitura
            db = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            // 3. Identifica o nome da tabela (pode ser 'contas' ou 'contasListadas')
            val tableName = if (tableExists(db!!, "contasListadas")) "contasListadas" else "contas"
            
            if (!tableExists(db, tableName)) {
                Log.e(TAG, "Tabela de contas não encontrada no arquivo.")
                return@withContext null
            }

            // 4. Executa a query para buscar todos os registros
            db.query(tableName, null, null, null, null, null, null).use { cursor ->
                val total = cursor.count
                var atual = 0

                while (cursor.moveToNext()) {
                    val conta = mapCursorToConta(cursor)
                    if (conta != null) {
                        listaContas.add(conta)
                    }
                    
                    atual++
                    listener?.onProgress(atual, total)
                }
            }

            Log.d(TAG, "Importação concluída. ${listaContas.size} registros lidos.")
            return@withContext listaContas

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar banco de dados: ${e.message}", e)
            return@withContext null
        } finally {
            db?.close()
            tempFile?.delete()
        }
    }

    /**
     * Mapeia os dados do cursor para um objeto [Conta], tratando nomes de colunas antigos.
     */
    private fun mapCursorToConta(cursor: Cursor): Conta? {
        return try {
            val conta = Conta()

            // Mapeamento flexível de colunas (Novo vs Antigo)
            conta.nome = getStringSafe(cursor, "nome_conta", "nome") ?: ""
            conta.tipo = getIntSafe(cursor, "tipo_conta", "tipo")
            conta.classeConta = getIntSafe(cursor, "classe_conta", "classe")
            conta.categoria = getIntSafe(cursor, "categoria_conta", "categoria")
            conta.dia = getIntSafe(cursor, "dia_data", "dia")
            conta.mes = getIntSafe(cursor, "mes_data", "mes")
            conta.ano = getIntSafe(cursor, "ano_data", "ano")
            conta.valor = getDoubleSafe(cursor, "valor_conta", "valor")
            conta.pagamento = getStringSafe(cursor, "pagou_conta", "pagamento") ?: "falta"
            conta.qtRepete = getIntSafe(cursor, "qt_repeticoes", "qt_repete")
            conta.nRepete = getIntSafe(cursor, "nr_repeticao", "n_repete")
            conta.intervalo = getIntSafe(cursor, "intervalo_conta", "intervalo")
            conta.codigo = getStringSafe(cursor, "codigo", "codigo_conta") ?: ""
            conta.valorJuros = getDoubleSafe(cursor, "valor_juros", "")

            // Correção de compatibilidade: Se o mês estiver em formato 0-based (comum em versões antigas), ajusta para 1-based
            if (conta.mes == 0) {
                conta.mes = 1
            } else if (conta.mes < 0) {
                conta.mes = 1
            }

            conta
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao mapear registro de conta: ${e.message}")
            null
        }
    }

    // --- MÉTODOS AUXILIARES PARA LEITURA SEGURA DO CURSOR ---

    private fun getStringSafe(cursor: Cursor, primary: String, secondary: String): String? {
        var idx = cursor.getColumnIndex(primary)
        if (idx == -1 && secondary.isNotEmpty()) idx = cursor.getColumnIndex(secondary)
        return if (idx != -1) cursor.getString(idx) else null
    }

    private fun getIntSafe(cursor: Cursor, primary: String, secondary: String): Int {
        var idx = cursor.getColumnIndex(primary)
        if (idx == -1 && secondary.isNotEmpty()) idx = cursor.getColumnIndex(secondary)
        return if (idx != -1) cursor.getInt(idx) else 0
    }

    private fun getDoubleSafe(cursor: Cursor, primary: String, secondary: String): Double {
        var idx = cursor.getColumnIndex(primary)
        if (idx == -1 && secondary.isNotEmpty()) idx = cursor.getColumnIndex(secondary)
        return if (idx != -1) cursor.getDouble(idx) else 0.0
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = ?", arrayOf(tableName)).use { cursor ->
            return cursor.count > 0
        }
    }

    companion object {
        private const val TAG = "ImportarBancoAntigo"
    }
}
