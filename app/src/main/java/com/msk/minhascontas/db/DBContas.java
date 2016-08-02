package com.msk.minhascontas.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.msk.minhascontas.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.Vector;

public class DBContas {

    // Nomes das Colunas do Banco de Dados
    private static final String COLUNA_ANO_DATA_CONTA = "ano_data";
    private static final String COLUNA_CODIGO_CONTA = "codigo";
    private static final String COLUNA_DIA_DATA_CONTA = "dia_data";
    private static final String COLUNA_INTERVALO_CONTA = "dia_repeticao";
    // NOVO VALOR ADICIONADO AO BANCO DE DADOS PARA ATUALIZAR
    private static final String COLUNA_ID_CONTA = "_id";
    private static final String COLUNA_MES_DATA_CONTA = "mes_data";
    private static final String COLUNA_NOME_CONTA = "conta";
    private static final String COLUNA_NR_REPETICAO_CONTA = "nr_repeticao";
    private static final String COLUNA_PAGOU_CONTA = "pagou";
    private static final String COLUNA_QT_REPETICOES_CONTA = "qt_repeticoes";
    private static final String COLUNA_TIPO_CONTA = "tipo_conta";
    private static final String COLUNA_CLASSE_CONTA = "classe_conta";
    private static final String COLUNA_CATEGORIA_CONTA = "categoria_conta";
    private static final String COLUNA_VALOR_CONTA = "valor";
    // Nomes para criar o Bancos de Dados
    private static final String BANCO_DE_DADOS = "minhas_contas";
    private static final String TABELA_CONTAS = "contas";
    // Comando SQL para criar o Banco de Dados com as colunas
    private static final String CRIA_TABELA_CONTAS = "CREATE TABLE " + TABELA_CONTAS + " ( "
            + COLUNA_ID_CONTA + " INTEGER PRIMARY KEY AUTOINCREMENT," // coluna 0
            + COLUNA_NOME_CONTA + " TEXT NOT NULL,"                   // coluna 1
            + COLUNA_TIPO_CONTA + " INTEGER NOT NULL, "               // coluna 2
            + COLUNA_CLASSE_CONTA + " INTEGER NOT NULL,"              // coluna 3
            + COLUNA_CATEGORIA_CONTA + " TEXT, "                      // coluna 4
            + COLUNA_DIA_DATA_CONTA + " INTEGER NOT NULL, "           // coluna 5
            + COLUNA_MES_DATA_CONTA + " INTEGER NOT NULL, "           // coluna 6
            + COLUNA_ANO_DATA_CONTA + " INTEGER NOT NULL, "           // coluna 7
            + COLUNA_VALOR_CONTA + " REAL NOT NULL, "                 // coluna 8
            + COLUNA_PAGOU_CONTA + " TEXT NOT NULL, "                 // coluna 9
            + COLUNA_QT_REPETICOES_CONTA + " INTEGER NOT NULL, "      // coluna 10
            + COLUNA_NR_REPETICAO_CONTA + " INTEGER NOT NULL, "       // coluna 11
            + COLUNA_INTERVALO_CONTA + " INTEGER NOT NULL,"           // coluna 12
            + COLUNA_CODIGO_CONTA + " TEXT NOT NULL );";              // coluna 13

    private static final String TAG = "DBContas";
    // NOVO VALOR ADICIONADO AO BANCO DE DADOS PARA ATUALIZAR
    private static final int VERSAO_BANCO_DE_DADOS = 4;
    // NOMES DAS COLUNAS DAS TABELAS
    private static String[] colunas_contas = {COLUNA_ID_CONTA, COLUNA_NOME_CONTA,
            COLUNA_TIPO_CONTA, COLUNA_CLASSE_CONTA, COLUNA_CATEGORIA_CONTA,
            COLUNA_DIA_DATA_CONTA, COLUNA_MES_DATA_CONTA, COLUNA_ANO_DATA_CONTA,
            COLUNA_VALOR_CONTA, COLUNA_PAGOU_CONTA, COLUNA_QT_REPETICOES_CONTA,
            COLUNA_NR_REPETICAO_CONTA, COLUNA_INTERVALO_CONTA, COLUNA_CODIGO_CONTA};
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

