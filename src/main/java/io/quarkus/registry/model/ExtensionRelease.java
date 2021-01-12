package io.quarkus.registry.model;

import javax.persistence.Entity;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class ExtensionRelease extends BaseEntity {
    public Extension extension;
    public String version;
    public CoreRelease quarkusVersion;

    public JsonNode metadata;
}
