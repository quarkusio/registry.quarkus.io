package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;

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

    public static Optional<PlatformReleaseCategory> findByNaturalKey(PlatformRelease platformRelease, Category category) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformReleaseCategory.class)
                .using("platformRelease", platformRelease)
                .using("category", category)
                .loadOptional();
    }

}
