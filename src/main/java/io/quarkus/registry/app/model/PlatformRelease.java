package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.registry.app.util.Semver;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

@Entity
@NamedQuery(name = "PlatformRelease.findByQuarkusCore", query = "from PlatformRelease pr where pr.quarkusCore = :quarkusCore")
public class PlatformRelease extends BaseEntity {

    @NaturalId
    @ManyToOne
    public Platform platform;

    @NaturalId
    @Column(nullable = false, updatable = false)
    public String version;

    /**
     * The version above formatted as a valid semver (for max and order-by operations)
     */
    @Column(updatable = false,  columnDefinition = "semver")
    public String semver;

    @Column(nullable = false)
    public String quarkusCore;

    public String quarkusCoreUpstream;

    @OneToMany(mappedBy = "platformRelease", orphanRemoval = true)
    public List<PlatformExtension> extensions = new ArrayList<>();

    @OneToMany(mappedBy = "platformRelease", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<PlatformReleaseCategory> categories = new ArrayList<>();

    @PrePersist
    void updateSemVer() {
        this.semver = Semver.toSemver(version);
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

    public static List<PlatformRelease> findByQuarkusCore(String quarkusCore) {
        EntityManager em = JpaOperations.getEntityManager();
        return em.createNamedQuery("PlatformRelease.findByQuarkusCore", PlatformRelease.class)
                .setParameter("quarkusCore", quarkusCore)
                .getResultList();
    }
}
