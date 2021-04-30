package io.quarkus.registry.app.model.compat;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.registry.app.model.BaseEntity;
import io.quarkus.registry.app.model.ExtensionRelease;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

@Entity
public class ExtensionReleaseCompatible extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public ExtensionRelease extensionRelease;

    @NaturalId
    public String quarkusCore;

    public static Optional<ExtensionReleaseCompatible> findByNaturalKey(ExtensionRelease extensionRelease, String quarkusCore) {
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(ExtensionReleaseCompatible.class)
                .using("extensionRelease", extensionRelease)
                .using("quarkusCore", quarkusCore)
                .loadOptional();
    }

}
