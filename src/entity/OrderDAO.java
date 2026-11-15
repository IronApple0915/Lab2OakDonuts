/**
 * Project: OakDonutsOD
 * Purpose Details: POS System for donut shop
 * Course: Java Programming
 * Author: Alex Beauchamp
 * Date Developed: 11/07/2026
 * Last Date Changed: 11/07/2026
 * Revision: 1.0
 */


package entity;

import core.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO implements DAO<Order> {

    private List<OrderItem> loadItems(Connection c, int transactionID) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT Item_Name, Item_Options, Quantity, Unit_Price " + "FROM OD_Order_Item WHERE Transaction_ID = ? ORDER BY Line_ID";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, transactionID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(rs.getString("Item_Name"), rs.getString("Item_Options"), rs.getInt("Quantity"), rs.getBigDecimal("Unit_Price")));
                }
            }
        }
        return items;
    }

    private void insertItems(Connection c, Order order) throws SQLException {
        String sql = "INSERT INTO OD_Order_Item(Transaction_ID, Item_Name, Item_Options, Quantity, Unit_Price, Line_Total) " + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (OrderItem i : order.getItems()) {
                ps.setInt(1, order.getTransactionID());
                ps.setString(2, i.getItemName());
                ps.setString(3, i.getOptions());
                ps.setInt(4, i.getQuantity());
                ps.setBigDecimal(5, i.getUnitPrice());
                ps.setBigDecimal(6, i.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public Optional<Order> get(int id) {
        Connection c = DB.getInstance().getConnection();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT Transaction_ID, Order_Date FROM OD_Order WHERE Transaction_ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Order order = new Order(rs.getInt("Transaction_ID"), rs.getTimestamp("Order_Date"));
                order.getItems().addAll(loadItems(c, id));
                return Optional.of(order);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read order " + id + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Order> getAll() {
        Connection c = DB.getInstance().getConnection();
        List<Order> orders = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT Transaction_ID, Order_Date FROM OD_Order ORDER BY Transaction_ID DESC")) {
            while (rs.next()) {
                orders.add(new Order(rs.getInt("Transaction_ID"), rs.getTimestamp("Order_Date")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read orders: " + ex.getMessage(), ex);
        }
        try {
            for (Order o : orders) o.getItems().addAll(loadItems(c, o.getTransactionID()));
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read order items: " + ex.getMessage(), ex);
        }
        return orders;
    }


    @Override
    public void insert(Order order) {
        Connection c = DB.getInstance().getConnection();
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO OD_Order(Order_Date, Subtotal, Tax, Order_Total) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setTimestamp(1, order.getOrderDate());
                ps.setBigDecimal(2, order.getSubtotal());
                ps.setBigDecimal(3, order.getTax());
                ps.setBigDecimal(4, order.getTotal());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No transaction ID was generated.");
                    order.setTransactionID(keys.getInt(1));
                }
            }
            insertItems(c, order);
            c.commit();
        } catch (SQLException ex) {
            rollbackQuietly(c);
            throw new IllegalStateException("Could not save order: " + ex.getMessage(), ex);
        } finally {
            restoreAutoCommit(c, previousAutoCommit);
        }
    }

    @Override
    public void update(Order order) {
        Connection c = DB.getInstance().getConnection();
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE OD_Order SET Order_Date=?, Subtotal=?, Tax=?, Order_Total=? WHERE Transaction_ID=?")) {
                ps.setTimestamp(1, order.getOrderDate());
                ps.setBigDecimal(2, order.getSubtotal());
                ps.setBigDecimal(3, order.getTax());
                ps.setBigDecimal(4, order.getTotal());
                ps.setInt(5, order.getTransactionID());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM OD_Order_Item WHERE Transaction_ID = ?")) {
                ps.setInt(1, order.getTransactionID());
                ps.executeUpdate();
            }
            insertItems(c, order);
            c.commit();
        } catch (SQLException ex) {
            rollbackQuietly(c);
            throw new IllegalStateException("Could not update order: " + ex.getMessage(), ex);
        } finally {
            restoreAutoCommit(c, previousAutoCommit);
        }
    }

    @Override
    public void delete(Order order) {
        try (PreparedStatement ps = DB.getInstance().getPreparedStatement(
                "DELETE FROM OD_Order WHERE Transaction_ID = ?")) {
            ps.setInt(1, order.getTransactionID());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not delete order: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getColumnNames() {
        List<String> headers = new ArrayList<>();
        try (Statement st = DB.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM OD_Order WHERE Transaction_ID = -1")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) headers.add(md.getColumnLabel(i));
            return headers;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private static void rollbackQuietly(Connection c) {
        try { c.rollback(); } catch (SQLException ignored) { }
    }

    private static void restoreAutoCommit(Connection c, boolean value) {
        try { c.setAutoCommit(value); } catch (SQLException ignored) { }
    }
}
