package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.registry.app.util.Version;

@Entity
@NamedQuery(name = "ExtensionRelease.findNonPlatformExtensions", query = """
            select ext from ExtensionRelease ext
            where ext.platforms is empty
            and (ext.versionSortable) = (
                select max(ext2.versionSortable) from ExtensionRelease ext2
                where ext2.extension = ext.extension
                and (
                       (ext2.quarkusCoreVersionSortable <= :quarkusCoreSortable
                        and ext2.quarkusCoreVersionSortable < :maximumVersion
                        and ext2.quarkusCoreVersionSortable >= :minimumVersion)
                    or ((select erc.compatible
                         from ExtensionReleaseCompatibility erc
                           where erc.extensionRelease = ext2 and erc.quarkusCoreVersion = :quarkusCore) is true)
                    )
            )
        """)
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

    public static List<ExtensionRelease> findNonPlatformExtensions(String quarkusCore) {
        DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(quarkusCore);
        String minimumVersion = artifactVersion.getMajorVersion() + ".0.0.Final";
        String maximumVersion = (artifactVersion.getMajorVersion() + 1) + ".0.0.Final";
        return getEntityManager().createNamedQuery("ExtensionRelease.findNonPlatformExtensions", ExtensionRelease.class)
                .setParameter("minimumVersion", Version.toSortable(minimumVersion))
                .setParameter("maximumVersion", Version.toSortable(maximumVersion))
                .setParameter("quarkusCore", quarkusCore)
                .setParameter("quarkusCoreSortable", Version.toSortable(quarkusCore))
                .getResultList();
    }

}
