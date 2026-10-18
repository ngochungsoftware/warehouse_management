package com.utc.warehouse.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {

    private static final NumberFormat VI = NumberFormat.getInstance(new Locale("vi", "VN"));

    static {
        VI.setGroupingUsed(true);
        VI.setMaximumFractionDigits(0);
    }

    private MoneyFormat() { }

    public static String format(BigDecimal value) {
        if (value == null) return "0 đ";
        return VI.format(value) + " đ";
    }

    public static String format(Long value) {
        if (value == null) return "0 đ";
        return VI.format(value) + " đ";
    }
}
