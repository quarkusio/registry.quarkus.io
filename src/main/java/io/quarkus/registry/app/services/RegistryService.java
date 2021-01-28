package io.quarkus.registry.app.services;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.util.JsonNodes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RegistryService {

    private final ArtifactResolverService resolver;

    private final JsonNodes jsonNodes;

    private static final Logger logger = Logger.getLogger(RegistryService.class);

    @Inject
    public RegistryService(ArtifactResolverService resolver, JsonNodes jsonNodes) {
        this.resolver = resolver;
        this.jsonNodes = jsonNodes;
    }

    @Transactional
    public void onPlatformCreate(@ObservesAsync PlatformCreateEvent event) {
        try {
            String groupId = event.getGroupId();
            String artifactId = event.getArtifactId();
            String version = event.getVersion();
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
                        newPlatformRelease.metadata = jsonNodes.toJsonNode(descriptor.getMetadata());
                        newPlatformRelease.persist();
                        return newPlatformRelease;
                    });

            descriptor.getCategories().forEach(category -> createCategory(category, platformRelease));

            // Insert extensions
            descriptor.getExtensions().forEach(ext -> createExtensionRelease(ext, platformRelease));
        } catch (Exception e) {
            logger.error("Error while inserting platform", e);
            throw e;
        }
    }

    @Transactional
    public void onExtensionCreate(@ObservesAsync ExtensionCreateEvent event) {
        String groupId = event.getGroupId();
        String artifactId = event.getArtifactId();
        String version = event.getVersion();

        JsonNode jsonNode = resolver.readExtensionYaml(groupId, artifactId, version);

        createExtensionRelease(groupId, artifactId, version, jsonNode.get("name").asText(),
                jsonNode.get("description").asText(), jsonNode.get("metadata"), null);
    }

    private Category createCategory(io.quarkus.dependencies.Category cat,
            PlatformRelease platformRelease) {
        // Insert Category if doesn't exist
        Optional<Category> byName = Category.findByName(cat.getName());
        if (byName.isPresent()) {
        }
        return byName
                .orElseGet(() -> {
                    Category category = new Category();
                    category.name = cat.getName();
                    category.description = cat.getDescription();
                    category.metadata = jsonNodes.toJsonNode(cat.getMetadata());
                    category.persistAndFlush();
                    return category;
                });
    }

    private ExtensionRelease createExtensionRelease(io.quarkus.dependencies.Extension ext, PlatformRelease platformRelease) {
        return createExtensionRelease(ext.getGroupId(), ext.getArtifactId(), ext.getVersion(), ext.getName(),
                ext.getDescription(), jsonNodes.toJsonNode(ext.getMetadata()), platformRelease);
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
                    newExtensionRelease.extension = extension;

                    // Many-to-many
                    if (platformRelease != null) {
                        PlatformExtension platformExtension = new PlatformExtension();
                        platformExtension.extensionRelease = newExtensionRelease;
                        platformExtension.platformRelease = platformRelease;
                        platformExtension.metadata = metadata;

                        platformRelease.extensions.add(platformExtension);
                        newExtensionRelease.platforms.add(platformExtension);
                    }
                    newExtensionRelease.persist();
                    return newExtensionRelease;
                });
    }

}
