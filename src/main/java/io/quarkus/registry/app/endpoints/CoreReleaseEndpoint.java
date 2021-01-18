package io.quarkus.registry.app.endpoints;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkus.registry.app.model.CoreRelease;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.pgclient.PgPool;

@Path("/core-releases")
public class CoreReleaseEndpoint {

    @Inject
    PgPool client;

    @GET
    public Multi<String> getCoreReleases() {
        return CoreRelease.findAllVersions(client);
    }

    @POST
    public void persist(CoreRelease coreRelease) {
        CoreRelease.persist(coreRelease);
    }

}
