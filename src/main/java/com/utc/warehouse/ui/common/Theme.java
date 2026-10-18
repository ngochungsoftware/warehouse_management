package com.utc.warehouse.ui.common;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Theme trung tâm: khởi tạo Look & Feel, đặt font mặc định,
 * cung cấp palette màu + hằng số layout.
 *
 * <p>Mọi panel/frame nên gọi {@link #install()} ngay đầu main.
 * Style màu/bo góc FlatLaf được set qua ClientProperty (xem {@link FlatClientProperties}).
 */
public final class Theme {

    /* ---------- Brand palette (dùng nội bộ thay cho UIConstants) ---------- */
    public static final Color BRAND         = new Color(0x1B7F4F);  // xanh lá
    public static final Color BRAND_DARK    = new Color(0x0E5A37);
    public static final Color BRAND_LIGHT   = new Color(0xE6F4EC);
    public static final Color ACCENT        = new Color(0x2563EB);  // xanh dương
    public static final Color DANGER        = new Color(0xDC2626);
    public static final Color WARNING       = new Color(0xF59E0B);
    public static final Color SUCCESS       = new Color(0x16A34A);
    public static final Color SIDEBAR_BG    = new Color(0x14272A);  // dark slate
    public static final Color SIDEBAR_HOVER = new Color(0x1F3940);
    public static final Color SIDEBAR_ACTIVE= new Color(0x1B7F4F);
    public static final Color BG            = new Color(0xF4F6F8);
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color BORDER        = new Color(0xE2E8F0);
    public static final Color TEXT          = new Color(0x1F2937);
    public static final Color TEXT_MUTED    = new Color(0x64748B);
    public static final Color ZEBRA         = new Color(0xF8FAFC);
    public static final Color SELECT_BG     = new Color(0xDBEAFE);
    public static final Color SELECT_BORDER = new Color(0x93C5FD);

    /* ---------- Fonts ---------- */
    public static Font  FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 22);
    public static Font  FONT_HEADER   = new Font("Segoe UI", Font.BOLD, 15);
    public static Font  FONT_NORMAL   = new Font("Segoe UI", Font.PLAIN, 13);
    public static Font  FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 12);
    public static Font  FONT_BRAND    = new Font("Segoe UI", Font.BOLD, 18);
    public static Font  FONT_BRAND_SM = new Font("Segoe UI", Font.PLAIN, 11);
    public static Font  FONT_BIG      = new Font("Segoe UI", Font.BOLD, 28);

    /* ---------- Spacing ---------- */
    public static final Insets PADDING_CARD = new Insets(16, 20, 16, 20);
    public static final Insets PADDING_BTN  = new Insets(8, 16, 8, 16);
    public static final int    GAP          = 10;
    public static final int    RADIUS       = 10;

    private Theme() { }

    /** Khởi tạo LAF + set các thuộc tính toàn cục. Gọi 1 lần trong main. */
    public static void install() {
        // Có thể đổi sang FlatMacLightLaf / FlatArcOrangeIJTheme tùy thích
        try {
            FlatMacLightLaf.setup();
        } catch (Exception ex) {
            try { FlatArcOrangeIJTheme.setup(); } catch (Exception ignored) { }
        }
        java.util.Map<String, String> extras = new java.util.HashMap<>();
        extras.put("defaultFont", FONT_NORMAL.getFamily() + " " + FONT_NORMAL.getSize());
        extras.put("@accentColor",         hex(BRAND));
        extras.put("@background",          hex(BG));
        extras.put("Component.focusColor", hex(BRAND));
        extras.put("Button.default.focusedBackground", hex(BRAND_LIGHT));
        extras.put("Button.arc",     "10");
        extras.put("Component.arc",  "10");
        extras.put("TextComponent.arc", "10");
        extras.put("ProgressBar.arc", "10");
        extras.put("TitlePane.background",          hex(CARD_BG));
        extras.put("TitlePane.foreground",          hex(TEXT));
        extras.put("TitlePane.inactiveBackground",  hex(CARD_BG));
        extras.put("TitlePane.inactiveForeground",  hex(TEXT_MUTED));
        FlatLaf.setGlobalExtraDefaults(extras);
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Bật placeholder cho JTextField/JPasswordField/JComboBox. */
    public static void applyPlaceholder(JTextField tf, String hint) {
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, hint);
    }
    public static void applyPlaceholder(JPasswordField pf, String hint) {
        pf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, hint);
        pf.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
    }
    public static void applyPlaceholder(JComboBox<?> cb, String hint) {
        cb.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, hint);
    }
}
