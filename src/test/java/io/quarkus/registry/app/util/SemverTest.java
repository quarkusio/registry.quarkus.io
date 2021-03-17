package io.quarkus.registry.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void should_handle_alpha_and_beta() {
        assertEquals("1.0.0-Alpha", Semver.toSemver("1.0.0.Alpha"));
        assertEquals("1.2.0-Beta", Semver.toSemver("1.2.0.Beta"));
    }

    @Test
    void should_handle_extra_qualifiers() {
        assertEquals("1.0.0+redhat-0001", Semver.toSemver("1.0.0.Final-redhat-0001"));
    }
}