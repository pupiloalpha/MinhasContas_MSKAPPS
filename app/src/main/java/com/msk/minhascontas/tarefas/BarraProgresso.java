package com.msk.minhascontas.tarefas;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import java.lang.ref.WeakReference;
import android.app.Activity;
import android.widget.Toast;

import com.msk.minhascontas.R;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.db.ContasContract;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.excel.ExportarExcel;
import com.msk.minhascontas.excel.ImportarExcel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public class BarraProgresso extends AsyncTask<Void, Integer, Boolean> {

    private ProgressDialog progressDialog;
    private final WeakReference<Context> contextRef;
    // --- NOVOS CAMPOS DE CLASSE ---
    private final TarefaExecutavel tarefa; // NOVO: O objeto de tarefa a ser executado
    private final String title;
    private final String message;
    private final int quantidade;
    private final int tempoEspera;

    public BarraProgresso(Context context, TarefaExecutavel tarefa) {
        this.contextRef = new WeakReference<>(context);
        this.tarefa = tarefa;

        // Obtém as informações de exibição DIRETAMENTE da Tarefa
        this.title = tarefa.getTitulo(context);
        this.message = tarefa.getMensagemInicial(context);
        this.quantidade = tarefa.getQuantidadePassos();

        // 'tempoEspera' pode ser fixo ou removido se não for mais usado na simulação de progresso.
        // Mantendo 10ms como um valor padrão de simulação (se ainda existir).
        this.tempoEspera = 10;
    }

    @Override
    protected void onPreExecute() {
        Context context = contextRef.get();

        // 1. Checagem de segurança (mantida do passo anterior)
        if (context == null || (context instanceof Activity && ((Activity) context).isFinishing())) {
            return; // Aborta a exibição se a Activity estiver indisponível ou finalizando
        }

        try {
            // 2. Inicialização do diálogo
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setMax(quantidade);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            // 3. Tenta mostrar o diálogo. A linha 64 é esta:
            progressDialog.show();
        } catch (Exception e) {
            // 4. Último recurso: Captura qualquer exceção (incluindo o WindowLeaked/BadTokenException)
            // que possa ocorrer devido a uma condição de corrida.
            // Isso previne o crash.
            Log.e("BarraProgresso", "Falha ao exibir ProgressDialog: " + e.getMessage());
        }

    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null) return false;

        // Lógica da simulação de progresso (se aplicável, para o publishProgress)
        // Se o progresso não for usado, pode ser uma simples iteração:
        if (quantidade > 0) {
            for (int i = 0; i < quantidade; i++) {
                // Se a TarefaExecutavel não tiver um mecanismo de callback para reportar progresso,
                // esta simulação genérica pode ser mantida para um UX mínimo.
                try {
                    Thread.sleep(tempoEspera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e("BarraProgresso", "Thread interrompida.", e);
                }
                publishProgress(i + 1);
            }
        }

        // Execução da tarefa, a única lógica de negócio
        return tarefa.executarTarefa(context);
    }

    @Override
    protected void onPostExecute(Boolean sucesso) {
        super.onPostExecute(sucesso);
        Context context = contextRef.get();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (context != null) {
            // Obtém a mensagem de resultado diretamente da Tarefa
            String mensagemFinal = tarefa.getMensagemResultado(context);

            // Exibe o resultado
            Toast.makeText(context, mensagemFinal, Toast.LENGTH_LONG).show();

            if (!sucesso) {
                Log.e("BarraProgresso", "Operação Falhou: " + mensagemFinal);
            }

            // Opcional: Se a tarefa for uma Activity (como Ajustes),
            // talvez seja necessário chamar um método de callback aqui.
        }
    }

    @Override
    protected void onProgressUpdate(Integer... integers) {
        this.progressDialog.setProgress(integers[0]);
    }

    private double getSumFromCursor(Cursor cursor, String columnName) {
        double total = 0;
        // Use try-with-resources for automatic cursor closing
        try (cursor) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    do {
                        total += cursor.getDouble(columnIndex);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e("BarraProgresso", "Error getting sum from cursor", e);
        }
        return total;
    }

}