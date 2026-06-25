package com.msk.minhascontas.features.resumos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.msk.minhascontas.R
import com.msk.minhascontas.db.Conta
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_FUNDOS
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_POUPANCA
import com.msk.minhascontas.db.ContasContract.CLASSE_APLICACAO_RENDAVARIAVEL
import com.msk.minhascontas.db.ContasContract.CLASSE_DESPESA_CARTAO
import com.msk.minhascontas.db.ContasContract.CLASSE_DESPESA_FIXA
import com.msk.minhascontas.db.ContasContract.CLASSE_DESPESA_PRESTACOES
import com.msk.minhascontas.db.ContasContract.CLASSE_DESPESA_VARIAVEL
import com.msk.minhascontas.db.ContasContract.TIPO_APLICACAO
import com.msk.minhascontas.db.ContasContract.TIPO_DESPESA
import com.msk.minhascontas.db.ContasContract.TIPO_RECEITA
import com.msk.minhascontas.db.DBContas.ContaFilter
import com.msk.minhascontas.utils.LabelUtils.getClasseLabel

class ResumoTipoDiario : BaseResumoFragment() {
    private var valorDesp: TextView? = null
    private var valorRec: TextView? = null
    private var valorAplic: TextView? = null
    private var valorSaldo: TextView? = null
    private var valorPagar: TextView? = null
    private var valorPago: TextView? = null
    private var valorCartao: TextView? = null
    private var valorSaldoAtual: TextView? = null
    private var valorSaldoAnterior: TextView? = null
    private var valorDespFixa: TextView? = null
    private var valorDespVar: TextView? = null
    private var valorPrestacoes: TextView? = null
    private var valorFundos: TextView? = null
    private var valorPoupanca: TextView? = null
    private var valorPrevidencia: TextView? = null
    private var valorReceber: TextView? = null
    private var valorRecebido: TextView? = null

    private var tDespFixa: TextView? = null
    private var tDespVar: TextView? = null
    private var tPrestacoes: TextView? = null
    private var tCartao: TextView? = null
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
        get() = R.layout.resumo_por_tipo

    override fun initializeArrays() {
        valores = DoubleArray(4)
        valoresDesp = DoubleArray(6)
        valoresRec = DoubleArray(2)
        valoresSaldo = DoubleArray(2)
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
        valorDespFixa = layoutDespesas?.findViewById(R.id.valor_desp_fixa)
        valorDespVar = layoutDespesas?.findViewById(R.id.valor_desp_var)
        valorPrestacoes = layoutDespesas?.findViewById(R.id.valor_prestacoes)
        valorCartao = layoutDespesas?.findViewById(R.id.valor_cartao_credito)
        valorDesp = layoutDespesas?.findViewById(R.id.valor_despesas)

        tDespFixa = layoutDespesas?.findViewById(R.id.tv_titulo_desp_fixa)
        tDespVar = layoutDespesas?.findViewById(R.id.tv_titulo_desp_var)
        tPrestacoes = layoutDespesas?.findViewById(R.id.tv_titulo_prestacoes)
        tCartao = layoutDespesas?.findViewById(R.id.tv_titulo_cartao)

        valorFundos = layoutAplicacoes?.findViewById(R.id.valor_fundos)
        valorPoupanca = layoutAplicacoes?.findViewById(R.id.valor_poupancas)
        valorPrevidencia = layoutAplicacoes?.findViewById(R.id.valor_previdencias)
        valorAplic = layoutAplicacoes?.findViewById(R.id.valor_aplicacoes)

        tFundos = layoutAplicacoes?.findViewById(R.id.tv_titulo_fundos)
        tPoupanca = layoutAplicacoes?.findViewById(R.id.tv_titulo_poupancas)
        tPrevidencia = layoutAplicacoes?.findViewById(R.id.tv_titulo_previdencias)

        valorSaldoAtual = layoutSaldo?.findViewById(R.id.valor_saldo_atual)
        valorSaldoAnterior = layoutSaldo?.findViewById(R.id.valor_saldo_anterior)
        valorSaldo = layoutSaldo?.findViewById(R.id.valor_saldo)
    }

    override val contaFilter: ContaFilter?
        get() = ContaFilter().setDia(this.dia).setMes(this.mes).setAno(this.ano)

