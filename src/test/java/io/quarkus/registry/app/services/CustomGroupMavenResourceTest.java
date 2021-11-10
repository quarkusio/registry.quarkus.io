package io.quarkus.registry.app.services;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
@QuarkusTestResource(value = CustomGroupMavenResourceTest.CustomRegistryTestResource.class, restrictToAnnotatedClass = true)
public class CustomGroupMavenResourceTest {

    @Test
    void should_use_custom_descriptor_settings() {
        given()
                .get("/maven/foo/quarkus-registry-descriptor/1.0-SNAPSHOT/quarkus-registry-descriptor-1.0-SNAPSHOT.json")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("descriptor.artifact", is("foo:quarkus-registry-descriptor::json:1.0-SNAPSHOT"),
                        "platforms.artifact", is("foo:quarkus-platforms::json:1.0-SNAPSHOT"),
                        "non-platforms-extensions.artifact", is(nullValue()),
                        "quarkus-versions.recognized-versions-expression", is("[2.1.0.Final,)"),
                        "quarkus-versions.exclusive-provider", is(true),
                        "maven.repository.id", is("custom"));
    }

    public static class CustomRegistryTestResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of(
                    "quarkus.registry.groupId", "foo",
                    "quarkus.registry.non-platform-extensions.support", "false",
                    "quarkus.registry.quarkus-versions.expression", "[2.1.0.Final,)",
                    "quarkus.registry.quarkus-versions.exclusive-provider", "true",
                    "quarkus.registry.id", "custom"
            );
        }

        @Override
        public void stop() {

        }
    }

}
