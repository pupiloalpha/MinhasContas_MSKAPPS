package com.msk.minhascontas.features.planos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.msk.minhascontas.ui.MetasScreen
import com.msk.minhascontas.ui.theme.MinhasContasTheme

/**
 * Fragmento que exibe o acompanhamento das metas de gastos por categoria para um mês específico.
 */
class MetasFragment : Fragment() {

    private var mes: Int = 1
    private var ano: Int = 2024
    private var dia: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mes = it.getInt(ARG_MES)
            ano = it.getInt(ARG_ANO)
            dia = it.getInt(ARG_DIA, -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MinhasContasTheme {
                    MetasScreen(mes = mes, ano = ano, dia = dia)
                }
            }
        }
    }

    companion object {
        private const val ARG_MES = "mes"
        private const val ARG_ANO = "ano"
        private const val ARG_DIA = "dia"

        /**
         * Cria uma nova instância do fragmento para o mês e ano fornecidos.
         */
        @JvmStatic
        fun newInstance(mes: Int, ano: Int, dia: Int = -1) =
            MetasFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MES, mes)
                    putInt(ARG_ANO, ano)
                    putInt(ARG_DIA, dia)
                }
            }
    }
}
