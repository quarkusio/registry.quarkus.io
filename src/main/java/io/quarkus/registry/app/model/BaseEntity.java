package io.quarkus.registry.app.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.TypeDef;

/**
 * The base class for all entities
 */
@TypeDef(name = "jsonb-node",
        typeClass = JsonNodeStringType.class,
        defaultForType = JsonNode.class)
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    //    @Version
    //    @ColumnDefault("0")
    //    public int versionLock;

    @Column(updatable = false, insertable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    public Date createdAt;
}
