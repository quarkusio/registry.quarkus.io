package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class Extension extends BaseEntity {
    public String groupId;
    public String artifactId;

    public String name;
    public String description;

    public Category category;
    public Platform platform;

    public JsonNode metadata;

    public List<ExtensionRelease> releases;
}
