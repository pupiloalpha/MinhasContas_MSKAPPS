package com.msk.minhascontas.features.listas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.database.SQLException
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.msk.minhascontas.MinhasContas
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasRepository
import com.msk.minhascontas.db.ContasRepository.Companion.getInstance
import com.msk.minhascontas.db.DBContas
import com.msk.minhascontas.db.DBContas.ContaFilter
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ListaMensalContas : Fragment() {
    // Use List<Long> em vez de ArrayList<Long> para consistência com interfaces
    var contas: MutableList<Long?> = ArrayList() // Armazena os IDs das contas selecionadas

    private var repository: ContasRepository? = null
    private val c: Calendar = Calendar.getInstance()
    private var res: Resources? = null
    private var buscaPreferencias: SharedPreferences? = null
    private var dinheiro: NumberFormat? = null

    // Elementos da UI
    private var semContas: TextView? = null
    private var listaContas: RecyclerView? = null

    // Variáveis de estado
    private var mes = 0
    private var ano = 0
    private var dia = 0
    private var tipo = 0
    private var filtro = 0
    private var idConta: Long =
        0 // O ID do item único atualmente selecionado (quando contas.size() == 1)
    private var ordemListaDeContas: String? = null
    private var nomeConta: String? = null // Nome do item único atualmente selecionado
    private var buscaContas: AdaptaListaMensalRC? = null

    // MUDANÇA: De Cursor para List<Conta>
    private var contasParaLista: List<Conta> = ArrayList()

    // RENOMEADO 'valorConta' para 'valorContaTotalSelecionada'
    private var valorContaTotalSelecionada = 0.0

    // endregion
    // region 3. Métodos de Ciclo de Vida (Lifecycle Methods)
    // =============================================================================================
    override fun onAttach(context: Context) {
        super.onAttach(context)
        repository = getInstance(context)
        buscaPreferencias = PreferenceManager
            .getDefaultSharedPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.contas_do_mes, container, false)
        ordemListaDeContas = buscaPreferencias!!.getString(
            "ordem",
            ContasContract.Colunas.COLUNA_NOME_CONTA + " ASC"
        )
        buscaPreferencias!!
            .registerOnSharedPreferenceChangeListener(preferencias)

        val bundle = arguments
        if (bundle != null) {
            Log.d("MINHAS_CONTAS", "ListaMensalContas.onCreateView: Obtendo argumentos do bundle.")
            ano = bundle.getInt("ano")
            mes = bundle.getInt("mes") // 'mes' agora será 1-indexado (1 para janeiro)
            dia = bundle.getInt("dia", 0)
            tipo = bundle.getInt("tipo")
            filtro = bundle.getInt("filtro")
            Log.d(
                "MINHAS_CONTAS",
                "ListaMensalContas.onCreateView: mes=$mes, ano=$ano, dia=$dia, tipo=$tipo, filtro=$filtro"
            )
        } else {
            val c = Calendar.getInstance()
            // *** MUDANÇA: Use c.get(Calendar.MONTH) + 1 para garantir que o mês padrão seja 1-indexado. ***
            mes = c[Calendar.MONTH] + 1
            ano = c[Calendar.YEAR]
            dia = c[Calendar.DAY_OF_MONTH]
            tipo = -1
            filtro = -1
            Log.d(
                "MINHAS_CONTAS",
                "ListaMensalContas.onCreateView: Bundle NULO. Usando defaults: mes=$mes, ano=$ano, dia=$dia, tipo=$tipo, filtro=$filtro"
            )
        }

        res = requireActivity().resources
        val current: Locale = res!!.configuration.locales[0]
        dinheiro = NumberFormat.getCurrencyInstance(current)

        listaContas = rootView.findViewById(R.id.lvContasCriadas)
        listaContas!!.layoutManager = LinearLayoutManager(requireContext())

        semContas = rootView.findViewById(R.id.tvSemContas)

        montaLista()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        montaLista()
    }

    // endregion
    // region 4. Métodos Públicos/Package-Private
    // =============================================================================================
    fun updateFilter(newFiltro: Int) {
        this.filtro = newFiltro
        montaLista()
    }

    fun updateDate(mes: Int, ano: Int, dia: Int) {
        this.mes = mes
        this.ano = ano
        this.dia = dia
        montaLista()
    }

    fun refreshLista() {
        montaLista()
    }

    // endregion
    // region 5. Callbacks de ActionMode
    // =============================================================================================
    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.menu_altera_lista, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
            val count = contas.size
            val selectionSingle = (count == 1)

            // Itens que só fazem sentido para uma conta (Editar, Lembrete)
            val itemEditar = menu.findItem(R.id.botao_editar)
            val itemLembrete = menu.findItem(R.id.botao_lembrete)
            if (itemEditar != null) itemEditar.isVisible = selectionSingle
            if (itemLembrete != null) itemLembrete.isVisible = selectionSingle

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.botao_editar -> {
                    if (contas.size == 1) {
                        val id = contas[0]!!
                        val act = activity
                        if (act is MinhasContas) {
                            act.onEditarConta(id)
                        } else {
                            val localBundle = Bundle()
                            localBundle.putLong("id", id)
                            val localIntent = Intent("com.msk.minhascontas.EDITACONTA")
                            localIntent.putExtras(localBundle)
                            startActivity(localIntent)
                        }
                    }
                    mode.finish()
                    true
                }
                R.id.botao_pagar -> {
                    if (contas.isNotEmpty()) {
                        for (id in contas) {
                            try {
                                val contaToUpdate = repository!!.getConta(id!!)
                                if (contaToUpdate != null) {
                                    val newStatus =
                                        if (ContasContract.STATUS_PAGO_RECEBIDO == contaToUpdate.pagamento)
                                            ContasContract.STATUS_PENDENTE
                                        else
                                            ContasContract.STATUS_PAGO_RECEBIDO
                                    repository!!.atualizarPagamento(id, newStatus)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "ListaMensalContas",
                                    "Erro ao atualizar conta ID $id: ${e.message}"
                                )
                            }
                        }
                        refreshLista()
                    }
                    mode.finish()
                    true
                }
                R.id.botao_excluir -> {
                    if (contas.isNotEmpty()) {
                        if (contas.size == 1) {
                            idConta = contas[0]!!
                            dialogo()
                        } else {
                            AlertDialog.Builder(requireContext(), R.style.TemaDialogo)
                                .setTitle(R.string.confirmar_exclusao_multipla_titulo)
                                .setMessage(
                                    res!!.getQuantityString(
                                        R.plurals.confirmar_exclusao_multipla_mensagem,
                                        contas.size,
                                        contas.size
                                    )
                                )
                                .setPositiveButton(
                                    R.string.sim
                                ) { _, _ ->
                                    for (id in contas) {
                                        repository!!.excluirConta(id!!)
                                    }
                                    Toast.makeText(
                                        requireContext(),
                                        res!!.getQuantityString(
                                            R.plurals.dica_contas_excluidas,
                                            contas.size,
                                            contas.size
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    mode.finish()
                                    refreshLista()
                                }
                                .setNegativeButton(R.string.nao, null)
                                .show()
                        }
                    }
                    mode.finish()
                    true
                }
                R.id.botao_lembrete -> {
                    if (contas.size == 1) {
                        val id = contas[0]!!
                        try {
                            val contaParaLembrete = repository!!.getConta(id)
                            if (contaParaLembrete != null) {
                                val dia = contaParaLembrete.dia
                                val mesLembrete = contaParaLembrete.mes
                                val anoLembrete = contaParaLembrete.ano
                                val valorLembrete = contaParaLembrete.valor
                                val nomeContaCalendario = res!!.getString(
                                    R.string.dica_evento, contaParaLembrete.nome
                                )
                                c.set(anoLembrete, mesLembrete - 1, dia)
                                val evento = Intent(Intent.ACTION_EDIT).apply {
                                    type = "vnd.android.cursor.item/event"
                                    putExtra(CalendarContract.Events.TITLE, nomeContaCalendario)

                                    val current: Locale = res!!.configuration.locales.get(0)
                                    val dinheiroFormat = NumberFormat.getCurrencyInstance(current)

                                    putExtra(
                                        CalendarContract.Events.DESCRIPTION,
                                        res!!.getString(
                                            R.string.dica_calendario,
                                            dinheiroFormat.format(valorLembrete)
                                        )
                                    )
                                    putExtra(
                                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        c.timeInMillis
                                    )
                                    putExtra(
                                        CalendarContract.EXTRA_EVENT_END_TIME,
                                        c.timeInMillis
                                    )
                                    putExtra(
                                        CalendarContract.Events.ACCESS_LEVEL,
                                        CalendarContract.Events.ACCESS_PRIVATE
                                    )
                                    putExtra(
                                        CalendarContract.Events.AVAILABILITY,
                                        CalendarContract.Events.AVAILABILITY_BUSY
                                    )
                                }
                                startActivity(evento)
                            }
                        } catch (e: SQLException) {
                            Log.e("ListaMensalContas", "Erro lembrete: ${e.message}")
                            Toast.makeText(requireContext(), R.string.erro_geral_bd, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    mode.finish()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mActionMode = null
            buscaContas?.limpaSelecao()
            contas.clear()
            valorContaTotalSelecionada = 0.0
            
            (activity as? MinhasContas)?.syncViewPagerPositionAndRefresh(-1)
        }
    }

    // endregion
    // endregion
    // region 6. Listeners (Ouvintes)
    // =============================================================================================
    private val preferencias =
        OnSharedPreferenceChangeListener { _, key: String? ->
            if ("ordem" == key) {
                montaLista()
            }
        }

    // endregion
    // region 7. Lógica Principal (Core Logic Methods)
    // =============================================================================================
    private fun montaLista() {
        val ordem = buscaPreferencias!!.getString("ordem", ordemListaDeContas)
        Log.d(
            "MINHAS_CONTAS",
            "montaLista() - Iniciando busca. Mes=$mes, Ano=$ano, Tipo=$tipo, Filtro=$filtro, Ordem=$ordem"
        )

        // Não é mais necessário fechar 'contasParaLista' aqui, pois é uma List<Conta>
        // e o Cursor subjacente é fechado pelo método dbContasDoMes.getContas().
        val filter = ContaFilter()
        filter.setMes(mes)
            .setAno(ano)

        if (dia > 0) {
            filter.setDiaFim(dia)
        }

        if (tipo != -1) {
            filter.setTipo(tipo)
            if (filtro >= 0) {
                // Lógica de filtro mais robusta
                when (tipo) {
                    ContasContract.TIPO_DESPESA -> {
                        when (filtro) {
                            4 -> filter.setPagamento(ContasContract.STATUS_PENDENTE)
                            5 -> filter.setPagamento(ContasContract.STATUS_PAGO_RECEBIDO)
                            else -> filter.setClasse(filtro)
                        }
                    }
                    ContasContract.TIPO_RECEITA -> {
                        when (filtro) {
                            3 -> filter.setPagamento(ContasContract.STATUS_PENDENTE)
                            4 -> filter.setPagamento(ContasContract.STATUS_PAGO_RECEBIDO)
                            else -> filter.setClasse(filtro)
                        }
                    }
                    ContasContract.TIPO_APLICACAO -> {
                        filter.setClasse(filtro)
                    }
                    else -> { // Para outros tipos ou classes não específicas de status de pagamento
                        filter.setClasse(filtro)
                    }
                }
            }
        }
        // MUDANÇA PRINCIPAL: Usando o repositório que retorna List<Conta>
        // e aplicando a ordem obtida das preferências.
        contasParaLista = repository!!.getContas(filter, ordem)

        Log.d(
            "MINHAS_CONTAS",
            "montaLista() - Lista de Contas retornada com ${contasParaLista.size} registros."
        )


        // Atualizado para usar List<Conta>
        if (buscaContas == null) {
            // MUDANÇA: Construtor do AdaptaListaMensalRC agora aceita List<Conta>
            buscaContas = AdaptaListaMensalRC(requireContext(), contasParaLista)

            // Configuração do Listener de clique/toque longo para o RecyclerView
            buscaContas!!.setOnItemClickListener(object : AdaptaListaMensalRC.OnItemClickListener {
                override fun onItemClick(id: Long, position: Int) {
                    handleItemClick(id, position)
                }

                override fun onItemLongClick(id: Long, position: Int): Boolean {
                    return handleItemLongClick(id, position)
                }
            })

            listaContas!!.adapter = buscaContas
        } else {
            // MUDANÇA: Chama o novo método swapList() do adaptador
            buscaContas!!.swapList(contasParaLista)
        }

        // Tratamento manual para Empty View (RecyclerView não suporta setEmptyView diretamente)
        if (contasParaLista.isEmpty()) { // MUDANÇA: Verifica se a lista está vazia
            Log.d(
                "MINHAS_CONTAS",
                "montaLista() - Nenhum registro encontrado. Exibindo tvSemContas."
            )

            listaContas!!.visibility = View.GONE
            semContas!!.visibility = View.VISIBLE
        } else {
            Log.d("MINHAS_CONTAS", "montaLista() - Registros encontrados. Exibindo listaContas.")

            listaContas!!.visibility = View.VISIBLE
            semContas!!.visibility = View.GONE
        }

        contas.clear() // Limpa IDs de contas selecionadas
        valorContaTotalSelecionada = 0.0 // Reseta o valor total selecionado
        if (buscaContas != null) {
            buscaContas!!.limpaSelecao() // Garante que o estado de seleção do adaptador é resetado
        }
    }

    // endregion
    // region 8. Handlers de Interação (Interaction Handlers)
    // =============================================================================================
    /**
     * Lógica de toque simples (seleção individual ou toggle de multi-seleção).
     */
    private fun handleItemClick(id: Long, position: Int) {
        if (mActionMode != null) {
            toggleSelection(id, position)
        } else {
            // Se preferir que o clique apenas abra o item, chame a lógica de visualização aqui.
            // Para manter o comportamento atual de iniciar a seleção com um clique:
            startSelection(id, position)
        }
    }

    /**
     * Lógica de toque longo (inicia o modo de multi-seleção).
     */
    private fun handleItemLongClick(id: Long, position: Int): Boolean {
        if (mActionMode == null) {
            startSelection(id, position)
        } else {
            toggleSelection(id, position)
        }
        return true
    }

    private fun startSelection(id: Long, position: Int) {
        val clickedConta = buscaContas!!.getItem(position)
        if (clickedConta != null) {
            contas.clear()
            contas.add(id)
            idConta = id

            nomeConta = clickedConta.nome
            if (clickedConta.qtRepete > 1 && clickedConta.tipo == ContasContract.TIPO_DESPESA &&
                (clickedConta.classeConta == ContasContract.CLASSE_DESPESA_CARTAO ||
                        clickedConta.classeConta == ContasContract.CLASSE_DESPESA_PRESTACOES)
            ) {
                nomeConta = String.format(
                    Locale.getDefault(), "%s %d/%d",
                    clickedConta.nome, clickedConta.nRepete, clickedConta.qtRepete
                )
            }

            valorContaTotalSelecionada = clickedConta.valor

            buscaContas!!.marcaConta(id, true)

            val act = activity as AppCompatActivity?
            if (act != null) {
                mActionMode = act.startSupportActionMode(actionModeCallback)
                updateActionMode()
            }
        }
    }

    private fun toggleSelection(id: Long, position: Int) {
        val clickedConta = buscaContas!!.getItem(position)
        if (clickedConta != null) {
            if (contas.contains(id)) {
                contas.remove(id)
                buscaContas!!.marcaConta(id, false)
                valorContaTotalSelecionada -= clickedConta.valor
            } else {
                contas.add(id)
                buscaContas!!.marcaConta(id, true)
                valorContaTotalSelecionada += clickedConta.valor
            }

            if (contas.isEmpty()) {
                mActionMode?.finish()
            } else {
                updateActionMode()
            }
        }
    }

    private fun updateActionMode() {
        if (mActionMode != null) {
            val count = contas.size
            if (count == 1) {
                // Busca o nome do item único selecionado
                idConta = contas[0]!!
                for (c in contasParaLista) {
                    if (c.idConta == idConta) {
                        nomeConta = c.nome
                        if (c.qtRepete > 1 && c.tipo == ContasContract.TIPO_DESPESA &&
                            (c.classeConta == ContasContract.CLASSE_DESPESA_CARTAO ||
                                    c.classeConta == ContasContract.CLASSE_DESPESA_PRESTACOES)
                        ) {
                            nomeConta = String.format(
                                Locale.getDefault(), "%s %d/%d",
                                c.nome, c.nRepete, c.qtRepete
                            )
                        }
                        break
                    }
                }
                mActionMode!!.title = nomeConta
            } else {
                mActionMode!!.title = res!!.getQuantityString(R.plurals.selecao, count, count)
            }

            if (tipo != -1) {
                mActionMode!!.subtitle = dinheiro!!.format(valorContaTotalSelecionada)
            }
            mActionMode!!.invalidate() // Atualiza a visibilidade dos menus (onPrepareActionMode)
        }
    }

    // endregion
    // region 9. Métodos de UI/Diálogo
    // =============================================================================================
    private fun dialogo() {
        val dialogoBuilder = AlertDialog.Builder(requireActivity(), R.style.TemaDialogo)
        dialogoBuilder.setTitle(getString(R.string.dica_menu_exclusao))
        dialogoBuilder.setItems(
            R.array.TipoAjusteConta
        ) { _, id: Int ->
            try {
                // MUDANÇA: Obtém o POJO Conta para obter os detalhes necessários via Repositório
                val contaParaExcluir = repository!!.getConta(idConta)
                if (contaParaExcluir != null) {
                    val nomeContaExcluir = contaParaExcluir.nome
                    val nr = contaParaExcluir.nRepete

                    val cod = contaParaExcluir.codigo.ifEmpty { "" }

                    when (id) {
                        0 -> { // Excluir apenas esta conta
                            repository!!.excluirConta(idConta)
                        }
                        1 -> { // Excluir esta e as futuras contas recorrentes
                            repository!!.excluirContasRecorrentes(
                                idConta,
                                cod,
                                nr,
                                DBContas.TipoExclusao.DESTA_EM_DIANTE
                            )
                        }
                        2 -> { // Excluir todas as contas recorrentes com o mesmo código
                            repository!!.excluirContasRecorrentes(
                                idConta,
                                cod,
                                nr,
                                DBContas.TipoExclusao.TODAS_AS_REPETICOES
                            )
                        }
                    }
                    Toast.makeText(
                        activity,
                        String.format(
                            res!!.getString(R.string.dica_conta_excluida),
                            nomeContaExcluir
                        ), Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } catch (e: Exception) {
                Log.e(
                    "ListaMensalContas",
                    "Erro no diálogo de exclusão de conta com ID $idConta: ${e.message}"
                )
                Toast.makeText(requireContext(), R.string.erro_geral_bd, Toast.LENGTH_SHORT)
                    .show()
            }
            montaLista() // Chama montaLista para atualizar completamente e resetar o estado

            // ATUALIZAÇÃO DA TOOLBAR: Garante que o somatório na Activity reflita a exclusão
            (activity as? MinhasContas)?.syncViewPagerPositionAndRefresh(-1)

            idConta = 0
            nomeConta = " "
        }
        val alertDialog = dialogoBuilder.create()
        alertDialog.show()
    }

    companion object {
        // region 1. Campos (Fields)
        // =============================================================================================
        @JvmField
        var mActionMode: ActionMode? = null

        // endregion
        // region 2. Método de Fábrica Estático (Static Factory Method)
        // =============================================================================================
        @JvmStatic
        fun newInstance(mes: Int, ano: Int, dia: Int, tipo: Int, filtro: Int): ListaMensalContas {
            val fragment = ListaMensalContas()
            val args = Bundle()
            args.putInt("ano", ano)
            args.putInt("mes", mes)
            args.putInt("dia", dia)
            args.putInt("tipo", tipo)
            args.putInt("filtro", filtro)
            fragment.setArguments(args)
            return fragment
        }
    }
}