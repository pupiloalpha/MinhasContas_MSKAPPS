package com.msk.minhascontas.db

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositório para gerenciar Metas Financeiras usando SharedPreferences (JSON).
 * Evita problemas de migração no Room.
 */
class MetasRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("metas_coach_prefs", Context.MODE_PRIVATE)
    private val _metas = MutableLiveData<List<MetaFinanceira>>()
    val metas: LiveData<List<MetaFinanceira>> = _metas

    init {
        loadGoals()
    }

    fun getAllGoals(): List<MetaFinanceira> {
        val json = prefs.getString("lista_metas", "[]") ?: "[]"
        val array = JSONArray(json)
        val lista = mutableListOf<MetaFinanceira>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(parseJson(obj))
        }
        return lista
    }

    private fun loadGoals() {
        val lista = getAllGoals()
        _metas.postValue(lista)
    }

    fun saveGoal(meta: MetaFinanceira) {
        val listaAtual = _metas.value?.toMutableList() ?: mutableListOf()
        // Remove se já existir (update) ou adiciona
        listaAtual.removeAll { it.id == meta.id }
        listaAtual.add(meta)
        persist(listaAtual)
    }

    fun deleteGoal(id: String) {
        val listaAtual = _metas.value?.toMutableList() ?: return
        listaAtual.removeAll { it.id == id }
        persist(listaAtual)
    }

    private fun persist(lista: List<MetaFinanceira>) {
        val array = JSONArray()
        lista.forEach { array.put(toJson(it)) }
        prefs.edit().putString("lista_metas", array.toString()).apply()
        _metas.postValue(lista)
    }

    private fun toJson(meta: MetaFinanceira): JSONObject {
        return JSONObject().apply {
            put("id", meta.id)
            put("nome", meta.nome)
            put("tipoMeta", meta.tipoMeta)
            put("valorObjetivo", meta.valorObjetivo)
            put("valorAtual", meta.valorAtual)
            put("taxaJurosMensal", meta.taxaJurosMensal)
            put("aporteMensalAlvo", meta.aporteMensalAlvo)
            put("dataInicio", meta.dataInicio)
            put("dataPrevisaoFim", meta.dataPrevisaoFim ?: -1L)
            put("ativa", meta.ativa)
            put("codigoVinculo", meta.codigoVinculo)
            put("prioridade", meta.prioridade)
        }
    }

    private fun parseJson(obj: JSONObject): MetaFinanceira {
        val fim = obj.getLong("dataPrevisaoFim")
        return MetaFinanceira(
            id = obj.getString("id"),
            nome = obj.getString("nome"),
            tipoMeta = obj.getInt("tipoMeta"),
            valorObjetivo = obj.getDouble("valorObjetivo"),
            valorAtual = obj.getDouble("valorAtual"),
            taxaJurosMensal = obj.getDouble("taxaJurosMensal"),
            aporteMensalAlvo = obj.getDouble("aporteMensalAlvo"),
            dataInicio = obj.getLong("dataInicio"),
            dataPrevisaoFim = if (fim == -1L) null else fim,
            ativa = obj.getBoolean("ativa"),
            codigoVinculo = obj.optString("codigoVinculo", null),
            prioridade = obj.optInt("prioridade", 0)
        )
    }
}
