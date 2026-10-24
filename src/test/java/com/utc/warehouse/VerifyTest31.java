package com.utc.warehouse;

import com.utc.warehouse.dao.ReportDAO;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * VerifyTest31 — Kiểm chứng hàm chuyển đổi ngày trong ReportDAO.
 *
 * <p>Bug gốc: query {@code DATE(t.createdAt)} là hàm SQL nên Hibernate trả về
 * {@link java.sql.Date}, code cũ cast thẳng sang LocalDate → ClassCastException
 * khi mở màn Báo cáo tồn kho ("Lỗi tải chart: java.lang.ClassCastException").
 *
 * <p>Test này KHÔNG cần database — chỉ kiểm tra hàm chuyển đổi.
 */
public class VerifyTest31 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM CHỨNG CHUYỂN ĐỔI NGÀY (ReportDAO) ===\n");

        // toLocalDate là private static → gọi qua reflection
        Method m = ReportDAO.class.getDeclaredMethod("toLocalDate", Object.class);
        m.setAccessible(true);

        LocalDate expected = LocalDate.of(2026, 10, 21);

        // 1. java.sql.Date — TRƯỜNG HỢP GÂY LỖI
        Object sqlDate = java.sql.Date.valueOf(expected);
        check("java.sql.Date (trường hợp gây lỗi)", expected, m.invoke(null, sqlDate));

        // 2. LocalDate — trường hợp đã hoạt động
        check("LocalDate", expected, m.invoke(null, expected));

        // 3. LocalDateTime — cắt phần giờ
        check("LocalDateTime (cắt phần giờ)", expected,
                m.invoke(null, LocalDateTime.of(2026, 10, 21, 14, 30, 15)));

        // 4. java.util.Date
        check("java.util.Date", expected,
                m.invoke(null, new Date(expected.toEpochDay() * 86_400_000L)));

        // 5. String (driver trả về text)
        check("String \"2026-10-21\"", expected, m.invoke(null, "2026-10-21"));

        // 6. null → null, không ném lỗi
        check("null → null", null, m.invoke(null, new Object[]{null}));

        // 7. Không mất dữ liệu khi qua java.sql.Date (kiểm tra round-trip)
        LocalDate roundTrip = (LocalDate) m.invoke(null, java.sql.Date.valueOf(expected));
        checkTrue("Round-trip giữ nguyên ngày", expected.equals(roundTrip));

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    private static void check(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name
                + "  (kỳ vọng=" + expected + ", thực tế=" + actual + ")");
        if (ok) pass++; else fail++;
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }
}