package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.util.Version;

/**
 * Fixes the version_sortable value column in the extension_release table
 */
public class V11__Fix_Extension_Release_Version_Sortable extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, String> transformedVersions = new HashMap<>();
        Connection connection = context.getConnection();
        // Get existing Quarkus Core Versions
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT VERSION FROM extension_release");
                ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                String version = resultSet.getString(1);
                transformedVersions.put(version, Version.toSortable(version));
            }
        }
        if (transformedVersions.size() > 0) {
            // Execute Updates
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE extension_release set version_sortable = ? where version = ?");) {
                for (Map.Entry<String, String> entry : transformedVersions.entrySet()) {
                    String original = entry.getKey();
                    String sortable = entry.getValue();
                    stmt.setString(1, sortable);
                    stmt.setString(2, original);
                    int rows = stmt.executeUpdate();
                    Log.debugf("%s rows updated from %s to %s%n", rows, original, sortable);
                }
            }
        }
    }
}
