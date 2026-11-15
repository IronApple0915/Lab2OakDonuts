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

import core.DB;
import entity.Menu;
import entity.MenuDAO;
import entity.Order;
import entity.OrderDAO;
import entity.OrderItem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Main extends JFrame {
    private static final String ALL = "All";
    private static final String[] ICING = {"None", "Chocolate", "Strawberry", "Maple", "Vanilla"};
    private static final String[] FILLING = {"None", "Custard", "Jelly", "Cream"};

    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private List<Menu> allMenu = new ArrayList<>();
    private final List<OrderItem> orderItems = new ArrayList<>();
    private Order editingOrder;

    // Filters
    private final JComboBox<String> categoryCombo = new JComboBox<>();
    private final JTextField searchField = new JTextField();
    private final JMenu categoryMenu = new JMenu("Category");
    private boolean loadingCategories = false;

    // Item options
    private final JComboBox<String> icingCombo = new JComboBox<>(ICING);
    private final JComboBox<String> fillingCombo = new JComboBox<>(FILLING);

    // Menu
    private final DefaultListModel<Menu> menuModel = new DefaultListModel<>();
    private final JList<Menu> menuList = new JList<>(menuModel);
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JLabel unitLabel = new JLabel("Unit: $0.00");

    // Order
    private final JLabel orderTitle = sectionLabel("Order");
    private final DefaultTableModel orderModel = new DefaultTableModel(new String[]{"Item", "Options", "Qty", "Price", "Total"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable orderTable = new JTable(orderModel);
    private final JLabel subtotalValue = new JLabel("$0.00", SwingConstants.RIGHT);
    private final JLabel taxValue = new JLabel("$0.00", SwingConstants.RIGHT);
    private final JLabel totalValue = new JLabel("$0.00", SwingConstants.RIGHT);

    public Main() {
        super("Oak Donuts – Ordering");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitApplication(); }
        });

        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        wireEvents();

        reloadMenu();
        refreshOrderTable();

        setSize(1050, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }


    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        return l;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem newOrder = new JMenuItem("New Order");
        newOrder.addActionListener(e -> clearOrder());
        JMenuItem history = new JMenuItem("Order History...");
        history.addActionListener(e -> openOrderHistory());
        JMenuItem manage = new JMenuItem("Manage Menu...");
        manage.addActionListener(e -> openMenuManager());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exitApplication());
        file.add(newOrder);
        file.add(history);
        file.add(manage);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this, "Oak Donuts OD\nJava Swing GUI + Apache Derby database\n" + "Menu and orders with full CRUD.", "About Oak Donuts OD", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(categoryMenu);
        bar.add(help);
        return bar;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel title = new JLabel("Oak Donuts");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        JLabel subtitle = new JLabel("Menu & Order");
        subtitle.setForeground(Color.GRAY);
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        // Filters + item options
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(190, 0));
        left.add(sectionLabel("Filters"));
        left.add(Box.createVerticalStrut(8));
        left.add(leftAligned(new JLabel("Category:")));
        left.add(fullWidth(categoryCombo));
        left.add(Box.createVerticalStrut(10));
        left.add(leftAligned(new JLabel("Search:")));
        left.add(fullWidth(searchField));
        left.add(Box.createVerticalStrut(14));
        left.add(new JSeparator());
        left.add(Box.createVerticalStrut(14));
        left.add(sectionLabel("Item Options"));
        left.add(Box.createVerticalStrut(8));
        left.add(leftAligned(new JLabel("Icing:")));
        left.add(fullWidth(icingCombo));
        left.add(Box.createVerticalStrut(10));
        left.add(leftAligned(new JLabel("Filling:")));
        left.add(fullWidth(fillingCombo));
        left.add(Box.createVerticalGlue());
        root.add(left, BorderLayout.WEST);

        // Menu (middle)
        JPanel menuPanel = new JPanel(new BorderLayout(0, 8));
        menuPanel.add(sectionLabel("Menu"), BorderLayout.NORTH);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuPanel.add(new JScrollPane(menuList), BorderLayout.CENTER);
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addRow.add(new JLabel("Qty:"));
        qtySpinner.setPreferredSize(new Dimension(60, qtySpinner.getPreferredSize().height));
        addRow.add(qtySpinner);
        addRow.add(unitLabel);
        JButton addButton = new JButton("Add to Order");
        addButton.addActionListener(e -> addToOrder());
        addRow.add(addButton);
        menuPanel.add(addRow, BorderLayout.SOUTH);

        // Order (right)
        JPanel orderPanel = new JPanel(new BorderLayout(0, 8));
        orderPanel.add(orderTitle, BorderLayout.NORTH);
        orderTable.setRowHeight(22);
        orderTable.getTableHeader().setReorderingAllowed(false);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {170, 230, 45, 70, 80};
        for (int i = 0; i < widths.length; i++) {
            orderTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        orderPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel totals = new JPanel(new GridLayout(3, 2, 4, 2));
        totals.add(new JLabel("Subtotal:"));
        totals.add(subtotalValue);
        totals.add(new JLabel("Tax (6%):"));
        totals.add(taxValue);
        JLabel totalLabel = new JLabel("Total:");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalValue.setFont(totalValue.getFont().deriveFont(Font.BOLD, 16f));
        totals.add(totalLabel);
        totals.add(totalValue);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelected());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearOrder());
        JButton checkoutButton = new JButton("Checkout");
        checkoutButton.addActionListener(e -> checkout());
        buttons.add(removeButton);
        buttons.add(clearButton);
        buttons.add(checkoutButton);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(totals, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        orderPanel.add(south, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0.36;
        gc.insets = new Insets(0, 0, 0, 12);
        center.add(menuPanel, gc);
        gc.gridx = 1;
        gc.weightx = 0.64;
        gc.insets = new Insets(0, 0, 0, 0);
        center.add(orderPanel, gc);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private static JComponent leftAligned(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 4));
        return c;
    }

    private void wireEvents() {
        categoryCombo.addActionListener(e -> {
            if (!loadingCategories) refreshMenuList();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshMenuList(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshMenuList(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshMenuList(); }
        });
        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelectedItem();
        });
    }


    void reloadMenu() {
        try {
            allMenu = menuDAO.getAll();
        } catch (RuntimeException ex) {
            showError(ex);
            allMenu = new ArrayList<>();
        }
        Object previous = categoryCombo.getSelectedItem();
        Set<String> categories = new LinkedHashSet<>();
        for (Menu m : allMenu) categories.add(m.getCategory());

        loadingCategories = true;
        categoryCombo.removeAllItems();
        categoryCombo.addItem(ALL);
        for (String c : categories) categoryCombo.addItem(c);
        categoryCombo.setSelectedItem(previous != null && (ALL.equals(previous) || categories.contains(previous))
                ? previous : ALL);
        loadingCategories = false;

        categoryMenu.removeAll();
        JMenuItem allItem = new JMenuItem(ALL);
        allItem.addActionListener(e -> categoryCombo.setSelectedItem(ALL));
        categoryMenu.add(allItem);
        for (String c : categories) {
            JMenuItem item = new JMenuItem(c);
            item.addActionListener(e -> categoryCombo.setSelectedItem(c));
            categoryMenu.add(item);
        }
        refreshMenuList();
    }

    private void refreshMenuList() {
        String category = (String) categoryCombo.getSelectedItem();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        menuModel.clear();
        for (Menu m : allMenu) {
            boolean categoryOk = category == null || ALL.equals(category) || category.equals(m.getCategory());
            boolean searchOk = query.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(query);
            if (categoryOk && searchOk) menuModel.addElement(m);
        }
        if (!menuModel.isEmpty()) menuList.setSelectedIndex(0);
        updateSelectedItem();
    }

    private void updateSelectedItem() {
        Menu m = menuList.getSelectedValue();
        unitLabel.setText("Unit: " + (m == null ? "$0.00" : money(m.getPrice())));
        boolean customizable = m != null && m.isCustomizable();
        icingCombo.setEnabled(customizable);
        fillingCombo.setEnabled(customizable);
        if (!customizable) {
            icingCombo.setSelectedIndex(0);
            fillingCombo.setSelectedIndex(0);
        }
    }



    private void addToOrder() {
        Menu m = menuList.getSelectedValue();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Select a menu item first.", "Add to Order", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int qty = (Integer) qtySpinner.getValue();
        String options = optionsText(m);

        // Identical item + options merge into one line.
        for (OrderItem line : orderItems) {
            if (line.getItemName().equals(m.getName()) && line.getOptions().equals(options) && line.getUnitPrice().compareTo(m.getPrice()) == 0) {
                line.setQuantity(line.getQuantity() + qty);
                refreshOrderTable();
                return;
            }
        }
        orderItems.add(new OrderItem(m.getName(), options, qty, m.getPrice()));
        refreshOrderTable();
    }

    private String optionsText(Menu m) {
        if (!m.isCustomizable()) return "-";
        List<String> parts = new ArrayList<>();
        String icing = (String) icingCombo.getSelectedItem();
        String filling = (String) fillingCombo.getSelectedItem();
        if (icing != null && !icing.equals("None")) parts.add("Icing: " + icing);
        if (filling != null && !filling.equals("None")) parts.add("Filling: " + filling);
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private void removeSelected() {
        int row = orderTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order line to remove.", "Remove", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        orderItems.remove(row);
        refreshOrderTable();
    }

    private void clearOrder() {
        orderItems.clear();
        editingOrder = null;
        refreshOrderTable();
    }

    private Order currentOrder() {
        Order order = new Order(editingOrder == null ? 0 : editingOrder.getTransactionID(), editingOrder == null ? new Timestamp(System.currentTimeMillis()) : editingOrder.getOrderDate());
        order.getItems().addAll(orderItems);
        return order;
    }

    private void refreshOrderTable() {
        orderModel.setRowCount(0);
        for (OrderItem i : orderItems) {
            orderModel.addRow(new Object[]{i.getItemName(), i.getOptions(), i.getQuantity(), money(i.getUnitPrice()), money(i.getLineTotal())});
        }
        Order order = currentOrder();
        subtotalValue.setText(money(order.getSubtotal()));
        taxValue.setText(money(order.getTax()));
        totalValue.setText(money(order.getTotal()));
        orderTitle.setText(editingOrder == null ? "Order" : "Order – editing transaction #" + editingOrder.getTransactionID());
    }

    private void checkout() {
        if (orderItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The order is empty.", "Checkout", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Order order = currentOrder();
        boolean updating = editingOrder != null;
        try {
            if (updating) orderDAO.update(order); else orderDAO.insert(order);
        } catch (RuntimeException ex) {
            showError(ex);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Thank you!\nTransaction #" + order.getTransactionID() + (updating ? " updated" : " saved") + "\nTotal due: " + money(order.getTotal()), "Checkout", JOptionPane.INFORMATION_MESSAGE);
        clearOrder();
    }

    void editOrder(Order order) {
        orderItems.clear();
        for (OrderItem i : order.getItems()) {
            orderItems.add(new OrderItem(i.getItemName(), i.getOptions(), i.getQuantity(), i.getUnitPrice()));
        }
        editingOrder = order;
        refreshOrderTable();
        toFront();
    }



    private void openOrderHistory() {
        new OrderHistoryDialog(this, orderDAO, this::editOrder).setVisible(true);
    }

    private void openMenuManager() {
        new MenuManagerDialog(this, menuDAO, this::reloadMenu).setVisible(true);
    }

    private void exitApplication() {
        DB.shutdown();
        dispose();
        System.exit(0);
    }

    static String money(BigDecimal value) {
        return String.format("$%.2f", value);
    }

    static void showError(Component parent, RuntimeException ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage(), "Oak Donuts", JOptionPane.ERROR_MESSAGE);
    }

    private void showError(RuntimeException ex) {
        showError(this, ex);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> {
            try {
                DB.getInstance();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Oak Donuts", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new Main().setVisible(true);
        });
    }
}
