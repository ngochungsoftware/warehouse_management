package com.utc.warehouse.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton giữ {@link EntityManagerFactory} cho cả app.
 * Mỗi thao tác DB lấy EntityManager mới qua {@link #getEntityManager()}.
 * Nhớ đóng EntityManager sau khi dùng xong (try-with-resources).
 */
public final class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);

    private static final String PERSISTENCE_UNIT = "warehousePU";
    private static final EntityManagerFactory EMF;

    static {
        try {
            log.info("Bootstrapping EntityManagerFactory [{}] ...", PERSISTENCE_UNIT);
            EMF = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
            log.info("EntityManagerFactory ready.");
        } catch (RuntimeException e) {
            log.error("Cannot init EntityManagerFactory", e);
            throw e;
        }
    }

    private HibernateUtil() { }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }

    public static void shutdown() {
        if (EMF != null && EMF.isOpen()) {
            log.info("Closing EntityManagerFactory ...");
            EMF.close();
        }
    }
}
