package com.utc.warehouse.service;

import com.utc.warehouse.dao.UserDAO;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = AuthService.get();

    public List<User> allActive() { return userDAO.findAllActive(); }

    public List<User> all() { return userDAO.findAll(); }

    /** Danh sách nhân viên (role = STAFF), dùng cho màn Quản lý nhân viên. */
    public List<User> allStaff() { return userDAO.findAllStaff(); }

    /**
     * Cấp tài khoản cho nhân viên mới.
     *
     * <p>Cố định {@link Role#STAFF}: màn này chỉ dành cho nhân viên, cấp quyền
     * ADMIN thì phải sang màn "Quản lý người dùng" để có bước kiểm tra riêng.
     */
    public User createEmployee(String username, String password, String fullName) {
        return create(username, password, fullName, Role.STAFF);
    }

    public Optional<User> findByUsername(String username) { return userDAO.findByUsername(username); }

    public User create(String username, String password, String fullName, Role role) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username không được trống");
        if (password == null || password.length() < 3) throw new IllegalArgumentException("Mật khẩu phải >= 3 ký tự");
        if (userDAO.findByUsername(username).isPresent())
            throw new IllegalArgumentException("Username đã tồn tại");
        return authService.createUser(username, password, fullName, role);
    }

    public User update(User u) {
        if (u.getUsername() != null) u.setUsername(u.getUsername().trim());
        if (u.getFullName() != null) u.setFullName(u.getFullName().trim());
        return userDAO.update(u);
    }

    /**
     * Xoá user.
     *
     * <p>Hai chốt an toàn:
     * <ul>
     *   <li>Không cho tự xoá chính mình (dễ hiểu nhầm là đang đăng nhập
     *       rồi tự khoá tài khoản của chính mình).</li>
     *   <li>Không cho xoá nếu đó là admin cuối cùng — tránh tự khoá
     *       quyền quản trị khỏi hệ thống.</li>
     * </ul>
     *
     * <p>User đã phát sinh giao dịch / hoá đơn sẽ bị MySQL chặn bởi foreign key
     * {@code fk_tx_user}; khi đó hệ thống báo lỗi và hướng dẫn dùng khoá tài
     * khoản (soft delete) thay vì xoá hẳn.
     */
    public void delete(Integer id) {
        User target = userDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        User me = authService.getCurrentUser();
        if (me != null && id.equals(me.getId())) {
            throw new IllegalArgumentException(
                    "Không thể xoá tài khoản đang đăng nhập. Hãy khoá (vô hiệu hoá) nếu cần.");
        }
        if (target.isAdmin() && countActiveAdmins() <= 1) {
            throw new IllegalArgumentException(
                    "Phải còn ít nhất 1 tài khoản ADMIN đang hoạt động.");
        }
        userDAO.deleteById(id);
    }

    /** Đếm số admin còn hoạt động (dùng để chặn xoá admin cuối cùng). */
    public long countActiveAdmins() {
        return userDAO.findAll().stream()
                .filter(u -> u.isAdmin() && Boolean.TRUE.equals(u.getActive()))
                .count();
    }

    /** Số bản ghi tham chiếu tới user — dùng để cảnh báo trước khi xoá. */
    public long countReferences(Integer userId) {
        return userDAO.countReferences(userId);
    }

    public void resetPassword(User u, String newPassword) {
        if (newPassword == null || newPassword.length() < 3)
            throw new IllegalArgumentException("Mật khẩu phải >= 3 ký tự");
        authService.changePassword(u, newPassword);
    }

    public void deactivate(Integer id) {
        User u = userDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        u.setActive(false);
        userDAO.update(u);
    }

    public void activate(Integer id) {
        User u = userDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        u.setActive(true);
        userDAO.update(u);
    }
}
