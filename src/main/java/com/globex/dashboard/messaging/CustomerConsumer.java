package com.globex.dashboard.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globex.dashboard.model.Customer;
import com.globex.dashboard.service.OrderState;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CustomerConsumer {
    private static final Logger LOG = Logger.getLogger(CustomerConsumer.class);

    @Inject
    OrderState orderState;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("customer-in")
    @Blocking
    public void consume(String message) {
        LOG.infof("Received customer message: %s", message);
        try {
            DebeziumMessage debeziumMessage = objectMapper.readValue(message, DebeziumMessage.class);
            LOG.infof("Parsed Debezium message, op: %s", debeziumMessage.getOp());
            
            // Handle create (c), update (u), and read/snapshot (r) operations
            String op = debeziumMessage.getOp();
            if (op != null && ("c".equals(op) || "u".equals(op) || "r".equals(op))) {
                
                Object after = debeziumMessage.getAfter();
                if (after != null) {
                    ObjectNode customerNode = objectMapper.valueToTree(after);
                    LOG.infof("Processing customer after data, has user_id: %s, has id: %s", 
                        customerNode.has("user_id"), customerNode.has("id"));
                    
                    Customer customer = new Customer();
                    
                    // Use user_id as the customer ID since orders reference customers by user_id
                    // If user_id doesn't exist, fall back to id
                    if (customerNode.has("user_id") && !customerNode.get("user_id").isNull()) {
                        String userId = customerNode.get("user_id").asText();
                        customer.setId(userId);
                        LOG.infof("Set customer ID from user_id: %s", userId);
                    } else if (customerNode.has("id")) {
                        if (customerNode.get("id").isNumber()) {
                            String id = String.valueOf(customerNode.get("id").asLong());
                            customer.setId(id);
                            LOG.infof("Set customer ID from numeric id: %s", id);
                        } else {
                            String id = customerNode.get("id").asText();
                            customer.setId(id);
                            LOG.infof("Set customer ID from string id: %s", id);
                        }
                    } else {
                        LOG.warnf("Customer message has neither user_id nor id field");
                    }
                    
                    // Combine first_name and last_name into name
                    String firstName = customerNode.has("first_name") && !customerNode.get("first_name").isNull() 
                        ? customerNode.get("first_name").asText() : "";
                    String lastName = customerNode.has("last_name") && !customerNode.get("last_name").isNull() 
                        ? customerNode.get("last_name").asText() : "";
                    customer.setName((firstName + " " + lastName).trim());
                    
                    // Set email
                    if (customerNode.has("email") && !customerNode.get("email").isNull()) {
                        customer.setEmail(customerNode.get("email").asText());
                    }
                    
                    // Only upsert if customer has a valid ID
                    if (customer.getId() != null && !customer.getId().isEmpty()) {
                        orderState.upsertCustomer(customer);
                        LOG.infof("Successfully processed customer: %s, name: %s (op: %s)", customer.getId(), customer.getName(), op);
                    } else {
                        java.util.Iterator<String> fieldNames = customerNode.fieldNames();
                        java.util.ArrayList<String> fieldList = new java.util.ArrayList<>();
                        fieldNames.forEachRemaining(fieldList::add);
                        String fieldNamesStr = fieldList.isEmpty() ? "none" : String.join(", ", fieldList);
                        LOG.warnf("Skipping customer with null or empty ID. Customer node keys: %s (op: %s)", fieldNamesStr, op);
                    }
                } else {
                    LOG.warnf("Customer message has null 'after' field (op: %s)", op);
                }
            } else {
                LOG.infof("Skipping customer message with op: %s (not c, u, or r)", op);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing customer message: %s", message);
        }
    }
}

