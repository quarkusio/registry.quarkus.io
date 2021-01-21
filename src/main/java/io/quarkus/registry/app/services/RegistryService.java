package io.quarkus.registry.app.services;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.Platform;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class RegistryService {

    private final ArtifactResolverService resolver;

    private final ObjectMapper objectMapper;

    @Inject
    public RegistryService(ArtifactResolverService resolver, ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.objectMapper = objectMapper;
    }

    public Uni<Platform> includePlatform(String groupId, String artifactId, String version) {
        QuarkusPlatformDescriptor descriptor = resolver
                .resolvePlatformDescriptor(groupId, artifactId, version);
        final Platform platform = new Platform();

        platform.groupId = groupId;
        platform.artifactId = artifactId;
        platform.version = version;

        Uni<?> result = platform.persistAndFlush();
        for (io.quarkus.dependencies.Category cat : descriptor.getCategories()) {
            result = result.chain(() -> createCategory(cat));
        }
        // Insert extensions
        for (io.quarkus.dependencies.Extension ext : descriptor.getExtensions()) {
            result = result.chain(() -> createExtension(ext, platform));
        }
        return result.onItem().transform(x -> platform);
    }

    private Uni<Category> createCategory(io.quarkus.dependencies.Category cat) {
        // Insert Category if doesn't exist
        return Category.findByName(cat.getName())
                .onItem().ifNull().switchTo(() -> {
                    Category category = new Category();
                    category.name = cat.getName();
                    category.description = cat.getDescription();
                    category.metadata = toJsonNode(cat.getMetadata());
                    return category.persistAndFlush().onItem().transform(x -> category);
                });
    }

    private Uni<Extension> createExtension(io.quarkus.dependencies.Extension ext, Platform platform) {
        return Extension.findByGAV(ext.getGroupId(), ext.getArtifactId(), ext.getVersion())
                .onItem().ifNull().switchTo(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = ext.getGroupId();
                    newExtension.artifactId = ext.getArtifactId();
                    newExtension.version = ext.getVersion();
                    newExtension.name = ext.getName();
                    newExtension.description = ext.getDescription();
                    newExtension.metadata = toJsonNode(ext.getMetadata());
                    newExtension.platforms.add(platform);
                    return newExtension.persistAndFlush()
                            .onItem().transform(x -> newExtension);
                });
    }

    public Uni<Extension> includeExtension(String groupId, String artifactId, String version) {
        JsonNode jsonNode = resolver.readExtensionYaml(groupId, artifactId, version);
        return Extension.findByGAV(groupId, artifactId, version)
                .onItem().ifNull().switchTo(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = groupId;
                    newExtension.artifactId = artifactId;
                    newExtension.version = version;
                    newExtension.name = jsonNode.get("name").asText();
                    newExtension.description = jsonNode.get("description").asText();
                    newExtension.metadata = jsonNode.get("metadata");

                    return newExtension.persistAndFlush()
                            .onItem().transform(x -> newExtension);
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
