package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.smallrye.mutiny.Uni;

@Entity
@Table(indexes = { @Index(columnList = "groupId,artifactId", unique = true) })
@NamedQueries({
        @NamedQuery(name = "Platform.findByGroupIdAndArtifactId", query = "select p from Platform p where p.groupId = ?1 and p.artifactId = ?2")
})
public class Platform extends BaseEntity {

    public String groupId;
    public String artifactId;

    @OneToMany(cascade = CascadeType.PERSIST)
    public List<PlatformRelease> releases = new ArrayList<>();

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

    public static Uni<Platform> findByGroupIdAndArtifactId(String groupId, String artifactId) {
        return Platform.find("#Platform.findByGroupIdAndArtifactId", groupId, artifactId).firstResult();
    }
}
