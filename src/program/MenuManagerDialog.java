/**
 * Project: OakDonutsOD
 * Purpose Details: POS System for donut shop
 * Course: Java Programming
 * Author: Alex Beauchamp
 * Date Developed: 11/07/2026
 * Last Date Changed: 11/07/2026
 * Revision: 1.0
 */


package program;

import entity.Menu;
import entity.MenuDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


class MenuManagerDialog extends JDialog {
    private final MenuDAO dao;
    private final Runnable onChange;
    private List<Menu> items = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Category", "Price", "Icing/Filling"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField nameField = new JTextField(18);
    private final JComboBox<String> categoryCombo = new JComboBox<>();
    private final JTextField priceField = new JTextField(8);
    private final JCheckBox customizableBox = new JCheckBox("Icing / filling options");

    MenuManagerDialog(Frame owner, MenuDAO dao, Runnable onChange) {
        super(owner, "Manage Menu", true);
        this.dao = dao;
        this.onChange = onChange;
        categoryCombo.setEditable(true);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedIntoForm();
        });
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Name:"), gc);
        gc.gridx = 1; form.add(nameField, gc);
        gc.gridx = 2; form.add(new JLabel("Category:"), gc);
        gc.gridx = 3; form.add(categoryCombo, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(new JLabel("Price:"), gc);
        gc.gridx = 1; form.add(priceField, gc);
        gc.gridx = 2; gc.gridwidth = 2; form.add(customizableBox, gc);

        JButton add = new JButton("Add");
        add.addActionListener(e -> addItem());
        JButton update = new JButton("Update");
        update.addActionListener(e -> updateItem());
        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> deleteItem());
        JButton clear = new JButton("Clear Form");
        clear.addActionListener(e -> clearForm());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);
        buttons.add(close);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(720, 470);
        setLocationRelativeTo(owner);
        refresh();
    }

    private void refresh() {
        try {
            items = dao.getAll();
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        model.setRowCount(0);
        Set<String> categories = new LinkedHashSet<>();
        for (Menu m : items) {
            model.addRow(new Object[]{m.getID(), m.getName(), m.getCategory(), Main.money(m.getPrice()), m.isCustomizable() ? "Yes" : "No"});
            categories.add(m.getCategory());
        }
        Object current = categoryCombo.getEditor().getItem();
        categoryCombo.removeAllItems();
        for (String c : categories) categoryCombo.addItem(c);
        categoryCombo.setSelectedItem(current);
    }

    private void loadSelectedIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= items.size()) return;
        Menu m = items.get(row);
        nameField.setText(m.getName());
        categoryCombo.setSelectedItem(m.getCategory());
        priceField.setText(m.getPrice().toPlainString());
        customizableBox.setSelected(m.isCustomizable());
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        categoryCombo.setSelectedItem("");
        priceField.setText("");
        customizableBox.setSelected(false);
    }


    private Menu readForm() {
        String name = nameField.getText().trim();
        Object cat = categoryCombo.getEditor().getItem();
        String category = cat == null ? "" : cat.toString().trim();
        if (name.isEmpty() || category.isEmpty()) {
            warn("Name and category are required.");
            return null;
        }
        if (name.length() > 40 || category.length() > 20) {
            warn("Name can be up to 40 characters and category up to 20.");
            return null;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim().replace("$", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            warn("Price must be a number, for example 1.49.");
            return null;
        }
        if (price.signum() < 0 || price.compareTo(new BigDecimal("999999.99")) > 0) {
            warn("Price must be between 0.00 and 999999.99.");
            return null;
        }
        return new Menu(0, name, category, price, customizableBox.isSelected());
    }

    private void addItem() {
        Menu m = readForm();
        if (m == null) return;
        try {
            dao.insert(m);
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        afterChange();
        clearForm();
    }

    private void updateItem() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a menu item to update."); return; }
        Menu m = readForm();
        if (m == null) return;
        m.setID(items.get(row).getID());
        try {
            dao.update(m);
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        afterChange();
    }

    private void deleteItem() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a menu item to delete."); return; }
        Menu m = items.get(row);
        int choice = JOptionPane.showConfirmDialog(this, "Delete \"" + m.getName() + "\" from the menu?", "Delete Menu Item", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            dao.delete(m);
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        afterChange();
        clearForm();
    }

    private void afterChange() {
        refresh();
        onChange.run();
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Manage Menu", JOptionPane.WARNING_MESSAGE);
    }
}
