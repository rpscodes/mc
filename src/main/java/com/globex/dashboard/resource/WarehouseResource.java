package com.globex.dashboard.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

@Path("/warehouse")
public class WarehouseResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getWarehouseDashboard() {
        InputStream htmlStream = getClass().getClassLoader()
                .getResourceAsStream("META-INF/resources/warehouse.html");
        
        if (htmlStream == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("<h1>Warehouse dashboard not found</h1>")
                    .build();
        }
        
        return Response.ok(htmlStream).build();
    }
}

