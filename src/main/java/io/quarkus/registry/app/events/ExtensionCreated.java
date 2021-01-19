package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.Extension;

public class ExtensionCreated {
    private final Extension extension;

    public ExtensionCreated(Extension extension) {
        this.extension = extension;
    }
    
    public Extension getExtension() {
        return extension;
    }
}
