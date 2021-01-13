package io.quarkus.registry.model;

import java.io.Serializable;
import java.util.Objects;

public class ArtifactKey implements Serializable {
    public String groupId;
    public String artifactId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArtifactKey)) {
            return false;
        }
        ArtifactKey that = (ArtifactKey) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }
}
