package io.quarkus.registry.app.maven;

import io.quarkus.maven.dependency.ArtifactCoords;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface ArtifactContentProvider {
    boolean supports(ArtifactCoords artifact, UriInfo uriInfo);

    Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception;

}
