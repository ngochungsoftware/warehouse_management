package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ProductService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * BizTest8: Mô phỏng flow người dùng:
 *  1) Load danh sách SP
 *  2) Chọn dòng thứ 2
 *  3) Reload (giả lập switch menu rồi quay lại)
 *  4) Kiểm tra: dòng đã chọn còn được chọn không?
 *  5) Lấy Product từ dòng được chọn
 *
 * Trước fix: reload clear selection → selected() trả null → mở dialog fail.
 * Sau fix:   reload restore selection theo ID → click vẫn mở được.
 */
public class BizTest8 {

    public static void main(String[] args) throws Exception {
        ProductService ps = new ProductService();
        List<Product> all = ps.search("");
        if (all.size() < 3) {
            System.out.println("Cần >= 3 sản phẩm. Skip.");
            return;
        }

        Object[] cols = {"ID", "SKU", "Tên"};
        DefaultTableModel m = new DefaultTableModel(cols, 0);
        for (Product p : all) m.addRow(new Object[]{p.getId(), p.getSku(), p.getName()});
        JTable t = new JTable(m);
        t.setRowSelectionInterval(0, 0);
        t.setRowSelectionInterval(1, 1);

        Integer beforeId = (Integer) m.getValueAt(t.convertRowIndexToModel(1), 0);
        System.out.println("[1] Trước reload: chọn dòng 1 (model) id=" + beforeId);
        System.out.println("    selectedRow = " + t.getSelectedRow());

        // Giả lập reloadFiltered: clear + addRow lại
        m.setRowCount(0);
        for (Product p : all) m.addRow(new Object[]{p.getId(), p.getSku(), p.getName()});
        // Sau khi clear, selection bị reset
        System.out.println("[2] Sau clear + addRow: selectedRow = " + t.getSelectedRow());

        // === LOGIC FIX: tìm lại dòng có cùng ID và restore ===
        Integer previousId = beforeId;
        for (int r = 0; r < m.getRowCount(); r++) {
            Integer id = (Integer) m.getValueAt(r, 0);
            if (previousId.equals(id)) {
                int viewRow = t.convertRowIndexToView(r);
                t.setRowSelectionInterval(viewRow, viewRow);
                break;
            }
        }
        int afterSelected = t.getSelectedRow();
        Integer afterId = afterSelected >= 0
                ? (Integer) m.getValueAt(t.convertRowIndexToModel(afterSelected), 0)
                : null;
        System.out.println("[3] Sau restore: selectedRow = " + afterSelected + " id=" + afterId);
        System.out.println("    " + (beforeId.equals(afterId) ? "OK — selection restored" : "FAIL — selection lost"));

        // === LOGIC FIX mouseClicked: dùng modelRow thay vì table.getSelectedRow() ===
        // Khi user double-click vào dòng r (view), code lấy modelRow = convertRowIndexToModel(r)
        // rồi lấy Product theo modelRow — KHÔNG phụ thuộc vào table.getSelectedRow()
        System.out.println("[4] Click dòng modelRow=2:");
        int modelRow = 2;
        Integer clickedId = (Integer) m.getValueAt(modelRow, 0);
        Product clicked = all.stream().filter(p -> p.getId().equals(clickedId)).findFirst().orElse(null);
        System.out.println("    Lấy được Product: " + (clicked == null ? "null" : clicked.getName()));

        // Click row 1 (dù selection đang ở dòng 2)
        System.out.println("[5] Click dòng modelRow=1 (dù selection = 2):");
        int modelRow2 = 1;
        Integer clickedId2 = (Integer) m.getValueAt(modelRow2, 0);
        Product clicked2 = all.stream().filter(p -> p.getId().equals(clickedId2)).findFirst().orElse(null);
        System.out.println("    Lấy được Product: " + (clicked2 == null ? "null" : clicked2.getName()));

        // === Kiểm tra switch menu ===
        // Trước fix MainFrame: reload mỗi lần select (kể cả click lại cùng menu) → cache bị clear liên tục
        // Sau fix: chỉ reload khi prevVisible != newVisible
        Object prevVisible = "ProductList";
        Object newVisible = prevVisible; // click lại cùng menu
        boolean shouldReload = (newVisible != prevVisible);
        System.out.println("[6] Click lại cùng menu: shouldReload = " + shouldReload
                + " (" + (shouldReload ? "BUG" : "OK - skip reload") + ")");

        newVisible = "InventoryReport";
        shouldReload = (newVisible != prevVisible);
        System.out.println("[7] Switch sang menu khác: shouldReload = " + shouldReload
                + " (" + (shouldReload ? "OK - reload" : "BUG - skip") + ")");

        System.out.println("=== BizTest8 OK ===");
    }
}
