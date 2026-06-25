package com.msk.minhascontas.features.resumos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract
import com.msk.minhascontas.db.ContasContract.CATEGORIA_ALIMENTACAO
import com.msk.minhascontas.db.ContasContract.CATEGORIA_EDUCACAO
import com.msk.minhascontas.db.ContasContract.CATEGORIA_MORADIA
import com.msk.minhascontas.db.ContasContract.CATEGORIA_OUTROS
import com.msk.minhascontas.db.ContasContract.CATEGORIA_SAUDE
import com.msk.minhascontas.db.ContasContract.CATEGORIA_TRANSPORTE
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_FUNDOS
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_POUPANCA
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL
import com.msk.minhascontas.db.ContasContract.TIPO_APLICACAO
import com.msk.minhascontas.db.ContasContract.TIPO_DESPESA
import com.msk.minhascontas.db.ContasContract.TIPO_RECEITA
import com.msk.minhascontas.db.DBContas.ContaFilter
import com.msk.minhascontas.utils.LabelUtils.getCategoriaLabel
import com.msk.minhascontas.utils.LabelUtils.getClasseLabel

class ResumoCategoriaMensal : BaseResumoFragment() {
    private var valorDesp: TextView? = null
    private var valorRec: TextView? = null
    private var valorAplic: TextView? = null
    private var valorSaldo: TextView? = null
    private var valorBanco: TextView? = null
    private var valorPagar: TextView? = null
    private var valorPago: TextView? = null
    private var valorCartao: TextView? = null
    private var valorSaldoAtual: TextView? = null
    private var valorSaldoAnterior: TextView? = null
    private var valorFundos: TextView? = null
    private var valorPoupanca: TextView? = null
    private var valorPrevidencia: TextView? = null
    private var vAlimentacao: TextView? = null
    private var vEducacao: TextView? = null
    private var vMoradia: TextView? = null
    private var vTransporte: TextView? = null
    private var vSaude: TextView? = null
    private var vOutros: TextView? = null
    private var valorReceber: TextView? = null
    private var valorRecebido: TextView? = null

    private var tAlimentacao: TextView? = null
    private var tEducacao: TextView? = null
    private var tMoradia: TextView? = null
    private var tTransporte: TextView? = null
    private var tSaude: TextView? = null
    private var tOutros: TextView? = null
    private var tFundos: TextView? = null
    private var tPoupanca: TextView? = null
    private var tPrevidencia: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDadosAtualizados() {
        // Implementação vazia, mantida como no original
    }

    override val layoutResId: Int
        get() = R.layout.resumo_por_categoria

    override fun initializeArrays() {
        valores = DoubleArray(4)
        valoresDesp = DoubleArray(11)
        valoresRec = DoubleArray(2)
        valoresSaldo = DoubleArray(4)
        valoresAplicados = DoubleArray(4)
        valoresAplicAnterior = DoubleArray(5) // 0-3 classes, 4 total
    }

