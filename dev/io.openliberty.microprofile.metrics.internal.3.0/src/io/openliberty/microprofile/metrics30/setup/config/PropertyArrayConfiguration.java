package io.openliberty.microprofile.metrics30.setup.config;

import java.util.Arrays;

public abstract class PropertyArrayConfiguration<T> extends PropertyConfiguration {
    protected T[] values = null;

    public PropertyArrayConfiguration(String metricName, T[] values) {
        this.metricName = metricName;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getName() + "<Metric name: [%s]>; <values: %s>", metricName,
                             Arrays.toString(values));
    }

    public T[] getValues() {
        return values;
    }

}
