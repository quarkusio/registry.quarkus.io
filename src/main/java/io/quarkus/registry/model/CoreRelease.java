package io.quarkus.registry.model;

import javax.persistence.Entity;

/**
 * Quarkus core releases
 */
@Entity
public class CoreRelease extends BaseEntity {
    public String version;
}
