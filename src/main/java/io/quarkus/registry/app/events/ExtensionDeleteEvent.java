package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.Extension;

public record ExtensionDeleteEvent(Extension extension) {

}
