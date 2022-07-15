package io.quarkus.registry.app.events;

import io.quarkus.registry.catalog.Extension;

public record ExtensionCreateEvent(Extension extension) {
}
