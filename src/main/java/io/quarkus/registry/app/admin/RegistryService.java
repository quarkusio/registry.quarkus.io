package io.quarkus.registry.app.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.transaction.Transactional;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RegistryService {

    private static final Logger logger = Logger.getLogger(RegistryService.class);

    @Transactional
    public void onPlatformCreate(@ObservesAsync PlatformCreateEvent event) {
        try {
            insertPlatform(event.getPlatform());
        } catch (Exception e) {
            logger.error("Error while inserting platform", e);
        }
    }

    private void insertPlatform(io.quarkus.registry.catalog.Platform payload) {
        final String groupId = payload.getBom().getGroupId();
        final String artifactId = payload.getBom().getArtifactId();
        final String version = payload.getBom().getVersion();
        final Platform platform = Platform.findByGA(groupId, artifactId)
                .orElseGet(() -> {
                    Platform newPlatform = new Platform();
                    newPlatform.groupId = groupId;
                    newPlatform.artifactId = artifactId;
                    newPlatform.persist();
                    return newPlatform;
                });

        PlatformRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    PlatformRelease newPlatformRelease = new PlatformRelease();
                    platform.releases.add(newPlatformRelease);
                    newPlatformRelease.platform = platform;
                    newPlatformRelease.version = version;
                    newPlatformRelease.quarkusCore = payload.getQuarkusCoreVersion();
                    newPlatformRelease.quarkusCoreUpstream = payload.getUpstreamQuarkusCoreVersion();
                    newPlatformRelease.persist();
                    return newPlatformRelease;
                });
    }

    @Transactional
    public void onExtensionCreate(@ObservesAsync ExtensionCreateEvent event) {
        // Non-platform extension
        try {
            createExtensionRelease(event.getExtension(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ExtensionRelease createExtensionRelease(io.quarkus.registry.catalog.Extension ext,
                                                    PlatformRelease platformRelease) {
        ArtifactCoords coords = ext.getArtifact();
        final String groupId = coords.getGroupId();
        final String artifactId = coords.getArtifactId();
        final String version = coords.getVersion();
        final Extension extension = Extension.findByGA(groupId, artifactId)
                .orElseGet(() -> {
                    Extension newExtension = new Extension();
                    newExtension.groupId = coords.getGroupId();
                    newExtension.artifactId = artifactId;
                    newExtension.name = ext.getName();
                    newExtension.description = ext.getDescription();

                    newExtension.persist();
                    return newExtension;
                });
        return ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    ExtensionRelease newExtensionRelease = new ExtensionRelease();
                    newExtensionRelease.version = version;
                    newExtensionRelease.extension = extension;
                    newExtensionRelease.quarkusCore = (String) ext.getMetadata().get("built-with-quarkus-core");
                    // Many-to-many
                    if (platformRelease != null) {
                        PlatformExtension platformExtension = new PlatformExtension();
                        platformExtension.extensionRelease = newExtensionRelease;
                        platformExtension.platformRelease = platformRelease;
                        platformExtension.metadata = ext.getMetadata();

                        platformRelease.extensions.add(platformExtension);
                        newExtensionRelease.platforms.add(platformExtension);
                    } else {
                        newExtensionRelease.metadata = ext.getMetadata();
                    }
                    newExtensionRelease.persist();
                    return newExtensionRelease;
                });
    }

    /**
     * TODO: Check if this is still necessary
     */
    private PlatformReleaseCategory createCategory(io.quarkus.registry.catalog.Category cat,
                                                   PlatformRelease platformRelease) {
        // Insert Category if doesn't exist
        Category category =
                Category.findByName(cat.getName())
                        .orElseGet(() -> {
                            Category newCategory = new Category();
                            newCategory.name = cat.getName();
                            newCategory.description = cat.getDescription();
                            newCategory.persistAndFlush();
                            return newCategory;
                        });
        PlatformReleaseCategory entity = new PlatformReleaseCategory();
        entity.category = category;
        entity.platformRelease = platformRelease;
        entity.metadata = cat.getMetadata();
        entity.persist();
        return entity;
    }
}
