package io.quarkus.registry.model;

import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {
}
