package io.quarkus.registry.app.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void toSortable() {
        assertSoftly(softly -> {
            softly.assertThat(Version.toSortable("1.2.3.Final")).isEqualTo("00001.00002.00003.00000.Final");
            softly.assertThat(Version.toSortable("1.20.3.Final")).isEqualTo("00001.00020.00003.00000.Final");
            softly.assertThat(Version.toSortable("1.10.3.Final-redhat-00001")).isEqualTo("00001.00010.00003.00000.Final-redhat-00001");
            softly.assertThat(Version.toSortable("1.2.3")).isEqualTo("00001.00002.00003.00000.Final");
            softly.assertThat(Version.toSortable("3.17")).isEqualTo("00003.00017.00000.00000.Final");

        });
    }

    @Test
    void shouldWorkWith4DigitsVersions() {
        assertThat(Version.toSortable("3.15.3.1")).isEqualTo("00003.00015.00003.00001.Final");
    }

    @Test
    void isVersionValid() {
        assertThatCode(() -> Version.validateVersion("1.2.3.Final")).doesNotThrowAnyException();
        assertThatCode(() -> Version.validateVersion("3.20.2.2")).doesNotThrowAnyException();
        // According to Maven, this is a valid version
        assertThatCode(
                () -> Version.validateVersion("%3c%68%74%6d%6c%3e%3c%68%65%61%64%3e%3c%73%63%72%69%70%74%3e%61%6c%65"
                        + "%72%74%28%64%6f%63%75%6d%65%6e%74%2e%6c%6f%63%61%74%69%6f%6e%29%3c%2f%73%63"
                        + "%72%69%70%74%3e%3c%2f%68%65%61%64%3e%3c%2f%68%74%6d%6c%3e")).doesNotThrowAnyException();
    }

    @Test
    void sortByReleaseImportance() {
        List<String> versions = Arrays.asList(
                "2.7.5.Final-redhat-00001",
                "2.3.9.CR3",
                "2.4.0.RC3",
                "2.5.0",
                "2.5.1.CR1",
                "2.4.0.CR2",
                "2.4.0.Final",
                "2.2.5.SP1-redhat-00001");
        versions.sort(Version.RELEASE_IMPORTANCE_COMPARATOR);
        assertThat(versions).containsExactly(
                "2.7.5.Final-redhat-00001",
                "2.5.0",
                "2.4.0.Final",
                "2.2.5.SP1-redhat-00001",
                "2.3.9.CR3",
                "2.4.0.CR2",
                "2.4.0.RC3",
                "2.5.1.CR1");
    }

    @Test
    void sortSPs() {
        List<String> versions = Arrays.asList(
                "2.13.7.Final-redhat-00003",
                "2.13.5.SP1-redhat-00002",
                "2.13.5.Final-redhat-00002",
                "2.7.6.Final-redhat-00012",
                "2.7.6.Final-redhat-00011",
                "2.7.6.Final-redhat-00009",
                "2.7.6.Final-redhat-00006",
                "2.7.5.Final-redhat-00011",
                "2.2.5.SP2-redhat-00003",
                "2.2.5.SP1-redhat-00001",
                "2.2.5.Final-redhat-00007",
                "2.2.3.SP2-redhat-00001",
                "2.2.3.SP1-redhat-00002",
                "2.2.3.Final-redhat-00013",
                "2.2.3-SNAPSHOT");
        versions.sort(Version.RELEASE_IMPORTANCE_COMPARATOR);
        assertThat(versions).containsExactly(
                "2.13.7.Final-redhat-00003",
                "2.13.5.SP1-redhat-00002",
                "2.13.5.Final-redhat-00002",
                "2.7.6.Final-redhat-00012",
                "2.7.6.Final-redhat-00011",
                "2.7.6.Final-redhat-00009",
                "2.7.6.Final-redhat-00006",
                "2.7.5.Final-redhat-00011",
                "2.2.5.SP2-redhat-00003",
                "2.2.5.SP1-redhat-00001",
                "2.2.5.Final-redhat-00007",
                "2.2.3.SP2-redhat-00001",
                "2.2.3.SP1-redhat-00002",
                "2.2.3.Final-redhat-00013",
                "2.2.3-SNAPSHOT");
    }

    @Test
    void shouldSort4DigitsVersions() {
        List<String> versions = Arrays.asList(
                "3.15.3.1",
                "3.15",
                "3.15.1",
                "3.15.2",
                "3.15.3",
                "3.15.4");
        versions.sort(Version.RELEASE_IMPORTANCE_COMPARATOR);
        assertThat(versions).containsExactly(
                "3.15.4",
                "3.15.3.1",
                "3.15.3",
                "3.15.2",
                "3.15.1",
                "3.15");
    }

}
