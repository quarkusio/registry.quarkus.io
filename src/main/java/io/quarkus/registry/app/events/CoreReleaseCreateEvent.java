package io.quarkus.registry.app.events;

import java.util.Objects;

public class CoreReleaseCreateEvent implements BaseEvent {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public CoreReleaseCreateEvent(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoreReleaseCreateEvent)) {
            return false;
        }
        CoreReleaseCreateEvent that = (CoreReleaseCreateEvent) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return "CoreReleaseCreateEvent{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

}
