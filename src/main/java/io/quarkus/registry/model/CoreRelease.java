package io.quarkus.registry.model;

import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * Quarkus core releases
 */
@Entity
public class CoreRelease extends BaseEntity {

    @Id
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

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    public boolean isPreRelease() {
        return !version.endsWith("Final");
    }
}
