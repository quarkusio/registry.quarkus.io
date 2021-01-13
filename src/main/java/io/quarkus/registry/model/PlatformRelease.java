package io.quarkus.registry.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@IdClass(PlatformRelease.PlatformKey.class)
public class PlatformRelease extends BaseEntity {

    @Id
    @ManyToOne
    public Platform platform;

    @Id
    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @ManyToMany
    public List<ExtensionRelease> extensions;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    public static class PlatformKey implements Serializable {
        public Platform platform;

        public String version;
    }

}
