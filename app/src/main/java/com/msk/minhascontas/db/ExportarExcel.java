package com.msk.minhascontas.db;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import jxl.Workbook;
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
    WritableWorkbook arquivoExcel;
    WritableSheet planilha;
    File cartaoSD;

    // FONTE PARA TEXTO
    WritableFont arial10font, times16font;

    // FORMATO DE NUMERO
    NumberFormat duasCasas;

    // FORMATADORES DE CELULAS COM TEXTO, DATA e NUMEROS
    WritableCellFormat integerFormat, arial10format, decDuasCasasFontFormat;

    // TEXTO PARA CELULA
    Label nomeColuna, nomeLinha, conteudoColuna;

    // INFORMACOES QUE ALIMENTAM AS PLANILHAS
    private int erro;

    public int CriaExcel(String mes_ano, String[] jan, String[] fev,
                         String[] mar, String[] abr, String[] mai, String[] jun,
                         String[] jul, String[] ago, String[] set, String[] out,
                         String[] nov, String[] dez, String[] colunas, String[] linhas, String pasta) {

        if (!pasta.equals(""))
            cartaoSD = new File(pasta);
        else
            cartaoSD = Environment.getExternalStorageDirectory();
        erro = 0;
        try {

            // -------- CRIANDO UM ARQUIVO E UMA PLANILHA EXCEL ------

            // CRIA O ARQUIVO EXCEL
            arquivoExcel = Workbook.createWorkbook(new File(cartaoSD,
                    "minhas_contas.xls"));
            // CRIA UMA PLANILHA
            planilha = arquivoExcel.createSheet(mes_ano, 0);

            Log.i("Excel Format", "Criou arquivo excel");

            // -------- FORMATO PARA TEXTO E NUMERO NA PLANILHA ------

            // FORMATO DE CELULA FONTE ARIAL 10
            arial10font = new WritableFont(WritableFont.ARIAL, 10);
            arial10format = new WritableCellFormat(arial10font);

            // -------- ESCREVENDO NAS CELULAS DA PLANILHA ------

            EscreveNomeColunas(colunas, linhas);

            EscrevePlanilha(1, jan);
            EscrevePlanilha(2, fev);
            EscrevePlanilha(3, mar);
            EscrevePlanilha(4, abr);
            EscrevePlanilha(5, mai);
            EscrevePlanilha(6, jun);
            EscrevePlanilha(7, jul);
            EscrevePlanilha(8, ago);
            EscrevePlanilha(9, set);
            EscrevePlanilha(10, out);
            EscrevePlanilha(11, nov);
            EscrevePlanilha(12, dez);

            // ESCREVE O ARQUIVO
            arquivoExcel.write();
            Log.i("Excel Format", "Salvou arquivo excel");
            // FECHA O ARQUIVO
            arquivoExcel.close();
            Log.i("Excel Format", "Fechou arquivo excel");
        } catch (IOException e) {
            e.printStackTrace();
            erro = erro + 1;
        } catch (WriteException e) {
            e.printStackTrace();
            erro = erro + 1;
        }

        return erro;
    }

    private void EscreveNomeColunas(String[] colunas, String[] linhas) {
        // ESCREVE O ROTULO DE CADA COLUNA E LINHA DA PLANILHA

        erro = 0;

        try {

            // NOME DOS ROTULOS DAS COLUNAS DA PLANILHA
            nomeColuna = new Label(1, 0, colunas[0], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(2, 0, colunas[1], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(3, 0, colunas[2], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(4, 0, colunas[3], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(5, 0, colunas[4], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(6, 0, colunas[5], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(7, 0, colunas[6], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(8, 0, colunas[7], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(9, 0, colunas[8], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(10, 0, colunas[9], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(11, 0, colunas[10], arial10format);
            planilha.addCell(nomeColuna);
            nomeColuna = new Label(12, 0, colunas[11], arial10format);
            planilha.addCell(nomeColuna);

            Log.i("Excel Format", "Escreveu o nome das colunas");

            // NOME DOS ROTULOS DAS LINHAS DA PLANILHA
            for (int i = 0; i < linhas.length; i++) {
                nomeLinha = new Label(0, i + 1, linhas[i], arial10format);
                planilha.addCell(nomeLinha);
            }

            Log.i("Excel Format", "Escreveu o nome das linhas");

        } catch (RowsExceededException e) {
            e.printStackTrace();
            erro = erro + 1;
        } catch (WriteException e) {
            e.printStackTrace();
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
            erro = erro + 1;
        } catch (WriteException e) {
            e.printStackTrace();
            erro = erro + 1;
        }
    }

}
