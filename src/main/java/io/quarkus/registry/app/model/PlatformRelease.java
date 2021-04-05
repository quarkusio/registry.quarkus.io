package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.registry.app.util.Version;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

@Entity
@NamedQueries({
        @NamedQuery(name = "PlatformRelease.findQuarkusCores", query = "select pr.version from PlatformRelease pr " +
                "where pr.platform.isDefault = true order by pr.versionSortable"),
        @NamedQuery(name = "PlatformRelease.findByQuarkusCore", query = "from PlatformRelease pr where pr.quarkusCore = ?1"),
        @NamedQuery(name = "PlatformRelease.findLatest", query = "from PlatformRelease pr " +
                "where (pr.platform, pr.versionSortable) in (" +
                "    select pr2.platform, max(pr2.versionSortable) from PlatformRelease pr2" +
                "    group by pr2.platform" +
                ")")
})
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
    @Column(updatable = false, length = 100)
    private String versionSortable;

    @Column(nullable = false)
    public String quarkusCore;

    public String quarkusCoreUpstream;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platformRelease", orphanRemoval = true)
    public List<PlatformExtension> extensions = new ArrayList<>();

    @OneToMany(mappedBy = "platformRelease", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<PlatformReleaseCategory> categories = new ArrayList<>();

    @PrePersist
    void updateSemVer() {
        this.versionSortable = Version.toSortable(version);
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
        return list("#PlatformRelease.findByQuarkusCore", quarkusCore);
    }

    public static List<PlatformRelease> findLatest() {
        return list("#PlatformRelease.findLatest");
    }

    public static List<String> findQuarkusCores() {
        EntityManager em = JpaOperations.getEntityManager();
        return em.createNamedQuery("PlatformRelease.findQuarkusCores", String.class)
                .getResultList();
    }

}
