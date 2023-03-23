package io.quarkus.registry.app;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PlatformStreamTest extends BaseTest {

    @BeforeEach
    @Transactional
    void setUp() {
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();
            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.pinned = true;
            stream20.persistAndFlush();

            PlatformRelease release201 = new PlatformRelease();
            release201.platformStream = stream20;
            release201.version = "2.0.1.Final";
            release201.quarkusCoreVersion = release201.version;
            release201.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.1.Final";
            release201.persistAndFlush();

            PlatformRelease release202 = new PlatformRelease();
            release202.platformStream = stream20;
            release202.version = "2.0.2.Final";
            release202.quarkusCoreVersion = release202.version;
            release202.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.2.Final";
            release202.persistAndFlush();

            PlatformStream stream21 = new PlatformStream();
            stream21.platform = platform;
            stream21.streamKey = "2.1";
            stream21.persistAndFlush();

            PlatformRelease release210CR1 = new PlatformRelease();
            release210CR1.platformStream = stream21;
            release210CR1.version = "2.1.0.CR1";
            release210CR1.quarkusCoreVersion = release210CR1.version;
            release210CR1.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.CR1";
            release210CR1.persistAndFlush();

            PlatformRelease release210Final = new PlatformRelease();
            release210Final.platformStream = stream21;
            release210Final.version = "2.1.0.Final";
            release210Final.quarkusCoreVersion = release210Final.version;
            release210Final.unlisted = true;
            release210Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.Final";
            release210Final.persistAndFlush();

            PlatformStream stream22 = new PlatformStream();
            stream22.platform = platform;
            stream22.streamKey = "2.2";
            stream22.persistAndFlush();

            PlatformRelease release220Final = new PlatformRelease();
            release220Final.platformStream = stream22;
            release220Final.version = "2.2.0.Final";
            release220Final.quarkusCoreVersion = release220Final.version;
            release220Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.2.0.Final";
            release220Final.persistAndFlush();

            PlatformRelease release221Final = new PlatformRelease();
            release221Final.platformStream = stream22;
            release221Final.version = "2.2.1.Final";
            release221Final.quarkusCoreVersion = release221Final.version;
            release221Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.2.1.Final";
            release221Final.persistAndFlush();

            PlatformStream stream23 = new PlatformStream();
            stream23.platform = platform;
            stream23.streamKey = "2.3";
            stream23.persistAndFlush();

            PlatformRelease release230Final = new PlatformRelease();
            release230Final.platformStream = stream23;
            release230Final.version = "2.3.0.Final";
            release230Final.quarkusCoreVersion = release230Final.version;
            release230Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.3.0.Final";
            release230Final.persistAndFlush();

            PlatformRelease release231Final = new PlatformRelease();
            release231Final.platformStream = stream23;
            release231Final.version = "2.3.1.Final";
            release231Final.quarkusCoreVersion = release231Final.version;
            release231Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.3.1.Final";
            release231Final.persistAndFlush();
        }
    }

    @Test
    void should_return_pinned_platform_streams() {
        given()
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms", hasSize(1),
                        "platforms[0].streams", hasSize(3),
                        "platforms[0].streams.id", hasItems("2.3", "2.2", "2.0"));

    }
}
