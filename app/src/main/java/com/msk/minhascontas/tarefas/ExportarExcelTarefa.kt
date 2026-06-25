package com.msk.minhascontas.tarefas

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.msk.minhascontas.R
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.features.excel.ExportarExcel

class ExportarExcelTarefa(
    private val arquivoUri: Uri?,
    private val mesExportacao: Int,
    private val anoExportacao: Int,
) : TarefaExecutavel {
    private var mensagemResultado: String? = null
    private val excel = ExportarExcel() // Instância do serviço Excel

    override fun getTitulo(context: Context?): String? {
        return context?.getString(R.string.exportar_excel)
    }

    override fun getMensagemInicial(context: Context?): String? {
        return context?.getString(R.string.aguarde_exportacao)
    }

    override fun executarTarefa(context: Context?, onProgress: ((Int, Int) -> Unit)?): Boolean {
        // Handle nullable context
        if (context == null) {
            mensagemResultado = "Erro: Contexto nulo."
            return false
        }

        val repository = ContasRepository.getInstance(context)
        var dadosDetalheCursor: Cursor? = null
        var sucesso: Boolean

        try {
            onProgress?.invoke(10, 100)
            // 1. Coleta os dados de resumo e nomes das linhas
            val valoresResumo =
                repository.coletaDadosResumo(context, mesExportacao, anoExportacao)
            val nomesLinhas = repository.getNomeLinhas(context)

            onProgress?.invoke(40, 100)
            // 2. Coleta os dados detalhados como Cursor
            dadosDetalheCursor = repository.getListaContasCompletaCursor(mesExportacao, anoExportacao)

            onProgress?.invoke(60, 100)
            // 3. CONVERTE O CURSOR PARA LISTA DE CONTAS
            val dadosDetalheLista = repository.cursorToListaContas(dadosDetalheCursor)

            onProgress?.invoke(80, 100)
            // 3.5 Coleta metas
            val metas = repository.getMetasSincrono()

            // 4. Chama CriaExcel com a Lista (AGORA COM 6 ARGUMENTOS)
            val erro = excel.CriaExcel(
                context,
                arquivoUri,
                nomesLinhas,  // Argumento 3: Nomes das linhas (Aba RESUMO)
                valoresResumo,  // Argumento 4: Valores do resumo (Aba RESUMO)
                dadosDetalheLista, // Argumento 5: Lista de objetos Conta (Aba DADOS)
                metas // Argumento 6: Metas Financeiras
            )

            if (erro == 0) {
                mensagemResultado = context.getString(R.string.dica_exporta_excel)
                sucesso = true
                onProgress?.invoke(100, 100)
            } else {
                mensagemResultado = context.getString(R.string.dica_erro_exporta_excel)
                sucesso = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a exportação: " + e.message, e)
            mensagemResultado = context.getString(R.string.dica_erro_exporta_excel)
            sucesso = false
        } finally {
            dadosDetalheCursor?.close()
        }
        return sucesso
    }

    override fun getMensagemResultado(context: Context?): String? {
        return mensagemResultado
    }

    override fun getQuantidadePassos(): Int {
        return 100 // Valor fixo, pois o número de passos é difícil de prever
    }

    companion object {
        private const val TAG = "ExportarExcelTarefa"
    }
}