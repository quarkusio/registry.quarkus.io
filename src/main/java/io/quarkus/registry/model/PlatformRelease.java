package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class PlatformRelease extends BaseEntity {
    @Id
    @GeneratedValue
    public Long id;

    @ManyToOne
    public Platform platform;

    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @ManyToMany
    public List<ExtensionRelease> extensions;

    @Column(columnDefinition = "json")
    public JsonNode metadata;
}
