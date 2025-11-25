package com.globex.dashboard.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globex.dashboard.model.LineItem;
import com.globex.dashboard.service.OrderState;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LineItemConsumer {
    private static final Logger LOG = Logger.getLogger(LineItemConsumer.class);

    @Inject
    OrderState orderState;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("lineitem-in")
    @Blocking
    public void consume(String message) {
        try {
            DebeziumMessage debeziumMessage = objectMapper.readValue(message, DebeziumMessage.class);
            
            // Handle create (c), update (u), and read/snapshot (r) operations
            String op = debeziumMessage.getOp();
            if (op != null && ("c".equals(op) || "u".equals(op) || "r".equals(op))) {
                
                Object after = debeziumMessage.getAfter();
                if (after != null) {
                    ObjectNode lineItemNode = objectMapper.valueToTree(after);
                    
                    LineItem lineItem = new LineItem();
                    
                    // Handle id - can be number or string
                    if (lineItemNode.has("id")) {
                        if (lineItemNode.get("id").isNumber()) {
                            lineItem.setId(String.valueOf(lineItemNode.get("id").asLong()));
                        } else {
                            lineItem.setId(lineItemNode.get("id").asText());
                        }
                    }
                    
                    // Handle order_id - can be number or string
                    if (lineItemNode.has("order_id")) {
                        if (lineItemNode.get("order_id").isNumber()) {
                            lineItem.setOrderId(String.valueOf(lineItemNode.get("order_id").asLong()));
                        } else {
                            lineItem.setOrderId(lineItemNode.get("order_id").asText());
                        }
                    }
                    
                    // Use product_code instead of product_name
                    lineItem.setProductName(getStringValue(lineItemNode, "product_code"));
                    
                    lineItem.setQuantity(getIntValue(lineItemNode, "quantity"));
                    
                    // Use price instead of unit_price (price might be a string)
                    if (lineItemNode.has("price") && !lineItemNode.get("price").isNull()) {
                        if (lineItemNode.get("price").isTextual()) {
                            try {
                                lineItem.setUnitPrice(Double.parseDouble(lineItemNode.get("price").asText()));
                            } catch (NumberFormatException e) {
                                LOG.warnf("Failed to parse price as double: %s", lineItemNode.get("price").asText());
                                lineItem.setUnitPrice(0.0);
                            }
                        } else {
                            lineItem.setUnitPrice(lineItemNode.get("price").asDouble());
                        }
                    } else {
                        lineItem.setUnitPrice(0.0);
                    }
                    
                    orderState.upsertLineItem(lineItem);
                    LOG.infof("Processed line item: %s (op: %s)", lineItem.getId(), op);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing line item message: %s", message);
        }
    }

    private String getStringValue(ObjectNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    private int getIntValue(ObjectNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asInt();
        }
        return 0;
    }

    private double getDoubleValue(ObjectNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asDouble();
        }
        return 0.0;
    }
}