    override fun saldo(contasAtuais: List<Conta>) {
        val vals = valores ?: return
        val vRec = valoresRec ?: return
        val vDesp = valoresDesp ?: return
        val vAplic = valoresAplicados ?: return
        val vAplicAnt = valoresAplicAnterior ?: return
        val vSald = valoresSaldo ?: return
        val repo = repository ?: return
        val prefs = buscaPreferencias ?: return

        // 1. Somas do período atual (do dia 1 até 'this.dia') em memória
        val contasNoPeriodo = contasAtuais.filter { it.dia <= this.dia }

        vals[0] = contasNoPeriodo.filter { it.tipo == TIPO_RECEITA }.sumOf { it.valor }
        vRec[0] = contasNoPeriodo.filter { it.tipo == TIPO_RECEITA && it.pagamento == "paguei" }.sumOf { it.valor }
        vRec[1] = contasNoPeriodo.filter { it.tipo == TIPO_RECEITA && it.pagamento == "falta" }.sumOf { it.valor }

        vals[1] = contasNoPeriodo.filter { it.tipo == TIPO_DESPESA }.sumOf { it.valor }
        vDesp[0] = contasNoPeriodo.filter { it.tipo == TIPO_DESPESA && it.pagamento == "paguei" }.sumOf { it.valor }
        vDesp[1] = contasNoPeriodo.filter { it.tipo == TIPO_DESPESA && it.pagamento == "falta" }.sumOf { it.valor }

        for (i in 0..3) {
            vDesp[i + 2] = contasNoPeriodo.filter { it.tipo == TIPO_DESPESA && it.classeConta == i }.sumOf { it.valor }
        }

        vals[2] = contasNoPeriodo.filter { it.tipo == TIPO_APLICACAO }.sumOf { it.valor }
        for (j in 0..3) {
            vAplic[j] = contasNoPeriodo.filter { it.tipo == TIPO_APLICACAO && it.classeConta == j }.sumOf { it.valor }
        }

        // 2. Buscas históricas (IO via Room no Repository)
        for (k in 0..3) {
            vAplicAnt[k] = repo.somaAplicacoesAnteriores(1, this.mes, this.ano, false, k)
        }
        vAplicAnt[4] = repo.somaAplicacoesAnteriores(1, this.mes, this.ano, false, -1)

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
            vSald[1] = repo.somaSaldoAnterior(1, this.mes, this.ano, false)
            vals[3] = (vRec[0] - vDesp[0]) + vSald[1]
        } else {
            vSald[1] = getSaldoMesAnterior(this.mes, this.ano)
            vals[3] = vRec[0] - vDesp[0]
        }
    }

    override fun insereValores() {
        val context = getContext()
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

        tCartao?.text = getClasseLabel(context, TIPO_DESPESA, CLASSE_DESPESA_CARTAO)
        tDespFixa?.text = getClasseLabel(context, TIPO_DESPESA, CLASSE_DESPESA_FIXA)
        tDespVar?.text = getClasseLabel(context, TIPO_DESPESA, CLASSE_DESPESA_VARIAVEL)
        tPrestacoes?.text = getClasseLabel(context, TIPO_DESPESA, CLASSE_DESPESA_PRESTACOES)

        tFundos?.text = getClasseLabel(context, TIPO_APLICACAO, CLASSE_APLICACAO_FUNDOS)
        tPoupanca?.text = getClasseLabel(context, TIPO_APLICACAO, CLASSE_APLICACAO_POUPANCA)
        tPrevidencia?.text =
            getClasseLabel(
                context,
                TIPO_APLICACAO,
                CLASSE_APLICACAO_RENDAVARIAVEL
            )

        valorPago?.text = dinheiro.format(vDesp[0])
        valorPagar?.text = dinheiro.format(vDesp[1])

        valorCartao?.text = dinheiro.format(vDesp[2])
        valorDespFixa?.text = dinheiro.format(vDesp[3])
        valorDespVar?.text = dinheiro.format(vDesp[4])
        valorPrestacoes?.text = dinheiro.format(vDesp[5])

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
        private const val TAG = "ResumoTipoDiario"

        @JvmStatic
        fun newInstance(dia: Int, mes: Int, ano: Int): ResumoTipoDiario {
            val fragment = ResumoTipoDiario()
            val args = Bundle()
            args.putInt("ano", ano)
            args.putInt("mes", mes)
            args.putInt("dia", dia)
            fragment.arguments = args
            return fragment
        }
    }
}
