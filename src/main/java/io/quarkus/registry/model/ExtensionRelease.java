package io.quarkus.registry.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@IdClass(ExtensionRelease.ExtensionKey.class)
public class ExtensionRelease extends BaseEntity {
    @Id
    public String version;

    @Id
    @ManyToOne
    public Extension extension;

    @ManyToOne
    public CoreRelease builtWith;

    @ManyToMany
    public List<CoreRelease> compatibleReleases;

    @ManyToMany
    public List<PlatformRelease> platforms;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    public static class ExtensionKey implements Serializable {
        public Extension extension;
        public String version;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ExtensionKey)) {
                return false;
            }
            ExtensionKey that = (ExtensionKey) o;
            return Objects.equals(extension, that.extension) && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(extension, version);
        }
    }
}
