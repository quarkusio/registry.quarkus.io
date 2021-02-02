package io.quarkus.registry.app.endpoints.maven;

import java.util.function.BiFunction;

import javax.ws.rs.core.UriInfo;

import org.apache.maven.artifact.Artifact;

public interface ArtifactRequestHandler {
    boolean supports(Artifact artifact, UriInfo uriInfo);

    String handle(Artifact artifact, UriInfo uriInfo) throws Exception;
}
