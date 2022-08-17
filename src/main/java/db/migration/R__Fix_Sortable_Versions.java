package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.registry.app.util.Version;

/**
 * Fixes the version_sortable value column in the extension_release table
 */
public class R__Fix_Sortable_Versions extends BaseJavaMigration {

    /**
     * Increase this number to have this script re-run when the application starts
     */
    private final Integer checksum = 1;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        // Update quarkus_core_version_sortable
        updateSortableColumn(connection,
                "SELECT DISTINCT QUARKUS_CORE_VERSION FROM extension_release",
                "UPDATE extension_release set quarkus_core_version_sortable = ? where quarkus_core_version = ?");
        // Update version_sortable
        updateSortableColumn(connection,
                "SELECT DISTINCT VERSION FROM extension_release",
                "UPDATE extension_release set version_sortable = ? where version = ?");
    }

    private static void updateSortableColumn(Connection connection, String selectVersionsSQL, String updateVersionsSQL)
            throws SQLException {
        Map<String, String> transformedVersions = new HashMap<>();
        // Get existing Quarkus Core Versions
        try (PreparedStatement stmt = connection.prepareStatement(selectVersionsSQL);
                ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                String quarkusCoreVersion = resultSet.getString(1);
                transformedVersions.put(quarkusCoreVersion, Version.toSortable(quarkusCoreVersion));
            }
        }
        if (transformedVersions.size() > 0) {
            int rowsUpdated = 0;
            // Execute Updates
            try (PreparedStatement stmt = connection.prepareStatement(updateVersionsSQL)) {
                for (Map.Entry<String, String> entry : transformedVersions.entrySet()) {
                    String original = entry.getKey();
                    String sortable = entry.getValue();
                    stmt.setString(1, sortable);
                    stmt.setString(2, original);
                    int rows = stmt.executeUpdate();
                    rowsUpdated += rows;
                    Log.debugf("%s rows updated from %s to %s%n", rows, original, sortable);
                }
            }
            // Update DbState timestamp only if any rows changed
            if (rowsUpdated > 0) {
                DbState.updateUpdatedAt(connection);
            }
        }
    }

    @Override
    public Integer getChecksum() {
        return checksum;
    }
}
