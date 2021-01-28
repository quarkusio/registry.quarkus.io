package io.quarkus.registry.app.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.databind.JsonNode;
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

    @Lob
    public String description;

    @Column(columnDefinition = "json")
    public JsonNode metadata;
}
