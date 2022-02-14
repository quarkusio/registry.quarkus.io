package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Define the default platform based on the environment variable
 */
public class R__Define_Default_Platform extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Optional<String> defaultPlatform = ConfigProvider.getConfig().getOptionalValue("QUARKUS_DEFAULT_PLATFORM",
                String.class);
        if (defaultPlatform.isEmpty()) {
            // No Default Platform defined, skip
            return;
        }
        Connection connection = context.getConnection();
        // Set marked platform as default
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE platform set is_default = true where platform_key = ?")) {
            stmt.setString(1, defaultPlatform.get());
            int rows = stmt.executeUpdate();
            if (rows != 1) {
                throw new IllegalArgumentException(
                        "Platform " + defaultPlatform.get() + " does not exist. Check the QUARKUS_DEFAULT_PLATFORM setting");
            }
        }
        // Set other platforms as false
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE platform set is_default = false where platform_key <> ?")) {
            stmt.setString(1, defaultPlatform.get());
            stmt.executeUpdate();
        }
    }

    /**
     * Execute the migration only if the platform changes
     */
    @Override
    public Integer getChecksum() {
        String defaultPlatform = ConfigProvider.getConfig().getOptionalValue("QUARKUS_DEFAULT_PLATFORM",
                String.class).orElse(null);
        return defaultPlatform == null ? null : defaultPlatform.hashCode();
    }
}
