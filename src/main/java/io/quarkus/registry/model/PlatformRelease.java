package io.quarkus.registry.model;

import javax.persistence.Entity;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class PlatformRelease extends BaseEntity {
    public Platform platform;
    public String version;
    public CoreRelease quarkusVersion;

    public JsonNode metadata;
}
