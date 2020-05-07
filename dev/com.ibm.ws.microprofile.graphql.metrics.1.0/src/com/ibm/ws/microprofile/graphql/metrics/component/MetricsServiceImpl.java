package com.ibm.ws.microprofile.graphql.metrics.component;

import com.ibm.ws.microprofile.metrics.cdi23.producer.MetricRegistryFactory;

import io.smallrye.graphql.spi.MetricsService;

import org.eclipse.microprofile.metrics.MetricRegistry;

public class MetricsServiceImpl implements MetricsService {

    @Override
    public String getName() {
        return "Liberty MetricsServiceImpl";
    }

    @Override
    public MetricRegistry getMetricRegistry(MetricRegistry.Type type) {
        switch (type) {
            case VENDOR: return MetricRegistryFactory.getVendorRegistry();
            case BASE: return MetricRegistryFactory.getBaseRegistry();
            case APPLICATION: return MetricRegistryFactory.getApplicationRegistry();
            default: throw new IllegalArgumentException("Unexpected MetricRegistry.Type, " + type);
        }
    }
}
