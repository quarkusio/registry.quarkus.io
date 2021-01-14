package io.quarkus.registry.app.model;

import java.util.Objects;

import javax.persistence.Entity;

/**
 * Categories an extension belongs to
 */
@Entity
public class Category extends BaseEntity {
    public String name;
    public String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category)) {
            return false;
        }
        Category category = (Category) o;
        return Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
