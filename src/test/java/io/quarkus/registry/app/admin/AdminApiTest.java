package io.quarkus.registry.app.admin;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;

import javax.transaction.Transactional;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@QuarkusTest
class AdminApiTest {

    private static final String GROUP_ID = "foo.bar";
    private static final String ARTIFACT_ID = "foo-extension";

    @BeforeAll
    @Transactional
    static void setUp() {
        {
            Extension extension = new Extension();
            extension.name = "Foo";
            extension.description = "A Foo Extension";
            extension.groupId = GROUP_ID;
            extension.artifactId = ARTIFACT_ID;
            extension.persistAndFlush();

            ExtensionRelease extensionRelease = new ExtensionRelease();
            extensionRelease.extension = extension;
            extensionRelease.version = "1.0.0";
            extensionRelease.quarkusCoreVersion = "2.0.0.Final";
            extensionRelease.persistAndFlush();
        }
        {
            Extension extensionToBeDeleted = new Extension();
            extensionToBeDeleted.name = "ToDelete";
            extensionToBeDeleted.description = "A Foo Extension to be deleted";
            extensionToBeDeleted.groupId = "delete";
            extensionToBeDeleted.artifactId = "me";
            extensionToBeDeleted.persistAndFlush();

            ExtensionRelease extensionReleaseToBeDeleted = new ExtensionRelease();
            extensionReleaseToBeDeleted.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted.version = "1.0.0";
            extensionReleaseToBeDeleted.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted.persistAndFlush();

            ExtensionRelease extensionReleaseToBeDeleted2 = new ExtensionRelease();
            extensionReleaseToBeDeleted2.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted2.version = "1.1.0";
            extensionReleaseToBeDeleted2.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted2.persistAndFlush();
        }
    }

    @Test
    void unauthenticated_requests_should_forbid_access() {
        given().body(
                        "{ \"artifact\":\"aaaa\",\"artifactId\": \"string\", \"description\": \"string\", \"groupId\": \"messging\", \"metadata\":   {}, \"name\": \"string\",\"origins\": [   {     \"id\": \"string\",     \"platform\": false   } ], \"version\": \"string\"}")
                .post("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    void extension_description_and_name_may_be_updated_between_releases() throws IOException {
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions[0].name", is("Foo"),
                        "extensions[0].description", is("A Foo Extension"));
        // Add a new Extension release
        JsonExtension jsonExtension = new JsonExtension();
        jsonExtension.setGroupId(GROUP_ID);
        jsonExtension.setArtifactId(ARTIFACT_ID);
        jsonExtension.setVersion("1.0.1");
        jsonExtension.setName("Another Name");
        jsonExtension.setDescription("Another Description");
        jsonExtension.setArtifact(new ArtifactCoords(GROUP_ID, ARTIFACT_ID, "1.0.1"));
        StringWriter sw = new StringWriter();
        JsonCatalogMapperHelper.serialize(jsonExtension, sw);

        given()
                .body(sw.toString())
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .post("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.name", hasItem("Another Name"),
                        "extensions.description", hasItem("Another Description"));

    }

    @Test
    void validate_input() {
        given()
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .contentType(ContentType.JSON)
                .body("parameter-violations.message", hasItem("X-Platform header missing"),
                        "parameter-violations.message", hasItem("Body payload is missing"));
    }

    @Test
    void validate_long_input() {
        given()
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .body("{\n"
                        + "\"artifact\":\"com.redhat.quarkus.A[" + StringUtils.repeat('A', 5308414)
                        + "]A:quarkus-bom::pom:2.2.3.Final-redhat-00114\",\n"
                        + "\"artifactId\": \"string\",\n"
                        + "\"description\": \"string\",\n"
                        + "\"groupId\": \"string\",\n"
                        + "\"metadata\":\n"
                        + "\n"
                        + "{ \"additionalProp1\": \"string\" }\n"
                        + ",\n"
                        + "\"name\": \"string\",\n"
                        + "\"origins\": [\n"
                        + "\n"
                        + "{ \"id\": \"string\", \"platform\": true }\n"
                        + "],\n"
                        + "\"version\": \"string\"\n"
                        + "}\n"
                        + "\n")
                .post("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @Test
    void delete_extension() {
        // Delete version
        given().formParams("groupId", "delete",
                        "artifactId", "me",
                        "version", "1.1.0")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.version", not(hasItem("1.1.0")),
                        "extensions.name", hasItem("ToDelete"));

        // Delete extension
        given().formParams("groupId", "delete",
                        "artifactId", "me")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.name", not(hasItem("ToDelete")));

    }

}