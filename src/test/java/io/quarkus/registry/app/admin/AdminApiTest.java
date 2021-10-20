package io.quarkus.registry.app.admin;

import java.net.HttpURLConnection;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class AdminApiTest {

    @Test
    void unauthenticated_requests_should_forbid_access() {
        given().body(
                        "{ \"artifact\":\"aaaa\",\"artifactId\": \"string\", \"description\": \"string\", \"groupId\": \"messging\", \"metadata\":   {}, \"name\": \"string\",\"origins\": [   {     \"id\": \"string\",     \"platform\": false   } ], \"version\": \"string\"}")
                .post("/admin/v1/extensions")
                .then()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN);
    }
}