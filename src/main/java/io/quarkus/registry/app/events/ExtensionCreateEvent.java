package io.quarkus.registry.app.events;

import java.util.Objects;

public class ExtensionCreateEvent implements BaseEvent {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public ExtensionCreateEvent(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtensionCreateEvent)) {
            return false;
        }
        ExtensionCreateEvent that = (ExtensionCreateEvent) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return "ExtensionCreateEvent{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
