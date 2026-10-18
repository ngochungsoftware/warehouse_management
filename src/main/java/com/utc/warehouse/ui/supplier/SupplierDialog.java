package com.utc.warehouse.ui.supplier;

import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import java.awt.*;

public class SupplierDialog extends JDialog {

    private final PartnerService service = new PartnerService();
    private final Supplier original;
    private boolean saved = false;

    private final JTextField txtName    = new JTextField(20);
    private final JTextField txtPhone   = new JTextField(15);
    private final JTextField txtEmail   = new JTextField(20);
    private final JTextArea  txtAddress = new JTextArea(3, 20);

    public SupplierDialog(Window owner, Supplier s) {
        super(owner, s == null ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp", ModalityType.APPLICATION_MODAL);
        this.original = s;
        setSize(480, 320);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; form.add(label("Tên NCC (*)"), g);
        g.gridx = 1; form.add(txtName, g);

        g.gridx = 0; g.gridy = 1; form.add(label("Số điện thoại"), g);
        g.gridx = 1; form.add(txtPhone, g);

        g.gridx = 0; g.gridy = 2; form.add(label("Email"), g);
        g.gridx = 1; form.add(txtEmail, g);

        g.gridx = 0; g.gridy = 3; form.add(label("Địa chỉ"), g);
        g.gridx = 1;
        txtAddress.setLineWrap(true); txtAddress.setWrapStyleWord(true);
        form.add(new JScrollPane(txtAddress), g);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(UIConstants.PRIMARY); btnSave.setForeground(Color.WHITE);
        JButton btnCancel = new JButton("Hủy");
        btns.add(btnSave); btns.add(btnCancel);
        add(btns, BorderLayout.SOUTH);

        if (original != null) {
            txtName.setText(original.getName());
            txtPhone.setText(original.getPhone());
            txtEmail.setText(original.getEmail());
            txtAddress.setText(original.getAddress());
        }
        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());
    }

    private JLabel label(String s) { JLabel l = new JLabel(s); l.setFont(UIConstants.FONT_NORMAL); return l; }

    private void onSave() {
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên NCC không được trống."); return;
        }
        try {
            Supplier s = (original == null) ? new Supplier() : original;
            s.setName(txtName.getText().trim());
            s.setPhone(txtPhone.getText().trim());
            s.setEmail(txtEmail.getText().trim());
            s.setAddress(txtAddress.getText().trim());
            if (original == null) service.saveSupplier(s); else service.updateSupplier(s);
            saved = true; dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}
