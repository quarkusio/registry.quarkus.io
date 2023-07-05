package io.quarkus.registry.app.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.concurrent.TimeUnit;

@Singleton
public class MetricsConfiguration {

    /**
     * Enable histogram buckets for http.server.requests metric
     */
    @Produces
    @Singleton
    public MeterFilter enableHistogram() {
        return new MeterFilter() {

            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .serviceLevelObjectives(
                                    TimeUnit.MILLISECONDS.toNanos(100),
                                    TimeUnit.MILLISECONDS.toNanos(500),
                                    TimeUnit.SECONDS.toNanos(1),
                                    TimeUnit.SECONDS.toNanos(5)
                            )
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
