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
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    private int transactionID;
    private Timestamp orderDate;
    private final List<OrderItem> items = new ArrayList<>();

    public Order(int transactionID, Timestamp orderDate) {
        this.transactionID = transactionID;
        this.orderDate = orderDate;
    }

    public int getTransactionID() { return transactionID; }
    public void setTransactionID(int transactionID) { this.transactionID = transactionID; }

    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }

    public List<OrderItem> getItems() { return items; }

    public BigDecimal getSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem i : items) sum = sum.add(i.getLineTotal());
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTax() {
        return getSubtotal().multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotal() {
        return getSubtotal().add(getTax());
    }

    public int getItemCount() {
        int n = 0;
        for (OrderItem i : items) n += i.getQuantity();
        return n;
    }

    public String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(orderDate);
    }

    @Override
    public String toString() {
        return "Order{transactionID=" + transactionID + ", date=" + getFormattedDate() + ", items=" + items + ", total=" + getTotal() + '}';
    }
}
