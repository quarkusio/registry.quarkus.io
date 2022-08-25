package io.quarkus.registry.app.maven;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.maven.dependency.ArtifactCoords;

public interface ArtifactContentProvider {
    boolean supports(ArtifactCoords artifact, UriInfo uriInfo);

    Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception;

}
