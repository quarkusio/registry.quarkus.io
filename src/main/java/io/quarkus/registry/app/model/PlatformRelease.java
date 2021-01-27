package io.quarkus.registry.app.model;

import java.util.ArrayList;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(indexes = { @Index(name = "PlatformRelease_NaturalId", columnList = "platform_id,version", unique = true) })
@NamedQueries({
        @NamedQuery(name = "PlatformRelease.findByGAV", query = "select p from PlatformRelease p where p.platform.groupId = ?1 and p.platform.artifactId = ?2 and p.version= ?3")
})
public class PlatformRelease extends BaseEntity {

    @ManyToOne
    public Platform platform;

    @Column(nullable = false)
    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @OneToMany(mappedBy = "platformRelease")
    public List<PlatformExtension> extensions = new ArrayList<>();

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatformRelease)) {
            return false;
        }
        PlatformRelease platform = (PlatformRelease) o;
        return Objects.equals(platform, platform.platform) &&
                Objects.equals(version, platform.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, version);
    }

    public static Optional<PlatformRelease> findByGAV(String groupId, String artifactId, String version) {
        return PlatformRelease.find("#PlatformRelease.findByGAV", groupId, artifactId, version).firstResultOptional();
    }
}
