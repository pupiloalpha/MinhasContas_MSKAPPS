package com.msk.minhascontas.features.planos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.msk.minhascontas.R
import com.msk.minhascontas.ui.PersonalizarCategoriasScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme

/**
 * Activity para personalizar os nomes das classes e categorias.
 */
class PersonalizarCategorias : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MinhasContasTheme {
                PersonalizarCategoriasScreen(
                    onBack = { finish() },
                    onSaved = {
                        Toast.makeText(this, R.string.ajustes_salvos, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}
