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

    public List<User> allStaff() { return userDAO.findAllStaff(); }

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

    public long countActiveAdmins() {
        return userDAO.findAll().stream()
                .filter(u -> u.isAdmin() && Boolean.TRUE.equals(u.getActive()))
                .count();
    }

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
