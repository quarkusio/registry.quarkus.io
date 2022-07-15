package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.PlatformRelease;

public record ExtensionCatalogDeleteEvent(PlatformRelease platformRelease) {
}