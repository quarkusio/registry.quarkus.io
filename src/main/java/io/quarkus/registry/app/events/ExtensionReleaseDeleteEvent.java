package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.ExtensionRelease;

public class ExtensionReleaseDeleteEvent implements BaseEvent {

    private final ExtensionRelease extensionRelease;

    public ExtensionReleaseDeleteEvent(ExtensionRelease extensionRelease) {
        this.extensionRelease = extensionRelease;
    }

    public ExtensionRelease getExtensionRelease() {
        return extensionRelease;
    }
}
