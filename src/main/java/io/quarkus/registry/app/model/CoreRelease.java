package io.quarkus.registry.app.model;

import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import io.quarkus.hibernate.reactive.panache.runtime.JpaOperations;
import io.smallrye.mutiny.Multi;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Quarkus core releases
 */
@Entity
@NamedQuery(name = "CoreRelease.findAllVersions", query = "SELECT r.version FROM CoreRelease r ORDER BY r.createdAt DESC")
//@Table(indexes = { @Index(columnList = "version", unique = true) })
public class CoreRelease extends BaseEntity {

    @Id
    @Column(nullable = false)
    public String version;

    @ManyToOne
    public CoreRelease majorRelease;

    @OneToMany(mappedBy = "majorRelease")
    public List<CoreRelease> minorReleases;

    @ManyToMany
    public List<ExtensionRelease> compatibleExtensions;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoreRelease)) {
            return false;
        }
        CoreRelease that = (CoreRelease) o;
        return Objects.equals(version, that.version);
    }

    @Override public String toString() {
        return "CoreRelease{" +
                "version='" + version + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    public boolean isPreRelease() {
        return !version.endsWith("Final");
    }

    public static Multi<String> findAllVersions() {
        try (Mutiny.Session session = JpaOperations.getSession()) {
            return session
                    .createNamedQuery("CoreRelease.findAllVersions", String.class)
                    .getResults();
        }
    }
}