    override fun iniciarViews(view: View?) {
        if (view == null) return
        layoutAplicacoes = view.findViewById(R.id.resumo_aplicacoes)
        layoutDespesas = view.findViewById(R.id.resumo_despesas)
        layoutReceitas = view.findViewById(R.id.resumo_receitas)
        layoutSaldo = view.findViewById(R.id.resumo_saldo)

        valorReceber = layoutReceitas?.findViewById(R.id.valor_receber)
        valorRecebido = layoutReceitas?.findViewById(R.id.valor_recebido)
        valorRec = layoutReceitas?.findViewById(R.id.valor_receitas)

        valorPago = layoutDespesas?.findViewById(R.id.valor_desp_paga)
        valorPagar = layoutDespesas?.findViewById(R.id.valor_desp_pagar)
        vAlimentacao = layoutDespesas?.findViewById(R.id.valor_alimentacao)
        vEducacao = layoutDespesas?.findViewById(R.id.valor_educacao)
        vMoradia = layoutDespesas?.findViewById(R.id.valor_moradia)
        vTransporte = layoutDespesas?.findViewById(R.id.valor_transporte)
        vSaude = layoutDespesas?.findViewById(R.id.valor_saude)
        vOutros = layoutDespesas?.findViewById(R.id.valor_outros)
        valorDesp = layoutDespesas?.findViewById(R.id.valor_despesas)

        tAlimentacao = layoutDespesas?.findViewById(R.id.tv_titulo_alimentacao)
        tEducacao = layoutDespesas?.findViewById(R.id.tv_titulo_educacao)
        tMoradia = layoutDespesas?.findViewById(R.id.tv_titulo_moradia)
        tTransporte = layoutDespesas?.findViewById(R.id.tv_titulo_transporte)
        tSaude = layoutDespesas?.findViewById(R.id.tv_titulo_saude)
        tOutros = layoutDespesas?.findViewById(R.id.tv_titulo_outros)

        valorFundos = layoutAplicacoes?.findViewById(R.id.valor_fundos)
        valorPoupanca = layoutAplicacoes?.findViewById(R.id.valor_poupancas)
        valorPrevidencia = layoutAplicacoes?.findViewById(R.id.valor_previdencias)
        valorAplic = layoutAplicacoes?.findViewById(R.id.valor_aplicacoes)

        tFundos = layoutAplicacoes?.findViewById(R.id.tv_titulo_fundos)
        tPoupanca = layoutAplicacoes?.findViewById(R.id.tv_titulo_poupancas)
        tPrevidencia = layoutAplicacoes?.findViewById(R.id.tv_titulo_previdencias)

        valorSaldoAtual = layoutSaldo?.findViewById(R.id.valor_saldo_atual)
        valorSaldoAnterior = layoutSaldo?.findViewById(R.id.valor_saldo_anterior)
        valorBanco = layoutSaldo?.findViewById(R.id.valor_banco)
        valorCartao = layoutSaldo?.findViewById(R.id.valor_cartao_credito)
        valorSaldo = layoutSaldo?.findViewById(R.id.valor_saldo)
    }

    override val contaFilter: ContaFilter?
        get() = ContaFilter().setMes(this.mes).setAno(this.ano)

    override fun saldo(contasAtuais: List<Conta>) {
        val vals = valores ?: return
        val vRec = valoresRec ?: return
        val vDesp = valoresDesp ?: return
        val vAplic = valoresAplicados ?: return
        val vAplicAnt = valoresAplicAnterior ?: return
        val vSald = valoresSaldo ?: return
        val repo = repository ?: return
        val prefs = buscaPreferencias ?: return

        // 1. Somas do período atual em memória (Otimizado via Flow)
        vals[0] = contasAtuais.filter { it.tipo == TIPO_RECEITA }.sumOf { it.valor }
        vRec[0] = contasAtuais.filter { it.tipo == TIPO_RECEITA && it.pagamento == "paguei" }.sumOf { it.valor }
        vRec[1] = contasAtuais.filter { it.tipo == TIPO_RECEITA && it.pagamento == "falta" }.sumOf { it.valor }
        
        vals[1] = contasAtuais.filter { it.tipo == TIPO_DESPESA }.sumOf { it.valor }
        vDesp[0] = contasAtuais.filter { it.tipo == TIPO_DESPESA && it.pagamento == "paguei" }.sumOf { it.valor }
        vDesp[1] = contasAtuais.filter { it.tipo == TIPO_DESPESA && it.pagamento == "falta" }.sumOf { it.valor }
        
        for (i in 0..8) {
            vDesp[i + 2] = contasAtuais.filter { it.tipo == TIPO_DESPESA && it.categoria == i }.sumOf { it.valor }
        }
        
        vals[2] = contasAtuais.filter { it.tipo == TIPO_APLICACAO }.sumOf { it.valor }
        for (j in 0..3) {
            vAplic[j] = contasAtuais.filter { it.tipo == TIPO_APLICACAO && it.classeConta == j }.sumOf { it.valor }
        }

        // 2. Buscas de dados históricos (Ainda via Repositório, mas agora usando Room)
        for (k in 0..3) {
            vAplicAnt[k] = repo.somaAplicacoesAnteriores(0, this.mes, this.ano, isMonthly = true, k)
        }
        vAplicAnt[4] = repo.somaAplicacoesAnteriores(0, this.mes, this.ano, isMonthly = true, -1)

        val acumulaAplic = prefs.getBoolean("aplicacao_acumulada", false)
        if (acumulaAplic) {
            vals[2] += vAplicAnt[4]
            for (l in 0..3) {
                vAplic[l] += vAplicAnt[l]
            }
        }

        vSald[0] = vals[0] - vals[1]
        val somaSaldo = prefs.getBoolean("saldo", false)
        if (somaSaldo) {
            vSald[1] = repo.somaSaldoAnterior(0, this.mes, this.ano, isMonthly = true)
            vals[3] = (vRec[0] - vDesp[0]) + vSald[1]
        } else {
            vSald[1] = getSaldoMesAnterior(this.mes, this.ano)
            vals[3] = vRec[0] - vDesp[0]
        }

        vSald[2] = vRec[0] // Banco/Carteira (Aproximação)
        vSald[3] = contasAtuais.filter { it.tipo == TIPO_DESPESA && it.classeConta == ContasContract.CLASSE_DESPESA_CARTAO }.sumOf { it.valor }
    }

