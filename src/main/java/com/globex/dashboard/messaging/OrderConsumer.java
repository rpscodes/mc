package com.globex.dashboard.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globex.dashboard.model.Order;
import com.globex.dashboard.service.OrderState;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class OrderConsumer {
    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);

    @Inject
    OrderState orderState;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("orders-in")
    @Blocking
    public void consume(String message) {
        try {
            DebeziumMessage debeziumMessage = objectMapper.readValue(message, DebeziumMessage.class);
            
            // Handle create (c), update (u), and read/snapshot (r) operations
            String op = debeziumMessage.getOp();
            if (op != null && ("c".equals(op) || "u".equals(op) || "r".equals(op))) {
                
                Object after = debeziumMessage.getAfter();
                if (after != null) {
                    ObjectNode orderNode = objectMapper.valueToTree(after);
                    
                    Order order = new Order();
                    
                    // Handle id - can be number or string
                    if (orderNode.has("id")) {
                        if (orderNode.get("id").isNumber()) {
                            order.setId(String.valueOf(orderNode.get("id").asLong()));
                        } else {
                            order.setId(orderNode.get("id").asText());
                        }
                    }
                    
                    order.setCustomerId(getStringValue(orderNode, "customer_id"));
                    
                    // total_amount might not exist in the message - calculate from line items later or set to 0
                    if (orderNode.has("total_amount")) {
                        order.setTotalAmount(getDoubleValue(orderNode, "total_amount"));
                    } else {
                        // Will be calculated from line items when they arrive
                        order.setTotalAmount(0.0);
                    }
                    
                    // Handle order_ts (microseconds since epoch) instead of created_at
                    if (orderNode.has("order_ts") && !orderNode.get("order_ts").isNull()) {
                        try {
                            long orderTsMicros = orderNode.get("order_ts").asLong();
                            // Convert microseconds to Instant (divide by 1,000,000 to get seconds)
                            order.setCreatedAt(Instant.ofEpochSecond(orderTsMicros / 1_000_000, 
                                (orderTsMicros % 1_000_000) * 1_000));
                        } catch (Exception e) {
                            LOG.warnf("Failed to parse order_ts, using current time: %s", e.getMessage());
                            order.setCreatedAt(Instant.now());
                        }
                    } else if (orderNode.has("created_at") && !orderNode.get("created_at").isNull()) {
                        // Fallback to created_at if it exists
                        String createdAtStr = getStringValue(orderNode, "created_at");
                        try {
                            order.setCreatedAt(Instant.parse(createdAtStr));
                        } catch (Exception e) {
                            order.setCreatedAt(Instant.now());
                        }
                    } else {
                        order.setCreatedAt(Instant.now());
                    }
                    
                    // Only upsert if order has a valid ID
                    if (order.getId() != null && !order.getId().isEmpty()) {
                        orderState.upsertOrder(order);
                        LOG.infof("Processed order: %s (op: %s)", order.getId(), op);
                    } else {
                        LOG.warnf("Skipping order with null or empty ID (op: %s)", op);
                    }
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing order message: %s", message);
        }
    }

    private String getStringValue(ObjectNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    private double getDoubleValue(ObjectNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asDouble();
        }
        return 0.0;
    }
}

