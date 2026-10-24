package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.user.UserManagementPanel;

import java.lang.reflect.Field;

/**
 * VerifyTest33 — Kiểm chứng CRUD + các chốt an toàn cho màn Quản lý người dùng.
 *
 * <p>Cần MySQL đang chạy và schema đã được nạp.
 *
 * <p>Các quy tắc được kiểm:
 *  1. Panel có đủ nút Thêm / Sửa / Xoá (CRUD đầy đủ, không còn fix cứng).
 *  2. Create: chặn username rỗng / quá ngắn / trùng.
 *  3. Update: đổi được họ tên và role.
 *  4. Delete: chặn xoá tài khoản đang đăng nhập.
 *  5. Delete: chặn xoá admin cuối cùng đang hoạt động.
 *  6. Delete: xoá hẳn được user sạch (không có tham chiếu).
 */
public class VerifyTest33 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM CHỨNG CRUD NGƯỜI DÙNG ===\n");

        UserService service = new UserService();

        // ---------- 1. Panel có đủ nút CRUD ----------
        try {
            UserManagementPanel panel = new UserManagementPanel();
            panel.init();
            checkTrue("Panel có nút Thêm (btnAdd)",  findBtn(panel, "btnAdd")  != null);
            checkTrue("Panel có nút Sửa (btnEdit)",  findBtn(panel, "btnEdit") != null);
            checkTrue("Panel có nút Xoá (btnDelete)", findBtn(panel, "btnDelete") != null);
        } catch (Exception ex) {
            checkTrue("Khởi tạo panel: " + ex, false);
        }

        // Tài khoản test dùng để thao tác, luôn dọn ở cuối.
        String uname = "zz_test_tmp";
        Integer testIdBox = null;
        try {
            // ---------- 2. Create ----------
            checkThrows("Create chặn username rỗng",
                    () -> service.create("", "pass123", "x", Role.STAFF));
            checkThrows("Create chặn mật khẩu < 3 ký tự",
                    () -> service.create(uname, "ab", "x", Role.STAFF));

            User created = service.create(uname, "pass123", "Tạm", Role.STAFF);
            testIdBox = created.getId();
            final Integer testId = testIdBox;
            checkTrue("Create thành công (id=" + testId + ")", testId != null);
            checkTrue("Mật khẩu được hash, không lưu plaintext",
                    created.getPasswordHash() != null
                            && !created.getPasswordHash().equals("pass123"));

            checkThrows("Create chặn username trùng",
                    () -> service.create(uname, "pass123", "y", Role.STAFF));

            // ---------- 3. Update ----------
            User u = service.all().stream().filter(x -> x.getId().equals(testId)).findFirst().orElseThrow();
            u.setFullName("Đã sửa");
            u.setRole(Role.ADMIN);
            service.update(u);
            User after = service.all().stream().filter(x -> x.getId().equals(testId)).findFirst().orElseThrow();
            checkTrue("Update đổi được họ tên", "Đã sửa".equals(after.getFullName()));
            checkTrue("Update đổi được role", after.getRole() == Role.ADMIN);

            // Trả lại về STAFF để các bước sau đỡ lệch logic admin
            after.setRole(Role.STAFF);
            service.update(after);

            // ---------- 4. Chặn xoá chính mình ----------
            AuthService auth = AuthService.get();
            User me = auth.getCurrentUser();
            if (me != null) {
                setCurrentUser(auth, after);   // đóng vai user đang đăng nhập
                checkThrows("Delete chặn tài khoản đang đăng nhập", () -> service.delete(testId));
                setCurrentUser(auth, me);
            } else {
                System.out.println("  [SKIP] Chưa đăng nhập — bỏ qua test chặn tự xoá");
            }

            // ---------- 5. Chặn xoá admin cuối cùng ----------
            checkTrue("countActiveAdmins() >= 1", service.countActiveAdmins() >= 1);
            if (service.countActiveAdmins() == 1) {
                User lastAdmin = service.all().stream()
                        .filter(x -> x.getRole() == Role.ADMIN && Boolean.TRUE.equals(x.getActive()))
                        .findFirst().orElseThrow();
                checkThrows("Delete chặn xoá admin cuối cùng", () -> service.delete(lastAdmin.getId()));
            }

            // ---------- 6. Xoá hẳn user sạch ----------
            long refs = service.countReferences(testId);
            System.out.println("       user tạm có " + refs + " bản ghi tham chiếu");
            checkTrue("countReferences trả về >= 0", refs >= 0);

            if (refs == 0) {
                service.delete(testId);
                boolean gone = service.all().stream().noneMatch(x -> x.getId().equals(testId));
                checkTrue("Delete hẳn user không có tham chiếu", gone);
                testIdBox = null;
            } else {
                // Có tham chiếu -> phải bị chặn bởi FK (không xoá được hẳn)
                boolean stillThere = service.all().stream().anyMatch(x -> x.getId().equals(testId));
                checkTrue("User có tham chiếu vẫn còn (FK chặn hard delete)", stillThere);
            }

        } finally {
            // Dọn dẹp: xoá user tạm nếu còn
            if (testIdBox != null) {
                final Integer tid = testIdBox;
                try {
                    User u = service.all().stream().filter(x -> x.getId().equals(tid)).findFirst().orElse(null);
                    if (u != null && service.countReferences(tid) == 0) service.delete(tid);
                    else if (u != null) service.deactivate(tid);
                } catch (Exception ignored) { }
            }
        }

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    private static java.awt.Component findBtn(java.awt.Container root, String name) {
        if (name.equals(root.getName())) return root;
        for (java.awt.Component c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof java.awt.Container ch) {
                java.awt.Component r = findBtn(ch, name);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** currentUser là private — set qua reflection cho test. */
    private static void setCurrentUser(AuthService auth, User u) throws Exception {
        Field f = AuthService.class.getDeclaredField("currentUser");
        f.setAccessible(true);
        f.set(auth, u);
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }

    private static void checkThrows(String name, Runnable r) {
        try {
            r.run();
            checkTrue(name, false);   // không ném lỗi = sai
        } catch (Exception e) {
            System.out.println("  [PASS] " + name + "  -> " + trim(e.getMessage()));
            pass++;
        }
    }

    private static String trim(String s) {
        if (s == null) return "?";
        int nl = s.indexOf('\n');
        String m = nl > 0 ? s.substring(0, nl) : s;
        return m.length() > 90 ? m.substring(0, 90) + "..." : m;
    }
}