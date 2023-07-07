package io.quarkus.registry.app.metrics;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

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
                if ("http.server.requests".equals(id.getName())) {
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
        };
    }
}
