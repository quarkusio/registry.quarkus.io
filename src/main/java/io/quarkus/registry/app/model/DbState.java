package io.quarkus.registry.app.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.ColumnDefault;

@Entity
@NamedQueries({
        @NamedQuery(name = "DbState.findUpdatedAt", query = "select d.updatedAt from DbState d where d.id=1"),
        @NamedQuery(name = "DbState.updateUpdateAt", query = "UPDATE DbState d set d.updatedAt = CURRENT_TIMESTAMP where d.id=1")
})
public class DbState extends BaseEntity {

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
}
