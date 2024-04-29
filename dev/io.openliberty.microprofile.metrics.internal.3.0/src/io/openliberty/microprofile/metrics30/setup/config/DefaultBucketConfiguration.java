package io.openliberty.microprofile.metrics30.setup.config;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.logging.Logger;

public class DefaultBucketConfiguration extends PropertyBooleanConfiguration {

    private static final String CLASS_NAME = DefaultBucketConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public DefaultBucketConfiguration(String metricName, boolean value) {
        this.metricName = metricName;
        this.isEnabled = value;
    }

    public static Collection<DefaultBucketConfiguration> parse(String input) {

        ArrayDeque<DefaultBucketConfiguration> metricBucketConfiCollection = new ArrayDeque<DefaultBucketConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            DefaultBucketConfiguration metricDefaultedBucketConfiguration = null;

            //metricGroup=<blank> => default to false
            if (keyValueSplit.length == 1) {
                continue;
            } else {
                boolean isEnabledParam = Boolean.parseBoolean(keyValueSplit[1].trim());

                metricDefaultedBucketConfiguration = new DefaultBucketConfiguration(metricName, isEnabledParam);
            }

            // LIFO - right most configuration takes precedence
            metricBucketConfiCollection.addFirst(metricDefaultedBucketConfiguration);
        }
        return metricBucketConfiCollection;

    }

}
