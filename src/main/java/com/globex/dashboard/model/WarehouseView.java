package com.globex.dashboard.model;

import java.time.Instant;
import java.util.List;

public class WarehouseView {
    private String orderId;
    private String customerName;
    private Instant createdAt;
    private List<LineItemView> items;
    private int totalItems;

    public WarehouseView() {
    }

    public WarehouseView(String orderId, String customerName, Instant createdAt, List<LineItemView> items) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.createdAt = createdAt;
        this.items = items;
        this.totalItems = items != null ? items.stream().mapToInt(LineItemView::getQuantity).sum() : 0;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<LineItemView> getItems() {
        return items;
    }

    public void setItems(List<LineItemView> items) {
        this.items = items;
        this.totalItems = items != null ? items.stream().mapToInt(LineItemView::getQuantity).sum() : 0;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public static class LineItemView {
        private String productName;
        private int quantity;

        public LineItemView() {
        }

        public LineItemView(String productName, int quantity) {
            this.productName = productName;
            this.quantity = quantity;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}

