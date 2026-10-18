package com.utc.warehouse.ui.common;

import java.awt.*;

/**
 * Backward-compat: trỏ tất cả hằng số cũ sang {@link Theme}.
 * Các panel cũ dùng UIConstants sẽ tự động lấy giá trị mới.
 * Nên dùng {@code Theme} trong code mới.
 */
@Deprecated
public final class UIConstants {

    // ----- Colors -----
    public static final Color PRIMARY        = Theme.BRAND;
    public static final Color PRIMARY_DARK   = Theme.BRAND_DARK;
    public static final Color ACCENT         = Theme.ACCENT;
    public static final Color DANGER         = Theme.DANGER;
    public static final Color WARNING        = Theme.WARNING;
    public static final Color BG             = Theme.BG;
    public static final Color CARD_BG        = Theme.CARD_BG;
    public static final Color BORDER         = Theme.BORDER;
    public static final Color TEXT           = Theme.TEXT;
    public static final Color TEXT_MUTED     = Theme.TEXT_MUTED;

    // ----- Fonts -----
    public static final Font  FONT_TITLE     = Theme.FONT_TITLE;
    public static final Font  FONT_HEADER    = Theme.FONT_HEADER;
    public static final Font  FONT_NORMAL    = Theme.FONT_NORMAL;
    public static final Font  FONT_SMALL     = Theme.FONT_SMALL;

    // ----- Spacing -----
    public static final Insets PADDING       = Theme.PADDING_CARD;
    public static final int    GAP           = Theme.GAP;

    private UIConstants() { }
}
