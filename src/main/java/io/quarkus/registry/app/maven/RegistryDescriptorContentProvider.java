package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.app.CacheNames;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryMavenRepoConfig;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;

@Singleton
public class RegistryDescriptorContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesRegistryDescriptor(artifact);
    }

    @Override
    @CacheResult(cacheName = CacheNames.DESCRIPTOR)
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        StringWriter sw = new StringWriter();
        RegistryConfig registryConfig = getRegistryConfig(uriInfo);
        RegistriesConfigMapperHelper.toJson(registryConfig, sw);
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

    @CacheResult(cacheName = CacheNames.DESCRIPTOR)
    RegistryConfig getRegistryConfig(UriInfo uriInfo) {
        final RegistryConfig.Mutable qer = RegistryConfig.builder();
        qer.setId(mavenConfig.getRegistryId());

        final RegistryDescriptorConfig.Mutable descriptor = RegistryDescriptorConfig.builder();
        descriptor.setArtifact(
                ArtifactCoords.of(mavenConfig.getRegistryGroupId(),
                        Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                        Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        qer.setDescriptor(descriptor.build());

        final RegistryMavenConfig.Mutable registryMavenConfig = RegistryMavenConfig.builder();

        final RegistryPlatformsConfig.Mutable platformsConfig = RegistryPlatformsConfig.builder();
        platformsConfig.setArtifact(ArtifactCoords.of(mavenConfig.getRegistryGroupId(),
                Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        qer.setPlatforms(platformsConfig.build());

        if (mavenConfig.supportsNonPlatforms()) {
            final RegistryNonPlatformExtensionsConfig.Mutable nonPlatformExtensionsConfig = RegistryNonPlatformExtensionsConfig
                    .builder();
            nonPlatformExtensionsConfig.setArtifact(ArtifactCoords.of(mavenConfig.getRegistryGroupId(),
                    Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
            qer.setNonPlatformExtensions(nonPlatformExtensionsConfig.build());
        }
        if (mavenConfig.getQuarkusVersionsExpression().isPresent()) {
            final RegistryQuarkusVersionsConfig.Mutable quarkusVersionsConfig = RegistryQuarkusVersionsConfig.builder();
            quarkusVersionsConfig.setRecognizedVersionsExpression(mavenConfig.getQuarkusVersionsExpression().orElse(null));
            quarkusVersionsConfig.setExclusiveProvider(mavenConfig.getQuarkusVersionsExclusiveProvider().orElse(false));
            qer.setQuarkusVersions(quarkusVersionsConfig.build());
        }
        final RegistryMavenRepoConfig.Mutable mavenRepo = RegistryMavenRepoConfig.builder();
        mavenRepo.setId(mavenConfig.getRegistryId());
        mavenRepo.setUrl(mavenConfig.getRegistryUrl());
        registryMavenConfig.setRepository(mavenRepo.build());
        qer.setMaven(registryMavenConfig.build());
        return qer.build();
    }
}