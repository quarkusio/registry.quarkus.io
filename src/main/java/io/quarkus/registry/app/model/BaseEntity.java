package io.quarkus.registry.app.model;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * The base class for all entities
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    //    @Version
    //    @ColumnDefault("0")
    //    public int versionLock;

    @Column(updatable = false, insertable = false)
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    public Instant createdAt;
}
