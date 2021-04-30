package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.ExtensionRelease;

public class ExtensionCompatibleDeleteEvent implements BaseEvent {
    private final ExtensionRelease extensionRelease;

    private final String quarkusCore;

    public ExtensionCompatibleDeleteEvent(ExtensionRelease extensionRelease, String quarkusCore) {
        this.extensionRelease = extensionRelease;
        this.quarkusCore = quarkusCore;
    }

    public ExtensionRelease getExtensionRelease() {
        return extensionRelease;
    }

    public String getQuarkusCore() {
        return quarkusCore;
    }
}
