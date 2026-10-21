package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.PartnerService;
import jakarta.persistence.EntityManager;

import java.util.List;

/** Kiem tra DB co NCC va partnerService tra ve gi. */
public class SupplierDiag {
    public static void main(String[] args) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Long n = em.createQuery("SELECT COUNT(s) FROM Supplier s", Long.class).getSingleResult();
            System.out.println("DB SUPPLIERS: " + n);

            @SuppressWarnings("unchecked")
            List<Supplier> all = em.createQuery("FROM Supplier s ORDER BY s.id", Supplier.class).getResultList();
            System.out.println("Danh sach:");
            for (Supplier s : all) System.out.println("  #" + s.getId() + " | " + s.getName());

            var svc = new PartnerService().allSuppliers();
            System.out.println("partnerService.allSuppliers() size = " + svc.size());
        } finally {
            em.close();
        }
    }
}