package io.quarkus.registry.app.model;

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

@Entity
public class Extension extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String groupId;

    @NaturalId
    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String name;

    @Column(length = 4096)
    public String description;

    @OneToMany(mappedBy = "extension")
    @OrderBy("version")
    public List<ExtensionRelease> releases;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Extension)) {
            return false;
        }
        Extension extension = (Extension) o;
        return Objects.equals(groupId, extension.groupId) &&
                Objects.equals(artifactId, extension.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    public static Optional<Extension> findByGA(String groupId, String artifactId) {
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Extension.class)
                .using("groupId", groupId)
                .using("artifactId", artifactId)
                .loadOptional();
    }

}
