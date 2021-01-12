package io.quarkus.registry.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class ExtensionRelease extends BaseEntity {

    public String version;

    @ManyToOne
    public CoreRelease quarkusVersion;

    @ManyToOne
    public Extension extension;

    @OneToMany
    public List<PlatformExtension> platforms;

    public JsonNode metadata;
}
