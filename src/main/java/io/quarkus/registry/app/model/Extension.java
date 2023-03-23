package io.quarkus.registry.app.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

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

    @Column
    public String description;

    @OneToMany(mappedBy = "extension", orphanRemoval = true)
    @OrderBy("versionSortable DESC")
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
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Extension.class)
                .using("groupId", groupId)
                .using("artifactId", artifactId)
                .loadOptional();
    }

}
