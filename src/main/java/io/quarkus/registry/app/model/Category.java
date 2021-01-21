package io.quarkus.registry.app.model;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import io.smallrye.mutiny.Uni;

/**
 * Categories an extension belongs to
 */
@Entity
@Table(indexes = { @Index(columnList = "name", unique = true) })
public class Category extends BaseEntity {
    @Id
    @GeneratedValue
    public Long id;

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

    public static Uni<Category> findByName(String name) {
        return Category.find("name = ?1", name).firstResult();
    }
}
