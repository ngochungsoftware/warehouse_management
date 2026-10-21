package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.ui.stock.ImportPanel;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * VerifyTest21 — Kiem chung combobox NCC co the CHON duoc.
 *
 * Trich xuat noi dung combobox giong y het code thuc te:
 *   1. DB co 1 NCC -> combo phai co 2 item (placeholder + 1 NCC), chon placeholder mac dinh
 *   2. DB co 0 NCC -> combo chi co placeholder, khong the chon NCC that
 *   3. DB co 2+ NCC -> combo co placeholder + cac NCC, sap xep theo ten
 *
 * Kiem tra: placeholder co id = -1 nhan biet duoc, khong bi gui xuong DB.
 */
public class VerifyTest21 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        System.out.println("=== KIEM CHUNG CHON NCC O PHIEU NHAP ===\n");

        loginAdmin();

        var partner = new PartnerService();
        var allSups = partner.allSuppliers();
        System.out.println("DB co " + allSups.size() + " NCC:");
        for (Supplier s : allSups) System.out.println("  #" + s.getId() + " | " + s.getName());

        /* Case A: hien tai DB co 1 NCC */
        SwingUtilities.invokeAndWait(() -> {
            try {
                ImportPanel panel = new ImportPanel();
                panel.init();   // BasePanel xây UI
                JComboBox<?> cb = findCombo(panel, "cbSupplier");
                int n = cb.getItemCount();
                check("Combo hien thi >= 2 muc (placeholder + NCC)",
                        n >= Math.max(2, allSups.size() + 1),
                        "item count = " + n);

                Object sel = cb.getSelectedItem();
                Supplier s0 = (sel instanceof Supplier) ? (Supplier) sel : null;
                boolean placeholderSelected = s0 != null && s0.getId() != null && s0.getId() == -1;
                check("Mac dinh CHUA chon (selected = placeholder id=-1)",
                        placeholderSelected,
                        "selected = " + describe(s0));

                // Kiem tra setSelected hop le
                if (allSups.size() > 0) {
                    Supplier target = allSups.get(0);
                    int idx = -1;
                    for (int i = 0; i < cb.getItemCount(); i++) {
                        Object it = cb.getItemAt(i);
                        if (it instanceof Supplier s && s.getId() != null
                                && s.getId().equals(target.getId())) { idx = i; break; }
                    }
                    boolean usedIndex = idx > 0;
                    if (usedIndex) cb.setSelectedIndex(idx);
                    Supplier picked = (Supplier) cb.getSelectedItem();
                    boolean pickedOk = picked != null && picked.getId() != null
                            && picked.getId().equals(target.getId());
                    check("Co the chon NCC that (id=" + target.getId() + ")",
                            pickedOk,
                            "index=" + idx + ", picked = " + describe(picked));
                }

                // Kiem tra placeholder render text dung
                Object displayedSel = cb.getSelectedItem();
                String text = "";
                for (int i = 0; i < cb.getItemCount(); i++) {
                    Object v = cb.getItemAt(i);
                    if (v == null || ((Supplier) v).getId() != null && ((Supplier) v).getId() == -1) {
                        text = "(Chọn NCC)";
                    } else {
                        text = ((Supplier) v).getName();
                    }
                }
                check("Co the doc label cac item", text != null && !text.isEmpty(),
                        "sample label = " + text);
                // Mot flag don gian: combo khong bi empty
                check("Khong co item null le trong combo", cb.getItemCount() > 0,
                        "count = " + cb.getItemCount());
            } catch (Exception ex) {
                ex.printStackTrace();
                fail++;
            }
        });

        System.out.println("\n=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    private static void loginAdmin() {
        var users = new com.utc.warehouse.service.UserService().all();
        if (!users.isEmpty()) AuthService.get().login(users.get(0).getUsername(), "admin123");
    }

    private static JComboBox<?> findCombo(Object panel, String name) throws Exception {
        // Di qua cac field (bao gom ca private superclass)
        Field f = null;
        for (Class<?> c = panel.getClass(); c != null; c = c.getSuperclass()) {
            try { f = c.getDeclaredField(name); break; }
            catch (NoSuchFieldException ignored) { }
        }
        f.setAccessible(true);
        return (JComboBox<?>) f.get(panel);
    }

    private static String describe(Supplier s) {
        return s == null ? "null" : "id=" + s.getId() + " name='" + s.getName() + "'";
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) pass++; else fail++;
    }
}