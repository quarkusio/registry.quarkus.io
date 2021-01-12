package io.quarkus.registry.endpoints;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.neo4j.driver.Driver;

@Path("/hello")
public class WebhookEndpoint {

    @Inject
    ElasticSearc driver;

    @GET
    public Response get() {

    }
}
