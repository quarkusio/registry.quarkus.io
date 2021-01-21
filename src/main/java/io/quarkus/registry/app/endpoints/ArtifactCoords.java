package io.quarkus.registry.app.endpoints;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;

@Vetoed
public class ArtifactCoords {

    @NotNull
    @FormParam("groupId")
    public String groupId;

    @NotNull
    @FormParam("artifactId")
    public String artifactId;

    @FormParam("version")
    public String version;

}
