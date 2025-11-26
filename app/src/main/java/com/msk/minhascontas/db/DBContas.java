package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.text.TextUtils;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.Arrays;

import static com.msk.minhascontas.db.ContasContract.Colunas; // Import Estático para simplificar referências

import com.msk.minhascontas.R;

/**
 * Singleton class to manage database operations for 'contas' (accounts/bills).
 * Provides methods for creating, reading, updating, and deleting account records.
 * It also handles recurring accounts, filtering, and aggregation.
 */
public final class DBContas {

    // --- DATABASE CONSTANTS ---
    private static final String BANCO_DE_DADOS = "minhas_contas";
    private static final String TABELA_CONTAS = Colunas.TABELA_NOME;

    // SQL command to create the 'contas' table
    private static final String CRIA_TABELA_CONTAS = "CREATE TABLE " + TABELA_CONTAS + " ( "
            + Colunas._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," // column 0
            + Colunas.COLUNA_NOME_CONTA + " TEXT NOT NULL,"                   // column 1
            + Colunas.COLUNA_TIPO_CONTA + " INTEGER, "                        // column 2
            + Colunas.COLUNA_CLASSE_CONTA + " INTEGER,"                       // column 3
            + Colunas.COLUNA_CATEGORIA_CONTA + " INTEGER, "                   // column 4
            + Colunas.COLUNA_DIA_DATA_CONTA + " INTEGER NOT NULL, "           // column 5
            + Colunas.COLUNA_MES_DATA_CONTA + " INTEGER NOT NULL, "           // column 6
            + Colunas.COLUNA_ANO_DATA_CONTA + " INTEGER NOT NULL, "           // column 7
            + Colunas.COLUNA_VALOR_CONTA + " REAL NOT NULL, "                 // column 8
            + Colunas.COLUNA_PAGOU_CONTA + " TEXT NOT NULL, "                 // column 9
            + Colunas.COLUNA_QT_REPETICOES_CONTA + " INTEGER NOT NULL, "      // column 10
            + Colunas.COLUNA_NR_REPETICAO_CONTA + " INTEGER NOT NULL, "       // column 11
            + Colunas.COLUNA_INTERVALO_CONTA + " INTEGER NOT NULL,"           // column 12
            + Colunas.COLUNA_CODIGO_CONTA + " TEXT NOT NULL,"                 // column 13
            + Colunas.COLUNA_VALOR_JUROS + " REAL DEFAULT 0.0);";             // column 14 (NOVO CAMPO)

    // Array of all column names for queries
    private static final String[] colunas_contas = {
            Colunas._ID, Colunas.COLUNA_NOME_CONTA, Colunas.COLUNA_TIPO_CONTA,
            Colunas.COLUNA_CLASSE_CONTA, Colunas.COLUNA_CATEGORIA_CONTA, Colunas.COLUNA_DIA_DATA_CONTA,
            Colunas.COLUNA_MES_DATA_CONTA, Colunas.COLUNA_ANO_DATA_CONTA, Colunas.COLUNA_VALOR_CONTA,
            Colunas.COLUNA_PAGOU_CONTA, Colunas.COLUNA_QT_REPETICOES_CONTA, Colunas.COLUNA_NR_REPETICAO_CONTA,
            Colunas.COLUNA_INTERVALO_CONTA, Colunas.COLUNA_CODIGO_CONTA, Colunas.COLUNA_VALOR_JUROS
    };

    // --- PAYMENT STATUS CONSTANTS ---
    public static final String PAGAMENTO_PAGO = "paguei";
    public static final String PAGAMENTO_FALTA = "falta";

    // --- DATABASE VERSION ---
    private static final int VERSAO_BANCO_DE_DADOS = 7; // Updated version for category column correction

    // --- SINGLETON INSTANCE ---
    private static DBContas sInstance;

    // --- DATABASE ELEMENTS ---
    private DatabaseHelper DBHelper;
    private Context contexto;
    private SQLiteDatabase db;

    private static final String TAG = "DBContas"; // Tag for logging

    // --- CONSTRUCTOR & LIFECYCLE ---

    /**
     * Private constructor to prevent direct instantiation (Singleton pattern).
     */
    private DBContas() {
        // Required for class definition, actual constructor is private(Context)
    }

    /**
     * Private constructor for the Singleton, initializes the DatabaseHelper.
     * @param context Application context.
     */
    private DBContas(Context context) {
        this.contexto = context;
        this.DBHelper = new DatabaseHelper(context);
        open(); // Open the database immediately upon instantiation
    }

    /**
     * Provides the singleton instance of DBContas.
     * Uses the Application Context to prevent memory leaks from Activity contexts.
     *
     * @param context The context from which to get the Application Context.
     * @return The single instance of DBContas.
     */
    public static synchronized DBContas getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DBContas(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Opens the database for writing.
     *
     * @return This DBContas instance.
     * @throws SQLException If the database cannot be opened.
     */
    public DBContas open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    /**
     * Closes the database helper.
     */
    public void close() {
        if (DBHelper != null) {
            DBHelper.close();
        }
    }

    // --- ENUMS FOR RECURRING ACCOUNT OPERATIONS ---

    /** Options for deleting recurring accounts. */
    public enum TipoExclusao {
        SOMENTE_ESTA,      // Delete only this specific account instance
        DESTA_EM_DIANTE,   // Delete this account and all subsequent recurring accounts
        TODAS_AS_REPETICOES // Delete all recurring accounts in the series
    }

    /** Options for updating recurring accounts. */
    public enum TipoAtualizacao {
        SOMENTE_ESTA,      // Update only this specific account instance
        DESTA_EM_DIANTE,   // Update this account and all subsequent recurring accounts (recalculating values)
        TODAS_AS_REPETICOES // Update all recurring accounts in the series (recalculating values)
    }

    // --- HELPER / CONVERSION METHODS ---

    /**
     * Retorna o nome das colunas da tabela de Contas.
     * Usado para criar o cabeçalho da planilha Excel de DADOS DETALHADOS.
     * @return Um array de String contendo os nomes das colunas do banco.
     */
    public String[] getNomeColunas() {
        return new String[]{
                Colunas._ID,
                Colunas.COLUNA_NOME_CONTA,
                Colunas.COLUNA_TIPO_CONTA,
                Colunas.COLUNA_CLASSE_CONTA,
                Colunas.COLUNA_CATEGORIA_CONTA,
                Colunas.COLUNA_DIA_DATA_CONTA,
                Colunas.COLUNA_MES_DATA_CONTA,
                Colunas.COLUNA_ANO_DATA_CONTA,
                Colunas.COLUNA_VALOR_CONTA,
                Colunas.COLUNA_PAGOU_CONTA,
                Colunas.COLUNA_QT_REPETICOES_CONTA,
                Colunas.COLUNA_NR_REPETICAO_CONTA,
                Colunas.COLUNA_INTERVALO_CONTA,
                Colunas.COLUNA_CODIGO_CONTA,
                Colunas.COLUNA_VALOR_JUROS
        };
    }

    /**
     * Converts the Cursor positioned at a row to a Conta object.
     * Must be called only if the Cursor is not null and is in a valid position.
     *
     * @param cursor The Cursor pointing to the current record.
     * @return A Conta object populated with data from the cursor, or null if the cursor is invalid.
     * @deprecated Use {@link #cursorToConta(Cursor)} or the optimized {@link #cursorToContas(Cursor)} for lists.
     */
    @Deprecated
    private Conta cursorParaConta(Cursor cursor) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return null;
        }

