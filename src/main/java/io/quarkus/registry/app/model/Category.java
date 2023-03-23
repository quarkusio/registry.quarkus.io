package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Categories an extension belongs to
 */
@Entity
@Cacheable
public class Category extends BaseEntity {

    @NaturalId
    @Column(nullable = false)
    public String categoryKey;

    @Column(nullable = false)
    public String name;

    @Column(length = 4096)
    public String description;

    @JdbcTypeCode(SqlTypes.JSON)
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

    public static Optional<Category> findByKey(String key) {
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Category.class)
                .using("categoryKey", key)
                .loadOptional();
    }
}
