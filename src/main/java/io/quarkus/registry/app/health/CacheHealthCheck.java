package io.quarkus.registry.app.health;

import com.github.benmanes.caffeine.cache.Cache;
import io.quarkus.registry.app.maven.cache.MavenCache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Check the cache
 */
@Liveness
@ApplicationScoped
public class CacheHealthCheck implements HealthCheck {

    @Inject MavenCache mavenCache;
    
    @Override
    public HealthCheckResponse call() {
        Cache cache = mavenCache.getCache();
        return HealthCheckResponse.builder()
                .name("Maven cache health")
                .withData("last invalidate all time", mavenCache.getLastInvalidateTime().toString())
                .withData("estimated size", cache.estimatedSize())
                .withData("request count", cache.stats().requestCount())
                .withData("hit count", cache.stats().hitCount())
                .withData("miss count", cache.stats().missCount())
                .withData("load count", cache.stats().loadCount())
                .withData("eviction count", String.valueOf(cache.stats().evictionCount()))
                .withData("hit rate", String.valueOf(cache.stats().hitRate()))
                .withData("miss rate", String.valueOf(cache.stats().missRate()))
                .up().build();
    }
    
}
