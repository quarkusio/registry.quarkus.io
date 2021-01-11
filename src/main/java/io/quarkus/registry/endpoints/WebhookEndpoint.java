package io.quarkus.registry.endpoints;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.neo4j.driver.Driver;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/hello")
public class WebhookEndpoint {

    @Inject
    Driver driver;

    @GET
    public CompletionStage<Response> get() {
        try (org.neo4j.ogm.driver.Driver ogmDriver = new BoltDriver(driver)) {
            SessionFactory sessionFactory = new SessionFactory(ogmDriver, "io.quarkus.registry.model");
            Session session = sessionFactory.openSession();
            Result query = session.query("MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 5", Collections.emptyMap());
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Response.ok().build());
        }
    }
}
