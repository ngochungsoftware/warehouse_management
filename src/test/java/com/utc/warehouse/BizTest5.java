package com.utc.warehouse;

import com.utc.warehouse.dao.StockTransactionDAO;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * BizTest5: Verify JOIN FETCH details + parse id an toàn.
 */
public class BizTest5 {

    public static void main(String[] args) throws Exception {
        StockTransactionDAO dao = new StockTransactionDAO();

        // 1. findAllOrdered: phải trả list có details + product sẵn (ngoài session)
        List<StockTransaction> all = dao.findAllOrdered();
        System.out.println("[1] findAllOrdered: " + all.size() + " phiếu");

        // 2. Truy cập details.size() NGOÀI session (DAO đã close()) → không LazyInit
        for (StockTransaction t : all) {
            int sz = t.getDetails() == null ? 0 : t.getDetails().size();
            // Truy cập product.getSku() cũng ngoài session → nếu fetch OK sẽ chạy
            String firstSku = (t.getDetails() != null && !t.getDetails().isEmpty()
                    && t.getDetails().get(0).getProduct() != null)
                    ? t.getDetails().get(0).getProduct().getSku() : "(empty)";
            System.out.println("    #" + t.getId() + " type=" + t.getType()
                    + " details=" + sz + " firstSku=" + firstSku);
        }

        // 3. Tính tổng tiền cho mỗi phiếu — nếu lazy thì sẽ throw
        BigDecimal sum = BigDecimal.ZERO;
        for (StockTransaction t : all) {
            for (StockTransactionDetail d : t.getDetails()) {
                sum = sum.add(d.getLineTotal());
            }
        }
        System.out.println("[2] Tổng tất cả line_total = " + sum);

        // 4. findByType
        List<StockTransaction> imports = dao.findByType(TransactionType.IMPORT);
        List<StockTransaction> exports = dao.findByType(TransactionType.EXPORT);
        System.out.println("[3] IMPORT=" + imports.size() + " EXPORT=" + exports.size());

        // 5. findByIdWithDetails
        if (!all.isEmpty()) {
            Integer firstId = all.get(0).getId();
            StockTransaction t = dao.findByIdWithDetails(firstId);
            System.out.println("[4] findByIdWithDetails(" + firstId + ") = "
                    + (t == null ? "NULL" : t.getId() + " details=" + t.getDetails().size()));
        }

        // 6. extractId giả lập — chuỗi "#5" -> 5
        String sample = "#42";
        Integer parsed = Integer.valueOf(sample.replace("#", "").trim());
        System.out.println("[5] parse '#42' -> " + parsed);

        System.out.println("=== BizTest5 OK ===");
    }
}
