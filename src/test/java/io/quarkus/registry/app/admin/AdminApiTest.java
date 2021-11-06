package io.quarkus.registry.app.admin;

import java.net.HttpURLConnection;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class AdminApiTest {

    private static final String GROUP_ID = "foo.bar";
    private static final String ARTIFACT_ID = "foo-extension";

    @BeforeAll
    @Transactional
    static void setUp() {
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

    @Inject
    AdminService adminService;

    @Test
    void unauthenticated_requests_should_forbid_access() {
        given().body(
                        "{ \"artifact\":\"aaaa\",\"artifactId\": \"string\", \"description\": \"string\", \"groupId\": \"messging\", \"metadata\":   {}, \"name\": \"string\",\"origins\": [   {     \"id\": \"string\",     \"platform\": false   } ], \"version\": \"string\"}")
                .post("/admin/v1/extensions")
                .then()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    void extension_description_and_name_may_be_updated_between_releases() {
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(200)
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
        adminService.onExtensionCreate(new ExtensionCreateEvent(jsonExtension));

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("extensions[0].name", is("Another Name"),
                        "extensions[0].description", is("Another Description"));

    }
}