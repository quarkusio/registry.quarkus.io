package io.quarkus.registry.model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Quarkus core releases
 */
@Entity
public class CoreRelease extends BaseEntity {
    @Id
    public String version;
    public boolean preRelease;

}
