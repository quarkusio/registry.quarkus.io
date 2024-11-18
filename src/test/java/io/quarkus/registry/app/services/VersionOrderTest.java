package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class VersionOrderTest extends BaseTest {

    @BeforeEach
    @Transactional
    void setUp() {
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();
            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.persistAndFlush();

            PlatformRelease release201 = new PlatformRelease();
            release201.platformStream = stream20;
            release201.version = "2.0.1.Final";
            release201.quarkusCoreVersion = "2.0.1.Final";
            release201.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.1.Final";
            release201.persistAndFlush();

            PlatformRelease release202 = new PlatformRelease();
            release202.platformStream = stream20;
            release202.version = "2.0.2.Final";
            release202.quarkusCoreVersion = "2.0.2.Final";
            release202.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.2.Final";
            release202.persistAndFlush();

            PlatformStream stream21 = new PlatformStream();
            stream21.platform = platform;
            stream21.streamKey = "2.1";
            stream21.persistAndFlush();

            PlatformRelease release210CR1 = new PlatformRelease();
            release210CR1.platformStream = stream21;
            release210CR1.version = "2.1.0.CR1";
            release210CR1.quarkusCoreVersion = "2.1.0.CR1";
            release210CR1.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.CR1";
            release210CR1.persistAndFlush();
        }
        {
            Platform platform = Platform.findByKey("com.redhat.quarkus.platform").get();
            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.persistAndFlush();

            PlatformRelease release200 = new PlatformRelease();
            release200.platformStream = stream20;
            release200.version = "2.0.0.Final-redhat0001";
            release200.quarkusCoreVersion = release200.version;
            release200.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.0.Final";
            release200.persistAndFlush();
        }
    }

    @Test
    public void should_order_by_qualifier() {
        List<List<String>> ids = given()
                .get("/client/platforms")
                .then()
                .statusCode(200)
                .header("Content-Type", containsString(MediaType.APPLICATION_JSON))
                .extract().path("platforms.streams.id");
        assertThat(ids).containsExactly(List.of("2.0", "2.1"), List.of("2.0"));
    }
}