        long idConta = cursor.getLong(cursor.getColumnIndexOrThrow(Colunas._ID));
        String nome = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA));
        int tipo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA));
        int classeConta = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA));
        int categoria = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA));
        int dia = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA));
        int mes = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA));
        int ano = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA));
        double valor = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
        String pagamento = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA));
        int qtRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA));
        int nRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA));
        int intervalo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA));
        String codigo = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA));
        double valorJuros = 0.0;

        try {
            valorJuros = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS));
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Coluna valor_juros ainda não existe na versão atual do banco (normal during upgrade).");
        }

        return new Conta(idConta, nome, tipo, classeConta, categoria, dia, mes, ano, valor,
                pagamento, qtRepete, nRepete, intervalo, codigo, valorJuros);
    }

    /**
     * Converts the current record of a Cursor to a Conta object.
     * Ensures that the Cursor is not null and is positioned correctly.
     *
     * @param cursor Cursor positioned on a valid record.
     * @return Conta object populated with data from the Cursor.
     */
    private Conta cursorToConta(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            return null;
        }

        try {
            long idConta = cursor.getLong(cursor.getColumnIndexOrThrow(Colunas._ID));
            String nome = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA));
            int tipo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA));
            int classeConta = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA));
            int categoria = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA));
            int dia = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA));
            int mes = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA));
            int ano = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA));
            double valor = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
            String pagamento = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA));
            int qtRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA));
            int nRepete = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA));
            int intervalo = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA));
            String codigo = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA));
            double valorJuros = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS));

            return new Conta(idConta, nome, tipo, classeConta, categoria,
                    dia, mes, ano, valor, pagamento, qtRepete,
                    nRepete, intervalo, codigo, valorJuros);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error in cursorToConta: Missing column in cursor or database schema mismatch: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a Cursor (with multiple records) into a list of Conta objects.
     * Ensures the Cursor is closed after processing.
     *
     * @param cursor The Cursor containing the query results.
     * @return A list of Conta objects. Returns an empty list if the Cursor is null or empty.
     */
    private List<Conta> cursorToContas(Cursor cursor) {
        List<Conta> lista = new ArrayList<>();
        if (cursor == null || cursor.getCount() == 0) {
            return lista;
        }

        try {
            // Map column indices once for optimization
            int idContaIndex = cursor.getColumnIndexOrThrow(Colunas._ID);
            int nomeContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA);
            int tipoContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_TIPO_CONTA);
            int classeContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_CLASSE_CONTA);
            int categoriaContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_CATEGORIA_CONTA);
            int diaDataIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA);
            int mesDataIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA);
            int anoDataIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA);
            int valorContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA);
            int pagamentoIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA);
            int qtRepeteIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA);
            int nRepeteIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA);
            int intervaloRepeteIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA);
            int codigoContaIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA);
            int valorJurosIndex = cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_JUROS);

            while (cursor.moveToNext()) {
                Conta conta = new Conta(
                        cursor.getLong(idContaIndex),
                        cursor.getString(nomeContaIndex),
                        cursor.getInt(tipoContaIndex),
                        cursor.getInt(classeContaIndex),
                        cursor.getInt(categoriaContaIndex),
                        cursor.getInt(diaDataIndex),
                        cursor.getInt(mesDataIndex),
                        cursor.getInt(anoDataIndex),
                        cursor.getDouble(valorContaIndex),
                        cursor.getString(pagamentoIndex),
                        cursor.getInt(qtRepeteIndex),
                        cursor.getInt(nRepeteIndex),
                        cursor.getInt(intervaloRepeteIndex),
                        cursor.getString(codigoContaIndex),
                        cursor.getDouble(valorJurosIndex)
                );
                lista.add(conta);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter Cursor para List<Conta>: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure cursor is closed
            }
        }
        return lista;
    }

    /**
     * Converts a Conta object to ContentValues for database insertion/update.
     *
     * @param conta The Conta object to convert.
     * @return ContentValues containing the account data.
     */
    private ContentValues criarContentValues(Conta conta) {
        ContentValues values = new ContentValues();
        // _ID is not put here as it's typically auto-incremented by the database
        values.put(Colunas.COLUNA_NOME_CONTA, conta.getNome());
        values.put(Colunas.COLUNA_TIPO_CONTA, conta.getTipo());
        values.put(Colunas.COLUNA_CLASSE_CONTA, conta.getClasseConta());
        values.put(Colunas.COLUNA_CATEGORIA_CONTA, conta.getCategoria());
        values.put(Colunas.COLUNA_DIA_DATA_CONTA, conta.getDia());
        values.put(Colunas.COLUNA_MES_DATA_CONTA, conta.getMes());
        values.put(Colunas.COLUNA_ANO_DATA_CONTA, conta.getAno());
        values.put(Colunas.COLUNA_VALOR_CONTA, conta.getValor());
        values.put(Colunas.COLUNA_PAGOU_CONTA, conta.getPagamento());
        values.put(Colunas.COLUNA_QT_REPETICOES_CONTA, conta.getQtRepete());
        values.put(Colunas.COLUNA_NR_REPETICAO_CONTA, conta.getNRepete());
        values.put(Colunas.COLUNA_INTERVALO_CONTA, conta.getIntervalo());
        values.put(Colunas.COLUNA_CODIGO_CONTA, conta.getCodigo());
        values.put(Colunas.COLUNA_VALOR_JUROS, conta.getValorJuros());
        return values;
    }

    /**
     * Lista todas as contas (detalhada) de um determinado mês e ano.
     * Usado para popular a aba de DADOS DETALHADOS na exportação.
     * @param mes O mês de referência.
     * @param ano O ano de referência.
     * @return Um Cursor contendo todos os dados. O chamador é responsável por fechar o Cursor.
     */
    @SuppressLint("Recycle")
    public Cursor listaContasCompleta(int mes, int ano) {
        String orderBy = Colunas.COLUNA_DIA_DATA_CONTA + " ASC, " + Colunas.COLUNA_NOME_CONTA + " ASC";

        // A QUERY é a mesma usada em outros listadores, mas sem filtro de status/classe/categoria
        String selecao = Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = ?";
        String[] argumentos = {String.valueOf(mes), String.valueOf(ano)};

        try {
            return db.query(
                    TABELA_CONTAS,
                    getNomeColunas(), // As colunas que queremos (todas)
                    selecao,
                    argumentos,
                    null,
                    null,
                    orderBy
            );
        } catch (Exception e) {
            Log.e(TAG, "Erro ao listar contas completas: " + e.getMessage());
            return null;
        }
    }

    /**
     * Utility method to concatenate two String arrays.
     * Essential for combining base selection arguments with filter arguments.
     *
     * @param args1 The first array of arguments.
     * @param args2 The second array of arguments.
     * @return A new array containing all elements from both input arrays.
     */
    private String[] appendArgs(String[] args1, String[] args2) {
        if (args2 == null || args2.length == 0) return args1;
        if (args1 == null || args1.length == 0) return args2;

        String[] result = Arrays.copyOf(args1, args1.length + args2.length);
        System.arraycopy(args2, 0, result, args1.length, args2.length);
        return result;
    }

    // --- CREATE OPERATIONS ---

    /**
     * Inserts a new generic account record into the database (stub method).
     *
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     * @deprecated Use {@link #geraConta(Conta)} for structured data insertion.
     */
    @Deprecated
    public long geraConta() {
        return db.insert(TABELA_CONTAS, null, new ContentValues()); // Stub
    }

    /**
     * Inserts a new account record into the database using a Conta POJO.
     *
     * @param conta The Conta object containing the data to insert.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long geraConta(Conta conta) {
        ContentValues dadosConta = criarContentValues(conta);
        return db.insert(TABELA_CONTAS, null, dadosConta);
    }

    /**
     * Generates and inserts a series of recurring accounts into the database.
     * The first account is inserted based on 'primeiraConta', and subsequent accounts
     * are calculated based on the interval and number of repetitions. Juros (interest)
     * are applied for DESPESAS (type 0) and RECEITAS (type 2) if specified.
     *
     * @param primeiraConta The base Conta object for the first recurrence.
     * @param qtRepeticoes  The total number of repetitions for the series (including the first).
     * @param intervalo     The interval type for recurrence (e.g., 300 for monthly, 3650 for yearly).
     */
    public void geraContasRecorrentes(Conta primeiraConta, int qtRepeticoes, int intervalo) {
        // Set recurrence properties for the first account
        primeiraConta.setNRepete(1);
        primeiraConta.setIntervalo(intervalo);
        primeiraConta.setQtRepete(qtRepeticoes);

        // Insert the first account
        geraConta(primeiraConta);

        if (qtRepeticoes <= 1) {
            return; // No repetitions to generate
        }

        // Logic for calculating future dates and inserting new rows
        Calendar data = Calendar.getInstance();
        data.set(primeiraConta.getAno(), primeiraConta.getMes() - 1, primeiraConta.getDia()); // Calendar month is 0-based

        Log.d(TAG, "Inserting recurring account for '" + primeiraConta.getNome() + "' with interval: " + intervalo + ", qtRepeticoes: " + qtRepeticoes);

        double valorContaBase = primeiraConta.getValor();
        double taxaJuros = primeiraConta.getValorJuros();
        String codigoConta = primeiraConta.getCodigo(); // Ensure same code for all in series

        for (int i = 2; i <= qtRepeticoes; i++) {
            // Advance the date based on interval
            if (intervalo == 300) { // Monthly
                data.add(Calendar.MONTH, 1);
            } else if (intervalo == 3650) { // Annual
                data.add(Calendar.YEAR, 1);
            } else { // Daily (101) or Weekly (107) - subtract 100 as per internal logic
                data.add(Calendar.DATE, intervalo - 100);
            }

            double valorCalculado;
            // Apply compound interest for expenses (type 0) and revenues (type 2)
            if ((primeiraConta.getTipo() == 0 || primeiraConta.getTipo() == 2) && taxaJuros != 0.0) {
                // Compound interest calculation: M = P * (1 + i)^(n-1)
                valorCalculado = valorContaBase * Math.pow(1.0 + taxaJuros, i - 1);
            } else {
                valorCalculado = valorContaBase;
            }

            // Create a NEW Conta object to avoid modifying the 'primeiraConta' reference
            Conta novaConta = new Conta.Builder(
                    primeiraConta.getNome(),
                    valorCalculado,
                    data.get(Calendar.DAY_OF_MONTH),
                    data.get(Calendar.MONTH) + 1, // Adjust month back to 1-based
                    data.get(Calendar.YEAR),
                    codigoConta
            )
                    .setTipo(primeiraConta.getTipo())
                    .setClasseConta(primeiraConta.getClasseConta())
                    .setCategoria(primeiraConta.getCategoria())
                    .setPagamento(PAGAMENTO_FALTA) // Assume repetition is not yet paid
                    .setQtRepete(qtRepeticoes)
                    .setNRepete(i) // Current repetition number
                    .setIntervalo(intervalo)
                    .setValorJuros(taxaJuros)
                    .build();

            // Insert the new account
            geraConta(novaConta);
        }
    }

    // --- READ OPERATIONS (POJO-based) ---

    /**
     * Retrieves a single Conta object by its ID.
     *
     * @param idConta The ID of the account.
     * @return The Conta object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Conta getConta(long idConta) throws SQLException {
        try (Cursor cursor = db.query(TABELA_CONTAS,
                null, // All columns
                Colunas._ID + "=?",
                new String[]{String.valueOf(idConta)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToConta(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar conta por ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a single Conta object by its ID (alternative name).
     *
     * @param id The ID of the account.
     * @return The Conta object, or null if not found.
     */
    public Conta getContaById(long id) {
        return getConta(id); // Delegates to the primary getConta method
    }

    /**
     * Retrieves a list of all detailed account records from the database.
     * Essential for the 'DADOS' tab in export functionality.
     *
     * @return A List of all Conta objects. Returns an empty list if no accounts are found or an error occurs.
     */
    public List<Conta> getAllContasDetalhado() {
        try {
            // Query all columns without WHERE clauses or ordering.
            // cursorToContas ensures the cursor is closed.
            return cursorToContas(db.query(TABELA_CONTAS, colunas_contas, null, null, null, null, null));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar todas as contas detalhadas: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Converte um Cursor de Contas em uma Lista de objetos Conta.
     * Esta é a camada de mapeamento entre o banco de dados e o modelo de objeto (POJO).
     * @param cursor O Cursor retornado pela query (ex: listaContasCompleta).
     * @return Uma lista de objetos Conta.
     */
    public List<Conta> cursorToListaContas(Cursor cursor) {
        List<Conta> listaContas = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Recupera os índices das colunas (é mais seguro fazer isso a cada vez)
                int colId = cursor.getColumnIndex(Colunas._ID);
                int colNome = cursor.getColumnIndex(Colunas.COLUNA_NOME_CONTA);
                int colTipo = cursor.getColumnIndex(Colunas.COLUNA_TIPO_CONTA);
                int colClasse = cursor.getColumnIndex(Colunas.COLUNA_CLASSE_CONTA);
                int colCategoria = cursor.getColumnIndex(Colunas.COLUNA_CATEGORIA_CONTA);
                int colDia = cursor.getColumnIndex(Colunas.COLUNA_DIA_DATA_CONTA);
                int colMes = cursor.getColumnIndex(Colunas.COLUNA_MES_DATA_CONTA);
                int colAno = cursor.getColumnIndex(Colunas.COLUNA_ANO_DATA_CONTA);
                int colValor = cursor.getColumnIndex(Colunas.COLUNA_VALOR_CONTA);
                int colPagamento = cursor.getColumnIndex(Colunas.COLUNA_PAGOU_CONTA);
                int colQtRepete = cursor.getColumnIndex(Colunas.COLUNA_QT_REPETICOES_CONTA);
                int colNRepete = cursor.getColumnIndex(Colunas.COLUNA_NR_REPETICAO_CONTA);
                int colIntervalo = cursor.getColumnIndex(Colunas.COLUNA_INTERVALO_CONTA);
                int colCodigo = cursor.getColumnIndex(Colunas.COLUNA_CODIGO_CONTA);
                int colValorJuros = cursor.getColumnIndex(Colunas.COLUNA_VALOR_JUROS);

                // Cria o objeto Conta a partir do construtor completo
                Conta conta = new Conta(
                        cursor.getLong(colId),
                        cursor.getString(colNome),
                        cursor.getInt(colTipo),
                        cursor.getInt(colClasse),
                        cursor.getInt(colCategoria),
                        cursor.getInt(colDia),
                        cursor.getInt(colMes),
                        cursor.getInt(colAno),
                        cursor.getDouble(colValor),
                        cursor.getString(colPagamento),
                        cursor.getInt(colQtRepete),
                        cursor.getInt(colNRepete),
                        cursor.getInt(colIntervalo),
                        cursor.getString(colCodigo),
                        cursor.getDouble(colValorJuros)
                        // NOTA: Assegure-se de que a ordem dos campos no construtor
                        // de Conta.java está correta.
                );

                listaContas.add(conta);
            } while (cursor.moveToNext());
        }
        return listaContas;
    }

    /**
     * Retrieves a list of Conta objects filtered and ordered according to the provided criteria.
     * This method is the primary way to query accounts with flexible filtering.
     *
     * @param filter The ContaFilter object containing search criteria (month, year, type, etc.). Can be null for no filter.
     * @param ordem  The ORDER BY clause for sorting the results (e.g., "COL_NAME ASC"). Can be null for default order.
     * @return A List<Conta> containing the results. Returns an empty list if no accounts match the criteria.
     */
    public List<Conta> getContas(ContaFilter filter, String ordem) {
        List<Conta> listaContas = new ArrayList<>();
        Cursor cursor = null;

        try {
            String selection = null;
            String[] selectionArgs = null;

            if (filter != null) {
                selection = filter.getSelection();
                selectionArgs = filter.getSelectionArgs();
            }

            cursor = db.query(
                    TABELA_CONTAS,
                    null, // Columns: all
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    ordem // orderBy
            );

            // cursorToContas handles null/empty cursor and closes it.
            listaContas = cursorToContas(cursor);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter contas com filtro e ordem: " + e.getMessage());
        } finally {
            // Note: cursorToContas now closes the cursor. If any path doesn't use it, close here.
            // For now, it's safe as cursorToContas is always called.
        }

        return listaContas;
    }

    /**
     * Retrieves a list of accounts for a specific month and year, applying additional filters.
     *
     * @param mes    The month (1-12).
     * @param ano    The year.
     * @param tipo   The type of account (e.g., 0 for expense, 1 for income).
     * @param filtro An optional ContaFilter object for further filtering.
     * @return A List of Conta objects matching the criteria.
     */
    public List<Conta> getContasDoMes(int mes, int ano, int tipo, ContaFilter filtro) {
        List<Conta> contas = new ArrayList<>();
        Cursor cursor = null;

        String selection = Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " +
                Colunas.COLUNA_ANO_DATA_CONTA + " = ? AND " +
                Colunas.COLUNA_TIPO_CONTA + " = ?";
        String[] selectionArgs = new String[]{
                String.valueOf(mes),
                String.valueOf(ano),
                String.valueOf(tipo)
        };

        if (filtro != null) {
            String filtroSelection = filtro.getSelection();
            String[] filtroArgs = filtro.getSelectionArgs();
            if (!TextUtils.isEmpty(filtroSelection)) {
                selection += " AND (" + filtroSelection + ")";
                selectionArgs = appendArgs(selectionArgs, filtroArgs);
            }
        }

        // Ordering: By Day, then by Value
        String orderBy = Colunas.COLUNA_DIA_DATA_CONTA + " ASC, " + Colunas.COLUNA_VALOR_CONTA + " DESC";

        try {
            cursor = db.query(
                    TABELA_CONTAS, null, selection, selectionArgs, null, null, orderBy
            );

            // cursorToContas handles null/empty cursor and closes it.
            contas = cursorToContas(cursor);

        } catch (SQLException e) {
            Log.e("DBContas", "Erro ao obter contas do mês: " + e.getMessage());
        }
        return contas;
    }

    /**
     * Retrieves a list of Conta objects based on the provided filter.
     * Dynamically builds the WHERE clause from ContaFilter.
     *
     * @param filtro ContaFilter object containing search criteria (can be null).
     * @return A List of Conta objects. Returns an empty list in case of error.
     * @throws SQLException If a database error occurs during the query construction or execution.
     */
    public List<Conta> getAllContas(ContaFilter filtro) throws SQLException {
        StringBuilder selecao = new StringBuilder();
        List<String> argumentosList = new ArrayList<>();
        String ordenacao = Colunas.COLUNA_ANO_DATA_CONTA + " DESC, " + Colunas.COLUNA_MES_DATA_CONTA + " DESC, " + Colunas.COLUNA_DIA_DATA_CONTA + " DESC";

        if (filtro != null) {
            if (!TextUtils.isEmpty(filtro.getNome())) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_NOME_CONTA).append(" LIKE ?");
                argumentosList.add("%" + filtro.getNome() + "%");
            }

            if (filtro.getTipo() != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.getTipo()));
            }

            if (filtro.getClasse() != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.getClasse()));
            }

            if (filtro.getCategoria() != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CATEGORIA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.getCategoria()));
            }

            int ano = filtro.getAno();
            int mes = filtro.getMes();
            int dia = filtro.getDia();

            if (ano > 0) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(ano));

                if (mes > 0) {
                    selecao.append(" AND ").append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ?");
                    argumentosList.add(String.valueOf(mes));

                    if (dia > 0) {
                        selecao.append(" AND ").append(Colunas.COLUNA_DIA_DATA_CONTA).append(" = ?");
                        argumentosList.add(String.valueOf(dia));
                    }
                }
            }
        }

        String[] argumentos = argumentosList.toArray(new String[0]);
        String selecaoFinal = selecao.length() > 0 ? selecao.toString() : null;

        Cursor cursor = null;
        try {
            cursor = db.query(TABELA_CONTAS,
                    null,
                    selecaoFinal,
                    argumentos,
                    null,
                    null,
                    ordenacao);

            return cursorToContas(cursor); // cursorToContas handles null/empty cursor and closes it.

        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar todas as contas com filtro: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- READ OPERATIONS (Cursor-based - Legacy, prefer POJO-based methods) ---

    /**
     * Retrieves a Cursor for a single account by its ID.
     *
     * @param idConta The ID of the account to retrieve.
     * @return A Cursor with the account data. The caller is responsible for closing the Cursor.
     * @deprecated Prefer {@link #getConta(long)} which returns a POJO and manages cursor closing.
     */
    @Deprecated
    public Cursor getContaPeloId(long idConta) {
        return db.query(TABELA_CONTAS, null, Colunas._ID + " = ? ", new String[]{String.valueOf(idConta)}, null, null, null);
    }

    /**
     * Retrieves a Cursor with accounts filtered by a ContaFilter and ordered.
     *
     * @param filter  The ContaFilter object containing search criteria.
     * @param orderBy The ORDER BY clause for sorting.
     * @return A Cursor with the filtered and ordered accounts. The caller is responsible for closing the Cursor.
     * @deprecated Prefer {@link #getContas(ContaFilter, String)} which returns a List<Conta> and manages cursor closing.
     */
    @Deprecated
    public Cursor getContasByFilter(ContaFilter filter, String orderBy) {
        String whereClause = null;
        String[] whereArgs = null;

        if (db == null || !db.isOpen()) {
            Log.e(TAG, "Falha ao buscar contas com filtro (Cursor-based): Database não está aberto.");
            return null;
        }

        try {
            // CORREÇÃO DE NULIDADE: Verifica se o filtro é nulo antes de chamar métodos nele.
            if (filter != null) {
                whereClause = filter.buildWhereClause();
                whereArgs = filter.buildWhereArgs();
            } else {
                // Se o filtro é nulo, busca todas as contas.
                Log.w(TAG, "getContasByFilter chamado com filtro nulo. Retornando todas as contas.");
            }

            return db.query(
                    TABELA_CONTAS,
                    colunas_contas,
                    whereClause, // Será null se o filtro for null, buscando todas as linhas
                    whereArgs,   // Será null se o filtro for null
                    null,
                    null,
                    orderBy
            );
        } catch (SQLException e) {
            Log.e(TAG, "Erro de SQL ao buscar contas com filtro (Cursor-based): " + e.getMessage());
            return null;
        } catch (Exception e) {
            // Captura qualquer NPE que possa ter escapado ou outro erro inesperado.
            Log.e(TAG, "Erro inesperado (incluindo possível NPE) ao buscar contas com filtro (Cursor-based): " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves a Cursor for a single account by its ID.
     *
     * @param idConta The ID of the account.
     * @return A Cursor pointing to the account record. The caller is responsible for closing the Cursor.
     * @deprecated Prefer {@link #getConta(long)} which returns a POJO and manages cursor closing.
     */
    @Deprecated
    public Cursor buscaUmaConta(long idConta) {
        return db.query(TABELA_CONTAS, null, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
    }

    /**
     * Retrieves a Cursor with accounts for a specific day/month/year or month/year.
     *
     * @param dia   The day of the month (0 if filtering by month/year only).
     * @param mes   The month.
     * @param ano   The year.
     * @param ordem The ORDER BY clause.
     * @return A Cursor with the matching accounts. The caller is responsible for closing the Cursor.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter for better structure.
     */
    @Deprecated
    public Cursor buscaContas(int dia, int mes, int ano, String ordem) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_DIA_DATA_CONTA
                    + " = '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA
                    + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                    + ano + "' ", null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_MES_DATA_CONTA
                    + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                    + ano + "' ", null, null, null, ordem);
    }

    // --- (Other legacy `buscaContas...` methods, similar comments apply) ---

    /**
     * Retrieves a Cursor with accounts of a specific type for a given day/month/year or month/year.
     *
     * @param dia   The day of the month (0 if filtering by month/year only).
     * @param mes   The month.
     * @param ano   The year.
     * @param ordem The ORDER BY clause.
     * @param tipo  The type of account.
     * @return A Cursor with the matching accounts. The caller is responsible for closing the Cursor.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public Cursor buscaContasTipo(int dia, int mes, int ano, String ordem, int tipo) {
        // ... (implementation similar to buscaContas, but adds COLUNA_TIPO_CONTA filter) ...
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo + "' ",
                    null, null, null, ordem);
    }

    /**
     * Retrieves a Cursor with accounts of a specific type and payment status for a given date range.
     *
     * @param dia       The day of the month (0 if filtering by month/year only).
     * @param mes       The month.
     * @param ano       The year.
     * @param ordem     The ORDER BY clause.
     * @param tipo      The type of account.
     * @param pagamento The payment status (e.g., "paguei", "falta").
     * @return A Cursor with the matching accounts. The caller is responsible for closing the Cursor.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public Cursor buscaContasTipoPagamento(int dia, int mes, int ano, String ordem, int tipo, String pagamento) {
        // ... (implementation similar to buscaContasTipo, but adds COLUNA_PAGOU_CONTA filter) ...
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '" + pagamento + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '" + pagamento + "' ",
                    null, null, null, ordem);
    }

    /**
     * Retrieves a Cursor with accounts of a specific type and class for a given date range.
     *
     * @param dia    The day of the month (0 if filtering by month/year only).
     * @param mes    The month.
     * @param ano    The year.
     * @param ordem  The ORDER BY clause.
     * @param tipo   The type of account.
     * @param classe The class of the account.
     * @return A Cursor with the matching accounts. The caller is responsible for closing the Cursor.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public Cursor buscaContasClasse(int dia, int mes, int ano, String ordem, int tipo, int classe) {
        // ... (implementation similar to buscaContasTipo, but adds COLUNA_CLASSE_CONTA filter) ...
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' ",
                    null, null, null, ordem);
    }

    /**
     * Retrieves a Cursor with accounts of a specific category for a given date range (assuming type 0 - expenses).
     *
     * @param dia       The day of the month (0 if filtering by month/year only).
     * @param mes       The month.
     * @param ano       The year.
     * @param ordem     The ORDER BY clause.
     * @param categoria The category of the account.
     * @return A Cursor with the matching accounts. The caller is responsible for closing the Cursor.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public Cursor buscaContasCategoria(int dia, int mes, int ano, String ordem, int categoria) {
        // ... (implementation similar to buscaContasTipo, but adds COLUNA_CATEGORIA_CONTA filter for type 0) ...
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = 0 AND "
                            + Colunas.COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + Colunas.COLUNA_TIPO_CONTA + " = 0 AND "
                            + Colunas.COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' ",
                    null, null, null, ordem);
    }

    /**
     * Retrieves a Cursor with accounts matching a specific name.
     *
     * @param nome The name of the account to search for.
     * @return A Cursor with the matching accounts, ordered by date. The caller is responsible for closing the Cursor.
     * @deprecated Prefer {@link #getAllContas(ContaFilter)} with name filter, which returns POJOs.
     */
    @Deprecated
    public Cursor buscaContasPorNome(String nome) {
        nome = nome.replace("'", "''"); // Basic SQL injection prevention, but parameterized queries are safer.
        return db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_NOME_CONTA
                + " = '" + nome + "' ", null, null, null, Colunas.COLUNA_ANO_DATA_CONTA
                + " ASC, " + Colunas.COLUNA_MES_DATA_CONTA + " ASC, " + Colunas.COLUNA_DIA_DATA_CONTA + " ASC");
    }

    // --- READ OPERATIONS (Specific Data - Legacy, prefer POJO field access after getContaById) ---

    /**
     * Retrieves a formatted string of accounts of a specific type for a given month/year.
     *
     * @param nome The generic name prefix for the output string.
     * @param tipo The type of account.
     * @param mes  The month.
     * @param ano  The year.
     * @return A string listing the accounts and their values.
     * @throws SQLException If a database error occurs.
     * @deprecated Retrieve `List<Conta>` and format programmatically for better flexibility.
     */
    @Deprecated
    public String mostraContasPorTipo(String nome, int tipo, int mes, int ano) throws SQLException {
        Cursor cursor = null;
        String str = nome + " do mês:\n";
        Locale current = contexto.getResources().getConfiguration().locale;
        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(current);

        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    Colunas.COLUNA_TIPO_CONTA + " = '" + tipo + "' AND "
                            + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                    null, null, Colunas.COLUNA_NOME_CONTA + " ASC ");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    str = str + dinheiro.format(cursor.getDouble(8)) + " " + cursor.getString(1) + ";\n";
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return str;
    }

    /**
     * Retrieves the name of an account by its ID.
     *
     * @param idConta The ID of the account.
     * @return The name of the account, or " " if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getNome()`.
     */
    @Deprecated
    public String mostraNomeConta(long idConta) throws SQLException {
        Cursor cursor = null;
        String str = " ";
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst())
                str = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA));
        } finally {
            if (cursor != null) cursor.close();
        }
        return str;
    }

    /**
     * Retrieves the day, month, and year of an account by its ID.
     *
     * @param idConta The ID of the account.
     * @return An array of three integers: [day, month, year], or null if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getDia()`, `conta.getMes()`, `conta.getAno()`.
     */
    @Deprecated
    public int[] mostraDMAConta(long idConta) throws SQLException {
        Cursor cursor = null;
        int[] arrayOfInt = null;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                arrayOfInt = new int[3];
                arrayOfInt[0] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_DIA_DATA_CONTA));
                arrayOfInt[1] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_MES_DATA_CONTA));
                arrayOfInt[2] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_ANO_DATA_CONTA));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return arrayOfInt;
    }

    /**
     * Retrieves the value of an account by its ID.
     *
     * @param idConta The ID of the account.
     * @return The value of the account (double), or 0.0D if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getValor()`.
     */
    @Deprecated
    public double mostraValorConta(long idConta) throws SQLException {
        Cursor cursor = null;
        double d = 0.0D;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst())
                d = cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
        } finally {
            if (cursor != null) cursor.close();
        }
        return d;
    }

    /**
     * Retrieves the payment status of an account by its ID.
     *
     * @param idConta The ID of the account.
     * @return The payment status string (e.g., "paguei", "falta"), or empty string if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getPagamento()`.
     */
    @Deprecated
    public String mostraPagamentoConta(long idConta) throws SQLException {
        Cursor cursor = null;
        String pg = "";
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst())
                pg = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_PAGOU_CONTA));
        } finally {
            if (cursor != null) cursor.close();
        }
        return pg;
    }

    /**
     * Retrieves the recurrence details (total repetitions, current repetition number, interval) of an account by its ID.
     *
     * @param idConta The ID of the account.
     * @return An array of three integers: [qtRepete, nRepete, intervalo], or null if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getQtRepete()`, etc.
     */
    @Deprecated
    public int[] mostraRepeticaoConta(long idConta) throws SQLException {
        Cursor cursor = null;
        int[] arrayOfInt = null;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                arrayOfInt = new int[3];
                arrayOfInt[0] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_QT_REPETICOES_CONTA));
                arrayOfInt[1] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NR_REPETICAO_CONTA));
                arrayOfInt[2] = cursor.getInt(cursor.getColumnIndexOrThrow(Colunas.COLUNA_INTERVALO_CONTA));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return arrayOfInt;
    }

    /**
     * Retrieves the ID of the first repetition of a recurring account series.
     *
     * @param nome     The name of the account.
     * @param qtRepete The total number of repetitions in the series.
     * @param codigo   The unique code for the recurring series.
     * @return The ID of the first repetition account, or 0 if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Consider using `ContaFilter` for more robust filtering and POJO return.
     */
    @Deprecated
    public long mostraPrimeiraRepeticaoConta(String nome, int qtRepete, String codigo) throws SQLException {
        nome = nome.replace("'", "''");
        Cursor cursor = null;
        long u = 0;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_NOME_CONTA
                            + " = '" + nome + "' AND " + Colunas.COLUNA_QT_REPETICOES_CONTA
                            + " = '" + qtRepete + "' AND " + Colunas.COLUNA_CODIGO_CONTA
                            + " = '" + codigo + "' AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " = 1 ", // Ensure it's the first repetition
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                u = cursor.getLong(cursor.getColumnIndexOrThrow(Colunas._ID));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return u;
    }

    /**
     * Retrieves the unique code of an account series by its ID.
     *
     * @param idConta The ID of the account.
     * @return The unique code string, or empty string if not found.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #getConta(long)} and then `conta.getCodigo()`.
     */
    @Deprecated
    public String mostraCodigoConta(long idConta) throws SQLException {
        Cursor cursor = null;
        String dConta = "";
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas._ID + " = '" + idConta + "' ", null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                dConta = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_CODIGO_CONTA));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return dConta;
    }

    /**
     * Retrieves a list of unique account names from the database, sorted alphabetically.
     *
     * @return A Vector of unique account names.
     * @throws SQLException If a database error occurs.
     * @deprecated Consider retrieving full `Conta` objects and extracting names for more context.
     */
    @Deprecated
    public Vector<String> mostraNomeContas() throws SQLException {
        Cursor cursor = null;
        Vector<String> v = new Vector<>();
        String str = " "; // Used to track last added name for uniqueness

        try {
            cursor = db.query(TABELA_CONTAS, new String[]{Colunas.COLUNA_NOME_CONTA},
                    null, null, null, null, Colunas.COLUNA_NOME_CONTA + " ASC ");

            if (cursor != null && cursor.moveToFirst()) {
                str = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA));
                v.add(str);
                while (cursor.moveToNext()) {
                    String currentName = cursor.getString(cursor.getColumnIndexOrThrow(Colunas.COLUNA_NOME_CONTA));
                    if (!str.equals(currentName)) {
                        v.add(currentName);
                        str = currentName;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return v;
    }

    /**
     * Coleta os valores do resumo financeiro para um determinado mês e ano.
     * @param context O contexto para acessar Resources e NumberFormat.
     * @param mes O mês de referência.
     * @param ano O ano de referência.
     * @return Um array de String contendo os valores formatados.
     */
    public String[] coletaDadosResumo(Context context, int mes, int ano) {
        Resources res = context.getResources();
        NumberFormat dinheiro = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        int anoExportacao = ano;
        int mesExportacao = mes;

        // --- 1. PREPARAÇÃO DE VARIÁVEIS ---
        String[] despesasCategorias = res.getStringArray(R.array.TipoDespesa);
        String[] receitasCategorias = res.getStringArray(R.array.TipoReceita);
        String[] aplicacoesCategorias = res.getStringArray(R.array.TipoAplicacao);

        int ajusteReceita = (receitasCategorias.length > 1) ? receitasCategorias.length : 0;
        int numLinhasResumo = despesasCategorias.length + ajusteReceita + aplicacoesCategorias.length + 9;
        String[] valores = new String[numLinhasResumo];
        int indice = 0;

        double totalAplicacoes = 0.0;

        // --- 2. DESPESAS (Tipo 0) ---
        valores[indice++] = ""; // Linha Título 'DESPESA'
        for (int i = 0; i < despesasCategorias.length; i++) {
            // Soma de todas as despesas por Categoria (Pago + Falta)
            double soma = somaValoresPorFiltro(
                    anoExportacao, mesExportacao,
                    ContasContract.TIPO_DESPESA, -1, // Ignora classe (filtra por Categoria, que é o 'i')
                    i, null
            );
            valores[indice++] = dinheiro.format(soma);
        }

        // --- 3. RECEITAS (Tipo 1) ---
        valores[indice++] = ""; // Linha Título 'RECEITA'
        if (receitasCategorias.length > 1) {
            for (int i = 0; i < receitasCategorias.length; i++) {
                // Soma de todas as receitas por Categoria
                double soma = somaValoresPorFiltro(
                        anoExportacao, mesExportacao,
                        ContasContract.TIPO_RECEITA, -1, // Ignora classe
                        i, null
                );
                valores[indice++] = dinheiro.format(soma);
            }
        }

        // --- 4. APLICAÇÕES (Tipo 2) ---
        valores[indice++] = ""; // Linha Título 'APLICAÇÃO'
        for (int i = 0; i < aplicacoesCategorias.length; i++) {
            // Soma de todas as aplicações por Classe/Categoria
            double soma = somaValoresPorFiltro(
                    anoExportacao, mesExportacao,
                    ContasContract.TIPO_APLICACAO, i, // Usa 'i' como CLASSE (Fundos=0, Poupança=1...)
                    -1, null
            );
            valores[indice++] = dinheiro.format(soma);
            totalAplicacoes += soma; // Acumula o total de aplicações
        }

        // --- 5. TOTAIS DE RODAPÉ (Os 6 itens finais) ---
        // Busca dos 4 totais primários no banco de dados
        double totalDespesasPagas = somaValoresPorFiltro(
                anoExportacao, mesExportacao, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO
        );
        double totalDespesasPendentes = somaValoresPorFiltro(
                anoExportacao, mesExportacao, ContasContract.TIPO_DESPESA, -1, -1, ContasContract.STATUS_PENDENTE
        );
        double totalReceitasRecebidas = somaValoresPorFiltro(
                anoExportacao, mesExportacao, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PAGO_RECEBIDO
        );
        double totalReceitasPendentes = somaValoresPorFiltro(
                anoExportacao, mesExportacao, ContasContract.TIPO_RECEITA, -1, -1, ContasContract.STATUS_PENDENTE
        );

        // 1. SALDO (R. Recebidas - D. Pagas)
        double saldo = totalReceitasRecebidas - totalDespesasPagas;
        valores[indice++] = dinheiro.format(saldo);

        // 2. TOTAL DESPESAS PAGAS
        valores[indice++] = dinheiro.format(totalDespesasPagas);

        // 3. TOTAL DESPESAS PENDENTES
        valores[indice++] = dinheiro.format(totalDespesasPendentes);

        // 4. TOTAL RECEITAS RECEBIDAS
        valores[indice++] = dinheiro.format(totalReceitasRecebidas);

        // 5. TOTAL RECEITAS PENDENTES
        valores[indice++] = dinheiro.format(totalReceitasPendentes);

        // 6. TOTAL APLICAÇÕES
        valores[indice] = dinheiro.format(totalAplicacoes);

        return valores;
    }

    /**
     * Retorna os nomes das linhas do resumo (Títulos das categorias/tipos).
     * @param context O contexto para acessar Resources e Strings.
     * @return Um array de String com os nomes das linhas.
     */
    public String[] NomeLinhas(Context context) { // Deve ser adaptado da sua implementação original
        Resources res = context.getResources();
        // Use a lógica original do NomeLinhas() que estava em BarraProgresso.java:

        String despesa = res.getString(R.string.linha_despesa);
        String receita = res.getString(R.string.linha_receita);
        String aplicacao = res.getString(R.string.linha_aplicacoes);

        String[] despesas = res.getStringArray(R.array.TipoDespesa);
        String[] receitas = res.getStringArray(R.array.TipoReceita);
        String[] aplicacoes = res.getStringArray(R.array.TipoAplicacao);

        int ajusteReceita = (receitas.length > 1) ? receitas.length : 0;

        // O novo tamanho deve acomodar 6 linhas de totais no rodapé (Saldo, D. Paga, D. Pendente, R. Recebida, R. Pendente, Aplicações)
        // Total fixo: 9 (3 títulos + 6 totais) + Categorias
        int numLinhasResumo = despesas.length + ajusteReceita + aplicacoes.length + 9;
        String[] linhas = new String[numLinhasResumo];
        int indice = 0;

        // TÍTULOS E DISCRIMINAÇÃO POR CATEGORIA
        linhas[indice++] = despesa;
        System.arraycopy(despesas, 0, linhas, indice, despesas.length);
        indice += despesas.length;

        linhas[indice++] = receita;
        if (receitas.length > 1) {
            System.arraycopy(receitas, 0, linhas, indice, receitas.length);
            indice += receitas.length;
        }

        linhas[indice++] = aplicacao;
        System.arraycopy(aplicacoes, 0, linhas, indice, aplicacoes.length);
        indice += aplicacoes.length;

        // RODA PÉ (As 6 últimas linhas)
        // Assumo que as strings de recurso R.string já existem: linha_saldo, linha_despesa_paga, etc.

        linhas[indice++] = res.getString(R.string.linha_saldo);
        linhas[indice++] = res.getString(R.string.resumo_pagas);
        linhas[indice++] = res.getString(R.string.resumo_faltam);
        linhas[indice++] = res.getString(R.string.resumo_recebidas);
        linhas[indice++] = res.getString(R.string.resumo_areceber); // NOVA LINHA
        linhas[indice] = res.getString(R.string.linha_aplicacoes); // NOVA LINHA

        return linhas;
    }

    // --- UPDATE OPERATIONS (POJO-based) ---

    /**
     * Updates the data of a specific account by its ID.
     *
     * @param conta The Conta object with the updated data.
     * @return true if the update was successful, false otherwise.
     */
    public boolean alteraConta(Conta conta) {
        ContentValues args = criarContentValues(conta);
        args.remove(Colunas._ID); // _ID is used in WHERE clause, not updated directly
        return db.update(TABELA_CONTAS, args, Colunas._ID + " = ? ", new String[]{String.valueOf(conta.getIdConta())}) > 0;
    }

    /**
     * Updates recurring accounts based on the specified update type.
     * - {@link TipoAtualizacao#SOMENTE_ESTA}: Updates only the base account.
     * - {@link TipoAtualizacao#DESTA_EM_DIANTE}: Updates the base account and all subsequent recurrences.
     * - {@link TipoAtualizacao#TODAS_AS_REPETICOES}: Updates all recurrences in the series.
     * Values (like valor with juros) are recalculated for affected accounts.
     *
     * @param contaBase     The Conta object with the updated base data.
     * @param tipoAtualizacao The type of update to perform for the recurring series.
     */
    public void alteraContasRecorrentes(Conta contaBase, TipoAtualizacao tipoAtualizacao) {
        if (tipoAtualizacao == TipoAtualizacao.SOMENTE_ESTA) {
            alteraConta(contaBase); // Only update the current instance
            return;
        }

        ContaFilter filter = new ContaFilter()
                .setCodigoConta(contaBase.getCodigo());

        if (tipoAtualizacao == TipoAtualizacao.DESTA_EM_DIANTE) {
            filter.setNrRepeticaoMin(contaBase.getNRepete());
        }

        try (Cursor cursor = getContasByFilter(filter, Colunas.COLUNA_NR_REPETICAO_CONTA + " ASC")) {
            if (cursor == null || cursor.getCount() == 0) {
                return;
            }

            double novoValorBase = contaBase.getValor();
            double novaTaxaJuros = contaBase.getValorJuros();
            int novoIntervalo = contaBase.getIntervalo();
            int novaQtRepeticoes = contaBase.getQtRepete();

            if (cursor.moveToFirst()) {
                do {
                    Conta contaAntiga = cursorToConta(cursor); // Get existing data to preserve ID, date, payment status

                    double valorRecalculado = novoValorBase;
                    if ((contaBase.getTipo() == 0 || contaBase.getTipo() == 2) && novaTaxaJuros != 0.0) {
                        valorRecalculado = novoValorBase * Math.pow(1.0 + novaTaxaJuros, contaAntiga.getNRepete() - 1);
                    }

                    // Build updated account, preserving original ID, payment status, and repetition number
                    Conta contaAtualizada = new Conta.Builder(
                            contaBase.getNome(),
                            valorRecalculado,
                            contaAntiga.getDia(),
                            contaAntiga.getMes(),
                            contaAntiga.getAno(),
                            contaBase.getCodigo()
                    )
                            .setIdConta(contaAntiga.getIdConta())
                            .setTipo(contaBase.getTipo())
                            .setClasseConta(contaBase.getClasseConta())
                            .setCategoria(contaBase.getCategoria())
                            .setPagamento(contaAntiga.getPagamento()) // Preserve old payment status
                            .setQtRepete(novaQtRepeticoes)
                            .setNRepete(contaAntiga.getNRepete()) // Preserve old repetition number
                            .setIntervalo(novoIntervalo)
                            .setValorJuros(novaTaxaJuros)
                            .build();

                    alteraConta(contaAtualizada);

                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            Log.e(TAG, "Erro de SQL durante a alteração de contas recorrentes: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Erro inesperado durante a alteração de contas recorrentes: " + e.getMessage());
        }
    }

    /**
     * Updates the data of a specific account by its ID.
     *
     * @param conta The Conta object with the updated data.
     * @return true if the update was successful, false otherwise.
     * @deprecated Use {@link #alteraConta(Conta)} instead for consistency.
     */
    @Deprecated
    public boolean updateConta(Conta conta) {
        return alteraConta(conta); // Delegate to the main alteraConta method
    }

    /**
     * Updates the payment status (PAGO/FALTA) of a specific account.
     *
     * @param idConta The ID of the account to update.
     * @param status  The new status (DBContas.PAGAMENTO_PAGO or DBContas.PAGAMENTO_FALTA).
     * @return The number of rows affected (0 or 1).
     */
    public int updateContaPagamento(long idConta, String status) {
        ContentValues values = new ContentValues();
        values.put(Colunas.COLUNA_PAGOU_CONTA, status);
        String selection = Colunas._ID + " = ?";
        String[] selectionArgs = {String.valueOf(idConta)};

        return db.update(TABELA_CONTAS, values, selection, selectionArgs);
    }

    /**
     * Updates the payment status of a specific account.
     *
     * @param idConta   The ID of the account.
     * @param pagamento The new payment status.
     * @return true if the update was successful, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #updateContaPagamento(long, String)} for consistency and safety.
     */
    @Deprecated
    public boolean alteraPagamentoConta(long idConta, String pagamento) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, pagamento);
        // SQL injection risk here. Use parameterized query.
        return db.update(TABELA_CONTAS, dadosConta, Colunas._ID + " = ? ", new String[]{String.valueOf(idConta)}) > 0;
    }

    /**
     * Updates the payment status of accounts that are past due (before a specific day, in a given month/year, or before a given year).
     *
     * @param dia The current day (accounts before this day will be marked 'paguei').
     * @param mes The current month.
     * @param ano The current year.
     * @return true if any accounts were updated, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated This logic should be handled carefully. The WHERE clause is complex and potentially error-prone with direct string concatenation.
     */
    @Deprecated
    public boolean atualizaPagamentoContas(int dia, int mes, int ano) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, PAGAMENTO_PAGO); // Changed to use constant
        // This WHERE clause is complicated and prone to errors/SQL injection.
        // It should ideally be refactored into a safer, parameterized query.
        return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_DIA_DATA_CONTA
                        + " < '" + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' OR " + Colunas.COLUNA_ANO_DATA_CONTA + " < '" + ano + "'",
                null) > 0;
    }

    /**
     * Confirms payments for accounts that are not marked 'paguei' by setting them to 'falta'.
     * This seems like a reset or re-evaluation logic.
     *
     * @return true if the update was successful or no accounts needed update, false if an error occurred.
     * @throws SQLException If a database error occurs.
     * @deprecated Logic seems counter-intuitive ("falta" if not "paguei"). Review if this method is still needed.
     */
    @Deprecated
    public boolean confirmaPagamentos() throws SQLException {
        Cursor c = null;
        try {
            c = db.query(TABELA_CONTAS, new String[]{Colunas.COLUNA_PAGOU_CONTA}, Colunas.COLUNA_PAGOU_CONTA
                    + " != '" + PAGAMENTO_PAGO + "' ", null, null, null, null);
            int count = c.getCount();
            if (count > 0) {
                ContentValues dadosConta = new ContentValues();
                dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, PAGAMENTO_FALTA); // Sets all non-paid to 'falta'
                return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_PAGOU_CONTA
                        + " != '" + PAGAMENTO_PAGO + "' ", null) > 0;
            } else return true;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Adjusts the 'intervalo' column for accounts with an interval less than '32' to '300' (monthly).
     * This seems like a data migration/correction utility.
     *
     * @return true if any accounts were adjusted, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Should be a one-time migration, not a regular operation.
     */
    @Deprecated
    public boolean ajustaRepeticoesContas() throws SQLException {
        Cursor c = null;
        try {
            c = db.query(TABELA_CONTAS, new String[]{Colunas.COLUNA_INTERVALO_CONTA}, Colunas.COLUNA_INTERVALO_CONTA
                    + " < '32' ", null, null, null, null);
            int count = c.getCount();
            if (count > 0) {
                ContentValues dadosConta = new ContentValues();
                int intervalo = 300; // Represents monthly
                int diaThreshold = 31; // Days up to 31
                dadosConta.put(Colunas.COLUNA_INTERVALO_CONTA, intervalo);
                return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_INTERVALO_CONTA
                        + " <= '" + diaThreshold + "' ", null) > 0;
            } else return true;
        } finally {
            if (c != null) c.close();
        }
    }

    // --- UPDATE OPERATIONS (Legacy - specific field updates, prefer POJO-based alteraConta or updateConta) ---

    /**
     * Updates the name of accounts in a recurring series.
     * Only updates accounts with repetition number greater than `nrRepete - 1`.
     *
     * @param nomeNovo   The new name for the accounts.
     * @param nomeAntigo The old name of the accounts.
     * @param codigo     The unique code of the recurring series.
     * @param nrRepete   The repetition number from which to start updating (exclusive).
     * @return true if any accounts were updated, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #alteraContasRecorrentes(Conta, TipoAtualizacao)} with `TipoAtualizacao.DESTA_EM_DIANTE`
     *              or `TODAS_AS_REPETICOES` for better control and safety.
     */
    @Deprecated
    public boolean alteraNomeContas(String nomeNovo, String nomeAntigo, String codigo, int nrRepete) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''"); // Basic SQL injection prevention
        nrRepete = nrRepete - 1; // Adjust index for query
        dadosConta.put(Colunas.COLUNA_NOME_CONTA, nomeNovo);
        // Direct string concatenation in WHERE clause: SQL injection risk.
        return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + Colunas.COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    /**
     * Updates the type, class, and category of accounts in a recurring series.
     * Only updates accounts with repetition number greater than `nrRepete - 1`.
     *
     * @param tipo       The new account type.
     * @param classeConta The new account class.
     * @param categoria  The new account category.
     * @param nomeAntigo The old name of the accounts.
     * @param codigo     The unique code of the recurring series.
     * @param nrRepete   The repetition number from which to start updating (exclusive).
     * @return true if any accounts were updated, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #alteraContasRecorrentes(Conta, TipoAtualizacao)} for better control and safety.
     */
    @Deprecated
    public boolean alteraTipoContas(int tipo, int classeConta, int categoria, String nomeAntigo,
                                    String codigo, int nrRepete) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''");
        nrRepete = nrRepete - 1;
        dadosConta.put(Colunas.COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(Colunas.COLUNA_CLASSE_CONTA, classeConta);
        dadosConta.put(Colunas.COLUNA_CATEGORIA_CONTA, categoria);
        return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + Colunas.COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    /**
     * Updates the value and payment status of accounts in a recurring series.
     * Only updates accounts with repetition number greater than `nrRepete - 1`.
     *
     * @param valor      The new value for the accounts.
     * @param pagamento  The new payment status.
     * @param nomeAntigo The old name of the accounts.
     * @param codigo     The unique code of the recurring series.
     * @param nrRepete   The repetition number from which to start updating (exclusive).
     * @return true if any accounts were updated, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #alteraContasRecorrentes(Conta, TipoAtualizacao)} for better control and safety.
     */
    @Deprecated
    public boolean alteraValorContas(double valor, String pagamento, String nomeAntigo,
                                     String codigo, int nrRepete) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''");
        nrRepete = nrRepete - 1;
        dadosConta.put(Colunas.COLUNA_VALOR_CONTA, valor);
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, pagamento);
        return db.update(TABELA_CONTAS, dadosConta, Colunas.COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + Colunas.COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    /**
     * Updates specific data for a single account.
     * This method takes individual parameters.
     *
     * @param idConta       The ID of the account to update.
     * @param nome          The new name.
     * @param tipo          The new type.
     * @param classeConta   The new class.
     * @param categoria     The new category.
     * @param dia           The new day.
     * @param mes           The new month.
     * @param ano           The new year.
     * @param valor         The new value.
     * @param pagamento     The new payment status.
     * @param qtRepete      The new total repetitions.
     * @param nRepete       The new current repetition number.
     * @param intervalo     The new interval.
     * @param codigo        The new recurring code.
     * @param valorJuros    The new interest value.
     * @return true if the update was successful, false otherwise.
     * @deprecated Use {@link #alteraConta(Conta)} with a Conta POJO for better code readability and maintainability.
     */
    @Deprecated
    public boolean alteraDadosConta(long idConta, String nome, int tipo, int classeConta, int categoria, int dia, int mes, int ano, double valor, String pagamento, int qtRepete, int nRepete, int intervalo, String codigo, double valorJuros) {
        ContentValues args = new ContentValues();
        args.put(Colunas.COLUNA_NOME_CONTA, nome);
        args.put(Colunas.COLUNA_TIPO_CONTA, tipo);
        args.put(Colunas.COLUNA_CLASSE_CONTA, classeConta);
        args.put(Colunas.COLUNA_CATEGORIA_CONTA, categoria);
        args.put(Colunas.COLUNA_DIA_DATA_CONTA, dia);
        args.put(Colunas.COLUNA_MES_DATA_CONTA, mes);
        args.put(Colunas.COLUNA_ANO_DATA_CONTA, ano);
        args.put(Colunas.COLUNA_VALOR_CONTA, valor);
        args.put(Colunas.COLUNA_PAGOU_CONTA, pagamento);
        args.put(Colunas.COLUNA_QT_REPETICOES_CONTA, qtRepete);
        args.put(Colunas.COLUNA_NR_REPETICAO_CONTA, nRepete);
        args.put(Colunas.COLUNA_INTERVALO_CONTA, intervalo);
        args.put(Colunas.COLUNA_CODIGO_CONTA, codigo);
        args.put(Colunas.COLUNA_VALOR_JUROS, valorJuros); // New field

        return db.update(TABELA_CONTAS, args, Colunas._ID + " = ? ", new String[]{String.valueOf(idConta)}) > 0;
    }

    /**
     * Updates specific data for a single account (legacy overload without valorJuros).
     *
     * @param idConta       The ID of the account to update.
     * @param nome          The new name.
     * @param tipo          The new type.
     * @param classeConta   The new class.
     * @param categoria     The new category.
     * @param dia           The new day.
     * @param mes           The new month.
     * @param ano           The new year.
     * @param valor         The new value.
     * @param pagamento     The new payment status.
     * @param qtRepete      The new total repetitions.
     * @param nRepete       The new current repetition number.
     * @param intervalo     The new interval.
     * @param codigo        The new recurring code.
     * @return true if the update was successful, false otherwise.
     * @deprecated Use {@link #alteraConta(Conta)} or the other {@link #alteraDadosConta(long, String, int, int, int, int, int, int, double, String, int, int, int, String, double)} for completeness.
     */
    @Deprecated
    public boolean alteraDadosConta(long idConta, String nome, int tipo, int classeConta, int categoria,
                                    int dia, int mes, int ano, double valor, String pagamento, int qtRepete,
                                    int nRepete, int intervalo, String codigo) {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(Colunas.COLUNA_NOME_CONTA, nome);
        dadosConta.put(Colunas.COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(Colunas.COLUNA_CLASSE_CONTA, classeConta);
        dadosConta.put(Colunas.COLUNA_CATEGORIA_CONTA, categoria);
        dadosConta.put(Colunas.COLUNA_DIA_DATA_CONTA, dia);
        dadosConta.put(Colunas.COLUNA_MES_DATA_CONTA, mes);
        dadosConta.put(Colunas.COLUNA_ANO_DATA_CONTA, ano);
        dadosConta.put(Colunas.COLUNA_VALOR_CONTA, valor);
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, pagamento);
        dadosConta.put(Colunas.COLUNA_QT_REPETICOES_CONTA, qtRepete);
        dadosConta.put(Colunas.COLUNA_NR_REPETICAO_CONTA, nRepete);
        dadosConta.put(Colunas.COLUNA_INTERVALO_CONTA, intervalo);
        dadosConta.put(Colunas.COLUNA_CODIGO_CONTA, codigo);
        // SQL injection risk here. Use parameterized query.
        return db.update(TABELA_CONTAS, dadosConta, Colunas._ID + " = '" + idConta + "' ", null) > 0;
    }

    /**
     * Updates the recurring code for accounts matching a name and repetition count.
     * This method's intent is unclear as it only updates the code, but uses repetition count and name.
     *
     * @param nome   The name of the account.
     * @param codigo The new recurring code.
     * @param nr     The repetition count.
     * @return true if the update was successful, false otherwise.
     * @throws SQLException If a database error occurs.
     * @deprecated Review this method's purpose. It seems to update the recurring code based on an old repetition count.
     */
    @Deprecated
    public boolean atualizaDataContas(String nome, String codigo, int nr) throws SQLException {
        ContentValues dataContas = new ContentValues();
        nome = nome.replace("'", "''");
        dataContas.put(Colunas.COLUNA_CODIGO_CONTA, codigo);
        return db.update(TABELA_CONTAS, dataContas, Colunas.COLUNA_NOME_CONTA + " = '"
                + nome + "' AND " + Colunas.COLUNA_QT_REPETICOES_CONTA + " = '" + nr
                + "' ", null) > 0;
    }

    // --- DELETE OPERATIONS (POJO-based & Modern) ---

    /**
     * Deletes a single account record by its ID.
     *
     * @param idConta The ID of the account to delete.
     * @return The number of rows deleted (0 or 1).
     */
    public int deleteConta(long idConta) {
        String selection = Colunas._ID + " = ?";
        String[] selectionArgs = {String.valueOf(idConta)};
        return db.delete(TABELA_CONTAS, selection, selectionArgs);
    }

    /**
     * Deletes a single account record by its ID (alternative name).
     *
     * @param idConta The ID of the account to delete.
     * @return true if the deletion was successful, false otherwise.
     * @deprecated Use {@link #deleteConta(long)} which returns affected rows, or this one if boolean is preferred.
     */
    @Deprecated
    public boolean deletarConta(long idConta) {
        return deleteConta(idConta) > 0;
    }

    /**
     * Deletes a single account by its ID.
     *
     * @param idConta The ID of the account to delete.
     * @return true if the deletion was successful, false otherwise.
     * @deprecated Use {@link #deleteConta(long)} for consistency.
     */
    @Deprecated
    public boolean deleteContaById(long idConta) {
        return deleteConta(idConta) > 0;
    }

    /**
     * Deletes recurring accounts based on the series code, starting repetition number, and exclusion type.
     * - {@link TipoExclusao#SOMENTE_ESTA}: Deletes only the specific account instance.
     * - {@link TipoExclusao#DESTA_EM_DIANTE}: Deletes this account and all subsequent recurring accounts in the series.
     *   Also adjusts the `qt_repeticoes` of remaining accounts in the series.
     * - {@link TipoExclusao#TODAS_AS_REPETICOES}: Deletes all accounts in the recurring series.
     *
     * @param idConta           The ID of the specific account (used only for SOMENTE_ESTA).
     * @param codigoConta        The unique code of the recurring series.
     * @param nrRepeticao       The repetition number of the current account (used for DESTA_EM_DIANTE).
     * @param tipoExclusao       The type of exclusion to perform.
     * @return true if at least one account was deleted, false otherwise.
     */
    public boolean deletarContasRecorrentes(long idConta, String codigoConta, int nrRepeticao, TipoExclusao tipoExclusao) {
        if (tipoExclusao == TipoExclusao.SOMENTE_ESTA) {
            return deletarConta(idConta); // Delegates to simple delete
        } else {
            ContaFilter filterDelecao = new ContaFilter()
                    .setCodigoConta(codigoConta);

            if (tipoExclusao == TipoExclusao.DESTA_EM_DIANTE) {
                filterDelecao.setNrRepeticaoMin(nrRepeticao); // Delete this and all greater repetitions
            }

            String whereClauseDelecao = filterDelecao.buildWhereClause();
            String[] whereArgsDelecao = filterDelecao.buildWhereArgs();

            if (whereClauseDelecao.isEmpty()) {
                Log.e(TAG, "Tentativa de deletar contas recorrentes sem cláusula WHERE válida.");
                return false;
            }

            int linhasDeletadas = db.delete(TABELA_CONTAS, whereClauseDelecao, whereArgsDelecao);

            if (linhasDeletadas > 0 && tipoExclusao == TipoExclusao.DESTA_EM_DIANTE) {
                // Correct qt_repeticoes for remaining accounts in the series
                int novoQtRepete = nrRepeticao - 1;

                if (novoQtRepete > 0) {
                    ContaFilter filterAtualizacao = new ContaFilter()
                            .setCodigoConta(codigoConta)
                            .setNrRepeticaoMax(novoQtRepete); // Update only remaining ones

                    String whereClauseAtualizacao = filterAtualizacao.buildWhereClause();
                    String[] whereArgsAtualizacao = filterAtualizacao.buildWhereArgs();

                    ContentValues argsUpdate = new ContentValues();
                    argsUpdate.put(Colunas.COLUNA_QT_REPETICOES_CONTA, novoQtRepete);

                    int linhasAtualizadas = db.update(TABELA_CONTAS, argsUpdate, whereClauseAtualizacao, whereArgsAtualizacao);
                    Log.d(TAG, "Contas remanescentes atualizadas (qt_repeticoes): " + linhasAtualizadas);
                }
            }
            return linhasDeletadas > 0;
        }
    }

    /**
     * Deletes recurring accounts based on the series code, starting repetition number, and exclusion type.
     * This is an alternative overload to the other `deletarContasRecorrentes` method.
     *
     * @param codigoConta        The unique code of the recurring series.
     * @param nrRepeticaoInicial The number of the first repetition to be deleted.
     * @param tipoExclusao       Defines whether to delete only this account, this and future ones, or all.
     * @return true if the deletion was successful, false otherwise.
     * @deprecated Use {@link #deletarContasRecorrentes(long, String, int, TipoExclusao)} for consistency.
     */
    @Deprecated
    public boolean deleteContasRecorrentes(String codigoConta, int nrRepeticaoInicial, TipoExclusao tipoExclusao) {
        String whereClause;
        String[] whereArgs;

        switch (tipoExclusao) {
            case SOMENTE_ESTA:
                // This branch would need an ID, but this overload doesn't have it.
                // Assuming it means 'the one with codigo and nrRepeticaoInicial'
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ? AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " = ?";
                whereArgs = new String[]{codigoConta, String.valueOf(nrRepeticaoInicial)};
                break;
            case DESTA_EM_DIANTE:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ? AND " + Colunas.COLUNA_NR_REPETICAO_CONTA + " >= ?";
                whereArgs = new String[]{codigoConta, String.valueOf(nrRepeticaoInicial)};
                break;
            case TODAS_AS_REPETICOES:
                whereClause = Colunas.COLUNA_CODIGO_CONTA + " = ?";
                whereArgs = new String[]{codigoConta};
                break;
            default:
                return false;
        }
        return db.delete(TABELA_CONTAS, whereClause, whereArgs) > 0;
    }

    /**
     * Deletes all accounts from the database.
     */
    public void deleteAllContas() {
        db.delete(TABELA_CONTAS, null, null);
    }

    // --- DELETE OPERATIONS (Legacy - direct SQL string, prefer parameterized deleteConta/deletarContasRecorrentes) ---

    /**
     * Exclui uma conta pelo seu ID.
     *
     * @param idConta O ID da conta a ser excluída.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     * @deprecated Use {@link #deleteConta(long)} for parameter safety.
     */
    @Deprecated
    public boolean excluiConta(long idConta) {
        // SQL injection risk here. Use parameterized query.
        return db.delete(TABELA_CONTAS, Colunas._ID + " = '" + idConta + "' ", null) > 0;
    }

    /**
     * Exclui contas por nome e código de série.
     *
     * @param nome   O nome da conta.
     * @param codigo O código da série.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     * @deprecated Use  with an appropriate filter.
     */
    @Deprecated
    public boolean excluiContaPorNome(String nome, String codigo) {
        nome = nome.replace("'", "''"); // Basic SQL injection prevention
        return db.delete(TABELA_CONTAS, Colunas.COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' ", null) > 0;
    }

    /**
     * Exclui uma série de contas recorrentes a partir de um número de repetição.
     *
     * @param nome      O nome da conta.
     * @param codigo    O código da série.
     * @param nr_repete O número da repetição a partir da qual as contas serão excluídas (exclusive).
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     * @deprecated Use  with `DESTA_EM_DIANTE` or `TODAS_AS_REPETICOES`.
     */
    @Deprecated
    public boolean excluiSerieContaPorNome(String nome, String codigo, int nr_repete) {
        nr_repete = nr_repete - 1; // Adjust index for query
        nome = nome.replace("'", "''");
        return db.delete(TABELA_CONTAS, Colunas.COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND "
                + Colunas.COLUNA_NR_REPETICAO_CONTA + " > '" + nr_repete + "' ", null) > 0;
    }

    /**
     * Exclui todas as contas do banco de dados.
     * @deprecated Use {@link #deleteAllContas()} for consistency.
     */
    @Deprecated
    public void excluiTodasAsContas() {
        db.delete(TABELA_CONTAS, null, null);
    }

    // --- AGGREGATION & COUNTING OPERATIONS ---

    /**
     * Counts the total number of distinct accounts in the database.
     *
     * @return The total number of accounts.
     */
    public int quantasContas() {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.query(true, TABELA_CONTAS, colunas_contas, null, null, null, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Calculates the total sum of account values for a specific type, month, and year, applying filters.
     * This is the recommended method for calculating totals.
     *
     * @param mes    The month (1-12).
     * @param ano    The year.
     * @param tipo   The type of account (e.g., 0 for expense, 1 for income).
     * @param filtro An optional ContaFilter object for further filtering.
     * @return The total sum of values.
     */
    public double calcularTotalMensal(int mes, int ano, int tipo, ContaFilter filtro) {
        double total = 0.0;
        Cursor cursor = null;
        String[] projection = new String[]{
                "SUM(" + Colunas.COLUNA_VALOR_CONTA + ")"
        };

        String selection = Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " +
                Colunas.COLUNA_ANO_DATA_CONTA + " = ? AND " +
                Colunas.COLUNA_TIPO_CONTA + " = ?";
        String[] selectionArgs = new String[]{
                String.valueOf(mes),
                String.valueOf(ano),
                String.valueOf(tipo)
        };

        if (filtro != null) {
            // Apply additional filters from ContaFilter
            String filtroSelection = filtro.getSelection();
            String[] filtroArgs = filtro.getSelectionArgs();
            if (!TextUtils.isEmpty(filtroSelection)) {
                selection += " AND (" + filtroSelection + ")";
                selectionArgs = appendArgs(selectionArgs, filtroArgs);
            }
        }

        try {
            cursor = db.query(
                    TABELA_CONTAS, projection, selection, selectionArgs, null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (SQLException e) {
            Log.e("DBContas", "Erro ao calcular total: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    // --- (Other legacy `quantas...` and `soma...` methods, similar comments apply for modernization) ---

    /**
     * Counts how many paid/unpaid accounts of a specific type exist for a given month/year or day/month/year.
     *
     * @param tipo      The type of account.
     * @param pagamento The payment status ("paguei" or "falta").
     * @param dia       The day (0 if filtering by month/year only).
     * @param mes       The month.
     * @param ano       The year.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} and then count the list for better filtering.
     */
    @Deprecated
    public int quantasContasPagasPorTipo(int tipo, String pagamento, int dia, int mes, int ano) {
        Cursor cursor = null;
        int count = 0;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                        + " = '" + tipo + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '"
                        + pagamento + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' ", null, null, null, null);
            else
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                                + " = '" + tipo + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '"
                                + pagamento + "' AND " + Colunas.COLUNA_DIA_DATA_CONTA + " < '" // Note: '<' dia
                                + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes
                                + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many accounts of a specific class exist for a given month/year or day/month/year.
     *
     * @param classe The class of the account.
     * @param dia    The day (0 if filtering by month/year only, or `dia + 1` is used if `ano != 0`).
     * @param mes    The month.
     * @param ano    The year.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public int quantasContasPorClasse(int classe, int dia, int mes, int ano) {
        Cursor cursor = null;
        int count = 0;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            else if (ano != 0) {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                                + Colunas.COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            } else { // If ano is 0, fetches all accounts of that class
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "'", null, null,
                        null, null);
            }
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many accounts exist for a specific month and year.
     *
     * @param mes The month.
     * @param ano The year.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getContas(ContaFilter, String)} with month/year filters.
     */
    @Deprecated
    public int quantasContasPorMes(int mes, int ano) {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.query(true, TABELA_CONTAS, colunas_contas,
                    Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                    null, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many accounts of a specific type exist for a given month/year or day/month/year.
     *
     * @param tipo The type of account.
     * @param dia  The day (0 if filtering by month/year only, or `dia + 1` is used).
     * @param mes  The month.
     * @param ano  The year.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getContasDoMes(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public int quantasContasPorTipo(int tipo, int dia, int mes, int ano) {
        Cursor cursor = null;
        int count = 0;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                        + " = '" + tipo + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' ", null, null, null, null);
            else {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                                + " = '" + tipo + "' AND " + Colunas.COLUNA_DIA_DATA_CONTA + " < '"
                                + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes
                                + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            }
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many accounts exist with a specific name.
     *
     * @param nome The name of the account.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getAllContas(ContaFilter)} with name filter and then count the list.
     */
    @Deprecated
    public int quantasContasPorNome(String nome) {
        nome = nome.replace("'", "''");
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_NOME_CONTA
                    + " = '" + nome + "'", null, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many repetitions exist for a recurring account series by name and code.
     *
     * @param nome   The name of the account.
     * @param codigo The unique code for the recurring series.
     * @return The number of repetitions.
     * @deprecated Use {@link #getContas(ContaFilter, String)} with code/name filters and then count.
     */
    @Deprecated
    public int quantasRepeticoesDaConta(String nome, String codigo) {
        nome = nome.replace("'", "''");
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_NOME_CONTA
                            + " = '" + nome + "' AND "
                            + Colunas.COLUNA_CODIGO_CONTA + " = '" + codigo + "' ",
                    null, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Counts how many accounts with a specific name exist on a particular day.
     *
     * @param nome The name of the account.
     * @param dia  The day.
     * @param mes  The month.
     * @param ano  The year.
     * @return The count of matching accounts.
     * @deprecated Use {@link #getAllContas(ContaFilter)} with name and date filters.
     */
    @Deprecated
    public int quantasContasPorNomeNoDia(String nome, int dia, int mes, int ano) {
        nome = nome.replace("'", "''");
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    Colunas.COLUNA_NOME_CONTA + " = '" + nome + "' AND "
                            + Colunas.COLUNA_DIA_DATA_CONTA + " = '" + dia + "' AND "
                            + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                    null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Sums the values of accounts of a specific type for a given month/year or day/month/year.
     *
     * @param tipo The type of account.
     * @param dia  The day (0 if filtering by month/year only, or `dia + 1` is used).
     * @param mes  The month.
     * @param ano  The year.
     * @return The total sum of values.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #calcularTotalMensal(int, int, int, ContaFilter)} for better filtering and structure.
     */
    @Deprecated
    public double somaContas(int tipo, int dia, int mes, int ano) throws SQLException {
        Cursor cursor = null;
        double total = 0.0D;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                        + " = '" + tipo + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' ", null, null, null, null);
            else {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                                + " = '" + tipo + "' AND " + Colunas.COLUNA_DIA_DATA_CONTA + " < '"
                                + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes
                                + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    total += cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    /**
     * Sums the values of paid/unpaid accounts of a specific type for a given month/year or day/month/year.
     *
     * @param tipo      The type of account.
     * @param pagamento The payment status.
     * @param dia       The day (0 if filtering by month/year only, or `dia + 1` is used).
     * @param mes       The month.
     * @param ano       The year.
     * @return The total sum of values.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #calcularTotalMensal(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public double somaContasPagas(int tipo, String pagamento, int dia, int mes, int ano) throws SQLException {
        Cursor cursor = null;
        double total = 0.0D;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                        + " = '" + tipo + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '"
                        + pagamento + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' ", null, null, null, null);
            else {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas, Colunas.COLUNA_TIPO_CONTA
                                + " = '" + tipo + "' AND " + Colunas.COLUNA_PAGOU_CONTA + " = '"
                                + pagamento + "' AND " + Colunas.COLUNA_DIA_DATA_CONTA + " < '"
                                + dia + "' AND " + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes
                                + "' AND " + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    total += cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    /**
     * Sums the values of accounts of a specific class for a given month/year or day/month/year.
     *
     * @param classe The class of the account.
     * @param dia    The day (0 if filtering by month/year only, or `dia + 1` is used).
     * @param mes    The month.
     * @param ano    The year.
     * @return The total sum of values.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #calcularTotalMensal(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public double somaContasPorClasse(int classe, int dia, int mes, int ano) throws SQLException {
        Cursor cursor = null;
        double total = 0.0D;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            else {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                                + Colunas.COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    total += cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    /**
     * Sums the values of accounts of a specific category for a given month/year or day/month/year.
     *
     * @param categoria The category of the account.
     * @param dia       The day (0 if filtering by month/year only, or `dia + 1` is used).
     * @param mes       The month.
     * @param ano       The year.
     * @return The total sum of values.
     * @throws SQLException If a database error occurs.
     * @deprecated Use {@link #calcularTotalMensal(int, int, int, ContaFilter)} with appropriate filter.
     */
    @Deprecated
    public double somaContasPorCategoria(int categoria, int dia, int mes, int ano) throws SQLException {
        Cursor cursor = null;
        double total = 0.0D;
        try {
            if (dia == 0)
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            else {
                dia = dia + 1; // Adjust day for '<' comparison
                cursor = db.query(TABELA_CONTAS, colunas_contas,
                        Colunas.COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' AND "
                                + Colunas.COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                                + Colunas.COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                                + Colunas.COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                        null, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    total += cursor.getDouble(cursor.getColumnIndexOrThrow(Colunas.COLUNA_VALOR_CONTA));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    /**
     * Soma os valores de contas com base em filtros complexos.
     * Este método utiliza cláusulas WHERE parametrizadas para segurança e flexibilidade.
     *
     * @param ano O ano a ser filtrado (obrigatório).
     * @param mes O mês a ser filtrado (1-12). Use 0 para ignorar o mês (soma anual).
     * @param tipo Tipo da conta (ContasContract.TIPO_...). Use -1 para ignorar o tipo.
     * @param classe Classe da conta (ContasContract.CLASSE_...). Use -1 para ignorar a classe.
     * @param categoria Categoria da conta. Use -1 para ignorar a categoria.
     * @param statusPagamento Status de pagamento/recebimento (ContasContract.STATUS_...). Use null ou String vazia para ignorar.
     * @return A soma total dos valores que correspondem ao filtro.
     */
    public double somaValoresPorFiltro(int ano, int mes, int tipo, int classe, int categoria, String statusPagamento) {
        StringBuilder selection = new StringBuilder();
        ArrayList<String> selectionArgsList = new ArrayList<>();

        // 1. FILTRO ANUAL (Obrigatório)
        selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ?");
        selectionArgsList.add(String.valueOf(ano));

        // 2. FILTRO MENSAL
        if (mes > 0) {
            selection.append(" AND ").append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(mes));
        }

        // 3. FILTRO POR TIPO
        if (tipo != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(tipo));
        }

        // 4. FILTRO POR CLASSE
        if (classe != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(classe));
        }

        // 5. FILTRO POR CATEGORIA
        if (categoria != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_CATEGORIA_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(categoria));
        }

        // 6. FILTRO POR STATUS (Pago/Falta)
        if (statusPagamento != null && !statusPagamento.isEmpty()) {
            selection.append(" AND ").append(Colunas.COLUNA_PAGOU_CONTA).append(" = ?");
            selectionArgsList.add(statusPagamento);
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);
        String[] colunasParaSoma = {"SUM(" + Colunas.COLUNA_VALOR_CONTA + ")"};
        double soma = 0.0;
        Cursor cursor = null;

        try {
            // Executa a consulta de soma
            cursor = db.query(TABELA_CONTAS, colunasParaSoma, selection.toString(),
                    selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // O resultado da soma é a primeira (e única) coluna
                soma = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao somar valores com filtro: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return soma;
    }

    // MÉTODOS CRIADOS PARA IMPORTAR CONTAS EM MASSA

    /**
     * Insere uma lista de objetos Conta no banco de dados usando uma transação.
     * Isso otimiza o desempenho para grandes volumes de dados.
     *
     * @param contas A lista de contas a serem inseridas.
     * @return O número de linhas inseridas.
     */
    public int inserirContasEmMassa(List<Conta> contas) {
        if (contas == null || contas.isEmpty()) {
            return 0;
        }

        db.beginTransaction();
        int linhasInseridas = 0;

        try {
            for (Conta conta : contas) {
                ContentValues values = new ContentValues();
                // O ID da conta é ignorado em uma nova inserção (auto-incremento)
                values.put(Colunas.COLUNA_NOME_CONTA, conta.getNome());
                values.put(Colunas.COLUNA_TIPO_CONTA, conta.getTipo());
                values.put(Colunas.COLUNA_CLASSE_CONTA, conta.getClasseConta());
                values.put(Colunas.COLUNA_CATEGORIA_CONTA, conta.getCategoria());
                values.put(Colunas.COLUNA_DIA_DATA_CONTA, conta.getDia());
                values.put(Colunas.COLUNA_MES_DATA_CONTA, conta.getMes());
                values.put(Colunas.COLUNA_ANO_DATA_CONTA, conta.getAno());
                values.put(Colunas.COLUNA_VALOR_CONTA, conta.getValor());
                values.put(Colunas.COLUNA_PAGOU_CONTA, conta.getPagamento());
                values.put(Colunas.COLUNA_QT_REPETICOES_CONTA, conta.getQtRepete());
                values.put(Colunas.COLUNA_NR_REPETICAO_CONTA, conta.getNRepete());
                values.put(Colunas.COLUNA_INTERVALO_CONTA, conta.getIntervalo());
                values.put(Colunas.COLUNA_CODIGO_CONTA, conta.getCodigo());
                values.put(Colunas.COLUNA_VALOR_JUROS, conta.getValorJuros());

                long id = db.insert(TABELA_CONTAS, null, values);
                if (id != -1) {
                    linhasInseridas++;
                } else {
                    Log.e(TAG, "Falha ao inserir conta em massa: " + conta.getNome());
                    // Não lança exceção, apenas registra o erro e continua
                }
            }
            db.setTransactionSuccessful(); // Marca a transação como bem-sucedida
        } catch (SQLException e) {
            Log.e(TAG, "Erro na transação de inserção em massa: " + e.getMessage());
            // Retorna o que foi inserido até o erro, mas o setTransactionSuccessful
            // garante que apenas o que passou é efetivado.
        } finally {
            db.endTransaction(); // Finaliza a transação (commit ou rollback)
        }
        return linhasInseridas;
    }


    // --- INNER CLASS: CONTAFILTER ---
    /**
     * Builder class to create filters for 'contas' (accounts/bills).
     * Useful for searching recurring series or accounts with specific criteria.
     * This class is {@link Serializable} to allow passing via Bundle between Android components.
     */
    public static class ContaFilter implements Serializable {
        private String codigoConta = null;
        private int nrRepeticaoMin = -1;
        private int nrRepeticaoMax = -1;
        private int dia = -1;
        private int mes = -1;
        private int ano = -1;
        private String nome;
        private int tipo = -1;
        private int classe = -1;
        private int categoria = -1;
        private String pagamento = null;

        // --- CONSTRUCTORS ---
        public ContaFilter() {
            // Empty constructor
        }

        // --- GETTERS ---
        public String getNome() { return nome; }
        public int getTipo() { return tipo; }
        public int getClasse() { return classe; }
        public int getCategoria() { return categoria; }
        public int getDia() { return dia; }
        public int getMes() { return mes; }
        public int getAno() { return ano; }

        // --- SETTERS (fluent interface) ---
        public ContaFilter setCodigoConta(String codigo) {
            this.codigoConta = codigo;
            return this;
        }
        public ContaFilter setNome(String nome) {
            this.nome = nome;
            return this;
        }
        public ContaFilter setNrRepeticaoMin(int nr) {
            this.nrRepeticaoMin = nr;
            return this;
        }
        public ContaFilter setNrRepeticaoMax(int nr) {
            this.nrRepeticaoMax = nr;
            return this;
        }
        public ContaFilter setDia(int dia) {
            this.dia = dia;
            return this;
        }
        public ContaFilter setMes(int mes) {
            this.mes = mes;
            return this;
        }
        public ContaFilter setAno(int ano) {
            this.ano = ano;
            return this;
        }
        public ContaFilter setTipo(int tipo) {
            this.tipo = tipo;
            return this;
        }
        public ContaFilter setClasse(int classe) {
            this.classe = classe;
            return this;
        }
        public ContaFilter setCategoria(int categoria) {
            this.categoria = categoria;
            return this;
        }
        public ContaFilter setPagamento(String pagamento) {
            this.pagamento = pagamento;
            return this;
        }
        /**
         * Sets multiple date filters simultaneously.
         *
         * @param ano The year (-1 for no year filter).
         * @param mes The month (-1 for no month filter).
         * @param dia The day (-1 for no day filter).
         * @return The current ContaFilter instance for chaining.
         */
        public ContaFilter setFiltroData(int ano, int mes, int dia) {
            this.ano = ano;
            this.mes = mes;
            this.dia = dia;
            return this;
        }

        // --- WHERE CLAUSE BUILDERS ---

        /**
         * Builds the WHERE clause string based on the defined filters.
         * Uses '?' placeholders for arguments to prevent SQL injection.
         *
         * @return A SQL WHERE clause string (e.g., "COLUMN_NAME = ? AND ANOTHER_COLUMN LIKE ?").
         */
        public String buildWhereClause() {
            List<String> clauses = new ArrayList<>(); // Changed from Vector to ArrayList for modern Java style
            if (codigoConta != null) {
                clauses.add(Colunas.COLUNA_CODIGO_CONTA + " = ?");
            }
            if (!TextUtils.isEmpty(nome)) {
                clauses.add(Colunas.COLUNA_NOME_CONTA + " LIKE ?");
            }
            if (nrRepeticaoMin > 0) {
                clauses.add(Colunas.COLUNA_NR_REPETICAO_CONTA + " >= ?");
            }
            if (nrRepeticaoMax > 0) {
                clauses.add(Colunas.COLUNA_NR_REPETICAO_CONTA + " <= ?");
            }
            if (dia > 0) {
                clauses.add(Colunas.COLUNA_DIA_DATA_CONTA + " = ?");
            }
            if (mes > 0) {
                clauses.add(Colunas.COLUNA_MES_DATA_CONTA + " = ?");
            }
            if (ano > 0) {
                clauses.add(Colunas.COLUNA_ANO_DATA_CONTA + " = ?");
            }
            if (tipo != -1) {
                clauses.add(Colunas.COLUNA_TIPO_CONTA + " = ?");
            }
            if (classe != -1) {
                clauses.add(Colunas.COLUNA_CLASSE_CONTA + " = ?");
            }
            if (categoria != -1) {
                clauses.add(Colunas.COLUNA_CATEGORIA_CONTA + " = ?");
            }
            if (pagamento != null) {
                clauses.add(Colunas.COLUNA_PAGOU_CONTA + " = ?");
            }
            return TextUtils.join(" AND ", clauses);
        }

        /**
         * Builds the array of arguments corresponding to the '?' placeholders in the WHERE clause.
         *
         * @return A String array of arguments.
         */
        public String[] buildWhereArgs() {
            List<String> args = new ArrayList<>(); // Changed from Vector to ArrayList
            if (codigoConta != null) {
                args.add(codigoConta);
            }
            if (!TextUtils.isEmpty(nome)) {
                args.add("%" + nome + "%"); // Use wildcard for partial matching
            }
            if (nrRepeticaoMin > 0) {
                args.add(String.valueOf(nrRepeticaoMin));
            }
            if (nrRepeticaoMax > 0) {
                args.add(String.valueOf(nrRepeticaoMax));
            }
            if (dia > 0) {
                args.add(String.valueOf(dia));
            }
            if (mes > 0) {
                args.add(String.valueOf(mes));
            }
            if (ano > 0) {
                args.add(String.valueOf(ano));
            }
            if (tipo != -1) {
                args.add(String.valueOf(tipo));
            }
            if (classe != -1) {
                args.add(String.valueOf(classe));
            }
            if (categoria != -1) {
                args.add(String.valueOf(categoria));
            }
            if (pagamento != null) {
                args.add(pagamento);
            }
            return args.toArray(new String[0]);
        }

        /**
         * Constructs a partial WHERE clause for common filters (month, year, type, class, payment).
         * Used by methods like {@link DBContas#getContasDoMes(int, int, int, ContaFilter)}.
         *
         * @return A WHERE clause string with '?' placeholders, or null if no filters are set.
         */
        public String getSelection() {
            List<String> selectionParts = new ArrayList<>();
            if (mes != -1) {
                selectionParts.add(Colunas.COLUNA_MES_DATA_CONTA + " = ?");
            }
            if (ano != -1) {
                selectionParts.add(Colunas.COLUNA_ANO_DATA_CONTA + " = ?");
            }
            if (tipo != -1) {
                selectionParts.add(Colunas.COLUNA_TIPO_CONTA + " = ?");
            }
            if (classe != -1) {
                selectionParts.add(Colunas.COLUNA_CLASSE_CONTA + " = ?");
            }
            if (pagamento != null) {
                selectionParts.add(Colunas.COLUNA_PAGOU_CONTA + " = ?");
            }
            return selectionParts.isEmpty() ? null : TextUtils.join(" AND ", selectionParts);
        }

        /**
         * Returns the arguments for the partial WHERE clause constructed by {@link #getSelection()}.
         *
         * @return A String array of arguments corresponding to the '?' placeholders.
         */
        public String[] getSelectionArgs() {
            List<String> args = new ArrayList<>();
            if (mes != -1) {
                args.add(String.valueOf(mes));
            }
            if (ano != -1) {
                args.add(String.valueOf(ano));
            }
            if (tipo != -1) {
                args.add(String.valueOf(tipo));
            }
            if (classe != -1) {
                args.add(String.valueOf(classe));
            }
            if (pagamento != null) {
                args.add(pagamento);
            }
            return args.toArray(new String[0]);
        }
    }

    // --- INNER CLASS: DATABASEHELPER ---
    /**
     * Inner helper class for creating and upgrading the database.
     * Manages schema changes across different app versions.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, BANCO_DE_DADOS, null, VERSAO_BANCO_DE_DADOS);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CRIA_TABELA_CONTAS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Atualizando o Banco de Dados da versao " + oldVersion
                    + " para " + newVersion);

            // Migration logic for each version increment
            if (oldVersion < 4) {
                // Add COLUNA_CODIGO_CONTA for recurring accounts
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " ADD COLUMN " + Colunas.COLUNA_CODIGO_CONTA + " TEXT DEFAULT ''");
            }

            // Note: oldVersion < 5 was "lost in data recovery" and implicitly drops/recreates the table.
            // This is generally not recommended for data preservation.
            if (oldVersion < 5) {
                Log.w(TAG, "Migration for version 5 involves dropping and recreating table. Data might be lost.");
                db.execSQL("DROP TABLE IF EXISTS " + TABELA_CONTAS);
                onCreate(db); // Recreate the table with the latest schema
            }

            if (oldVersion < 6) {
                // Add COLUNA_VALOR_JUROS for interest calculation
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " ADD COLUMN " + Colunas.COLUNA_VALOR_JUROS + " REAL DEFAULT 0.0;");
            }

            if (oldVersion < 7) {
                // Migration to change COLUNA_CATEGORIA_CONTA from TEXT to INTEGER
                final String TEMP_CATEGORIA = "categoria_temp";

                // 1. Add a temporary INTEGER column
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " ADD COLUMN " + TEMP_CATEGORIA + " INTEGER DEFAULT 0;");

                // 2. Copy data from old TEXT column to new INTEGER column, casting if possible
                // SQLite is flexible, string '10' becomes integer 10. Default 0 for non-numeric.
                db.execSQL("UPDATE " + TABELA_CONTAS + " SET " + TEMP_CATEGORIA + " = CAST(" + Colunas.COLUNA_CATEGORIA_CONTA + " AS INTEGER);");

                // 3. Rename the old TEXT column (optional, for backup)
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " RENAME COLUMN " + Colunas.COLUNA_CATEGORIA_CONTA + " TO categoria_old_text_v6;");

                // 4. Rename the temporary INTEGER column to the original column name
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " RENAME COLUMN " + TEMP_CATEGORIA + " TO " + Colunas.COLUNA_CATEGORIA_CONTA + ";");

                Log.d(TAG, "Migrated COLUNA_CATEGORIA_CONTA from TEXT to INTEGER for version 7.");
                // NOTE: RENAME COLUMN requires SQLite 3.25.0+. For older versions, a full DROP/CREATE/INSERT cycle would be needed.
            }
        }
    }
}