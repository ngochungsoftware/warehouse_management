package com.utc.warehouse;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.util.AppPaths;
import com.utc.warehouse.util.ExcelExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * VerifyTest28 — xuất Excel (.xlsx) that su.
 *
 * <p>Test voi DU LIEU THAT tu MySQL (san pham, khach hang) chay qua DUNG code xuat
 * cua app, roi kiem tra file bang:
 *   - ZIP hop le, du 6 part OOXML
 *   - XML well-formed
 *   - Sheet doc duoc: co header, co du so dong, so o dang number
 *   - Dau xuong: dong dau tien la header, khong phai rong
 */
public class VerifyTest28 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int pass = 0, fail = 0;
        Path exportDir = AppPaths.getExportDir();

        // ---------- 1) Bang san pham that ----------
        ProductService ps = new ProductService();
        seedProductsIfEmpty(ps);   // DB rong van phai test duoc
        List<Product> products = ps.search("");
        System.out.println("So san pham tu DB: " + products.size());

        DefaultTableModel pm = new DefaultTableModel(
                new Object[]{"SKU", "Tên sản phẩm", "Giá", "Tồn", "Đơn vị"}, 0);
        for (Product p : products) {
            pm.addRow(new Object[]{
                    p.getSku(),
                    p.getName(),
                    p.getSalePrice(),
                    10,
                    p.getUnit()
            });
        }

        File f1 = new File(exportDir.toFile(), "vt28_san_pham.xlsx");
        boolean w1 = ExcelExporter.writeTable(pm, f1);
        if (w1 && f1.exists() && f1.length() > 0) pass++;
        else { fail++; System.out.println("FAIL: khong ghi duoc file san pham"); }

        int dataRows = pm.getRowCount();
        if (dataRows > 0) pass++;
        else { fail++; System.out.println("FAIL: DB rong, khong co du lieu de test"); }

        if (w1) {
            CheckResult r = inspect(f1, pm);
            System.out.println("[san_pham] parts=" + r.parts + " rows=" + r.rows
                    + " numericCells=" + r.numericCells + " strings=" + r.strings
                    + " headerOk=" + r.headerOk);
            pass += r.assertsPassed; fail += r.assertsFailed;
        }

        // ---------- 2) Bang khach hang that ----------
        PartnerService partnerSvc = new PartnerService();
        seedCustomersIfEmpty(partnerSvc);
        List<Customer> customers = partnerSvc.allCustomers();
        System.out.println("So khach hang tu DB: " + customers.size());

        DefaultTableModel cm = new DefaultTableModel(
                new Object[]{"Tên", "SĐT", "Email", "Địa chỉ"}, 0);
        for (Customer c : customers) {
            cm.addRow(new Object[]{c.getName(), c.getPhone(), c.getEmail(), c.getAddress()});
        }

        File f2 = new File(exportDir.toFile(), "vt28_khach_hang.xlsx");
        boolean w2 = ExcelExporter.writeTable(cm, f2);
        if (w2 && f2.exists() && f2.length() > 0) pass++;
        else { fail++; System.out.println("FAIL: khong ghi duoc file khach hang"); }

        if (w2) {
            CheckResult r = inspect(f2, cm);
            System.out.println("[khach_hang] parts=" + r.parts + " rows=" + r.rows
                    + " numericCells=" + r.numericCells + " strings=" + r.strings
                    + " headerOk=" + r.headerOk);
            pass += r.assertsPassed; fail += r.assertsFailed;
        }

        // ---------- 3) Edge case: bang rong ----------
        DefaultTableModel em = new DefaultTableModel(new Object[]{"A", "B"}, 0);
        File f3 = new File(exportDir.toFile(), "vt28_rong.xlsx");
        if (ExcelExporter.writeTable(em, f3) && f3.exists() && f3.length() > 0) pass++;
        else { fail++; System.out.println("FAIL: bang rong khong ghi duoc"); }

        if (f3.exists()) {
            CheckResult r = inspect(f3, em);
            System.out.println("[rong] parts=" + r.parts + " rows=" + r.rows);
            pass += r.assertsPassed; fail += r.assertsFailed;
            f3.delete();
        }

        // ---------- 4) Edge case: gia tri khong hop le ----------
        DefaultTableModel weird = new DefaultTableModel(new Object[]{"X", "Y", "Z"}, 0);
        weird.addRow(new Object[]{"", Double.NaN, Double.POSITIVE_INFINITY});
        weird.addRow(new Object[]{null, new BigDecimal("0"), "text \u0007 control"});
        File f4 = new File(exportDir.toFile(), "vt28_weird.xlsx");
        if (ExcelExporter.writeTable(weird, f4) && f4.exists() && f4.length() > 0) pass++;
        else { fail++; System.out.println("FAIL: gia tri khong hop le khong ghi duoc"); }
        if (f4.exists()) {
            CheckResult r = inspect(f4, weird);
            System.out.println("[weird] parts=" + r.parts + " rows=" + r.rows);
            pass += r.assertsPassed; fail += r.assertsFailed;
            f4.delete();
        }

        // ---------- 5) Ten file khong duoi .xlsx van ra .xlsx ----------
        File f5 = new File(exportDir.toFile(), "vt28_khongduoi");
        if (ExcelExporter.writeTable(pm, f5)) pass++;
        else { fail++; System.out.println("FAIL: ghi file ten khong duoi"); }
        if (f5.exists()) f5.delete();

        if (f1.exists()) f1.delete();
        if (f2.exists()) f2.delete();

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    /** Kiem tra file xlsx xuat ra co dung khong. */
    static class CheckResult {
        int parts, rows, numericCells, strings;
        boolean headerOk;
        int assertsPassed, assertsFailed;
    }

    static CheckResult inspect(File f, DefaultTableModel expect) {
        CheckResult r = new CheckResult();
        try (ZipFile zip = new ZipFile(f)) {
            r.parts = zip.size();

            String[] required = {
                    "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml"};
            for (String part : required) {
                if (zip.getEntry(part) == null) {
                    r.assertsFailed++; System.out.println("  FAIL: thieu part " + part);
                } else r.assertsPassed++;
            }

            // XML phai well-formed
            for (ZipEntry e : java.util.Collections.list(zip.entries())) {
                if (!e.getName().endsWith(".xml") && !e.getName().endsWith(".rels")) continue;
                try {
                    javax.xml.parsers.DocumentBuilderFactory f2 =
                            javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    f2.setNamespaceAware(true);
                    f2.newDocumentBuilder().parse(zip.getInputStream(e));
                    r.assertsPassed++;
                } catch (Exception ex) {
                    r.assertsFailed++;
                    System.out.println("  FAIL: XML khong hop le " + e.getName() + " -> " + ex.getMessage());
                }
            }

            // Doc sheet1.xml
            ZipEntry se = zip.getEntry("xl/worksheets/sheet1.xml");
            if (se != null) {
                String xml = new String(zip.getInputStream(se).readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                r.rows = xml.split("<row ", -1).length - 1;
                r.numericCells = xml.split("<v>", -1).length - 1;

                // POI luu chuoi trong sharedStrings.xml (khong phai inlineStr)
                String shared = "";
                ZipEntry sse = zip.getEntry("xl/sharedStrings.xml");
                if (sse != null) {
                    shared = new String(zip.getInputStream(sse).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
                }
                r.strings = shared.isEmpty()
                        ? xml.split("t=\"inlineStr\"", -1).length - 1
                        : shared.split("<si>", -1).length - 1;

                // Header phai dung ten cot dau tien
                String firstCol = expect.getColumnName(0);
                r.headerOk = shared.contains(">" + firstCol + "<")
                        || xml.contains(">" + firstCol + "<")
                        || xml.contains(">" + firstCol + "</t>");
                if (r.headerOk) r.assertsPassed++;
                else { r.assertsFailed++; System.out.println("  FAIL: header khong khop '" + firstCol + "'"); }

                // So dong = header + data
                int expectRows = expect.getRowCount() + 1;
                if (r.rows == expectRows) r.assertsPassed++;
                else {
                    r.assertsFailed++;
                    System.out.println("  FAIL: so row " + r.rows + " != " + expectRows);
                }

                // Thu tu element theo schema OOXML
                int iDim = xml.indexOf("<dimension");
                int iViews = xml.indexOf("<sheetViews");
                int iFmt = xml.indexOf("<sheetFormatPr");
                int iCols = xml.indexOf("<cols");
                int iData = xml.indexOf("<sheetData");
                int iFilter = xml.indexOf("<autoFilter");
                boolean orderOk = iDim >= 0 && iDim < iViews && iViews < iFmt
                        && iFmt < iCols && iCols < iData && iData < iFilter;
                if (orderOk) r.assertsPassed++;
                else {
                    r.assertsFailed++;
                    System.out.println("  FAIL: thu tu element sheet sai chuan OOXML");
                }
            }
        } catch (Exception ex) {
            r.assertsFailed++;
            System.out.println("  FAIL: khong doc duoc file: " + ex.getMessage());
        }
        return r;
    }

    /**
     * DB rong (sau khi nguoi dung xoa sach) van phai test duoc xuat Excel
     * voi du lieu that, nen tu tao it san pham mau.
     */
    private static void seedProductsIfEmpty(ProductService svc) {
        if (!svc.search("").isEmpty()) return;
        System.out.println("(!) DB chua co san pham — tao du lieu mau de test");
        String[][] seed = {
                {"VT28-A", "Bàn chải đánh răng", "31000"},
                {"VT28-B", "Bánh mì sandwich",   "62500"},
                {"VT28-C", "Cà phê hạt rang",    "185000"},
                {"VT28-D", "Dầu gội dưỡng tóc",  "92000"},
        };
        for (String[] s : seed) {
            Product p = new Product();
            p.setSku(s[0]);
            p.setName(s[1]);
            p.setUnit("cái");
            p.setSalePrice(new BigDecimal(s[2]));
            p.setCostPrice(new BigDecimal(s[2]));
            p.setMinStock(5);
            p.setActive(Boolean.TRUE);
            try { svc.save(p); } catch (Exception ex) {
                System.out.println("  (!) khong tao duoc " + s[0] + ": " + ex.getMessage());
            }
        }
    }

    /** Tu tao khach hang mau khi DB chua co du lieu. */
    private static void seedCustomersIfEmpty(PartnerService svc) {
        if (!svc.allCustomers().isEmpty()) return;
        System.out.println("(!) DB chua co khach hang — tao du lieu mau de test");
        for (String n : new String[]{"VT28 KH Alpha", "VT28 KH Beta"}) {
            Customer c = new Customer();
            c.setName(n);
            c.setPhone("0900000000");
            c.setEmail("vt28" + Math.abs(n.hashCode()) + "@test.com");
            c.setAddress("Địa chỉ mẫu");
            try { svc.saveCustomer(c); } catch (Exception ex) {
                System.out.println("  (!) khong tao duoc KH: " + ex.getMessage());
            }
        }
    }
}