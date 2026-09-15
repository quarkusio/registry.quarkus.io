package io.quarkus.registry.app.services;

import static io.quarkus.registry.app.CatalogTestSupport.COMMUNITY_CATALOG;
import static io.quarkus.registry.app.CatalogTestSupport.catalogCoords;
import static io.quarkus.registry.app.CatalogTestSupport.deserializeCatalog;
import static io.quarkus.registry.app.CatalogTestSupport.getPlatformDescriptor;
import static io.quarkus.registry.app.CatalogTestSupport.postCatalog;
import static io.quarkus.registry.app.CatalogTestSupport.readCatalogBytes;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests if the {@link ExtensionCatalog} content is generated correctly
 */
@QuarkusTest
public class PlatformCatalogContentProviderTest extends BaseTest {

    @Test
    void should_return_catalog() throws Exception {
        byte[] catalog = readCatalogBytes(COMMUNITY_CATALOG);
        ExtensionCatalog expected = deserializeCatalog(catalog).build();
        ArtifactCoords id = catalogCoords(catalog);

        postCatalog(catalog, id.getGroupId());

        // Served under the registry's own artifact version
        assertThat(getPlatformDescriptor(id)).usingRecursiveComparison().isEqualTo(expected);
        // ...and under the same version as in the qualifier
        assertThat(getPlatformDescriptor(id, id.getVersion())).usingRecursiveComparison().isEqualTo(expected);
    }
}
