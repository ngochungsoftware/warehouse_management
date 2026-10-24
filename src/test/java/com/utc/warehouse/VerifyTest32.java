package com.utc.warehouse;

import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.report.InventoryReportPanel;

import javax.swing.*;
import java.awt.*;

/**
 * VerifyTest32 — Kiểm chứng trang tổng quan cuộn được khi cửa sổ nhỏ.
 *
 * <p>Bug gốc: dashboard cao cố định (4 hàng KPI + 2 chart ≈ 1000px) nhưng không
 * có JScrollPane. Thu nhỏ cửa sổ -> phần dưới (2 chart) bị cắt, KHÔNG lăn
 * chuột xuống được.
 *
 * <p>Test KHÔNG cần database — chỉ đo layout ở kích thước cửa sổ nhỏ.
 */
public class VerifyTest32 {

    private static int pass = 0, fail = 0;

    /** Panel cao cố định, mô phỏng dashboard. */
    private static class TallPanel extends BasePanel {
        TallPanel(boolean scroll) {
            super("test");
            scrollContent = scroll;
        }
        @Override protected JComponent buildContent() {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            // 8 hàng x 140px = 1120px, cao hơn viewport 420px
            for (int i = 0; i < 8; i++) {
                JPanel row = new JPanel();
                row.setPreferredSize(new Dimension(600, 140));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
                p.add(row);
                p.add(Box.createVerticalStrut(12));
            }
            return p;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM CHỮNG CUỘN TRANG TỔNG QUAN ===\n");

        // ---------- 1. Panel bật scrollContent (như trang tổng quan) ----------
        TallPanel scrolled = new TallPanel(true);
        scrolled.init();
        JScrollPane sp = layout(scrolled, 900, 420);

        checkTrue("Panel bật scrollContent được bọc JScrollPane", sp != null);

        if (sp != null) {
            int viewH = sp.getViewport().getHeight();
            int prefH = sp.getViewport().getView().getPreferredSize().height;
            System.out.println("       viewport=" + viewH + "px, nội dung=" + prefH + "px");

            checkTrue("Viewport có kích thước hợp lệ (> 0)", viewH > 0);
            checkTrue("Nội dung cao hơn viewport -> " + prefH + " > " + viewH, prefH > viewH);
            checkTrue("Thanh cuộn dọc khả dụng",
                    sp.getVerticalScrollBar().isEnabled());
            checkTrue("Không có thanh cuộn ngang",
                    sp.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // Cuộn thật tới cuối
            JScrollBar vsb = sp.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
            checkTrue("Lăn chuột cuộn tới cuối được (value=" + vsb.getValue() + ")",
                    vsb.getValue() > 0);
        }

        // ---------- 2. Panel KHÔNG bật (màn bảng) không bị bọc thêm ----------
        TallPanel plain = new TallPanel(false);
        plain.init();
        layout(plain, 900, 420);
        checkTrue("Panel mặc định KHÔNG bị bọc scroll (màn bảng giữ nguyên hành vi)",
                findScroll(plain) == null);

        // ---------- 3. Trang tổng quan thật: không glue, nội dung cao hơn khung ----------
        InventoryReportPanel dash = new InventoryReportPanel();
        dash.init();
        layout(dash, 1000, 400);   // cửa sổ thấp

        JScrollPane real = findScroll(dash);
        checkTrue("Trang Tổng quan có thanh cuộn", real != null);
        if (real != null) {
            int viewH = real.getViewport().getHeight();
            int prefH = real.getViewport().getView().getPreferredSize().height;
            System.out.println("       viewport=" + viewH + "px, nội dung=" + prefH + "px");
            checkTrue("Nội dung dashboard cao hơn viewport (" + prefH + " > " + viewH + ")",
                    prefH > viewH);
            checkTrue("Thanh cuộn dọc của dashboard đang bật",
                    real.getVerticalScrollBar().isEnabled());
        }

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    /**
     * Layout panel trong kích thước w x h mà không cần JFrame, để chạy được
     * cả khi headless.
     *
     * <p>Không dùng extentSize của viewport vì headless JViewport không thực
     * sự layout (trả về 0). Thay vào đó ta tự đặt kích thước viewport để phản
     * ánh đúng hành vi lúc chạy thật.
     */
    private static JScrollPane layout(BasePanel panel, int w, int h) {
        JPanel host = new JPanel(new BorderLayout());
        host.setSize(w, h);
        host.add(panel, BorderLayout.CENTER);
        host.doLayout();
        panel.setSize(w, h);
        panel.doLayout();

        JScrollPane sp = findScroll(panel);
        if (sp != null) {
            // Mô phỏng viewport nhỏ hơn nội dung như thật
            sp.setSize(w, h);
            sp.getViewport().setSize(w, h);
            sp.doLayout();
        }
        return sp;
    }

    /** Tìm JScrollPane đầu tiên trong cây component. */
    private static JScrollPane findScroll(Container root) {
        if (root instanceof JScrollPane s) return s;
        for (Component c : root.getComponents()) {
            if (c instanceof JScrollPane s) return s;
            if (c instanceof Container child) {
                JScrollPane r = findScroll(child);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }
}