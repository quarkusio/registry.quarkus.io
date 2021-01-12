package io.quarkus.registry.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class PlatformExtension extends BaseEntity {

    @ManyToOne
    public ExtensionRelease extension;

    @ManyToOne
    public PlatformRelease platform;
}
