package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

@Entity
public class PlatformRelease extends BaseEntity implements Versioned {

    @NaturalId
    @ManyToOne
    public Platform platform;

    @NaturalId
    @Column(nullable = false)
    public String version;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @Column(nullable = false)
    public String quarkusCore;

    public String quarkusCoreUpstream;

    @OneToMany(mappedBy = "platformRelease", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    public List<PlatformExtension> extensions = new ArrayList<>();

    @OneToMany(mappedBy = "platformRelease", cascade = CascadeType.ALL)
    public List<PlatformReleaseCategory> categories = new ArrayList<>();

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatformRelease)) {
            return false;
        }
        PlatformRelease platform = (PlatformRelease) o;
        return Objects.equals(this.platform, platform.platform) &&
                Objects.equals(version, platform.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, version);
    }

    public static Optional<PlatformRelease> findByGAV(String groupId, String artifactId, String version) {
        Optional<Platform> p = Platform.findByGA(groupId, artifactId);
        if (!p.isPresent()) {
            return Optional.empty();
        }
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformRelease.class)
                .using("platform", p.get())
                .using("version", version)
                .loadOptional();
    }
}
