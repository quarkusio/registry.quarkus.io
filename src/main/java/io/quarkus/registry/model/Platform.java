package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class Platform extends BaseEntity {
    public String groupId;
    public String artifactId;

    public JsonNode metadata;

    @OneToMany
    public List<PlatformRelease> releases;

}
