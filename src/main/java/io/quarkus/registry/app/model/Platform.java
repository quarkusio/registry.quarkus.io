package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortComparator;

@Entity
public class Platform extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String groupId;

    @NaturalId
    @Column(nullable = false)
    public String artifactId;

    @OneToMany(mappedBy = "platform", orphanRemoval = true)
    @OrderBy("semver")
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
        return Objects.equals(groupId, platform.groupId) &&
                Objects.equals(artifactId, platform.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    public static Optional<Platform> findByGA(String groupId, String artifactId) {
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Platform.class)
                .using("groupId", groupId)
                .using("artifactId", artifactId)
                .loadOptional();
    }
}
