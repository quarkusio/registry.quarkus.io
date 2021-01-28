package io.quarkus.registry.app.model;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.NaturalId;

/**
 * Categories an extension belongs to
 */
@Entity
public class Category extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String name;

    @Lob
    public String description;

    @Column(columnDefinition = "json")
    public JsonNode metadata;

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

    public static Optional<Category> findByName(String name) {
        return Category.find("name = ?1", name).firstResultOptional();
    }
}
