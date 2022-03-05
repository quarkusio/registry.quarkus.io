package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.PlatformRelease;

public class ExtensionCatalogDeleteEvent implements BaseEvent {

    private final PlatformRelease platformRelease;

    public ExtensionCatalogDeleteEvent(PlatformRelease platformRelease) {
        this.platformRelease = platformRelease;
    }

    public PlatformRelease getPlatformRelease() {
        return platformRelease;
    }
}
