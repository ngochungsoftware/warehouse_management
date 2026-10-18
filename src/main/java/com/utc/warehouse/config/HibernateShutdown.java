package com.utc.warehouse.config;

import com.utc.warehouse.ui.LoginFrame;

import javax.swing.*;

/** Helper shutdown để dùng trong các listener thoát. */
public final class HibernateShutdown {
    private HibernateShutdown() { }

    public static void shutdownAndExit(int code) {
        try { HibernateUtil.shutdown(); } catch (Exception ignored) { }
        System.exit(code);
    }
}
