package com.utc.warehouse.service;

import com.utc.warehouse.dao.CustomerDAO;
import com.utc.warehouse.dao.SupplierDAO;
import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Supplier;

import java.util.List;

public class PartnerService {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<Supplier> allSuppliers() { return supplierDAO.findAllOrdered(); }
    public Supplier saveSupplier(Supplier s) {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Tên nhà cung cấp không được trống");
        return supplierDAO.save(s);
    }
    public Supplier updateSupplier(Supplier s) { return supplierDAO.update(s); }
    public void deleteSupplier(Integer id) { supplierDAO.deleteById(id); }

    public List<Customer> allCustomers() { return customerDAO.findAllOrdered(); }
    public Customer saveCustomer(Customer c) {
        if (c.getName() == null || c.getName().isBlank())
            throw new IllegalArgumentException("Tên khách hàng không được trống");
        return customerDAO.save(c);
    }
    public Customer updateCustomer(Customer c) { return customerDAO.update(c); }
    public void deleteCustomer(Integer id) { customerDAO.deleteById(id); }
}
