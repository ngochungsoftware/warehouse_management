package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/** Kiểm tra image_path thực tế trong DB. */
public class ImageDiag {

    public static void main(String[] args) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT p.id, p.sku, p.name, p.imagePath FROM Product p ORDER BY p.id")
                    .getResultList();

            System.out.println("Tong so san pham: " + rows.size());
            System.out.println("---");
            int coAnh = 0;
            for (Object[] r : rows) {
                String img = (String) r[3];
                if (img != null && !img.isBlank()) coAnh++;
                System.out.printf("id=%-4s sku=%-14s anh=%s%n",
                        r[0], r[1], (img == null || img.isBlank()) ? "(null)" : img);
            }
            System.out.println("---");
            System.out.println("Co anh: " + coAnh + "/" + rows.size());
        } finally {
            em.close();
        }
    }
}