    public long geraConta(String nome, int tipo, int classeConta, int categoria,
                          int dia, int mes, int ano, double valor, String pagamento, int qtRepete,
                          int nRepete, int intervalo, String codigo) {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_NOME_CONTA, nome);
        dadosConta.put(COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(COLUNA_CLASSE_CONTA, classeConta);
        dadosConta.put(COLUNA_CATEGORIA_CONTA, categoria);
        dadosConta.put(COLUNA_DIA_DATA_CONTA, dia);
        dadosConta.put(COLUNA_MES_DATA_CONTA, mes);
        dadosConta.put(COLUNA_ANO_DATA_CONTA, ano);
        dadosConta.put(COLUNA_VALOR_CONTA, valor);
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        dadosConta.put(COLUNA_QT_REPETICOES_CONTA, qtRepete);
        dadosConta.put(COLUNA_NR_REPETICAO_CONTA, nRepete);
        dadosConta.put(COLUNA_INTERVALO_CONTA, intervalo);
        dadosConta.put(COLUNA_CODIGO_CONTA, codigo);
        return db.insert(TABELA_CONTAS, null, dadosConta);
    }

    // ----------- MÉTODOS QUE ALTERAM DADOS DAS CONTAS NO BANCO DE DADOS

    public boolean alteraDadosConta(long idConta, String nome, int tipo, int classeConta, int categoria,
                                    int dia, int mes, int ano, double valor, String pagamento, int qtRepete,
                                    int nRepete, int intervalo, String codigo) {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_NOME_CONTA, nome);
        dadosConta.put(COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(COLUNA_CLASSE_CONTA, classeConta);
        dadosConta.put(COLUNA_CATEGORIA_CONTA, categoria);
        dadosConta.put(COLUNA_DIA_DATA_CONTA, dia);
        dadosConta.put(COLUNA_MES_DATA_CONTA, mes);
        dadosConta.put(COLUNA_ANO_DATA_CONTA, ano);
        dadosConta.put(COLUNA_VALOR_CONTA, valor);
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        dadosConta.put(COLUNA_QT_REPETICOES_CONTA, qtRepete);
        dadosConta.put(COLUNA_NR_REPETICAO_CONTA, nRepete);
        dadosConta.put(COLUNA_INTERVALO_CONTA, intervalo);
        dadosConta.put(COLUNA_CODIGO_CONTA, codigo);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;
    }

