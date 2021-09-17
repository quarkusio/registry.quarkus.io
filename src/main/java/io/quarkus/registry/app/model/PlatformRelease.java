package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.registry.app.util.Version;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

@Entity
@NamedQueries({
        @NamedQuery(name = "PlatformRelease.findByPlatformKey", query = "from PlatformRelease pr where pr.platformStream.platform.platformKey = ?1 and pr.version = ?2"),
        @NamedQuery(name = "PlatformRelease.findQuarkusCores", query = "select pr.version from PlatformRelease pr " +
                "where pr.platformStream.platform.isDefault = true order by pr.versionSortable"),
        @NamedQuery(name = "PlatformRelease.findLatestByQuarkusCoreVersion", query = "from PlatformRelease pr " +
                "where pr.quarkusCoreVersion = ?1 " +
                "   and (pr.platformStream, pr.versionSortable) in (" +
                "    select pr2.platformStream, max(pr2.versionSortable) from PlatformRelease pr2" +
                "    where pr2.quarkusCoreVersion = ?1 " +
                "    group by pr2.platformStream" +
                "  ) order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc"),
        @NamedQuery(name = "PlatformRelease.findLatest", query = "from PlatformRelease pr " +
                "where (pr.platformStream, pr.versionSortable) in (" +
                "    select pr2.platformStream, max(pr2.versionSortable) from PlatformRelease pr2" +
                "    group by pr2.platformStream" +
                ") order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc")
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
        if (quarkusCore != null && !quarkusCore.isBlank()) {
            return list("#PlatformRelease.findLatestByQuarkusCoreVersion", quarkusCore);
        } else {
            return list("#PlatformRelease.findLatest");
        }
    }

    public static Optional<PlatformRelease> findByPlatformKey(String platformKey, String version) {
        return find("#PlatformRelease.findByPlatformKey", platformKey, version).firstResultOptional();
    }

    public static List<String> findQuarkusCores() {
        return getEntityManager().createNamedQuery("PlatformRelease.findQuarkusCores", String.class)
                .getResultList();
    }

}
