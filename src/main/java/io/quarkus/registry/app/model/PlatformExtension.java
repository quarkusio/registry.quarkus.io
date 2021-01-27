package io.quarkus.registry.app.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
public class PlatformExtension {
    @ManyToOne
    public PlatformRelease platformRelease;

    @ManyToOne
    public ExtensionRelease extensionRelease;

    public JsonNode metadata;
}
