package io.quarkus.registry.app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/.well-known")
public class ACMEResource {

    @Inject
    @ConfigProperty(name = "ACME_CONTENTS")
    Optional<String> acmeContents;

    @Inject
    @ConfigProperty(name = "ACME_THUMBPRINT")
    Optional<String> acmeThumbprint;

    private static final Logger logger = Logger.getLogger(ACMEResource.class);

    @GET
    @Path("/{type:(acme-challenge|pki-validation)}/{token}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public Response challenge(@PathParam("type") String type, @PathParam("token") String token) {
        logger.infof("Requested Token: %s", token);
        return Response.ok(acmeThumbprint
                                   .map(s -> token + "." + s)
                                   .orElseGet(() -> acmeContents.orElseThrow(NotFoundException::new)))
                .header("Connection", "close")
                .build();
    }
}
