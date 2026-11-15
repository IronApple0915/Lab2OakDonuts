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


public class OrderItem {
    private final String itemName;
    private final String options;
    private int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(String itemName, String options, int quantity, BigDecimal unitPrice) {
        this.itemName = itemName;
        this.options = options;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getItemName() { return itemName; }
    public String getOptions() { return options; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return quantity + " x " + itemName + (options.equals("-") ? "" : " (" + options + ")");
    }
}
