package com.msk.minhascontas.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import android.util.Log; // Adicionado para depuração

public final class DBContas {

    private static final String BANCO_DE_DADOS = "minhas_contas";
    // --- ATUALIZE ESTA VERSÃO AO FAZER ALTERAÇÕES ESTRUTURAIS NO BANCO ---
    private static final int VERSAO_BANCO_DE_DADOS = 6; // Incrementada para refletir a nova coluna de juros e possíveis ajustes
    private static final String TABELA_CONTAS = "contas";

    public static final String PAGAMENTO_PAGO = "paguei";
    public static final String PAGAMENTO_FALTA = "falta";

    private static volatile DBContas sInstance;
    private final SQLiteOpenHelper mDbHelper;

    // --- Singleton ---
    public static DBContas getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DBContas.class) {
                if (sInstance == null) {
                    sInstance = new DBContas(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private DBContas(Context context) {
        mDbHelper = new DatabaseHelper(context);
    }

    // --- Colunas ---
    public static final class Colunas implements BaseColumns {
        public static final String COLUNA_NOME_CONTA = "nome";
        public static final String COLUNA_TIPO_CONTA = "tipo"; // 0: Despesa, 1: Receita, 2: Aplicação
        public static final String COLUNA_CLASSE_CONTA = "classe";
        public static final String COLUNA_CATEGORIA_CONTA = "categoria";
        public static final String COLUNA_DIA_DATA_CONTA = "dia";
        public static final String COLUNA_MES_DATA_CONTA = "mes";
        public static final String COLUNA_ANO_DATA_CONTA = "ano";
        public static final String COLUNA_VALOR_CONTA = "valor";
        public static final String COLUNA_PAGOU_CONTA = "pagamento"; // "paguei" ou "falta"
        public static final String COLUNA_QT_REPETICOES_CONTA = "qt_repeticoes"; // Quantidade total de repetições
        public static final String COLUNA_NR_REPETICAO_CONTA = "nr_repeticao"; // Número da repetição atual (começando de 1)
        public static final String COLUNA_INTERVALO_CONTA = "intervalo"; // Em dias ou meses (ex: 300 para mensal)
        public static final String COLUNA_CODIGO_CONTA = "codigo"; // Código único para identificar uma série de contas recorrentes
        public static final String COLUNA_VALOR_JUROS = "valor_juros"; // Para armazenar a taxa de juros (como decimal, ex: 0.05 para 5%)
    }

    // --- Database Helper ---
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, BANCO_DE_DADOS, null, VERSAO_BANCO_DE_DADOS);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String CRIA_TABELA_CONTAS = "CREATE TABLE " + TABELA_CONTAS + " ( "
                    + Colunas._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Colunas.COLUNA_NOME_CONTA + " TEXT NOT NULL,"
                    + Colunas.COLUNA_TIPO_CONTA + " INTEGER, "
                    + Colunas.COLUNA_CLASSE_CONTA + " INTEGER,"
                    + Colunas.COLUNA_CATEGORIA_CONTA + " TEXT, "
                    + Colunas.COLUNA_DIA_DATA_CONTA + " INTEGER NOT NULL, "
                    + Colunas.COLUNA_MES_DATA_CONTA + " INTEGER NOT NULL, "
                    + Colunas.COLUNA_ANO_DATA_CONTA + " INTEGER NOT NULL, "
                    + Colunas.COLUNA_VALOR_CONTA + " REAL NOT NULL, "
                    + Colunas.COLUNA_PAGOU_CONTA + " TEXT NOT NULL, "
                    + Colunas.COLUNA_QT_REPETICOES_CONTA + " INTEGER NOT NULL, "
                    + Colunas.COLUNA_NR_REPETICAO_CONTA + " INTEGER NOT NULL, "
                    + Colunas.COLUNA_INTERVALO_CONTA + " INTEGER NOT NULL,"
                    + Colunas.COLUNA_CODIGO_CONTA + " TEXT NOT NULL,"
                    + Colunas.COLUNA_VALOR_JUROS + " REAL DEFAULT 0.0 );"; // Nova coluna de juros
            db.execSQL(CRIA_TABELA_CONTAS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Lógica de migração do banco de dados
            // Se a versão antiga for < 6, adicione a coluna de juros
            if (oldVersion < 6) {
                // Adiciona a coluna COLUNA_VALOR_JUROS se ela não existir
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " ADD COLUMN " + Colunas.COLUNA_VALOR_JUROS + " REAL DEFAULT 0.0;");
            }
            // Adicione mais migrações aqui se necessário para futuras versões

            // Se alguma versão anterior não criou a tabela ou a estrutura mudou drasticamente,
            // você pode optar por recriar a tabela. CUIDADO: isso pode apagar dados existentes.
            // Se a migração acima é suficiente, esta parte pode ser omitida ou comentada.
            // db.execSQL("DROP TABLE IF EXISTS " + TABELA_CONTAS);
            // onCreate(db);
        }
    }

    // --- CREATE ---

    /**
     * Insere uma nova conta no banco de dados.
     *
     * @param conta O objeto Conta a ser inserido.
     * @return O ID da linha inserida, ou -1 em caso de erro.
     */
    public long insertConta(Conta conta) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final ContentValues dadosConta = new ContentValues();
        preencherContentValues(dadosConta, conta);
        return db.insert(TABELA_CONTAS, null, dadosConta);
    }

    /**
     * Insere múltiplas contas recorrentes.
     *
     * @param conta        A conta base a ser repetida. Deve conter o valor original do juros (se aplicável).
     * @param qtRepeticoes O número total de repetições desejadas.
     * @param intervalo    O intervalo entre as repetições.
     */
    public void insertContasRecorrentes(Conta conta, int qtRepeticoes, int intervalo) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Calendar data = Calendar.getInstance();
        data.set(conta.getAno(), conta.getMes(), conta.getDia());

        Log.d("DBContas", "Inserting recurring account for '" + conta.getNome() + "' with interval: " + intervalo + ", qtRepeticoes: " + qtRepeticoes); // Log para depuração

        double valorContaBase = conta.getValor(); // Valor base inicial da conta
        double taxaJuros = conta.getValorJuros(); // Taxa de juros (ex: 0.05 para 5%)

        for (int i = 1; i <= qtRepeticoes; i++) {
            conta.setnRepete(i);
            conta.setDia(data.get(Calendar.DAY_OF_MONTH));
            conta.setMes(data.get(Calendar.MONTH));
            conta.setAno(data.get(Calendar.YEAR));

            double valorCalculado;
            if ((conta.getTipo() == 0 || conta.getTipo() == 2) && taxaJuros != 0.0) {
                valorCalculado = valorContaBase * Math.pow(1.0 + taxaJuros, i - 1);
            } else {
                valorCalculado = valorContaBase;
            }
            conta.setValor(valorCalculado);

            insertConta(conta);

            if (intervalo == 300) { // Mensal
                data.add(Calendar.MONTH, 1);
            } else if (intervalo == 3650) { // Anual
                data.add(Calendar.YEAR, 1);
            } else { // Diária ou Semanal
                data.add(Calendar.DATE, intervalo - 100);
            }
        }
    }

    // --- READ ---

    /**
     * Retorna um Cursor para uma única conta pelo seu ID.
     *
     * @param idConta O ID da conta a ser buscada.
     * @return Cursor com os dados da conta.
     */
    public Cursor getContaById(long idConta) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.query(TABELA_CONTAS, null, Colunas._ID + " = ? ", new String[]{String.valueOf(idConta)}, null, null, null);
    }

    /**
     * Busca contas com filtros flexíveis.
     *
     * @param filter Objeto ContaFilter com os critérios de busca.
     * @param ordem  String de ordenação (ex: "nome ASC").
     * @return Cursor com as contas encontradas.
     */
    public Cursor getContasByFilter(ContaFilter filter, String ordem) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<String> selectionArgsList = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();

        if (filter.getDia() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_DIA_DATA_CONTA, String.valueOf(filter.getDia()));
        }
        if (filter.getMes() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_MES_DATA_CONTA, String.valueOf(filter.getMes()));
        }
        if (filter.getAno() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_ANO_DATA_CONTA, String.valueOf(filter.getAno()));
        }
        if (filter.getTipo() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_TIPO_CONTA, String.valueOf(filter.getTipo()));
        }
        if (filter.getClasse() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CLASSE_CONTA, String.valueOf(filter.getClasse()));
        }
        if (filter.getCategoria() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CATEGORIA_CONTA, filter.getCategoria());
        }
        if (filter.getPagamento() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_PAGOU_CONTA, filter.getPagamento());
        }
        if (filter.getCodigoConta() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CODIGO_CONTA, filter.getCodigoConta());
        }
        if (filter.getNrRepeticao() != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_NR_REPETICAO_CONTA, String.valueOf(filter.getNrRepeticao()));
        }
        if (filter.getNomeConta() != null && !filter.getNomeConta().trim().isEmpty()) {
            if (selectionArgsList.size() > 0) {
                selectionBuilder.append(" AND ");
            }
            selectionBuilder.append(Colunas.COLUNA_NOME_CONTA).append(" LIKE ?");
            selectionArgsList.add("%" + filter.getNomeConta() + "%");
        }

        String selection = selectionBuilder.toString();

        return db.query(TABELA_CONTAS, null, selection, selectionArgsList.toArray(new String[0]), null, null, ordem);
    }

    // Modified appendSelection to correctly handle the condition and argument list
    private void appendSelection(StringBuilder builder, List<String> args, String column, String value) {
        if (args.size() > 0) { // If there are already conditions, add " AND "
            builder.append(" AND ");
        }
        builder.append(column).append(" = ?"); // Append column name and placeholder
        args.add(value); // Add the actual value for this column
    }

    /**
     * Verifica se existem contas com pagamento pendente.
     *
     * @return true se não houver contas pendentes, false caso contrário.
     */
    public boolean confirmaPagamentos() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] str = {Colunas.COLUNA_PAGOU_CONTA};
        try (Cursor c = db.query(TABELA_CONTAS, str, Colunas.COLUNA_PAGOU_CONTA + " != ? ", new String[]{PAGAMENTO_PAGO}, null, null, null)) {
            if (c.getCount() > 0) {
                SQLiteDatabase writableDb = mDbHelper.getWritableDatabase();
                ContentValues dadosConta = new ContentValues();
                dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, PAGAMENTO_FALTA);
                return writableDb.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_PAGOU_CONTA + " != ? ", new String[]{PAGAMENTO_PAGO}) > 0;
            } else {
                return true;
            }
        }
    }

    // --- UPDATE ---

    /**
     * Atualiza os dados de uma conta específica pelo seu ID.
     *
     * @param conta O objeto Conta com os dados atualizados.
     * @return true se a atualização foi bem-sucedida, false caso contrário.
     */
    public boolean updateConta(Conta conta) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final ContentValues dadosConta = new ContentValues();
        preencherContentValues(dadosConta, conta);
        return db.update(TABELA_CONTAS, dadosConta, Colunas._ID + " = ? ", new String[]{String.valueOf(conta.getId())}) > 0;
    }

    /**
     * Enum para definir o escopo da atualização de contas recorrentes.
     */
    public enum TipoAtualizacao {
        APENAS_ESTA, ESTA_E_FUTURAS, TODAS
    }

    /**
     * Atualiza contas com base no código da série e no número de repetição.
     *
     * @param conta           A conta com os novos dados. Deve conter o ID correto para APENAS_ESTA,
     *                        e código/nrRepeticao corretos para as outras opções.
     * @param tipoAtualizacao Define se atualiza apenas esta conta, esta e as futuras, ou todas.
     * @return true se a atualização foi bem-sucedida, false caso contrário.
     */
    public boolean updateContasRecorrentes(Conta conta, TipoAtualizacao tipoAtualizacao) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final ContentValues dadosConta = new ContentValues();
        preencherContentValues(dadosConta, conta); // Usa os novos dados da conta

        String whereClause;
        String[] whereArgs;

        switch (tipoAtualizacao) {
            case APENAS_ESTA:
                whereClause = Colunas._ID + " = ?";
                whereArgs = new String[]{String.valueOf(conta.getId())};
                break;
            case ESTA_E_FUTURAS:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ? AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " >= ?";
                whereArgs = new String[]{conta.getCodigo(), String.valueOf(conta.getnRepete())};
                break;
            case TODAS:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ?";
                whereArgs = new String[]{conta.getCodigo()};
                break;
            default:
                return false;
        }
        return db.update(TABELA_CONTAS, dadosConta, whereClause, whereArgs) > 0;
    }

    // Método auxiliar para preencher ContentValues a partir de um objeto Conta
    private void preencherContentValues(ContentValues values, Conta conta) {
        values.put(Colunas.COLUNA_NOME_CONTA, conta.getNome());
        values.put(Colunas.COLUNA_TIPO_CONTA, conta.getTipo());
        values.put(Colunas.COLUNA_CLASSE_CONTA, conta.getClasseConta());
        values.put(Colunas.COLUNA_CATEGORIA_CONTA, String.valueOf(conta.getCategoria()));
        values.put(Colunas.COLUNA_DIA_DATA_CONTA, conta.getDia());
        values.put(Colunas.COLUNA_MES_DATA_CONTA, conta.getMes());
        values.put(Colunas.COLUNA_ANO_DATA_CONTA, conta.getAno());
        values.put(Colunas.COLUNA_VALOR_CONTA, conta.getValor()); // Este valor pode ter sido ajustado com juros
        values.put(Colunas.COLUNA_PAGOU_CONTA, conta.getPagamento());
        values.put(Colunas.COLUNA_QT_REPETICOES_CONTA, conta.getQtRepete());
        values.put(Colunas.COLUNA_NR_REPETICAO_CONTA, conta.getnRepete());
        values.put(Colunas.COLUNA_INTERVALO_CONTA, conta.getIntervalo());
        values.put(Colunas.COLUNA_CODIGO_CONTA, conta.getCodigo());
        values.put(Colunas.COLUNA_VALOR_JUROS, conta.getValorJuros()); // Salva o valor dos juros
    }

    // --- DELETE ---

    /**
     * Exclui uma conta pelo seu ID.
     *
     * @param idConta O ID da conta a ser excluída.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     */
    public boolean deleteContaById(long idConta) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        return db.delete(TABELA_CONTAS, Colunas._ID + " = ? ", new String[]{String.valueOf(idConta)}) > 0;
    }

    /**
     * Enum para definir o escopo da exclusão de contas recorrentes.
     */
    public enum TipoExclusao {
        APENAS_ESTA, ESTA_E_FUTURAS, TODAS
    }

    /**
     * Exclui contas com base no código da série e no número de repetição.
     *
     * @param codigoConta        O código da série de contas recorrentes.
     * @param nrRepeticaoInicial O número da primeira repetição a ser excluída.
     * @param tipoExclusao       Define se exclui apenas esta conta, esta e as futuras, ou todas.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     */
    public boolean deleteContasRecorrentes(String codigoConta, int nrRepeticaoInicial, TipoExclusao tipoExclusao) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String whereClause;
        String[] whereArgs;

        switch (tipoExclusao) {
            case APENAS_ESTA:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ? AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " = ?";
                whereArgs = new String[]{codigoConta, String.valueOf(nrRepeticaoInicial)};
                break;
            case ESTA_E_FUTURAS:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ? AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " >= ?";
                whereArgs = new String[]{codigoConta, String.valueOf(nrRepeticaoInicial)};
                break;
            case TODAS:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ?";
                whereArgs = new String[]{codigoConta};
                break;
            default:
                return false;
        }
        return db.delete(TABELA_CONTAS, whereClause, whereArgs) > 0;
    }

    /**
     * Exclui todas as contas do banco de dados.
     */
    public void deleteAllContas() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(TABELA_CONTAS, null, null);
    }

    // --- Métodos Auxiliares e de Manutenção ---

    /**
     * Ajusta repetições de contas, possivelmente corrigindo intervalos.
     *
     * @return true se a operação foi bem-sucedida.
     */
    public boolean ajustaRepeticoesContas() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String[] str = {Colunas.COLUNA_INTERVALO_CONTA};
        try (Cursor c = db.query(TABELA_CONTAS, str, Colunas.COLUNA_INTERVALO_CONTA + " < ? ", new String[]{"32"}, null, null, null)) {
            if (c.getCount() > 0) {
                SQLiteDatabase writableDb = mDbHelper.getWritableDatabase();
                ContentValues dadosConta = new ContentValues();
                int intervaloPadraoMensal = 300;
                int limiteDias = 31;
                dadosConta.put(Colunas.COLUNA_INTERVALO_CONTA, intervaloPadraoMensal);
                return writableDb.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_INTERVALO_CONTA + " <= ? ", new String[]{String.valueOf(limiteDias)}) > 0;
            } else {
                return true;
            }
        }
    }

    /**
     * Atualiza o status de pagamento de contas com base na data.
     *
     * @param dia O dia atual.
     * @param mes O mês atual.
     * @param ano O ano atual.
     * @return true se a atualização foi bem-sucedida.
     */
    public boolean atualizaPagamentoContasVencidas(int dia, int mes, int ano) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, PAGAMENTO_PAGO);
        String whereClause = Colunas.COLUNA_DIA_DATA_CONTA + " < ? AND " + Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = ? OR " + Colunas.COLUNA_ANO_DATA_CONTA + " < ?";
        String[] whereArgs = {String.valueOf(dia), String.valueOf(mes), String.valueOf(ano), String.valueOf(ano)};
        return db.update(TABELA_CONTAS, dadosConta, whereClause, whereArgs) > 0;
    }

    // --- Classe auxiliar para filtros ---
    public static class ContaFilter {
        private Integer dia;
        private Integer mes;
        private Integer ano;
        private Integer tipo;
        private Integer classe;
        private String categoria;
        private String pagamento;
        private String codigoConta;
        private Integer nrRepeticao;
        private String nomeConta;

        // Getters
        public Integer getDia() {
            return dia;
        }

        public Integer getMes() {
            return mes;
        }

        public Integer getAno() {
            return ano;
        }

        public Integer getTipo() {
            return tipo;
        }

        public Integer getClasse() {
            return classe;
        }

        public String getCategoria() {
            return categoria;
        }

        public String getPagamento() {
            return pagamento;
        }

        public String getCodigoConta() {
            return codigoConta;
        }

        public Integer getNrRepeticao() {
            return nrRepeticao;
        }

        public String getNomeConta() {
            return nomeConta;
        }

        // Setters
        public ContaFilter setDia(Integer dia) {
            this.dia = dia;
            return this;
        }

        public ContaFilter setMes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public ContaFilter setAno(Integer ano) {
            this.ano = ano;
            return this;
        }

        public ContaFilter setTipo(Integer tipo) {
            this.tipo = tipo;
            return this;
        }

        public ContaFilter setClasse(Integer classe) {
            this.classe = classe;
            return this;
        }

        public ContaFilter setCategoria(String categoria) {
            this.categoria = categoria;
            return this;
        }

        public ContaFilter setPagamento(String pagamento) {
            this.pagamento = pagamento;
            return this;
        }

        public ContaFilter setCodigoConta(String codigoConta) {
            this.codigoConta = codigoConta;
            return this;
        }

        public ContaFilter setNrRepeticao(Integer nrRepeticao) {
            this.nrRepeticao = nrRepeticao;
            return this;
        }

        public ContaFilter setNomeConta(String nomeConta) {
            this.nomeConta = nomeConta;
            return this;
        }
    }

    // --- NEW METHODS TO BE ADDED IN DBContas.java ---

    /**
     * Busca contas pelo tipo, com filtros opcionais de dia, mês e ano.
     *
     * @param dia         O dia (opcional, use 0 para ignorar).
     * @param mes         O mês (opcional, use -1 para ignorar).
     * @param ano         O ano.
     * @param codigoConta O código da conta recorrente (opcional, use null para ignorar).
     * @param tipo        O tipo da conta (0: Despesa, 1: Receita, 2: Aplicação).
     * @return Cursor com as contas encontradas.
     */
    public Cursor buscaContasTipo(int dia, int mes, int ano, String codigoConta, int tipo) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<String> selectionArgsList = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();

        selectionBuilder.append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
        selectionArgsList.add(String.valueOf(tipo));

        if (dia > 0) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_DIA_DATA_CONTA, String.valueOf(dia));
        }
        if (mes >= 0) { // Mês é baseado em 0
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_MES_DATA_CONTA, String.valueOf(mes));
        }
        appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_ANO_DATA_CONTA, String.valueOf(ano));

        if (codigoConta != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CODIGO_CONTA, codigoConta);
        }

        String selection = selectionBuilder.toString();
        return db.query(TABELA_CONTAS, null, selection, selectionArgsList.toArray(new String[0]), null, null, null);
    }

    /**
     * Busca contas por classe, com filtros opcionais de dia, mês e ano.
     *
     * @param dia         O dia (opcional, use 0 para ignorar).
     * @param mes         O mês (opcional, use -1 para ignorar).
     * @param ano         O ano.
     * @param codigoConta O código da conta recorrente (opcional, use null para ignorar).
     * @param tipo        O tipo da conta (0: Despesa, 1: Receita, 2: Aplicação).
     * @param classe      A classe da conta.
     * @return Cursor com as contas encontradas.
     */
    public Cursor buscaContasClasse(int dia, int mes, int ano, String codigoConta, int tipo, int classe) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<String> selectionArgsList = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();

        selectionBuilder.append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
        selectionArgsList.add(String.valueOf(classe));

        if (tipo >= 0) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_TIPO_CONTA, String.valueOf(tipo));
        }
        if (dia > 0) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_DIA_DATA_CONTA, String.valueOf(dia));
        }
        if (mes >= 0) { // Mês é baseado em 0
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_MES_DATA_CONTA, String.valueOf(mes));
        }
        appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_ANO_DATA_CONTA, String.valueOf(ano));

        if (codigoConta != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CODIGO_CONTA, codigoConta);
        }

        String selection = selectionBuilder.toString();
        return db.query(TABELA_CONTAS, null, selection, selectionArgsList.toArray(new String[0]), null, null, null);
    }

    /**
     * Busca contas pelo tipo e método de pagamento, com filtros opcionais de dia, mês e ano.
     *
     * @param dia         O dia (opcional, use 0 para ignorar).
     * @param mes         O mês (opcional, use -1 para ignorar).
     * @param ano         O ano.
     * @param codigoConta O código da conta recorrente (opcional, use null para ignorar).
     * @param tipo        O tipo da conta (0: Despesa, 1: Receita, 2: Aplicação).
     * @param pagamento   O status do pagamento ("paguei" ou "falta").
     * @return Cursor com as contas encontradas.
     */
    public Cursor buscaContasTipoPagamento(int dia, int mes, int ano, String codigoConta, int tipo, String pagamento) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<String> selectionArgsList = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();

        selectionBuilder.append(Colunas.COLUNA_PAGOU_CONTA).append(" = ?");
        selectionArgsList.add(pagamento);

        if (tipo >= 0) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_TIPO_CONTA, String.valueOf(tipo));
        }
        if (dia > 0) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_DIA_DATA_CONTA, String.valueOf(dia));
        }
        if (mes >= 0) { // Mês é baseado em 0
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_MES_DATA_CONTA, String.valueOf(mes));
        }
        appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_ANO_DATA_CONTA, String.valueOf(ano));

        if (codigoConta != null) {
            appendSelection(selectionBuilder, selectionArgsList, Colunas.COLUNA_CODIGO_CONTA, codigoConta);
        }

        String selection = selectionBuilder.toString();
        return db.query(TABELA_CONTAS, null, selection, selectionArgsList.toArray(new String[0]), null, null, null);
    }
}