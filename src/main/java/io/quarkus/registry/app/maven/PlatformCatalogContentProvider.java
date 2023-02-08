package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;

@Singleton
public class PlatformCatalogContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    /**
     * @return true only if
     *         <ul>
     *         <li>System property/env quarkus.registry.platform.extension-catalog-included is <code>true</code></li>
     *         <li>The requested artifact version is 1.0-SNAPSHOT or matches the qualifier</li>
     *         <li>The requested groupId/artifactId/classifier matches an existing {@link PlatformRelease}</li>
     *         </ul>
     */
    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.getExtensionCatalogIncluded().orElse(Boolean.FALSE) &&
                (Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION.equals(artifact.getVersion())
                        || Objects.equals(artifact.getClassifier(), artifact.getVersion()))
                && PlatformRelease.artifactCoordinatesExist(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        String result = findExtensionCatalog(artifact, uriInfo);
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

    private String findExtensionCatalog(ArtifactCoords artifact, UriInfo uriInfo) throws IOException {
        PlatformRelease platformRelease = PlatformRelease.findByArtifactCoordinates(artifact)
                .orElseThrow(() -> new NotFoundException("Platform release requested not found"));
        Platform platform = platformRelease.platformStream.platform;
        List<Category> categories = Category.listAll();
        //TODO: This information is not stored in the DB
        String id = ArtifactCoords.of(platform.groupId, platform.artifactId, platformRelease.version, "json",
                platformRelease.version).toString();
        ExtensionCatalog expected = ExtensionCatalog.builder()
                .setId(id)
                //TODO: This information is not stored in the DB
                .setBom(ArtifactCoords.pom(platform.groupId, "quarkus-bom", platformRelease.version))
                .setPlatform(true)
                .setQuarkusCoreVersion(platformRelease.quarkusCoreVersion)
                .setMetadata(platformRelease.metadata)
                .setUpstreamQuarkusCoreVersion(platformRelease.upstreamQuarkusCoreVersion)
                .setCategories(
                        platformRelease.categories.stream().map(prc -> io.quarkus.registry.catalog.Category.builder()
                                .setId(prc.category.categoryKey)
                                .setName(prc.category.name)
                                .setMetadata(prc.metadata)
                                .setDescription(prc.category.description)
                                .build()).toList())
                .setExtensions(
                        platformRelease.extensions.stream().map(pe -> Extension.builder()
                                .setName(pe.extensionRelease.extension.name)
                                .setDescription(pe.extensionRelease.extension.description)
                                .setGroupId(pe.extensionRelease.extension.groupId)
                                .setArtifactId(pe.extensionRelease.extension.artifactId)
                                .setVersion(pe.extensionRelease.version)
                                .setMetadata(pe.metadata)
                                //TODO: This information is not stored in the DB
                                .setOrigins(List.of(ExtensionOrigin.builder().setId(id).build()))
                                .build()).toList())
                .build();
        return toString(expected);
    }

    private String toString(ExtensionCatalog catalog) throws IOException {
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(catalog, sw);
        return sw.toString();
    }

}
