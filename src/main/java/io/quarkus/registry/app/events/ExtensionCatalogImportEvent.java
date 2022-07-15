package io.quarkus.registry.app.events;

import io.quarkus.registry.catalog.ExtensionCatalog;

public record ExtensionCatalogImportEvent(ExtensionCatalog extensionCatalog, String platformKey, boolean pinned) {

}