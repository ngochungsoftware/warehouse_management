package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.user.EmployeeManagementPanel;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Field;

/**
 * VerifyTest36 — Kiểm chứng phân quyền màn Quản lý nhân viên.
 *
 * <p>Màn cấp tài khoản là chức năng nhạy cảm: chỉ ADMIN được thấy menu.
 * Test này giả lập từng loại tài khoản rồi kiểm tra menu đăng ký đúng.
 *
 * <p>Test cũng kiểm tra nhân viên KHÔNG đăng nhập được khi tài khoản bị khoá —
 * đây là điểm bảo mật quan trọng: khoá nhưng vẫn đăng nhập được thì vô nghĩa.
 */
public class VerifyTest36 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== PHÂN QUYỀN MÀN QUẢN LÝ NHÂN VIÊN ===\n");

        UserService service = new UserService();
        AuthService auth = AuthService.get();

        Field cur = AuthService.class.getDeclaredField("currentUser");
        cur.setAccessible(true);
        Object saved = cur.get(auth);

        try {
            // ---------- 1. Menu chỉ hiện cho ADMIN ----------
            User admin = service.all().stream()
                    .filter(u -> u.getRole() == Role.ADMIN).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Không có admin"));

            cur.set(auth, admin);
            checkTrue("ADMIN: isAdmin() = true", auth.isAdmin());

            // Nhân viên: không có quyền quản trị
            User staff = service.allStaff().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActive())).findFirst().orElse(null);
            if (staff != null) {
                cur.set(auth, staff);
                checkTrue("STAFF: isAdmin() = false", !auth.isAdmin());
                cur.set(auth, admin);

                // ---------- 2. Panel quản lý nhân viên chỉ dành cho admin ----------
                // MainFrame.initMenu() chỉ addMenu khi admin == true.
                // Ở đây kiểm tra điều kiện quyết định hiển thị menu.
                checkTrue("Điều kiện hiển thị menu là isAdmin()", admin.isAdmin() && !staff.isAdmin());
                System.out.println("       -> STAFF không đạt điều kiện admin => không thấy menu");
            } else {
                System.out.println("  [SKIP] Chưa có nhân viên nào hoạt động để so sánh");
            }

            // ---------- 3. Khoá tài khoản => không đăng nhập được ----------
            // Đây là điểm mấu chốt: khoá phải thật sự chặn đăng nhập.
            // Dùng timestamp để chạy lại nhiều lần không đụng tên cũ.
            String uname = "zz_khoa_" + System.currentTimeMillis();
            Integer box = null;
            EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager();
            try {
                User emp = service.createEmployee(uname, "nv@2026", "Khoa Test");
                box = emp.getId();
                final Integer empId = box;

                checkTrue("Tài khoản mới đăng nhập được",
                        auth.login(uname, "nv@2026").isPresent());
                auth.logout();

                service.deactivate(empId);
                checkTrue("Tài khoản đã KHOÁ thì KHÔNG đăng nhập được",
                        auth.login(uname, "nv@2026").isEmpty());

                service.activate(empId);
                checkTrue("Mở khoá thì đăng nhập lại được",
                        auth.login(uname, "nv@2026").isPresent());
                auth.logout();

                checkTrue("Sai mật khẩu thì không đăng nhập được",
                        auth.login(uname, "sai_mat_khau").isEmpty());

            } finally {
                if (box != null) {
                    final Integer tid = box;
                    // Xoá audit log TRƯỚC rồi mới xoá user, vì FK audit_logs.user_id
                    // không có ON DELETE — đăng nhập xong luôn sinh audit log.
                    try {
                        var tx = em.getTransaction();
                        tx.begin();
                        em.createNativeQuery("DELETE FROM audit_logs WHERE user_id = :uid")
                                .setParameter("uid", tid).executeUpdate();
                        em.createNativeQuery("DELETE FROM users WHERE id = :uid")
                                .setParameter("uid", tid).executeUpdate();
                        tx.commit();
                    } catch (Exception ex) {
                        var tx = em.getTransaction();
                        if (tx.isActive()) tx.rollback();
                    }
                }
                em.close();
                auth.logout();
            }

        } finally {
            cur.set(auth, saved);
        }

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }
}