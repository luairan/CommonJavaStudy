package util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Reads the first sheet, preserving sparse rows/cells and closing owned workbooks. */
public class ExcelUtils {
    public enum Type { XLS, XLSX }

    public static String[] readExcelTitle(String path) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get(path))) { return readExcelTitle(in,null); }
    }
    public static List<String[]> readExcelContent(String path) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get(path))) { return readExcelContent(in,null); }
    }

    // The caller owns the supplied stream; closing a workbook must not close it.
    private static Workbook open(InputStream source, Type type) throws IOException {
        InputStream in = new FilterInputStream(Objects.requireNonNull(source,"source")) {
            @Override public void close() { }
        };
        if (type == Type.XLS) return new HSSFWorkbook(in);
        if (type == Type.XLSX) return new XSSFWorkbook(in);
        return WorkbookFactory.create(in);
    }

    public static String[] readExcelTitle(InputStream in, Type type) throws IOException {
        try (Workbook workbook = open(in,type)) {
            if (workbook.getNumberOfSheets() == 0) return new String[0];
            Row row = workbook.getSheetAt(0).getRow(0);
            if (row == null) return new String[0];
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            String[] title = new String[Math.max(0,row.getLastCellNum())];
            for (int i = 0; i < title.length; i++) title[i] = value(row.getCell(i),evaluator,formatter);
            return title;
        }
    }

    public static List<String[]> readExcelContent(InputStream in, Type type) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (Workbook workbook = open(in,type)) {
            if (workbook.getNumberOfSheets() == 0) return result;
            Sheet sheet = workbook.getSheetAt(0);
            int width = 0;
            for (Row row : sheet) width = Math.max(width,row.getLastCellNum());
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                String[] values = new String[width];
                for (int column = 0; column < width; column++) {
                    values[column] = row == null ? "" : value(row.getCell(column),evaluator,formatter).trim();
                }
                result.add(values);
            }
        }
        return result;
    }

    private static String value(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) type = evaluator.evaluateFormulaCell(cell);
        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(cell.getDateCellValue());
        }
        return formatter.formatCellValue(cell,evaluator);
    }
}
