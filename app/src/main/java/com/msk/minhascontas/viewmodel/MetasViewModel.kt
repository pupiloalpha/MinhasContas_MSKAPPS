package com.msk.minhascontas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.DBContas.ContaFilter
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

    /**
     * Carrega o progresso das metas de forma REATIVA observando o Room.
     */
    fun getProgressoMensal(mes: Int, ano: Int, dia: Int = -1): LiveData<List<ProgressoCategoria>> {
        val filter = ContaFilter().setMes(mes).setAno(ano)
        if (dia > 0) filter.setDiaFim(dia)
        
        return repository.getContasFlow(filter, null).map { listaContas ->
            processarListaParaProgressos(listaContas)
        }.asLiveData()
    }

    private fun processarListaParaProgressos(contas: List<Conta>): List<ProgressoCategoria> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
        val context = getApplication<Application>()
        val receitaReferencia = prefs.getFloat("plan_receita_referencia", 3000.0f).toDouble()
        val numCategorias = context.resources.getStringArray(R.array.CategoriaConta).size
        
        // Agrupa gastos por categoria em memória
        val gastosReais = contas.groupBy { 
            if (it.tipo == ContasContract.TIPO_APLICACAO) 8 else it.categoria 
        }.mapValues { it.value.sumOf { c -> c.valor } }
        
        val progressos = mutableListOf<ProgressoCategoria>()
        for (i in 0 until numCategorias) {
            val percPlanejado = prefs.getFloat("plan_perc_$i", -1.0f).toDouble()
            val defaultPerc = when(i) {
                0 -> 15.0; 1 -> 10.0; 2 -> 10.0; 3 -> 25.0; 4 -> 5.0
                5 -> 5.0; 6 -> 5.0; 7 -> 5.0; 8 -> 20.0; else -> 0.0
            }
            
            val percFinal = if (percPlanejado >= 0) percPlanejado else defaultPerc
            val valorPlanejado = (percFinal / 100.0) * receitaReferencia
            val valorReal = gastosReais[i] ?: 0.0
            
            progressos.add(
                ProgressoCategoria(
                    index = i,
                    nome = LabelUtils.getCategoriaLabel(context, i),
                    valorPlanejado = valorPlanejado,
                    valorReal = valorReal,
                    corRes = categoriaCoresRes.getOrElse(i) { R.color.cinza },
                    onCorRes = categoriaOnCoresRes.getOrElse(i) { R.color.on_surface }
                )
            )
        }
        
        return progressos
    }
}
