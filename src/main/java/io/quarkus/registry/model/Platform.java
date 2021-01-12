package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class Platform extends BaseEntity {
    public String groupId;
    public String artifactId;

    public JsonNode metadata;
    public List<ExtensionRelease> extensions;
    public List<PlatformRelease> releases;

}
