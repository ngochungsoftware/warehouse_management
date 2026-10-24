package com.utc.warehouse.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Xuất JTable ra file Excel (.xlsx) bằng Apache POI.
 *
 * <p>Dùng POI (thay vì tự dựng XML) để đảm bảo file luôn tương thích tuyệt đối với
 * Excel, LibreOffice, Numbers và Google Sheets — không lo báo "repair" hay mở ra trống.
 *
 * <p>Định dạng áp dụng:
 * <ul>
 *   <li>Header: chữ đậm, nền xanh, viền, căn giữa, chiều cao 24px.</li>
 *   <li>Ô số ghi đúng kiểu number để Excel tính toán, lọc và sort được.</li>
 *   <li>Cột tự co giãn theo nội dung, dòng header đóng băng khi cuộn.</li>
 *   <li>Bật AutoFilter ngay trên dòng header.</li>
 * </ul>
 */
public final class ExcelExporter {

    private ExcelExporter() { }

    /* ================== API công khai ================== */

    /**
     * Hộp thoại chọn nơi lưu rồi xuất bảng ra file .xlsx.
     *
     * @return file đã ghi, hoặc {@code null} nếu người dùng hủy
     */
    public static File exportTable(JTable table, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(AppPaths.getExportDir().toFile(),
                defaultName + "_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx"));
        if (fc.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) return null;

        File out = fc.getSelectedFile();
        // Người dùng có thể gõ tên không kèm đuôi -> ép đuôi .xlsx
        if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            out = new File(out.getParentFile(), out.getName() + ".xlsx");
        }
        return writeTable(table.getModel(), out) ? out : null;
    }

    /**
     * Ghi TableModel ra file .xlsx.
     *
     * @return {@code true} nếu ghi thành công
     */
    public static boolean writeTable(TableModel m, File out) {
        try (Workbook wb = new XSSFWorkbook();
             OutputStream os = new FileOutputStream(out)) {
            Sheet sheet = wb.createSheet("Sheet1");
            writeSheet(sheet, m);
            wb.write(os);
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Lỗi xuất Excel: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /* ================== Ghi sheet ================== */

    private static void writeSheet(Sheet sheet, TableModel m) {
        int colCount = m.getColumnCount();
        int rowCount = m.getRowCount();

        // --- Styles ---
        CellStyle headerStyle = createHeaderStyle(wbOf(sheet));
        CellStyle intStyle = wbOf(sheet).createCellStyle();
        intStyle.setDataFormat(wbOf(sheet).createDataFormat().getFormat("#,##0"));
        CellStyle decStyle = wbOf(sheet).createCellStyle();
        decStyle.setDataFormat(wbOf(sheet).createDataFormat().getFormat("#,##0.00"));

        // --- Header ---
        Row head = sheet.createRow(0);
        head.setHeightInPoints(24f);
        for (int c = 0; c < colCount; c++) {
            Cell cell = head.createCell(c);
            cell.setCellValue(m.getColumnName(c));
            cell.setCellStyle(headerStyle);
        }

        // --- Dữ liệu ---
        for (int r = 0; r < rowCount; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < colCount; c++) {
                Object v = m.getValueAt(r, c);
                Cell cell = row.createCell(c);
                if (v == null) {
                    cell.setBlank();
                    continue;
                }
                if (v instanceof Number num) {
                    double d = num.doubleValue();
                    if (Double.isNaN(d) || Double.isInfinite(d)) {
                        cell.setCellValue(String.valueOf(v));
                    } else {
                        cell.setCellValue(d);
                        cell.setCellStyle(isInteger(d) ? intStyle : decStyle);
                    }
                } else if (v instanceof Boolean b) {
                    cell.setCellValue(b);
                } else {
                    cell.setCellValue(String.valueOf(v));
                }
            }
        }

        // --- AutoFilter trên dòng header ---
        if (colCount > 0 && rowCount >= 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowCount), 0, colCount - 1));
        }

        // --- Đóng băng dòng header + tự co cột ---
        sheet.createFreezePane(0, 1);
        for (int c = 0; c < colCount; c++) {
            int maxLen = m.getColumnName(c).length();
            for (int r = 0; r < rowCount; r++) {
                Object v = m.getValueAt(r, c);
                if (v != null) maxLen = Math.max(maxLen, String.valueOf(v).length());
            }
            // giới hạn độ rộng để không bị quá rộng
            int width = Math.min(60, Math.max(12, maxLen + 3));
            sheet.setColumnWidth(c, width * 256);
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // Nền xanh #2563EB
        style.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 0x25, (byte) 0x63, (byte) 0xEB}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    private static boolean isInteger(double d) {
        return d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15;
    }

    /** Lấy workbook từ sheet (POI không expose getter trên interface Sheet). */
    private static Workbook wbOf(Sheet sheet) {
        return sheet.getWorkbook();
    }
}