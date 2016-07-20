package com.msk.minhascontas.info;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.msk.minhascontas.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class EscolhePasta extends ListActivity {

    public static final String CHOSEN_DIRECTORY = "chosenDir";
    private static final String START_DIR = "startDir";
    private static final String SHOW_HIDDEN = "showHidden";
    private static final int PICK_DIRECTORY = 4352;
    private File dir;
    private String[] pastas;
    private String tipo;
    private boolean showHidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        tipo = extras.getString("tipo");

        String strSDCardPath = System.getenv("SECONDARY_STORAGE");
        if ((strSDCardPath == null) || strSDCardPath.length() == 0) {
            strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
        }
        if (strSDCardPath != null) {
            if (strSDCardPath.contains(":")) {
                strSDCardPath = strSDCardPath.substring(0, strSDCardPath.indexOf(":"));
            }

            File externalFilePath = new File(strSDCardPath);
            if (externalFilePath.exists() && externalFilePath.canWrite()) {
                dir = externalFilePath.getParentFile();
            } else {

                dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getParentFile().getParentFile().getParentFile();
            }

        } else {

            dir = Environment.getExternalStorageDirectory();
        }

        String preferredStartDir = extras.getString(START_DIR);
        showHidden = extras.getBoolean(SHOW_HIDDEN, false);
        if (preferredStartDir != null) {
            File startDir = new File(preferredStartDir);
            if (startDir.isDirectory()) {
                dir = startDir;
            }
        }

        setContentView(R.layout.lista_pastas);
        setTheme(R.style.TemaNovo);
        setTitle(dir.getAbsolutePath());

        TextView sem = (TextView) findViewById(R.id.tvSemResultados);

        Button btnChoose = (Button) findViewById(R.id.btnChoose);
        if (!tipo.equals("")) {
            btnChoose.setVisibility(View.GONE);
        } else {
            String name = dir.getName();
            if (name.length() == 0)
                name = "/";
            btnChoose.setText(" Salvar em '" + name + "'");
            btnChoose.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    returnDir(dir.getAbsolutePath());
                }
            });
        }

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        if (!dir.canRead()) {
            Toast.makeText(getApplicationContext(), "Acesso negado",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final ArrayList<File> files = filter(dir.listFiles(), showHidden);

        pastas = names(files);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {

            public View getView(int position, View convertView, ViewGroup parent) {

                LayoutInflater inflater = getLayoutInflater();
                View rowView = inflater.inflate(R.layout.linha_pastas, null);

                TextView tv = (TextView) rowView.findViewById(R.id.tvPasta);
                TextView data = (TextView) rowView.findViewById(R.id.tvData);
                AppCompatImageView iv = (AppCompatImageView) rowView.findViewById(R.id.ivFolder);

                String str = pastas[position];
                tv.setText(str);

                if (!files.get(position).isDirectory()) {
                    iv.setImageResource(R.drawable.ic_archive);
                    iv.setColorFilter(getResources().getColor(R.color.verde_escuro));

                    Date lastModified = new Date(files.get(position).lastModified());
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    String formattedDateString = formatter.format(lastModified);
                    data.setText(formattedDateString);
                } else {
                    iv.setImageResource(R.drawable.ic_folder);
                    iv.setColorFilter(getResources().getColor(R.color.cinza_claro));
                    data.setVisibility(View.GONE);

                }

                return rowView;
            }

            @Override
            public int getCount() {
                return files.size();
            }

            @Override

            public long getItemId(int posicao) {
                return posicao;
            }

        };
        lv.setAdapter(adapter);
        lv.setEmptyView(sem);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (files.get(position).isDirectory()) {
                    String path = files.get(position).getAbsolutePath();
                    Intent intent = new Intent(EscolhePasta.this, EscolhePasta.class);
                    intent.putExtra(EscolhePasta.START_DIR, path);
                    intent.putExtra(EscolhePasta.SHOW_HIDDEN, showHidden);
                    intent.putExtra("tipo", tipo);
                    startActivityForResult(intent, PICK_DIRECTORY);
                } else {
                    String path = files.get(position).getAbsolutePath();
                    if (path.endsWith(tipo)) {
                        Intent result = new Intent();
                        result.putExtra(CHOSEN_DIRECTORY, path);
                        setResult(RESULT_OK, result);
                        finish();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_DIRECTORY && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            String path = (String) extras.get(EscolhePasta.CHOSEN_DIRECTORY);
            returnDir(path);
        }
    }

    private void returnDir(String path) {

        Intent result = new Intent();
        result.putExtra(CHOSEN_DIRECTORY, path);
        setResult(RESULT_OK, result);
        finish();
    }

    private ArrayList<File> filter(File[] file_list, boolean showHidden) {
        ArrayList<File> pastas = new ArrayList<File>();
        for (File file : file_list) {
            if (!file.isDirectory())
                continue;
            if (!showHidden && file.isHidden())
                continue;
            if (!file.canRead())
                continue;
            pastas.add(file);
        }
        Collections.sort(pastas);

        ArrayList<File> xls = new ArrayList<File>();
        if (!tipo.equals("")) {
            for (File file : file_list) {
                if (file.isDirectory())
                    continue;
                if (!file.getAbsolutePath().endsWith(tipo))
                    continue;
                if (!showHidden && file.isHidden())
                    continue;
                if (!file.canRead())
                    continue;
                xls.add(file);
            }
            Collections.sort(xls);
        }

        ArrayList<File> files = new ArrayList<File>();
        for (int i = 0; i < pastas.size(); i++) {
            files.add(pastas.get(i));
        }
        for (int j = 0; j < xls.size(); j++) {
            files.add(xls.get(j));
        }
        return files;
    }

    private String[] names(ArrayList<File> files) {
        String[] names = new String[files.size()];
        int i = 0;
        for (File file : files) {
            names[i] = file.getName();
            i++;
        }
        return names;
    }
}