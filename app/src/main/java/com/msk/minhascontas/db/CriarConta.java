package com.msk.minhascontas.db;

import static com.msk.minhascontas.db.DBContas.PAGAMENTO_FALTA;
import static com.msk.minhascontas.db.DBContas.PAGAMENTO_PAGO;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.msk.minhascontas.R;
import com.msk.minhascontas.info.AlertaCalendario;
import com.msk.minhascontas.info.BarraProgresso;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID; // Importado para gerar códigos únicos

public class CriarConta extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener,
        AdapterView.OnItemClickListener {

    private static final int LER_AGENDA = 444;
    // ELEMENTOS DA TELA
    private static Button dataConta;
    // VARIAVEIS UTILIZADAS
    private static int dia, mes, ano, diaRepete, mesRepete, anoRepete;
    private TextInputLayout layoutJuros; // Renomeado para clareza
    private TextInputEditText nomeConta;
    private AutoCompleteTextView classificaConta, categoriaConta, intervaloRepete;
    private AppCompatEditText repeteConta, valorConta, jurosConta; // jurosConta para o input do usuário
    private RadioGroup tipo;
    private AppCompatCheckBox parcelarConta, pagamento, lembrete;
    private LinearLayout layoutCategoria, layoutPagamento; // Renomeados para clareza
    private Resources res;
    private DBContas dbNovasContas;
    private int contaTipo, contaClasse, contaCategoria, qtRepete, intervalo, nr;
    private String contaData, contaNome, contaPaga, contaCodigo; // Adicionado contaCodigo
    private double contaValor, valorJurosDigitado; // Renomeado valorJuros para valorJurosDigitado
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cria_conta);
        res = getResources();
        dbNovasContas = DBContas.getInstance(this); // Singleton
        iniciarComponentes(); // Renomeado para clareza
        usarActionBar();
        DataDeHoje();
        configurarSpinnersECampos(); // Agrupando configurações
        definirListeners(); // Agrupando listeners
    }

    private void iniciarComponentes() {
        toolbar = findViewById(R.id.toolbar);
        valorConta = findViewById(R.id.etValorNovaConta);
        jurosConta = findViewById(R.id.etJurosNovaConta); // Input para taxa de juros
        repeteConta = findViewById(R.id.etRepeticoes);
        tipo = findViewById(R.id.rgTipoNovaConta);
        layoutCategoria = findViewById(R.id.layout_categoria);
        layoutPagamento = findViewById(R.id.layout_pagamento);
        parcelarConta = findViewById(R.id.cbValorParcelar);
        pagamento = findViewById(R.id.cbPagamento);
        lembrete = findViewById(R.id.cbLembrete);
        dataConta = findViewById(R.id.etData);
        layoutJuros = findViewById(R.id.layout_juros); // Layout que envolve o campo de juros
        classificaConta = findViewById(R.id.spClasseConta);
        categoriaConta = findViewById(R.id.spCategoriaConta);
        intervaloRepete = findViewById(R.id.spRepeticoes);
        nomeConta = findViewById(R.id.acNomeNovaConta);

        // Valores padrão
        contaTipo = 0; // Despesa por padrão
        contaClasse = 0; // Primeira classe de despesa
        contaCategoria = 7; // Categoria padrão (verificar seu R.array.CategoriaConta)
        contaPaga = PAGAMENTO_FALTA; // Começa como pendente
        intervalo = 300; // Mensal por padrão
        nr = 0; // Contador para verificação de salvamento
        contaCodigo = gerarCodigoUnico(); // Gera um código para esta conta/série
    }

    private void DataDeHoje() {
        Locale current = res.getConfiguration().getLocales().get(0);
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, current);
        Calendar c = Calendar.getInstance();
        dia = c.get(Calendar.DAY_OF_MONTH);
        mes = c.get(Calendar.MONTH); // Mês é 0-indexado
        ano = c.get(Calendar.YEAR);
        dataConta.setText(df.format(c.getTime()));
        // Para repetições, a data inicial é a mesma da data da conta
        diaRepete = dia;
        mesRepete = mes;
        anoRepete = ano;
    }

    private void configurarSpinnersECampos() {
        // Configura a exibição do campo de juros e pagamentos com base no tipo de conta
        onCheckedChanged(tipo, tipo.getCheckedRadioButtonId()); // Chama uma vez para configurar o estado inicial

        // Configura os adaptadores para os Spinners (AutoCompleteTextViews)
        configurarAdaptadoresSpinner();

        // Define valores padrão para campos que podem ser vazios ou ter lógica própria
        if (repeteConta.getText().toString().isEmpty()) {
            qtRepete = 1; // Se não especificado, conta como uma única parcela
        } else {
            qtRepete = Integer.parseInt(repeteConta.getText().toString());
        }
        // Juros será lido no momento de salvar, para evitar parsear um valor vazio
    }

    private void configurarAdaptadoresSpinner() {
        ArrayAdapter<String> classesContasAdapter;
        ArrayAdapter<String> categoriasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.CategoriaConta));
        ArrayAdapter<String> intervalosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.repete_conta));

        // Configura o adapter para a classificação da conta com base no tipo
        if (contaTipo == 1) { // Receita
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.TipoReceita));
            contaClasse = 0; // Receita sempre tem classe 0 (ou verificar seu array)
        } else if (contaTipo == 2) { // Aplicação
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.TipoAplicacao));
            contaClasse = 0; // Aplicação pode ter classe 0 (ou verificar seu array)
        } else { // Despesa
            classesContasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.TipoDespesa));
            contaClasse = 0; // Despesa, classe inicial
        }

        classificaConta.setAdapter(classesContasAdapter);
        // Define a seleção inicial da classe da conta
        if (!classesContasAdapter.isEmpty()) {
            classificaConta.setText(classesContasAdapter.getItem(contaClasse), false);
        }

        categoriaConta.setAdapter(categoriasAdapter);
        // Define a seleção inicial da categoria
        if (!categoriasAdapter.isEmpty() && contaCategoria < categoriasAdapter.getCount()) {
            categoriaConta.setText(categoriasAdapter.getItem(contaCategoria), false);
        }

        intervaloRepete.setAdapter(intervalosAdapter);
        // Define a seleção inicial do intervalo de repetição
        if (!intervalosAdapter.isEmpty() && intervaloCorrespondePosicao(intervalo, intervalosAdapter) != -1) {
            intervaloRepete.setText(intervalosAdapter.getItem(intervaloCorrespondePosicao(intervalo, intervalosAdapter)), false);
        } else {
            // Se o intervalo não corresponder a nenhum item, define o padrão (Mensal = 300)
            intervaloRepete.setText(intervalosAdapter.getItem(2), false); // Mensal é geralmente o índice 2
            intervalo = 300;
        }
    }

    // Helper para encontrar o índice correto do intervalo no adapter
    private int intervaloCorrespondePosicao(int intervalo, ArrayAdapter<String> adapter) {
        if (intervalo == 101 && adapter.getPosition("Diariamente") != -1) return adapter.getPosition("Diariamente");
        if (intervalo == 107 && adapter.getPosition("Semanalmente") != -1) return adapter.getPosition("Semanalmente");
        if (intervalo == 300 && adapter.getPosition("Mensalmente") != -1) return adapter.getPosition("Mensalmente");
        if (intervalo == 3650 && adapter.getPosition("Anualmente") != -1) return adapter.getPosition("Anualmente");
        return -1; // Não encontrado
    }


    private void definirListeners() {
        tipo.setOnCheckedChangeListener(this);
        dataConta.setOnClickListener(this);
        pagamento.setOnClickListener(this);
        classificaConta.setOnItemClickListener(this);
        categoriaConta.setOnItemClickListener(this);
        intervaloRepete.setOnItemClickListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rDespNovaConta) {
            contaTipo = 0;
            configurarVisibilidadeCampos(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE); // Nome, Pagamento, Categoria, Juros
            pagamento.setText(R.string.dica_pagamento);
            atualizarClasseEJanelaJuros();
        } else if (checkedId == R.id.rRecNovaConta) {
            contaTipo = 1;
            configurarVisibilidadeCampos(View.VISIBLE, View.VISIBLE, View.GONE, View.GONE); // Nome, Pagamento, sem Categoria, sem Juros
            pagamento.setText(R.string.dica_recebe);
            atualizarClasseEJanelaJuros();
        } else if (checkedId == R.id.rAplicNovaConta) {
            contaTipo = 2;
            configurarVisibilidadeCampos(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE); // Nome, sem Pagamento, sem Categoria, Juros
            atualizarClasseEJanelaJuros();
        }
        // Reconfigura os spinners após mudar o tipo, caso a classe ou categoria mude
        configurarAdaptadoresSpinner();
    }

    private void configurarVisibilidadeCampos(int visibilidadeNome, int visibilidadePagamento, int visibilidadeCategoria, int visibilidadeJuros) {
        // O campo de nome (nomeConta) geralmente é sempre visível
        nomeConta.setVisibility(visibilidadeNome); // Mantendo nomeConta sempre visível
        layoutPagamento.setVisibility(visibilidadePagamento);
        layoutCategoria.setVisibility(visibilidadeCategoria);
        layoutJuros.setVisibility(visibilidadeJuros); // Layout que engloba jurosConta
    }

    private void atualizarClasseEJanelaJuros() {
        // Atualiza a classe da conta com base no tipo selecionado
        int indiceClasseInicial = 0;
        ArrayAdapter<String> classesAdapter = (ArrayAdapter<String>) classificaConta.getAdapter();
        if (classesAdapter != null) {
            if (contaTipo == 0) { // Despesa
                classesAdapter.setNotifyOnChange(true); // Notifica para reexibir se necessário
                classesAdapter.clear();
                classesAdapter.addAll(getResources().getStringArray(R.array.TipoDespesa));
                classesAdapter.notifyDataSetChanged();
                indiceClasseInicial = 0; // Classe inicial para despesa
                layoutJuros.setVisibility(View.VISIBLE); // Juros é relevante para despesas
                if (parcelarConta.isChecked() || contaClasse == 0 || contaClasse == 3) { // Lógica simplificada para exibir juros dependendo da classe
                    // Adapte esta lógica conforme sua necessidade
                }
            } else if (contaTipo == 1) { // Receita
                classesAdapter.setNotifyOnChange(true);
                classesAdapter.clear();
                classesAdapter.addAll(getResources().getStringArray(R.array.TipoReceita));
                classesAdapter.notifyDataSetChanged();
                indiceClasseInicial = 0; // Classe inicial para receita
                layoutJuros.setVisibility(View.GONE);
            } else { // Aplicação
                classesAdapter.setNotifyOnChange(true);
                classesAdapter.clear();
                classesAdapter.addAll(getResources().getStringArray(R.array.TipoAplicacao));
                classesAdapter.notifyDataSetChanged();
                indiceClasseInicial = 0; // Classe inicial para aplicação
                layoutJuros.setVisibility(View.VISIBLE); // Juros pode ser relevante para aplicações
            }
            // Atualiza a seleção da classe da conta
            if (!classesAdapter.isEmpty() && indiceClasseInicial < classesAdapter.getCount()) {
                classificaConta.setText(classesAdapter.getItem(indiceClasseInicial), false);
                contaClasse = indiceClasseInicial; // Atualiza a variável interna
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.etData) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (id == R.id.cbPagamento) {
            if (pagamento.isChecked()) {
                contaPaga = PAGAMENTO_PAGO;
            } else {
                contaPaga = PAGAMENTO_FALTA;
            }
        }
    }

    // Método para confirmar os dados e preparar para salvar no banco
    private void ConfereDadosConta() {
        contaNome = nomeConta.getText().toString().trim();
        if (contaNome.isEmpty()) {
            contaNome = res.getString(R.string.sem_nome); // Use uma string de recurso para "Nome Inválido"
        }

        String valorStr = valorConta.getText().toString().trim();
        if (!valorStr.isEmpty()) {
            try {
                contaValor = Double.parseDouble(valorStr);
            } catch (NumberFormatException e) {
                contaValor = 0.0; // Valor inválido
            }
        } else {
            contaValor = 0.0;
        }

        String jurosStr = jurosConta.getText().toString().trim();
        if (!jurosStr.isEmpty()) {
            try {
                valorJurosDigitado = Double.parseDouble(jurosStr);
                // Converte a porcentagem digitada para decimal (ex: 5.0 para 0.05)
                valorJurosDigitado = valorJurosDigitado / 100.0;
            } catch (NumberFormatException e) {
                valorJurosDigitado = 0.0; // Juros inválido
            }
        } else {
            valorJurosDigitado = 0.0; // Se o campo estiver vazio, juros é zero
        }

        String repeteStr = repeteConta.getText().toString().trim();
        if (!repeteStr.isEmpty()) {
            try {
                qtRepete = Integer.parseInt(repeteStr);
            } catch (NumberFormatException e) {
                qtRepete = 1; // Se inválido, conta como uma única parcela
            }
        } else {
            qtRepete = 1; // Se vazio, conta como uma única parcela
        }

        // O intervalo e a categoria já são definidos nos listeners dos Spinners
        // contaCategoria é definida em onItemClick para categoriaConta
        // intervalo é definido em onItemClick para intervaloRepete
    }

    // Método para armazena os dados da conta no banco de dados
    private void ArmazenaDadosConta() {
        // Cria o objeto Conta com todos os dados coletados
        // O código único será usado para agrupar repetições
        Conta novaConta = new Conta(contaNome, contaTipo, contaClasse, contaCategoria, dia, mes, ano, contaValor, contaPaga, qtRepete, 1, intervalo, contaCodigo, valorJurosDigitado);

        if (qtRepete <= 1) { // Se for uma conta única ou sem repetições
            dbNovasContas.insertConta(novaConta);
        } else {
            dbNovasContas.insertContasRecorrentes(novaConta, qtRepete, intervalo);
        }
        nr = 1; // Sinaliza que a conta foi salva
    }


    private void AdicionaLembrete() {
        // Verifica permissões para adicionar evento ao calendário
        int permEscrever = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        int permLer = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);

        if (permEscrever != PackageManager.PERMISSION_GRANTED && permLer != PackageManager.PERMISSION_GRANTED) {
            // Solicita permissões se não as tiver
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, LER_AGENDA);
        } else {
            // Se já tiver permissão, adiciona o evento
            AlertaCalendario.adicionarEventoNoCalendario(
                    getContentResolver(),
                    this.getString(R.string.dica_evento, contaNome), // Título do evento
                    // Descrição do evento pode incluir o valor da conta
                    this.getString(R.string.dica_calendario, String.format(Locale.US, "%.2f", contaValor)),
                    dia, mes, ano, // Data do evento
                    true, // Evento recorrente (ajustar se necessário)
                    qtRepete, // Quantas vezes repetir
                    intervalo // Intervalo de repetição
            );
            setResult(RESULT_OK, null); // Informa que a operação foi bem sucedida
            finish(); // Fecha a activity
        }
    }

    // Callback para resultado da solicitação de permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LER_AGENDA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, agora adiciona o evento
                AlertaCalendario.adicionarEventoNoCalendario(
                        getContentResolver(),
                        this.getString(R.string.dica_evento, contaNome),
                        this.getString(R.string.dica_calendario, String.format(Locale.US, "%.2f", contaValor)),
                        dia, mes, ano, true, qtRepete, intervalo
                );
                setResult(RESULT_OK, null);
                finish();
            } else {
                // Permissão negada, informa o usuário
                Toast.makeText(this, "Permissão para acessar o calendário negada. Lembrete não será configurado.", Toast.LENGTH_LONG).show();
                finish(); // Fecha a activity mesmo se a permissão for negada
            }
        }
    }

    // Criação da aplicação (caixa de diálogo)
    private void CriaAplicacao() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_despesa_saque) // Título da caixa de diálogo
                .setMessage(R.string.texto_despesa_saque) // Mensagem da caixa de diálogo
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> {
                            // Cria a conta como Despesa (tipo 0)
                            Conta contaAplicacao = new Conta(contaNome, 0, 1, 7, dia, mes, ano, contaValor, contaPaga, qtRepete, 1, intervalo, contaCodigo, valorJurosDigitado); // Classe 1, Categoria 7, etc.
                            dbNovasContas.insertConta(contaAplicacao);
                            if (lembrete.isChecked()) {
                                AdicionaLembrete(); // Se lembrete estiver marcado, adiciona
                            } else {
                                finish(); // Senão, finaliza
                            }
                        })
                .setNegativeButton(R.string.cancelar,
                        (dialog, which) -> {
                            dialog.dismiss(); // Fecha a caixa de diálogo
                            if (lembrete.isChecked()) {
                                AdicionaLembrete(); // Se lembrete estiver marcado, adiciona
                            } else {
                                finish(); // Senão, finaliza
                            }
                        }).show();
    }

    // Listener para seleção nos Spinners (AutoCompleteTextViews)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int parentId = parent.getId();
        if (parentId == R.id.spClasseConta) {
            contaClasse = position; // Atualiza a variável de classe da conta
            // Lógica adicional pode ser necessária aqui dependendo da classe selecionada
            // Por exemplo, habilitar/desabilitar parcelamento ou juros
            if (contaTipo == 0) { // Despesa
                if (position == 0 || position == 3) { // Se for uma classe específica de despesa (ex: Cartão de Crédito, Financiamento)
                    parcelarConta.setVisibility(View.VISIBLE);
                    layoutJuros.setVisibility(View.VISIBLE); // Exibe juros se aplicável à classe
                } else {
                    parcelarConta.setVisibility(View.GONE);
                    layoutJuros.setVisibility(View.VISIBLE); // Mantém juros visível para outras despesas se necessário
                }
                // Se a classe for outra (ex: Aluguel, Salário), pode não ter juros ou parcelamento
            } else if (contaTipo == 2) { // Aplicação
                layoutJuros.setVisibility(View.VISIBLE); // Juros é relevante para aplicações
            }
            parcelarConta.setChecked(false); // Reseta o checkbox de parcelamento ao mudar a classe
        } else if (parentId == R.id.spCategoriaConta) {
            contaCategoria = position; // Atualiza a variável de categoria da conta
        } else if (parentId == R.id.spRepeticoes) {
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
            }
        }
    }

    // Criação do menu de opções na ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cria_conta, menu);
        return true;
    }

    // Tratamento dos itens selecionados no menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { // Botão "Voltar" na ActionBar
            setResult(RESULT_OK, null); // Opcional: pode indicar sucesso ou falha para a activity que chamou
            finish(); // Fecha a activity
            return true;
        } else if (id == R.id.menu_cria) { // Item "Salvar" (ou similar) do menu
            ConfereDadosConta(); // Confere e valida os dados digitados pelo usuário
            ArmazenaDadosConta(); // Salva os dados no banco de dados

            // Se a conta foi salva com sucesso (nr = 1), mostra progresso se houver repetições
            if (nr == 1) {
                if (qtRepete > 1) {
                    // Exibe uma barra de progresso se a conta tiver repetições
                    new BarraProgresso(this, getResources().getString(
                            R.string.dica_titulo_barra), getResources().getString(
                            R.string.dica_barra_progresso), qtRepete, 0, "mskapp").execute(); // Parâmetros do BarraProgresso precisam ser verificados
                }

                // Processa o lembrete ou finaliza a activity
                if (contaTipo == 2) { // Se for uma Aplicação
                    CriaAplicacao(); // Mostra a caixa de diálogo específica para aplicações
                } else { // Se for Despesa ou Receita
                    if (lembrete.isChecked()) {
                        AdicionaLembrete(); // Tenta adicionar um lembrete no calendário
                    } else {
                        finish(); // Se não houver lembrete, apenas finaliza
                    }
                }
            } else {
                // Se a conta não foi salva (nr != 1), mostra um aviso
                Toast.makeText(this, "Erro ao salvar a conta.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item); // Para outros itens de menu não tratados
    }

    // Configura a ActionBar
    private void usarActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.titulo_criar);
    }

    // Finaliza a activity e mostra um Toast de confirmação
    @Override
    protected void onDestroy() {
        if (nr == 1) { // Se a conta foi salva com sucesso
            Toast.makeText(
                    getApplicationContext(),
                    String.format(
                            getResources().getString(
                                    R.string.dica_conta_criada),
                            contaNome), Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK, null); // Opcional: retornar RESULT_OK para a activity que chamou
        super.onDestroy();
    }

    // Fragmento para o DatePicker
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Cria um DatePickerDialog com a data atual
            return new DatePickerDialog(getActivity(), this, ano, mes, dia);
        }

        // Callback quando o usuário seleciona uma data
        public void onDateSet(DatePicker view, int mAno, int mMes, int mDia) {
            // Atualiza as variáveis globais de data
            ano = mAno;
            mes = mMes; // Mês é 0-indexado no Calendar, mas DatePickerDialog retorna corretamente
            dia = mDia;

            // Atualiza a exibição do botão/campo de data
            Calendar data = Calendar.getInstance();
            data.set(ano, mes, dia);
            Locale current = getActivity().getResources().getConfiguration().getLocales().get(0);
            DateFormat dataFormato = DateFormat.getDateInstance(DateFormat.SHORT, current);
            dataConta.setText(dataFormato.format(data.getTime()));

            // Atualiza as datas de repetição iniciais
            diaRepete = dia;
            mesRepete = mes;
            anoRepete = ano;
        }
    }

    // Helper para gerar um código único para a série de contas
    private String gerarCodigoUnico() {
        return UUID.randomUUID().toString();
    }
}