package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.ExtensionRelease;

public record ExtensionReleaseDeleteEvent(ExtensionRelease extensionRelease) {

}
