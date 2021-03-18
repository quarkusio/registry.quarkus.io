package io.quarkus.registry.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SemverTest {

    @Test
    void should_support_semver_versions() {
        assertEquals("1.0.0-alpha-1", Semver.toSemver("1.0.0-alpha-1"));
        assertEquals("1.0.0", Semver.toSemver("1.0.0"));
        assertEquals("0.0.1", Semver.toSemver("0.0.1"));
    }

    @Test
    void should_handle_final_releases() {
        assertEquals("1.0.0", Semver.toSemver("1.0.0.Final"));
        assertEquals("1.2.0", Semver.toSemver("1.2.0.Final"));
        assertEquals("2.1.0", Semver.toSemver("2.1.0.Final"));
    }

    @Test
    void should_handle_qualifiers() {
        assertEquals("1.0.0-Alpha.1", Semver.toSemver("1.0.0.Alpha1"));
        assertEquals("1.2.0-Beta.2", Semver.toSemver("1.2.0.Beta2"));
        assertEquals("1.3.0-Beta.2", Semver.toSemver("1.3.0.Beta2"));
        assertEquals("10.1.0-Beta.10", Semver.toSemver("10.1.0.Beta10"));

    }

    @Test
    void should_handle_extra_qualifiers() {
        assertEquals("1.0.0-redhat-0001", Semver.toSemver("1.0.0.Final-redhat-0001"));
        assertEquals("1.0.0-Beta.1-redhat-0001", Semver.toSemver("1.0.0.Beta1-redhat-0001"));
    }
    @Test
    void should_handle_null_versions() {
        assertNull(Semver.toSemver(null));
        assertFalse(Semver.isSemver(null));
    }

}