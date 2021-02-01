package io.quarkus.registry.app.model;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.registry.app.util.JsonNodes;
import org.hibernate.annotations.NaturalId;

/**
 * Overrides category values for a specific platform
 */
@Entity
public class PlatformReleaseCategory extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public PlatformRelease platformRelease;

    @NaturalId
    @ManyToOne(optional = false)
    public Category category;

    public String name;

    @Column(length = 4096)
    public String description;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

    public String getName() {
        return name == null ? category.name : name;
    }

    public String getDescription() {
        return description == null ? category.description : description;
    }

    public JsonNode getMetadata() {
        JsonNodes jsonNodes = CDI.current().select(JsonNodes.class).get();
        return jsonNodes.merge((ObjectNode) metadata, category.metadata);
    }
}
