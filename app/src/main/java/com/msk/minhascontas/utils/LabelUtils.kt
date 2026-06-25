package com.msk.minhascontas.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.msk.minhascontas.R

/**
 * Utilitário para gerenciar os rótulos dinâmicos de classes e categorias.
 * Permite que o usuário modifique os nomes através das configurações.
 */
object LabelUtils {

    /**
     * Retorna o nome da classe (tipo de despesa/aplicação/receita) baseado no tipo e no índice.
     */
    @JvmStatic
    fun getClasseLabel(context: Context, tipo: Int, index: Int): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "label_classe_${tipo}_$index"
        val customLabel = prefs.getString(key, null)
        
        if (!customLabel.isNullOrBlank()) {
            return customLabel
        }

        val arrayResId = when (tipo) {
            0 -> R.array.TipoDespesa
            1 -> R.array.TipoReceita
            2 -> R.array.TipoAplicacao
            else -> return ""
        }
        
        val labels = context.resources.getStringArray(arrayResId)
        return if (index >= 0 && index < labels.size) labels[index] else ""
    }

    /**
     * Retorna o nome da categoria baseada no índice.
     */
    @JvmStatic
    fun getCategoriaLabel(context: Context, index: Int): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "label_categoria_$index"
        val customLabel = prefs.getString(key, null)
        
        if (!customLabel.isNullOrBlank()) {
            return customLabel
        }

        val labels = context.resources.getStringArray(R.array.CategoriaConta)
        return if (index >= 0 && index < labels.size) labels[index] else ""
    }

    /**
     * Salva um novo nome para uma classe.
     */
    @JvmStatic
    fun setClasseLabel(context: Context, tipo: Int, index: Int, label: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "label_classe_${tipo}_$index"
        val trimmedLabel = label.trim()
        
        if (trimmedLabel.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, trimmedLabel).apply()
        }
    }

    /**
     * Salva um novo nome para uma categoria.
     */
    @JvmStatic
    fun setCategoriaLabel(context: Context, index: Int, label: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = "label_categoria_$index"
        val trimmedLabel = label.trim()
        
        if (trimmedLabel.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, trimmedLabel).apply()
        }
    }

    /**
     * Reverte todos os nomes para o padrão original.
     */
    @JvmStatic
    fun revertToDefault(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        
        // Remove labels de classes (tipos 0, 1, 2)
        // Usamos um loop generoso para garantir que removemos tudo que possa ter sido salvo
        for (tipo in 0..2) {
            for (index in 0..10) {
                editor.remove("label_classe_${tipo}_$index")
            }
        }
        
        // Remove labels de categorias
        for (index in 0..15) {
            editor.remove("label_categoria_$index")
        }
        
        editor.apply()
    }
}
