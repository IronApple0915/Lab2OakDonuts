/**
 * Project: OakDonutsOD
 * Purpose Details: POS System for donut shop
 * Course: Java Programming
 * Author: Alex Beauchamp
 * Date Developed: 11/07/2026
 * Last Date Changed: 11/07/2026
 * Revision: 1.0
 */


package core;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//Derby DB setup
public class DB {
    private static final String DB_URL =
            "jdbc:derby:oakdonutsdb;create=true;user=oakdonutsdb;password=oakdonutsdb";
    private static DB instance;
    private final Connection mConnection;

    private DB() throws SQLException {
        mConnection = DriverManager.getConnection(DB_URL);
        try {
            initializeSchema();
        } catch (SQLException ex) {
            try { mConnection.close(); } catch (SQLException closeEx) { ex.addSuppressed(closeEx); }
            throw ex;
        }
    }

    public static synchronized DB getInstance() {
        if (instance == null) {
            try {
                instance = new DB();
            } catch (SQLException ex) {
                throw new IllegalStateException("Unable to initialize Apache Derby database", ex);
            }
        }
        return instance;
    }


    public static synchronized void shutdown() {
        try {
            if (instance != null) {
                instance.mConnection.close();
                instance = null;
            }
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException ex) {

        }
    }

    private boolean tableExists(String table) throws SQLException {

        try (ResultSet rs = mConnection.getMetaData().getTables(
                null, null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void initializeSchema() throws SQLException {

        if (!tableExists("OD_MENU")) {
            try (Statement st = mConnection.createStatement()) {
                st.executeUpdate("CREATE TABLE OD_Menu (" +
                        "Menu_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
                        "Item_Name VARCHAR(40) NOT NULL, " +
                        "Menu_Category VARCHAR(20) NOT NULL, " +
                        "Price DECIMAL(8,2) NOT NULL, " +
                        "Customizable BOOLEAN NOT NULL DEFAULT FALSE)");
            }
            seedMenu();
        }
        if (!tableExists("OD_ORDER")) {
            try (Statement st = mConnection.createStatement()) {
                st.executeUpdate("CREATE TABLE OD_Order (" +
                        "Transaction_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1001, INCREMENT BY 1) PRIMARY KEY, " +
                        "Order_Date TIMESTAMP NOT NULL, " +
                        "Subtotal DECIMAL(10,2) NOT NULL, " +
                        "Tax DECIMAL(10,2) NOT NULL, " +
                        "Order_Total DECIMAL(10,2) NOT NULL)");
            }
        }
        if (!tableExists("OD_ORDER_ITEM")) {
            try (Statement st = mConnection.createStatement()) {
                st.executeUpdate("CREATE TABLE OD_Order_Item (" +
                        "Line_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
                        "Transaction_ID INT NOT NULL, " +
                        "Item_Name VARCHAR(40) NOT NULL, " +
                        "Item_Options VARCHAR(80) NOT NULL, " +
                        "Quantity INT NOT NULL, " +
                        "Unit_Price DECIMAL(8,2) NOT NULL, " +
                        "Line_Total DECIMAL(10,2) NOT NULL, " +
                        "FOREIGN KEY (Transaction_ID) REFERENCES OD_Order(Transaction_ID) ON DELETE CASCADE)");
            }
        }
    }

    private void seedMenu() throws SQLException {
        Object[][] rows = {
                {"Glazed Donut", "Donut", "1.49", true},
                {"Chocolate Sprinkles", "Donut", "1.79", true},
                {"Boston Creme", "Donut", "1.99", true},
                {"House Coffee", "Coffee", "2.00", false},
                {"Latte", "Coffee", "3.00", false},
                {"Breakfast Sandwich", "Sandwich", "4.50", false},
        };
        try (PreparedStatement ps = mConnection.prepareStatement(
                "INSERT INTO OD_Menu(Item_Name, Menu_Category, Price, Customizable) VALUES (?, ?, ?, ?)")) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setBigDecimal(3, new BigDecimal((String) r[2]));
                ps.setBoolean(4, (Boolean) r[3]);
                ps.executeUpdate();
            }
        }
    }

    public Connection getConnection() {
        return mConnection;
    }

    public int executeUpdate(String sql) throws SQLException {
        try (Statement st = mConnection.createStatement()) {
            return st.executeUpdate(sql);
        }
    }


    public ResultSet executeQuery(String sql) throws SQLException {
        return mConnection.createStatement().executeQuery(sql);
    }

    public PreparedStatement getPreparedStatement(String sql) throws SQLException {
        return mConnection.prepareStatement(sql);
    }
}
