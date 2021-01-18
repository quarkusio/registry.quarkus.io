package io.quarkus.registry.app.endpoints;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

@Path("/registry")
public class RegistryEndpoint {

    @POST
    @Path("/platform")
    public Uni<Response> addPlatform(@NotNull @FormParam("groupId") String groupId, @NotNull @FormParam("artifactId") String artifactId) {
        return Uni.createFrom().item(Response.ok().build());
    }

}
