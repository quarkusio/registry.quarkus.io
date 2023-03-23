package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class PlatformExtension extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public PlatformRelease platformRelease;

    @NaturalId
    @ManyToOne(optional = false)
    public ExtensionRelease extensionRelease;

    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> metadata;

    public static Optional<PlatformExtension> findByNaturalKey(PlatformRelease platformRelease,
            ExtensionRelease extensionRelease) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformExtension.class)
                .using("platformRelease", platformRelease)
                .using("extensionRelease", extensionRelease)
                .loadOptional();
    }

}
