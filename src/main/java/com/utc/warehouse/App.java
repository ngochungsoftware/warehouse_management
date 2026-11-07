package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.ui.LoginFrame;
import com.utc.warehouse.ui.common.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Theme.install();

        try {
            HibernateUtil.getEntityManagerFactory();
        } catch (Exception ex) {
            log.error("Cannot start app: DB connection failed", ex);
            JOptionPane.showMessageDialog(null,
                    "Không kết nối được MySQL.\nKiểm tra lại thông tin trong persistence.xml và đã chạy schema.sql chưa.\n\n"
                            + ex.getMessage(),
                    "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            AuthService.get().bootstrapDefaultUsers();
        } catch (Exception ex) {
            log.error("Bootstrap users failed", ex);
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
