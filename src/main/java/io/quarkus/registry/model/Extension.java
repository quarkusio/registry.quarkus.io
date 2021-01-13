package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@IdClass(ArtifactKey.class)
public class Extension extends BaseEntity {
    @Id
    public String groupId;
    @Id
    public String artifactId;

    public String name;
    public String description;

    @ManyToOne
    public Category category;

    public JsonNode metadata;

    @OneToMany
    public List<ExtensionRelease> releases;
}
