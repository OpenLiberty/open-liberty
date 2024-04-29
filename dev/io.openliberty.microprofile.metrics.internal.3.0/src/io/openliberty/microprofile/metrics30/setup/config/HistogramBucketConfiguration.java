package io.openliberty.microprofile.metrics30.setup.config;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistogramBucketConfiguration extends PropertyArrayConfiguration<Double> {

    private static final String CLASS_NAME = HistogramBucketConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public HistogramBucketConfiguration(String metricName, Double[] values) {
        super(metricName, values);

    }

    public static Collection<HistogramBucketConfiguration> parse(String input) {

        ArrayDeque<HistogramBucketConfiguration> metricBucketConfiCollection = new ArrayDeque<HistogramBucketConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            HistogramBucketConfiguration metricBucketConfiguration = null;

            /*
             * <metricName>=<blank> --> invalid
             * Nothing happens and metric name and empty-value is not recorded
             */
            if (keyValueSplit.length == 1) {
                continue;
            } else {
                // Parse values of buckets
                Double[] bucketValues = Arrays.asList(keyValueSplit[1].split(",")).stream().map(s -> {
                    if (s.matches("[0-9]+[.]*[0-9]*")) {
                        return Double.parseDouble(s);
                    } else {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, null,
                                    "The value \"{0}\" is invalid for the \"{1}\" property. Only integer "
                                                                     + "and decimal values are accepted.",
                                    new Object[] { s, MetricsConfigurationManager.MP_HISTOGRAM_BUCKET_PROP });
                        return null;
                    }
                }).filter(x -> x != null).toArray(Double[]::new);

                Arrays.sort(bucketValues);

                metricBucketConfiguration = new HistogramBucketConfiguration(metricName, bucketValues);
            }

            // LIFO - right most configuration takes precedence
            metricBucketConfiCollection.addFirst(metricBucketConfiguration);
        }
        return metricBucketConfiCollection;

    }

}
