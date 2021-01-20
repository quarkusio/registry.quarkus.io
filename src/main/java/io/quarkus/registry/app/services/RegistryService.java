package io.quarkus.registry.app.services;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

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
            // Insert Category if doesn't exist
            result = result.chain(() -> Category.findByName(cat.getName())
                    .onFailure().recoverWithUni(() -> {
                        Category category = new Category();
                        category.name = cat.getName();
                        category.description = cat.getDescription();
                        return category.persistAndFlush().onItem().castTo(Category.class);
                    }));
        }
        // Insert extensions
//        for (io.quarkus.dependencies.Extension ext : descriptor.getExtensions()) {
//            result = result.chain(() -> Extension.findByGroupIdAndArtifactId(ext.getGroupId(), ext.getArtifactId())
//                    .onFailure(NoResultException.class)
//                    .recoverWithUni(() -> {
//                        final Extension extension = new Extension();
//                        extension.groupId = groupId;
//                        extension.artifactId = artifactId;
//                        extension.name = ext.getName();
//                        extension.description = ext.getDescription();
//
//                        ExtensionRelease extensionRelease = new ExtensionRelease();
//                        extensionRelease.extension = extension;
//                        extensionRelease.version = ext.getVersion();
//                        extensionRelease.platforms = Collections.singletonList(platformRelease);
//                        extension.releases = Collections.singletonList(extensionRelease);
//
//                        return extension.persistAndFlush().onItem().castTo(Extension.class);
//                    }));
//        }
        return result.onItem().castTo(Platform.class);
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

        return extension.persistAndFlush().onItem().castTo(Extension.class);
    }
}
