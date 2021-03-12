package io.quarkus.registry.app.events;

import io.quarkus.registry.catalog.Platform;

public class PlatformCreateEvent implements BaseEvent {

    private final Platform platform;

    public PlatformCreateEvent(Platform platform) {
        this.platform = platform;
    }

    public Platform getPlatform() {
        return platform;
    }
}
