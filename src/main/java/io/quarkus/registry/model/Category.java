package io.quarkus.registry.model;

import javax.persistence.Entity;

@Entity
public class Category extends BaseEntity {
    public String name;
    public String description;
}
