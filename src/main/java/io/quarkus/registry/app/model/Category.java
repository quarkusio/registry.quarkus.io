package io.quarkus.registry.app.model;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;

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

    /**
     * @return the category id based on the name
     */
    @Transient
    public String getCategoryId() {
        return toCategoryId(this.name);
    }

    public static String toCategoryId(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT).replace(' ','-');
    }

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
        Session session = getEntityManager().unwrap(Session.class);
        return session.byNaturalId(Category.class)
                .using("name", name)
                .loadOptional();
    }
}
