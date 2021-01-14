package io.quarkus.registry.app.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(indexes = { @Index(columnList = "platform_id,version", unique = true) })
public class PlatformRelease extends BaseEntity {

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
