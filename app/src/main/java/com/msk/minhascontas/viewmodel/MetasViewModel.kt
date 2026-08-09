package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContaFilter
import com.msk.minhascontas.db.ProgressoCategoria
import com.msk.minhascontas.utils.LabelUtils
import kotlinx.coroutines.flow.map

class MetasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContasRepository.getInstance(application)

    private val categoriaCoresRes = listOf(
        R.color.cat_alimentacao_container,
        R.color.cat_educacao_container,
        R.color.cat_lazer_container,
        R.color.cat_moradia_container,
        R.color.cat_saude_container,
        R.color.cat_transporte_container,
        R.color.cat_vestuario_container,
        R.color.cat_outros_container,
        R.color.cat_invest_dividas_container
    )

    private val categoriaOnCoresRes = listOf(
        R.color.cat_alimentacao_on_container,
        R.color.cat_educacao_on_container,
        R.color.cat_lazer_on_container,
        R.color.cat_moradia_on_container,
        R.color.cat_saude_on_container,
        R.color.cat_transporte_on_container,
        R.color.cat_vestuario_on_container,
        R.color.cat_outros_on_container,
        R.color.cat_invest_dividas_on_container
    )

    fun getProgressoMensal(mes: Int, ano: Int): LiveData<List<ProgressoCategoria>> = liveData(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Busca os dados de despesas idênticos aos usados no Resumo (usando o método do repositório)
        val gastosPorCategoria = repository.getGastosPorCategoria(mes, ano)
        
        // 2. Busca investimentos acumulados
        val aplicacoes = repository.getAplicacoesPorClasse(mes, ano)
        val totalInvestimentos = aplicacoes.values.sum()

        val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
        val receitaReferencia = prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()

            val context = getApplication<Application>()
            val progressos = (0..8).map { i ->
            val percPlanejado = prefs.getFloat("plan_perc_$i", -1.0f).toDouble()
            val defaultPerc = when(i) {
                0 -> 15.0; 1 -> 10.0; 2 -> 10.0; 3 -> 25.0; 4 -> 5.0
                5 -> 5.0; 6 -> 5.0; 7 -> 5.0; 8 -> 20.0; else -> 0.0
            }
            val percFinal = if (percPlanejado >= 0) percPlanejado else defaultPerc
            
            // O valor real para metas agora vem diretamente da mesma fonte de verdade que o Resumo
            val valorReal = if (i == 8) totalInvestimentos else (gastosPorCategoria[i] ?: 0.0)

            ProgressoCategoria(
                index = i,
                nome = LabelUtils.getCategoriaLabel(context, i),
                valorPlanejado = (percFinal / 100.0) * receitaReferencia,
                valorReal = valorReal,
                corRes = categoriaCoresRes.getOrElse(i) { R.color.cinza },
                onCorRes = categoriaOnCoresRes.getOrElse(i) { R.color.on_surface }
            )
        }
        emit(progressos)
    }
}