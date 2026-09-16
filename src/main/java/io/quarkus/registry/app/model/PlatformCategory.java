package io.quarkus.registry.app.model;

import java.util.Map;
import java.util.Objects;

/**
 * A category as declared by a platform release, stored as JSON on {@link PlatformRelease#categories}.
 * <p>
 * This is deliberately not an entity. Categories are not independent things the registry curates; they are part of the
 * extension catalog a platform publishes, and the only place their name, description and metadata ever come from is
 * that catalog. Storing them verbatim against the release that declared them keeps the registry a faithful mirror of
 * what was published, and means a category cannot drift from the platform that defines it.
 * <p>
 * Replaces the former {@code Category} and {@code PlatformReleaseCategory} entities, which held a hardcoded list
 * seeded by Flyway that had drifted from Quarkus' {@code catalog-overrides.json}.
 */
public class PlatformCategory {

    public String id;

    public String name;

    public String description;

    public Map<String, Object> metadata;

    public PlatformCategory() {
    }

    public PlatformCategory(String id, String name, String description, Map<String, Object> metadata) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }

    public static PlatformCategory from(io.quarkus.registry.catalog.Category category) {
        // A catalog may reference a category by id alone, to pin extensions to it for example. Fall back to the id so
        // there is always something displayable.
        String name = category.getName() == null ? category.getId() : category.getName();
        // The description deliberately gets no such fallback. Only a real declaration in a catalog carries one, so a
        // null description is what distinguishes a category someone properly defined from one that is just an id
        // somebody claimed. Do not fill it in with a placeholder; that would throw the distinction away.
        return new PlatformCategory(category.getId(), name, category.getDescription(), category.getMetadata());
    }

    public io.quarkus.registry.catalog.Category toCatalogCategory() {
        return io.quarkus.registry.catalog.Category.builder()
                .setId(id)
                .setName(name)
                .setDescription(description)
                .setMetadata(metadata)
                .build();
    }

    /**
     * Value equality over every field, not just the id. Hibernate dirty-checks a JSON column by comparing the loaded
     * value against a deep copy of it, so an id-only equals would let an edit to a category's metadata be silently
     * discarded at flush time.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlatformCategory that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, metadata);
    }
}
