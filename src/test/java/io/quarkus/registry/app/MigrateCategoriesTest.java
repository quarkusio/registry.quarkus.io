package io.quarkus.registry.app;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Covers {@code V20__Move_categories_to_platform_release.sql}.
 * <p>
 * Every other test starts from a fully migrated, empty database, where the backfill in V20 has no rows to work on. The
 * only database where it does anything is production, so it is worth standing up the pre-V20 schema by hand, putting
 * the shape of data production holds into it, and checking what comes out the other side.
 */
@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MigrateCategoriesTest {

    private static final MigrationVersion BEFORE_THE_MOVE = MigrationVersion.fromVersion("19");

    @Inject
    Flyway flyway;

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    void migrateToTheVersionBeforeTheMove() {
        flyway.clean();
        Flyway.configure()
                .configuration(flyway.getConfiguration())
                .target(BEFORE_THE_MOVE)
                .load()
                .migrate();
    }

    @AfterEach
    void clean() {
        flyway.clean();
    }

    @Test
    void should_move_categories_onto_the_release_that_declared_them() throws Exception {
        givenLegacyCategories();

        flyway.migrate();

        // Declared in the order the join rows were inserted in, with the join row's metadata carried across
        assertThat(categoriesOf("2.8.0.Final")).containsExactly(
                Map.of("id", "data", "name", "Data", "description", "Accessing and managing your data",
                        "metadata", Map.of("pinned", true)),
                withoutMetadata("web", "Web", "REST endpoints, HTTP and web formats"));

        // A release that declared nothing keeps an empty list rather than a null
        assertThat(categoriesOf("2.7.0.Final")).isEmpty();
    }

    @Test
    void should_drop_the_category_tables() throws Exception {
        givenLegacyCategories();

        flyway.migrate();

        assertThat(tableExists("category")).isFalse();
        assertThat(tableExists("platform_release_category")).isFalse();
    }

    /**
     * The point of the backfill: what production already holds must still be served after the move.
     */
    @Test
    void should_serve_migrated_categories_to_clients() throws Exception {
        givenLegacyCategories();

        flyway.migrate();

        given()
                .get("/client/categories/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("categories.id", contains("data", "web"))
                .body("categories.find { it.id == 'data' }.name", is("Data"))
                .body("categories.find { it.id == 'data' }.metadata.pinned", is(true));
    }

    /**
     * Two listed releases, one of which declares two categories through the join table the way production does.
     */
    private void givenLegacyCategories() throws SQLException {
        execute("""
                insert into platform_stream (id, stream_key, stream_key_sortable, platform_id)
                    select 1, '2.8', '000000002.000000008', id from platform where platform_key = 'io.quarkus.platform';

                insert into platform_release (id, version, version_sortable, quarkus_core_version, bom, platform_stream_id)
                    values (1, '2.8.0.Final', '000000002.000000008.000000000.Final',
                            '2.8.0.Final', 'io.quarkus.platform:quarkus-bom::pom:2.8.0.Final', 1),
                           (2, '2.7.0.Final', '000000002.000000007.000000000.Final',
                            '2.7.0.Final', 'io.quarkus.platform:quarkus-bom::pom:2.7.0.Final', 1);

                insert into category (id, name, category_key, description) values
                    (1, 'Web', 'web', 'REST endpoints, HTTP and web formats'),
                    (2, 'Data', 'data', 'Accessing and managing your data');

                insert into platform_release_category (id, platform_release_id, category_id, metadata) values
                    (1, 1, 2, '{"pinned": true}'),
                    (2, 1, 1, null);
                """);
    }

    private List<Map<String, Object>> categoriesOf(String version) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select categories from platform_release where version = '" + version + "'")) {
            assertThat(rs.next()).as("No platform release " + version).isTrue();
            return objectMapper.readValue(rs.getString(1), new TypeReference<>() {
            });
        }
    }

    private boolean tableExists(String table) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "select to_regclass('public." + table + "') is not null")) {
            rs.next();
            return rs.getBoolean(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * A category whose join row carried no metadata. Spelled out longhand because {@link Map#of} rejects null values.
     */
    private static Map<String, Object> withoutMetadata(String id, String name, String description) {
        Map<String, Object> category = new HashMap<>();
        category.put("id", id);
        category.put("name", name);
        category.put("description", description);
        category.put("metadata", null);
        return category;
    }
}
