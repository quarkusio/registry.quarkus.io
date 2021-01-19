package io.quarkus.registry.app.model;

import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(indexes = { @Index(columnList = "groupId,artifactId", unique = true) })
@NamedQuery(name = "Platform.findByGroupIdAndArtifactId", query = "select p from Platform p where p.groupId = ?1 and p.artifactId = ?2")
public class Platform extends BaseEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String groupId;
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
