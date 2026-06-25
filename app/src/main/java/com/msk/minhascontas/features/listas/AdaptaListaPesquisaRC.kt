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
import com.msk.minhascontas.utils.LabelUtils
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Adapter para RecyclerView que exibe os resultados da pesquisa usando List<Conta>.
 * Migrado de Cursor para uma implementação mais segura com Room.
 */
class AdaptaListaPesquisaRC(context: Context, private var contas: List<Conta> = emptyList()) :
    RecyclerView.Adapter<AdaptaListaPesquisaRC.ViewHolder>() {
    private val res: Resources = context.resources
    private val semana: Array<String?>
    private val categoriaConta: Array<String?>
    private val dinheiro: NumberFormat
    private val dataFormato: DateFormat

    /**
     * Retorna o Set de IDs de contas selecionadas.
     */
    val selecoes: MutableSet<Long> = HashSet()

    // Constantes para cores
    private val redColor: Int = ContextCompat.getColor(context, R.color.despesa_color)
    private val greenColor: Int = ContextCompat.getColor(context, R.color.aplicacao_color)
    private val blueColor: Int = ContextCompat.getColor(context, R.color.receita_color)
    private val selectionColor: Int = ContextCompat.getColor(context, R.color.linha_selecionada)

    private var itemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(id: Long, position: Int)
        fun onItemLongClick(id: Long, position: Int): Boolean
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.itemClickListener = listener
    }

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
        dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current)
        semana = res.getStringArray(R.array.Semana)
        categoriaConta = res.getStringArray(R.array.CategoriaConta)
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.linha_pesquisa, parent, false)
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

        holder.nome.text = conta.nome
        if (conta.qtRepete > 1 && (conta.classeConta == 0 || conta.classeConta == 3) && conta.tipo == 0) {
            holder.nome.text = String.format(Locale.getDefault(), "%s %d/%d", conta.nome, conta.nRepete, conta.qtRepete)
        }

        val c = Calendar.getInstance()
        c.set(conta.ano, conta.mes - 1, conta.dia)
        holder.data.text = dataFormato.format(c.time)
        holder.dia.text = semana[c.get(Calendar.DAY_OF_WEEK) - 1]

        holder.categoria.text = LabelUtils.getClasseLabel(holder.itemView.context, conta.tipo, conta.classeConta)
        if (conta.tipo == 0) {
            holder.categoria.text = "${holder.categoria.text} | ${categoriaConta[conta.categoria]}"
        }

        holder.valor.text = dinheiro.format(conta.valor)
        holder.pagamento.visibility = View.INVISIBLE

        when (conta.tipo) {
            0 -> {
                holder.valor.setTextColor(redColor)
                if ("paguei" == conta.pagamento) holder.pagamento.visibility = View.VISIBLE
            }
            2 -> holder.valor.setTextColor(greenColor)
            else -> {
                holder.valor.setTextColor(blueColor)
                if ("paguei" == conta.pagamento) holder.pagamento.visibility = View.VISIBLE
            }
        }

        holder.itemView.setBackgroundColor(if (selecoes.contains(idConta)) selectionColor else Color.TRANSPARENT)
    }

    override fun getItemCount(): Int = contas.size
    override fun getItemId(position: Int): Long = contas[position].idConta

    fun swapList(newList: List<Conta>) {
        this.contas = newList
        notifyDataSetChanged()
    }

    fun marcaConta(idConta: Long, seleciona: Boolean) {
        val position = contas.indexOfFirst { it.idConta == idConta }
        if (seleciona) selecoes.add(idConta) else selecoes.remove(idConta)
        if (position != -1) notifyItemChanged(position)
    }

    fun limpaSelecao() {
        if (selecoes.isNotEmpty()) {
            selecoes.clear()
            notifyDataSetChanged()
        }
    }
}
