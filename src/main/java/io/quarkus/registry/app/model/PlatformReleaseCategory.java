package io.quarkus.registry.app.model;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Map;

/**
 * Many-to-Many relationship with {@link PlatformRelease} and {@link Category}
 */
@Entity
public class PlatformReleaseCategory extends BaseEntity {
    @NaturalId
    @ManyToOne(optional = false)
    public PlatformRelease platformRelease;

    @NaturalId
    @ManyToOne(optional = false)
    public Category category;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;
}