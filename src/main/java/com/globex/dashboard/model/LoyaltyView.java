package com.globex.dashboard.model;

public class LoyaltyView {
    private String customerId;
    private String customerName;
    private double totalSpend;
    private long loyaltyPoints;

    public LoyaltyView() {
    }

    public LoyaltyView(String customerId, String customerName, double totalSpend, long loyaltyPoints) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.totalSpend = totalSpend;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(double totalSpend) {
        this.totalSpend = totalSpend;
    }

    public long getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(long loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }
}

