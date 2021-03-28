package io.quarkus.registry.app.admin;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.events.ExtensionCatalogImportEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import io.quarkus.registry.catalog.ExtensionCatalog;
import org.jboss.logging.Logger;

/**
 * This class listens for async events fired from {@link AdminResource}
 */
@ApplicationScoped
public class AdminService {

    private static final Logger logger = Logger.getLogger(AdminService.class);

    @Transactional
    public void onExtensionCatalogImport(ExtensionCatalogImportEvent event) {
        try {
            ExtensionCatalog extensionCatalog = event.getExtensionCatalog();
            PlatformRelease platformRelease = insertPlatform(extensionCatalog.getBom(),
                                                             extensionCatalog.getQuarkusCoreVersion(),
                                                             extensionCatalog.getUpstreamQuarkusCoreVersion(),
                                                             extensionCatalog.getMetadata());
            for (io.quarkus.registry.catalog.Extension extension : extensionCatalog.getExtensions()) {
                insertExtensionRelease(extension, platformRelease);
            }
        } catch (Exception e) {
            logger.error("Error while inserting platform", e);
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void onPlatformCreate(PlatformCreateEvent event) {
        try {
            io.quarkus.registry.catalog.Platform platform = event.getPlatform();
            insertPlatform(platform.getBom(), platform.getQuarkusCoreVersion(), platform.getUpstreamQuarkusCoreVersion(), null);
        } catch (Exception e) {
            logger.error("Error while inserting platform", e);
        }
    }

    private PlatformRelease insertPlatform(ArtifactCoords bom, String quarkusCore, String quarkusCoreUpstream, Map<String, Object> metadata) {
        final String groupId = bom.getGroupId();
        final String artifactId = bom.getArtifactId();
        final String version = bom.getVersion();
        final Platform platform = Platform.findByGA(groupId, artifactId)
                .orElseGet(() -> {
                    Platform newPlatform = new Platform();
                    newPlatform.groupId = groupId;
                    newPlatform.artifactId = artifactId;
                    newPlatform.persist();
                    return newPlatform;
                });

        return PlatformRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    PlatformRelease newPlatformRelease = new PlatformRelease();
                    platform.releases.add(newPlatformRelease);
                    newPlatformRelease.platform = platform;
                    newPlatformRelease.version = version;
                    newPlatformRelease.quarkusCore = quarkusCore;
                    newPlatformRelease.quarkusCoreUpstream = quarkusCoreUpstream;
                    newPlatformRelease.metadata = metadata;
                    newPlatformRelease.persist();
                    return newPlatformRelease;
                });
    }

    @Transactional
    public void onExtensionCreate(ExtensionCreateEvent event) {
        // Non-platform extension
        try {
            insertExtensionRelease(event.getExtension(), null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    private ExtensionRelease insertExtensionRelease(io.quarkus.registry.catalog.Extension ext,
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
                    String quarkusCore = (String) ext.getMetadata().get("built-with-quarkus-core");
                    // Some extensions were published using the full GAV
                    if (quarkusCore != null) {
                        try {
                            quarkusCore = ArtifactCoords.fromString(quarkusCore).getVersion();
                        } catch (IllegalArgumentException iae) {
                            // ignore
                        }
                    } else {
                        // Cannot determine Quarkus version
                        quarkusCore = "0.0.0";
                    }
                    newExtensionRelease.quarkusCore = quarkusCore;
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
