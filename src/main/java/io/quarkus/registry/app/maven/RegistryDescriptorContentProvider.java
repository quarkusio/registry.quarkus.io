package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.app.CacheNames;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig;
import io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.JsonRegistryQuarkusVersionsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;

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

    private RegistryConfig getRegistryConfig(UriInfo uriInfo) {
        final JsonRegistryConfig qer = new JsonRegistryConfig();
        qer.setId(mavenConfig.getRegistryId());

        final JsonRegistryDescriptorConfig descriptor = new JsonRegistryDescriptorConfig();
        qer.setDescriptor(descriptor);
        descriptor.setArtifact(
                new ArtifactCoords(mavenConfig.getRegistryGroupId(),
                        Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID, null,
                        Constants.JSON, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

        final JsonRegistryMavenConfig registryMavenConfig = new JsonRegistryMavenConfig();
        qer.setMaven(registryMavenConfig);

        final JsonRegistryPlatformsConfig platformsConfig = new JsonRegistryPlatformsConfig();
        qer.setPlatforms(platformsConfig);
        platformsConfig.setArtifact(new ArtifactCoords(mavenConfig.getRegistryGroupId(),
                Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));

        if (mavenConfig.supportsNonPlatforms()) {
            final JsonRegistryNonPlatformExtensionsConfig nonPlatformExtensionsConfig = new JsonRegistryNonPlatformExtensionsConfig();
            qer.setNonPlatformExtensions(nonPlatformExtensionsConfig);
            nonPlatformExtensionsConfig.setArtifact(new ArtifactCoords(mavenConfig.getRegistryGroupId(),
                    Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID, null, Constants.JSON,
                    Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        }
        if (mavenConfig.getQuarkusVersionsExpression().isPresent()) {
            final JsonRegistryQuarkusVersionsConfig quarkusVersionsConfig = new JsonRegistryQuarkusVersionsConfig();
            quarkusVersionsConfig.setRecognizedVersionsExpression(mavenConfig.getQuarkusVersionsExpression().orElse(null));
            quarkusVersionsConfig.setExclusiveProvider(mavenConfig.getQuarkusVersionsExclusiveProvider().orElse(false));
            qer.setQuarkusVersions(quarkusVersionsConfig);
        }
        final JsonRegistryMavenRepoConfig mavenRepo = new JsonRegistryMavenRepoConfig();
        registryMavenConfig.setRepository(mavenRepo);
        mavenRepo.setId(mavenConfig.getRegistryId());
        mavenRepo.setUrl(mavenConfig.getRegistryUrl());
        return qer;
    }
}