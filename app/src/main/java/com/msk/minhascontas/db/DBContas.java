package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.Vector;

public class DBContas {

    // Nomes das Colunas do Banco de Dados
    public static final String COLUNA_ANO_DATA_CONTA = "ano_data";
    public static final String COLUNA_CATEGORIA_CONTA = "classificacao";
    public static final String COLUNA_DATA_CONTA = "data";
    public static final String COLUNA_DIA_DATA_CONTA = "dia_data";
    public static final String COLUNA_DIA_REPETICAO_CONTA = "dia_repeticao";
    // NOVO VALOR ADICIONADO AO BANCO DE DADOS PARA ATUALIZAR
    public static final String COLUNA_ID_CONTA = "_id";
    public static final String COLUNA_MES_DATA_CONTA = "mes_data";
    public static final String COLUNA_NOME_CONTA = "conta";
    public static final String COLUNA_NR_REPETICAO_CONTA = "nr_repeticao";
    public static final String COLUNA_PAGOU_CONTA = "pagou";
    public static final String COLUNA_QT_REPETICOES_CONTA = "qt_repeticoes";
    public static final String COLUNA_TIPO_CONTA = "tipo_conta";
    public static final String COLUNA_VALOR_CONTA = "valor";
    // Nomes para criar o Bancos de Dados
    private static final String BANCO_DE_DADOS = "minhas_contas";
    private static final String TABELA_CONTAS = "contas";
    // Comando SQL para criar o Banco de Dados com as colunas
    private static final String CRIA_TABELA_CONTAS = "CREATE TABLE "
            + TABELA_CONTAS + " ( " + COLUNA_ID_CONTA
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUNA_NOME_CONTA
            + " TEXT NOT NULL," + COLUNA_TIPO_CONTA + " TEXT NOT NULL, "
            + COLUNA_CATEGORIA_CONTA + " TEXT NOT NULL," + COLUNA_PAGOU_CONTA
            + " TEXT NOT NULL, " + COLUNA_DATA_CONTA + " TEXT NOT NULL, "
            + COLUNA_DIA_DATA_CONTA + " INTEGER NOT NULL, "
            + COLUNA_MES_DATA_CONTA + " INTEGER NOT NULL, "
            + COLUNA_ANO_DATA_CONTA + " INTEGER NOT NULL, "
            + COLUNA_VALOR_CONTA + " REAL NOT NULL, "
            + COLUNA_QT_REPETICOES_CONTA + " INTEGER NOT NULL, "
            + COLUNA_NR_REPETICAO_CONTA + " INTEGER NOT NULL, "
            + COLUNA_DIA_REPETICAO_CONTA + " INTEGER NOT NULL);";
    private static final String TAG = "DBContas";
    // NOVO VALOR ADICIONADO AO BANCO DE DADOS PARA ATUALIZAR
    private static final int VERSAO_BANCO_DE_DADOS = 3;
    // NOMES DAS COLUNAS DAS TABELAS
    static String[] colunas_contas = {COLUNA_ID_CONTA, COLUNA_NOME_CONTA,
            COLUNA_TIPO_CONTA, COLUNA_CATEGORIA_CONTA, COLUNA_PAGOU_CONTA,
            COLUNA_DATA_CONTA, COLUNA_DIA_DATA_CONTA, COLUNA_MES_DATA_CONTA,
            COLUNA_ANO_DATA_CONTA, COLUNA_VALOR_CONTA,
            COLUNA_QT_REPETICOES_CONTA, COLUNA_NR_REPETICAO_CONTA,
            COLUNA_DIA_REPETICAO_CONTA};
    private static DatabaseHelper sInstance;
    // ELEMENTOS QUE GERENCIAM O BANCO DE DADOS
    private DatabaseHelper DBHelper;
    private Context contexto;
    private SQLiteDatabase db;

    public DBContas() {
        // CONSTRUTOR NECESSARIO
    }

    // CONSTRUTOR DA CLASSE QUE GERENCIA O BANCO DE DADOS
    public DBContas(Context ctx) {
        contexto = ctx;
        DBHelper = new DatabaseHelper(contexto);
    }

