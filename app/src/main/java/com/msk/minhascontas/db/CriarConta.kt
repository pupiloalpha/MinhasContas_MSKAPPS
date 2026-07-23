package com.msk.minhascontas.db

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.ui.CriarContaScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme

class CriarConta : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val extras = intent.extras
        val initialPagina = extras?.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE) ?: MinhasContas.START_PAGE
        val initialMes = extras?.getInt(MinhasContas.KEY_MES, -1) ?: -1
        val initialAno = extras?.getInt(MinhasContas.KEY_ANO, -1) ?: -1

        setContent {
            MinhasContasTheme {
                CriarContaScreen(
                    initialMes = initialMes,
                    initialAno = initialAno,
                    onBack = { finishActivity(RESULT_CANCELED, initialPagina) },
                    onSuccess = { finishActivity(RESULT_OK, initialPagina) }
                )
            }
        }
    }

    private fun finishActivity(resultCode: Int, pagina: Int) {
        val intent = Intent()
        intent.putExtra(MinhasContas.RETURN_KEY_PAGINA, pagina)
        setResult(resultCode, intent)
        finish()
    }
}