    override fun insereValores() {
        val context = context
        val vals = valores ?: return
        val vRec = valoresRec ?: return
        val vDesp = valoresDesp ?: return
        val vAplic = valoresAplicados ?: return
        val vSald = valoresSaldo ?: return

        if (context == null || !isAdded) {
            Log.w(TAG, "insereValores abortado: Fragment não anexado.")
            return
        }

        val dinheiro = currencyFormat

        tAlimentacao?.text = getCategoriaLabel(context, CATEGORIA_ALIMENTACAO)
        tEducacao?.text = getCategoriaLabel(context, CATEGORIA_EDUCACAO)
        tMoradia?.text = getCategoriaLabel(context, CATEGORIA_MORADIA)
        tSaude?.text = getCategoriaLabel(context, CATEGORIA_SAUDE)
        tTransporte?.text = getCategoriaLabel(context, CATEGORIA_TRANSPORTE)
        tOutros?.text = getCategoriaLabel(context, CATEGORIA_OUTROS)

        tFundos?.text = getClasseLabel(context, TIPO_APLICACAO, CLASSE_APLICACAO_FUNDOS)
        tPoupanca?.text = getClasseLabel(context, TIPO_APLICACAO, CLASSE_APLICACAO_POUPANCA)
        tPrevidencia?.text =
            getClasseLabel(
                context,
                TIPO_APLICACAO,
                CLASSE_APLICACAO_RENDAVARIAVEL,
            )

        valorPago?.text = dinheiro.format(vDesp[0])
        valorPagar?.text = dinheiro.format(vDesp[1])

        vAlimentacao?.text = dinheiro.format(vDesp[2])
        vEducacao?.text = dinheiro.format(vDesp[3])
        vMoradia?.text = dinheiro.format(vDesp[5])
        vSaude?.text = dinheiro.format(vDesp[6])
        vTransporte?.text = dinheiro.format(vDesp[7])
        vOutros?.text = dinheiro.format(vDesp[4] + vDesp[8] + vDesp[9] + vDesp[10]) // Lazer (4), Vestuário (8), Outros (9), Investimentos (10)

        valorCartao?.text = dinheiro.format(vSald[3])
        valorCartao?.setTextColor(getBalanceColor(vSald[3]))
        valorBanco?.text = dinheiro.format(vSald[2])
        valorBanco?.setTextColor(getBalanceColor(vSald[2]))

        valorReceber?.text = dinheiro.format(vRec[1])
        valorRecebido?.text = dinheiro.format(vRec[0])

        valorFundos?.text = dinheiro.format(vAplic[0])
        valorPoupanca?.text = dinheiro.format(vAplic[1])
        valorPrevidencia?.text = dinheiro.format(vAplic[2])

        valorSaldoAtual?.text = dinheiro.format(vSald[0])
        valorSaldoAtual?.setTextColor(getBalanceColor(vSald[0]))

        valorSaldoAnterior?.text = dinheiro.format(vSald[1])
        valorSaldoAnterior?.setTextColor(getBalanceColor(vSald[1]))

        valorDesp?.text = dinheiro.format(vals[1])
        valorRec?.text = dinheiro.format(vals[0])
        valorAplic?.text = dinheiro.format(vals[2])
        valorSaldo?.text = dinheiro.format(vals[3])

        valorSaldo?.setTextColor(getBalanceColor(vals[3]))
    }

    companion object {
        private const val TAG = "ResumoCategoriaMensal"

        @JvmStatic
        fun newInstance(nrPagina: Int, mes: Int, ano: Int): ResumoCategoriaMensal {
            val fragment = ResumoCategoriaMensal()
            val args = Bundle()
            args.putInt("ano", ano)
            args.putInt("mes", mes)
            args.putInt(ARG_NR_PAGINA, nrPagina)
            fragment.arguments = args
            return fragment
        }
    }
}
