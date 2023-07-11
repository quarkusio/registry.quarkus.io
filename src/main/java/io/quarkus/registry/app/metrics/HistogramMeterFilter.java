package io.quarkus.registry.app.metrics;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.inject.Singleton;

/**
 * Enable histogram buckets for http.server.requests metric
 */
@Singleton
public class HistogramMeterFilter implements MeterFilter {
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Tag OP_READ = Tag.of("op", "r");
    private static final Tag OP_WRITE = Tag.of("op", "w");

    @Override
    public Meter.Id map(Meter.Id id) {
        if (isHttpServerRequests(id)) {
            String uri = id.getTag("uri");
            String method = id.getTag("method");
            if (uri == null || method == null) {
                return id;
            }
            if (WRITE_METHODS.contains(method) && uri.startsWith("/admin")) {
                return id.withTag(OP_WRITE);
            }
            return id.withTag(OP_READ);
        }
        return id;
    }

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (isHttpServerRequests(id)) {
            //            if (id.getTags().contains(Outcome.CLIENT_ERROR.asTag())) {
            //                return MeterFilter.deny().configure(id, config);
            //            }
            return DistributionStatisticConfig.builder()
                    .percentilesHistogram(false) // histogram buckets (e.g. prometheus histogram_quantile)
                    .serviceLevelObjectives(
                            TimeUnit.MILLISECONDS.toNanos(5),
                            TimeUnit.MILLISECONDS.toNanos(10),
                            TimeUnit.MILLISECONDS.toNanos(25),
                            TimeUnit.MILLISECONDS.toNanos(50),
                            TimeUnit.MILLISECONDS.toNanos(250),
                            TimeUnit.MILLISECONDS.toNanos(500),
                            TimeUnit.MILLISECONDS.toNanos(1_000),
                            TimeUnit.MILLISECONDS.toNanos(2_500),
                            TimeUnit.MILLISECONDS.toNanos(5_000),
                            TimeUnit.MILLISECONDS.toNanos(10_000)) //slo slots
                    .build()
                    .merge(config);
        }
        return config;
    }

    private boolean isHttpServerRequests(Meter.Id id) {
        return "http.server.requests".equals(id.getName());
    }
}
