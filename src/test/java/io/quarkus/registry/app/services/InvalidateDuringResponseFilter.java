package io.quarkus.registry.app.services;

import io.quarkus.registry.app.maven.cache.MavenCache;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Test-only filter that invalidates the Maven cache in the middle of producing a response, reproducing what the
 * scheduled {@code MavenCacheState} refresh does when a publish lands while a request is in flight.
 * <p>
 * Response filters run in descending priority order, so this one, at a higher priority than
 * {@code MavenCacheFilter}'s default {@code Priorities.USER}, runs before it: the invalidation is already done by
 * the time the cache filter decides whether to store the response.
 */
@Provider
@Priority(6000)
public class InvalidateDuringResponseFilter implements ContainerResponseFilter {

    private static volatile boolean armed = false;

    @Inject
    MavenCache mavenCache;

    /** Invalidates the cache during the next response, then disarms. */
    static void armOnce() {
        armed = true;
    }

    static void disarm() {
        armed = false;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (armed) {
            armed = false;
            mavenCache.clear();
        }
    }
}
