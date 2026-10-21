package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.ui.stock.ImportPanel;
import jakarta.persistence.EntityManager;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.List;

/**
 * VerifyTest22 — Khi DB KHONG CO NCC nao, combobox van dung duoc
 * (chi co placeholder, nguoi dung khong the chon NCC that).
 */
public class VerifyTest22 {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // Backup + xoa het supplier
        EntityManager em = HibernateUtil.getEntityManager();
        em.getTransaction().begin();
        List<Supplier> all = em.createQuery("FROM Supplier", Supplier.class).getResultList();
        for (Supplier s : all) em.remove(s);
        em.getTransaction().commit();
        em.close();
        System.out.println("Da xoa het NCC. Con 0 NCC.");

        // Login + test
        var users = new com.utc.warehouse.service.UserService().all();
        if (!users.isEmpty()) AuthService.get().login(users.get(0).getUsername(), "admin123");

        SwingUtilities.invokeAndWait(() -> {
            try {
                ImportPanel panel = new ImportPanel();
                panel.init();
                Field f = null;
                for (Class<?> c = panel.getClass(); c != null; c = c.getSuperclass())
                    try { f = c.getDeclaredField("cbSupplier"); break; } catch (NoSuchFieldException e) { }
                f.setAccessible(true);
                JComboBox<?> cb = (JComboBox<?>) f.get(panel);
                System.out.println("DB rong -> combo co " + cb.getItemCount() + " item");
                for (int i = 0; i < cb.getItemCount(); i++) {
                    Object v = cb.getItemAt(i);
                    if (v instanceof Supplier s)
                        System.out.println("  [" + i + "] id=" + s.getId() + " name='" + s.getName() + "'");
                }
                int ok = 0, fail = 0;
                if (cb.getItemCount() == 1
                        && cb.getSelectedItem() instanceof Supplier s
                        && s.getId() != null && s.getId() == -1) ok++; else { fail++; }
                System.out.println(fail == 0 ? "OK - phong truong hop DB rong" : "FAIL");
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // Phuc hoi
        EntityManager em2 = HibernateUtil.getEntityManager();
        em2.getTransaction().begin();
        Supplier orig = new Supplier();
        orig.setName("Nước ngọt Hà Thành");
        em2.persist(orig);
        em2.getTransaction().commit();
        em2.close();
        System.out.println("Da phuc hoi NCC goc.");
    }
}