    public boolean alteraNomeContas(String nomeNovo, String nomeAntigo, String codigo, int nrRepete)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''");
        nrRepete = nrRepete - 1;
        dadosConta.put(COLUNA_NOME_CONTA, nomeNovo);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    public boolean alteraTipoContas(int tipo, int classeConta, int categoria, String nomeAntigo,
                                    String codigo, int nrRepete) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''");
        nrRepete = nrRepete - 1;
        dadosConta.put(COLUNA_TIPO_CONTA, tipo);
        dadosConta.put(COLUNA_CLASSE_CONTA, classeConta);
        dadosConta.put(COLUNA_CATEGORIA_CONTA, categoria);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    public boolean alteraValorContas(double valor, String pagamento, String nomeAntigo,
                                     String codigo, int nrRepete) throws SQLException {
        ContentValues dadosConta = new ContentValues();
        nomeAntigo = nomeAntigo.replace("'", "''");
        nrRepete = nrRepete - 1;
        dadosConta.put(COLUNA_VALOR_CONTA, valor);
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_NOME_CONTA + " = '" + nomeAntigo + "' AND "
                + COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND " + COLUNA_NR_REPETICAO_CONTA
                + " > " + nrRepete + " ", null) > 0;
    }

    public boolean alteraPagamentoConta(long idConta, String pagamento)
            throws SQLException {
        ContentValues dadosConta = new ContentValues();
        dadosConta.put(COLUNA_PAGOU_CONTA, pagamento);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_ID_CONTA + " = '"
                + idConta + "' ", null) > 0;
    }

    // -------- MÉTODO QUE ALTERA DADOS EM MAIS DE UMA CONTA

    public boolean atualizaDataContas(String nome, String codigo, int nr)
            throws SQLException {
        ContentValues dataContas = new ContentValues();
        nome = nome.replace("'", "''");
        dataContas.put(COLUNA_CODIGO_CONTA, codigo);
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
        dadosConta.put(COLUNA_INTERVALO_CONTA, intervalo);
        return db.update(TABELA_CONTAS, dadosConta, COLUNA_INTERVALO_CONTA
                + " <= '" + dia + "' ", null) > 0;
    }

    // ----------- MÉTODOS QUE EXCLUEM AS CONTAS NO BANCO DE DADOS

    public boolean excluiConta(long idConta) {
        return db.delete(TABELA_CONTAS, COLUNA_ID_CONTA + " = '" + idConta
                + "' ", null) > 0;
    }

    public boolean excluiContaPorNome(String nome, String codigo) {
        nome = nome.replace("'", "''");
        return db.delete(TABELA_CONTAS, COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + COLUNA_CODIGO_CONTA + " = '" + codigo + "' ", null) > 0;
    }

    public boolean excluiSerieContaPorNome(String nome, String codigo,
                                           int nr_repete) {
        nr_repete = nr_repete - 1;
        nome = nome.replace("'", "''");
        return db.delete(TABELA_CONTAS, COLUNA_NOME_CONTA + " = '" + nome
                + "' AND " + COLUNA_CODIGO_CONTA + " = '" + codigo + "' AND "
                + COLUNA_NR_REPETICAO_CONTA + " > '" + nr_repete + "' ", null) > 0;
    }

    public void excluiTodasAsContas() {
        db.delete(TABELA_CONTAS, null, null);
    }

    // ----------- MÉTODOS QUE BUSCAM AS CONTAS NO BANCO DE DADOS

    public Cursor buscaUmaConta(long idConta) {
        return db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
    }

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
                                  int tipo) {
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
                                           int tipo, String pagamento) {
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
                                    int tipo, int classe) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + COLUNA_CLASSE_CONTA + " = '" + classe + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = '" + tipo
                            + "' AND " + COLUNA_CLASSE_CONTA + " = '" + classe + "' ",
                    null, null, null, ordem);
    }

    public Cursor buscaContasCategoria(int dia, int mes, int ano, String ordem, int categoria) {
        if (dia != 0)
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_DIA_DATA_CONTA
                            + " = '" + dia + "' AND " + COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = 0 AND "
                            + COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' ",
                    null, null, null, ordem);
        else
            return db.query(TABELA_CONTAS, colunas_contas, COLUNA_MES_DATA_CONTA
                            + " = '" + mes + "' AND " + COLUNA_ANO_DATA_CONTA + " = '"
                            + ano + "' AND " + COLUNA_TIPO_CONTA + " = 0 AND "
                            + COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' ",
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
        String str = tipo + " do mês:\n";
        cursor.moveToFirst();
        while (true) {
            if (cursor.isAfterLast()) {
                cursor.close();
                return str;
            }
            str = str + "R$ " + String.format(Locale.US, "%.2f", cursor.getDouble(8))
                    + " de " + cursor.getString(1) + ";\n";
            cursor.moveToNext();
        }
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

    public int[] mostraDMAConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        int[] arrayOfInt = null;
        if (cursor != null && cursor.moveToFirst()) {
            int i = cursor.getInt(5);
            int j = cursor.getInt(6);
            int k = cursor.getInt(7);
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
            d = cursor.getDouble(8);
        cursor.close();
        return d;
    }

    public String mostraPagamentoConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String pg = "";
        if (cursor != null && cursor.moveToFirst())
            pg = cursor.getString(9);
        cursor.close();
        return pg;
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

    public long mostraPrimeiraRepeticaoConta(String nome, int qtRepete, String codigo)
            throws SQLException {
        nome = nome.replace("'", "''");
        Cursor cursor = db
                .query(TABELA_CONTAS, colunas_contas, COLUNA_NOME_CONTA
                        + " = '" + nome + "' AND " + COLUNA_QT_REPETICOES_CONTA
                        + " = '" + qtRepete + "' AND " + COLUNA_CODIGO_CONTA
                        + " = '" + codigo + "' ", null, null, null, null);
        long u = 0;
        if (cursor != null && cursor.moveToFirst()) {
            u = cursor.getLong(0);
        }
        cursor.close();
        return u;
    }

    public String mostraCodigoConta(long idConta) throws SQLException {
        Cursor cursor = db.query(TABELA_CONTAS, colunas_contas, COLUNA_ID_CONTA
                + " = '" + idConta + "' ", null, null, null, null);
        String dConta = "";
        if (cursor != null && cursor.moveToFirst()) {
            dConta = cursor.getString(13);
        }
        cursor.close();
        return dConta;
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

    // - MÉTODOS QUE CONTAM QUANTAS CONTAS EXISTEM NO BANCO DE DADOS

    public int quantasContas() {
        Cursor cursor;
        cursor = db.query(true, TABELA_CONTAS, colunas_contas, null, null,
                null, null, null, null);
        int i = cursor.getCount();
        cursor.close();
        return i;
    }

    public int quantasContasPagasPorTipo(int tipo, String pagamento,
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

    public int quantasContasPorClasse(int classe, int dia, int mes, int ano) {
        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        else if (ano != 0) {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                            + COLUNA_DIA_DATA_CONTA + " < '" + dia + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        } else {
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CLASSE_CONTA + " = '" + classe + "'", null, null,
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

    public int quantasContasPorTipo(int tipo, int dia, int mes, int ano) {
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

    public int quantasRepeticoesDaConta(String nome, String codigo) {
        nome = nome.replace("'", "''");
        Cursor cursor = db
                .query(TABELA_CONTAS, colunas_contas, COLUNA_NOME_CONTA
                                + " = '" + nome + "' AND "
                                + COLUNA_CODIGO_CONTA + " = '" + codigo + "' ",
                        null, null, null, null);
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

    public double somaContas(int tipo, int dia, int mes, int ano)
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
            d += cursor.getDouble(8);
            cursor.moveToPrevious();
        }
    }

    public double somaContasPagas(int tipo, String pagamento, int dia,
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
            d += cursor.getDouble(8);
            cursor.moveToPrevious();
        }
    }

    public double somaContasPorClasse(int classe, int dia, int mes, int ano)
            throws SQLException {

        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CLASSE_CONTA + " = '" + classe + "' AND "
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
            d += cursor.getDouble(8);
            cursor.moveToPrevious();
        }
    }

    public double somaContasPorCategoria(int categoria, int dia, int mes, int ano)
            throws SQLException {

        Cursor cursor;
        if (dia == 0)
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' AND "
                            + COLUNA_MES_DATA_CONTA + " = '" + mes + "' AND "
                            + COLUNA_ANO_DATA_CONTA + " = '" + ano + "' ",
                    null, null, null, null);
        else {
            dia = dia + 1;
            cursor = db.query(TABELA_CONTAS, colunas_contas,
                    COLUNA_CATEGORIA_CONTA + " = '" + categoria + "' AND "
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
            d += cursor.getDouble(8);
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
                String backupDBPath = BANCO_DE_DADOS;
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
            Log.w(TAG, "Erro ao guardar dados");
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
                //String backupDBPath = BANCO_DE_DADOS;
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
            Log.w(TAG, "Erro ao recuperar dados");
        }
    }

    public void atualizaBD() throws SQLException {

        try {

            String[] colunas = {COLUNA_ID_CONTA, COLUNA_NOME_CONTA,
                    COLUNA_TIPO_CONTA, COLUNA_CLASSE_CONTA, COLUNA_CATEGORIA_CONTA,
                    COLUNA_DIA_DATA_CONTA, COLUNA_MES_DATA_CONTA, COLUNA_ANO_DATA_CONTA,
                    COLUNA_VALOR_CONTA, COLUNA_PAGOU_CONTA, COLUNA_QT_REPETICOES_CONTA,
                    COLUNA_NR_REPETICAO_CONTA, COLUNA_INTERVALO_CONTA, COLUNA_CODIGO_CONTA,
                    "auxiliar", "classifica"};

            Cursor cursor = db.query("tabela_temporaria", colunas, null, null, null, null, null);
            cursor.moveToLast();
            long idLong;
            ContentValues cv;
            Resources res = contexto.getResources();
            String[] rec = res.getStringArray(R.array.TipoReceita);
            String[] desp = res.getStringArray(R.array.TipoDespesa);
            String[] apl = res.getStringArray(R.array.TipoAplicacao);

            for (int i = 0; i < cursor.getCount(); i++) {
                cv = new ContentValues();
                idLong = cursor.getLong(0);
                if (cursor.getString(14).equals(res.getString(R.string.linha_despesa))) {
                    cv.put(COLUNA_TIPO_CONTA, 0);
                    for (int j = 0; j < desp.length; j++) {
                        if (cursor.getString(15).equals(desp[j])) {
                            cv.put(COLUNA_CLASSE_CONTA, j);
                        }
                    }
                    cv.put(COLUNA_CATEGORIA_CONTA, 7);
                } else if (cursor.getString(14).equals(res.getString(R.string.linha_receita))) {
                    cv.put(COLUNA_TIPO_CONTA, 1);
                    for (int j = 0; j < rec.length; j++) {
                        if (cursor.getString(15).equals(rec[j])) {
                            cv.put(COLUNA_CLASSE_CONTA, j);
                        }
                    }
                } else {
                    cv.put(COLUNA_TIPO_CONTA, 2);
                    for (int j = 0; j < apl.length; j++) {
                        if (cursor.getString(15).equals(apl[j])) {
                            cv.put(COLUNA_CLASSE_CONTA, j);
                        }
                    }
                }
                db.update("tabela_temporaria", cv, COLUNA_ID_CONTA + " = '"
                        + idLong + "' ", null);
                cursor.moveToPrevious();
            }
            cursor.close();

            db.execSQL("INSERT INTO " + TABELA_CONTAS
                    + " SELECT " + COLUNA_ID_CONTA
                    + ", " + COLUNA_NOME_CONTA
                    + ", " + COLUNA_TIPO_CONTA
                    + ", " + COLUNA_CLASSE_CONTA
                    + ", " + COLUNA_CATEGORIA_CONTA
                    + ", " + COLUNA_DIA_DATA_CONTA
                    + ", " + COLUNA_MES_DATA_CONTA
                    + ", " + COLUNA_ANO_DATA_CONTA
                    + ", " + COLUNA_VALOR_CONTA
                    + ", " + COLUNA_PAGOU_CONTA
                    + ", " + COLUNA_QT_REPETICOES_CONTA
                    + ", " + COLUNA_NR_REPETICAO_CONTA
                    + ", " + COLUNA_INTERVALO_CONTA
                    + ", " + COLUNA_CODIGO_CONTA
                    + " FROM tabela_temporaria");

            db.execSQL("DROP TABLE tabela_temporaria");

        } catch (Exception e) {
            Log.w(TAG, "Erro ao modificar dados");
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
            db.execSQL("DROP TABLE IF EXISTS categorias_contas");

            db.execSQL("CREATE TABLE tabela_temporaria" + " ( "
                    + COLUNA_ID_CONTA + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUNA_NOME_CONTA + " TEXT, "
                    + COLUNA_TIPO_CONTA + " INTEGER, "
                    + COLUNA_CLASSE_CONTA + " INTEGER, "
                    + COLUNA_CATEGORIA_CONTA + " INTEGER, "
                    + COLUNA_DIA_DATA_CONTA + " INTEGER, "
                    + COLUNA_MES_DATA_CONTA + " INTEGER, "
                    + COLUNA_ANO_DATA_CONTA + " INTEGER, "
                    + COLUNA_VALOR_CONTA + " REAL, "
                    + COLUNA_PAGOU_CONTA + " TEXT, "
                    + COLUNA_QT_REPETICOES_CONTA + " INTEGER, "
                    + COLUNA_NR_REPETICAO_CONTA + " INTEGER, "
                    + COLUNA_INTERVALO_CONTA + " INTEGER, "
                    + COLUNA_CODIGO_CONTA + " TEXT, "
                    + "auxiliar TEXT, "
                    + " classifica TEXT )");

            db.execSQL("INSERT INTO tabela_temporaria"
                    + " SELECT " + COLUNA_ID_CONTA
                    + ", " + COLUNA_NOME_CONTA
                    + ", " + null
                    + ", " + null
                    + ", " + null
                    + ", " + COLUNA_DIA_DATA_CONTA
                    + ", " + COLUNA_MES_DATA_CONTA
                    + ", " + COLUNA_ANO_DATA_CONTA
                    + ", " + COLUNA_VALOR_CONTA
                    + ", " + COLUNA_PAGOU_CONTA
                    + ", " + COLUNA_QT_REPETICOES_CONTA
                    + ", " + COLUNA_NR_REPETICAO_CONTA
                    + ", " + COLUNA_INTERVALO_CONTA
                    + ", data"
                    + ", " + COLUNA_TIPO_CONTA
                    + ", classificacao FROM " + TABELA_CONTAS);

            db.execSQL("DROP TABLE " + TABELA_CONTAS);

            db.execSQL(CRIA_TABELA_CONTAS);

            //db.execSQL("DROP TABLE tabela_temporaria");
        }
    }
}