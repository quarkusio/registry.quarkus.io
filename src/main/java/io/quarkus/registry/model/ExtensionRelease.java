package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class ExtensionRelease extends BaseEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @ManyToOne
    public Extension extension;

    @ManyToMany
    public List<PlatformRelease> platforms;

    public JsonNode metadata;
}
