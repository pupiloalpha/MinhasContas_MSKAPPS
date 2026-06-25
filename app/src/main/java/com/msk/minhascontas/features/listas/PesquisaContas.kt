package com.msk.minhascontas.features.listas

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.features.info.Ajustes
import com.msk.minhascontas.ui.EditarContaScreen
import com.msk.minhascontas.ui.PesquisaScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import com.msk.minhascontas.utils.AjustesUtils
import java.text.NumberFormat
import java.util.Calendar

class PesquisaContas : AppCompatActivity() {

    private var nrPagina = 0

    private val mLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Ajustes.RESULT_RESTART_REQUIRED) {
            AjustesUtils.pendingRestartReason = result.data?.getStringExtra(Ajustes.EXTRA_RESTART_REASON)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        nrPagina = extras?.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE) ?: MinhasContas.START_PAGE
        Log.d(TAG, "Posição recebida (KEY_PAGINA): $nrPagina")

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            var editingAccountId by remember { mutableStateOf<Long?>(null) }

            MinhasContasTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    PesquisaScreen(
                        onBack = {
                            RetornaPosicaoParaSincronizacao()
                            finish()
                        },
                        onEditConta = { id ->
                            if (isTablet) {
                                editingAccountId = id
                            } else {
                                val nova = Bundle()
                                nova.putLong("id", id)
                                val app = Intent("com.msk.minhascontas.EDITACONTA")
                                app.putExtras(nova)
                                startActivityForResult(app, 1)
                            }
                        },
                        onLembrete = { conta ->
                            createCalendarReminder(conta)
                        }
                    )

                    // Overlay de Edição para Tablet
                    if (isTablet && editingAccountId != null) {
                        BackHandler { editingAccountId = null }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.32f))
                                .clickable { editingAccountId = null },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.45f)
                                    .clickable(enabled = false) { },
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp
                            ) {
                                val editarViewModel: com.msk.minhascontas.viewmodel.EditarContaViewModel = viewModel()
                                LaunchedEffect(editingAccountId) { 
                                    editingAccountId?.let { editarViewModel.loadConta(it) } 
                                }
                                
                                EditarContaScreen(
                                    viewModel = editarViewModel,
                                    onComplete = { success ->
                                        editingAccountId = null
                                        // A lista na PesquisaScreen deve atualizar via Flow do DB
                                    },
                                    onCancel = { editingAccountId = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // O Compose Screen reage automaticamente a mudanças no banco de dados
        // via Flow no ViewModel, então não precisamos recarregar nada manualmente aqui
        // se a edição/exclusão for feita corretamente no repositório.
    }

    override fun onResume() {
        super.onResume()
        AjustesUtils.checkPendingUpdates(this) {
            // Atualização automática via Flow no ViewModel do Compose
        }
    }

    private fun RetornaPosicaoParaSincronizacao() {
        val returnIntent = Intent()
        returnIntent.putExtra(MinhasContas.RETURN_KEY_PAGINA, nrPagina)
        setResult(RESULT_OK, returnIntent)
        Log.d(TAG, "Retornando posição: $nrPagina")
    }

    private fun createCalendarReminder(conta: Conta) {
        val r = resources
        val c = Calendar.getInstance()
        val dia = conta.dia
        val mes = conta.mes
        val ano = conta.ano
        val valorConta = conta.valor
        val nomeContaCalendario = r.getString(R.string.dica_evento, conta.nome)

        c.set(ano, mes - 1, dia)

        val evento = Intent(Intent.ACTION_EDIT)
        evento.type = "vnd.android.cursor.item/event"
        evento.putExtra(CalendarContract.Events.TITLE, nomeContaCalendario)

        val current = r.configuration.locales[0]
        val dinheiro = NumberFormat.getCurrencyInstance(current)

        evento.putExtra(
            CalendarContract.Events.DESCRIPTION,
            r.getString(R.string.dica_calendario, dinheiro.format(valorConta))
        )

        evento.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, c.timeInMillis)
        evento.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, c.timeInMillis)

        evento.putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
        evento.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        startActivity(evento)
    }

    companion object {
        private const val TAG = "PesquisaContas"
    }
}
