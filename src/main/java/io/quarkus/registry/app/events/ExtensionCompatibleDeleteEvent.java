package io.quarkus.registry.app.events;

import io.quarkus.registry.app.model.ExtensionRelease;

public record ExtensionCompatibleDeleteEvent(ExtensionRelease extensionRelease, String quarkusCore) {

}
