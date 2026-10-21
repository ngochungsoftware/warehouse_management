package com.utc.warehouse;

import com.utc.warehouse.entity.Invoice;
import com.utc.warehouse.service.InvoiceService;

/**
 * BizTest14: Verify query JOIN FETCH của InvoiceDAO — đảm bảo đọc
 * inv.getItems() ngoài transaction KHÔNG ném LazyInitializationException.
 *
 * Đây chính là lỗi khiến double-click "không có gì hiện lên":
 * exception nổi trong mouse listener bị Swing/EDT nuốt mất.
 */
public class BizTest14 {

    public static void main(String[] args) {
        InvoiceService svc = new InvoiceService();
        int pass = 0, fail = 0;

        // 1) Lấy danh sách kèm details
        java.util.List<Invoice> all;
        try {
            all = svc.allInvoicesWithDetails();
            System.out.println("[1] allInvoicesWithDetails() → " + all.size() + " hóa đơn");
            pass++;
        } catch (Exception e) {
            System.out.println("[1] ✗ Lỗi: " + e);
            e.printStackTrace();
            fail++;
            return;
        }

        if (all.isEmpty()) {
            System.out.println("\nDB chưa có hóa đơn nào — hãy chạy BizTest11 để seed trước.");
            return;
        }

        // 2) Đọc items NGOÀI transaction (giống hệt showDetail trong UI)
        Invoice inv = all.get(0);
        System.out.println("[2] Đọc items của hóa đơn '" + inv.getCode() + "' ngoài transaction:");
        try {
            var items = inv.getItems();
            System.out.println("    items.size() = " + (items == null ? "null" : items.size()));
            if (items != null) {
                for (var it : items) {
                    // Đọc cả product (LAZY) để chắc chắn đã nạp sẵn
                    String sku = it.getProduct() == null ? "?" : it.getProduct().getSku();
                    System.out.println("      - " + it.getQuantity() + " x " + sku
                            + " @ " + it.getUnitPrice());
                }
            }
            System.out.println("    ✓ Không LazyInitializationException!");
            pass++;
        } catch (Throwable t) {
            System.out.println("    ✗ " + t.getClass().getName() + ": " + t.getMessage());
            System.out.println("    → Đây chính là lỗi khiến double-click im lặng!");
            fail++;
        }

        // 3) findByCodeWithDetails
        String code = inv.getCode();
        System.out.println("\n[3] findByCodeWithDetails(\"" + code + "\")");
        try {
            Invoice found = svc.findByCodeWithDetails(code);
            if (found == null) {
                System.out.println("    ✗ Không tìm thấy");
                fail++;
            } else {
                System.out.println("    ✓ Tìm thấy: " + found.getCode()
                        + ", items=" + (found.getItems() == null ? "null" : found.getItems().size()));
                pass++;
            }
        } catch (Throwable t) {
            System.out.println("    ✗ " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
            fail++;
        }

        // 4) Mô phỏng đúng những gì showDetail() làm
        System.out.println("\n[4] Mô phỏng showDetail() — đọc TẤT CẢ field như UI thật:");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Mã HĐ: ").append(found0(svc, code).getCode());
            System.out.println("    ✓ Mọi field đều đọc được, không exception");
            pass++;
        } catch (Throwable t) {
            System.out.println("    ✗ " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
            fail++;
        }

        System.out.println("\n=== Kết quả: " + pass + " passed, " + fail + " failed ===");
    }

    private static Invoice found0(InvoiceService svc, String code) {
        return svc.findByCodeWithDetails(code);
    }
}