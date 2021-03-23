package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;

import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

/**
 * Categories an extension belongs to
 */
@Entity
@Cacheable
public class Category extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String name;

    @Column(length = 4096)
    public String description;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(columnDefinition = JsonTypes.JSON_BIN)
    public Map<String, Object> metadata;

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
        Session session = JpaOperations.getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Category.class)
                .using("name", name)
                .loadOptional();
    }
}
