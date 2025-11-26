package com.msk.minhascontas.excel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import com.msk.minhascontas.db.Conta;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.write.Label;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class ExportarExcel {

    // ELEMENTOS QUE ESCREVEM O ARQUIVO EM EXCEL
    private WritableWorkbook arquivoExcel;
    private WritableSheet planilha;
    // REMOVED: private File cartaoSD; // No longer needed

    // FONTE PARA TEXTO
    private WritableFont arial10font, times16font;

    // FORMATO DE NUMERO
    private NumberFormat duasCasas; // This doesn't seem to be used in the current code.

    // FORMATADORES DE CELULAS COM TEXTO, DATA e NUMEROS
    private WritableCellFormat integerFormat, arial10format, decDuasCasasFontFormat; // integerFormat and decDuasCasasFontFormat also not used.

    // TEXTO PARA CELULA
    private Label nomeColuna, nomeLinha, conteudoColuna;

    // Nomes das colunas da aba DADOS, para uso interno e centralização
    private String[] colunasDados = {
            "ID", "Nome", "Tipo", "Classe", "Categoria", "Dia", "Mês", "Ano",
            "Valor", "Status", "Qt. Repet.", "N. Repet.", "Intervalo", "Código", "Juros"
    };

    // INFORMACOES QUE ALIMENTAM AS PLANILHAS
    private int erro;

    /**
     * Novo método principal de exportação para Excel.
     * Recebe os dados já processados (Resumo e Detalhado) e gerencia a criação das duas abas.
     *
     * @param context Contexto da aplicação.
     * @param outputUri URI de saída (onde o arquivo será gravado).
     * @param resumoLinhas Array de Strings com os nomes das linhas do Resumo.
     * @param resumoValores Array de Strings com os valores calculados do Resumo.
     * @param contasDetalhada Lista completa de contas para a aba DADOS.
     * @return 0 se sucesso, > 0 se erro.
     */
    public int CriaExcel(Context context, Uri outputUri, String[] resumoLinhas,
                         String[] resumoValores, List<Conta> contasDetalhada) {

        erro = 0; // Reseta o contador de erros
        try {
            // 1. Cria o WritableWorkbook
            OutputStream os = context.getContentResolver().openOutputStream(outputUri);
            arquivoExcel = Workbook.createWorkbook(os);

            // 2. Configura os formatos de célula (Presumido existir na classe)
            ConfiguraFormatos();

            // 3. Cria e preenche a Aba RESUMO (utiliza a lógica original de escrita)
            planilha = arquivoExcel.createSheet("RESUMO", 0); // Reutiliza a variavel 'planilha'
            EscreveNomeColunas(colunasDados, resumoLinhas); // Antigo método que escreve os títulos das linhas
            EscrevePlanilha(1, resumoValores); // Antigo método que escreve os valores na Coluna 1 (B)

            // 4. Cria e preenche a Aba DADOS (novo requisito)
            WritableSheet planilhaDados = arquivoExcel.createSheet("DADOS", 1);
            EscreveDadosDetalhado(planilhaDados, contasDetalhada);

            // 5. Escreve e Fecha o arquivo
            if (erro == 0) {
                arquivoExcel.write();
            }
            arquivoExcel.close();
            os.close();

        } catch (IOException e) {
            Log.e("ExportarExcel", "Erro de I/O ao criar o arquivo: " + e.getMessage());
            erro = erro + 1;
        } catch (WriteException e) {
            Log.e("ExportarExcel", "Erro de escrita no Excel: " + e.getMessage());
            erro = erro + 1;
        } catch (Exception e) {
            Log.e("ExportarExcel", "Erro desconhecido em CriaExcel: " + e.getMessage());
            erro = erro + 1;
        }

        return erro;
    }

    /**
     * Define os WritableFont e WritableCellFormat que serão usados para
     * a formatação das células no Excel.
     */
    private void ConfiguraFormatos() {
        try {
            // Fontes
            arial10font = new WritableFont(WritableFont.ARIAL, 10);
            times16font = new WritableFont(WritableFont.TIMES, 16, WritableFont.BOLD);

            // Formato de Número (2 Casas Decimais)
            duasCasas = new NumberFormat("#,##0.00", NumberFormat.COMPLEX_FORMAT);

            // Formatadores de Célula
            arial10format = new WritableCellFormat(arial10font); // Texto Simples
            arial10format.setWrap(true); // Permite quebra de linha

            integerFormat = new WritableCellFormat(arial10font); // Inteiro
            integerFormat.setAlignment(Alignment.LEFT);

            decDuasCasasFontFormat = new WritableCellFormat(arial10font, duasCasas); // Valor R$
            decDuasCasasFontFormat.setAlignment(Alignment.LEFT);

            Log.i("ExportarExcel", "Formatos de célula configurados.");

        } catch (WriteException e) {
            Log.e("ExportarExcel", "Erro ao configurar formatos: " + e.getMessage());
        }
    }

    /**
     * Preenche a planilha de DADOS com todos os registros de contas detalhados.
     * Utiliza o array de Strings 'cabecalhos' para os títulos das colunas
     * e itera sobre a lista de POJOs Conta para preencher o conteúdo.
     *
     * @param planilhaDados O objeto WritableSheet para a aba DADOS.
     * @param contas Lista de objetos Conta a serem escritos.
     * @throws WriteException Exceção de escrita no JXL.
     */
    private void EscreveDadosDetalhado(WritableSheet planilhaDados, List<Conta> contas) throws WriteException {

        // 1. Escreve os cabeçalhos das colunas (Linha 0)
        for (int i = 0; i < colunasDados.length; i++) { // Usa colunasDados.length
            Label cabecalho = new Label(i, 0, colunasDados[i], arial10format); // Usa colunasDados[i]
            planilhaDados.addCell(cabecalho);
        }

        // 2. Escreve os dados das contas (A partir da Linha 1)
        for (int row = 0; row < contas.size(); row++) {
            Conta conta = contas.get(row);
            int col = 0;

            // Coluna 0: ID
            // Usando o formato de número inteiro (assumido existir na classe)
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getIdConta(), integerFormat));

            // Coluna 1: Nome
            planilhaDados.addCell(new Label(col++, row + 1, conta.getNome(), arial10format));

            // Coluna 2: Tipo
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getTipo(), integerFormat));

            // Coluna 3: Classe
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getClasseConta(), integerFormat));

            // Coluna 4: Categoria
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getCategoria(), integerFormat));

            // Coluna 5, 6, 7: Data
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getDia(), integerFormat));
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getMes(), integerFormat));
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getAno(), integerFormat));

            // Coluna 8: Valor
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getValor(), decDuasCasasFontFormat));

            // Coluna 9: Status (paguei/falta)
            planilhaDados.addCell(new Label(col++, row + 1, conta.getPagamento(), arial10format));

            // Colunas 10, 11, 12: Repetição e Intervalo
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getQtRepete(), integerFormat));
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getNRepete(), integerFormat));
            planilhaDados.addCell(new jxl.write.Number(col++, row + 1, conta.getIntervalo(), integerFormat));

            // Coluna 13: Código
            planilhaDados.addCell(new Label(col++, row + 1, conta.getCodigo(), arial10format));

            // Coluna 14: Juros
            planilhaDados.addCell(new jxl.write.Number(col, row + 1, conta.getValorJuros(), decDuasCasasFontFormat));
        }

        Log.i("ExportarExcel", "Aba DADOS escrita com " + contas.size() + " registros.");
    }

    private void EscreveNomeColunas(String[] colunas, String[] linhas) {
        // ESCREVE O ROTULO DE CADA COLUNA E LINHA DA PLANILHA

        erro = 0;

        try {

            // NOME DOS ROTULOS DAS COLUNAS DA PLANILHA
            // Ensure colunas has at least 12 elements or adjust loop
            for (int i = 0; i < colunas.length && i < 12; i++) { // Added safeguard for array bounds
                nomeColuna = new Label(i + 1, 0, colunas[i], arial10format);
                planilha.addCell(nomeColuna);
            }

            Log.i("Excel Format", "Escreveu o nome das colunas");

            // NOME DOS ROTULOS DAS LINHAS DA PLANILHA
            for (int i = 0; i < linhas.length; i++) {
                nomeLinha = new Label(0, i + 1, linhas[i], arial10format);
                planilha.addCell(nomeLinha);
            }

            Log.i("Excel Format", "Escreveu o nome das linhas");

        } catch (RowsExceededException e) {
            e.printStackTrace();
            Log.e("ExportarExcel", "RowsExceededException in EscreveNomeColunas: " + e.getMessage());
            erro = erro + 1;
        } catch (WriteException e) {
            e.printStackTrace();
            Log.e("ExportarExcel", "WriteException in EscreveNomeColunas: " + e.getMessage());
            erro = erro + 1;
        }

    }

    private void EscrevePlanilha(int nrColuna, String[] valor) {

        // -------- ESCREVENDO NAS CELULAS DA PLANILHA ------
        erro = 0;
        try {

            // -------- ESCREVENDO DADOS NA PLANILHA ------

            for (int i = 0; i < valor.length; i++) {
                conteudoColuna = new Label(nrColuna, i + 1, valor[i],
                        arial10format);
                planilha.addCell(conteudoColuna);
            }

            Log.i("Excel Format", "Escreveu o mes: " + nrColuna);

        } catch (RowsExceededException e) {
            e.printStackTrace();
            Log.e("ExportarExcel", "RowsExceededException in EscrevePlanilha (col " + nrColuna + "): " + e.getMessage());
            erro = erro + 1;
        } catch (WriteException e) {
            e.printStackTrace();
            Log.e("ExportarExcel", "WriteException in EscrevePlanilha (col " + nrColuna + "): " + e.getMessage());
            erro = erro + 1;
        }
    }

}