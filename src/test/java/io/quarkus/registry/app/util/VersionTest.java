package io.quarkus.registry.app.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    @Test
    void toSortable() {
        assertThat(Version.toSortable("1.2.3.Final")).isEqualTo("00001.00002.00003.Final");
        assertThat(Version.toSortable("1.20.3.Final")).isEqualTo("00001.00020.00003.Final");
        assertThat(Version.toSortable("1.10.3.Final-redhat-00001")).isEqualTo("00001.00010.00003.Final-redhat-00001");
    }
}