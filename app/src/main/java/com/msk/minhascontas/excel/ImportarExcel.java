// ImportarExcel.java
package com.msk.minhascontas.excel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.msk.minhascontas.db.ContasContract.Colunas;
import com.msk.minhascontas.db.Conta;
import com.msk.minhascontas.R;
import com.msk.minhascontas.db.ContasContract;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar; // ADICIONADO
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// IMPORTS DO APACHE POI
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.usermodel.DateUtil; // ADICIONADO


/**
 * Classe de serviço responsável pela leitura, validação e conversão de dados
 * de um arquivo Excel (.xls ou .xlsx) para uma lista de objetos Conta.
 * Implementado com Apache POI para suporte a formatos modernos.
 */
public class ImportarExcel {

    private static final String TAG = "ImportarExcel";

    // --- CONSTANTES PARA IDENTIFICAÇÃO DE COLUNAS (HEADER EXPECTED) ---
    private static final Map<String, String> DB_HEADERS_MAP = new HashMap<>();
    static {
        DB_HEADERS_MAP.put(Colunas.COLUNA_NOME_CONTA, "NOME");
        DB_HEADERS_MAP.put(Colunas.COLUNA_TIPO_CONTA, "TIPO");
        DB_HEADERS_MAP.put(Colunas.COLUNA_CLASSE_CONTA, "CLASSE");
        DB_HEADERS_MAP.put(Colunas.COLUNA_CATEGORIA_CONTA, "CATEGORIA");
        DB_HEADERS_MAP.put(Colunas.COLUNA_DIA_DATA_CONTA, "DIA");
        DB_HEADERS_MAP.put(Colunas.COLUNA_MES_DATA_CONTA, "MES");
        DB_HEADERS_MAP.put(Colunas.COLUNA_ANO_DATA_CONTA, "ANO");
        DB_HEADERS_MAP.put(Colunas.COLUNA_VALOR_CONTA, "VALOR");
        DB_HEADERS_MAP.put(Colunas.COLUNA_PAGOU_CONTA, "PAGAMENTO");
        DB_HEADERS_MAP.put(Colunas.COLUNA_QT_REPETICOES_CONTA, "QT_REPETE");
        DB_HEADERS_MAP.put(Colunas.COLUNA_NR_REPETICAO_CONTA, "N_REPETE");
        DB_HEADERS_MAP.put(Colunas.COLUNA_INTERVALO_CONTA, "INTERVALO");
        DB_HEADERS_MAP.put(Colunas.COLUNA_CODIGO_CONTA, "CODIGO");
        DB_HEADERS_MAP.put(Colunas.COLUNA_VALOR_JUROS, "VALOR_JUROS");

        DB_HEADERS_MAP.put("DATA_COMPLETA", "DATA");
    }

    // --- CLASSE INTERNA PARA MAPEAR COLUNAS ENCONTRADAS ---
    private static class ColumnMapping {
        public enum ImportMode {
            FULL_DB_MATCH,
            BASIC_MATCH,
            INVALID
        }
        public ImportMode mode = ImportMode.INVALID;
        public final Map<String, Integer> colIndexMap = new HashMap<>();
    }


