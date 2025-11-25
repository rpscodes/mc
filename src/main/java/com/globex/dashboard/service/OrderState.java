package com.globex.dashboard.service;

import com.globex.dashboard.model.Customer;
import com.globex.dashboard.model.LineItem;
import com.globex.dashboard.model.Order;
import com.globex.dashboard.model.LoyaltyView;
import com.globex.dashboard.model.OrderView;
import com.globex.dashboard.model.WarehouseView;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderState {
    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, List<LineItem>> lineItemsByOrder = new HashMap<>();

    public synchronized void upsertCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    public synchronized void upsertOrder(Order order) {
        orders.put(order.getId(), order);
        
        // Recalculate total from existing line items if any exist
        // This handles the case where line items arrived before the order
        List<LineItem> lineItems = lineItemsByOrder.get(order.getId());
        if (lineItems != null && !lineItems.isEmpty()) {
            double totalAmount = lineItems.stream()
                    .mapToDouble(li -> li.getUnitPrice() * li.getQuantity())
                    .sum();
            order.setTotalAmount(totalAmount);
        }
    }

    public synchronized void upsertLineItem(LineItem lineItem) {
        lineItemsByOrder.computeIfAbsent(lineItem.getOrderId(), k -> new ArrayList<>())
                .removeIf(li -> li.getId().equals(lineItem.getId()));
        lineItemsByOrder.computeIfAbsent(lineItem.getOrderId(), k -> new ArrayList<>())
                .add(lineItem);
        
        // Calculate and update order total from line items
        Order order = orders.get(lineItem.getOrderId());
        if (order != null) {
            List<LineItem> lineItems = lineItemsByOrder.get(lineItem.getOrderId());
            double totalAmount = lineItems.stream()
                    .mapToDouble(li -> li.getUnitPrice() * li.getQuantity())
                    .sum();
            order.setTotalAmount(totalAmount);
        }
    }

    public synchronized List<OrderView> getRecentOrders(int limit) {
        return orders.values().stream()
                .filter(order -> order != null && order.getId() != null)
                .sorted(Comparator.comparing(
                    (Order order) -> order.getCreatedAt() != null ? order.getCreatedAt() : Instant.EPOCH,
                    Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .limit(limit)
                .map(order -> {
                    String customerId = order.getCustomerId();
                    Customer customer = customerId != null ? customers.get(customerId) : null;
                    String customerName = customer != null ? customer.getName() : "Unknown";
                    return new OrderView(
                            order.getId(),
                            customerName,
                            order.getTotalAmount(),
                            order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now()
                    );
                })
                .collect(Collectors.toList());
    }

    public synchronized List<LoyaltyView> getLoyaltyByCustomer() {
        Map<String, Double> totalSpendByCustomer = new HashMap<>();
        
        // Calculate total spend per customer
        for (Order order : orders.values()) {
            if (order != null && order.getCustomerId() != null) {
                totalSpendByCustomer.merge(
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        Double::sum
                );
            }
        }

        // Build loyalty views
        List<LoyaltyView> loyaltyViews = new ArrayList<>();
        for (Map.Entry<String, Double> entry : totalSpendByCustomer.entrySet()) {
            String customerId = entry.getKey();
            double totalSpend = entry.getValue();
            long loyaltyPoints = (long) (totalSpend * 100);
            
            Customer customer = customers.get(customerId);
            String customerName = customer != null ? customer.getName() : "Unknown";
            
            loyaltyViews.add(new LoyaltyView(customerId, customerName, totalSpend, loyaltyPoints));
        }

        // Sort by loyalty points descending
        return loyaltyViews.stream()
                .sorted(Comparator.comparing(LoyaltyView::getLoyaltyPoints).reversed())
                .collect(Collectors.toList());
    }

    public synchronized int getCustomerCount() {
        return customers.size();
    }

    public synchronized int getOrderCount() {
        return orders.size();
    }

    public synchronized Set<String> getCustomerIds() {
        return customers.keySet();
    }

    public synchronized Set<String> getOrderCustomerIds() {
        return orders.values().stream()
                .filter(order -> order != null && order.getCustomerId() != null)
                .map(Order::getCustomerId)
                .collect(Collectors.toSet());
    }

    public synchronized List<WarehouseView> getWarehouseOrders(int limit) {
        return orders.values().stream()
                .filter(order -> order != null && order.getId() != null)
                .sorted(Comparator.comparing(
                    (Order order) -> order.getCreatedAt() != null ? order.getCreatedAt() : Instant.EPOCH,
                    Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .limit(limit)
                .map(order -> {
                    String customerId = order.getCustomerId();
                    Customer customer = customerId != null ? customers.get(customerId) : null;
                    String customerName = customer != null ? customer.getName() : "Unknown";
                    
                    // Get line items for this order
                    List<LineItem> lineItems = lineItemsByOrder.getOrDefault(order.getId(), new ArrayList<>());
                    List<WarehouseView.LineItemView> itemViews = lineItems.stream()
                            .map(li -> new WarehouseView.LineItemView(
                                    li.getProductName() != null ? li.getProductName() : "Unknown Product",
                                    li.getQuantity()
                            ))
                            .collect(Collectors.toList());
                    
                    return new WarehouseView(
                            order.getId(),
                            customerName,
                            order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now(),
                            itemViews
                    );
                })
                .collect(Collectors.toList());
    }
}

