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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/.well-known")
public class LetsEncryptResource {

    @Inject
    @ConfigProperty(name = "ACME_CONTENTS")
    Optional<String> acmeContents;

    private static final Logger logger = Logger.getLogger(LetsEncryptResource.class);
    @GET
    @Path("/acme-challenge/{token}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public String challenge(@PathParam("token") String key) {
        logger.infof("Requested Token: %s", key);
        return acmeContents.orElseThrow(NotFoundException::new);
    }
}
