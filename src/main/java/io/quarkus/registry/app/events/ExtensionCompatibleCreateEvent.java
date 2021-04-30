package io.quarkus.registry.app.events;

import java.util.Objects;

import io.quarkus.registry.app.model.ExtensionRelease;

public class ExtensionCompatibleCreateEvent implements BaseEvent {
    private final ExtensionRelease extensionRelease;

    private final String quarkusCore;

    public ExtensionCompatibleCreateEvent(ExtensionRelease extensionRelease, String quarkusCore) {
        this.extensionRelease = Objects.requireNonNull(extensionRelease, "Extension Release must not be null");
        this.quarkusCore = Objects.requireNonNull(quarkusCore, "Quarkus core must not be null");
    }

    public ExtensionRelease getExtensionRelease() {
        return extensionRelease;
    }

    public String getQuarkusCore() {
        return quarkusCore;
    }
}
