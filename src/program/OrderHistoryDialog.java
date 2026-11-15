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

import entity.Order;
import entity.OrderDAO;
import entity.OrderItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


class OrderHistoryDialog extends JDialog {
    private final OrderDAO dao;
    private final Consumer<Order> onEdit;
    private List<Order> orders = new ArrayList<>();

    private final DefaultTableModel orderModel = new DefaultTableModel(
            new String[]{"Transaction ID", "Date", "Items", "Subtotal", "Tax", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable orderTable = new JTable(orderModel);

    private final DefaultTableModel itemModel = new DefaultTableModel(
            new String[]{"Item", "Options", "Qty", "Price", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable itemTable = new JTable(itemModel);

    OrderHistoryDialog(Frame owner, OrderDAO dao, Consumer<Order> onEdit) {
        super(owner, "Order History", true);
        this.dao = dao;
        this.onEdit = onEdit;

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setRowHeight(22);
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showItemsForSelection();
        });
        itemTable.setRowHeight(22);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(new JLabel("Orders"), BorderLayout.NORTH);
        top.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.add(new JLabel("Items ordered"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.5);
        root.add(split, BorderLayout.CENTER);

        JButton edit = new JButton("Edit Order");
        edit.addActionListener(e -> editSelected());
        JButton delete = new JButton("Delete Order");
        delete.addActionListener(e -> deleteSelected());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(760, 520);
        setLocationRelativeTo(owner);
        refresh();
    }

    private void refresh() {
        try {
            orders = dao.getAll();
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        orderModel.setRowCount(0);
        for (Order o : orders) {
            orderModel.addRow(new Object[]{o.getTransactionID(), o.getFormattedDate(), o.getItemCount(), Main.money(o.getSubtotal()), Main.money(o.getTax()), Main.money(o.getTotal())});
        }
        itemModel.setRowCount(0);
        if (!orders.isEmpty()) orderTable.setRowSelectionInterval(0, 0);
    }

    private Order selected() {
        int row = orderTable.getSelectedRow();
        return row < 0 || row >= orders.size() ? null : orders.get(row);
    }

    private void showItemsForSelection() {
        itemModel.setRowCount(0);
        Order o = selected();
        if (o == null) return;
        for (OrderItem i : o.getItems()) {
            itemModel.addRow(new Object[]{i.getItemName(), i.getOptions(), i.getQuantity(), Main.money(i.getUnitPrice()), Main.money(i.getLineTotal())});
        }
    }

    private void editSelected() {
        Order o = selected();
        if (o == null) {
            JOptionPane.showMessageDialog(this, "Select an order to edit.", "Order History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        dispose();
        onEdit.accept(o);
    }

    private void deleteSelected() {
        Order o = selected();
        if (o == null) {
            JOptionPane.showMessageDialog(this, "Select an order to delete.", "Order History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete transaction #" + o.getTransactionID() + " (" + Main.money(o.getTotal()) + ")?", "Delete Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            dao.delete(o);
        } catch (RuntimeException ex) {
            Main.showError(this, ex);
            return;
        }
        refresh();
    }
}
