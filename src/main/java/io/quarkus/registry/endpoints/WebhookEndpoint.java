package io.quarkus.registry.endpoints;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.quarkus.registry.model.Actor;
import org.neo4j.driver.Driver;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/hello")
public class WebhookEndpoint {

    @Inject
    Driver driver;

    @GET
    public Response get() {
        try (org.neo4j.ogm.driver.Driver ogmDriver = new BoltDriver(driver)) {
            SessionFactory sessionFactory = new SessionFactory(ogmDriver, "io.quarkus.registry.model");
            Session session = sessionFactory.openSession();
            Iterable<Actor> query = session
                    .query(Actor.class, "MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 5", Collections.emptyMap());
            return Response.ok(query).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }
}
