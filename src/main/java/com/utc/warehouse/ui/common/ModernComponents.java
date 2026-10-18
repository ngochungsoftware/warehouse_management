package com.utc.warehouse.ui.common;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Các component tùy biến: button, text field, card, v.v.
 * Tất cả đều flat + bo góc + hover đẹp.
 */
public final class ModernComponents {

    private ModernComponents() { }

    /* ===================== BUTTONS ===================== */

    /** Button primary (xanh lá brand). */
    public static JButton primaryButton(String text) {
        return makeButton(text, Theme.BRAND, Color.WHITE, true);
    }

    /** Button phụ (viền xám). */
    public static JButton secondaryButton(String text) {
        return makeButton(text, Color.WHITE, Theme.TEXT, false);
    }

    /** Button nguy hiểm (đỏ). */
    public static JButton dangerButton(String text) {
        return makeButton(text, Theme.DANGER, Color.WHITE, true);
    }

    /** Button ghost (trong suốt, chữ thường). */
    public static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setForeground(Theme.TEXT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        return b;
    }

    private static JButton makeButton(String text, Color bg, Color fg, boolean filled) {
        JButton b = new JButton(text) {
            private boolean hover = false;
            private boolean press = false;
            { addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { press = true;  repaint(); }
                @Override public void mouseReleased(MouseEvent e) { press = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color paint = bg;
                if (!isEnabled()) {
                    paint = new Color(0xCBD5E1);
                } else if (filled) {
                    if (press)      paint = darken(bg, 0.18f);
                    else if (hover) paint = darken(bg, 0.08f);
                } else {
                    paint = hover ? Theme.BG : Color.WHITE;
                }
                g2.setColor(paint);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, Theme.RADIUS, Theme.RADIUS));
                if (!filled) {
                    g2.setColor(Theme.BORDER);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.FONT_HEADER);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(Theme.PADDING_BTN);
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        return b;
    }

    private static Color darken(Color c, float amt) {
        return new Color(
                Math.max(0, (int) (c.getRed()   * (1 - amt))),
                Math.max(0, (int) (c.getGreen() * (1 - amt))),
                Math.max(0, (int) (c.getBlue()  * (1 - amt))));
    }

    /* ===================== CARDS ===================== */

    /** Panel dạng card trắng, có bo góc + shadow nhẹ. */
    public static JPanel card() {
        return card(Theme.CARD_BG, Theme.RADIUS);
    }

    public static JPanel card(Color bg, int radius) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(2, 4, w - 4, h - 4, radius, radius);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 4, h - 6, radius, radius);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 4, h - 6, radius, radius);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    /* ===================== HEADER (PAGE TITLE) ===================== */

    /** Header panel cho mỗi page: icon + tiêu đề + phụ đề. */
    public static JPanel pageHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_TITLE);
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle == null ? "" : subtitle);
        s.setFont(Theme.FONT_NORMAL);
        s.setForeground(Theme.TEXT_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        p.add(t);
        p.add(s);
        p.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 0));
        return p;
    }

    /* ===================== SEARCH FIELD (tròn) ===================== */

    public static JTextField searchField() {
        JTextField tf = new JTextField(20);
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm kiếm...");
        tf.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, iconSearch());
        tf.setPreferredSize(new Dimension(220, 34));
        tf.setMaximumSize(new Dimension(280, 34));
        return tf;
    }

    private static Icon iconSearch() {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 2, y + 2, 11, 11);
                g2.drawLine(x + 12, y + 12, x + 17, y + 17);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return 18; }
            @Override public int getIconHeight() { return 18; }
        };
    }

    /* ===================== BORDERS ===================== */

    public static Border padding(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    /** Bo góc đơn giản. */
    public static Border rounded(int radius) {
        return new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(8, 12, 8, 12); }
        };
    }
}
