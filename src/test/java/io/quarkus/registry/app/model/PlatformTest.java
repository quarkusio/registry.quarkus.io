package io.quarkus.registry.app.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlatformTest {

    @Test
    void toPlatformName() {
        assertNull(Platform.toPlatformName(null));
        assertEquals("Qpid jms",
                Platform.toPlatformName("io.quarkus.platform:quarkus-qpid-jms-bom-quarkus-platform-descriptor"));
        assertEquals("Kogito", Platform.toPlatformName("io.quarkus.platform:quarkus-kogito-bom-quarkus-platform-descriptor"));
        assertEquals("Hazelcast client",
                Platform.toPlatformName("io.quarkus.platform:quarkus-hazelcast-client-quarkus-platform-descriptor"));
        assertEquals("quarkus-foo-bom-quarkus-platform-descriptor",
                Platform.toPlatformName("quarkus-foo-bom-quarkus-platform-descriptor"));
    }
}
