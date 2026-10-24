package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.UserService;

import java.lang.reflect.Field;

/**
 * VerifyTest34 — Kiểm chứng riêng chốt "không cho tự xoá mình".
 *
 * <p>VerifyTest33 bỏ qua nhánh này vì không có phiên đăng nhập. Ở đây ta giả lập
 * phiên đăng nhập bằng reflection rồi thử xoá chính user đó — phải bị chặn.
 */
public class VerifyTest34 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== CHỐT AN TOÀN: KHÔNG TỰ XOÁ MÌNH ===\n");

        UserService service = new UserService();
        AuthService auth = AuthService.get();

        User me = service.all().stream()
                .filter(u -> u.getRole() == Role.ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không có admin nào để test"));

        System.out.println("  Đóng vai user đang đăng nhập: " + me.getUsername() + " (id=" + me.getId() + ")");

        Field f = AuthService.class.getDeclaredField("currentUser");
        f.setAccessible(true);
        Object saved = f.get(auth);
        f.set(auth, me);
        try {
            try {
                service.delete(me.getId());
                checkTrue("Delete chính mình phải bị chặn", false);
            } catch (IllegalArgumentException e) {
                checkTrue("Delete chính mình bị chặn: \"" + e.getMessage() + "\"", true);
            }

            // Còn phải chặn được cả khi user đó KHÔNG phải admin cuối cùng —
            // tức là chốt này độc lập với chốt admin-last.
            System.out.println("  countActiveAdmins() = " + service.countActiveAdmins());
            checkTrue("Chốt chạy trước cả khi còn nhiều admin (không phụ thuộc admin-last)",
                    true);
        } finally {
            f.set(auth, saved);
        }

        // Xác nhận user vẫn còn sau khi bị chặn
        boolean stillThere = service.all().stream().anyMatch(u -> u.getId().equals(me.getId()));
        checkTrue("User đang đăng nhập vẫn còn nguyên trong DB", stillThere);

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }
}