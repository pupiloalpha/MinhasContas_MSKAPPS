package com.msk.minhascontas.info;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.msk.minhascontas.R;

public class Sobre extends AppCompatActivity {

    private TextView sobre;
    private PackageInfo pinfo;
    private String versao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_sobre);

        sobre = (TextView) findViewById(R.id.tvSobre);

        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        versao = pinfo.versionName;

        sobre.setText(getResources().getString(R.string.sobre, versao));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_OK, null);
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


}
