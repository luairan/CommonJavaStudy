package util;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 操作Excel表格的功能类
 */
public class ExcelUtils {

    public enum Type {
        XLS, XLSX
    }

    /**
     * 读取Excel表格表头的内容
     *
     * @param InputStream
     * @return String 表头内容的数组
     * @throws IOException
     */
    public static String[] readExcelTitle(String filePath) throws IOException {
        InputStream is = null;
        String[] returnValue = new String[0];
        String fileType = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());

        if (fileType.equals("xls")) {
            is = new FileInputStream(filePath);
            return readExcelTitle(is, Type.XLS);
        } else if (fileType.equals("xlsx")) {
            is = new FileInputStream(filePath);
            return readExcelTitle(is, Type.XLSX);
        } else {
            return returnValue;
        }

    }

    /**
     * 读取Excel数据内容
     *
     * @param InputStream
     * @return Map 包含单元格数据内容的Map对象
     * @throws IOException
     */
    public static List<String[]> readExcelContent(String filePath) throws IOException {
        InputStream is = null;
        List<String[]> info = new ArrayList<String[]>();
        String fileType = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());

        if (fileType.equals("xls")) {
            is = new FileInputStream(filePath);
            return readExcelContent(is, Type.XLS);
        } else if (fileType.equals("xlsx")) {
            is = new FileInputStream(filePath);
            return readExcelContent(is, Type.XLSX);
        } else {
            return info;
        }
    }

    /**
     * 读取Excel表格表头的内容
     *
     * @param InputStream
     * @return String 表头内容的数组
     * @throws IOException
     */
    public static String[] readExcelTitle(InputStream is, Type type) throws IOException {

        String[] returnValue = new String[0];

        Workbook wb = null;
        if (type == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int length = 0;

            byte[] buf = new byte[1024];
            while ((length = is.read(buf)) > 0) {
                baos.write(buf, 0, length);
            }

            ByteArrayInputStream baisxls = new ByteArrayInputStream(baos.toByteArray());
            ByteArrayInputStream baixlsx = new ByteArrayInputStream(baos.toByteArray());

            try {
                wb = new HSSFWorkbook(baisxls);
            } catch (Exception e) {

                wb = new XSSFWorkbook(baixlsx);
            }
        } else if (type == Type.XLS) {
            wb = new HSSFWorkbook(is);
        } else if (type == Type.XLSX) {
            wb = new XSSFWorkbook(is);
        }
        Sheet sheet = wb.getSheetAt(0);
        Row row = sheet.getRow(0);
        // 标题总列数
        int colNum = row.getPhysicalNumberOfCells();
        returnValue = new String[colNum];
        for (int i = 0; i < colNum; i++) {
            returnValue[i] = getCellFormatValue(row.getCell(i));
        }

        return returnValue;
    }

    /**
     * 读取Excel数据内容
     *
     * @param is
     * @return Map 包含单元格数据内容的Map对象
     * @throws IOException
     */
    public static List<String[]> readExcelContent(InputStream is, Type type) throws IOException {
        List<String[]> info = new ArrayList<String[]>();
        Workbook wb = null;

        if (type == null) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int length = 0;

            byte[] buf = new byte[1024];
            while ((length = is.read(buf)) > 0) {
                baos.write(buf, 0, length);
            }

            ByteArrayInputStream baisxls = new ByteArrayInputStream(baos.toByteArray());
            ByteArrayInputStream baixlsx = new ByteArrayInputStream(baos.toByteArray());
            try {
                wb = new HSSFWorkbook(baisxls);
            } catch (Exception e) {
                wb = new XSSFWorkbook(baixlsx);
            }
        } else if (type == Type.XLS) {
            wb = new HSSFWorkbook(is);
        } else if (type == Type.XLSX) {
            wb = new XSSFWorkbook(is);
        }
        Sheet sheet = wb.getSheetAt(0);
        // 得到总行数
        int rowNum = sheet.getLastRowNum();
        Row row = sheet.getRow(0);
        int colNum = row.getPhysicalNumberOfCells();
        // 正文内容应该从第二行开始,第一行为表头的标题
        for (int i = 1; i <= rowNum; i++) {
            row = sheet.getRow(i);
            String[] temp = new String[colNum];
            int j = 0;
            while (j < colNum) {
                temp[j] = getCellFormatValue(row.getCell(j)).trim();
                j++;
            }
            info.add(temp);
        }
        return info;
    }

    /**
     * 获取单元格数据内容为字符串类型的数据
     *
     * @param cell Excel单元格
     * @return String 单元格数据内容
     */
    private String getStringCellValue(HSSFCell cell) {
        String strCell = "";
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                strCell = cell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_NUMERIC:
                strCell = String.valueOf(cell.getNumericCellValue());
                break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                strCell = String.valueOf(cell.getBooleanCellValue());
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                strCell = "";
                break;
            default:
                strCell = "";
                break;
        }
        if (strCell.equals("") || strCell == null) {
            return "";
        }
        return strCell;
    }

    /**
     * 获取单元格数据内容为日期类型的数据
     *
     * @param cell Excel单元格
     * @return String 单元格数据内容
     */
    private String getDateCellValue(HSSFCell cell) {
        String result = "";
        try {
            int cellType = cell.getCellType();
            if (cellType == HSSFCell.CELL_TYPE_NUMERIC) {
                Date date = cell.getDateCellValue();
                result = (date.getYear() + 1900) + "-" + (date.getMonth() + 1) + "-" + date.getDate();
            } else if (cellType == HSSFCell.CELL_TYPE_STRING) {
                String date = getStringCellValue(cell);
                result = date.replaceAll("[年月]", "-").replace("日", "").trim();
            } else if (cellType == HSSFCell.CELL_TYPE_BLANK) {
                result = "";
            }
        } catch (Exception e) {
            System.out.println("日期格式不正确!");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 根据HSSFCell类型设置数据
     *
     * @param cell
     * @return
     */
    private static String getCellFormatValue(Cell cell) {
        String cellvalue = "";
        if (cell != null) {
            // 判断当前Cell的Type
            switch (cell.getCellType()) {
                // 如果当前Cell的Type为NUMERIC
                case HSSFCell.CELL_TYPE_NUMERIC:
                case HSSFCell.CELL_TYPE_FORMULA: {
                    // 判断当前的cell是否为Date
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        // 如果是Date类型则，转化为Data格式

                        // 方法1：这样子的data格式是带时分秒的：2011-10-12 0:00:00
                        // cellvalue = cell.getDateCellValue().toLocaleString();

                        // 方法2：这样子的data格式是不带带时分秒的：2011-10-12
                        Date date = cell.getDateCellValue();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        cellvalue = sdf.format(date);

                    }
                    // 如果是纯数字
                    else {
                        // 取得当前Cell的数值
                        cellvalue = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                // 如果当前Cell的Type为STRIN
                case HSSFCell.CELL_TYPE_STRING:
                    // 取得当前的Cell字符串
                    cellvalue = cell.getRichStringCellValue().getString();
                    break;
                // 默认的Cell值
                default:
                    cellvalue = " ";
            }
        } else {
            cellvalue = "";
        }
        return cellvalue;

    }

    public static void main(String[] args) throws IOException {
        try {
            // 对读取Excel表格标题测试

            String[] title = ExcelUtils.readExcelTitle(new FileInputStream("/Users/luairan/Downloads/dq.xlsx"), null);
            System.out.println("获得Excel表格的标题:");
            for (String s : title) {
                System.out.print("|" + s + "|");
            }

            // 对读取Excel表格内容测试
            List<String[]> map = ExcelUtils.readExcelContent(new FileInputStream("/Users/luairan/Downloads/dq.xlsx"), null);
            System.out.println("获得Excel表格的内容:");

            for (String[] s : map) {
                for (int i = 0; i < s.length; i++) {
                    System.out.print(s[i] + "|");
                }
                System.out.println();
            }

        } catch (FileNotFoundException e) {
            System.out.println("未找到指定路径的文件!");
            e.printStackTrace();
        }
    }
}