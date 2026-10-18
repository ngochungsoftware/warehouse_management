package com.utc.warehouse.ui.common;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quản lý phím tắt cho 1 panel / frame.
 *
 * <p>Cách dùng:
 * <pre>{@code
 *   ShortcutManager sc = new ShortcutManager(panel);
 *   sc.bind("ctrl N", "new",     this::onAdd);     // Ctrl+N -> Thêm
 *   sc.bind("DELETE",  "delete",  this::onDelete);  // Delete   -> Xoá
 *   sc.bind("F5",      "refresh", this::reload);    // F5       -> Làm mới
 *   sc.bind("ctrl F",  "search",  () -> searchField.requestFocus());
 * }</pre>
 *
 * <p>Cú pháp phím: ctrl/alt/shift/meta + SPACE + tên phím (F1..F12, DELETE, INSERT, ESCAPE, ENTER, A..Z, 0..9, COMMA, ...).
 * So khớp KHÔNG phân biệt hoa/thường với chữ cái.
 */
public class ShortcutManager {

    private final java.awt.Component root;
    private final java.util.Map<String, Action> actions = new java.util.LinkedHashMap<>();

    public ShortcutManager(java.awt.Component root) {
        this.root = root;
    }

    /** Bind phím tắt với 1 hành động. */
    public ShortcutManager bind(String keySpec, String name, Runnable action) {
        return bind(keySpec, name, action, null);
    }

