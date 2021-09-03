package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class ExtensionCatalogImportEvent implements BaseEvent {

    private Platform platform;

    private final ExtensionCatalog extensionCatalog;

    private final boolean pinned;

    public ExtensionCatalogImportEvent(Platform platform, ExtensionCatalog extensionCatalog, boolean pinned) {
        this.platform = platform;
        this.extensionCatalog = extensionCatalog;
        this.pinned = pinned;
    }

    public Platform getPlatform() {
        return platform;
    }

    public ExtensionCatalog getExtensionCatalog() {
        return extensionCatalog;
    }

    public boolean isPinned() {
        return pinned;
    }
}
