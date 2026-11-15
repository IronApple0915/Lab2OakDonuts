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

import java.math.BigDecimal;


public class Menu {
    private int ID;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean customizable;

    public Menu(int ID, String name, String category, BigDecimal price, boolean customizable) {
        this.ID = ID;
        this.name = name;
        this.category = category;
        this.price = price;
        this.customizable = customizable;
    }

    public int getID() { return ID; }
    public void setID(int ID) { this.ID = ID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isCustomizable() { return customizable; }
    public void setCustomizable(boolean customizable) { this.customizable = customizable; }

    // Text shown in the menu list
    @Override
    public String toString() {
        return name + " — $" + String.format("%.2f", price);
    }
}
