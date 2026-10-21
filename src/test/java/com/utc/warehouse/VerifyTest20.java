package com.utc.warehouse;

import com.utc.warehouse.ui.product.ProductListPanel;
import com.utc.warehouse.util.AppPaths;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * VerifyTest20 — Chứng minh lỗi "1 sản phẩm có ảnh hiện cho MỌI dòng" đã được sửa.
 *
 * Cơ chế: Swing dùng chung 1 JLabel cho các dòng liên tiếp của 1 cột.
 * Nếu renderer không setIcon(null) khi dòng không có ảnh, icon sẽ bị "dính".
 */
public class VerifyTest20 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.out.println("=== KIEM CHUNG LOI HIEN ANH CHO MOI DONG ===\n");

        // Chuẩn bị 1 file ảnh thật trong thư mục ảnh sản phẩm
        File dir = new File(AppPaths.getProductImageDir().toString());
        dir.mkdirs();
        BufferedImage sample = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sample.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        File f = new File(dir, "verifytest20.jpg");
        ImageIO.write(sample, "jpg", f);
        System.out.println("Anh mau: " + f.getName());

        // Dung table don gian: 3 dong - chi dong GIUA co anh
        Object[][] data = {
                {null, 1, "A"},
                {null, 2, "B"},
                {null, 3, "C"},
        };
        javax.swing.table.DefaultTableModel tm = new javax.swing.table.DefaultTableModel(
                new String[]{"Anh", "ID", "Ten"}, 0);
        tm.addRow(new Object[]{null, 1, "A"});
        tm.addRow(new Object[]{null, 2, "B"});
        tm.addRow(new Object[]{null, 3, "C"});
        JTable t = new JTable(tm);
        t.setSize(400, 120);

        // Dung renderer THAT cua ProductListPanel
        ProductListPanel panel = new ProductListPanel();
        Field rf = null;
        for (Class<?> c = ProductListPanel.class; c != null; c = c.getSuperclass()) {
            try { rf = c.getDeclaredField("imagePathCache"); break; }
            catch (NoSuchFieldException ignored) { }
        }
        if (rf == null) { System.out.println("Khong tim thay imagePathCache"); return; }
        rf.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<Integer, String> cache =
                (java.util.Map<Integer, String>) rf.get(panel);
        cache.put(2, "verifytest20.jpg");   // chi san pham ID=2 co anh

        Class<?> rcls = null;
        for (Class<?> c : ProductListPanel.class.getDeclaredClasses())
            if (c.getSimpleName().contains("Renderer")) rcls = c;
        if (rcls == null) { System.out.println("Khong tim thay renderer"); return; }
        Method m = rcls.getDeclaredMethod("getTableCellRendererComponent",
                JTable.class, Object.class, boolean.class, boolean.class, int.class, int.class);
        m.setAccessible(true);

        var ctor = rcls.getDeclaredConstructor(ProductListPanel.class);
        ctor.setAccessible(true);
        DefaultTableCellRenderer r = (DefaultTableCellRenderer) ctor.newInstance(panel);

        System.out.println("\nKet qua render tung dong (dung CHUNG mot JLabel):");
        int[] rows = {0, 1, 2, 0, 1};
        String[] expect = {"khong", "CO ANH", "khong", "khong", "CO ANH"};

        for (int row : rows) {
            JLabel l = (JLabel) m.invoke(r, t, null, false, false, row, 0);
            boolean hasIcon = l.getIcon() != null;
            String txt = l.getText();
            boolean ok;

            if (row == 1) {
                ok = hasIcon;
                System.out.printf("  dong %d (ID=2, co anh): icon=%-5s text='%s'  -> %s%n",
                        row, hasIcon, txt, ok ? "OK" : "SAI - phai co icon!");
            } else {
                ok = !hasIcon;
                System.out.printf("  dong %d (khong anh):  icon=%-5s text='%s'  -> %s%n",
                        row, hasIcon, txt, ok ? "OK" : "SAI - icon bi dinh!");
            }
            check("Dong " + row + " render dung", ok,
                    "icon=" + (hasIcon ? "CO" : "khong") + ", text='" + txt + "'");
        }

        f.delete();
        System.out.println("\n=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) pass++; else fail++;
    }
}