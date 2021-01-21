package io.quarkus.registry.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

@Entity
@Table(indexes = { @Index(columnList = "extension_id,version", unique = true) })
public class ExtensionRelease extends BaseEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String version;

    @ManyToOne
    public Extension extension;

    @ManyToOne
    public CoreRelease builtWith;

    @ManyToMany
    public List<CoreRelease> compatibleReleases;

    @ManyToMany
    public List<PlatformRelease> platforms = new ArrayList<>();

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtensionRelease)) {
            return false;
        }
        ExtensionRelease that = (ExtensionRelease) o;
        return Objects.equals(version, that.version) && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, extension);
    }

    public static Uni<ExtensionRelease> findByExtensionAndVersion(Extension extension, String version) {
        return ExtensionRelease.find("extension = ?1 and version =?2", extension, version).firstResult();
    }
}
