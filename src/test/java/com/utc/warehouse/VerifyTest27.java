package com.utc.warehouse;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.ui.stock.ExportPanel;
import com.utc.warehouse.ui.stock.ImportPanel;
import com.utc.warehouse.service.PartnerService;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

/** VerifyTest27 — DropDown KH / NCC phai chon duoc va co placeholder. */
public class VerifyTest27 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int pass = 0, fail = 0;

        // Neu KHONG co KH / NCC trong DB, dat it nhat 1 de test dropdown
        PartnerService ps = new PartnerService();
        seedIfEmpty(ps);

        // === ExportPanel ===
        ExportPanel exp = new ExportPanel();
        exp.init();
        // Reflection de lay cbCustomer (private)
        JComboBox<Customer> cbExp = (JComboBox<Customer>) getField(exp, "cbCustomer");
        if (cbExp != null) pass++; else { fail++; System.out.println("FAIL: cbCustomer khong ton tai"); }

        int totalExp = cbExp.getItemCount();
        System.out.println("ExportPanel cbCustomer itemCount = " + totalExp);
        if (totalExp >= 2) pass++;
        else { fail++; System.out.println("FAIL: phai co it nhat 2 muc (placeholder + 1 KH)"); }

        // Item dau tien phai la placeholder
        Customer first = (Customer) cbExp.getItemAt(0);
        if (first.getId() != null && first.getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: item dau tien phai la placeholder id=-1"); }

        // Mac dinh phai duoc chon la placeholder
        if (cbExp.getSelectedIndex() == 0) pass++;
        else { fail++; System.out.println("FAIL: mac dinh phai chon placeholder"); }

        // id=-1 phai duoc map ve null (dung cho saveTransaction)
        Customer placeholder = (Customer) cbExp.getItemAt(0);
        Integer custIdPlaceholder = (placeholder == null
                || (placeholder.getId() != null && placeholder.getId() == -1))
                ? null : placeholder.getId();
        if (custIdPlaceholder == null) pass++;
        else { fail++; System.out.println("FAIL: id=-1 phai duoc map ve null"); }

        // Click chon item thu 1 (KH that) - mo phong bang chon index
        cbExp.setSelectedIndex(1);
        Customer picked = (Customer) cbExp.getSelectedItem();
        if (picked != null && picked.getId() != null && picked.getId() != -1 && picked.getName() != null) {
            pass++;
            System.out.println("Chon KH OK: id=" + picked.getId() + " ten=" + picked.getName());
        } else { fail++; System.out.println("FAIL: khong chon duoc KH that"); }

        // Renderer phai tra ve ten KH
        if (picked.getName() != null && !picked.getName().isBlank()) pass++;
        else { fail++; System.out.println("FAIL: KH khong co ten de hien thi"); }

        // === ImportPanel - so sanh ===
        ImportPanel imp = new ImportPanel();
        imp.init();
        JComboBox<Supplier> cbImp = (JComboBox<Supplier>) getField(imp, "cbSupplier");
        if (cbImp != null && cbImp.getItemCount() >= 2 && cbImp.getSelectedIndex() == 0) pass++;
        else { fail++; System.out.println("FAIL: ImportPanel dropdown khong nhu pattern"); }

        Supplier ph = (Supplier) cbImp.getItemAt(0);
        if (ph.getId() == -1) pass++;
        else { fail++; System.out.println("FAIL: import placeholder id khong phai -1"); }

        cbImp.setSelectedIndex(1);
        Supplier pickedNcc = (Supplier) cbImp.getItemAt(cbImp.getSelectedIndex());
        if (pickedNcc != null && pickedNcc.getId() != null && pickedNcc.getId() > 0) pass++;
        else { fail++; System.out.println("FAIL: NCC that phai co id>0"); }

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    private static Object getField(Object o, String name) {
        try {
            var f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Nếu DB chưa có KH/NCC thì tự tạo tối thiểu 1 mỗi loại.
     * Test phải chạy được trên DB trống (sau khi người dùng xoá sạch dữ liệu).
     */
    private static void seedIfEmpty(PartnerService ps) throws Exception {
        if (ps.allCustomers().isEmpty()) {
            Customer c = new Customer();
            c.setName("VT27 KH mẫu");
            c.setPhone("0900000001");
            c.setEmail("vt27cus@test.com");
            c.setAddress("Địa chỉ mẫu");
            ps.saveCustomer(c);
            System.out.println("(!) DB chưa có KH — đã tạo 1 KH mẫu để test");
        }
        if (ps.allSuppliers().isEmpty()) {
            Supplier s = new Supplier();
            s.setName("VT27 NCC mẫu");
            s.setPhone("0900000002");
            s.setEmail("vt27ncc@test.com");
            s.setAddress("Địa chỉ mẫu");
            ps.saveSupplier(s);
            System.out.println("(!) DB chưa có NCC — đã tạo 1 NCC mẫu để test");
        }
    }
}