package com.utc.warehouse.ui.category;

import com.utc.warehouse.entity.Category;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import java.awt.*;

public class CategoryDialog extends JDialog {

    private final ProductService service = new ProductService();
    private final Category original;
    private boolean saved = false;

    private final JTextField txtName = new JTextField(20);
    private final JTextArea  txtDesc = new JTextArea(3, 20);

    public CategoryDialog(Window owner, Category category) {
        super(owner, category == null ? "Thêm danh mục" : "Sửa danh mục", ModalityType.APPLICATION_MODAL);
        this.original = category;
        setSize(420, 240);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        form.add(label("Tên danh mục (*)"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtName, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(label("Mô tả"), c);
        c.gridx = 1; c.weightx = 1;
        txtDesc.setLineWrap(true); txtDesc.setWrapStyleWord(true);
        form.add(new JScrollPane(txtDesc), c);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(UIConstants.PRIMARY); btnSave.setForeground(Color.WHITE);
        JButton btnCancel = new JButton("Hủy");
        btns.add(btnSave); btns.add(btnCancel);
        add(btns, BorderLayout.SOUTH);

        if (original != null) {
            txtName.setText(original.getName());
            txtDesc.setText(original.getDescription());
        }

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s); l.setFont(UIConstants.FONT_NORMAL); return l;
    }

    private void onSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên danh mục không được trống.");
            return;
        }
        try {
            Category c = (original == null) ? new Category() : original;
            c.setName(name);
            c.setDescription(txtDesc.getText().trim());
            if (original == null) service.saveCategory(c);
            else service.updateCategory(c);
            JOptionPane.showMessageDialog(this, "Lưu thành công!");
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}
