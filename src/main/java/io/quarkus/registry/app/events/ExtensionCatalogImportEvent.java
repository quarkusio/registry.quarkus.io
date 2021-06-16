package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class ExtensionCatalogImportEvent implements BaseEvent {

    private Platform platform;

    private final ExtensionCatalog extensionCatalog;

    public ExtensionCatalogImportEvent(Platform platform, ExtensionCatalog extensionCatalog) {
        this.platform = platform;
        this.extensionCatalog = extensionCatalog;
    }

    public Platform getPlatform() {
        return platform;
    }

    public ExtensionCatalog getExtensionCatalog() {
        return extensionCatalog;
    }

}
