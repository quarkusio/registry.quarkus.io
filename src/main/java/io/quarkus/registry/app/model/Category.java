package io.quarkus.registry.app.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

/**
 * Categories an extension belongs to
 */
@Entity
@Table(indexes = { @Index(name = "Category_NaturalId", columnList = "name", unique = true) })
public class Category extends BaseEntity {

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

    public static Uni<Category> findByName(String name) {
        return Category.find("name = ?1", name).firstResult();
    }
}
