package io.openliberty.microprofile.metrics30.setup.config;

public abstract class PropertySingleValueConfiguration<T> extends PropertyConfiguration {
    protected T value = null;

    public PropertySingleValueConfiguration(String metricName, T value) {
        this.metricName = metricName;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getName() + "<Metric name: [%s]>; <value: %s>", metricName,
                value);
    }

    public T getValue() {
        return value;
    }
}
