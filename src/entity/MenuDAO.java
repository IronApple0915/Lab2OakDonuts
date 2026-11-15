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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuDAO implements DAO<Menu> {

    private static Menu map(ResultSet rs) throws SQLException {
        return new Menu(rs.getInt("Menu_ID"), rs.getString("Item_Name"), rs.getString("Menu_Category"), rs.getBigDecimal("Price"), rs.getBoolean("Customizable"));
    }

    @Override
    public Optional<Menu> get(int id) {
        String sql = "SELECT * FROM OD_Menu WHERE Menu_ID = ?";
        try (PreparedStatement ps = DB.getInstance().getPreparedStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read menu item " + id + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Menu> getAll() {
        List<Menu> items = new ArrayList<>();
        String sql = "SELECT * FROM OD_Menu ORDER BY Menu_ID";
        try (Statement st = DB.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) items.add(map(rs));
            return items;
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not read menu: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void insert(Menu m) {
        String sql = "INSERT INTO OD_Menu(Item_Name, Menu_Category, Price, Customizable) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DB.getInstance().getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setBigDecimal(3, m.getPrice());
            ps.setBoolean(4, m.isCustomizable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setID(keys.getInt(1));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not add menu item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void update(Menu m) {
        String sql = "UPDATE OD_Menu SET Item_Name=?, Menu_Category=?, Price=?, Customizable=? WHERE Menu_ID=?";
        try (PreparedStatement ps = DB.getInstance().getPreparedStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setBigDecimal(3, m.getPrice());
            ps.setBoolean(4, m.isCustomizable());
            ps.setInt(5, m.getID());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not update menu item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(Menu m) {
        String sql = "DELETE FROM OD_Menu WHERE Menu_ID = ?";
        try (PreparedStatement ps = DB.getInstance().getPreparedStatement(sql)) {
            ps.setInt(1, m.getID());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not delete menu item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getColumnNames() {
        List<String> headers = new ArrayList<>();
        try (Statement st = DB.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM OD_Menu WHERE Menu_ID = -1")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) headers.add(md.getColumnLabel(i));
            return headers;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}
