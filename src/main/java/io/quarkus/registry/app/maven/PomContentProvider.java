package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

@ApplicationScoped
public class PomContentProvider implements ArtifactContentProvider {

    private static final MavenXpp3Writer POM_WRITER = new MavenXpp3Writer();

    @Inject
    Config config;

    @Override
    public boolean supports(Artifact artifact, UriInfo uriInfo) {
        return config.supports(artifact) &&
                artifact.getType().equals("pom");
    }

    @Override
    public String provide(Artifact artifact, UriInfo uriInfo) throws Exception {
        String result = generatePom(artifact, uriInfo);
        if (artifact.getType().endsWith(".md5")) {
            result = HashUtil.md5(result);
        } else if (artifact.getType().endsWith(".sha1")) {
            result = HashUtil.sha1(result);
        }
        return result;
    }

    private static String generatePom(Artifact artifact, UriInfo uriInfo) throws IOException {
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
