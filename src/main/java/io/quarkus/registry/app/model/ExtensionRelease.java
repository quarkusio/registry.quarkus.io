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
@NamedQuery(name = "ExtensionRelease.findNonPlatformExtensions", query = "from ExtensionRelease ext " +
        "where ext.platforms is empty " +
        "and (ext.versionSortable) = (" +
        "    select max(ext2.versionSortable) from ExtensionRelease ext2" +
        "    where ext2.extension = ext.extension" +
        ")")
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
    public String quarkusCore;

    @OneToMany(mappedBy = "extensionRelease", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    public List<PlatformExtension> platforms = new ArrayList<>();

    /**
     * This hook will update the version_sortable field with the version
     */
    @PrePersist
    void updateSemVer() {
        this.versionSortable = Version.toSortable(version);
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
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(ExtensionRelease.class)
                .using("extension", extension.get())
                .using("version", version)
                .loadOptional();
    }

    public static List<ExtensionRelease> findNonPlatformExtensions(String quarkusCore) {
        EntityManager entityManager = JpaOperations.getEntityManager();
        return entityManager.createNamedQuery("ExtensionRelease.findNonPlatformExtensions", ExtensionRelease.class)
                .getResultList();
    }

}
