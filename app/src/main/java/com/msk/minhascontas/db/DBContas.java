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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.Arrays;

import static com.msk.minhascontas.db.ContasContract.Colunas; // Import Estático para simplificar referências
import static com.msk.minhascontas.db.ContasContract.Notificacoes; // Import Estático para notificações

import com.msk.minhascontas.db.ContaFilter;
import com.msk.minhascontas.db.TipoExclusao;
import com.msk.minhascontas.db.TipoAtualizacao;

import com.msk.minhascontas.R;
import com.msk.minhascontas.utils.LabelUtils;

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
            + Colunas.COLUNA_TIPO_CONTA + " INTEGER NOT NULL, "      // column 2
            + Colunas.COLUNA_CLASSE_CONTA + " INTEGER NOT NULL,"     // column 3
            + Colunas.COLUNA_CATEGORIA_CONTA + " INTEGER NOT NULL, " // column 4
            + Colunas.COLUNA_DIA_DATA_CONTA + " INTEGER NOT NULL, "           // column 5
            + Colunas.COLUNA_MES_DATA_CONTA + " INTEGER NOT NULL, "           // column 6
            + Colunas.COLUNA_ANO_DATA_CONTA + " INTEGER NOT NULL, "           // column 7
            + Colunas.COLUNA_VALOR_CONTA + " REAL NOT NULL, "                 // column 8
            + Colunas.COLUNA_PAGOU_CONTA + " TEXT NOT NULL, "                 // column 9
            + Colunas.COLUNA_QT_REPETICOES_CONTA + " INTEGER NOT NULL, "      // column 10
            + Colunas.COLUNA_NR_REPETICAO_CONTA + " INTEGER NOT NULL, "       // column 11
            + Colunas.COLUNA_INTERVALO_CONTA + " INTEGER NOT NULL,"           // column 12
            + Colunas.COLUNA_CODIGO_CONTA + " TEXT NOT NULL,"                 // column 13
            + Colunas.COLUNA_VALOR_JUROS + " REAL NOT NULL);";             // column 14 (NOVO CAMPO)

    // SQL command to create the 'notificacoes' table
    private static final String CRIA_TABELA_NOTIFICACOES = "CREATE TABLE " + Notificacoes.TABELA_NOME + " ( "
            + Notificacoes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Notificacoes.COLUNA_TITULO + " TEXT NOT NULL,"
            + Notificacoes.COLUNA_MENSAGEM + " TEXT NOT NULL,"
            + Notificacoes.COLUNA_DATA + " INTEGER NOT NULL,"
            + Notificacoes.COLUNA_LIDA + " INTEGER DEFAULT 0,"
            + Notificacoes.COLUNA_TIPO + " TEXT);";

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
    private static final int VERSAO_BANCO_DE_DADOS = 12; // Updated to 12 for Notifications table

    // --- CONSTANT FOR DB RESET FLAG ---
    public static final String PREF_DB_RESET_FLAG = ContasRepository.PREF_DB_RESET_FLAG;

    // Nome do arquivo de backup
    private static final String NOME_BACKUP_AUTOMATICO = "minhas_contas_backup_seguranca.db";

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
        if (sInstance.db == null || !sInstance.db.isOpen()) {
            try {
                sInstance.open();
                Log.d(TAG, "DBContas.getInstance: Banco de dados reaberto.");
            } catch (Exception e) {
                Log.e(TAG, "DBContas.getInstance: Falha crítica ao abrir banco.", e);
            }
        }
        return sInstance;
    }

    /**
     * Opens the database for writing.
     * Includes a recovery mechanism for upgrade failures:
     * If the initial database open/upgrade fails with SQLException, it attempts
     * to delete the entire database file and reopen it, effectively starting fresh.
     * A flag is set in SharedPreferences to notify the user.
     *
     * @return This DBContas instance.
     * @throws SQLException If the database cannot be opened after recovery attempts.
     */
    /**
     * Abre o banco de dados para escrita com mecanismo de segurança.
     * 1. Tenta fazer backup do banco atual antes de qualquer alteração.
     * 2. Tenta abrir/atualizar o banco.
     * 3. Em caso de falha catastrófica, preserva o backup e inicia um banco limpo.
     */
    public DBContas open() throws SQLException {
        try {
            // Passo 1: Tenta realizar o backup de segurança antes de tocar no banco
            realizarBackupDeSeguranca();

            // Passo 2: Tenta abrir o banco (Isso dispara o onUpgrade se a versão mudou)
            db = DBHelper.getWritableDatabase();

            // Se chegou aqui, sucesso. Limpa flags de erro se existirem.
            contexto.getSharedPreferences("MinhasContasPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean(PREF_DB_RESET_FLAG, false).apply();

            return this;

        } catch (Exception e) {
            Log.e(TAG, "ERRO CRÍTICO na migração/abertura: " + e.getMessage());

            // Passo 3: Recuperação de Desastre
            // A migração falhou. O arquivo original pode estar corrompido, mas temos o backup.
            // Vamos permitir que o usuário use o app (banco zerado), mas avisamos que houve reset.

            try {
                // Fecha qualquer conexão pendente
                if (DBHelper != null) DBHelper.close();

                // Renomeia o banco corrompido para análise futura (não apaga)
                File dbFile = contexto.getDatabasePath(BANCO_DE_DADOS);
                File corruptFile = new File(dbFile.getParent(), "minhas_contas_corrupt.db");
                if(dbFile.exists()) {
                    dbFile.renameTo(corruptFile);
                }

                // Cria uma nova instância do Helper para gerar um banco limpo
                DBHelper = new DatabaseHelper(contexto);
                db = DBHelper.getWritableDatabase();

                // Marca flag para avisar o usuário na UI (opcional)
                contexto.getSharedPreferences("MinhasContasPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean(PREF_DB_RESET_FLAG, true).apply();

                Log.w(TAG, "Banco reiniciado para permitir uso do app. Backup preservado em: " + NOME_BACKUP_AUTOMATICO);

                return this;

            } catch (Exception e2) {
                Log.e(TAG, "Falha total: Não foi possível nem recriar o banco zerado.", e2);
                throw new SQLException("Erro irrecuperável no banco de dados.");
            }
        }
    }

    /**
     * Copia o arquivo SQLite atual para uma área segura antes da atualização.
     */
    private void realizarBackupDeSeguranca() {
        try {
            File dbFile = contexto.getDatabasePath(BANCO_DE_DADOS);

            // Só faz backup se o banco existir
            if (!dbFile.exists()) return;

            // Define o local do backup (FilesDir é seguro e privado do app)
            File backupFile = new File(contexto.getFilesDir(), NOME_BACKUP_AUTOMATICO);

            // Se já existe um backup, não sobrescrevemos para não perder a cópia original da V4
            if (backupFile.exists()) {
                Log.d(TAG, "Backup de segurança já existe. Mantendo o original.");
                return;
            }

            try (FileInputStream fis = new FileInputStream(dbFile);
                 FileOutputStream fos = new FileOutputStream(backupFile);
                 FileChannel src = fis.getChannel();
                 FileChannel dst = fos.getChannel()) {

                dst.transferFrom(src, 0, src.size());
                Log.i(TAG, "Backup de segurança pré-migração realizado com sucesso: " + backupFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao criar backup de segurança: " + e.getMessage());
            // Não lançamos exceção aqui para tentar prosseguir com a abertura mesmo sem backup,
            // mas o ideal seria alertar.
        }
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }

    /**
     * Closes the database helper.
     */
    public void close() {
        if (DBHelper != null) {
            DBHelper.close();
        }
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
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
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
     * @deprecated Migrated to ContasRepository using Room. Keep for legacy reference.
     */
    @Deprecated
    public long geraConta(Conta conta) {
        ContentValues dadosConta = criarContentValues(conta);
        return db.insert(TABELA_CONTAS, null, dadosConta);
    }

    /**
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
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
     * @return A {@code List<Conta>} containing the results. Returns an empty list if no accounts match the criteria.
     */
    public List<Conta> getContas(ContaFilter filter, String ordem) {
        if (db == null || !db.isOpen()) {
            Log.e(TAG, "Tentativa de consulta com banco fechado.");
            return new ArrayList<>();
        }
        List<Conta> listaContas = new ArrayList<>();
        Cursor cursor = null;

        StringBuilder selecao = new StringBuilder();
        List<String> argumentosList = new ArrayList<>();

        // Replicar a lógica de construção da cláusula WHERE do getAllContas
        if (filter != null) {
            if (!TextUtils.isEmpty(filter.nome)) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_NOME_CONTA).append(" LIKE ?");
                argumentosList.add("%" + filter.nome + "%");
            }

            if (filter.tipo != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.tipo));
            }

            if (filter.classe != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.classe));
            }

            if (filter.categoria != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CATEGORIA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.categoria));
            }

            if (filter.dia > 0) { // Adicionado filtro por dia
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_DIA_DATA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.dia));
            }

            if (filter.mes > 0) { // Adicionado filtro por mês
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.mes));
            }

            if (filter.ano > 0) { // Adicionado filtro por ano
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filter.ano));
            }

            if (filter.pagamento != null && !filter.pagamento.isEmpty()) { // Adicionado filtro por pagamento
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_PAGOU_CONTA).append(" = ?");
                argumentosList.add(filter.pagamento);
            }

            // Adicionado filtros de repetição, se aplicáveis
            if (filter.nrRepeticaoMin > 0) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_NR_REPETICAO_CONTA).append(" >= ?");
                argumentosList.add(String.valueOf(filter.nrRepeticaoMin));
            }
            if (filter.nrRepeticaoMax > 0) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_NR_REPETICAO_CONTA).append(" <= ?");
                argumentosList.add(String.valueOf(filter.nrRepeticaoMax));
            }
            if (filter.codigoConta != null) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CODIGO_CONTA).append(" = ?");
                argumentosList.add(filter.codigoConta);
            }
        }

        String[] finalSelectionArgs = argumentosList.toArray(new String[0]);
        String finalSelection = selecao.length() > 0 ? selecao.toString() : null;

        // --- Logging Detalhado para Depuração ---
        Log.d(TAG, "getContas (modern): Iniciando consulta.");
        Log.d(TAG, "getContas (modern): Tabela: " + TABELA_CONTAS);
        Log.d(TAG, "getContas (modern): Colunas Selecionadas: " + Arrays.toString(colunas_contas));
        Log.d(TAG, "getContas (modern): Cláusula WHERE: " + (finalSelection != null ? finalSelection : "NULA/VAZIA (buscando todas as linhas)"));
        Log.d(TAG, "getContas (modern): Argumentos WHERE: " + (finalSelectionArgs != null ? Arrays.toString(finalSelectionArgs) : "NULO"));
        Log.d(TAG, "getContas (modern): Ordenação: " + (ordem != null ? ordem : "NULA"));
        // --- Fim do Logging Detalhado ---

        try {
            cursor = db.query(
                    TABELA_CONTAS,
                    colunas_contas, // Agora usando o array de colunas explícito
                    finalSelection, // Cláusula WHERE completa
                    finalSelectionArgs, // Argumentos WHERE completos
                    null, // groupBy
                    null, // having
                    ordem // orderBy
            );

            listaContas = cursorToContas(cursor); // Este método já fecha o cursor

            Log.d(TAG, "getContas (modern): Consulta concluída. Retornou " + (listaContas != null ? listaContas.size() : 0) + " contas.");

        } catch (SQLException e) {
            Log.e(TAG, "Erro de SQL ao obter contas com filtro e ordem (modern): " + e.getMessage(), e);
            listaContas = new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Erro inesperado ao obter contas com filtro e ordem (modern): " + e.getMessage(), e);
            listaContas = new ArrayList<>();
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

        String selection;
        String[] selectionArgs;

        if (tipo == -1) {
            selection = Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " +
                    Colunas.COLUNA_ANO_DATA_CONTA + " = ?";
            selectionArgs = new String[]{
                    String.valueOf(mes),
                    String.valueOf(ano)
            };
        } else {
            selection = Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " +
                    Colunas.COLUNA_ANO_DATA_CONTA + " = ? AND " +
                    Colunas.COLUNA_TIPO_CONTA + " = ?";
            selectionArgs = new String[]{
                    String.valueOf(mes),
                    String.valueOf(ano),
                    String.valueOf(tipo)
            };
        }

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
            if (!TextUtils.isEmpty(filtro.nome)) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_NOME_CONTA).append(" LIKE ?");
                argumentosList.add("%" + filtro.nome + "%");
            }

            if (filtro.tipo != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.tipo));
            }

            if (filtro.classe != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.classe));
            }

            if (filtro.categoria != -1) {
                if (selecao.length() > 0) selecao.append(" AND ");
                selecao.append(Colunas.COLUNA_CATEGORIA_CONTA).append(" = ?");
                argumentosList.add(String.valueOf(filtro.categoria));
            }

            int ano = filtro.ano;
            int mes = filtro.mes;
            int dia = filtro.dia;

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
     * @deprecated Prefer {@link #getContas(ContaFilter, String)} which returns a {@code List<Conta>} and manages cursor closing.
     */
    @Deprecated
    public Cursor getContasByFilter(ContaFilter filter, String orderBy) {
        // A verificação já foi movida para getInstance(), mas podemos manter uma log redundante aqui
        // se houver algum cenário onde getInstance() foi chamado, mas o db fechou *logo depois*.
        // No entanto, para simplicidade e considerando que getInstance() é o ponto de entrada,
        // a verificação lá é mais eficaz.
        if (db == null || !db.isOpen()) {
            // Este log será menos frequente se getInstance() funcionar corretamente,
            // mas ainda útil para cenários extremos.
            Log.e(TAG, "Falha ao buscar contas com filtro (Cursor-based): Database não está aberto mesmo após getInstance().");
            return null;
        }

        String whereClause = null;
        String[] whereArgs = null;

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
     * @deprecated Retrieve {@code List<Conta>} and format programmatically for better flexibility.
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
     * @deprecated Migrated to ContasRepository.
     */
    @Deprecated
    public String[] NomeLinhas(Context context) {
        Resources res = context.getResources();
        com.msk.minhascontas.utils.LabelUtils labelUtils = com.msk.minhascontas.utils.LabelUtils.INSTANCE;

        String despesa = res.getString(R.string.linha_despesa);
        String receita = res.getString(R.string.linha_receita);
        String aplicacao = res.getString(R.string.linha_aplicacoes);

        // Busca Rótulos Dinâmicos para Despesas (Classes)
        String[] despesas = new String[4];
        for (int i = 0; i < 4; i++) {
            despesas[i] = LabelUtils.getClasseLabel(context, ContasContract.TIPO_DESPESA, i);
        }

        // Busca Rótulos Dinâmicos para Receitas
        String[] receitas = new String[3];
        for (int i = 0; i < 3; i++) {
            receitas[i] = LabelUtils.getClasseLabel(context, ContasContract.TIPO_RECEITA, i);
        }

        // Busca Rótulos Dinâmicos para Aplicações
        String[] aplicacoes = new String[3];
        for (int i = 0; i < 3; i++) {
            aplicacoes[i] = LabelUtils.getClasseLabel(context, ContasContract.TIPO_APLICACAO, i);
        }

        int ajusteReceita = (receitas.length > 1) ? receitas.length : 0;

        int numLinhasResumo = despesas.length + ajusteReceita + aplicacoes.length + 9;
        String[] linhas = new String[numLinhasResumo];
        int indice = 0;

        // TÍTULOS E DISCRIMINAÇÃO POR CLASSE (Tradicionalmente chamado de Tipo no Excel)
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

        // RODA PÉ
        linhas[indice++] = res.getString(R.string.linha_saldo);
        linhas[indice++] = res.getString(R.string.resumo_pagas);
        linhas[indice++] = res.getString(R.string.resumo_faltam);
        linhas[indice++] = res.getString(R.string.resumo_recebidas);
        linhas[indice++] = res.getString(R.string.resumo_areceber);
        linhas[indice] = res.getString(R.string.linha_aplicacoes);

        return linhas;
    }

    // --- UPDATE OPERATIONS (POJO-based) ---

    /**
     * Updates the data of a specific account by its ID.
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
    public boolean alteraConta(Conta conta) {
        ContentValues args = criarContentValues(conta);
        args.remove(Colunas._ID); // _ID is used in WHERE clause, not updated directly
        return db.update(TABELA_CONTAS, args, Colunas._ID + " = ? ", new String[]{String.valueOf(conta.getIdConta())}) > 0;
    }

    /**
     * Updates recurring accounts based on the specified update type.
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
    public void alteraContasRecorrentes(Conta contaBase, TipoAtualizacao tipoAtualizacao) {
        if (tipoAtualizacao == TipoAtualizacao.SOMENTE_ESTA) {
            alteraConta(contaBase); // Only update the current instance
            return;
        }

        ContaFilter filter = new ContaFilter();
        filter.codigoConta = contaBase.getCodigo();

        if (tipoAtualizacao == TipoAtualizacao.DESTA_EM_DIANTE) {
            filter.nrRepeticaoMin = contaBase.getNRepete();
        }

        try (Cursor cursor = getContasByFilter(filter, Colunas.COLUNA_NR_REPETICAO_CONTA + " ASC")) {
            if (cursor == null || cursor.getCount() == 0) {
                return;
            }

            double novoValorBase = contaBase.getValor();
            double novaTaxaJuros = contaBase.getValorJuros();
            int novoIntervalo = contaBase.getIntervalo();
            int novaQtRepeticoes = contaBase.getQtRepete();
            int nRepeteBase = contaBase.getNRepete();
            Calendar calCalculo = Calendar.getInstance();

            if (cursor.moveToFirst()) {
                do {
                    Conta contaAntiga = cursorToConta(cursor); 
                    int nRepeteAtual = contaAntiga.getNRepete();

                    // 1. Recálculo do Valor com Juros Compostos (âncora na conta editada)
                    double valorRecalculado = novoValorBase;
                    if ((contaBase.getTipo() == 0 || contaBase.getTipo() == 2) && novaTaxaJuros != 0.0) {
                        valorRecalculado = novoValorBase * Math.pow(1.0 + novaTaxaJuros, nRepeteAtual - nRepeteBase);
                    }

                    // 2. Recálculo da Data (âncora na data editada)
                    calCalculo.set(contaBase.getAno(), contaBase.getMes() - 1, contaBase.getDia());
                    int diffRepete = nRepeteAtual - nRepeteBase;

                    if (diffRepete != 0 && novoIntervalo > 0) {
                        if (novoIntervalo == 300) { // Mensal
                            calCalculo.add(Calendar.MONTH, diffRepete);
                        } else if (novoIntervalo == 3650) { // Anual
                            calCalculo.add(Calendar.YEAR, diffRepete);
                        } else if (novoIntervalo > 100) { // Diário/Semanal
                            calCalculo.add(Calendar.DATE, (novoIntervalo - 100) * diffRepete);
                        }
                    }

                    int novoDia = calCalculo.get(Calendar.DAY_OF_MONTH);
                    int novoMes = calCalculo.get(Calendar.MONTH) + 1;
                    int novoAno = calCalculo.get(Calendar.YEAR);

                    // Build updated account, preserving original ID, payment status, and repetition number
                    Conta contaAtualizada = new Conta.Builder(
                            contaBase.getNome(),
                            valorRecalculado,
                            novoDia,
                            novoMes,
                            novoAno,
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
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
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
     * Updates the payment status of accounts that are due before the specified date (dia, mes, ano)
     * and are currently marked as 'falta' (pending).
     *
     * @param dia The day of the current date. Accounts with a date before this day, in the same month/year,
     *            or in previous months/years, will be marked as paid.
     * @param mes The month of the current date.
     * @param ano The year of the current date.
     * @return The number of rows affected. Returns 0 if no accounts were updated or an error occurred.
     */
    public int atualizaPagamentoContas(int dia, int mes, int ano) {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(Colunas.COLUNA_PAGOU_CONTA, PAGAMENTO_PAGO);

        // This WHERE clause identifies all accounts that are *past due* relative to the provided dia/mes/ano.
        // It covers:
        // 1. Accounts in previous years.
        // 2. Accounts in the same year but previous months.
        // 3. Accounts in the same year, same month, but previous days.
        String selection = "(" + Colunas.COLUNA_ANO_DATA_CONTA + " < ? )" + // Previous years
                " OR " +
                "(" + Colunas.COLUNA_ANO_DATA_CONTA + " = ? AND " + // Same year, previous months
                Colunas.COLUNA_MES_DATA_CONTA + " < ? )" +
                " OR " +
                "(" + Colunas.COLUNA_ANO_DATA_CONTA + " = ? AND " + // Same year, same month, previous days
                Colunas.COLUNA_MES_DATA_CONTA + " = ? AND " +
                Colunas.COLUNA_DIA_DATA_CONTA + " < ? )" +
                " AND " + Colunas.COLUNA_PAGOU_CONTA + " = ?"; // Only update pending accounts

        String[] selectionArgs = new String[] {
                String.valueOf(ano), // For previous years
                String.valueOf(ano), String.valueOf(mes), // For same year, previous months
                String.valueOf(ano), String.valueOf(mes), String.valueOf(dia), // For same year, same month, previous days
                PAGAMENTO_FALTA // To ensure only pending items are updated
        };

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABELA_CONTAS, dadosConta, selection, selectionArgs);
            Log.d(TAG, "atualizaPagamentoContas: " + rowsAffected + " contas atualizadas para 'paguei'.");
        } catch (SQLException e) {
            Log.e(TAG, "Erro de SQL ao atualizar pagamento de contas: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Erro inesperado ao atualizar pagamento de contas: " + e.getMessage(), e);
        }
        return rowsAffected;
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
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
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
     * @deprecated Migrated to ContasRepository using Room.
     */
    @Deprecated
    public boolean deletarContasRecorrentes(long idConta, String codigoConta, int nrRepeticao, TipoExclusao tipoExclusao) {
        if (tipoExclusao == TipoExclusao.SOMENTE_ESTA) {
            return deletarConta(idConta); // Delegates to simple delete
        } else {
            ContaFilter filterDelecao = new ContaFilter();
            filterDelecao.codigoConta = codigoConta;

            if (tipoExclusao == TipoExclusao.DESTA_EM_DIANTE) {
                filterDelecao.nrRepeticaoMin = nrRepeticao; // Delete this and all greater repetitions
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
                    ContaFilter filterAtualizacao = new ContaFilter();
                    filterAtualizacao.codigoConta = codigoConta;
                    filterAtualizacao.nrRepeticaoMax = novoQtRepete; // Update only remaining ones

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

    /**
     * Calcula a soma de todas as aplicações anteriores ao período especificado, opcionalmente filtrada por classe.
     * @param dia Dia atual (usado apenas se isMonthly for false).
     * @param mes Mês atual.
     * @param ano Ano atual.
     * @param isMonthly Se o resumo é mensal ou diário.
     * @param classe Classe da aplicação (ou -1 para todas).
     * @return A soma das aplicações anteriores.
     */
    public double somaAplicacoesAnteriores(int dia, int mes, int ano, boolean isMonthly, int classe) {
        double total = 0.0;
        Cursor cursor = null;
        String[] projection = new String[]{"SUM(" + Colunas.COLUNA_VALOR_CONTA + ")"};

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>();

        selection.append(Colunas.COLUNA_TIPO_CONTA).append(" = ? AND (");
        selectionArgsList.add(String.valueOf(ContasContract.TIPO_APLICACAO));

        if (isMonthly) {
            selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" < ? OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" < ?))");
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
        } else {
            selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" < ? OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" < ?) OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_DIA_DATA_CONTA).append(" < ?))");
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
            selectionArgsList.add(String.valueOf(dia));
        }

        if (classe != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(classe));
        }

        try {
            cursor = db.query(TABELA_CONTAS, projection, selection.toString(),
                    selectionArgsList.toArray(new String[0]), null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Erro ao somar aplicações anteriores: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    /**
     * Calcula o saldo (Receitas - Despesas) acumulado de todos os períodos anteriores ao especificado.
     * @param dia Dia atual (usado apenas se isMonthly for false).
     * @param mes Mês atual.
     * @param ano Ano atual.
     * @param isMonthly Se o resumo é mensal ou diário.
     * @return O saldo acumulado anterior.
     */
    public double somaSaldoAnterior(int dia, int mes, int ano, boolean isMonthly) {
        double recAnterior = somaAnteriorPorTipo(dia, mes, ano, isMonthly, ContasContract.TIPO_RECEITA);
        double despAnterior = somaAnteriorPorTipo(dia, mes, ano, isMonthly, ContasContract.TIPO_DESPESA);
        return recAnterior - despAnterior;
    }

    private double somaAnteriorPorTipo(int dia, int mes, int ano, boolean isMonthly, int tipo) {
        double total = 0.0;
        Cursor cursor = null;
        String[] projection = new String[]{"SUM(" + Colunas.COLUNA_VALOR_CONTA + ")"};
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>();

        selection.append(Colunas.COLUNA_TIPO_CONTA).append(" = ? AND (");
        selectionArgsList.add(String.valueOf(tipo));

        if (isMonthly) {
            selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" < ? OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" < ?))");
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
        } else {
            selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" < ? OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" < ?) OR (")
                    .append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ? AND ")
                    .append(Colunas.COLUNA_DIA_DATA_CONTA).append(" < ?))");
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
            selectionArgsList.add(String.valueOf(ano));
            selectionArgsList.add(String.valueOf(mes));
            selectionArgsList.add(String.valueOf(dia));
        }

        try {
            cursor = db.query(TABELA_CONTAS, projection, selection.toString(),
                    selectionArgsList.toArray(new String[0]), null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Erro ao somar valores anteriores por tipo: " + e.getMessage());
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

    // --- MÉTODOS PARA GERENCIAMENTO DE NOTIFICAÇÕES ---

    /**
     * Insere uma nova notificação no banco de dados.
     * @deprecated Migrated to NotificationDao in Room.
     */
    @Deprecated
    public long addNotificacao(String titulo, String mensagem, String tipo) {
        if (db == null || !db.isOpen()) open();
        
        // Verifica se já existe uma notificação IDÊNTICA hoje (independente de estar lida ou não)
        // Isso evita que ao reabrir o app o sistema gere novos registros para o mesmo alerta
        String sql = "SELECT " + Notificacoes._ID + " FROM " + Notificacoes.TABELA_NOME +
                     " WHERE " + Notificacoes.COLUNA_TITULO + " = ? AND " + 
                     Notificacoes.COLUNA_MENSAGEM + " = ? AND " +
                     "date(" + Notificacoes.COLUNA_DATA + "/1000, 'unixepoch', 'localtime') = date('now', 'localtime')";
        
        Cursor c = db.rawQuery(sql, new String[]{titulo, mensagem});
        
        if (c != null && c.getCount() > 0) {
            c.close();
            return -2; // Já notificado hoje
        }
        if (c != null) c.close();

        ContentValues values = new ContentValues();
        values.put(Notificacoes.COLUNA_TITULO, titulo);
        values.put(Notificacoes.COLUNA_MENSAGEM, mensagem);
        values.put(Notificacoes.COLUNA_DATA, System.currentTimeMillis());
        values.put(Notificacoes.COLUNA_LIDA, 0);
        values.put(Notificacoes.COLUNA_TIPO, tipo);

        return db.insert(Notificacoes.TABELA_NOME, null, values);
    }

    /**
     * Retorna um cursor com todas as notificações NÃO lidas.
     */
    public Cursor getNotificacoesNaoLidasCursor() {
        if (db == null || !db.isOpen()) open();
        return db.query(Notificacoes.TABELA_NOME, null, Notificacoes.COLUNA_LIDA + " = 0", null, null, null, Notificacoes.COLUNA_DATA + " DESC");
    }

    /**
     * Verifica se existem notificações não lidas.
     */
    public boolean temNotificacoesNaoLidas() {
        if (db == null || !db.isOpen()) open();
        Cursor c = null;
        try {
            c = db.query(Notificacoes.TABELA_NOME, new String[]{Notificacoes._ID},
                    Notificacoes.COLUNA_LIDA + " = 0", null, null, null, null);
            return c != null && c.getCount() > 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Marca uma notificação específica como lida.
     * @deprecated Migrated to NotificationDao in Room.
     */
    @Deprecated
    public void marcarNotificacaoComoLida(long id) {
        if (db == null || !db.isOpen()) open();
        ContentValues values = new ContentValues();
        values.put(Notificacoes.COLUNA_LIDA, 1);
        db.update(Notificacoes.TABELA_NOME, values, Notificacoes._ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Marca todas as notificações como lidas.
     */
    public void marcarTodasNotificacoesComoLidas() {
        if (db == null || !db.isOpen()) open();
        ContentValues values = new ContentValues();
        values.put(Notificacoes.COLUNA_LIDA, 1);
        db.update(Notificacoes.TABELA_NOME, values, Notificacoes.COLUNA_LIDA + " = 0", null);
    }

    /**
     * Exclui todas as notificações.
     */
    public void excluirTodasNotificacoes() {
        if (db == null || !db.isOpen()) open();
        db.delete(Notificacoes.TABELA_NOME, null, null);
    }

    /**
     * Realiza a limpeza de notificações antigas (lidas há mais de 30 dias ou não lidas há mais de 60 dias).
     * Mantém o banco de dados leve.
     */
    public void limparNotificacoesAntigas() {
        if (db == null || !db.isOpen()) open();
        
        // Remove lidas com mais de 30 dias
        db.delete(Notificacoes.TABELA_NOME, 
                Notificacoes.COLUNA_LIDA + " = 1 AND " + 
                Notificacoes.COLUNA_DATA + " < ?", 
                new String[]{String.valueOf(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000))});
        
        // Remove não lidas com mais de 90 dias (segurança para não acumular lixo muito antigo)
        db.delete(Notificacoes.TABELA_NOME, 
                Notificacoes.COLUNA_DATA + " < ?", 
                new String[]{String.valueOf(System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000))});
        
        Log.d(TAG, "limparNotificacoesAntigas: Higiene do banco de notificações realizada.");
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

    public double somaValoresNoPeriodo(int diaInicio, int diaFim, int mes, int ano, int tipo, int classe, int categoria, String statusPagamento) {
        StringBuilder selection = new StringBuilder();
        ArrayList<String> selectionArgsList = new ArrayList<>();

        selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ? AND ");
        selectionArgsList.add(String.valueOf(ano));
        selection.append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ? AND ");
        selectionArgsList.add(String.valueOf(mes));
        selection.append(Colunas.COLUNA_DIA_DATA_CONTA).append(" BETWEEN ? AND ?");
        selectionArgsList.add(String.valueOf(diaInicio));
        selectionArgsList.add(String.valueOf(diaFim));

        if (tipo != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_TIPO_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(tipo));
        }
        if (classe != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_CLASSE_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(classe));
        }
        if (categoria != -1) {
            selection.append(" AND ").append(Colunas.COLUNA_CATEGORIA_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(categoria));
        }
        if (statusPagamento != null && !statusPagamento.isEmpty()) {
            selection.append(" AND ").append(Colunas.COLUNA_PAGOU_CONTA).append(" = ?");
            selectionArgsList.add(statusPagamento);
        }

        String[] colunasParaSoma = {"SUM(" + Colunas.COLUNA_VALOR_CONTA + ")"};
        double soma = 0.0;
        try (Cursor cursor = db.query(TABELA_CONTAS, colunasParaSoma, selection.toString(),
                selectionArgsList.toArray(new String[0]), null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                soma = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao somar valores no período: " + e.getMessage());
        }
        return soma;
    }

    public double somaValoresPorFiltro(int ano, int mes, int tipo, int classe, int categoria, String statusPagamento) {
        return somaValoresPorFiltro(ano, mes, tipo, classe, categoria, statusPagamento, -1);
    }

    /**
     * Soma os valores de contas com base em filtros complexos, incluindo o dia.
     */
    public double somaValoresPorFiltro(int ano, int mes, int tipo, int classe, int categoria, String statusPagamento, int dia) {
        StringBuilder selection = new StringBuilder();
        ArrayList<String> selectionArgsList = new ArrayList<>();

        // 1. FILTRO ANUAL (Obrigatório)
        selection.append(Colunas.COLUNA_ANO_DATA_CONTA).append(" = ?");
        selectionArgsList.add(String.valueOf(ano));

        // 2. FILTRO MENSAL
        if (mes >= 1 && mes <= 12) {
            selection.append(" AND ").append(Colunas.COLUNA_MES_DATA_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(mes));
        }

        // 2b. FILTRO POR DIA (Novo)
        if (dia > 0) {
            selection.append(" AND ").append(Colunas.COLUNA_DIA_DATA_CONTA).append(" = ?");
            selectionArgsList.add(String.valueOf(dia));
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
            cursor = db.query(TABELA_CONTAS, colunasParaSoma, selection.toString(),
                    selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
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

    /**
     * Calcula a média de gastos de uma categoria nos últimos meses.
     */
    public double getMediaCategoriaUltimosMeses(int categoria, int mesesAtras) {
        Calendar cal = Calendar.getInstance();
        double somaTotal = 0.0;
        int mesesContados = 0;

        for (int i = 1; i <= mesesAtras; i++) {
            cal.add(Calendar.MONTH, -1);
            int mes = cal.get(Calendar.MONTH) + 1;
            int ano = cal.get(Calendar.YEAR);
            
            double valorMes = somaValoresPorFiltro(ano, mes, ContasContract.TIPO_DESPESA, -1, categoria, null);
            if (valorMes > 0) {
                somaTotal += valorMes;
                mesesContados++;
            }
        }
        
        return mesesContados > 0 ? somaTotal / mesesContados : 0.0;
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
                    conta.setIdConta(id); // ATUALIZAÇÃO CRÍTICA: Define o ID gerado pelo banco no objeto
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

    /**
     * Updates multiple accounts by their IDs with the provided values.
     * Uses a transaction for atomicity and performance.
     *
     * @param ids    List of account IDs to update.
     * @param values ContentValues containing the columns to update and their new values.
     * @return The number of rows successfully updated.
     */
    public int atualizarContasEmMassa(List<Long> ids, ContentValues values) {
        if (ids == null || ids.isEmpty() || values == null || values.size() == 0) {
            return 0;
        }

        db.beginTransaction();
        int rowsUpdated = 0;
        try {
            for (Long id : ids) {
                if (db.update(TABELA_CONTAS, values, Colunas._ID + " = ?", new String[]{String.valueOf(id)}) > 0) {
                    rowsUpdated++;
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Erro na atualização em massa: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rowsUpdated;
    }


    // --- INNER CLASS: CONTAFILTER ---
    // (MOVED TO KOTLIN)

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
            db.execSQL(CRIA_TABELA_NOTIFICACOES);
        }

        /**
         * Checks if a table exists in the database.
         *
         * @param db        The SQLiteDatabase instance.
         * @param tableName The name of the table to check.
         * @return true if the table exists, false otherwise.
         */
        private boolean tableExists(SQLiteDatabase db, String tableName) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = ?", new String[]{tableName});
                return cursor != null && cursor.getCount() > 0;
            } catch (Exception e) {
                Log.e(TAG, "Error checking table existence for " + tableName + ": " + e.getMessage());
                return false;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        /**
         * Checks if a column exists in a given table.
         *
         * @param db         The SQLiteDatabase instance.
         * @param tableName  The name of the table.
         * @param columnName The name of the column to check.
         * @return true if the column exists, false otherwise.
         */
        private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
            if (!tableExists(db, tableName)) { // Check if table exists first
                return false;
            }
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        @SuppressLint("Range")
                        String name = cursor.getString(cursor.getColumnIndex("name"));
                        if (columnName.equalsIgnoreCase(name)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking column existence for " + columnName + " in table " + tableName + ": " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return false;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Iniciando atualização de v" + oldVersion + " para v" + newVersion);

            // Se a versão for antiga (ex: 4), executamos a lógica de migração segura
            if (oldVersion < 8) {
                migrarParaVersao8ComSeguranca(db);
            }

            if (oldVersion < 12) {
                if (!tableExists(db, Notificacoes.TABELA_NOME)) {
                    db.execSQL(CRIA_TABELA_NOTIFICACOES);
                    Log.i(TAG, "Tabela de notificações criada na atualização para v12.");
                }
            }
        }

        /**
         * Método robusto para migrar dados de versões antigas para a estrutura nova.
         * Utiliza a estratégia: RENAME -> CREATE NEW -> INSERT SELECT -> DROP OLD
         */
        private void migrarParaVersao8ComSeguranca(SQLiteDatabase db) {
            String tabelaTemp = "contas_backup_old";

            try {
                db.beginTransaction();

                // 1. Renomeia a tabela atual para um nome temporário (seguro)
                db.execSQL("ALTER TABLE " + TABELA_CONTAS + " RENAME TO " + tabelaTemp);

                // 2. Cria a NOVA tabela com a estrutura mais recente (v8+)
                db.execSQL(CRIA_TABELA_CONTAS);

                // 3. Mapeamento de compatibilidade para Versão 4 e anteriores
                // Lista de colunas na nova tabela
                List<String> colunasNovas = getColunasTabela(db, TABELA_CONTAS);
                List<String> colunasAntigas = getColunasTabela(db, tabelaTemp);

                StringBuilder insertCols = new StringBuilder();
                StringBuilder selectCols = new StringBuilder();

                for (String colNova : colunasNovas) {
                    if (colNova.equals(Colunas._ID)) continue; // Auto-incremento

                    String colCorrespondente = null;

                    // Tenta encontrar o nome exato ou o mapeamento legado
                    if (colunasAntigas.contains(colNova)) {
                        colCorrespondente = colNova;
                    } else {
                        // Mapeamento de nomes antigos (Versão 4 -> Versão 8+)
                        switch (colNova) {
                            case Colunas.COLUNA_NOME_CONTA: colCorrespondente = "nome"; break;
                            case Colunas.COLUNA_TIPO_CONTA: colCorrespondente = "tipo"; break;
                            case Colunas.COLUNA_CLASSE_CONTA: colCorrespondente = "classe"; break;
                            case Colunas.COLUNA_CATEGORIA_CONTA: colCorrespondente = "categoria"; break;
                            case Colunas.COLUNA_DIA_DATA_CONTA: colCorrespondente = "dia"; break;
                            case Colunas.COLUNA_MES_DATA_CONTA: colCorrespondente = "mes"; break;
                            case Colunas.COLUNA_ANO_DATA_CONTA: colCorrespondente = "ano"; break;
                            case Colunas.COLUNA_VALOR_CONTA: colCorrespondente = "valor"; break;
                            case Colunas.COLUNA_PAGOU_CONTA: colCorrespondente = "pagamento"; break;
                            case Colunas.COLUNA_QT_REPETICOES_CONTA: colCorrespondente = "qt_repete"; break;
                            case Colunas.COLUNA_NR_REPETICAO_CONTA: colCorrespondente = "n_repete"; break;
                            case Colunas.COLUNA_INTERVALO_CONTA: colCorrespondente = "intervalo"; break;
                            case Colunas.COLUNA_CODIGO_CONTA: colCorrespondente = "codigo"; break;
                            case Colunas.COLUNA_VALOR_JUROS: colCorrespondente = "0.0"; break; // Valor padrão para campo novo
                        }
                    }

                    if (colCorrespondente != null) {
                        if (insertCols.length() > 0) {
                            insertCols.append(", ");
                            selectCols.append(", ");
                        }
                        insertCols.append(colNova);
                        // Se o correspondente não for uma coluna real (ex: valor fixo 0.0), usamos como literal
                        if (colCorrespondente.equals("0.0")) {
                            selectCols.append("0.0");
                        } else if (colunasAntigas.contains(colCorrespondente)) {
                            selectCols.append(colCorrespondente);
                        } else {
                            // Se a coluna não existe na antiga, removemos do INSERT para usar o DEFAULT da tabela
                            insertCols.setLength(insertCols.length() - colNova.length() - (insertCols.length() > colNova.length() ? 2 : 0));
                            selectCols.setLength(selectCols.length() - (selectCols.length() > 0 ? 2 : 0));
                        }
                    }
                }

                if (insertCols.length() > 0) {
                    String sqlCopy = "INSERT INTO " + TABELA_CONTAS + " (" + insertCols + ") " +
                            "SELECT " + selectCols + " FROM " + tabelaTemp;
                    db.execSQL(sqlCopy);
                }

                // 4. Correções de Dados Específicas (Pós-cópia)
                // Correção do Mês (0-based para 1-based detectada na v4)
                try (Cursor c0 = db.rawQuery("SELECT 1 FROM " + TABELA_CONTAS + " WHERE " + Colunas.COLUNA_MES_DATA_CONTA + " = 0 LIMIT 1", null)) {
                    if (c0 != null && c0.moveToFirst()) {
                        db.execSQL("UPDATE " + TABELA_CONTAS + " SET " + Colunas.COLUNA_MES_DATA_CONTA +
                                " = " + Colunas.COLUNA_MES_DATA_CONTA + " + 1");
                        Log.i(TAG, "Migração: Meses corrigidos de 0-based para 1-based.");
                    }
                }

                // 5. Remove a tabela temporária
                db.execSQL("DROP TABLE IF EXISTS " + tabelaTemp);

                db.setTransactionSuccessful();
                Log.i(TAG, "Migração com mapeamento v4 -> v12 concluída.");

            } catch (Exception e) {
                Log.e(TAG, "Falha crítica na migração: " + e.getMessage());
                throw new RuntimeException("Erro ao converter banco de dados antigo: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        }

        /**
         * Helper para encontrar interseção de colunas entre duas tabelas.
         * Evita erros de "Column not found" ao tentar copiar dados.
         */
        private List<String> getColunasComuns(SQLiteDatabase db, String tabelaOrigem, String tabelaDestino) {
            List<String> colunasOrigem = getColunasTabela(db, tabelaOrigem);
            List<String> colunasDestino = getColunasTabela(db, tabelaDestino);

            colunasOrigem.retainAll(colunasDestino); // Interseção
            return colunasOrigem;
        }

        @SuppressLint("Range")
        private List<String> getColunasTabela(SQLiteDatabase db, String tabela) {
            List<String> colunas = new ArrayList<>();
            Cursor c = null;
            try {
                c = db.rawQuery("PRAGMA table_info(" + tabela + ")", null);
                if (c != null) {
                    while (c.moveToNext()) {
                        colunas.add(c.getString(c.getColumnIndex("name")));
                    }
                }
            } finally {
                if (c != null) c.close();
            }
            return colunas;
        }

    }
}