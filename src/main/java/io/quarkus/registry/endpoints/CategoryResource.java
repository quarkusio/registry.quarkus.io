package io.quarkus.registry.endpoints;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.registry.model.Category;

public interface CategoryResource extends PanacheEntityResource<Category, String> {
}
