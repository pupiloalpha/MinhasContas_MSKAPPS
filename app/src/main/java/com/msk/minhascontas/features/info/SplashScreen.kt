package com.msk.minhascontas.features.info

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.msk.minhascontas.MinhasContas

/**
 * Created by msk on 30/05/16.
 */
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MinhasContas::class.java)
        startActivity(intent)
        finish()
    }
}