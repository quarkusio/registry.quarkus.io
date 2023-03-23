package io.quarkus.registry.app.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@NamedQueries({
        @NamedQuery(name = "DbState.findUpdatedAt", query = "select d.updatedAt from DbState d where d.id=1"),
})
@NamedNativeQueries({
        @NamedNativeQuery(name = "DbState.updateUpdateAt", query = DbState.UPDATE_SQL)
})
public class DbState extends BaseEntity {

    static final String UPDATE_SQL = "UPDATE db_state set updated_at = CURRENT_TIMESTAMP where id =1";

    @Temporal(value = TemporalType.TIMESTAMP)
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    public Date updatedAt;

    /**
     * There will be only one row in this table
     */
    public static Date findUpdatedAt() {
        return getEntityManager().createNamedQuery("DbState.findUpdatedAt", Date.class).getSingleResult();
    }

    public static void updateUpdatedAt() {
        getEntityManager()
                .createNamedQuery("DbState.updateUpdateAt")
                .executeUpdate();
    }

    /**
     * Used in Flyway migration scripts
     */
    public static void updateUpdatedAt(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_SQL)) {
            ps.executeUpdate();
        }
    }
}
