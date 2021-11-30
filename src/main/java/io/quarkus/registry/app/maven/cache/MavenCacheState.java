package io.quarkus.registry.app.maven.cache;

import java.util.Date;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class MavenCacheState {

    @Inject
    MavenCache cache;

    private Date lastUpdate;

    @PostConstruct
    void init() {
        lastUpdate = DbState.findUpdatedAt();
    }

    @Scheduled(cron = "{quarkus.registry.cache.cron}")
    void checkDbState() {
        Log.debugf("Checking cache state %s", lastUpdate);
        Date updatedAt = DbState.findUpdatedAt();
        if (!Objects.equals(lastUpdate, updatedAt)) {
            Log.debugf("Cache changed -> %s", updatedAt);
            lastUpdate = updatedAt;
            cache.clear();
        }
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
