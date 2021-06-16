package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

@Entity
public class PlatformStream extends BaseEntity {

    @NaturalId
    @ManyToOne
    public Platform platform;

    @NaturalId
    @Column(nullable = false)
    public String streamKey;

    @Column
    public String name;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;

    @OneToMany(mappedBy = "platformStream", orphanRemoval = true)
    @OrderBy("versionSortable DESC")
    public List<PlatformRelease> releases = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformStream that = (PlatformStream) o;
        return platform.equals(that.platform) && streamKey.equals(that.streamKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, streamKey);
    }
}
