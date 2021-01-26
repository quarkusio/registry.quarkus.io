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
import io.quarkus.registry.app.model.Platform;

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
    public Platform includePlatform(String groupId, String artifactId, String version) {
        QuarkusPlatformDescriptor descriptor = resolver
                .resolvePlatformDescriptor(groupId, artifactId, version);
        final Platform platform = new Platform();

        platform.groupId = groupId;
        platform.artifactId = artifactId;
        platform.version = version;

        platform.persistAndFlush();

        descriptor.getCategories().forEach(this::createCategory);

        // Insert extensions
        descriptor.getExtensions().forEach(ext -> createExtension(ext, platform));
        return platform;
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

    private Extension createExtension(io.quarkus.dependencies.Extension ext, Platform platform) {
        return Extension.findByGAV(ext.getGroupId(), ext.getArtifactId(), ext.getVersion())
                .orElseGet(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = ext.getGroupId();
                    newExtension.artifactId = ext.getArtifactId();
                    newExtension.version = ext.getVersion();
                    newExtension.name = ext.getName();
                    newExtension.description = ext.getDescription();
                    newExtension.metadata = toJsonNode(ext.getMetadata());
                    newExtension.platforms.add(platform);
                    newExtension.persistAndFlush();
                    return newExtension;
                });
    }

    @Transactional
    public Extension includeExtension(String groupId, String artifactId, String version) {
        JsonNode jsonNode = resolver.readExtensionYaml(groupId, artifactId, version);
        return Extension.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = groupId;
                    newExtension.artifactId = artifactId;
                    newExtension.version = version;
                    newExtension.name = jsonNode.get("name").asText();
                    newExtension.description = jsonNode.get("description").asText();
                    newExtension.metadata = jsonNode.get("metadata");
                    newExtension.persistAndFlush();
                    return newExtension;
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
