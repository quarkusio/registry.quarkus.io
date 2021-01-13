package io.quarkus.registry.endpoints;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkus.registry.model.CoreRelease;

@Path("/core-releases")
public class CoreReleaseEndpoint {

    @GET
    public List<String> getCoreReleases() {
        return CoreRelease.findAllVersions();
    }

    @POST
    public void persist(CoreRelease coreRelease) {
        CoreRelease.persist(coreRelease);
    }

}
