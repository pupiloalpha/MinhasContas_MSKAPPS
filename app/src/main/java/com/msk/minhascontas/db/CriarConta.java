package com.msk.minhascontas.db;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.msk.minhascontas.R;
import com.msk.minhascontas.info.AlertaCalendario;
import com.msk.minhascontas.tarefas.BarraProgresso;
import com.msk.minhascontas.MinhasContas;

import static com.msk.minhascontas.db.DBContas.PAGAMENTO_FALTA;
import static com.msk.minhascontas.db.DBContas.PAGAMENTO_PAGO;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class CriarConta extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    // --- Constantes ---
    private static final int LER_AGENDA = 444;
    public static final String EXTRA_RETURN_POSITION = "nrLista"; // Não usado diretamente aqui, mas pode ser útil para outras classes.

    // --- Variáveis Membro ---
    private BarraProgresso mProgressTask; // Tarefa de progresso (se for usada para salvar)
    private int nrPaginaRecebida = MinhasContas.START_PAGE; // Posição do ViewPager da MinhasContas
    private static Button dataConta; // Botão do seletor de data
    private static int dia, mes, ano, diaRepete, mesRepete, anoRepete; // Datas para a conta
    private TextInputLayout layoutJuros; // Layout para o campo de juros
    private TextInputEditText nomeConta; // Campo de texto para o nome da conta
    private AutoCompleteTextView classificaConta, categoriaConta, intervaloRepete; // Spinners/AutoComplete para classificação, categoria e intervalo de repetição
    private AppCompatEditText repeteConta, valorConta, jurosConta; // Campos de texto para repetições, valor e juros
    private RadioGroup tipo; // Grupo de rádio para tipo de conta (Despesa, Receita, Aplicação)
    private AppCompatCheckBox parcelarConta, pagamento, lembrete; // Checkboxes para parcelar, status de pagamento e lembrete
    private LinearLayout layoutCategoria, layoutPagamento; // Layouts para visibilidade condicional
    private Resources res; // Recursos da aplicação
    private DBContas dbNovasContas; // Instância do banco de dados
    private int contaTipo, contaClasse, contaCategoria, qtRepete, intervalo, nr; // Valores numéricos da conta
    private String contaData, contaNome, contaPaga, contaCodigo; // Valores em String da conta
    private double contaValor, valorJurosDigitado; // Valores decimais da conta
    private MaterialToolbar toolbar; // Toolbar da Activity


    // --- Métodos de Ciclo de Vida da Activity ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cria_conta);
        res = getResources();
        dbNovasContas = DBContas.getInstance(this);

        iniciarComponentes();
        usarActionBar();
        configurarComponentesIniciais(); // Agrupa métodos de configuração
        definirListeners();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nrPaginaRecebida = extras.getInt(MinhasContas.KEY_PAGINA, MinhasContas.START_PAGE);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    protected void onDestroy() {
        if (mProgressTask != null && mProgressTask.getStatus() == BarraProgresso.Status.RUNNING) {
            mProgressTask.cancel(true);
        }
        if (nr == 1) { // Verifica se uma conta foi criada/salva
            Toast.makeText(
                    getApplicationContext(),
                    String.format(
                            getResources().getString(
                                    R.string.dica_conta_criada),
                            contaNome), Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        // Retorna à Activity anterior informando que a operação foi cancelada
        finishActivityWithCancellation();
        super.onBackPressed();
    }


    // --- Inicialização da UI e Dados ---

    private void iniciarComponentes() {
        toolbar = findViewById(R.id.toolbar);
        valorConta = findViewById(R.id.etValorNovaConta);
        jurosConta = findViewById(R.id.etJurosNovaConta);
        repeteConta = findViewById(R.id.etRepeticoes);
        tipo = findViewById(R.id.rgTipoNovaConta);
        layoutCategoria = findViewById(R.id.layout_categoria);
        layoutPagamento = findViewById(R.id.layout_pagamento);
        parcelarConta = findViewById(R.id.cbValorParcelar);
        pagamento = findViewById(R.id.cbPagamento);
        lembrete = findViewById(R.id.cbLembrete);
        dataConta = findViewById(R.id.etData);
        layoutJuros = findViewById(R.id.layout_juros);
        classificaConta = findViewById(R.id.spClasseConta);
        categoriaConta = findViewById(R.id.spCategoriaConta);
        intervaloRepete = findViewById(R.id.spRepeticoes);
        nomeConta = findViewById(R.id.acNomeNovaConta);

        contaTipo = 0; // Padrão: Despesa
        contaClasse = 0; // Padrão: Primeira opção da classe
        contaCategoria = 7; // Padrão: Outros
        contaPaga = PAGAMENTO_FALTA; // Padrão: Não paga
        intervalo = 300; // Padrão: Mensal
        nr = 0; // Contador de contas salvas
        contaCodigo = gerarCodigoUnico(); // Gera código único para a conta
    }

    private void usarActionBar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(R.string.titulo_criar);
        }
    }

    private void configurarComponentesIniciais() {
        DataDeHoje(); // Define a data inicial
        configurarSpinnersECampos(); // Configura adaptadores e valores iniciais dos spinners
    }

    private void DataDeHoje() {
        Locale current = res.getConfiguration().getLocales().get(0);
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, current);
        Calendar c = Calendar.getInstance();
        dia = c.get(Calendar.DAY_OF_MONTH);
        mes = c.get(Calendar.MONTH);
        ano = c.get(Calendar.YEAR);
        dataConta.setText(df.format(c.getTime()));
        diaRepete = dia;
        mesRepete = mes;
        anoRepete = ano;
    }

    private void configurarSpinnersECampos() {
        onCheckedChanged(tipo, tipo.getCheckedRadioButtonId()); // Define o estado inicial com base no RadioGroup
        configurarAdaptadoresSpinner();

        if (repeteConta.getText().toString().isEmpty()) {
            qtRepete = 1;
        } else {
            qtRepete = Integer.parseInt(repeteConta.getText().toString());
        }
    }

    private void configurarAdaptadoresSpinner() {
        ArrayAdapter<String> classesContasAdapter;
        ArrayAdapter<String> categoriasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.CategoriaConta));
        ArrayAdapter<String> intervalosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.repete_conta));

        // Determina os dados iniciais do spinner de classificação e a cor da toolbar
        String[] dadosIniciais;
        int toolbarColor;
        if (contaTipo == 1) { // Receita
            dadosIniciais = getResources().getStringArray(R.array.TipoReceita);
            toolbarColor = ContextCompat.getColor(this, R.color.receita_color);
        } else if (contaTipo == 2) { // Aplicação
            dadosIniciais = getResources().getStringArray(R.array.TipoAplicacao);
            toolbarColor = ContextCompat.getColor(this, R.color.aplicacao_color);
        } else { // Despesa
            dadosIniciais = getResources().getStringArray(R.array.TipoDespesa);
            toolbarColor = ContextCompat.getColor(this, R.color.despesa_color);
        }

        classesContasAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new java.util.ArrayList<>(java.util.Arrays.asList(dadosIniciais))
        );

        classificaConta.setAdapter(classesContasAdapter);
        if (!classesContasAdapter.isEmpty()) {
            classificaConta.setText(classesContasAdapter.getItem(contaClasse), false);
        }

        categoriaConta.setAdapter(categoriasAdapter);
        if (!categoriasAdapter.isEmpty() && contaCategoria < categoriasAdapter.getCount()) {
            categoriaConta.setText(categoriasAdapter.getItem(contaCategoria), false);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(toolbarColor));
        }
        intervaloRepete.setAdapter(intervalosAdapter);
    }

    private void definirListeners() {
        tipo.setOnCheckedChangeListener(this);
        dataConta.setOnClickListener(this);
        pagamento.setOnClickListener(this);

        classificaConta.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                contaClasse = position; // Atualiza a variável de classe da conta
                atualizarVisibilidadeJurosEPacelamento(position);
            }
        });

        categoriaConta.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                contaCategoria = position; // Atualiza a variável de categoria da conta
            }
        });

        intervaloRepete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Define o intervalo com base na seleção do usuário
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
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rDespNovaConta) {
            contaTipo = 0; // Despesa
            configurarVisibilidadeCampos(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
            pagamento.setText(R.string.dica_pagamento);
            atualizarClasseEJanelaJuros();
        } else if (checkedId == R.id.rRecNovaConta) {
            contaTipo = 1; // Receita
            configurarVisibilidadeCampos(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE);
            pagamento.setText(R.string.dica_recebe);
            atualizarClasseEJanelaJuros();
        } else if (checkedId == R.id.rAplicNovaConta) {
            contaTipo = 2; // Aplicação
            configurarVisibilidadeCampos(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE);
            atualizarClasseEJanelaJuros();
        }
        configurarAdaptadoresSpinner(); // Reconfigura adaptadores e cores da toolbar
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.etData) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (id == R.id.cbPagamento) {
            contaPaga = pagamento.isChecked() ? PAGAMENTO_PAGO : PAGAMENTO_FALTA;
        }
    }


    // --- Atualizações da UI ---

    private void configurarVisibilidadeCampos(int visibilidadeNome, int visibilidadePagamento, int visibilidadeCategoria, int visibilidadeJuros) {
        nomeConta.setVisibility(visibilidadeNome);
        layoutPagamento.setVisibility(visibilidadePagamento);
        layoutCategoria.setVisibility(visibilidadeCategoria);
        layoutJuros.setVisibility(visibilidadeJuros);
    }

    private void atualizarClasseEJanelaJuros() {
        int indiceClasseInicial = 0;
        ArrayAdapter<String> classesAdapter = (ArrayAdapter<String>) classificaConta.getAdapter();

        if (classesAdapter != null) {
            String[] novosDados;

            if (contaTipo == 0) { // Despesa
                novosDados = getResources().getStringArray(R.array.TipoDespesa);
                layoutJuros.setVisibility(View.VISIBLE);
            } else if (contaTipo == 1) { // Receita
                novosDados = getResources().getStringArray(R.array.TipoReceita);
                layoutJuros.setVisibility(View.GONE);
            } else { // Aplicação
                novosDados = getResources().getStringArray(R.array.TipoAplicacao);
                layoutJuros.setVisibility(View.VISIBLE);
            }

            classesAdapter.clear();
            classesAdapter.addAll(novosDados);
            classesAdapter.notifyDataSetChanged();

            if (!classesAdapter.isEmpty() && indiceClasseInicial < classesAdapter.getCount()) {
                classificaConta.setText(classesAdapter.getItem(indiceClasseInicial), false);
                contaClasse = indiceClasseInicial;
            }
            atualizarVisibilidadeJurosEPacelamento(contaClasse); // Garante que a visibilidade esteja correta para a nova classe
        }
    }

    private void atualizarVisibilidadeJurosEPacelamento(int classeSelecionada) {
        if (contaTipo == 0) { // Despesa
            if (classeSelecionada == 0 || classeSelecionada == 3) { // Ex: Cartão de Crédito, Financiamento
                parcelarConta.setVisibility(View.VISIBLE);
                layoutJuros.setVisibility(View.VISIBLE);
            } else {
                parcelarConta.setVisibility(View.GONE);
                layoutJuros.setVisibility(View.VISIBLE); // Mantém juros visível para outras despesas se necessário
            }
        } else if (contaTipo == 1) { // Receita
            parcelarConta.setVisibility(View.GONE);
            layoutJuros.setVisibility(View.GONE);
        } else if (contaTipo == 2) { // Aplicação
            parcelarConta.setVisibility(View.GONE); // Aplicações não são "parceladas" no mesmo sentido
            layoutJuros.setVisibility(View.VISIBLE); // Juros é relevante para aplicações
        }
        parcelarConta.setChecked(false); // Reseta o checkbox de parcelamento ao mudar a classe/tipo
    }


    // --- Lógica de Negócios e Persistência de Dados ---

    private void ConfereDadosConta() {
        contaNome = nomeConta.getText().toString().trim();
        if (contaNome.isEmpty()) {
            contaNome = res.getString(R.string.sem_nome);
        }

        String valorStr = valorConta.getText().toString().trim();
        try {
            contaValor = valorStr.isEmpty() ? 0.0 : Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            contaValor = 0.0;
            Toast.makeText(this, R.string.erro_valor_invalido, Toast.LENGTH_SHORT).show();
            return; // Impede o salvamento com valor inválido
        }

        String jurosStr = jurosConta.getText().toString().trim();
        try {
            valorJurosDigitado = jurosStr.isEmpty() ? 0.0 : Double.parseDouble(jurosStr) / 100.0; // Converte para decimal
        } catch (NumberFormatException e) {
            valorJurosDigitado = 0.0;
            Toast.makeText(this, R.string.erro_valor_invalido, Toast.LENGTH_SHORT).show();
            return; // Impede o salvamento com juros inválido
        }

        String repeteStr = repeteConta.getText().toString().trim();
        String intervaloText = intervaloRepete.getText().toString().trim();

        try {
            qtRepete = repeteStr.isEmpty() ? 1 : Integer.parseInt(repeteStr);
            if (qtRepete <= 0) qtRepete = 1; // Garante que qtRepete seja pelo menos 1
        } catch (NumberFormatException e) {
            qtRepete = 1;
        }

        if (!intervaloText.isEmpty()) {
            intervalo = getIntervaloFromText(intervaloText);
        } else if (qtRepete > 1) {
            intervalo = 300; // Padrão mensal se repete, mas nenhum intervalo foi selecionado
        } else {
            intervalo = 0; // Se não repete, intervalo não é relevante
        }
    }

    private int getIntervaloFromText(String text) {
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
        return 300; // Padrão
    }

    private void ArmazenaDadosConta() {
        Conta novaConta = new Conta.Builder(
                contaNome,
                contaValor,
                dia,
                mes,
                ano,
                contaCodigo
        )
                .setTipo(contaTipo)
                .setClasseConta(contaClasse)
                .setCategoria(contaCategoria)
                .setPagamento(contaPaga)
                .setQtRepete(qtRepete)
                .setNRepete(1) // Sempre começa com a primeira repetição
                .setIntervalo(intervalo)
                .setValorJuros(valorJurosDigitado)
                .build();

        if (qtRepete <= 1) {
            dbNovasContas.geraConta(novaConta);
        } else {
            dbNovasContas.geraContasRecorrentes(novaConta, qtRepete, intervalo);
        }
        nr = 1; // Indica que a conta foi salva
    }

    private void CriaAplicacao() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_despesa_saque)
                .setMessage(R.string.texto_despesa_saque)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> {
                            // Cria uma conta de DESPESA para o saque/investimento
                            Conta contaAplicacaoSaque = new Conta.Builder(
                                    contaNome,
                                    contaValor,
                                    dia,
                                    mes,
                                    ano,
                                    gerarCodigoUnico() // Novo código para a despesa
                            )
                                    .setTipo(0) // Tipo Despesa
                                    .setClasseConta(1) // Classe de conta "Saque" ou "Investimento" (depende da array)
                                    .setCategoria(7) // Categoria "Outros"
                                    .setPagamento(PAGAMENTO_PAGO) // Saque é pago imediatamente
                                    .setQtRepete(1) // Geralmente não repete
                                    .setNRepete(1)
                                    .setIntervalo(0)
                                    .setValorJuros(0.0)
                                    .build();

                            dbNovasContas.geraConta(contaAplicacaoSaque); // Salva a despesa
                            ArmazenaDadosConta(); // Salva a conta de aplicação original
                            nr = 1; // Indica que pelo menos uma conta foi salva

                            if (lembrete.isChecked()) {
                                AdicionaLembrete();
                            } else {
                                finishActivityWithPosition();
                            }
                        })
                .setNegativeButton(R.string.cancelar,
                        (dialog, which) -> {
                            ArmazenaDadosConta(); // Apenas salva a conta de aplicação
                            dialog.dismiss();
                            // Se o usuário cancelar a despesa, a aplicação ainda é criada.
                            // Mas se a ideia é cancelar tudo, talvez não deveria chamar ArmazenaDadosConta() aqui.
                            // Por enquanto, segue a lógica existente de salvar a aplicação mesmo com cancelamento.
                            if (lembrete.isChecked()) {
                                AdicionaLembrete();
                            } else {
                                finishActivityWithPosition();
                            }
                        }).show();
    }


    // --- Lógica de Calendário e Lembretes ---

    private void AdicionaLembrete() {
        int permEscrever = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        int permLer = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);

        if (permEscrever != PackageManager.PERMISSION_GRANTED || permLer != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, LER_AGENDA);
        } else {
            AlertaCalendario.adicionarEventoNoCalendario(
                    getContentResolver(),
                    this.getString(R.string.dica_evento, contaNome),
                    this.getString(R.string.dica_calendario, String.format(Locale.US, "%.2f", contaValor)),
                    dia, mes, ano,
                    true, // isAllDay
                    qtRepete,
                    intervalo
            );
            finishActivityWithPosition();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LER_AGENDA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, adiciona o lembrete
                AlertaCalendario.adicionarEventoNoCalendario(
                        getContentResolver(),
                        this.getString(R.string.dica_evento, contaNome),
                        this.getString(R.string.dica_calendario, String.format(Locale.US, "%.2f", contaValor)),
                        dia, mes, ano, true, qtRepete, intervalo
                );
                finishActivityWithPosition();
            } else {
                // Permissão negada
                Toast.makeText(this, "Permissão para acessar o calendário negada. Lembrete não será configurado.", Toast.LENGTH_LONG).show();
                // Finaliza a activity, sem criar lembrete
                if (nr == 1) { // Se a conta foi salva, retorna com sucesso
                    finishActivityWithPosition();
                } else { // Caso contrário, retorna com cancelamento (embora 'nr' deveria ser 1 aqui se chegou a tentar salvar)
                    finishActivityWithCancellation();
                }
            }
        }
    }


    // --- Navegação e Retorno ---

    private void finishActivityWithPosition() {
        Intent intent = new Intent();
        intent.putExtra(MinhasContas.RETURN_KEY_PAGINA, nrPaginaRecebida);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishActivityWithCancellation() {
        Intent intent = new Intent();
        intent.putExtra(MinhasContas.RETURN_KEY_PAGINA, nrPaginaRecebida);
        setResult(RESULT_CANCELED, intent);
        finish();
    }


    // --- Menu de Opções ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cria_conta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finishActivityWithCancellation(); // Retorna sem salvar a conta
            return true;
        } else if (id == R.id.menu_cria) {
            ConfereDadosConta(); // Valida os dados antes de salvar

            // Se ConfereDadosConta() encontrou um erro e exibiu um Toast,
            // podemos adicionar uma verificação aqui para não prosseguir.
            // Por simplicidade, assumimos que se um Toast foi exibido, o usuário verá
            // e não continuará a operação até corrigir.
            // Para maior robustez, ConfereDadosConta() poderia retornar um boolean.

            if (contaTipo == 2) { // Se for uma aplicação, exibe diálogo adicional
                CriaAplicacao();
            } else {
                ArmazenaDadosConta(); // Salva a conta
                if (nr == 1) { // Verifica se a conta foi salva com sucesso
                    if (lembrete.isChecked()) {
                        AdicionaLembrete(); // Adiciona lembrete se marcado
                    } else {
                        finishActivityWithPosition(); // Finaliza a activity
                    }
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // --- Métodos Auxiliares/Utilitários ---

    private String gerarCodigoUnico() {
        return UUID.randomUUID().toString();
    }


    // --- Classes Internas ---

    /**
     * Fragmento para seleção de data usando DatePickerDialog.
     */
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Cria uma nova instância de DatePickerDialog com a data atual como padrão
            return new DatePickerDialog(getActivity(), this, ano, mes, dia);
        }

        public void onDateSet(DatePicker view, int mAno, int mMes, int mDia) {
            ano = mAno;
            mes = mMes;
            dia = mDia;

            Calendar data = Calendar.getInstance();
            data.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().getLocales().get(0);
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            dataConta.setText(dataFormato.format(data.getTime()));

            // Atualiza as datas de repetição também (podem ser alteradas mais tarde)
            diaRepete = dia;
            mesRepete = mes;
            anoRepete = ano;
        }
    }
}