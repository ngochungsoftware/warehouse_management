package com.utc.warehouse;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.ui.stock.ExportPanel;
import com.utc.warehouse.ui.stock.ImportPanel;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * VerifyTest29 — Dropdown NCC / KH phai cap nhat khi them/xoa o man danh muc.
 *
 * <p>Tai hien dung bug nguoi dung gap:
 *   1. App da mo, ImportPanel da load dropdown NCC luc khoi dong.
 *   2. User xoá NCC o man "Nha cung cap".
 *   3. User quay lai man "Phieu nhap" — dropdown van hien rong / danh sach cu.
 *
 * <p>Nguyen nhan goc: ImportPanel/ExportPanel KHONG override reload(), nen
 * MainFrame goi bp.reload() khi chuyen trang cung vo hieu.
 *
 * <p>Yeu cau: moi lan goi reload(), dropdown phai phan anh DB hien tai
 * (them NCC moi -> xuat hien; xoa NCC -> bien mat khoi danh sach).
 */
public class VerifyTest29 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int pass = 0, fail = 0;
        PartnerService ps = new PartnerService();

        // Panel tao 1 lan, giong app that (chi init 1 luc khoi dong)
        ImportPanel imp = new ImportPanel();
        imp.init();
        JComboBox<Supplier> cbImp = combo(imp, "cbSupplier");

        ExportPanel exp = new ExportPanel();
        exp.init();
        JComboBox<Customer> cbExp = combo(exp, "cbCustomer");

        // === Khoi diem ===
        int ncc0 = cbImp.getItemCount();
        int kh0 = cbExp.getItemCount();
        System.out.println("Ban dau: NCC=" + (ncc0 - 1) + " (placeholder), KH=" + (kh0 - 1));
        if (cbImp.getItemCount() >= 1 && cbExp.getItemCount() >= 1) pass++;
        else { fail++; System.out.println("FAIL: dropdown rong ngay tu dau"); }

        // placeholder phai luon o vi tri 0
        if (((Supplier) cbImp.getItemAt(0)).getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: NCC placeholder khong o index 0"); }
        if (((Customer) cbExp.getItemAt(0)).getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: KH placeholder khong o index 0"); }

        // === BƯỚC 1: them NCC + KH moi ===
        List<Supplier> created = new ArrayList<>();
        List<Customer> createdCus = new ArrayList<>();
        for (String n : new String[]{"VT29 NCC Alpha", "VT29 NCC Beta"}) {
            Supplier s = new Supplier();
            s.setName(n);
            s.setPhone("0900" + (int) (Math.random() * 900 + 100));
            s.setEmail("vt29" + Math.abs(n.hashCode()) + "@test.com");
            s.setAddress("Dia chi thu nghiem " + n);
            created.add(ps.saveSupplier(s));
        }
        for (String n : new String[]{"VT29 KH Alpha", "VT29 KH Beta"}) {
            Customer c = new Customer();
            c.setName(n);
            c.setPhone("0911" + (int) (Math.random() * 900 + 100));
            c.setEmail("vt29cus" + Math.abs(n.hashCode()) + "@test.com");
            c.setAddress("Dia chi KH " + n);
            createdCus.add(ps.saveCustomer(c));
        }
        System.out.println("Da tao " + created.size() + " NCC + " + createdCus.size() + " KH trong DB");

        // === BƯỚC 2: goi reload() (giong khi chuyen trang) ===
        imp.reload();
        exp.reload();

        int ncc1 = cbImp.getItemCount();
        int kh1 = cbExp.getItemCount();
        System.out.println("Sau reload: NCC=" + (ncc1 - 1) + ", KH=" + (kh1 - 1));

        // Truoc do dropdown khong co NCC moi -> bay phai co
        if (ncc1 > ncc0) pass++;
        else { fail++; System.out.println("FAIL: reload khong nap NCC moi (" + ncc0 + " -> " + ncc1 + ")"); }

        if (kh1 > kh0) pass++;
        else { fail++; System.out.println("FAIL: reload khong nap KH moi (" + kh0 + " -> " + kh1 + ")"); }

        // NCC moi phai xuat hien dung ten trong danh sach
        if (containsSupplier(cbImp, "VT29 NCC Alpha") && containsSupplier(cbImp, "VT29 NCC Beta")) pass++;
        else { fail++; System.out.println("FAIL: dropdown NCC thieu NCC vua tao"); }
        if (containsCustomer(cbExp, "VT29 KH Alpha") && containsCustomer(cbExp, "VT29 KH Beta")) pass++;
        else { fail++; System.out.println("FAIL: dropdown KH thieu KH vua tao"); }

        // Placeholder van con o dau sau khi reload
        if (((Supplier) cbImp.getItemAt(0)).getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: placeholder bi mat sau reload"); }
        if (((Customer) cbExp.getItemAt(0)).getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: KH placeholder bi mat sau reload"); }

        // === BƯỚC 3: chon NCC moi, reload lai -> phai giu nguyen lua chon ===
        int idxAlpha = indexOfSupplier(cbImp, "VT29 NCC Alpha");
        if (idxAlpha > 0) {
            cbImp.setSelectedIndex(idxAlpha);
            imp.reload();
            Supplier after = (Supplier) cbImp.getSelectedItem();
            if (after != null && "VT29 NCC Alpha".equals(after.getName())) pass++;
            else {
                fail++;
                System.out.println("FAIL: reload lam mat lua chon NCC (thanh: "
                        + (after == null ? "null" : after.getName()) + ")");
            }
        } else { fail++; System.out.println("FAIL: khong tim thay NCC Alpha de chon"); }

        // === BUOC 4: XOA NCC (dung thao tac nguoi dung) roi reload ===
        for (Supplier s : created) {
            ps.deleteSupplier(s.getId());
        }
        System.out.println("Da xoa " + created.size() + " NCC trong DB");

        imp.reload();
        exp.reload();

        int ncc2 = cbImp.getItemCount();
        int kh2 = cbExp.getItemCount();
        System.out.println("Sau xoa + reload: NCC=" + (ncc2 - 1) + ", KH=" + (kh2 - 1));

        // NCC da xoa phai bien mat khoi dropdown
        if (!containsSupplier(cbImp, "VT29 NCC Alpha") && !containsSupplier(cbImp, "VT29 NCC Beta")) pass++;
        else { fail++; System.out.println("FAIL: NCC da xoa van con trong dropdown"); }

        // Khi danh sach rong -> phai chi con placeholder, khong crash
        if (ncc2 >= 1) pass++;
        else { fail++; System.out.println("FAIL: dropdown NCC mat ca placeholder"); }
        if (kh2 >= 1) pass++;
        else { fail++; System.out.println("FAIL: dropdown KH mat ca placeholder"); }

        // === BƯỚC 5: reload lap lai nhieu lan -> khong duplicate, khong loi ===
        boolean stable = true;
        for (int i = 0; i < 5; i++) {
            try {
                imp.reload(); exp.reload();
            } catch (Exception ex) {
                stable = false;
                System.out.println("FAIL: reload lan " + (i + 1) + " loi: " + ex.getMessage());
                break;
            }
        }
        if (stable && cbImp.getItemCount() == cbImp.getItemCount()) {
            if (cbImp.getItemCount() >= 1) pass++; else { fail++; }
        } else { fail++; }

        // Danh sach phai khop chinh xac voi DB (khong thua, khong thieu)
        int dbNcc = ps.allSuppliers().size();
        if (cbImp.getItemCount() == dbNcc + 1) pass++;
        else {
            fail++;
            System.out.println("FAIL: dropdown NCC khong khop DB (cb=" + cbImp.getItemCount()
                    + ", db+placeholder=" + (dbNcc + 1) + ")");
        }
        int dbKh = ps.allCustomers().size();
        if (cbExp.getItemCount() == dbKh + 1) pass++;
        else {
            fail++;
            System.out.println("FAIL: dropdown KH khong khop DB (cb=" + cbExp.getItemCount()
                    + ", db+placeholder=" + (dbKh + 1) + ")");
        }

        // Don dep: xoa KH test
        for (Customer c : createdCus) {
            try { ps.deleteCustomer(c.getId()); } catch (Exception ignored) { }
        }

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    /* ===== helpers ===== */

    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> combo(Object panel, String field) throws Exception {
        Field f = panel.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return (JComboBox<T>) f.get(panel);
    }

    private static boolean containsSupplier(JComboBox<Supplier> cb, String name) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            Supplier s = cb.getItemAt(i);
            if (s != null && name.equals(s.getName())) return true;
        }
        return false;
    }

    private static boolean containsCustomer(JComboBox<Customer> cb, String name) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            Customer c = cb.getItemAt(i);
            if (c != null && name.equals(c.getName())) return true;
        }
        return false;
    }

    private static int indexOfSupplier(JComboBox<Supplier> cb, String name) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            Supplier s = cb.getItemAt(i);
            if (s != null && name.equals(s.getName())) return i;
        }
        return -1;
    }
}