package io.quarkus.registry.app.maven.cache;

import java.util.Date;
import java.util.Objects;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
public class MavenCacheState {

    @Inject
    MavenCache cache;

    private Date lastUpdate;

    /**
     * Check cache state on every 10 seconds
     */
    @PostConstruct
    @Scheduled(cron = "{quarkus.registry.cache.cron:*/10 * * * * ?}")
    void checkDbState() {
        Log.debugf("Checking cache state %s", lastUpdate);
        Date updatedAt = DbState.findUpdatedAt();
        if (lastUpdate == null || !Objects.equals(lastUpdate, updatedAt)) {
            Log.debugf("Cache changed -> %s", updatedAt);
            lastUpdate = updatedAt;
            cache.clear();
        }
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
