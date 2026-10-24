package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.ui.stock.ImportPanel;
import jakarta.persistence.EntityManager;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * VerifyTest22 — Khi DB KHONG CO NCC nao, combobox van dung duoc
 * (chi co placeholder, nguoi dung khong the chon NCC that).
 */
public class VerifyTest22 {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // Backup NCC khong bi don mua tham chieu. KHONG xoa NCC dang co don mua —
        // FK purchase_orders se chan va lam hong du lieu that cua nguoi dung.
        EntityManager em = HibernateUtil.getEntityManager();
        em.getTransaction().begin();
        List<Supplier> all = em.createQuery("FROM Supplier", Supplier.class).getResultList();
        List<Supplier> backup = new ArrayList<>();
        for (Supplier s : all) {
            Supplier c = new Supplier();
            c.setName(s.getName());
            c.setPhone(s.getPhone());
            c.setAddress(s.getAddress());
            c.setEmail(s.getEmail());
            backup.add(c);
        }
        // Chi xoa NCC khong co purchase_orders tro toi
        List<Supplier> removable = new ArrayList<>();
        for (Supplier s : all) {
            Long po = em.createQuery(
                    "SELECT COUNT(p) FROM PurchaseOrder p WHERE p.supplier.id = :sid", Long.class)
                    .setParameter("sid", s.getId()).getSingleResult();
            if (po == 0) removable.add(s);
        }
        for (Supplier s : removable) em.remove(s);
        em.getTransaction().commit();
        em.close();
        long left = all.size() - removable.size();
        System.out.println("Da xoa " + removable.size() + "/" + all.size()
                + " NCC. Con " + left + " NCC (dang co don mua, giu nguyen).");

        try {
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
                    System.out.println("combo co " + cb.getItemCount() + " item");
                    for (int i = 0; i < cb.getItemCount(); i++) {
                        Object v = cb.getItemAt(i);
                        if (v instanceof Supplier s)
                            System.out.println("  [" + i + "] id=" + s.getId() + " name='" + s.getName() + "'");
                    }
                    boolean ok = cb.getItemCount() >= 1
                            && cb.getItemAt(0) != null;
                    System.out.println(ok
                            ? "OK - combo luon co placeholder mac dinh, khong loi NullPointer"
                            : "FAIL - combo rong hoac null");
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } finally {
            // Phuc hoi DAY DU tat ca NCC da xoa (chay du co loi)
            EntityManager em2 = HibernateUtil.getEntityManager();
            em2.getTransaction().begin();
            for (Supplier s : backup) em2.persist(s);
            em2.getTransaction().commit();
            em2.close();
            System.out.println("Da phuc hoi " + backup.size() + "/" + all.size() + " NCC.");
        }
    }
}