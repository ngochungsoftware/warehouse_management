package com.utc.warehouse.ui.stock;

import com.utc.warehouse.dao.ReportDAO;
import com.utc.warehouse.service.ReportService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ChartHelper;
import com.utc.warehouse.ui.common.TableExportHelper;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Báo cáo tồn kho tổng hợp: tồn hiện tại của TOÀN BỘ sản phẩm trong 1 bảng.
 *
 * <p>Tồn lấy từ {@link ReportDAO#getInventory()} — cùng công thức động
 * SUM(nhập) - SUM(xuất) mà màn Phiếu xuất / Hóa đơn dùng, nên số liệu ở đây
 * luôn khớp với số chặn khi xuất hàng.
 *
 * <p>Màn này chỉ ĐỌC, không sửa tồn. Muốn thay đổi tồn phải qua
 * Phiếu nhập / Phiếu xuất / Hóa đơn — đó là nơi ghi vào lịch sử biến động.
 */
public class StockInventoryPanel extends BasePanel {

    private final ReportService service = new ReportService();

    // 0=SKU, 1=Tên, 2=Danh mục, 3=ĐVT, 4=Tồn hiện tại, 5=Tồn tối thiểu, 6=Trạng thái
    private static final int COL_SKU   = 0;
    private static final int COL_CAT   = 2;
    private static final int COL_UNIT  = 3;
    private static final int COL_ONHAND= 4;
    private static final int COL_MIN   = 5;
    private static final int COL_STATUS= 6;

    private static final String[] COLS = {
            "SKU", "Tên sản phẩm", "Danh mục", "ĐVT",
            "Tồn hiện tại", "Tồn tối thiểu", "Trạng thái"
    };

    /** Các bộ lọc trạng thái. */
    private static final String F_ALL      = "Tất cả";
    private static final String F_LOW      = "Dưới mức tối thiểu";
    private static final String F_EMPTY    = "Hết hàng";

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private JComboBox<String> cbFilter;
    private JLabel lblSummary;

    /** Container của chart nhập/xuất — reload() sẽ thay nội dung bên trong. */
    private JPanel chartFlowContainer;

    /** Cache toàn bộ dữ liệu gốc — lọc/tìm kiếm chạy trên RAM, không query lại DB. */
    private List<ReportDAO.InventoryRow> allRows = new ArrayList<>();

    public StockInventoryPanel() {
        super("Báo cáo tồn kho", "Tồn hiện tại = tổng đã nhập − tổng đã xuất");
    }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        txtSearch = new JTextField(16);
        Theme.applyPlaceholder(txtSearch, "Tìm SKU / tên sản phẩm...");
        txtSearch.addActionListener(e -> applyFilter());

        cbFilter = new JComboBox<>(new String[]{F_ALL, F_LOW, F_EMPTY});
        cbFilter.addActionListener(e -> applyFilter());

        JButton btnRefresh = primaryButton("Làm mới");
        btnRefresh.setName("btnRefresh");
        btnRefresh.addActionListener(e -> reload());

        JButton btnExport = secondaryButton("Xuất Excel");
        btnExport.setName("btnExport");
        btnExport.addActionListener(e -> exportExcel());

        bar.add(new JLabel("Tìm:"));
        bar.add(txtSearch);
        bar.add(new JLabel("Lọc:"));
        bar.add(cbFilter);
        bar.add(btnRefresh);
        bar.add(btnExport);
        return bar;
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        table.setModel(model);

        TableUtils.centerColumn(table, COL_SKU);
        TableUtils.centerColumn(table, COL_CAT);
        TableUtils.centerColumn(table, COL_UNIT);
        TableUtils.rightColumn(table, COL_ONHAND);
        TableUtils.rightColumn(table, COL_MIN);

        // Tô màu riêng cho cột tồn: đỏ khi hết hàng, cam khi dưới ngưỡng.
        // Cần renderer tự vẽ màu nền theo giá trị, nên tự set thay vì dùng helper.
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());

        table.getColumnModel().getColumn(COL_SKU).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_CAT).setPreferredWidth(130);
        table.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(70);
        table.getColumnModel().getColumn(COL_ONHAND).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_MIN).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(150);

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        // Chart nhập/xuất 7 ngày gần nhất (đặt trên bảng, chiều cao cố định)
        chartFlowContainer = new JPanel(new BorderLayout());
        chartFlowContainer.setBackground(Color.WHITE);
        chartFlowContainer.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        chartFlowContainer.setPreferredSize(new Dimension(0, 240));
        chartFlowContainer.setMinimumSize(new Dimension(0, 240));
        chartFlowContainer.add(buildChartPlaceholder("Đang tải..."), BorderLayout.CENTER);
        wrap.add(chartFlowContainer, BorderLayout.NORTH);

        wrap.add(TableUtils.scrollFor(table), BorderLayout.CENTER);

        lblSummary = new JLabel(" ");
        lblSummary.setFont(Theme.FONT_SMALL);
        lblSummary.setForeground(Theme.TEXT_MUTED);
        lblSummary.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 0));
        wrap.add(lblSummary, BorderLayout.SOUTH);
        return wrap;
    }

    /* ================== Chart nhập / xuất ================== */

    private JLabel buildChartPlaceholder(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(0, 180));
        return l;
    }

    /**
     * Load chart nhập/xuất 7 ngày gần nhất.
     *
     * <p>Dùng SwingWorker để không block EDT khi query + vẽ chart.
     */
    private void loadFlowChartAsync() {
        if (chartFlowContainer == null) return;
        replaceChart(chartFlowContainer, buildChartPlaceholder("Đang tải dữ liệu nhập/xuất..."),
                "🔄 Nhập / Xuất kho 7 ngày gần nhất");

        new SwingWorker<Map<String, Map<String, Number>>, Void>() {
            @Override
            protected Map<String, Map<String, Number>> doInBackground() {
                LocalDate to = LocalDate.now();
                LocalDate from = to.minusDays(6);
                List<ReportDAO.StockFlowRow> rows = service.stockFlowByDate(from, to);

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
                Map<String, Number> imports = new TreeMap<>();
                Map<String, Number> exports = new TreeMap<>();
                // Fill sẵn 7 ngày để chart đều các cột
                for (int i = 0; i <= 6; i++) {
                    String key = fmt.format(from.plusDays(i));
                    imports.put(key, 0);
                    exports.put(key, 0);
                }
                for (ReportDAO.StockFlowRow r : rows) {
                    String key = fmt.format(r.getDate() );
                    if ("IMPORT".equals(r.getType())) imports.put(key, r.getQuantity());
                    else if ("EXPORT".equals(r.getType())) exports.put(key, r.getQuantity());
                }

                Map<String, Map<String, Number>> series = new LinkedHashMap<>();
                series.put("Nhập", imports);
                series.put("Xuất", exports);
                return series;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Map<String, Number>> data = get();
                    JPanel chart = ChartHelper.stackedBarChart("Nhập / Xuất 7 ngày gần nhất", data);
                    replaceChart(chartFlowContainer, chart, "🔄 Nhập / Xuất kho 7 ngày gần nhất");
                } catch (Exception ex) {
                    JLabel l = new JLabel("Lỗi tải chart: " + ex.getMessage(), SwingConstants.CENTER);
                    System.out.println(l);
                    l.setFont(Theme.FONT_NORMAL);
                    l.setForeground(Theme.DANGER);
                    replaceChart(chartFlowContainer, l, "🔄 Nhập / Xuất kho 7 ngày gần nhất");
                }
            }
        }.execute();
    }

    /** Thay nội dung chart bên trong container, giữ nguyên title bar. */
    private void replaceChart(JPanel container, Component newContent, String title) {
        container.removeAll();
        container.setLayout(new BorderLayout());
        container.add(buildChartTitleBar(title), BorderLayout.NORTH);
        container.add(newContent, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private JComponent buildChartTitleBar(String title) {
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_HEADER);
        titleLbl.setForeground(Theme.TEXT);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));

        JPanel titleWrap = new JPanel(new BorderLayout());
        titleWrap.setBackground(Color.WHITE);
        titleWrap.add(titleLbl, BorderLayout.CENTER);
        titleWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        return titleWrap;
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc =
                new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("F5",     "refresh", this::reload,    "Làm mới báo cáo tồn kho");
        sc.bind("ctrl F", "search",  this::focusSearch, "Tìm sản phẩm");
        sc.bind("ctrl X", "export",  this::exportExcel, "Xuất Excel");
    }

    @Override protected void onShortcutFocusSearch() { focusSearch(); }

    private void focusSearch() {
        if (txtSearch != null) { txtSearch.requestFocusInWindow(); txtSearch.selectAll(); }
    }

    public void reload() {
        try {
            allRows = service.getInventory();
            applyFilter();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải báo cáo tồn kho: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        // Chart load async, không block EDT
        loadFlowChartAsync();
    }

    /** Lọc + tìm kiếm trên danh sách đã load, không query lại DB. */
    private void applyFilter() {
        if (model == null) return; // chưa buildContent()

        String keyword = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String filter  = cbFilter == null ? F_ALL : String.valueOf(cbFilter.getSelectedItem());

        model.setRowCount(0);
        int shown = 0, lowCount = 0, emptyCount = 0;

        for (ReportDAO.InventoryRow r : allRows) {
            long onHand = r.getOnHand() == null ? 0L : r.getOnHand();
            int  min    = r.getMinStock() == null ? 0 : r.getMinStock();

            if (onHand < min) lowCount++;
            if (onHand <= 0)  emptyCount++;

            if (!keyword.isEmpty()) {
                String hay = (r.getSku() + " " + (r.getName() == null ? "" : r.getName())
                        + " " + (r.getCategoryName() == null ? "" : r.getCategoryName()))
                        .toLowerCase(Locale.ROOT);
                if (!hay.contains(keyword)) continue;
            }

            if (F_LOW.equals(filter)   && onHand >= min)  continue;
            if (F_EMPTY.equals(filter) && onHand > 0)      continue;

            model.addRow(new Object[]{
                    r.getSku(),
                    r.getName(),
                    r.getCategoryName() == null ? "(Chưa phân loại)" : r.getCategoryName(),
                    r.getUnit(),
                    onHand,
                    min,
                    statusText(onHand, min)
            });
            shown++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(shown).append(" / ").append(allRows.size()).append(" sản phẩm");
        sb.append("  •  ").append(emptyCount).append(" hết hàng");
        sb.append("  •  ").append(lowCount).append(" dưới mức tối thiểu");
        if (shown < allRows.size()) sb.append("  (đang lọc)");
        lblSummary.setText(sb.toString());
    }

    private static String statusText(long onHand, int min) {
        if (onHand <= 0) return "🔴 Hết hàng";
        if (onHand < min) return "🟠 Dưới mức tối thiểu";
        return "🟢 Bình thường";
    }

    /** Tô màu chữ theo trạng thái để mắt dễ quét. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(Theme.FONT_SMALL);
            if (!sel) {
                String s = String.valueOf(value);
                if (s.startsWith("🔴"))      l.setForeground(Theme.DANGER);
                else if (s.startsWith("🟠")) l.setForeground(Theme.WARNING);
                else if (s.startsWith("🟢")) l.setForeground(Theme.SUCCESS);
            }
            return l;
        }
    }

    private void exportExcel() {
        if (model == null) return;
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.");
            return;
        }
        TableExportHelper.exportModel(model, this, "bao_cao_ton_kho");
    }

    private JButton primaryButton(String t) {
        return styleButton(t, Theme.BRAND, Color.WHITE);
    }

    private JButton secondaryButton(String t) {
        return styleButton(t, Theme.ACCENT, Color.WHITE);
    }

    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(120, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}