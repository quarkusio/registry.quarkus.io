package io.quarkus.registry.app.model;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

@Entity
public class PlatformExtension extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public PlatformRelease platformRelease;

    @NaturalId
    @ManyToOne(optional = false)
    public ExtensionRelease extensionRelease;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = "json")
    public Map<String, Object> metadata;
}
