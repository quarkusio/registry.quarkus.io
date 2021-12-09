package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.Extension;

public class ExtensionDeleteEvent implements BaseEvent {

    private final Extension extension;

    public ExtensionDeleteEvent(Extension extension) {
        this.extension = extension;
    }

    public Extension getExtension() {
        return extension;
    }
}
