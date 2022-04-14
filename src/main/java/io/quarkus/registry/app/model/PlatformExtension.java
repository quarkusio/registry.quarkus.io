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

    public static Optional<PlatformExtension> findByNaturalKey(PlatformRelease platformRelease,
            ExtensionRelease extensionRelease) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(PlatformExtension.class)
                .using("platformRelease", platformRelease)
                .using("extensionRelease", extensionRelease)
                .loadOptional();
    }

}
