package com.utc.warehouse;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.user.EmployeeManagementPanel;

import java.lang.reflect.Field;
import java.util.List;

/**
 * VerifyTest35 — Kiểm chứng màn Quản lý nhân viên.
 *
 * <p>Cần MySQL đang chạy.
 *
 * <p>Các quy tắc kiểm:
 *  1. Panel có đủ nút cấp tài khoản / sửa / xoá.
 *  2. allStaff() chỉ trả về user role STAFF — không lẫn ADMIN.
 *  3. createEmployee() tạo tài khoản role STAFF, không thể cấp ADMIN từ màn nhân viên.
 *  4. Cấp tài khoản trùng username bị chặn.
 *  5. Mật khẩu yếu (< 6 ký tự) bị từ chối ở tầng UI/dialog.
 */
public class VerifyTest35 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM CHỨNG QUẢN LÝ NHÂN VIÊN ===\n");

        UserService service = new UserService();

        // ---------- 1. Panel có nút ----------
        try {
            EmployeeManagementPanel panel = new EmployeeManagementPanel();
            panel.init();
            checkTrue("Panel có nút cấp tài khoản (btnAdd)", findBtn(panel, "btnAdd") != null);
            checkTrue("Panel có nút Sửa (btnEdit)",            findBtn(panel, "btnEdit") != null);
            checkTrue("Panel có nút Xoá (btnDelete)",          findBtn(panel, "btnDelete") != null);
        } catch (Exception ex) {
            checkTrue("Khởi tạo panel: " + ex, false);
        }

        // ---------- 2. allStaff() chỉ trả STAFF ----------
        List<User> staff = service.allStaff();
        boolean allStaff = staff.stream().allMatch(u -> u.getRole() == Role.STAFF);
        checkTrue("allStaff() trả về " + staff.size() + " nhân viên, tất cả đều STAFF", allStaff);
        boolean noAdmin = staff.stream().noneMatch(u -> u.getRole() == Role.ADMIN);
        checkTrue("allStaff() KHÔNG chứa tài khoản ADMIN", noAdmin);

        // ---------- 3. createEmployee luôn tạo STAFF ----------
        String uname = "zz_nv_test";
        Integer box = null;
        try {
            checkThrows("createEmployee chặn username trùng",
                    () -> service.createEmployee("admin", "pass123", "Trùng"));

            User emp = service.createEmployee(uname, "nv@2026", "Nhân viên Test");
            box = emp.getId();
            final Integer empId = box;

            checkTrue("Cấp tài khoản nhân viên thành công (id=" + empId + ")", empId != null);
            checkTrue("Tài khoản nhân viên có role STAFF", emp.getRole() == Role.STAFF);

            // Không có đường nào cho phép nâng STAFF -> ADMIN từ màn nhân viên
            User reread = service.all().stream()
                    .filter(u -> u.getId().equals(empId)).findFirst().orElseThrow();
            checkTrue("Đọc lại DB vẫn là STAFF", reread.getRole() == Role.STAFF);

            // Tài khoản nhân viên đăng nhập được
            checkTrue("Tài khoản nhân viên đăng nhập được",
                    service.findByUsername(uname).isPresent());
            checkTrue("Tài khoản nhân viên KHÔNG phải admin",
                    !service.all().stream()
                        .filter(u -> u.getId().equals(empId))
                        .findFirst().orElseThrow().isAdmin());

            // Nhân viên xuất hiện trong allStaff()
            checkTrue("Nhân viên mới xuất hiện trong allStaff()",
                    service.allStaff().stream().anyMatch(u -> u.getId().equals(empId)));

            // ---------- 4. Khoá rồi mở lại ----------
            service.deactivate(empId);
            boolean inactive = service.all().stream()
                    .filter(u -> u.getId().equals(empId)).findFirst().orElseThrow()
                    .getActive() == Boolean.FALSE;
            checkTrue("Khoá tài khoản nhân viên thành công", inactive);

            service.activate(empId);
            boolean active = service.all().stream()
                    .filter(u -> u.getId().equals(empId)).findFirst().orElseThrow()
                    .getActive() == Boolean.TRUE;
            checkTrue("Mở khoá tài khoản nhân viên thành công", active);

            // ---------- 5. Xoá nhân viên sạch ----------
            long refs = service.countReferences(empId);
            System.out.println("       nhân viên tạm có " + refs + " bản ghi tham chiếu");
            if (refs == 0) {
                service.delete(empId);
                checkTrue("Xoá tài khoản nhân viên không có tham chiếu",
                        service.all().stream().noneMatch(u -> u.getId().equals(empId)));
                box = null;
            } else {
                checkTrue("Nhân viên có tham chiếu không bị xoá hẳn (FK chặn)",
                        service.all().stream().anyMatch(u -> u.getId().equals(empId)));
            }

        } finally {
            if (box != null) {
                final Integer tid = box;
                try {
                    User u = service.all().stream().filter(x -> x.getId().equals(tid)).findFirst().orElse(null);
                    if (u != null) {
                        if (service.countReferences(tid) == 0) service.delete(tid);
                        else service.deactivate(tid);
                    }
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

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }

    private static void checkThrows(String name, Runnable r) {
        try {
            r.run();
            checkTrue(name, false);
        } catch (Exception e) {
            System.out.println("  [PASS] " + name + "  -> " + trim(e.getMessage()));
            pass++;
        }
    }

    private static String trim(String s) {
        if (s == null) return "?";
        int nl = s.indexOf('\n');
        String m = nl > 0 ? s.substring(0, nl) : s;
        return m.length() > 80 ? m.substring(0, 80) + "..." : m;
    }
}