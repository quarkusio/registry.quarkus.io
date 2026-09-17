package io.quarkus.registry.app.admin;

import static io.quarkus.registry.catalog.Extension.MD_BUILT_WITH_QUARKUS_CORE;

import java.util.List;
import java.util.Map;

import io.quarkus.logging.Log;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.events.ExtensionCatalogDeleteEvent;
import io.quarkus.registry.app.events.ExtensionCatalogImportEvent;
import io.quarkus.registry.app.events.ExtensionCompatibilityCreateEvent;
import io.quarkus.registry.app.events.ExtensionCompatibleDeleteEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.ExtensionDeleteEvent;
import io.quarkus.registry.app.events.ExtensionReleaseDeleteEvent;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.ExtensionReleaseCompatibility;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.util.PlatformArtifacts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Administrative operations on the database
 */
@ApplicationScoped
public class AdminService {

    @Transactional
    public void onExtensionCatalogImport(ExtensionCatalogImportEvent event) {
        try {
            ExtensionCatalog extensionCatalog = event.extensionCatalog();
            Platform platform = Platform.findByKey(event.platformKey()).orElseGet(() -> {
                Platform p = new Platform();
                p.platformKey = event.platformKey();
                ArtifactCoords catalogId = ArtifactCoords.fromString(extensionCatalog.getId());
                p.name = Platform.toPlatformName(event.platformKey());
                p.groupId = catalogId.getGroupId();
                p.artifactId = catalogId.getArtifactId();
                p.platformType = event.type();
                p.persist();
                return p;
            });
            PlatformRelease platformRelease = insertPlatformRelease(platform, extensionCatalog, event.pinned());
            for (io.quarkus.registry.catalog.Extension extension : extensionCatalog.getExtensions()) {
                insertExtensionRelease(extension, platformRelease);
            }
            //Add Categories
            for (io.quarkus.registry.catalog.Category category : extensionCatalog.getCategories()) {
                Category c = findOrCreateCategory(category);
                // The link table is what allows categories to be queried per platform release,
                // rather than only as the global Category.listAll() set
                if (PlatformReleaseCategory.findByNaturalKey(platformRelease, c).isEmpty()) {
                    PlatformReleaseCategory prc = new PlatformReleaseCategory();
                    prc.platformRelease = platformRelease;
                    prc.category = c;
                    prc.metadata = category.getMetadata();
                    platformRelease.categories.add(prc);
                    prc.persist();
                }
            }
            DbState.updateUpdatedAt();
        } catch (Exception e) {
            Log.error("Error while inserting platform", e);
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private PlatformRelease insertPlatformRelease(Platform platform, ExtensionCatalog extensionCatalog, boolean pinned) {
        Map<String, Object> platformReleaseMetadata = (Map<String, Object>) extensionCatalog.getMetadata()
                .get("platform-release");
        String streamKey = (String) platformReleaseMetadata.get("stream");
        String version = (String) platformReleaseMetadata.get("version");
        List<String> memberBoms = (List<String>) platformReleaseMetadata.get("members");
        PlatformStream platformStream = PlatformStream.findByNaturalKey(platform, streamKey).orElseGet(() -> {
            PlatformStream stream = new PlatformStream(platform, streamKey);
            stream.persist();
            return stream;
        });
        PlatformRelease platformRelease = PlatformRelease.findByNaturalKey(platformStream, version)
                .orElseGet(() -> {
                    PlatformRelease newPlatformRelease = new PlatformRelease(platformStream, version, pinned);
                    newPlatformRelease.bom = extensionCatalog.getBom().toString();
                    newPlatformRelease.quarkusCoreVersion = extensionCatalog.getQuarkusCoreVersion();
                    newPlatformRelease.upstreamQuarkusCoreVersion = extensionCatalog.getUpstreamQuarkusCoreVersion();
                    newPlatformRelease.memberBoms.addAll(memberBoms.stream().map(ArtifactCoords::fromString)
                            .map(PlatformArtifacts::ensureBomArtifact)
                            .map(ArtifactCoords::toString).toList());
                    newPlatformRelease.metadata = extensionCatalog.getMetadata();
                    return newPlatformRelease;
                });
        platformRelease.pinned = pinned;
        platformRelease.persistAndFlush();
        return platformRelease;
    }

    /**
     * Returns the {@link Category} for the given catalog category, creating it if the registry has never seen it before.
     * <p>
     * This makes the imported platform descriptors (ultimately backed by Quarkus' {@code catalog-overrides.json}) the
     * source of truth for the category list, instead of the hardcoded seed in {@code V2__Add_categories.sql}. Only
     * platform catalogs reach this method today.
     *
     * If we later want non-platform extensions to contribute categories as
     * well, call this from {@link #insertExtensionRelease} with the categories declared in the extension metadata.
     */
    private Category findOrCreateCategory(io.quarkus.registry.catalog.Category category) {
        Category c = Category.findByKey(category.getId()).orElseGet(() -> {
            Category newCategory = new Category();
            newCategory.categoryKey = category.getId();
            // Fall back to the key: a catalog may reference a category by id alone, and name is not nullable
            // TODO we may want to add some prettifying logic if we set the name to the id
            newCategory.name = category.getId();
            newCategory.persist();
            return newCategory;
        });
        // Name and description might have changed. A catalog referencing a category without redefining it (to pin
        // extensions to it, for example) must not wipe what a previous catalog told us about it.
        if (category.getName() != null) {
            c.name = category.getName();
        }
        if (category.getDescription() != null) {
            c.description = category.getDescription();
        }
        c.persist();
        return c;
    }

    @Transactional
    public void onExtensionCreate(ExtensionCreateEvent event) {
        // Non-platform extension
        try {
            insertExtensionRelease(event.extension(), null);
            DbState.updateUpdatedAt();
        } catch (Exception e) {
            Log.error("Error while inserting extension", e);
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void onExtensionDelete(ExtensionDeleteEvent event) {
        // Non-platform extension
        try {
            Extension extension = event.extension();
            // Reattach extension
            extension = Extension.getEntityManager().merge(extension);
            extension.delete();
            DbState.updateUpdatedAt();
        } catch (Exception e) {
            Log.error("Error while deleting extension", e);
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void onExtensionCatalogDelete(ExtensionCatalogDeleteEvent event) {
        try {
            PlatformRelease platformRelease = event.platformRelease();
            // Reattach platform release
            platformRelease = PlatformRelease.getEntityManager().merge(platformRelease);
            platformRelease.delete();
            DbState.updateUpdatedAt();
        } catch (Exception e) {
            Log.error("Error while deleting extension catalog", e);
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void onExtensionReleaseDelete(ExtensionReleaseDeleteEvent event) {
        // Non-platform extension
        try {
            ExtensionRelease extensionRelease = event.extensionRelease();
            // Reattach extension
            extensionRelease = ExtensionRelease.getEntityManager().merge(extensionRelease);
            extensionRelease.delete();
            DbState.updateUpdatedAt();
        } catch (Exception e) {
            Log.error("Error while deleting extension", e);
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

        // Name and description might have changed
        extension.name = ext.getName();
        extension.description = ext.getDescription();
        extension.persist();

        final ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseGet(() -> {
                    ExtensionRelease newExtensionRelease = new ExtensionRelease();
                    newExtensionRelease.version = version;
                    newExtensionRelease.extension = extension;
                    newExtensionRelease.quarkusCoreVersion = quarkusCoreVersionOf(ext);
                    newExtensionRelease.persist();
                    return newExtensionRelease;
                });

        if (platformRelease == null) {
            extensionRelease.metadata = ext.getMetadata();
            extensionRelease.persist();
        } else {
            // Many-to-many. An extension version is sometimes carried over unchanged into several platform releases, so
            // the link has to be recorded for each of them, and the metadata refreshed: the catalog being imported is
            // the most recent word on the extension, and it may well correct what an earlier release said about it
            // (a category, for example).
            PlatformExtension platformExtension = PlatformExtension.findByNaturalKey(platformRelease, extensionRelease)
                    .orElseGet(() -> {
                        PlatformExtension newPlatformExtension = new PlatformExtension();
                        newPlatformExtension.extensionRelease = extensionRelease;
                        newPlatformExtension.platformRelease = platformRelease;

                        platformRelease.extensions.add(newPlatformExtension);
                        extensionRelease.platforms.add(newPlatformExtension);
                        return newPlatformExtension;
                    });
            platformExtension.metadata = ext.getMetadata();
            platformExtension.persist();
        }
        return extensionRelease;
    }

    /**
     * Returns the Quarkus core version the extension was built with, or {@code 0.0.0} if the catalog does not say.
     */
    private static String quarkusCoreVersionOf(io.quarkus.registry.catalog.Extension ext) {
        String quarkusCore = (String) ext.getMetadata().get(MD_BUILT_WITH_QUARKUS_CORE);
        if (quarkusCore == null) {
            // Cannot determine Quarkus version
            return "0.0.0";
        }
        // Some extensions were published using the full GAV
        if (quarkusCore.contains(":")) {
            try {
                return ArtifactCoords.fromString(quarkusCore).getVersion();
            } catch (IllegalArgumentException iae) {
                // ignore
            }
        }
        return quarkusCore;
    }

    @Transactional
    public void onExtensionCompatibilityCreate(ExtensionCompatibilityCreateEvent event) {
        ExtensionReleaseCompatibility extensionReleaseCompatibility = ExtensionReleaseCompatibility.findByNaturalKey(
                event.extensionRelease(), event.quarkusCore()).orElseGet(() -> {
                    ExtensionReleaseCompatibility newEntity = new ExtensionReleaseCompatibility();
                    newEntity.extensionRelease = event.extensionRelease();
                    newEntity.quarkusCoreVersion = event.quarkusCore();
                    return newEntity;
                });
        extensionReleaseCompatibility.compatible = event.compatible();
        extensionReleaseCompatibility.persistAndFlush();
        DbState.updateUpdatedAt();
    }

    @Transactional
    public void onExtensionCompatibilityDelete(ExtensionCompatibleDeleteEvent event) {
        ExtensionReleaseCompatibility.delete(
                "from ExtensionReleaseCompatible rc where rc.extensionRelease = ?1 and rc.quarkusCore = ?2",
                event.extensionRelease(),
                event.quarkusCore());
        DbState.updateUpdatedAt();
    }
}
