package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.UserService;

/**
 * VerifyTest37 — Điều tra countReferences().
 *
 * <p>Nghi vấn: JPQL dùng TÊN ENTITY (AuditLog, StockTransaction...), không dùng
 * tên bảng (audit_logs, stock_transactions). Nếu truy vấn sai cú pháp, exception
 * bị nuốt và countReferences luôn trả 0 -> UI không bao giờ cảnh báo có dữ liệu
 * liên quan, và sẽ luôn cố xoá hẳn rồi gặp lỗi SQL thô.
 */
public class VerifyTest37 {

    public static void main(String[] args) {
        UserService service = new UserService();

        System.out.println("=== ĐIỀU TRA countReferences() ===\n");

        // Tìm 1 user chắc chắn có audit log: admin vừa đăng nhập ở test trước.
        User admin = service.all().stream()
                .filter(u -> "admin".equals(u.getUsername()))
                .findFirst().orElseThrow();

        // Đăng nhập để chắc chắn có audit log gắn với admin
        com.utc.warehouse.service.AuthService.get().login("admin", "admin123");
        com.utc.warehouse.service.AuthService.get().logout();

        long refs = service.countReferences(admin.getId());
        System.out.println("  countReferences(admin) = " + refs);
        System.out.println("  -> Kỳ vọng > 0 vì admin vừa đăng nhập và đăng xuất (ghi audit_logs)\n");

        if (refs == 0) {
            System.out.println("  [BUG XAC NHAN] countReferences tra 0 trong khi co audit log.");
            System.out.println("  -> Nguyen do: JPQL dung TEN BANG thay vi TEN ENTITY.");
            System.exit(1);
        } else {
            System.out.println("  [OK] Dem dung.");
        }
    }
}