    /** Bind phím tắt với hành động + mô tả (dùng cho dialog F1). */
    public ShortcutManager bind(String keySpec, String name, Runnable action, String description) {
        KeyStroke ks = parse(keySpec);
        String actionKey = "sc:" + name;
        Action a = new AbstractAction(name, (javax.swing.Icon) null) {
            @Override public void actionPerformed(ActionEvent e) {
                try { action.run(); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(root),
                            "Lỗi khi thực hiện '" + name + "':\n" + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        a.putValue(Action.SHORT_DESCRIPTION, description == null ? name : description);
        a.putValue("scKeySpec",  humanize(keySpec));
        a.putValue("scName",     name);
        a.putValue("scDesc",     description == null ? "" : description);
        InputMap imap = inputMapOf(root, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap amap = actionMapOf(root);
        if (imap != null && amap != null) {
            imap.put(ks, actionKey);
            amap.put(actionKey, a);
        }
        actions.put(name, a);
        return this;
    }

    /** Trả về InputMap của root, làm việc với cả JComponent, JFrame, JDialog, JRootPane. */
    public static InputMap inputMapOf(java.awt.Component c, int condition) {
        if (c instanceof JComponent jc) return jc.getInputMap(condition);
        if (c instanceof JFrame jf)     return jf.getRootPane().getInputMap(condition);
        if (c instanceof JDialog jd)    return jd.getRootPane().getInputMap(condition);
        if (c instanceof JRootPane jr)  return jr.getInputMap(condition);
        return null;
    }
    /** Trả về ActionMap của root. */
    public static ActionMap actionMapOf(java.awt.Component c) {
        if (c instanceof JComponent jc) return jc.getActionMap();
        if (c instanceof JFrame jf)     return jf.getRootPane().getActionMap();
        if (c instanceof JDialog jd)    return jd.getRootPane().getActionMap();
        if (c instanceof JRootPane jr)  return jr.getActionMap();
        return null;
    }

    /** Trả về bảng phím tắt đang bind: {name, key, description}. */
    public java.util.List<Entry> getBindings() {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        for (Action a : actions.values()) {
            out.add(new Entry(
                    (String) a.getValue("scName"),
                    (String) a.getValue("scKeySpec"),
                    (String) a.getValue("scDesc")));
        }
        return out;
    }

    public record Entry(String name, String key, String description) { }
    private static KeyStroke parse(String spec) {
        if (spec == null) throw new IllegalArgumentException("null key");
        boolean ctrl = false, alt = false, shift = false, meta = false;
        String s = spec.trim().toUpperCase();
        String[] parts = s.split("\\s+");
        String last = parts[parts.length - 1];
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i]) {
                case "CTRL", "CONTROL" -> ctrl = true;
                case "ALT"             -> alt = true;
                case "SHIFT"           -> shift = true;
                case "META", "CMD"     -> meta = true;
                default -> throw new IllegalArgumentException("Unknown modifier: " + parts[i]);
            }
        }
        int code;
        // Tên phím đặc biệt
        switch (last) {
            case "DELETE"  -> code = java.awt.event.KeyEvent.VK_DELETE;
            case "INSERT"  -> code = java.awt.event.KeyEvent.VK_INSERT;
            case "ESCAPE", "ESC" -> code = java.awt.event.KeyEvent.VK_ESCAPE;
            case "ENTER", "RETURN" -> code = java.awt.event.KeyEvent.VK_ENTER;
            case "SPACE"   -> code = java.awt.event.KeyEvent.VK_SPACE;
            case "TAB"     -> code = java.awt.event.KeyEvent.VK_TAB;
            case "HOME"    -> code = java.awt.event.KeyEvent.VK_HOME;
            case "END"     -> code = java.awt.event.KeyEvent.VK_END;
            case "PAGEUP", "PGUP"  -> code = java.awt.event.KeyEvent.VK_PAGE_UP;
            case "PAGEDOWN", "PGDN"-> code = java.awt.event.KeyEvent.VK_PAGE_DOWN;
            case "UP"      -> code = java.awt.event.KeyEvent.VK_UP;
            case "DOWN"    -> code = java.awt.event.KeyEvent.VK_DOWN;
            case "LEFT"    -> code = java.awt.event.KeyEvent.VK_LEFT;
            case "RIGHT"   -> code = java.awt.event.KeyEvent.VK_RIGHT;
            case "PLUS"    -> code = java.awt.event.KeyEvent.VK_PLUS;
            case "MINUS"   -> code = java.awt.event.KeyEvent.VK_MINUS;
            case "COMMA"   -> code = java.awt.event.KeyEvent.VK_COMMA;
            case "DOT", "PERIOD" -> code = java.awt.event.KeyEvent.VK_PERIOD;
            case "SLASH"   -> code = java.awt.event.KeyEvent.VK_SLASH;
            case "BACKSPACE" -> code = java.awt.event.KeyEvent.VK_BACK_SPACE;
            default -> {
                if (last.startsWith("F") && last.length() > 1) {
                    try { code = java.awt.event.KeyEvent.class.getField("VK_" + last).getInt(null); }
                    catch (Exception ex) { throw new IllegalArgumentException("Unknown key: " + last); }
                } else if (last.length() == 1) {
                    code = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(last.charAt(0));
                    if (code == java.awt.event.KeyEvent.VK_UNDEFINED) {
                        // Fallback cho ký tự không phải chữ số/chữ cái
                        code = (int) last.charAt(0);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown key: " + last);
                }
            }
        }
        return KeyStroke.getKeyStroke(code, modMask(ctrl, alt, shift, meta));
    }

    private static int modMask(boolean ctrl, boolean alt, boolean shift, boolean meta) {
        int m = 0;
        if (ctrl)  m |= java.awt.event.InputEvent.CTRL_DOWN_MASK;
        if (alt)   m |= java.awt.event.InputEvent.ALT_DOWN_MASK;
        if (shift) m |= java.awt.event.InputEvent.SHIFT_DOWN_MASK;
        if (meta)  m |= java.awt.event.InputEvent.META_DOWN_MASK;
        return m;
    }

    private static String humanize(String spec) {
        if (spec == null) return "";
        String s = spec.trim();
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (sb.length() > 0) sb.append('+');
            sb.append(capitalize(parts[i]));
        }
        if (sb.length() > 0) sb.append('+');
        sb.append(capitalize(parts[parts.length - 1]));
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
