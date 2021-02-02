package io.quarkus.registry.app.endpoints.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriInfo;

import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.util.HashUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

@ApplicationScoped
public class PomContentProvider implements ArtifactContentProvider {

    private static final MavenXpp3Writer POM_WRITER = new MavenXpp3Writer();

    @Override
    public boolean supports(Artifact artifact, UriInfo uriInfo) {
        return artifact.getType().startsWith("pom");
    }

    @Override
    public String provide(Artifact artifact, UriInfo uriInfo) throws Exception {
        Optional<PlatformRelease> platformRelease = PlatformRelease
                .findByGAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        if (platformRelease.isPresent()) {
            String result = generatePom(artifact, uriInfo);
            if (artifact.getType().endsWith(".md5")) {
                result = HashUtil.md5(result);
            } else if (artifact.getType().endsWith(".sha1")) {
                result = HashUtil.sha1(result);
            }
            return result;
        }
        return null;
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
        repo.setUrl(new URL(uriInfo.getBaseUri().toURL(), "maven").toExternalForm());

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
