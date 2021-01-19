package io.quarkus.registry.app.endpoints;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;

public class ArtifactCoords {

    @NotNull
    @FormParam("groupId")
    public String groupId;

    @NotNull
    @FormParam("groupId")
    public String artifactId;
}
