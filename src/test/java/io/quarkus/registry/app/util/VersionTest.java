package io.quarkus.registry.app.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class VersionTest {

    @Test
    void toSortable() {
        assertThat(Version.toSortable("1.2.3.Final")).isEqualTo("00001.00002.00003.Final");
        assertThat(Version.toSortable("1.20.3.Final")).isEqualTo("00001.00020.00003.Final");
        assertThat(Version.toSortable("1.10.3.Final-redhat-00001")).isEqualTo("00001.00010.00003.Final-redhat-00001");
    }

    @Test
    void isVersionValid() {
        assertThatCode(() -> Version.validateVersion("1.2.3.Final")).doesNotThrowAnyException();
        assertThatIllegalArgumentException().isThrownBy(
                () -> Version.validateVersion("%3c%68%74%6d%6c%3e%3c%68%65%61%64%3e%3c%73%63%72%69%70%74%3e%61%6c%65"
                        + "%72%74%28%64%6f%63%75%6d%65%6e%74%2e%6c%6f%63%61%74%69%6f%6e%29%3c%2f%73%63"
                        + "%72%69%70%74%3e%3c%2f%68%65%61%64%3e%3c%2f%68%74%6d%6c%3e"))
                .withMessageStartingWith("Invalid Version");
    }

}