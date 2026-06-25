package com.msk.minhascontas.features.listas

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Adapter para RecyclerView que exibe as contas mensais usando uma List de Objetos Conta.
 */
class AdaptaListaMensalRC(context: Context, private var contas: List<Conta>) :
    RecyclerView.Adapter<AdaptaListaMensalRC.ViewHolder>() {
    private val res: Resources = context.resources
    private val semana: Array<String?>
    private val categoriaConta: Array<String?>
    private val dinheiro: NumberFormat

    /**
     * Retorna o Set de IDs de contas selecionadas.
     */
    val selecoes: MutableSet<Long> = HashSet()

    // Constantes para cores
    private val redColor: Int = ContextCompat.getColor(context, R.color.despesa_color)
    private val greenColor: Int = ContextCompat.getColor(context, R.color.aplicacao_color)
    private val blueColor: Int = ContextCompat.getColor(context, R.color.receita_color)
    private val selectionColor: Int = ContextCompat.getColor(context, R.color.linha_selecionada)

    // Interface para manipular cliques
    private var itemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(id: Long, position: Int)
        fun onItemLongClick(id: Long, position: Int): Boolean
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.itemClickListener = listener
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.tvNomeContaCriada)
        val categoria: TextView = view.findViewById(R.id.tvNomeCategoria)
        val data: TextView = view.findViewById(R.id.tvDataContaCriada)
        val dia: TextView = view.findViewById(R.id.tvDiaContaCriada)
        val valor: TextView = view.findViewById(R.id.tvValorContaCriada)
        val pagamento: ImageView = view.findViewById(R.id.ivPagamento)
    }

    init {
        val current: Locale = res.configuration.locales.get(0)
        dinheiro = NumberFormat.getCurrencyInstance(current)
        semana = res.getStringArray(R.array.Semana)
        categoriaConta = res.getStringArray(R.array.CategoriaConta)
        setHasStableIds(true)
    }

    // -------------------------------------------------------------------------
    // Implementação de RecyclerView.Adapter
    // -------------------------------------------------------------------------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.linha_conta_nova, parent, false)
        val holder = ViewHolder(view)

        view.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                itemClickListener?.onItemClick(getItemId(pos), pos)
            }
        }

        view.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                itemClickListener?.onItemLongClick(getItemId(pos), pos) ?: false
            } else false
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conta = contas[position]
        val idConta = conta.idConta

        // 1. Popular Views - Nome (com ajuste para recorrentes)
        holder.nome.text = conta.nome

        if (conta.qtRepete > 1 && (conta.classeConta == 0 || conta.classeConta == 3) && conta.tipo == 0) {
            holder.nome.text = String.format(
                Locale.getDefault(), "%s %d/%d",
                conta.nome, conta.nRepete, conta.qtRepete
            )
        }

        // 2. Data e Dia da Semana
        holder.data.text = conta.dia.toString()
        val c = Calendar.getInstance()
        c.set(conta.ano, conta.mes - 1, conta.dia)
        val s = c.get(Calendar.DAY_OF_WEEK)
        holder.dia.text = semana[s - 1]

        // 3. Categoria e Classe
        when (conta.tipo) {
            0 -> { // Despesa
                val names = res.getStringArray(R.array.TipoDespesa)
                holder.categoria.text = String.format(
                    "%s | %s",
                    names[conta.classeConta],
                    categoriaConta[conta.categoria]
                )
            }
            1 -> { // Receita
                val names = res.getStringArray(R.array.TipoReceita)
                holder.categoria.text = names[conta.classeConta]
            }
            else -> { // Aplicação (tipo == 2)
                val names = res.getStringArray(R.array.TipoAplicacao)
                holder.categoria.text = names[conta.classeConta]
            }
        }

        // 4. Valor e Status de Pagamento/Cor
        holder.valor.text = dinheiro.format(conta.valor)
        holder.pagamento.visibility = View.INVISIBLE

        when (conta.tipo) {
            0 -> { // Despesa
                holder.valor.setTextColor(redColor)
                if ("paguei" == conta.pagamento) {
                    holder.pagamento.visibility = View.VISIBLE
                }
            }
            2 -> { // Aplicação
                holder.valor.setTextColor(greenColor)
            }
            else -> { // Receita
                holder.valor.setTextColor(blueColor)
                if ("paguei" == conta.pagamento) {
                    holder.pagamento.visibility = View.VISIBLE
                }
            }
        }

        // 5. Estado de Seleção
        if (selecoes.contains(idConta)) {
            holder.itemView.setBackgroundColor(selectionColor)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int = contas.size

    override fun getItemId(position: Int): Long = contas[position].idConta

    // -------------------------------------------------------------------------
    // Métodos Auxiliares
    // -------------------------------------------------------------------------
    fun getItem(position: Int): Conta? {
        return if (position in contas.indices) contas[position] else null
    }

    fun swapList(newList: List<Conta>) {
        this.contas = newList
        notifyDataSetChanged()
    }

    fun marcaConta(idConta: Long, seleciona: Boolean) {
        val position = contas.indexOfFirst { it.idConta == idConta }
        if (seleciona) {
            selecoes.add(idConta)
        } else {
            selecoes.remove(idConta)
        }

        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun limpaSelecao() {
        if (selecoes.isNotEmpty()) {
            selecoes.clear()
            notifyDataSetChanged()
        }
    }
}
