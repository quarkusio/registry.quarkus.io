package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.util.Version;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Update the quarkus_core_version_sortable column with the quarkus_core_version data
 */
public class V7__Update_Quarkus_Core_Version_Sortable extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, String> transformedVersions = new HashMap<>();
        Connection connection = context.getConnection();
        // Get existing Quarkus Core Versions
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT QUARKUS_CORE_VERSION FROM extension_release");
                ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                String quarkusCoreVersion = resultSet.getString(1);
                transformedVersions.put(quarkusCoreVersion, Version.toSortable(quarkusCoreVersion));
            }
        }
        if (transformedVersions.size() > 0) {
            // Execute Updates
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE extension_release set quarkus_core_version_sortable = ? where quarkus_core_version = ?");) {
                for (Map.Entry<String, String> entry : transformedVersions.entrySet()) {
                    String original = entry.getKey();
                    String sortable = entry.getValue();
                    stmt.setString(1, sortable);
                    stmt.setString(2, original);
                    int rows = stmt.executeUpdate();
                    Log.infof("%s rows updated from %s to %s%n", rows, original, sortable);
                }
            }
        }
    }
}
