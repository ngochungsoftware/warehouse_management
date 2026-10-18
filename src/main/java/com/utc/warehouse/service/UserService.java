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

    public Optional<User> findByUsername(String username) { return userDAO.findByUsername(username); }

    public User create(String username, String password, String fullName, Role role) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username không được trống");
        if (password == null || password.length() < 3) throw new IllegalArgumentException("Mật khẩu phải >= 3 ký tự");
        if (userDAO.findByUsername(username).isPresent())
            throw new IllegalArgumentException("Username đã tồn tại");
        return authService.createUser(username, password, fullName, role);
    }

    public User update(User u) { return userDAO.update(u); }

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
