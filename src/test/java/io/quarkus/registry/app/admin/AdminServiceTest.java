package io.quarkus.registry.app.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdminServiceTest extends BaseTest {

    private Extension extension;

    @BeforeEach
    @Transactional
    void setUp() {
        extension = new Extension();
        extension.name = "Test Extension";
        extension.description = "A Test Extension";
        extension.groupId = "test.group";
        extension.artifactId = "test-artifact";
        extension.persistAndFlush();
    }

    @Test
    @Transactional
    void isBestRelease_should_return_true_when_no_releases_exist() {
        boolean result = AdminService.isBestRelease(extension, "1.0.0.Final");
        assertTrue(result, "Should be true when no releases exist");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_true_for_first_final_release() {
        ExtensionRelease release = new ExtensionRelease();
        release.extension = extension;
        release.version = "1.0.0.Final";
        release.quarkusCoreVersion = "2.0.0.Final";
        release.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.0.0.Final");
        assertTrue(result, "Same version should be considered best (idempotent)");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_false_for_older_final_release() {
        ExtensionRelease newer = new ExtensionRelease();
        newer.extension = extension;
        newer.version = "1.1.0.Final";
        newer.quarkusCoreVersion = "2.0.0.Final";
        newer.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.0.0.Final");
        assertFalse(result, "Older Final release should not be best when newer Final exists");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_true_for_newer_final_release() {
        ExtensionRelease older = new ExtensionRelease();
        older.extension = extension;
        older.version = "1.0.0.Final";
        older.quarkusCoreVersion = "2.0.0.Final";
        older.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.1.0.Final");
        assertTrue(result, "Newer Final release should be best");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_false_for_cr_when_final_exists() {
        ExtensionRelease finalRelease = new ExtensionRelease();
        finalRelease.extension = extension;
        finalRelease.version = "1.0.0.Final";
        finalRelease.quarkusCoreVersion = "2.0.0.Final";
        finalRelease.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.1.0.CR1");
        assertFalse(result, "CR release should not supersede Final release");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_false_for_beta_when_final_exists() {
        ExtensionRelease finalRelease = new ExtensionRelease();
        finalRelease.extension = extension;
        finalRelease.version = "1.0.0.Final";
        finalRelease.quarkusCoreVersion = "2.0.0.Final";
        finalRelease.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.1.0.Beta1");
        assertFalse(result, "Beta release should not supersede Final release");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_false_for_alpha_when_final_exists() {
        ExtensionRelease finalRelease = new ExtensionRelease();
        finalRelease.extension = extension;
        finalRelease.version = "1.0.0.Final";
        finalRelease.quarkusCoreVersion = "2.0.0.Final";
        finalRelease.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.1.0.Alpha1");
        assertFalse(result, "Alpha release should not supersede Final release");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_false_for_snapshot_when_final_exists() {
        ExtensionRelease finalRelease = new ExtensionRelease();
        finalRelease.extension = extension;
        finalRelease.version = "1.0.0.Final";
        finalRelease.quarkusCoreVersion = "2.0.0.Final";
        finalRelease.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.1.0-SNAPSHOT");
        assertFalse(result, "Snapshot release should not supersede Final release");
    }

    @Test
    @Transactional
    void isBestRelease_should_return_true_for_service_pack_after_final() {
        ExtensionRelease finalRelease = new ExtensionRelease();
        finalRelease.extension = extension;
        finalRelease.version = "1.0.0.Final";
        finalRelease.quarkusCoreVersion = "2.0.0.Final";
        finalRelease.persistAndFlush();

        boolean result = AdminService.isBestRelease(extension, "1.0.0.SP1");
        assertTrue(result, "Service Pack should supersede Final release");
    }

}
