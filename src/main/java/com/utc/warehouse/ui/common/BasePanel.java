package com.utc.warehouse.ui.common;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel nền: header (title + subtitle) + toolbar (nút + tìm kiếm) + content + footer.
 * Style thống nhất cho mọi page. Tự động bind phím tắt mặc định khi init().
 */
public class BasePanel extends JPanel {

    public final String title;
    protected final String subtitle;

    /** Subclass set true để cho phép F5 reload, Ctrl+N add, Del delete, ... */
    protected boolean enableDefaultShortcuts = true;

    /**
     * Subclass set true để nội dung dài được cuộn bằng chuột.
     *
     * <p>Mặc định false: các màn bảng đã có {@code JScrollPane} riêng quanh
     * bảng, bọc thêm ở ngoài sẽ tạo 2 thanh cuộn lồng nhau và con lăn chuột
     * bị "kẹt". Chỉ bật cho màn dạng dashboard — nội dung cao cố định
     * (KPI + chart) và bị cắt mất khi thu nhỏ cửa sổ.
     */
    protected boolean scrollContent = false;

    public BasePanel(String title) { this(title, null); }

    public BasePanel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        setLayout(new BorderLayout(0, Theme.GAP));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    }

    /** Override để thêm header riêng. Mặc định: pageHeader(card). */
    protected JComponent buildHeader() {
        return ModernComponents.pageHeader(title, subtitle);
    }

    /** Override để thêm toolbar (nút thêm/sửa/xóa, ô tìm kiếm...). */
    protected JComponent buildToolbar() { return null; }

    /** Override để thêm nội dung chính. */
    protected JComponent buildContent() {
        JLabel l = new JLabel("(Chưa có nội dung)");
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** Override để thêm thanh trạng thái. */
    protected JComponent buildFooter() { return null; }

    /** Override để thêm hành động khi bấm "Thêm" (Ctrl+N). */
    protected void onShortcutAdd() { }

    /** Override để thêm hành động khi bấm "Sửa" (Ctrl+E). */
    protected void onShortcutEdit() { }

    /** Override để thêm hành động khi bấm "Xoá" (Delete). */
    protected void onShortcutDelete() { }

    /** Override để thêm hành động khi bấm "Làm mới" (F5). */
    protected void onShortcutRefresh() { reload(); }

    /** Override để thêm hành động khi bấm "Tìm" (Ctrl+F). */
    protected void onShortcutFocusSearch() {
        JTextField tf = findFirstSearchField();
        if (tf != null) { tf.requestFocusInWindow(); tf.selectAll(); }
    }

    /** Override để reload dữ liệu (mặc định no-op, panel nào có bảng thì override). */
    public void reload() { }

    public final void init() {
        JComponent header  = buildHeader();
        JComponent toolbar = buildToolbar();
        JComponent content = buildContent();
        JComponent footer  = buildFooter();

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        if (header  != null) { header.setAlignmentX(Component.LEFT_ALIGNMENT);  north.add(header); }
        if (toolbar != null) { toolbar.setAlignmentX(Component.LEFT_ALIGNMENT); north.add(toolbar); }

        JPanel contentCard = ModernComponents.card();
        contentCard.setLayout(new BorderLayout());
        contentCard.setBackground(Theme.CARD_BG);
        if (content != null) contentCard.add(content, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(scrollContent ? wrapScrollable(contentCard) : contentCard, BorderLayout.CENTER);
        if (footer != null) add(footer, BorderLayout.SOUTH);

        if (enableDefaultShortcuts) installDefaultShortcuts();

        // Subclass có thể override reloadDataOnShow() để auto-load khi panel
        // lần đầu được hiển thị. Mặc định gọi reload().
    }

    /**
     * Bọc component trong {@link JScrollPane} để cuộn được bằng chuột khi
     * nội dung cao hơn khung cửa sổ.
     *
     * <p>Bỏ thanh cuộn ngang (chiều ngang đã vừa khung) và cho bánh xe chuột
     * cuộn chính là bảng khi đang đặt con trỏ lên vùng có scroll riêng.
     */
    private JComponent wrapScrollable(JComponent inner) {
        JScrollPane sp = new JScrollPane(inner);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        // Dashboard nền trắng — không đổi màu nền viewport để tránh dải lệch tone.
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Cuộn 1 "page" bằng 1/10 chiều cao viewport, gần với cảm giác lăn chuột
        // của trình duyệt; mặc định JScrollPane lăn cả trang khiến nhảy quá đà.
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Được gọi 1 lần sau init() để load dữ liệu ban đầu. Mặc định gọi reload(). */
    public void reloadDataOnShow() {
        SwingUtilities.invokeLater(this::reload);
    }

    /* ============== Shortcut mặc định ============== */
    protected void installDefaultShortcuts() {
        ShortcutManager sc = new ShortcutManager(this);
        sc.bind("ctrl N",  "add",     this::onShortcutAdd,      "Thêm mới");
        sc.bind("ctrl E",  "edit",    this::onShortcutEdit,     "Sửa mục đang chọn");
        sc.bind("DELETE",  "delete",  this::onShortcutDelete,   "Xoá mục đang chọn");
        sc.bind("F5",      "refresh", this::onShortcutRefresh,  "Làm mới danh sách");
        sc.bind("ctrl F",  "search",  this::onShortcutFocusSearch, "Tìm kiếm");
    }

    /** Helper: tìm JTextField đầu tiên trong cây component (dùng cho search). */
    public JTextField findFirstSearchField() {
        return (JTextField) findFirst(this,
                c -> c instanceof JTextField
                        && !(c instanceof JPasswordField)
                        && c.isShowing());
    }

    /** Helper: tìm JTable đầu tiên trong cây component. */
    public JTable findFirstTable() {
        return (JTable) findFirst(this, c -> c instanceof JTable && c.isShowing());
    }

    /** DFS tìm component đầu tiên thoả mãn predicate. */
    public static Component findFirst(Container root, java.util.function.Predicate<Component> pred) {
        for (Component c : root.getComponents()) {
            if (pred.test(c)) return c;
            if (c instanceof Container child) {
                Component f = findFirst(child, pred);
                if (f != null) return f;
            }
        }
        return null;
    }

    /** Helper: tạo toolbar chuẩn (panel ngang có padding). */
    protected static JPanel createToolbar() {
        JPanel tb = new JPanel();
        tb.setLayout(new BoxLayout(tb, BoxLayout.X_AXIS));
        tb.setOpaque(false);
        tb.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return tb;
    }
}
