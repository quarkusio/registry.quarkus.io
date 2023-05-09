package io.quarkus.registry.app.metrics;

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
                if (id.getName().equals("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true) // histogram buckets (e.g. prometheus histogram_quantile)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
