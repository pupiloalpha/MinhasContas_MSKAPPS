package com.msk.minhascontas.db

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.msk.minhascontas.ui.EditarContaScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.viewmodel.EditarContaViewModel

class EditarConta : ComponentActivity() {
    private val viewModel: EditarContaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idConta = intent.getLongExtra("id", -1L)
        if (idConta != -1L) {
            viewModel.loadConta(idConta)
        } else {
            finish()
            return
        }

        setContent {
            MinhasContasTheme {
                EditarContaScreen(
                    viewModel = viewModel,
                    onComplete = { success ->
                        if (success) {
                            setResult(RESULT_OK)
                        }
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}
