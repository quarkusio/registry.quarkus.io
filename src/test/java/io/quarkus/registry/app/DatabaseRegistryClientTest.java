package io.quarkus.registry.app;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DatabaseRegistryClientTest {

    @Test
    void should_return_application_json_as_response_type() {
        given()
                .header(HttpHeaders.ACCEPT, "application/json;custom=aaaaaaaaaaaaaaaaaa;charset=utf-7")
                .get("/client/platforms")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }
}