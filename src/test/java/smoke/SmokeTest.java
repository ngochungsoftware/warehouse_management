package smoke;

import com.utc.warehouse.entity.Category;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.util.MoneyFormat;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.ServiceLoader;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.spi.PersistenceProvider;

/**
 * Smoke test: verify mọi thứ load OK ngoại trừ kết nối DB thật.
 * Không qua HibernateUtil (vì static init fail khi không có MySQL).
 */
public class SmokeTest {

    public static void main(String[] args) {
        System.out.println("=== Warehouse Management - Smoke Test ===");
        boolean allOk = true;

        // 1. Entity instances
        try {
            User u = new User("test", "$2a$10$dummy", "Test User", Role.ADMIN);
            Category c = new Category("Đồ uống", "test");
            Product p = new Product();
            p.setSalePrice(new BigDecimal("10000")); p.setCostPrice(new BigDecimal("6000")); p.setMinStock(50);

            System.out.println("[1] Entities OK");
            System.out.println("    User: " + u.getUsername() + " / " + u.getRole() + " / isAdmin=" + u.isAdmin());
            System.out.println("    Category: " + c.getName());
            System.out.println("    Product: " + p.getSku() + " / sale=" + MoneyFormat.format(p.getSalePrice())
                    + " / cost=" + MoneyFormat.format(p.getCostPrice())
                    + " / LN/đv=" + MoneyFormat.format(p.getProfitPerUnit())
                    + " / margin=" + p.getProfitMarginPercent() + "%");
        } catch (Throwable t) {
            System.out.println("[1] FAIL: " + t);
            allOk = false;
        }

        // 2. Money format locale
        try {
            String v1 = MoneyFormat.format(new BigDecimal("1500000"));
            String v2 = MoneyFormat.format((Long) 1500000L);
            String v3 = MoneyFormat.format((BigDecimal) null);
            if (!v1.contains("1.500.000") || !v2.contains("1.500.000") || !v3.equals("0 đ")) {
                throw new RuntimeException("format mismatch: " + v1 + " / " + v2 + " / " + v3);
            }
            System.out.println("[2] MoneyFormat OK (" + Locale.getDefault() + "): " + v1);
        } catch (Throwable t) {
            System.out.println("[2] FAIL: " + t);
            allOk = false;
        }

        // 3. JPA PersistenceProvider visibility
        try {
            var loader = ServiceLoader.load(PersistenceProvider.class);
            int count = 0;
            StringBuilder sb = new StringBuilder();
            for (var p : loader) {
                sb.append(p.getClass().getName()).append(", ");
                count++;
            }
            if (count == 0) throw new RuntimeException("No PersistenceProvider via ServiceLoader");
            System.out.println("[3] JPA providers found: " + count + " -> " + sb);
        } catch (Throwable t) {
            System.out.println("[3] FAIL: " + t);
            allOk = false;
        }

        // 4. JPA Persistence.createEMF (sẽ fail vì không có MySQL, nhưng phải tới được bước scan entity)
        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory("warehousePU");
            System.out.println("[4] SessionFactory ready (không mong đợi khi không có MySQL)");
        } catch (Throwable t) {
            // Lỗi này là bình thường. Tìm xem lỗi gốc là gì.
            Throwable cause = t;
            String finalMsg = "";
            while (cause != null) {
                finalMsg = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                cause = cause.getCause();
            }
            if (finalMsg.contains("Unable to build Hibernate SessionFactory")
                    || finalMsg.contains("UserSuppliedConnectionProvider")
                    || finalMsg.contains("Communications")
                    || finalMsg.contains("Connection")
                    || finalMsg.contains("Access denied")
                    || finalMsg.contains("supply JDBC connections")) {
                System.out.println("[4] Hibernate config + entities OK (chỉ fail ở bước kết nối MySQL — đúng)");
                System.out.println("    Last error: " + finalMsg);
            } else {
                System.out.println("[4] FAIL: unexpected error: " + finalMsg);
                allOk = false;
            }
        } finally {
            if (emf != null && emf.isOpen()) emf.close();
        }

        System.out.println("=== " + (allOk ? "Smoke test PASSED" : "Smoke test FAILED") + " ===");
        System.exit(allOk ? 0 : 1);
    }
}
