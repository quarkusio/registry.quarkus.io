package io.quarkus.registry.app.events;

import java.util.Objects;

import io.quarkus.registry.app.model.ExtensionRelease;

public record ExtensionCompatibilityCreateEvent(ExtensionRelease extensionRelease, String quarkusCore, boolean compatible) {
    public ExtensionCompatibilityCreateEvent {
        Objects.requireNonNull(extensionRelease, "Extension Release must not be null");
        Objects.requireNonNull(quarkusCore, "Quarkus core must not be null");
    }
}
