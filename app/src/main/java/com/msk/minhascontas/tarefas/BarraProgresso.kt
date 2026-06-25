package com.msk.minhascontas.tarefas

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.lang.ref.WeakReference

@Suppress("deprecation", "OVERRIDE_DEPRECATION")
class BarraProgresso(context: Context?, private val tarefa: TarefaExecutavel) :
    AsyncTask<Void?, Int?, Boolean?>() {
    private var progressDialog: ProgressDialog? = null
    private val contextRef: WeakReference<Context?> = WeakReference(context)

    // --- NOVOS CAMPOS DE CLASSE ---
    private val title: String? = tarefa.getTitulo(context)
    private val message: String? = tarefa.getMensagemInicial(context)
    private val quantidade: Int = tarefa.getQuantidadePassos()

    override fun onPreExecute() {
        val context = contextRef.get()

        // 1. Checagem de segurança (mantida do passo anterior)
        if (context == null || (context is Activity && context.isFinishing)) {
            return  // Aborta a exibição se a Activity estiver indisponível ou finalizando
        }

        try {
            // 2. Inicialização do diálogo
            progressDialog = ProgressDialog(context)
            progressDialog!!.isIndeterminate = false
            progressDialog!!.setCancelable(false)
            progressDialog!!.setTitle(title)
            progressDialog!!.setMessage(message)
            progressDialog!!.max = quantidade
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.progress = 0
            // 3. Tenta mostrar o diálogo. A linha 64 é esta:
            progressDialog!!.show()
        } catch (e: Exception) {
            // 4. Último recurso: Captura qualquer exceção (incluindo o WindowLeaked/BadTokenException)
            // que possa ocorrer devido a uma condição de corrida.
            // Isso previne o crash.
            Log.e("BarraProgresso", "Falha ao exibir ProgressDialog: " + e.message)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        val context = contextRef.get() ?: return false

        // Execução da tarefa, a única lógica de negócio
        // Passamos um callback para que a tarefa possa reportar seu progresso real
        return tarefa.executarTarefa(context = context) { atual, total ->
            if (total > 0) {
                // Normaliza o progresso para o valor 'max' (quantidade) definido no diálogo
                val progressoCalculado = (atual.toLong() * quantidade / total).toInt()
                publishProgress(progressoCalculado)
            } else {
                publishProgress(atual)
            }
        }
    }

    override fun onPostExecute(sucesso: Boolean?) {
        super.onPostExecute(sucesso)
        val context = contextRef.get()

        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }

        if (context != null) {
            // Obtém a mensagem de resultado diretamente da Tarefa
            val mensagemFinal: String? = tarefa.getMensagemResultado(context)

            // Exibe o resultado
            Toast.makeText(context, mensagemFinal, Toast.LENGTH_LONG).show()

            if (sucesso == false) {
                Log.e("BarraProgresso", "Operação Falhou: $mensagemFinal")
            }

            // Opcional: Se a tarefa for uma Activity (como Ajustes),
            // talvez seja necessário chamar um método de callback aqui.
        }
    }

    override fun onProgressUpdate(vararg integers: Int?) {
        this.progressDialog!!.progress = integers[0]!!
    }

    // Remoção de getSumFromCursor (não utilizado)
}