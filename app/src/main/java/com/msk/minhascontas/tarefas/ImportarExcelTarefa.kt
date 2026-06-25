// ImportarExcelTarefa.kt
package com.msk.minhascontas.tarefas

import android.content.Context
import android.net.Uri
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.excel.ImportarExcel
import kotlinx.coroutines.runBlocking

class ImportarExcelTarefa(private val arquivoUri: Uri?) : TarefaExecutavel {
    private var mensagemResultado: String? = null
    private var contasInseridas = 0
    private val importador = ImportarExcel()

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.importar_excel)
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.aguarde_importacao)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        if (context == null) return false
        val repository = ContasRepository.getInstance(context)

        val sucesso = try {
            Log.d(TAG, "Iniciando processo de identificação e importação...")
            
            // O método lerExcel agora realiza a identificação de colunas antes da leitura
            val contasParaImportar = runBlocking {
                importador.lerExcel(context, arquivoUri) { atual, total ->
                    onProgress?.invoke(atual, total)
                }
            }

            if (contasParaImportar == null) {
                // Falha na identificação das colunas ou erro de leitura
                mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
                false 
            } else if (contasParaImportar.isEmpty()) {
                mensagemResultado = context.getString(R.string.dica_importacao_vazia)
                true 
            } else {
                Log.d(TAG, "Colunas identificadas. Iniciando inserção de ${contasParaImportar.size} contas.")
                contasInseridas = repository.inserirContasEmMassa(contasParaImportar)

                if (contasInseridas > 0) {
                    mensagemResultado = String.format(
                        context.getString(R.string.dica_importacao_sucesso),
                        contasInseridas,
                    )
                    true
                } else {
                    mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a importação: " + e.message, e)
            mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
            false
        }
        return sucesso
    }

    override fun getMensagemResultado(context: Context?): String? {
        return mensagemResultado
    }

    override fun getQuantidadePassos(): Int {
        return 100 // Estimativa de passos
    }

    companion object {
        private const val TAG = "ImportarExcelTarefa"
    }
}