package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

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

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> metadata;

    public static Optional<PlatformReleaseCategory> findByNaturalKey(PlatformRelease platformRelease, Category category) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformReleaseCategory.class)
                .using("platformRelease", platformRelease)
                .using("category", category)
                .loadOptional();
    }

}
