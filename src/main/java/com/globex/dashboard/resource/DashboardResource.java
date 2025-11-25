package com.globex.dashboard.resource;

import com.globex.dashboard.model.LoyaltyView;
import com.globex.dashboard.model.OrderView;
import com.globex.dashboard.service.OrderState;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@Path("/api")
public class DashboardResource {

    @Inject
    OrderState orderState;

    @GET
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrderView> getOrders() {
        return orderState.getRecentOrders(50);
    }

    @GET
    @Path("/loyalty")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LoyaltyView> getLoyalty() {
        return orderState.getLoyaltyByCustomer();
    }

    @GET
    @Path("/debug")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("customerCount", orderState.getCustomerCount());
        debug.put("orderCount", orderState.getOrderCount());
        debug.put("customerIds", orderState.getCustomerIds());
        debug.put("orderCustomerIds", orderState.getOrderCustomerIds());
        
        // Show which order customer IDs don't have matching customers
        Set<String> customerIds = orderState.getCustomerIds();
        Set<String> orderCustomerIds = orderState.getOrderCustomerIds();
        Set<String> missingCustomers = new java.util.HashSet<>(orderCustomerIds);
        missingCustomers.removeAll(customerIds);
        debug.put("missingCustomerIds", missingCustomers);
        
        return debug;
    }
}

