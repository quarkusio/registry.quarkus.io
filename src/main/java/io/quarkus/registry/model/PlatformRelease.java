package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class PlatformRelease extends BaseEntity {

    @ManyToOne
    public Platform platform;

    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @OneToMany
    public List<PlatformExtension> extensions;

    public JsonNode metadata;
}
