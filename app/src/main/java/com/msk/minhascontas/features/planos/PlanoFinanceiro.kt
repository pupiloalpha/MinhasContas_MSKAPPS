package com.msk.minhascontas.features.planos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.ui.DashboardCoachScreen
import com.msk.minhascontas.ui.SimuladorCoachScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.AjustesUtils

class PlanoFinanceiroActivity : ComponentActivity() {

    private val mLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Ajustes.RESULT_RESTART_REQUIRED) {
            AjustesUtils.pendingRestartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MinhasContasTheme {
                PlanoFinanceiroScreen(
                    onVoltar = { finish() },
                    onSearch = {
                        val intent = Intent("com.msk.minhascontas.BUSCACONTA")
                        mLauncher.launch(intent)
                    },
                    onAjustes = {
                        val intent = Intent(this, Ajustes::class.java)
                        mLauncher.launch(intent)
                    },
                    onSobre = {
                        startActivity(Intent("com.msk.minhascontas.SOBRE"))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {
            // Se houver algo para atualizar na tela principal do Coach, faz aqui.
        }
    }
}

@Composable
fun PlanoFinanceiroScreen(
    onVoltar: () -> Unit,
    onSearch: () -> Unit,
    onAjustes: () -> Unit,
    onSobre: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var metaSelecionada by remember { mutableStateOf<com.msk.minhascontas.db.MetaFinanceira?>(null) }
    
    when (currentScreen) {
        "dashboard" -> {
            DashboardCoachScreen(
                onNavegarSimulador = {
                    metaSelecionada = null
                    currentScreen = "simulador"
                },
                onMetaClick = { meta ->
                    metaSelecionada = meta
                    currentScreen = "simulador"
                },
                onVoltar = onVoltar,
                onSearch = onSearch,
                onAjustes = onAjustes,
                onSobre = onSobre
            )
        }
        "simulador" -> {
            SimuladorCoachScreen(
                metaParaEditar = metaSelecionada,
                onMetaCriada = {
                    currentScreen = "dashboard"
                },
                onVoltar = {
                    currentScreen = "dashboard"
                }
            )
        }
    }
}
