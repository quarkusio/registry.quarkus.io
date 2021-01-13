package io.quarkus.registry.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Category extends BaseEntity {
    @Id
    public String name;
    public String description;
}
