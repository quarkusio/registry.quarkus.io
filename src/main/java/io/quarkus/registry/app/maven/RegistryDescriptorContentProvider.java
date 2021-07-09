package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.config.RegistriesConfigLocator;

@Singleton
public class RegistryDescriptorContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesRegistryDescriptor(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        StringWriter sw = new StringWriter();
        JsonCatalogMapperHelper.serialize(objectMapper, RegistriesConfigLocator.getDefaultRegistry(), sw);
        String result = sw.toString();
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        if (ArtifactParser.SUFFIX_MD5.equals(checksumSuffix)) {
            result = HashUtil.md5(result);
        } else if (ArtifactParser.SUFFIX_SHA1.equals(checksumSuffix)) {
            result = HashUtil.sha1(result);
        }
        return Response.ok(result)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }
}