package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Tuple;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

@Entity
@NamedQuery(name = "ExtensionReleaseCompatibility.findCompatibility",
        query = "select e.extensionRelease.id as id, e.compatible as compatible from ExtensionReleaseCompatibility e " +
                "where e.quarkusCore = :quarkusCore")
public class ExtensionReleaseCompatibility extends BaseEntity {

    @NaturalId
    @ManyToOne(optional = false)
    public ExtensionRelease extensionRelease;

    @NaturalId
    public String quarkusCore;

    @Column(nullable = false)
    public boolean compatible;

    public static Optional<ExtensionReleaseCompatibility> findByNaturalKey(ExtensionRelease extensionRelease, String quarkusCore) {
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(ExtensionReleaseCompatibility.class)
                .using("extensionRelease", extensionRelease)
                .using("quarkusCore", quarkusCore)
                .loadOptional();
    }

    public static Map<Long, Boolean> findCompatiblity(String quarkusCore) {
        EntityManager entityManager = JpaOperations.getEntityManager();
        return entityManager.createNamedQuery("ExtensionReleaseCompatibility.findCompatibility", Tuple.class)
                .setParameter("quarkusCore", quarkusCore)
                .getResultStream()
                .collect(
                        Collectors.toMap(
                                tuple -> tuple.get("id", Long.class),
                                tuple -> tuple.get("compatible", Boolean.class)
                        ));
    }

}
