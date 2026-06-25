package com.msk.minhascontas.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.db.ContasContract;
import com.msk.minhascontas.db.ContasRepository;
import com.msk.minhascontas.db.DBContas;
import com.msk.minhascontas.db.MetaFinanceira;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Classe auxiliar para verificar regras de negócio e gerar notificações internas.
 */
public class AlertHelper {

    private final Context context;
    private final ContasRepository repository;
    private final SharedPreferences prefs;
    private final DBContas db;
    private final NumberFormat currencyFormat;

    public AlertHelper(Context context) {
        this.context = context;
        this.repository = ContasRepository.getInstance(context);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.db = DBContas.getInstance(context);
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    /**
     * Executa todas as verificações de alertas habilitadas pelo usuário.
     */
    public void verificarTodosAlertas() {
        // Realiza higiene do banco de notificações antes de processar novas
        db.limparNotificacoesAntigas();

        Calendar cal = Calendar.getInstance();
        int dia = cal.get(Calendar.DAY_OF_MONTH);
        int mes = cal.get(Calendar.MONTH) + 1; // 1-indexado
        int ano = cal.get(Calendar.YEAR);
        String nomeMes = new DateFormatSymbols().getMonths()[mes - 1];

        if (prefs.getBoolean(context.getString(R.string.pref_alert_vencimento_key), true)) {
            checkVencimentos(dia, mes, ano);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_limite_categoria_key), true)) {
            checkLimitesCategorias(mes, ano);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_objetivo_plano_key), true)) {
            checkObjetivosMetas();
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_receita_referencia_key), true)) {
            checkReceitaReferencia(mes, ano);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_despesa_receita_key), true)) {
            checkDespesasVsReceitas(mes, ano, nomeMes);
        }
        if (prefs.getBoolean(context.getString(R.string.pref_alert_falta_aplicacao_key), true)) {
            checkFaltaAplicacoes(mes, ano, nomeMes);
        }
    }

    private void checkVencimentos(int dia, int mes, int ano) {
        DBContas.ContaFilter filter = new DBContas.ContaFilter()
                .setDia(dia).setMes(mes).setAno(ano)
                .setPagamento(ContasContract.STATUS_PENDENTE);
        
        List<Conta> hoje = repository.getContas(filter, null);
        for (Conta c : hoje) {
            String msg = context.getString(R.string.alert_vencimento_msg, c.getNome(), currencyFormat.format(c.getValor()));
            db.addNotificacao(context.getString(R.string.pref_alert_vencimento_titulo), msg, "alert_vencimento");
        }
    }

    private void checkLimitesCategorias(int mes, int ano) {
        double receitaRef = prefs.getFloat("plan_receita_referencia", 0f);
        if (receitaRef <= 0) return;

        Map<Integer, Double> gastos = repository.getGastosPorCategoria(mes, ano);
        for (int i = 0; i <= 8; i++) {
            float perc = prefs.getFloat("plan_perc_" + i, -1f);
            if (perc > 0) {
                double limite = (perc / 100.0) * receitaRef;
                Double gastoAtual = gastos.get(i);
                if (gastoAtual != null && gastoAtual > limite) {
                    double ultrapassou = gastoAtual - limite;
                    String catName = LabelUtils.getCategoriaLabel(context, i);
                    String msg = context.getString(R.string.alert_limite_categoria_msg, currencyFormat.format(ultrapassou), catName);
                    db.addNotificacao(context.getString(R.string.pref_alert_limite_categoria_titulo), msg, "alert_limite_categoria");
                }
            }
        }
    }

    private void checkObjetivosMetas() {
        List<MetaFinanceira> metas = repository.getMetasAtivas().getValue();
        if (metas == null) return;

        for (MetaFinanceira m : metas) {
            if (m.getValorAtual() >= m.getValorObjetivo() && m.getValorObjetivo() > 0) {
                String detalhes = currencyFormat.format(m.getValorAtual());
                String msg = context.getString(R.string.alert_objetivo_atingido_msg, m.getNome(), detalhes);
                db.addNotificacao(context.getString(R.string.pref_alert_objetivo_plano_titulo), msg, "alert_objetivo_plano");
            }
        }
    }

    private void checkReceitaReferencia(int mes, int ano) {
        double receitaRef = prefs.getFloat("plan_receita_referencia", 0f);
        if (receitaRef <= 0) return;

        double totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null);
        if (totalReceitas > receitaRef) {
            double dif = totalReceitas - receitaRef;
            String msg = context.getString(R.string.alert_receita_acima_ref_msg, currencyFormat.format(dif));
            db.addNotificacao(context.getString(R.string.pref_alert_receita_referencia_titulo), msg, "alert_receita_referencia");
        }
    }

    private void checkDespesasVsReceitas(int mes, int ano, String nomeMes) {
        double totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null);
        double totalDespesas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_DESPESA, null);
        
        if (totalDespesas > totalReceitas && totalReceitas > 0) {
            double deficit = totalDespesas - totalReceitas;
            String msg = context.getString(R.string.alert_deficit_mensal_msg, nomeMes, currencyFormat.format(deficit));
            db.addNotificacao(context.getString(R.string.pref_alert_despesa_receita_titulo), msg, "alert_despesa_receita");
        }
    }

    private void checkFaltaAplicacoes(int mes, int ano, String nomeMes) {
        double totalAplicacoes = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_APLICACAO, null);
        if (totalAplicacoes <= 0) {
            double totalReceitas = repository.calcularTotalMensal(mes, ano, ContasContract.TIPO_RECEITA, null);
            if (totalReceitas > 0) {
                // Sugestão de 10% da receita ou saldo disponível
                double sugestao = totalReceitas * 0.1;
                String msg = context.getString(R.string.alert_falta_investimento_msg, nomeMes, currencyFormat.format(sugestao));
                db.addNotificacao(context.getString(R.string.pref_alert_falta_aplicacao_titulo), msg, "alert_falta_aplicacao");
            }
        }
    }
}
