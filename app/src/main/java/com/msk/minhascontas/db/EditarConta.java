package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.msk.minhascontas.R;
import com.msk.minhascontas.tarefas.BarraProgresso;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.msk.minhascontas.db.DBContas.ContaFilter;
import com.msk.minhascontas.db.DBContas.TipoAtualizacao;

import static com.msk.minhascontas.db.DBContas.PAGAMENTO_FALTA;
import static com.msk.minhascontas.db.DBContas.PAGAMENTO_PAGO;

@SuppressLint("NewApi")
public class EditarConta extends AppCompatActivity implements
        View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    // --- Constantes ---
    // (Nenhuma constante explícita além das do DBContas, que são importadas estaticamente)

    // --- Variáveis Membro ---
    private BarraProgresso mProgressTask; // Usado para operações em segundo plano, como atualização de muitas repetições (embora não implementado na versão fornecida)
    private static Button data; // Botão para selecionar a data da conta
    private static int dia, mes, ano; // Variáveis estáticas para o DatePickerFragment
    private DBContas dbContaParaEditar;
    private AlertDialog dialogo; // Diálogo para escolha de edição (somente esta, desta em diante, todas)

    // Elementos da UI
    private TextInputEditText nome;
    private AutoCompleteTextView classificaConta, contaCategoria, intervaloRepete;
    private AppCompatEditText valor, etPrestacoes, juros;
    private RadioGroup tipo;
    private AppCompatRadioButton rec, desp, aplic;
    private AppCompatCheckBox pagamento;
    private LinearLayout categoria;
    private MaterialToolbar toolbar;

    // Variáveis de Dados da Conta
    private Conta conta; // Objeto POJO da conta sendo editada
    private double valorNovoConta;
    private double valorJurosNovo;
    private long idConta;
    private int tipoConta, classeConta, categoriaConta,
            qtPrest, // Quantidade total de repetições (originalmente 'qtPrestacoes')
            intervalo, // Intervalo de repetição (ex: 101-diario, 107-semanal, 300-mensal, 3650-anual)
            qtConta, // Quantidade total de repetições da série no BD
            nr, // Controla o escopo da edição (0: esta, 1: todas, >1: desta em diante)
            altera; // Flag para indicar se a conta foi alterada e mostrar Toast
    private String novoPagouConta, novoNomeConta;
    private Resources res;


    // --- Métodos de Ciclo de Vida da Activity ---

    @Override
    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.edita_conta);

        Bundle localBundle = getIntent().getExtras();
        if (localBundle != null) {
            idConta = localBundle.getLong("id");
        }

        res = getResources();
        dbContaParaEditar = DBContas.getInstance(this);

        IniciarComponentesUI();
        usarActionBar();
        carregarEExibirDadosDaConta(); // Carrega dados do BD e preenche a UI
        setDropdownListeners(); // Configura os listeners dos AutoCompleteTextViews
        definirListeners(); // Configura outros listeners (botões, radio group)
    }

    @Override
    protected void onDestroy() {
        if (altera == 1) {
            Toast.makeText(
                    getApplicationContext(),
                    String.format(
                            getResources().getString(
                                    R.string.dica_conta_alterada),
                            novoNomeConta), Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, null);
        // Cancela a AsyncTask se ela estiver em execução
        if (mProgressTask != null && mProgressTask.getStatus() == BarraProgresso.Status.RUNNING) {
            mProgressTask.cancel(true);
        }
        super.onDestroy();
    }


    // --- Inicialização da UI e Dados ---

    private void IniciarComponentesUI() {
        toolbar = findViewById(R.id.toolbar);
        data = findViewById(R.id.etDataConta);
        valor = findViewById(R.id.etValorNovo);
        juros = findViewById(R.id.etJurosContaModificada);
        tipo = findViewById(R.id.rgTipoContaModificada);
        rec = findViewById(R.id.rRecContaModificada);
        desp = findViewById(R.id.rDespContaModificada);
        aplic = findViewById(R.id.rAplicContaModificada);
        etPrestacoes = findViewById(R.id.etPrestacoes);
        classificaConta = findViewById(R.id.spClasseConta);
        contaCategoria = findViewById(R.id.spCategoriaConta);
        intervaloRepete = findViewById(R.id.spRepeticoes);
        categoria = findViewById(R.id.layout_categoria);
        nome = findViewById(R.id.acNomeContaModificada);
        pagamento = findViewById(R.id.cbPagamento);
        // O checkbox de pagamento é inicialmente escondido e sua visibilidade é gerenciada por configurarAdaptadoresSpinner
        pagamento.setVisibility(View.GONE);
    }

    private void usarActionBar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(R.string.titulo_editar);
        }
    }

    private void carregarEExibirDadosDaConta() {
        PegaContaDoBancoDeDados(); // Carrega a conta pelo ID

        if (conta != null) {
            ConfereRepeticaoDaConta(); // Verifica se a conta é recorrente e exibe diálogo se necessário
            MostraDadosNaUI(); // Popula os campos da UI com os dados da conta
        } else {
            Toast.makeText(this, "Erro ao carregar conta (ID " + idConta + ").", Toast.LENGTH_LONG).show();
            finish(); // Fecha a activity se a conta não puder ser carregada
        }
    }

    private void PegaContaDoBancoDeDados() {
        // Usa o método de DBContas que retorna o POJO Conta diretamente
        conta = dbContaParaEditar.getContaById(idConta);

        if (conta != null) {
            // Transfere os dados do POJO para as variáveis de instância
            dia = conta.getDia();
            mes = conta.getMes();
            ano = conta.getAno();
            tipoConta = conta.getTipo();
            classeConta = conta.getClasseConta();
            categoriaConta = conta.getCategoria();
            novoPagouConta = conta.getPagamento();
            intervalo = conta.getIntervalo();
            qtPrest = conta.getQtRepete();
            nr = conta.getNRepete(); // Importante: Número da repetição atual
        }
    }

    private void ConfereRepeticaoDaConta() {
        // Usando getContasByFilter para verificar por repetições do código da conta
        if (conta != null && conta.getCodigo() != null) {
            ContaFilter filter = new ContaFilter().setCodigoConta(conta.getCodigo());
            try (Cursor c = dbContaParaEditar.getContasByFilter(filter, null)) {
                qtConta = c.getCount();
            }
            if (qtConta > 1) {
                DialogoEscolhaEdicaoRepeticoes(); // Exibe diálogo se a conta tiver repetições
            }
        } else {
            qtConta = 0;
        }
    }

    private void MostraDadosNaUI() {
        nome.setText(conta.getNome());

        Calendar c = Calendar.getInstance();
        c.set(conta.getAno(), conta.getMes(), conta.getDia());
        Locale current = res.getConfiguration().getLocales().get(0);
        DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
        data.setText(dataFormato.format(c.getTime()));

        valor.setText(String.format(Locale.US, "%.2f", conta.getValor()));
        if (juros != null) {
            juros.setText(String.format(Locale.US, "%.2f", conta.getValorJuros() * 100)); // Mostra o valor percentual (multiplicado por 100)
        }
        pagamento.setChecked(PAGAMENTO_PAGO.equals(conta.getPagamento())); // Define o estado do checkbox

        ArrayAdapter<String> categoriasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.CategoriaConta));
        ArrayAdapter<String> intervalosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.repete_conta));

        configurarAdaptadoresSpinnerETipo(conta.getTipo()); // Configura adaptadores e seleção de tipo
        contaCategoria.setAdapter(categoriasAdapter);
        if (categoriaConta >= 0 && categoriaConta < categoriasAdapter.getCount()) {
            contaCategoria.setText(categoriasAdapter.getItem(categoriaConta), false);
        }
        intervaloRepete.setAdapter(intervalosAdapter);
        etPrestacoes.setText(String.valueOf(qtPrest)); // Mostra a quantidade de prestações

        setIntervaloRepeticaoTexto(intervalo, intervalosAdapter); // Define o texto do intervalo de repetição
    }

    // Helper method to configure adapters and visibility based on account type
    private void configurarAdaptadoresSpinnerETipo(int tipoContaAtual) {
        ArrayAdapter<String> classesContasAdapter;
        Resources res = getResources();

        int toolbarColor = 0;

        if (tipoContaAtual == 0) { // Despesa
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoDespesa));
            desp.setChecked(true); // Seleciona o RadioButton correto
            rec.setChecked(false);
            aplic.setChecked(false);
            pagamento.setText(R.string.dica_pagamento);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.VISIBLE);
            toolbarColor = ContextCompat.getColor(this, R.color.despesa_color);
            if (juros != null) juros.setVisibility(View.VISIBLE); // Juros visível para despesa
        } else if (tipoContaAtual == 1) { // Receita
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoReceita));
            rec.setChecked(true);
            desp.setChecked(false);
            aplic.setChecked(false);
            pagamento.setText(R.string.dica_recebe);
            pagamento.setVisibility(View.VISIBLE);
            categoria.setVisibility(View.GONE);
            toolbarColor = ContextCompat.getColor(this, R.color.receita_color);
            if (juros != null) juros.setVisibility(View.GONE); // Juros escondido para receita
        } else { // Aplicação (tipoContaAtual == 2)
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, res.getStringArray(R.array.TipoAplicacao));
            aplic.setChecked(true);
            rec.setChecked(false);
            desp.setChecked(false);
            pagamento.setVisibility(View.GONE); // Aplicações geralmente não têm status de pagamento
            categoria.setVisibility(View.GONE);
            toolbarColor = ContextCompat.getColor(this, R.color.aplicacao_color);
            if (juros != null) juros.setVisibility(View.VISIBLE); // Juros visível para aplicação
        }

        // Define o adaptador para a classificação da conta
        classificaConta.setAdapter(classesContasAdapter);
        // Define a seleção inicial da classe da conta
        if (classeConta >= 0 && classeConta < classesContasAdapter.getCount()) {
            classificaConta.setText(classesContasAdapter.getItem(classeConta), false);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(toolbarColor));
        }
    }

    // Helper method to set repetition spinner text
    private void setIntervaloRepeticaoTexto(int intervaloValor, ArrayAdapter<String> adapter) {
        String textoIntervalo = "";
        switch (intervaloValor) {
            case 101: // Diariamente
                if (adapter.getCount() > 0) textoIntervalo = adapter.getItem(0);
                break;
            case 107: // Semanalmente
                if (adapter.getCount() > 1) textoIntervalo = adapter.getItem(1);
                break;
            case 300: // Mensalmente
                if (adapter.getCount() > 2) textoIntervalo = adapter.getItem(2);
                break;
            case 3650: // Anualmente
                if (adapter.getCount() > 3) textoIntervalo = adapter.getItem(3);
                break;
        }
        if (!textoIntervalo.isEmpty()) {
            intervaloRepete.setText(textoIntervalo, false);
        } else {
            // Se nenhum item corresponder, limpa o texto ou define um padrão
            intervaloRepete.setText("", false);
        }
    }

    private void definirListeners() {
        pagamento.setOnClickListener(this);
        data.setOnClickListener(this);
        tipo.setOnCheckedChangeListener(this);
    }

    // This method sets individual listeners for AutoCompleteTextViews
    private void setDropdownListeners() {
        // Listener for classificaConta
        classificaConta.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                classeConta = position; // Atualiza a variável classeConta
                // Lógica para ajustar visibilidade ou valores de outros campos com base na classe selecionada
                if (tipoConta == 0) { // Despesa
                    // Exemplo: se a classe for específica para parcelamento (Cartão de Crédito)
                    if (position == 0) { // Índice 0 para "Cartão de Crédito" ou similar
                        etPrestacoes.setText(String.valueOf(1)); // Padrão 1, o usuário ajustará
                    } else {
                        // Reseta para 1 caso mude para uma classe que não é tipicamente parcelada
                        etPrestacoes.setText(String.valueOf(1));
                    }
                }
            }
        });

        // Listener for contaCategoria
        contaCategoria.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                categoriaConta = position; // Atualiza a variável categoriaConta
            }
        });

        // Listener for intervaloRepete
        intervaloRepete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Atualiza o valor do intervalo com base na seleção do spinner
                switch (position) {
                    case 0: // Diariamente
                        intervalo = 101;
                        break;
                    case 1: // Semanalmente
                        intervalo = 107;
                        break;
                    case 2: // Mensalmente
                        intervalo = 300;
                        break;
                    case 3: // Anualmente
                        intervalo = 3650;
                        break;
                    default:
                        intervalo = 300; // Padrão
                        break;
                }
            }
        });
    }


    // --- Manipuladores de Eventos (Listeners) ---

    @Override
    public void onClick(View paramView) {
        int viewId = paramView.getId();
        if (viewId == R.id.etDataConta) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (viewId == R.id.cbPagamento) {
            novoPagouConta = pagamento.isChecked() ? PAGAMENTO_PAGO : PAGAMENTO_FALTA;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // Atualiza o tipo de conta com base na seleção do RadioGroup
        if (checkedId == R.id.rDespContaModificada) {
            tipoConta = 0;
        } else if (checkedId == R.id.rRecContaModificada) {
            tipoConta = 1;
        } else if (checkedId == R.id.rAplicContaModificada) {
            tipoConta = 2;
        }
        // Reconfigura adaptadores e visibilidade de campos com base no novo tipo
        configurarAdaptadoresSpinnerETipo(tipoConta);
    }


    // --- Diálogos ---

    private void DialogoEscolhaEdicaoRepeticoes() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.dica_menu_edicao));
        alertDialogBuilder.setItems(R.array.TipoAjusteConta,
                (dialog, id) -> {
                    if (id == 0) { // Editar Somente Esta conta
                        nr = 0; // Indica que apenas a conta atual será modificada
                    } else if (id == 1) { // Editar Desta conta e futuras
                        nr = conta.getNRepete(); // Define o número da repetição atual como ponto de partida
                    } else if (id == 2) { // Editar Todas as repetições
                        nr = 1; // Indica que todas as repetições serão alteradas (começando da primeira)
                    }
                    dialogo.dismiss();
                });
        dialogo = alertDialogBuilder.create();
        dialogo.show();
    }


    // --- Lógica de Negócios e Persistência de Dados ---

    private void ConfereAlteracoesConta() {
        // 1. Nome da Conta
        novoNomeConta = nome.getText().toString().trim();
        if (novoNomeConta.isEmpty()) {
            novoNomeConta = res.getString(R.string.sem_nome);
        }

        // 2. Valor Principal
        String valorStr = valor.getText().toString().trim();
        if (!valorStr.isEmpty()) {
            try {
                // Usa replace(',', '.') para garantir o parse correto de double
                valorNovoConta = Double.parseDouble(valorStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                valorNovoConta = 0.0; // Valor inválido
                Toast.makeText(this, "Valor da conta principal inválido.", Toast.LENGTH_SHORT).show();
            }
        } else {
            valorNovoConta = 0.0;
        }

        // 3. Valor de Juros
        if (juros != null && juros.getVisibility() == View.VISIBLE) { // Verifica se o campo de juros está visível na UI
            String jurosStr = juros.getText().toString().trim();
            if (!jurosStr.isEmpty()) {
                try {
                    // Usa replace(',', '.') para garantir o parse correto de double
                    valorJurosNovo = Double.parseDouble(jurosStr.replace(',', '.')) / 100.0; // Converte para decimal
                } catch (NumberFormatException e) {
                    valorJurosNovo = 0.0;
                }
            } else {
                valorJurosNovo = 0.0;
            }
        } else {
            valorJurosNovo = 0.0; // Se o campo não estiver na tela ou não for relevante, assume zero
        }

        // 4. Repetições (Qt Prest e Intervalo)
        String prestacoesStr = etPrestacoes.getText().toString().trim();
        String intervaloText = intervaloRepete.getText().toString().trim();

        try {
            // Quantidade de Repetições
            if (!prestacoesStr.isEmpty()) {
                qtPrest = Integer.parseInt(prestacoesStr);
            } else {
                qtPrest = 1;
            }
            if (qtPrest <= 0) qtPrest = 1; // Garante que qtPrest seja pelo menos 1

            // Intervalo
            if (!intervaloText.isEmpty()) {
                intervalo = getIntervaloFromText(intervaloText);
            } else {
                if (intervalo == 0) { // Se o intervalo não foi alterado e era 0, define um padrão se qtPrest > 1
                    intervalo = (qtPrest > 1) ? 300 : 0; // Padrão Mensal se repete
                }
            }
        } catch (NumberFormatException e) {
            qtPrest = 1;
            intervalo = 300;
            Toast.makeText(this, "Valor de prestações ou intervalo inválido.", Toast.LENGTH_SHORT).show();
        }

        // 5. Ajuste para "Somente Esta Conta"
        if (nr == 0) { // Se 'nr' foi definido para 0 em DialogoEscolhaEdicaoRepeticoes()
            qtPrest = 1; // A quantidade total de repetições passa a ser 1
            intervalo = 0; // O intervalo não é mais relevante para uma única conta
        }
    }

    private int getIntervaloFromText(String text) {
        // Esta função deve ser robusta, verificando as strings do R.array.repete_conta
        String[] repeteOptions = getResources().getStringArray(R.array.repete_conta);
        if (text.equalsIgnoreCase(repeteOptions[0])) { // Diariamente
            return 101;
        } else if (text.equalsIgnoreCase(repeteOptions[1])) { // Semanalmente
            return 107;
        } else if (text.equalsIgnoreCase(repeteOptions[2])) { // Mensalmente
            return 300;
        } else if (text.equalsIgnoreCase(repeteOptions[3])) { // Anualmente
            return 3650;
        }
        return 300; // Retorna 300 (Mensal) como padrão de falha
    }


    // --- Menu de Opções ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edita_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_OK, null); // Retorna com RESULT_OK (pode indicar que não houve alteração substancial)
            finish();
            return true;
        } else if (itemId == R.id.menu_edita) {
            // 1. Captura e confere as alterações definidas pelo usuário
            ConfereAlteracoesConta();

            if (valorNovoConta == 0.0D) { // Verifica valor inválido/nulo
                Toast.makeText(this, "Valor da conta inválido.", Toast.LENGTH_SHORT).show();
                return false;
            }

            // 2. Atualiza os dados do objeto 'conta' (que será a contaBase para a atualização)
            conta.setNome(novoNomeConta);
            conta.setTipo(tipoConta);
            conta.setClasseConta(classeConta);
            conta.setCategoria(categoriaConta);
            conta.setDia(dia);
            conta.setMes(mes);
            conta.setAno(ano);
            conta.setValor(valorNovoConta);
            conta.setPagamento(novoPagouConta);
            conta.setQtRepete(qtPrest); // Nova quantidade total de repetições
            conta.setIntervalo(intervalo);
            conta.setValorJuros(valorJurosNovo); // Atualiza o juros na conta base

            // 3. Salva as informações no banco de dados
            if (nr == 0 || qtPrest <= 1) {
                // Caso 1: Conta simples (qtPrest <= 1) ou "Somente esta" (nr == 0)
                dbContaParaEditar.alteraConta(conta); // Atualiza apenas esta linha no BD

            } else {
                // Caso 2: Conta Recorrente (nr > 0 e qtPrest > 1)
                TipoAtualizacao tipoAtualizacao;

                // O valor de 'nr' é setado em DialogoEscolhaEdicaoRepeticoes():
                // 1 (Todas as repetições), ou
                // nRepete da conta (Desta em diante)
                if (nr == 1) {
                    tipoAtualizacao = TipoAtualizacao.TODAS_AS_REPETICOES;
                    // Para atualizar TODAS, a conta base deve ter nRepete=1 para o cálculo
                    conta.setNRepete(1);
                } else { // nr > 1 (Desta em diante)
                    tipoAtualizacao = TipoAtualizacao.DESTA_EM_DIANTE;
                }

                // Utiliza o método refatorado em DBContas que ATUALIZA a série
                // Nota: A BarraProgresso para essa operação não está completamente implementada
                // na classe BarraProgresso fornecida, mas o fluxo de chamada está correto.
                dbContaParaEditar.alteraContasRecorrentes(conta, tipoAtualizacao);

                // Exibe um Toast genérico para operações recorrentes
                Toast.makeText(this, getResources().getString(
                        R.string.dica_titulo_barra) + novoNomeConta + " em andamento...", Toast.LENGTH_LONG).show();
            }

            altera = 1; // Sinaliza que a conta foi alterada
            setResult(RESULT_OK, null); // Retorna com RESULT_OK
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    // --- Classes Internas ---

    /**
     * Fragmento para seleção de data usando DatePickerDialog.
     */
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Cria um DatePickerDialog com a data atual
            return new DatePickerDialog(getActivity(), this, ano, mes, dia);
        }

        // Callback quando o usuário seleciona uma data
        public void onDateSet(DatePicker view, int mAno, int mMes, int mDia) {
            // Atualiza variáveis de data estáticas
            ano = mAno;
            mes = mMes;
            dia = mDia;

            // Atualiza o texto do botão/campo de data na UI
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().getLocales().get(0);
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            EditarConta.data.setText(dataFormato.format(dateCalendar.getTime())); // Acessa 'data' estaticamente
        }
    }
}