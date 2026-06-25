package com.msk.minhascontas.tarefas

import android.content.Context
import android.net.Uri
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.pdf.ImportarPDF
import kotlinx.coroutines.runBlocking

/**
 * Tarefa para importar dados de um arquivo PDF em segundo plano.
 */
class ImportarPDFTarefa(private val arquivoUri: Uri?) : TarefaExecutavel {
    private var mensagemResultado: String? = null
    private var contasInseridas = 0
    private val importador = ImportarPDF()

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.importar_pdf)
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.aguarde_importacao_pdf)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        if ((context == null) || (arquivoUri == null)) {
            mensagemResultado = context?.getString(R.string.dica_erro_importacao_pdf_falhou)
            return false
        }

        val repository = ContasRepository.getInstance(context)

        return try {
            Log.d(TAG, "Iniciando importação de PDF: $arquivoUri")
            
            // Se lerPDF for suspend, use runBlocking. Assumindo que não é por enquanto ou que runBlocking será adicionado se necessário.
            val contasParaImportar = runBlocking {
                importador.lerPDF(context, arquivoUri) { atual, total ->
                    onProgress?.invoke(atual, total)
                }
            }


            if (contasParaImportar.isEmpty()) {
                mensagemResultado = context.getString(R.string.dica_importacao_pdf_vazia)
                true
            } else {
                contasInseridas = repository.inserirContasEmMassa(contasParaImportar)

                if (contasInseridas > 0) {
                    mensagemResultado = String.format(
                        context.getString(R.string.dica_importacao_sucesso),
                        contasInseridas,
                    )
                    true
                } else {
                    mensagemResultado = context.getString(R.string.dica_erro_importacao_pdf_falhou)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na tarefa de importação de PDF: ${e.message}", e)
            mensagemResultado = context.getString(R.string.dica_erro_importacao_pdf_falhou)
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
        private const val TAG = "ImportarPDFTarefa"
    }
}
