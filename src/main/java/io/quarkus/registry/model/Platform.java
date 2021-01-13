package io.quarkus.registry.model;

import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@IdClass(ArtifactKey.class)
public class Platform extends BaseEntity {
    @Id
    public String groupId;
    @Id
    public String artifactId;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Platform)) {
            return false;
        }
        Platform platform = (Platform) o;
        return Objects.equals(groupId, platform.groupId) && Objects.equals(artifactId, platform.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @OneToMany
    public List<PlatformRelease> releases;
}
