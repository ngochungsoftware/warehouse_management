package com.utc.warehouse.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateFormatUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateFormatUtil() { }

    public static String format(LocalDateTime dt) {
        return dt == null ? "" : dt.format(FMT);
    }

    public static String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE);
    }
}
