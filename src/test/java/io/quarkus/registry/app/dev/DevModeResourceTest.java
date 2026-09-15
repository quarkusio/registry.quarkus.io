package io.quarkus.registry.app.dev;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;

/**
 * {@link DevModeResource} offers an unauthenticated page whose whole purpose is to load data into the registry. It is
 * only acceptable because {@code @IfBuildProfile("dev")} drops the bean from any other build, and that is the sort of
 * guarantee that quietly stops holding when someone moves an annotation.
 * <p>
 * These tests run under the {@code test} profile, which is not {@code dev}, so a 404 here is the same answer
 * production gives.
 */
@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DevModeResourceTest {

    @Test
    void should_not_serve_the_dev_page_outside_dev_mode() {
        given()
                .get("/dev")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void should_not_serve_the_catalog_outside_dev_mode() {
        given()
                .get("/dev/catalog.json")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * The page imports by posting to {@code /admin/v1/extension/catalog}, so it inherits that endpoint's
     * authentication rather than having any of its own. Keep it that way. A write endpoint here would be one this
     * class had to secure by itself, and the first thing anyone would reach for is skipping the token because it is
     * "only dev".
     */
    @Test
    void should_have_no_write_endpoint_of_its_own() {
        Class<? extends Annotation>[] writes = new Class[] { POST.class, PUT.class, PATCH.class, DELETE.class };
        assertThat(DevModeResource.class.getDeclaredMethods())
                .filteredOn(method -> Arrays.stream(writes).anyMatch(method::isAnnotationPresent))
                .extracting(Method::getName)
                .isEmpty();
    }

    /**
     * The page reads the catalog off the filesystem rather than the classpath, so the default path is only correct
     * for as long as the fixture stays where it is. Nothing else would notice it moving.
     */
    @Test
    void should_default_to_a_catalog_that_exists() {
        assertThat(Path.of(DevModeResource.DEFAULT_CATALOG)).isReadable();
    }
}
