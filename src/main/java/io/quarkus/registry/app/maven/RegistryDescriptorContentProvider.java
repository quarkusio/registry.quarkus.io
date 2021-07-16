package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;

@Singleton
public class RegistryDescriptorContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    private static final RegistryConfig REGISTRY_CONFIG = RegistriesConfigLocator.getDefaultRegistry();

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesRegistryDescriptor(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        StringWriter sw = new StringWriter();
        RegistriesConfigMapperHelper.toJson(REGISTRY_CONFIG, sw);
        String result = sw.toString();
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        String contentType = MediaType.APPLICATION_JSON;
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
}