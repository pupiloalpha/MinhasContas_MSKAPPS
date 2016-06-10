package com.msk.minhascontas.info;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.msk.minhascontas.R;

public class BarraProgresso extends AsyncTask<Void, Integer, Void> {

    private ProgressDialog progressDialog = null;

    private String title = null, message = null;

    private Context context = null;

    private int quantidade = 0;
    private int tempoEspera = 0;

    public BarraProgresso(Context context, String title, String message,
                          int qt, int tempo) {
        this.context = context;
        this.title = title;
        this.message = message;
        this.quantidade = qt;
        this.tempoEspera = tempo;

    }

    @Override
    protected void onPreExecute() {
        this.progressDialog = new ProgressDialog(new ContextThemeWrapper(
                this.context, R.style.TemaDialogo));

        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setTitle(this.title);
        this.progressDialog.setMessage(this.message);
        this.progressDialog.setMax(this.quantidade);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            for (int progress = 1; progress <= quantidade; progress++) {
                if (tempoEspera == 0)
                    Thread.sleep(100);
                else
                    Thread.sleep(tempoEspera);

                publishProgress(progress);
            }

        } catch (Exception e) {
            Toast.makeText(this.context, "Exception: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        try {
            this.progressDialog.dismiss();
            this.progressDialog = null;
        } catch (Exception e) {
            // nothing
        }
    }

    @Override
    protected void onProgressUpdate(Integer... integers) {
        this.progressDialog.setProgress(integers[0]);
    }

}
