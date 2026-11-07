package com.utc.warehouse.config;

/** Helper shutdown để dùng trong các listener thoát. */
public final class HibernateShutdown {
    private HibernateShutdown() { }

    public static void shutdownAndExit(int code) {
        try { HibernateUtil.shutdown(); } catch (Exception ignored) { }
        System.exit(code);
    }
}
