package io.quarkus.registry.app.events;

import io.quarkus.registry.catalog.ExtensionCatalog;

public class ExtensionCatalogImportEvent implements BaseEvent {

    private final ExtensionCatalog extensionCatalog;

    public ExtensionCatalogImportEvent(ExtensionCatalog extensionCatalog) {
        this.extensionCatalog = extensionCatalog;
    }

    public ExtensionCatalog getExtensionCatalog() {
        return extensionCatalog;
    }
}