    private ColumnMapping identificarColunas(Sheet sheet) {
        ColumnMapping mapping = new ColumnMapping();
        if (sheet.getPhysicalNumberOfRows() < 1 || sheet.getRow(0) == null) return mapping;

        Row headerRow = sheet.getRow(0);

        Map<String, Integer> excelHeaders = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                try {
                    // Force string conversion for header reading
                    if (cell.getCellType() != CellType.STRING) {
                        cell.setCellType(CellType.STRING);
                    }
                    String header = cell.getStringCellValue().trim().toUpperCase(Locale.ROOT).replace(" ", "_").replace(".", "");
                    excelHeaders.put(header, i);
                } catch (Exception e) {
                    Log.w(TAG, "Célula de cabeçalho não pôde ser lida como String: [" + i + "]");
                }
            }
        }

        // --- REGRA 1: FULL DB MATCH ---
        boolean fullMatchFound = true;
        ColumnMapping fullMapping = new ColumnMapping();

        for (Map.Entry<String, String> entry : DB_HEADERS_MAP.entrySet()) {
            if (entry.getKey().equals("DATA_COMPLETA")) continue;

            String dbFieldName = entry.getKey();
            String expectedHeader = entry.getValue();

            if (excelHeaders.containsKey(expectedHeader)) {
                fullMapping.colIndexMap.put(dbFieldName, excelHeaders.get(expectedHeader));
            } else {
                if (dbFieldName.equals(Colunas.COLUNA_NOME_CONTA) ||
                        dbFieldName.equals(Colunas.COLUNA_VALOR_CONTA) ||
                        dbFieldName.equals(Colunas.COLUNA_DIA_DATA_CONTA) ||
                        dbFieldName.equals(Colunas.COLUNA_MES_DATA_CONTA) ||
                        dbFieldName.equals(Colunas.COLUNA_ANO_DATA_CONTA) ||
                        dbFieldName.equals(Colunas.COLUNA_TIPO_CONTA)) {
                    fullMatchFound = false;
                    break;
                }
            }
        }

        if (fullMatchFound) {
            fullMapping.mode = ColumnMapping.ImportMode.FULL_DB_MATCH;
            Log.d(TAG, "Regra 1 (FULL_DB_MATCH) Encontrada.");
            return fullMapping;
        }


        // --- REGRA 2: BASIC MATCH ---
        ColumnMapping basicMapping = new ColumnMapping();
        boolean basicMatchFound = true;

        if (excelHeaders.containsKey(DB_HEADERS_MAP.get(Colunas.COLUNA_NOME_CONTA))) {
            basicMapping.colIndexMap.put(Colunas.COLUNA_NOME_CONTA, excelHeaders.get(DB_HEADERS_MAP.get(Colunas.COLUNA_NOME_CONTA)));
        } else { basicMatchFound = false; }

        if (excelHeaders.containsKey(DB_HEADERS_MAP.get(Colunas.COLUNA_VALOR_CONTA))) {
            basicMapping.colIndexMap.put(Colunas.COLUNA_VALOR_CONTA, excelHeaders.get(DB_HEADERS_MAP.get(Colunas.COLUNA_VALOR_CONTA)));
        } else { basicMatchFound = false; }

        if (excelHeaders.containsKey(DB_HEADERS_MAP.get("DATA_COMPLETA"))) {
            basicMapping.colIndexMap.put("DATA_COMPLETA", excelHeaders.get(DB_HEADERS_MAP.get("DATA_COMPLETA")));
        } else { basicMatchFound = false; }

        if (basicMatchFound) {
            basicMapping.mode = ColumnMapping.ImportMode.BASIC_MATCH;
            Log.d(TAG, "Regra 2 (BASIC_MATCH) Encontrada.");
            return basicMapping;
        }

        Log.e(TAG, "Nenhuma regra de importação (FULL ou BASIC) foi satisfeita.");
        return new ColumnMapping();
    }


    private String getString(Sheet sheet, Integer col, int row) {
        if (col == null || col < 0) return "";
        Row poiRow = sheet.getRow(row);
        if (poiRow == null) return "";

        Cell cell = poiRow.getCell(col);
        if (cell == null) return "";

        try {
            if (cell.getCellType() == CellType.FORMULA) {
                return String.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return String.valueOf(cell.getNumericCellValue());
            } else {
                cell.setCellType(CellType.STRING);
                return cell.getStringCellValue().trim();
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro ao ler célula [" + row + "," + CellReference.convertNumToColString(col) + "]: " + e.getMessage());
            return "";
        }
    }


    private int parseInteger(String value, int defaultValue) {
        try {
            String cleanValue = value.replaceAll("[^\\d-]", "").trim();
            if (cleanValue.isEmpty()) return defaultValue;
            return Integer.parseInt(cleanValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            String cleanValue = value.replace(".", "").replace(",", ".");
            cleanValue = cleanValue.replaceAll("[^\\d.]", "").trim();
            if (cleanValue.isEmpty()) return defaultValue;

            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            try {
                java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("pt", "BR"));
                return nf.parse(value).doubleValue();
            } catch (java.text.ParseException e2) {
                return defaultValue;
            }
        }
    }

    /**
     * Extrai e converte o valor da célula para um objeto Calendar, validando a data.
     * @param col Índice da coluna.
     * @param row Índice da linha.
     * @return Calendar com a data correta ou null em caso de falha.
     */
    private Calendar getDateFromCell(Sheet sheet, Integer col, int row) {
        if (col == null || col < 0) return null;
        Row poiRow = sheet.getRow(row);
        if (poiRow == null) return null;

        Cell cell = poiRow.getCell(col);
        if (cell == null) return null;

        Calendar calendar = Calendar.getInstance();

        // 1. Tenta ler como data Excel (numérico com formatação de data)
        if (DateUtil.isCellDateFormatted(cell)) {
            try {
                calendar.setTime(cell.getDateCellValue());
                // Validação de intervalo básico para prevenir datas irreais
                if (calendar.get(Calendar.YEAR) > 1900 && calendar.get(Calendar.YEAR) < 2100) {
                    return calendar;
                }
            } catch (Exception e) {
                Log.w(TAG, "Erro ao obter data da célula formatada (Tipo 1). Tentando fallback. Linha: " + (row + 1));
            }
        }

        // 2. Tenta ler como data numérica (numérico sem formatação de data)
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            try {
                double numericValue = cell.getNumericCellValue();
                if (DateUtil.isValidExcelDate(numericValue)) {
                    calendar.setTime(DateUtil.getJavaDate(numericValue));
                    // Validação de intervalo básico
                    if (calendar.get(Calendar.YEAR) > 1900 && calendar.get(Calendar.YEAR) < 2100) {
                        return calendar;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Falha na conversão de data numérica (Tipo 2). Tentando fallback para String. Linha: " + (row + 1));
            }
        }


        // 3. Tenta ler como String (para o formato dd/mm/aaaa inserido como texto)
        try {
            // Sempre tenta obter o valor como String para a última tentativa
            if (cell.getCellType() != CellType.STRING) {
                cell.setCellType(CellType.STRING);
            }
            String dateString = cell.getStringCellValue().trim();

            // Tenta parsear o formato dd/mm/aaaa
            String[] partes = dateString.split("[^\\d]+");
            if (partes.length >= 3) {

                int day = parseInteger(partes[0], 0);
                int month = parseInteger(partes[1], 0);
                int year = parseInteger(partes[2], 0);

                // Validações básicas (evita datas incorretas como 90/90/90)
                if (day > 0 && day <= 31 && month >= 1 && month <= 12 && year > 1900) {
                    calendar.set(year, month - 1, day); // Calendar month é 0-based
                    return calendar;
                } else {
                    Log.w(TAG, "Data inválida encontrada como String: " + dateString + ". Linha: " + (row + 1));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Falha no parse da data como String. Linha: " + (row + 1));
        }

        return null; // Retorna nulo se a data não puder ser determinada ou for inválida
    }


    private List<Conta> lerContasDaPlanilha(Sheet sheet, ColumnMapping mapping) {
        List<Conta> contasLidas = new ArrayList<>();
        int linhasIgnoradas = 0;
        int fallbackCounter = 0; // Contador para garantir unicidade no fallback

        Integer colNome = mapping.colIndexMap.get(Colunas.COLUNA_NOME_CONTA);
        Integer colValor = mapping.colIndexMap.get(Colunas.COLUNA_VALOR_CONTA);

        boolean isBasicMode = mapping.mode == ColumnMapping.ImportMode.BASIC_MATCH;
        Integer colDataCompleta = mapping.colIndexMap.get("DATA_COMPLETA");

        Integer colDia = mapping.colIndexMap.get(Colunas.COLUNA_DIA_DATA_CONTA);
        Integer colMes = mapping.colIndexMap.get(Colunas.COLUNA_MES_DATA_CONTA);
        Integer colAno = mapping.colIndexMap.get(Colunas.COLUNA_ANO_DATA_CONTA);


        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row poiRow = sheet.getRow(i);
            if (poiRow == null) continue;

            try {
                String nome = getString(sheet, colNome, i);
                double valor = parseDouble(getString(sheet, colValor, i), 0.0);

                if (nome.isEmpty() || valor == 0.0) {
                    Log.w(TAG, "Linha ignorada (Nome ou Valor vazio/zero): " + (i + 1));
                    linhasIgnoradas++;
                    continue;
                }

                Conta novaConta = new Conta();

                novaConta.setNome(nome);
                novaConta.setValor(valor);

                novaConta.setTipo(ContasContract.TIPO_DESPESA);
                novaConta.setClasseConta(ContasContract.CLASSE_DESPESA_VARIAVEL);
                novaConta.setCategoria(ContasContract.CATEGORIA_OUTROS);
                novaConta.setPagamento("");
                novaConta.setQtRepete(1);
                novaConta.setNRepete(1);
                novaConta.setIntervalo(0);
                novaConta.setCodigo("");
                novaConta.setValorJuros(0.0);

                // Valores padrão (fallback inicial)
                novaConta.setDia(1);
                novaConta.setMes(1);
                novaConta.setAno(2000);


                // --- TRATAMENTO DE DATA (CORREÇÃO DE FALLBACK ÚNICO) ---
                if (isBasicMode && colDataCompleta != null) {
                    Calendar data = getDateFromCell(sheet, colDataCompleta, i);

                    if (data == null) {
                        // Aplica o fallback, incrementando o contador para garantir unicidade
                        fallbackCounter++;

                        Log.w(TAG, "Erro ao parsear DATA completa. Usando DATA ATUAL com offset para unicidade. Linha: " + (i + 1));

                        data = Calendar.getInstance();

                        // Aplica um offset no dia, usando o operador módulo para garantir que o dia
                        // não ultrapasse 28 (uma margem segura)
                        int currentDay = data.get(Calendar.DAY_OF_MONTH);
                        int offsetDay = (currentDay + fallbackCounter) % 28 + 1;

                        data.set(Calendar.DAY_OF_MONTH, offsetDay);

                        // Define um ano fixo e distante (2042) para que estas contas não colidam
                        // com contas reais já existentes no BD e sejam facilmente identificáveis.
                        data.set(Calendar.YEAR, 2042);
                    }

                    // Sobrescreve a data padrão (01/01/2000) com a data correta ou o fallback único
                    novaConta.setDia(data.get(Calendar.DAY_OF_MONTH));
                    novaConta.setMes(data.get(Calendar.MONTH) + 1); // DB é 1-based
                    novaConta.setAno(data.get(Calendar.YEAR));

                } else if (mapping.mode == ColumnMapping.ImportMode.FULL_DB_MATCH) {
                    // Mantém a lógica existente para colunas separadas
                    novaConta.setDia(parseInteger(getString(sheet, colDia, i), novaConta.getDia()));
                    novaConta.setMes(parseInteger(getString(sheet, colMes, i), novaConta.getMes()));
                    novaConta.setAno(parseInteger(getString(sheet, colAno, i), novaConta.getAno()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_TIPO_CONTA) != null)
                        novaConta.setTipo(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_TIPO_CONTA), i), novaConta.getTipo()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_CLASSE_CONTA) != null)
                        novaConta.setClasseConta(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_CLASSE_CONTA), i), novaConta.getClasseConta()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_CATEGORIA_CONTA) != null)
                        novaConta.setCategoria(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_CATEGORIA_CONTA), i), novaConta.getCategoria()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_PAGOU_CONTA) != null)
                        novaConta.setPagamento(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_PAGOU_CONTA), i));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_QT_REPETICOES_CONTA) != null)
                        novaConta.setQtRepete(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_QT_REPETICOES_CONTA), i), novaConta.getQtRepete()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_NR_REPETICAO_CONTA) != null)
                        novaConta.setNRepete(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_NR_REPETICAO_CONTA), i), novaConta.getNRepete()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_INTERVALO_CONTA) != null)
                        novaConta.setIntervalo(parseInteger(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_INTERVALO_CONTA), i), novaConta.getIntervalo()));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_CODIGO_CONTA) != null)
                        novaConta.setCodigo(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_CODIGO_CONTA), i));

                    if (mapping.colIndexMap.get(Colunas.COLUNA_VALOR_JUROS) != null)
                        novaConta.setValorJuros(parseDouble(getString(sheet, mapping.colIndexMap.get(Colunas.COLUNA_VALOR_JUROS), i), novaConta.getValorJuros()));
                }

                contasLidas.add(novaConta);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao processar a linha: " + (i + 1) + " / Erro: " + e.getMessage(), e);
                linhasIgnoradas++;
            }
        }

        Log.i(TAG, "Processamento concluído. Linhas lidas: " + contasLidas.size() + " / Linhas ignoradas/com erro: " + linhasIgnoradas);
        return contasLidas;
    }


    /**
     * MÉTODO PRINCIPAL DE IMPORTAÇÃO
     * Usa WorkbookFactory para suportar tanto .xls quanto .xlsx.
     */
    public List<Conta> lerExcel(Context context, Uri uri) {
        Workbook workbook = null;
        List<Conta> contasLidas = null;
        InputStream inputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream é nulo.");
                return null;
            }

            // Lê todo o stream para um array de bytes para evitar problemas de reset do stream
            byte[] fileBytes = IOUtils.toByteArray(inputStream);

            // Reabre o stream a partir do array de bytes (necessário para POI)
            inputStream.close();
            inputStream = new java.io.ByteArrayInputStream(fileBytes);

            workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            ColumnMapping mapping = identificarColunas(sheet);

            if (mapping.mode == ColumnMapping.ImportMode.INVALID) {
                Log.e(TAG, "Estrutura do arquivo Excel inválida.");
                return null;
            }

            contasLidas = lerContasDaPlanilha(sheet, mapping);

        } catch (IOException e) {
            // IOException cobre problemas de I/O, e também a InvalidFormatException
            // que é lançada pelo WorkbookFactory quando o formato é incorreto ou corrompido.
            Log.e(TAG, "Erro de formato (IOException). Arquivo não é um formato de planilha válido ou está corrompido.", e);
            contasLidas = null;
        } catch (Exception e) {
            Log.e(TAG, "Erro geral durante a importação (I/O, segurança, etc.): " + e.getMessage(), e);
            contasLidas = null;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar o Workbook: " + e.getMessage());
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar o InputStream: " + e.getMessage());
                }
            }
        }
        return contasLidas;
    }
}