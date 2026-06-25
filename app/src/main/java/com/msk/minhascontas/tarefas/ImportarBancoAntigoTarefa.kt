package com.msk.minhascontas.tarefas

import android.content.Context
import android.net.Uri
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ImportarBancoAntigo

/**
 * Tarefa para importar dados de um backup de banco de dados antigo.
 * Executa o processamento em background com barra de progresso.
 */
class ImportarBancoAntigoTarefa(private val arquivoUri: Uri?) : TarefaExecutavel {
    private var mensagemResultado: String? = null
    private var registrosImportados = 0
    private val importador = ImportarBancoAntigo()

    override fun getTitulo(context: Context?): String {
        // Reutilizamos strings existentes para evitar a necessidade de adicionar novas
        return context?.getString(R.string.importar_excel) ?: "Importar Backup Antigo"
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.aguarde_importacao)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        if (context == null) return false
        val repository = ContasRepository.getInstance(context)

        return try {
            Log.d(TAG, "Iniciando recuperação de dados de backup antigo...")
            
            // Se importar for suspend, use runBlocking. Assumindo que não é por enquanto.
            val contasParaImportar = importador.importar(context, arquivoUri, object : ImportarBancoAntigo.ProgressListener {
                override fun onProgress(atual: Int, total: Int) {
                    onProgress?.invoke(atual, total)
                }
            })

            if (contasParaImportar == null) {
                mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
                false
            } else if (contasParaImportar.isEmpty()) {
                mensagemResultado = context.getString(R.string.dica_importacao_vazia)
                true
            } else {
                Log.d(TAG, "Processamento do arquivo concluído. Inserindo ${contasParaImportar.size} contas.")
                registrosImportados = repository.inserirContasEmMassa(contasParaImportar)

                if (registrosImportados > 0) {
                    mensagemResultado = String.format(
                        context.getString(R.string.dica_importacao_sucesso),
                        registrosImportados
                    )
                    true
                } else {
                    mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a execução da tarefa de importação: ${e.message}", e)
            mensagemResultado = context.getString(R.string.dica_erro_importacao_falhou)
            false
        }
    }

    override fun getMensagemResultado(context: Context?): String? {
        return mensagemResultado
    }

    override fun getQuantidadePassos(): Int {
        return 100
    }

    companion object {
        private const val TAG = "ImportarBancoAntigoTarefa"
    }
}
