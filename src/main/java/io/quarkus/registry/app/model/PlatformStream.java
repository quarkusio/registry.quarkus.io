package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import io.quarkus.registry.app.util.Version;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;

@Entity
public class PlatformStream extends BaseEntity {

    @NaturalId
    @ManyToOne
    public Platform platform;

    @NaturalId
    @Column(nullable = false)
    public String streamKey;

    /**
     * The key above formatted as a valid semver (for max and order-by operations)
     */
    @Column(updatable = false)
    private String streamKeySortable;

    @Column
    public String name;

    @Column
    public boolean unlisted;

    @Column
    public boolean pinned;

    @Column
    public boolean lts;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platformStream", orphanRemoval = true)
    @OrderBy("versionSortable DESC")
    public List<PlatformRelease> releases = new ArrayList<>();

    public PlatformStream() {

    }

    public PlatformStream(Platform platform, String streamKey) {
        this.platform = platform;
        this.streamKey = streamKey;
    }

    @PrePersist
    void updateSemVer() {
        this.streamKeySortable = Version.toSortable(streamKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PlatformStream that = (PlatformStream) o;
        return platform.equals(that.platform) && streamKey.equals(that.streamKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, streamKey);
    }

    public static Optional<PlatformStream> findByNaturalKey(Platform platform, String streamKey) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformStream.class)
                .using("platform", platform)
                .using("streamKey", streamKey)
                .loadOptional();
    }
}
