package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SqlResultSetMapping;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.registry.app.util.Version;

@Entity
@NamedNativeQuery(name = "ExtensionRelease.findNonPlatformExtensions", query = """
            SELECT DISTINCT ON(extension_release.extension_id)
                extension_release.id as extension_release_id,
                extension_release.created_at as extension_release_created_at,
                extension_release.extension_id,
                extension_release.metadata,
                extension_release.quarkus_core_version,
                extension_release.quarkus_core_version_sortable,
                extension_release.version,
                extension_release.version_sortable,
                extension.created_at as extension_created_at,
                extension.name,
                extension.description,
                extension.artifact_id,
                extension.group_id
            FROM
                extension_release
                JOIN extension ON extension.id = extension_release.extension_id
                LEFT JOIN platform_extension ON extension_release.id = platform_extension.extension_release_id
                LEFT JOIN extension_release_compatibility ON extension_release_compatibility.extension_release_id = extension_release.id AND :quarkusCore like extension_release_compatibility.quarkus_core_version
            WHERE
                platform_extension.id IS NULL AND (
                    (extension_release.quarkus_core_version_sortable <= :quarkusCoreSortable AND
                       extension_release.quarkus_core_version_sortable < :upperBound AND
                       extension_release.quarkus_core_version_sortable >= :lowerBound)
                    OR extension_release_compatibility.id IS NOT NULL
                )
            ORDER BY extension_release.extension_id, extension_release.version_sortable DESC
        """, resultSetMapping = "ExtensionRelease.ExtensionReleaseRelease.Mapping")
@NamedNativeQuery(name = "ExtensionRelease.findLatestExtensions", query = """
            SELECT DISTINCT ON(extension_release.extension_id)
                extension_release.id as extension_release_id,
                extension_release.created_at as extension_release_created_at,
                extension_release.extension_id,
                extension_release.metadata,
                extension_release.quarkus_core_version,
                extension_release.quarkus_core_version_sortable,
                extension_release.version,
                extension_release.version_sortable,
                extension.created_at as extension_created_at,
                extension.name,
                extension.description,
                extension.artifact_id,
                extension.group_id
            FROM
                extension_release
                JOIN extension ON extension.id = extension_release.extension_id
            ORDER BY extension_release.extension_id, extension_release.version_sortable DESC
        """, resultSetMapping = "ExtensionRelease.ExtensionReleaseRelease.Mapping")
@SqlResultSetMapping(name = "ExtensionRelease.ExtensionReleaseRelease.Mapping", entities = {
        @EntityResult(entityClass = ExtensionRelease.class, fields = {
                @FieldResult(name = "id", column = "extension_release_id"),
                @FieldResult(name = "createdAt", column = "extension_release_created_at"),
                @FieldResult(name = "extension", column = "extension_id"),
                @FieldResult(name = "metadata", column = "metadata"),
                @FieldResult(name = "quarkusCoreVersion", column = "quarkus_core_version"),
                @FieldResult(name = "quarkusCoreVersionSortable", column = "quarkus_core_version_sortable"),
                @FieldResult(name = "version", column = "version"),
                @FieldResult(name = "versionSortable", column = "version_sortable"),
        }),
        @EntityResult(entityClass = Extension.class, fields = {
                @FieldResult(name = "id", column = "extension_id"),
                @FieldResult(name = "createdAt", column = "extension_created_at"),
                @FieldResult(name = "name", column = "name"),
                @FieldResult(name = "description", column = "description"),
                @FieldResult(name = "artifactId", column = "artifact_id"),
                @FieldResult(name = "groupId", column = "group_id")
        })
})
public class ExtensionRelease extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public Extension extension;

    @NaturalId
    @Column(nullable = false, updatable = false)
    public String version;

    /**
     * The version above formatted for max and order-by operations
     */
    @Column(updatable = false, length = 100)
    private String versionSortable;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;

    @Column(nullable = false)
    public String quarkusCoreVersion;

    @Column(nullable = false)
    public String quarkusCoreVersionSortable;

    @OneToMany(mappedBy = "extensionRelease", cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true)
    public List<PlatformExtension> platforms = new ArrayList<>();

    /**
     * This hook will update the version_sortable field with the version
     */
    @PrePersist
    void updateSemVer() {
        this.versionSortable = Version.toSortable(version);
        this.quarkusCoreVersionSortable = Version.toSortable(quarkusCoreVersion);
    }

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
        Optional<Extension> extension = Extension.findByGA(groupId, artifactId);
        if (!extension.isPresent()) {
            return Optional.empty();
        }
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(ExtensionRelease.class)
                .using("extension", extension.get())
                .using("version", version)
                .loadOptional();
    }

    @SuppressWarnings("unchecked")
    public static List<ExtensionRelease> findNonPlatformExtensions(String quarkusCore) {
        DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(quarkusCore);
        String lowerBound = artifactVersion.getMajorVersion() + ".0.0.A";
        String upperBound = (artifactVersion.getMajorVersion() + 1) + ".0.0.A";
        List<Object[]> results = getEntityManager().createNamedQuery("ExtensionRelease.findNonPlatformExtensions")
                .setParameter("lowerBound", Version.toSortable(lowerBound))
                .setParameter("upperBound", Version.toSortable(upperBound))
                .setParameter("quarkusCore", quarkusCore)
                .setParameter("quarkusCoreSortable", Version.toSortable(quarkusCore))
                .getResultList();

        return results.stream().map(r -> (ExtensionRelease) r[0]).toList();
    }

    @SuppressWarnings("unchecked")
    public static List<ExtensionRelease> findLatestExtensions() {
        List<Object[]> results = getEntityManager().createNamedQuery("ExtensionRelease.findLatestExtensions")
                .getResultList();

        return results.stream().map(r -> (ExtensionRelease) r[0]).toList();
    }
}
