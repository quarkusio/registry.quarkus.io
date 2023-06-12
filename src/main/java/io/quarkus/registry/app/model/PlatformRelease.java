package io.quarkus.registry.app.model;

import static io.quarkus.panache.common.Parameters.with;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.util.Version;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.TypedQuery;

@Entity
@NamedQueries({
        @NamedQuery(name = "PlatformRelease.findAllCorePlatforms", query = """
                select pr from PlatformRelease pr
                where pr.platformStream.platform.platformType = 'C'
                order by pr.versionSortable desc
                """),

        @NamedQuery(name = "PlatformRelease.findByPlatformKey", query = """
                select pr from PlatformRelease pr
                where pr.platformStream.platform.platformKey = ?1 and pr.version = ?2
                """),
        @NamedQuery(name = "PlatformRelease.findQuarkusCores", query = """
                select pr.version from PlatformRelease pr
                where pr.platformStream.platform.isDefault = true
                order by pr.versionSortable
                """),
        @NamedQuery(name = "PlatformRelease.findLatestByQuarkusCoreVersion", query = """
                select pr from PlatformRelease pr
                where pr.quarkusCoreVersion = ?1
                and pr.unlisted = false
                and pr.platformStream.platform.platformType = 'C'
                and (pr.platformStream.id, pr.versionSortable) in (
                    select pr2.platformStream.id, max(pr2.versionSortable) from PlatformRelease pr2
                        where pr2.quarkusCoreVersion = ?1
                        and pr2.platformStream.unlisted = false
                        and pr2.unlisted = false
                        group by pr2.platformStream.id
                )
                order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """),
        @NamedQuery(name = "PlatformRelease.findLatest", query = """
                  select pr from PlatformRelease pr
                  where pr.unlisted = false
                  and pr.platformStream.platform.platformType = 'C'
                  and (pr.platformStream.id, pr.versionSortable) in
                      (
                       select pr2.platformStream.id, max(pr2.versionSortable) from PlatformRelease pr2
                           where pr2.platformStream.unlisted = false
                           and pr2.unlisted = false
                           group by pr2.platformStream.id
                      )
                order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """),
        @NamedQuery(name = "PlatformRelease.findPinned", query = """
                  select pr from PlatformRelease pr
                  where pr.unlisted = false
                  and pr.pinned = true
                  and pr.platformStream.platform.platformType = 'C'
                  order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """),
        @NamedQuery(name = "PlatformRelease.findLatestPinnedStream", query = """
                  select pr from PlatformRelease pr
                  where pr.unlisted = false
                  and pr.platformStream.platform.platformType = 'C'
                  and (pr.platformStream.id, pr.versionSortable) in
                      (
                       select pr2.platformStream.id, max(pr2.versionSortable) from PlatformRelease pr2
                           where pr2.platformStream.unlisted = false
                           and pr2.platformStream.pinned = true
                           and pr2.unlisted = false
                           group by pr2.platformStream.id
                      )
                order by pr.versionSortable desc, pr.platformStream.platform.isDefault desc
                """),
        @NamedQuery(name = "PlatformRelease.findByArtifactCoordinates", query = """
                select pr from PlatformRelease pr
                where pr.platformStream.platform.groupId = :groupId
                and pr.platformStream.platform.artifactId = :artifactId
                and pr.version = :version
                and pr.unlisted = false
                """),
        @NamedQuery(name = "PlatformRelease.countArtifactCoordinates", query = """
                select count(pr) from PlatformRelease pr
                where pr.platformStream.platform.groupId = :groupId
                and pr.platformStream.platform.artifactId = :artifactId
                and pr.version = :version
                and pr.unlisted = false
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

    public boolean unlisted;

    @Column(nullable = false)
    public String bom;

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> memberBoms = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platformRelease", orphanRemoval = true)
    public List<PlatformExtension> extensions = new ArrayList<>();

    @OneToMany(mappedBy = "platformRelease", orphanRemoval = true)
    public List<PlatformReleaseCategory> categories = new ArrayList<>();

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
        if (!(o instanceof PlatformRelease platform)) {
            return false;
        }
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
        EntityManager em = getEntityManager();
        TypedQuery<PlatformRelease> query;
        boolean withQuarkusCore = quarkusCore != null && !quarkusCore.isBlank();
        if (withQuarkusCore) {
            query = em.createNamedQuery("PlatformRelease.findLatestByQuarkusCoreVersion", PlatformRelease.class)
                    .setParameter(1, quarkusCore);
        } else {
            query = em.createNamedQuery("PlatformRelease.findLatest", PlatformRelease.class);
        }
        // Check if we can perform this directly in the SQL
        // We need the top 3 releases for each platform
        var platformMap = new LinkedHashMap<Platform, List<PlatformRelease>>();
        for (var item : query.getResultList()) {
            var platformReleases = platformMap.computeIfAbsent(item.platformStream.platform,
                    k -> new ArrayList<>());
            if (platformReleases.size() < 3) {
                // Count stable versions
                long stableVersions = platformReleases.stream()
                        .filter(pr -> !Version.isPreFinal(pr.version))
                        .count();
                if (stableVersions < 2) {
                    platformReleases.add(item);
                }
            }
        }
        // Add pinned platforms to the result
        if (!withQuarkusCore) {
            var pinnedPlatforms = em.createNamedQuery("PlatformRelease.findPinned", PlatformRelease.class)
                    .getResultList();
            for (var item : pinnedPlatforms) {
                platformMap.computeIfAbsent(item.platformStream.platform, k -> new ArrayList<>()).add(item);
            }
            // Add latest platforms from pinned streams
            var pinnedStreams = em.createNamedQuery("PlatformRelease.findLatestPinnedStream", PlatformRelease.class)
                    .getResultList();
            for (var item : pinnedStreams) {
                platformMap.computeIfAbsent(item.platformStream.platform, k -> new ArrayList<>()).add(item);
            }
        }
        return platformMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static Optional<PlatformRelease> findByPlatformKey(String platformKey, String version) {
        return find("#PlatformRelease.findByPlatformKey", platformKey, version).firstResultOptional();
    }

    public static List<String> findQuarkusCores() {
        return getEntityManager().createNamedQuery("PlatformRelease.findQuarkusCores", String.class)
                .getResultList();
    }

    public static boolean artifactCoordinatesExist(ArtifactCoords artifact) {
        return count("#PlatformRelease.countArtifactCoordinates",
                with("groupId", artifact.getGroupId())
                        .and("artifactId", artifact.getArtifactId())
                        .and("version", artifact.getClassifier())) == 1;
    }

    public static Optional<PlatformRelease> findByArtifactCoordinates(ArtifactCoords artifact) {
        return find("#PlatformRelease.findByArtifactCoordinates",
                with("groupId", artifact.getGroupId())
                        .and("artifactId", artifact.getArtifactId())
                        .and("version", artifact.getClassifier()))
                .firstResultOptional();
    }

    public static List<PlatformRelease> findAllCorePlatforms() {
        return list("#PlatformRelease.findAllCorePlatforms");
    }
}
