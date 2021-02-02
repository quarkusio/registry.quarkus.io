package io.quarkus.registry.app.endpoints.maven;

import javax.ws.rs.core.UriInfo;

import org.apache.maven.artifact.Artifact;

public interface ArtifactContentProvider {
    boolean supports(Artifact artifact, UriInfo uriInfo);

    String provide(Artifact artifact, UriInfo uriInfo) throws Exception;
}
