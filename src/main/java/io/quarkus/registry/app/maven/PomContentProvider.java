package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import io.quarkus.maven.dependency.ArtifactCoords;

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
        String result = generatePom(artifact, uriInfo);
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        String contentType = MediaType.APPLICATION_XML;
        if (ArtifactParser.SUFFIX_MD5.equals(checksumSuffix)) {
            result = HashUtil.md5(result);
            contentType = MediaType.TEXT_PLAIN;
        } else if (ArtifactParser.SUFFIX_SHA1.equals(checksumSuffix)) {
            result = HashUtil.sha1(result);
            contentType = MediaType.TEXT_PLAIN;
        }
        return Response.ok(result)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .build();
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
