package io.quarkus.registry.app.maven.cache;

import java.time.LocalDateTime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.registry.app.CacheNames;

/**
 * Holding the maven cache
 */
@ApplicationScoped
public class MavenCache {

    @ConfigProperty(name = "maven.cache.maximum-size", defaultValue = "10000")
    int maximumSize;

    private Cache<String, MavenResponse> cache;

    private LocalDateTime lastInvalidateTime = LocalDateTime.MIN;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .recordStats()
                .maximumSize(maximumSize)
                .build();
    }

    @CacheInvalidateAll(cacheName = CacheNames.METADATA)
    public void clear() {
        cache.invalidateAll();
        lastInvalidateTime = LocalDateTime.now();
    }

    public LocalDateTime getLastInvalidateTime() {
        return this.lastInvalidateTime;
    }

    public Cache<String, MavenResponse> getCache() {
        return this.cache;
    }
}
