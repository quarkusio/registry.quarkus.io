package io.quarkus.registry.app.events;

import java.util.Objects;

import io.quarkus.registry.app.model.ExtensionRelease;

public class ExtensionCompatibilityCreateEvent implements BaseEvent {
    private final ExtensionRelease extensionRelease;

    private final String quarkusCore;

    private final boolean compatible;

    public ExtensionCompatibilityCreateEvent(ExtensionRelease extensionRelease, String quarkusCore, boolean compatible) {
        this.extensionRelease = Objects.requireNonNull(extensionRelease, "Extension Release must not be null");
        this.quarkusCore = Objects.requireNonNull(quarkusCore, "Quarkus core must not be null");
        this.compatible = compatible;
    }

    public ExtensionRelease getExtensionRelease() {
        return extensionRelease;
    }

    public String getQuarkusCore() {
        return quarkusCore;
    }

    public boolean isCompatible() {
        return compatible;
    }
}
