package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@NamedQueries({
        @NamedQuery(name = "ExtensionRelease.findByGAV",
                query = "select e from ExtensionRelease e where e.extension.groupId = ?1 and e.extension.artifactId = ?2 and e.version = ?3")
})
public class ExtensionRelease extends BaseEntity {

    @ManyToOne
    public Extension extension;

    @Column(nullable = false)
    public String version;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @ManyToMany
    public List<CoreRelease> compatibleReleases;

    @ManyToMany
    public List<PlatformRelease> platforms = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtensionRelease)) {
            return false;
        }
        ExtensionRelease that = (ExtensionRelease) o;
        return Objects.equals(extension, that.extension) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extension, version);
    }

    public static Optional<ExtensionRelease> findByGAV(String groupId, String artifactId, String version) {
        return ExtensionRelease.find("#ExtensionRelease.findByGAV", groupId, artifactId, version).firstResultOptional();
    }

}
