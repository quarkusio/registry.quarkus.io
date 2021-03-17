package io.quarkus.registry.app.model;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

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

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;

    public String getName() {
        return name == null ? category.name : name;
    }

    public String getDescription() {
        return description == null ? category.description : description;
    }

    public Map<String, Object> getMetadata() {
        return metadata == null ? category.metadata : metadata;
    }
}
