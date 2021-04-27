package io.quarkus.registry.app.model.compat;

import javax.persistence.Entity;

import io.quarkus.registry.app.model.BaseEntity;
import io.quarkus.registry.app.model.ExtensionRelease;
import org.hibernate.annotations.NaturalId;

//@Entity
public class ExtensionReleaseCompatible extends BaseEntity {
    @NaturalId
    public ExtensionRelease extensionRelease;

    @NaturalId
    public String quarkusCore;
}