    // CONSTRUTOR ALTERNATIVO DE GESTAO DO BANCO DE DADOS
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DatabaseHelper(context.getApplicationContext());
        return sInstance;
    }

    // ABERTURA DA CONEXAO COM O BANCO DE DADOS
    @SuppressLint("Instantiatable")
    public DBContas open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    // FECHA CONEXAO COM O BANCO DE DADOS
    public void close() {
        DBHelper.close();
        db.close();
    }

    // --------- MÉTODOS DE INCLUEM, ALTERAM E EXCLUEM DADOS NA TABELA

    public long geraConta(String nome, String tipo, String classifica,
                          String pagamento, String data, int dia, int mes, int ano,
                          double valor, int qtRepete, int nRepete, int diaRepete) {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_NOME_CONTA, nome);
        dadosConta.put(COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(COLUNA_CATEGORIA_CONTA, classifica);
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        dadosConta.put(COLUNA_DATA_CONTA, data);
        dadosConta.put(COLUNA_DIA_DATA_CONTA, Integer.valueOf(dia));
        dadosConta.put(COLUNA_MES_DATA_CONTA, Integer.valueOf(mes));
        dadosConta.put(COLUNA_ANO_DATA_CONTA, Integer.valueOf(ano));
        dadosConta.put(COLUNA_VALOR_CONTA, Double.valueOf(valor));
        dadosConta.put(COLUNA_QT_REPETICOES_CONTA, Integer.valueOf(qtRepete));
        dadosConta.put(COLUNA_NR_REPETICAO_CONTA, Integer.valueOf(nRepete));
        dadosConta.put(COLUNA_DIA_REPETICAO_CONTA, Integer.valueOf(diaRepete));
        return db.insert(TABELA_CONTAS, null, dadosConta);
    }

    // ----------- MÉTODOS QUE ALTERAM DADOS DAS CONTAS NO BANCO DE DADOS

    public boolean alteraClasseConta(long idConta, String classeConta)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_CATEGORIA_CONTA, classeConta);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;
    }

    public boolean alteraDataConta(long idConta, String data, int dia, int mes,
                                   int ano) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_DATA_CONTA, data);
        dadosConta.put(COLUNA_DIA_DATA_CONTA, Integer.valueOf(dia));
        dadosConta.put(COLUNA_MES_DATA_CONTA, Integer.valueOf(mes));
        dadosConta.put(COLUNA_ANO_DATA_CONTA, Integer.valueOf(ano));
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;
    }

    public boolean alteraNomeConta(long idConta, String nome)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_NOME_CONTA, nome);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;
    }

    public boolean alteraTipoConta(long idConta, String tipo)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_TIPO_CONTA, tipo);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;

    }

    public boolean alteraValorConta(long idConta, double valor, String pagamento)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_VALOR_CONTA, Double.valueOf(valor));
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;

    }

    public boolean alteraPagamentoConta(long idConta, String pagamento)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;

    }

    // -------- MÉTODO QUE ALTERA DADOS EM MAIS DE UMA CONTA

    public boolean atualizaDataContas(String nome, String dma, int nr)
            throws SQLException {
        ContentValues dataContas = new ContentValues();
        nome = nome.replace("'", "''");
        dataContas.put(COLUNA_DATA_CONTA, dma);
        return db.update(TABELA_CONTAS, dataContas, COLUNA_NOME_CONTA + " = '"
                + nome + "' AND " + COLUNA_QT_REPETICOES_CONTA + " = '" + nr
                + "' ", null) > 0;

    }

    public boolean atualizaPagamentoContas(int dia, int mes, int ano)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_PAGOU_CONTA, "paguei");
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_DIA_DATA_CONTA
                        + " < '" + dia + "' AND " + COLUNA_MES_DATA_CONTA + " = '"
                        + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano
                        + "' OR " + COLUNA_ANO_DATA_CONTA + " < '" + ano + "'",
                null) > 0;

    }

    public boolean confirmaPagamentos() throws SQLException {
        ContentValues dadosConta = new ContentValues();
        String pg = "paguei";
        dadosConta.put(COLUNA_PAGOU_CONTA, "falta");
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_PAGOU_CONTA
                + " != '" + pg + "' ", null) > 0;

    }

    public boolean ajustaRepeticoesContas() throws SQLException {
        ContentValues dadosConta = new ContentValues();
        int intervalo = 300;
        int dia = 31;
        dadosConta.put(COLUNA_DIA_REPETICAO_CONTA, intervalo);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_DIA_REPETICAO_CONTA
                + " <= '" + dia + "' ", null) > 0;

    }

    // ----------- MÉTODOS QUE EXCLUEM AS CONTAS NO BANCO DE DADOS

    public boolean excluiConta(long idConta) {
        return db.delete(TABELA_CONTAS, COLUNA_ID_CONTA + " = '" + idConta
                + "' ", null) > 0;
    }

    public boolean excluiContaPorNome(String nome, String dma) {
        nome = nome.replace("'", "''");
        return db.delete(TABELA_CONTAS, COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + COLUNA_DATA_CONTA + " = '" + dma + "' ", null) > 0;
    }

    public boolean excluiSerieContaPorNome(String nome, String dma,
                                           int nr_repete) {
        nr_repete = nr_repete - 1;
        nome = nome.replace("'", "''");
        return db.delete(TABELA_CONTAS, COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + COLUNA_DATA_CONTA + " = '" + dma + "' AND "
                + COLUNA_NR_REPETICAO_CONTA + " > '" + nr_repete + "' ", null) > 0;
    }

    public void excluiTodasAsContas() {
        db.delete(TABELA_CONTAS, null, null);
    }

    // ----------- MÉTODOS QUE BUSCAM AS CONTAS NO BANCO DE DADOS

    public Cursor buscaContas(int dia, int mes, int ano, String ordem) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                    + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                    + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                    + ano + "' ", null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                    + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                    + ano + "' ", null, null, null, ordem);
    }

    public Cursor buscaContasTipo(int dia, int mes, int ano, String ordem,
                                  String tipo) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo + "' ",
                    null, null, null, ordem);
    }

    public Cursor buscaContasTipoPagamento(int dia, int mes, int ano, String ordem,
                                           String tipo, String pagamento) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                        + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                        + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                        + "' AND " + COLUNA_PAGOU_CONTA + " = '" + pagamento + "' ",
                null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + COLUNA_PAGOU_CONTA + " = '" + pagamento + "' ",
                    null, null, null, ordem);
    }

    public Cursor buscaContasClasse(int dia, int mes, int ano, String ordem,
                                    String tipo, String classe) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                        + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                        + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                        + "' AND " + COLUNA_CATEGORIA_CONTA + " = '" + classe + "' ",
                null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + COLUNA_CATEGORIA_CONTA + " = '" + classe + "' ",
                    null, null, null, ordem);
    }

    public Cursor buscaContasPorNome(String nome) {
        nome = nome.replace("'", "''");
        return db.query(TABELA_CONTAS, colunas_contas, COLUNA_NOME_CONTA
                + " = '" + nome + "' ", null, null, null, COLUNA_NOME_CONTA
                + " ASC");
    }

    // ----------- MÉTODOS QUE MOSTRAM AS CONTAS DO BANCO DE DADOS

    public String mostraContasPorTipo(String tipo, int mes, int ano)
            throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas,
                COLUNA_TIPO_CONTA + " = '" + tipo + "' AND "
                        + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                        + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                null, null, COLUNA_NOME_CONTA + " ASC ");

        int i = cursor.getColumnIndex(COLUNA_NOME_CONTA);
        int j = cursor.getColumnIndex(COLUNA_VALOR_CONTA);
        String str = tipo + " do mês:\n";
        cursor.moveToFirst();
        while (true) {
            if (cursor.isAfterLast()) {
                cursor.close();
                return str;
            }
            str = str + "R$ " + String.format(Locale.US, "%.2f", cursor.getDouble(j))
                    + " de " + cursor.getString(i) + ";\n";
            cursor.moveToNext();
        }
    }

    public int[] mostraDMAConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        int[] arrayOfInt = null;
        if (cursor != null && cursor.moveToFirst()) {
            int i = cursor.getInt(6);
            int j = cursor.getInt(7);
            int k = cursor.getInt(8);
            arrayOfInt = new int[3];
            arrayOfInt[0] = i;
            arrayOfInt[1] = j;
            arrayOfInt[2] = k;
        }
        cursor.close();
        return arrayOfInt;

    }

    public String mostraDataConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String dConta = "";
        if (cursor != null && cursor.moveToFirst()) {
            dConta = cursor.getString(5);
        }
        cursor.close();
        return dConta;

    }

    public long mostraPrimeiraRepeticaoConta(String nome, int nrRepete)
            throws SQLException {
        nome = nome.replace("'", "''");
        Cursor cursor = db
                .query(TABELA_CONTAS, colunas_contas, COLUNA_NOME_CONTA
                        + " = '" + nome + "' AND " + COLUNA_QT_REPETICOES_CONTA
                        + " = '" + nrRepete + "' ", null, null, null, null);
        long u = 0;
        if (cursor != null && cursor.moveToFirst()) {
            u = cursor.getLong(0);
        }
        cursor.close();
        return u;
    }

    public String[] mostraDadosConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String[] arrayOfString = null;
        if (cursor != null && cursor.moveToFirst()) {
            String str1 = cursor.getString(1);
            String str2 = cursor.getString(2);
            String str3 = cursor.getString(3);
            String str4 = cursor.getString(4);
            String str5 = cursor.getString(5);
            arrayOfString = new String[5];
            arrayOfString[0] = str1;
            arrayOfString[1] = str2;
            arrayOfString[2] = str3;
            arrayOfString[3] = str4;
            arrayOfString[4] = str5;
        }
        cursor.close();
        return arrayOfString;

    }

    public String mostraNomeConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String str = " ";
        if (cursor != null && cursor.moveToFirst())
            str = cursor.getString(1);
        cursor.close();
        return str;
    }

    public Vector<String> mostraNomeContas() throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, null, null,
                null, null, COLUNA_NOME_CONTA + " ASC ");
        Vector<String> v = new Vector<String>();
        String str = " ";
        if (cursor != null && cursor.moveToFirst())
            str = cursor.getString(1);
        v.add(cursor.getString(1));
        while (!cursor.isAfterLast()) {

            if (!str.equals(cursor.getString(1)))
                v.add(cursor.getString(1));
            str = cursor.getString(1);
            cursor.moveToNext();

        }
        cursor.close();
        return v;
    }

    public int[] mostraRepeticaoConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        int[] arrayOfInt = null;
        if (cursor != null && cursor.moveToFirst()) {
            int i = cursor.getInt(10);
            int j = cursor.getInt(11);
            int k = cursor.getInt(12);
            arrayOfInt = new int[3];
            arrayOfInt[0] = i;
            arrayOfInt[1] = j;
            arrayOfInt[2] = k;
        }
        cursor.close();
        return arrayOfInt;

    }

    public double mostraValorConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        double d = 0.0D;
        if (cursor != null && cursor.moveToFirst())
            d = cursor.getDouble(9);
        cursor.close();
        return d;
    }

    public String mostraPagamentoConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String pg = "";
        if (cursor != null && cursor.moveToFirst())
            pg = cursor.getString(4);
        cursor.close();
        return pg;
    }

    // - MÉTODOS QUE CONTAM QUANTAS CONTAS EXISTEM NO BANCO DE DADOS

    public int quantasContas() {
        Cursor cursor;
        cursor = db.query(true, TABELA_CONTAS, colunas_contas, null, null,
                null, null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPagasPorTipo(String tipo, String pagamento,
                                         int dia, int mes, int ano) {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                    + " = '" + tipo + "' AND " + COLUNA_PAGOU_CONTA + " = '"
                    + pagamento + "' AND " + COLUNA_MES_DATA_CONTA + " = '"
                    + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano
                    + "' ", null, null, null, null);
        else
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                            + " = '" + tipo + "' AND " + COLUNA_PAGOU_CONTA + " = '"
                            + pagamento + "' AND " + COLUNA_DIA_DATA_CONTA + " < '"
                            + dia + "' AND " + COLUNA_MES_DATA_CONTA + " = '" + mes
                            + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPorClasse(String classe, int dia, int mes, int ano) {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + classe + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        else if (ano != 0) {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + classe + "' AND "
                            + COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        } else {
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + classe + "'", null, null,
                    null, null);
        }
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPorMes(int mes, int ano) {
        Cursor cursor = db.query(true, TABELA_CONTAS, colunas_contas,
                COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                        + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                null, null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPorTipo(String tipo, int dia, int mes, int ano) {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                    + " = '" + tipo + "' AND " + COLUNA_MES_DATA_CONTA + " = '"
                    + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano
                    + "' ", null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                            + " = '" + tipo + "' AND " + COLUNA_DIA_DATA_CONTA + " < '"
                            + dia + "' AND " + COLUNA_MES_DATA_CONTA + " = '" + mes
                            + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        }
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPorNome(String nome) {
        nome = nome.replace("'", "''");
        Cursor cursor = db
                .query(TABELA_CONTAS, colunas_contas, COLUNA_NOME_CONTA
                        + " = '" + nome + "'", null, null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPorNomeNoDia(String nome, int dia, int mes, int ano) {
        nome = nome.replace("'", "''");
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas,
                COLUNA_NOME_CONTA + " = '" + nome + "' AND "
                        + COLUNA_DIA_DATA_CONTA + " = '" + dia + "' AND "
                        + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                        + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ", null,
                null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    // ----------- MÉTODOS QUE SOMAM AS CONTAS DO BANCO DE DADOS

    public double somaContas(String tipo, int dia, int mes, int ano)
            throws SQLException {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                    + " = '" + tipo + "' AND " + COLUNA_MES_DATA_CONTA + " = '"
                    + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano
                    + "' ", null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                            + " = '" + tipo + "' AND " + COLUNA_DIA_DATA_CONTA + " < '"
                            + dia + "' AND " + COLUNA_MES_DATA_CONTA + " = '" + mes
                            + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        }
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        for (int j = 0; ; j++) {
            if (j >= i) {
                cursor.close();
                return d;
            }
            d += cursor.getDouble(9);
            cursor.moveToPrevious();
        }
    }

    public double somaContasPagas(String tipo, String pagamento, int dia,
                                  int mes, int ano) throws SQLException {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                    + " = '" + tipo + "' AND " + COLUNA_PAGOU_CONTA + " = '"
                    + pagamento + "' AND " + COLUNA_MES_DATA_CONTA + " = '"
                    + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano
                    + "' ", null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_TIPO_CONTA
                            + " = '" + tipo + "' AND " + COLUNA_PAGOU_CONTA + " = '"
                            + pagamento + "' AND " + COLUNA_DIA_DATA_CONTA + " < '"
                            + dia + "' AND " + COLUNA_MES_DATA_CONTA + " = '" + mes
                            + "' AND " + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        }
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        for (int j = 0; ; j++) {
            if (j >= i) {
                cursor.close();
                return d;
            }
            d += cursor.getDouble(9);
            cursor.moveToPrevious();
        }
    }

    public double somaContasPorClasse(String classe, int dia, int mes, int ano)
            throws SQLException {

        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + classe + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + classe + "' AND "
                            + COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        }
        int i = cursor.getCount();
        cursor.moveToLast();
        double d = 0.0D;
        for (int j = 0; ; j++) {
            if (j >= i) {
                cursor.close();
                return d;
            }
            d += cursor.getDouble(9);
            cursor.moveToPrevious();
        }
    }

    // ------------ CLASSE PRIVATIVA PARA CRIAÇÃO DO BANCO DE DADOS -------

    public void copiaBD(String pasta) {

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (!pasta.equals(""))
                sd = new File(pasta);

            if (sd.canWrite()) {
                String currentDBPath = "//data//com.msk.minhascontas//databases//minhas_contas";
                String backupDBPath = "minhas_contas";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB)
                            .getChannel();
                    FileChannel dst = new FileOutputStream(backupDB)
                            .getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
        }
    }

    // ----- Procedimentos para fazer BACKUP e RESTARURAR BACKUP

    @SuppressWarnings("resource")
    public void restauraBD(String pasta) {

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (!pasta.equals(""))
                sd = new File(pasta);

            if (sd.canWrite()) {
                String currentDBPath = "//data//com.msk.minhascontas//databases//minhas_contas";
                //String backupDBPath = "minhas_contas";
                File currentDB = new File(data, currentDBPath);
                //File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(sd)
                            .getChannel();
                    FileChannel dst = new FileOutputStream(currentDB)
                            .getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {

        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, BANCO_DE_DADOS, null, VERSAO_BANCO_DE_DADOS);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            if (!db.isReadOnly()) {
                db.execSQL("PRAGMA foreign_keys=ON;");
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CRIA_TABELA_CONTAS);
            Log.w(TAG, "DB criado com sucesso!");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
            Log.w(TAG, "Atualizando o banco de dados da versao " + arg1
                    + " para " + arg2 + ", todos os dados serao perdidos!");
            // db.execSQL("DROP TABLE IF EXISTS " + TABELA_CONTAS);
            // onCreate(db);
        }

    }

}
