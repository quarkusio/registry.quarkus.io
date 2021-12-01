package io.quarkus.registry.app.maven.cache;

import java.util.Date;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

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
