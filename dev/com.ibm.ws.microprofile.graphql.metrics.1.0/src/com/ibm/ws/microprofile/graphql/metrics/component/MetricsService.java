package com.ibm.ws.microprofile.graphql.metrics.component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.cdi.config.ConfigKey;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.spi.EventingService;

public class MetricsService implements EventingService {

	private static final String PRE = "mp_graphql_";
    private static final String UNDERSCORE = "_";

    private final MetricRegistry metricRegistry;

    private final Map<Context, Long> startTimes = Collections.synchronizedMap(new IdentityHashMap<>());

    @FFDCIgnore(Throwable.class) // note, only ignoring the first catch block - the second & third should still be logged to FFDC
    public MetricsService() {
        Class<?> metricRegistryFactoryClass = null;
        try {
            metricRegistryFactoryClass = Class.forName("com.ibm.ws.microprofile.metrics.cdi23.producer.MetricRegistryFactory");
        } catch (Throwable t) {
            try {
                metricRegistryFactoryClass = Class.forName("io.openliberty.microprofile.metrics.internal.cdi30.producer.MetricRegistryFactory");
            } catch (Throwable t2) {
                // Auto FFDC
            }
        }
        if (metricRegistryFactoryClass == null) {
            throw new RuntimeException("Unable to find MetricRegistryFactory implementation");
        }
        try {
            Method getVendorRegisterMethod = metricRegistryFactoryClass.getMethod("getVendorRegistry");
            metricRegistry =  (MetricRegistry) getVendorRegisterMethod.invoke(null);
        } catch (Throwable t) {
            // Auto FFDC
            throw new RuntimeException("Unable to obtain vendor registry from MetricRegistryFactory");
        }
    }

    @Override
    public Operation createOperation(Operation operation) {
        final String name = getName(operation);
        final String description = getDescription(operation);

        Metadata metadata = Metadata.builder()
                .withName(name)
                .withType(MetricType.SIMPLE_TIMER)
                .withDescription(description)
                .build();
        metricRegistry.simpleTimer(metadata);
        return operation;
    }

    @Override
    public void beforeDataFetch(Context context) {
        startTimes.put(context, System.nanoTime());
    }

    @Override
    public void afterDataFetch(Context context) {
        Long startTime = startTimes.remove(context);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            metricRegistry.simpleTimer(getName(context))
                    .update(Duration.ofNanos(duration));
        }
    }

    @Override
    public String getConfigKey() {
        return ConfigKey.ENABLE_METRICS;
    }

    private String getName(Context context) {
        return PRE + context.getOperationType().toString() + UNDERSCORE + context.getFieldName();
    }

    private String getName(Operation operation) {
        return PRE + operation.getOperationType().toString() + UNDERSCORE + operation.getName();
    }

    private String getDescription(Operation operation) {
        return "Call statistics for the "
                + operation.getOperationType().toString().toLowerCase()
                + " '"
                + operation.getName()
                + "'";
    }
}