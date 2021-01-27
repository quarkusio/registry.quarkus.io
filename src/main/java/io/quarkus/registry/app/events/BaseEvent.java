package io.quarkus.registry.app.events;

public interface BaseEvent {

    String getGroupId();
    String getArtifactId();
    String getVersion();
}
