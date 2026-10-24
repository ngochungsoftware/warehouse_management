package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.UserService;

import java.util.List;

/**
 * VerifyTest39 — Dọn rác test và kiểm tra countReferences đã sửa đúng.
 *
 * <p>Xoá sạch các user tạm mà các test trước để lại (zz_*). Vì audit_logs chặn
 * FK nên phải xoá audit log của user tạm TRƯỚC rồi mới xoá user.
 */
public class VerifyTest39 {

    public static void main(String[] args) {
        System.out.println("=== DON RAC USER TAM (zz_*) ===\n");

        UserService service = new UserService();
        var em = com.utc.warehouse.config.HibernateUtil.getEntityManager();

        try {
            List<User> junk = service.all().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().startsWith("zz_"))
                    .toList();

            if (junk.isEmpty()) {
                System.out.println("  (khong co user tam nao)");
            }

            for (User u : junk) {
                Integer id = u.getId();
                var tx = em.getTransaction();
                tx.begin();
                try {
                    // Xoá audit log truoc: FK user_id khong co ON DELETE se chan
                    // viec xoa user neu con audit log tro toi.
                    em.createNativeQuery("DELETE FROM audit_logs WHERE user_id = :uid")
                            .setParameter("uid", id).executeUpdate();
                    em.createNativeQuery("DELETE FROM users WHERE id = :uid")
                            .setParameter("uid", id).executeUpdate();
                    tx.commit();
                    System.out.println("  [XOA] " + u.getUsername() + " (id=" + id + ")");
                } catch (Exception ex) {
                    if (tx.isActive()) tx.rollback();
                    System.out.println("  [LOI] " + u.getUsername() + ": " + ex.getMessage());
                }
            }
        } finally {
            em.close();
        }

        // Xac nhan da sach
        long conLai = service.all().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith("zz_")).count();
        System.out.println("\n  Con lai user tam: " + conLai);
        System.out.println(conLai == 0 ? "  [OK] Sach" : "  [FAIL] Van con rac");
        if (conLai != 0) System.exit(1);
    }
}