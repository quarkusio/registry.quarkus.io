package io.quarkus.registry.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/hello")
public class WebhookEndpoint {

    @GET
    public Response get() {
        return Response.ok().build();
    }
}
