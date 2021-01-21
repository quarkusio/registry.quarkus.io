package io.quarkus.registry.app.services;

import java.util.Collections;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class RegistryService {

    @Inject
    ArtifactResolverService resolver;

    public Uni<Platform> includeLatestPlatform(String groupId, String artifactId) {
        String latestVersion = resolver.resolveLatestVersion(groupId, artifactId);
        QuarkusPlatformDescriptor descriptor = resolver
                .resolvePlatformDescriptor(groupId, artifactId, latestVersion);
        final Platform platform = new Platform();

        platform.groupId = groupId;
        platform.artifactId = artifactId;

        PlatformRelease platformRelease = new PlatformRelease();
        platformRelease.version = latestVersion;
        platformRelease.platform = platform;
        platform.releases = Collections.singletonList(platformRelease);

        Uni<?> result = platform.persistAndFlush();
        for (io.quarkus.dependencies.Category cat : descriptor.getCategories()) {
            result = result.chain(() -> createCategory(cat));
        }
        // Insert extensions
        for (io.quarkus.dependencies.Extension ext : descriptor.getExtensions()) {
            result = result.chain(() -> createExtension(ext, platformRelease));
        }
        return result.onItem().transform(x->platform);
    }

    private Uni<Category> createCategory(io.quarkus.dependencies.Category cat) {
        // Insert Category if doesn't exist
        return Category.findByName(cat.getName())
                .onItem().ifNull().switchTo(() -> {
                    Category category = new Category();
                    category.name = cat.getName();
                    category.description = cat.getDescription();
                    return category.persistAndFlush().onItem().transform(x -> category);
                });
    }

    private Uni<Extension> createExtension(io.quarkus.dependencies.Extension ext, PlatformRelease platformRelease) {
        Supplier<Uni<? extends Extension>> createNewExtension = () -> {
            final Extension newExtension = new Extension();
            newExtension.groupId = ext.getGroupId();
            newExtension.artifactId = ext.getArtifactId();
            newExtension.name = ext.getName();
            newExtension.description = ext.getDescription();
            return newExtension.persistAndFlush()
                    .onItem().transform(x -> newExtension);
        };
        return Extension.findByGroupIdAndArtifactId(ext.getGroupId(), ext.getArtifactId())
                .onItem().ifNull().switchTo(createNewExtension)
                .onItem().transformToUni(extension ->
                        ExtensionRelease.findByExtensionAndVersion(extension, ext.getVersion())
                                .onItem().ifNull()
                                .switchTo(() -> {
                                    ExtensionRelease extensionRelease = new ExtensionRelease();
                                    extensionRelease.extension = extension;
                                    extensionRelease.version = ext.getVersion();
                                    extensionRelease.platforms.add(platformRelease);
                                    return extensionRelease.persistAndFlush()
                                            .onItem().transform(x -> extensionRelease);
                                })
                                .onItem()
                                .transform(extensionRelease -> {
                                    // Add release to extension
                                    extension.releases.add(extensionRelease);
                                    return extension.persistAndFlush();
                                }).onItem().transform(x -> extension)
                );
    }

    public Uni<Extension> includeLatestExtension(String groupId, String artifactId) {
        String latestVersion = resolver.resolveLatestVersion(groupId, artifactId);
        JsonNode jsonNode = resolver.readExtensionYaml(groupId, artifactId, latestVersion);

        final Extension extension = new Extension();
        extension.groupId = groupId;
        extension.artifactId = artifactId;
        extension.name = jsonNode.get("name").asText();
        extension.description = jsonNode.get("description").asText();
        extension.metadata = jsonNode.get("metadata");

        ExtensionRelease extensionRelease = new ExtensionRelease();
        extensionRelease.extension = extension;
        extensionRelease.version = latestVersion;
        extension.releases = Collections.singletonList(extensionRelease);

        return extension.persistAndFlush().onItem().transform(x -> extension);
    }
}
