package io.quarkus.registry.app.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkus.registry.app.model.CoreRelease;
import io.smallrye.mutiny.Multi;

@Path("/core-releases")
public class CoreReleaseEndpoint {

    @GET
    public Multi<String> getCoreReleases() {
        return CoreRelease.findAllVersions();
    }

    @POST
    public void persist(CoreRelease coreRelease) {
        coreRelease.persistAndFlush();
    }

}
