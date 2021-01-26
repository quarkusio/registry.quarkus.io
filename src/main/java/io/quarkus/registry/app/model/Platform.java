package io.quarkus.registry.app.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(indexes = { @Index(name = "Platform_NaturalId", columnList = "groupId,artifactId,version", unique = true) })
@NamedQueries({
        @NamedQuery(name = "Platform.findByGAV", query = "select p from Platform p where p.groupId = ?1 and p.artifactId = ?2 and p.version= ?3")
})
public class Platform extends BaseEntity {

    @Column(nullable = false)
    public String groupId;

    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @ManyToMany
    @JsonIgnore
    public List<Extension> extensions;

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
        return Objects.equals(groupId, platform.groupId) &&
                Objects.equals(artifactId, platform.artifactId) &&
                Objects.equals(version, platform.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    public static Optional<Platform> findByGAV(String groupId, String artifactId, String version) {
        return Platform.find("#Platform.findByGAV", groupId, artifactId, version).firstResultOptional();
    }
}
