package io.quarkus.registry.model;

import java.util.Date;

import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TypeDef;

@TypeDef(name = "jsonb-node", typeClass = JsonNodeStringType.class, defaultForType = JsonNode.class)
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @CreationTimestamp
    public Date createdAt;
}
