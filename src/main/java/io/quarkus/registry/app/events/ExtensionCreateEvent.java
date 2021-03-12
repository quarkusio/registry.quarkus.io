package io.quarkus.registry.app.events;

import io.quarkus.registry.catalog.Extension;

public class ExtensionCreateEvent implements BaseEvent {

    private final Extension extension;

    public ExtensionCreateEvent(Extension extension) {
        this.extension = extension;
    }

    public Extension getExtension() {
        return extension;
    }
}
