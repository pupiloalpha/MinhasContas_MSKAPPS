package com.msk.minhascontas.info;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.msk.minhascontas.MinhasContas;

/**
 * Created by msk on 30/05/16.
 */
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MinhasContas.class);
        startActivity(intent);
        finish();
    }
}
