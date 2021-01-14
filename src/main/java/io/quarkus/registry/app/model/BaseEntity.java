package io.quarkus.registry.app.model;

import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.TypeDef;

/**
 * The base class for all entities
 */
@TypeDef(name = "jsonb-node",
        typeClass = JsonNodeStringType.class,
        defaultForType = JsonNode.class)
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    @Version
    @ColumnDefault("0")
    public int versionLock;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Generated(value = GenerationTime.INSERT)
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    public Date createdAt;
}
