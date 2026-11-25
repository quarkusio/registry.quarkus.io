package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import io.quarkus.maven.dependency.ArtifactCoords;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Singleton
@Priority(1000)
public class PomContentProvider implements ArtifactContentProvider {

    private static final MavenXpp3Writer POM_WRITER = new MavenXpp3Writer();

    @Inject
    MavenConfig mavenConfig;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.supports(artifact) &&
                artifact.getType().equals("pom");
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        return toResponse(artifact, uriInfo, generatePom(artifact, uriInfo), MediaType.APPLICATION_XML);
    }

    private static String generatePom(ArtifactCoords artifact, UriInfo uriInfo) throws IOException {
        Model model = new Model();
        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging("pom");

        final Repository repo = new Repository();
        repo.setId("quarkiverse-registry");
        repo.setName("Quarkiverse Extension Registry");
        repo.setUrl(uriInfo.getBaseUriBuilder().path("maven").toTemplate());

        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("always");
        repo.setSnapshots(policy);

        model.addRepository(repo);

        StringWriter writer = new StringWriter();
        POM_WRITER.write(writer, model);
        return writer.toString();
    }

}
