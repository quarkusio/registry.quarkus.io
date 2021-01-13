package io.quarkus.registry.model;

import java.util.List;

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

    public boolean isPreRelease() {
        return !version.endsWith("Final");
    }
}
