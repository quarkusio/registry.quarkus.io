package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.registry.app.util.Version;

@Entity
@NamedQueries({
        @NamedQuery(name = "PlatformRelease.findByPlatformKey", query = "select pr from PlatformRelease pr where pr.platformStream.platform.platformKey = ?1 and pr.version = ?2"),
        @NamedQuery(name = "PlatformRelease.findQuarkusCores", query = """
                select pr.version from PlatformRelease pr
                where pr.platformStream.platform.isDefault is true
                order by pr.versionSortable
                """),
        @NamedQuery(name = "PlatformRelease.findLatestByQuarkusCoreVersion", query = """
                select pr from PlatformRelease pr
                where pr.quarkusCoreVersion = ?1
                and (pr.platformStream, pr.versionSortable) in (
                    select pr2.platformStream, max(pr2.versionSortable) from PlatformRelease pr2
                        where pr2.quarkusCoreVersion = ?1
                        and pr2.platformStream.unlisted = false
                        group by pr2.platformStream
                )
                order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """),
        @NamedQuery(name = "PlatformRelease.findLatest", query = """
                  select pr from PlatformRelease pr
                  where (pr.platformStream, pr.versionSortable) in
                      (
                       select pr2.platformStream, max(pr2.versionSortable) from PlatformRelease pr2
                           where pr2.platformStream.unlisted is false
                               group by pr2.platformStream
                      )
                order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """)
})
public class PlatformRelease extends BaseEntity {

    @NaturalId
    @ManyToOne
    public PlatformStream platformStream;

    @NaturalId
    @Column(nullable = false, updatable = false)
    public String version;

    /**
     * The version above formatted as a valid semver (for max and order-by operations)
     */
    @Column(updatable = false)
    private String versionSortable;

    @Column(nullable = false)
    public String quarkusCoreVersion;

    public String upstreamQuarkusCoreVersion;

    public boolean pinned;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public List<String> memberBoms = new ArrayList<>();

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platformRelease", orphanRemoval = true)
    public List<PlatformExtension> extensions = new ArrayList<>();

    public PlatformRelease() {
    }

    public PlatformRelease(PlatformStream platformStream, String version, boolean pinned) {
        this.platformStream = platformStream;
        this.version = version;
        this.pinned = pinned;
    }

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
        return Objects.equals(this.platformStream, platform.platformStream) &&
                Objects.equals(version, platform.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformStream, version);
    }

    public static Optional<PlatformRelease> findByNaturalKey(PlatformStream platformStream, String version) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformRelease.class)
                .using("platformStream", platformStream)
                .using("version", version)
                .loadOptional();
    }

    public static List<PlatformRelease> findLatest(String quarkusCore) {
        EntityManager entityManager = getEntityManager();
        TypedQuery<PlatformRelease> query;
        if (quarkusCore != null && !quarkusCore.isBlank()) {
            query = entityManager.createNamedQuery("PlatformRelease.findLatestByQuarkusCoreVersion", PlatformRelease.class)
                    .setParameter(1, quarkusCore);
        } else {
            query = entityManager.createNamedQuery("PlatformRelease.findLatest", PlatformRelease.class);
        }
        // We just want the last 2 releases. See https://github.com/quarkusio/registry.quarkus.io/issues/34
        query.setMaxResults(2);
        return query.getResultList();
    }

    public static Optional<PlatformRelease> findByPlatformKey(String platformKey, String version) {
        return find("#PlatformRelease.findByPlatformKey", platformKey, version).firstResultOptional();
    }

    public static List<String> findQuarkusCores() {
        return getEntityManager().createNamedQuery("PlatformRelease.findQuarkusCores", String.class)
                .getResultList();
    }

}
