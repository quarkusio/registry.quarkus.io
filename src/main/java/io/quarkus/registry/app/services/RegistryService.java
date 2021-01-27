package io.quarkus.registry.app.services;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;

@ApplicationScoped
public class RegistryService {

    private final ArtifactResolverService resolver;

    private final ObjectMapper objectMapper;

    @Inject
    public RegistryService(ArtifactResolverService resolver, ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlatformRelease includePlatform(String groupId, String artifactId, String version) {
        QuarkusPlatformDescriptor descriptor = resolver
                .resolvePlatformDescriptor(groupId, artifactId, version);
        final Platform platform = Platform.findByGA(groupId, artifactId)
                .orElseGet(() -> {
                    Platform newPlatform = new Platform();
                    newPlatform.groupId = groupId;
                    newPlatform.artifactId = artifactId;
                    newPlatform.persist();
                    return newPlatform;
                });

        PlatformRelease platformRelease = PlatformRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    PlatformRelease newPlatformRelease = new PlatformRelease();
                    platform.releases.add(newPlatformRelease);
                    newPlatformRelease.platform = platform;
                    newPlatformRelease.version = version;
                    newPlatformRelease.metadata = toJsonNode(descriptor.getMetadata());
                    newPlatformRelease.persist();
                    return newPlatformRelease;
                });

        descriptor.getCategories().forEach(this::createCategory);

        // Insert extensions
        descriptor.getExtensions().forEach(ext -> createExtensionRelease(ext, platformRelease));
        return platformRelease;
    }

    @Transactional
    public ExtensionRelease includeExtensionRelease(String groupId, String artifactId, String version) {
        JsonNode jsonNode = resolver.readExtensionYaml(groupId, artifactId, version);

        return createExtensionRelease(groupId, artifactId, version, jsonNode.get("name").asText(),
                jsonNode.get("description").asText(), jsonNode.get("metadata"), null);
    }

    private Category createCategory(io.quarkus.dependencies.Category cat) {
        // Insert Category if doesn't exist
        return Category.findByName(cat.getName())
                .orElseGet(() -> {
                    Category category = new Category();
                    category.name = cat.getName();
                    category.description = cat.getDescription();
                    category.metadata = toJsonNode(cat.getMetadata());
                    category.persistAndFlush();
                    return category;
                });
    }

    private ExtensionRelease createExtensionRelease(io.quarkus.dependencies.Extension ext, PlatformRelease platformRelease) {
        return createExtensionRelease(ext.getGroupId(), ext.getArtifactId(), ext.getVersion(), ext.getName(),
                ext.getDescription(), toJsonNode(ext.getMetadata()), platformRelease);
    }

    private ExtensionRelease createExtensionRelease(String groupId, String artifactId, String version,
            String name, String description, JsonNode metadata, PlatformRelease platformRelease) {
        final Extension extension = Extension.findByGA(groupId, artifactId)
                .orElseGet(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = groupId;
                    newExtension.artifactId = artifactId;
                    newExtension.name = name;
                    newExtension.description = description;

                    newExtension.persist();
                    return newExtension;
                });
        return ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    ExtensionRelease newExtensionRelease = new ExtensionRelease();
                    newExtensionRelease.version = version;
                    newExtensionRelease.metadata = metadata;
                    newExtensionRelease.extension = extension;
                    // Many-to-many
                    newExtensionRelease.platforms.add(platformRelease);
                    platformRelease.extensions.add(newExtensionRelease);

                    newExtensionRelease.persist();
                    return newExtensionRelease;
                });
    }

    private JsonNode toJsonNode(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
