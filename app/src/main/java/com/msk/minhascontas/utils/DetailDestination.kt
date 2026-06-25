package com.msk.minhascontas.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Destinos para o painel de detalhes em tablets e navegação em celulares.
 */
@Parcelize
sealed class DetailDestination : Parcelable {
    @Parcelize
    data object Dashboard : DetailDestination()
    @Parcelize
    data class Contas(val tipo: Int = -1, val filtro: Int = -1) : DetailDestination()
    @Parcelize
    data object Metas : DetailDestination()
    @Parcelize
    data object Planejamento : DetailDestination()
    @Parcelize
    data object Ajustes : DetailDestination()
    @Parcelize
    data object Sobre : DetailDestination()
    @Parcelize
    data object CriarConta : DetailDestination()
    @Parcelize
    data object BuscarConta : DetailDestination()
    @Parcelize
    data class EditarConta(val id: Long) : DetailDestination()
    @Parcelize
    data object PersonalizarCategorias : DetailDestination()
    @Parcelize
    data object DefinirMetas : DetailDestination()
}