package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

@Entity
@Table(indexes = { @Index(name = "Extension_NaturalId", columnList = "groupId,artifactId, version", unique = true) })
@NamedQueries({
        @NamedQuery(name = "Extension.findByGAV",
                query = "select e from Extension e where e.groupId = ?1 and e.artifactId = ?2 and e.version = ?3")
})
public class Extension extends BaseEntity {

    @Column(nullable = false)
    public String groupId;

    @Column(nullable = false)
    public String artifactId;

    @Column(nullable = false)
    public String version;

    @Column(nullable = false)
    public String name;

    @Lob
    public String description;

    @ManyToOne
    public CoreRelease builtWith;

    @ManyToMany
    public List<CoreRelease> compatibleReleases;

    @ManyToMany
    public List<Platform> platforms = new ArrayList<>();

    @Column(columnDefinition = "json")
    public JsonNode metadata;

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
                Objects.equals(artifactId, extension.artifactId) &&
                Objects.equals(version, extension.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    public static Uni<Extension> findByGAV(String groupId, String artifactId, String version) {
        return Extension.find("#Extension.findByGAV", groupId, artifactId, version).firstResult();
    }

}
