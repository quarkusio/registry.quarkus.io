package db.migration;

import static io.quarkus.registry.catalog.Extension.MD_BUILT_WITH_QUARKUS_CORE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.CatalogMapperHelper;

/**
 * Quarkus core version column in ExtensionRelease table was always being set to 0.0.0.0
 * This migration will read from the JSON metadata and update the value accordingly
 */
public class V7__Fix_Quarkus_Core extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        // Get existing Quarkus Core Versions
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM extension_release WHERE quarkus_core_version = '0.0.0' and metadata is not null",
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE);
                ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                String metadata = resultSet.getString("metadata");
                String quarkusCoreVersion = extractQuarkusCore(metadata);
                resultSet.updateString("quarkus_core_version", quarkusCoreVersion);
                resultSet.updateRow();
            }
        }
    }

    private String extractQuarkusCore(String json) throws Exception {
        JsonNode jsonNode = CatalogMapperHelper.mapper().readTree(json);
        String quarkusCore = jsonNode.path(MD_BUILT_WITH_QUARKUS_CORE).textValue();
        // Some extensions were published using the full GAV
        if (quarkusCore == null) {
            // Cannot determine Quarkus version
            quarkusCore = "0.0.0";
        } else if (quarkusCore.contains(":")) {
            try {
                quarkusCore = ArtifactCoords.fromString(quarkusCore).getVersion();
            } catch (IllegalArgumentException iae) {
                // ignore
            }
        }
        return quarkusCore;
    }
}
