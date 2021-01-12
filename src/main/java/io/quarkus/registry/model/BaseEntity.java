package io.quarkus.registry.model;

import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import org.hibernate.annotations.TypeDef;

@TypeDef(name = "jsonb-node", typeClass = JsonNodeStringType.class, defaultForType = JsonNode.class)
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {
}
