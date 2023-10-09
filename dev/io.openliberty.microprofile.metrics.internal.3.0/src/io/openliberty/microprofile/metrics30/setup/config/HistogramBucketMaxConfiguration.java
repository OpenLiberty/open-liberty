package io.openliberty.microprofile.metrics30.setup.config;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistogramBucketMaxConfiguration extends PropertySingleValueConfiguration<Double> {

    private static final String CLASS_NAME = HistogramBucketMaxConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public HistogramBucketMaxConfiguration(String metricName, Double value) {
        super(metricName, value);
    }

    public static Collection<HistogramBucketMaxConfiguration> parse(String input) {

        ArrayDeque<HistogramBucketMaxConfiguration> metricBucketMinMax = new ArrayDeque<HistogramBucketMaxConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            HistogramBucketMaxConfiguration metricBucketConfiguration = null;

            // metricGroup=<blank> == invalid
            if (keyValueSplit.length == 2) {
                if (keyValueSplit[1].matches(("[0-9]+[.]*[0-9]*"))) {
                    Double value = Double.parseDouble(keyValueSplit[1].trim());
                    metricBucketConfiguration = new HistogramBucketMaxConfiguration(metricName, value);
                } else {
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.logp(Level.FINER, CLASS_NAME, null,
                                "The value \"{0}\" is invalid for the \"{1}\" property. Only integer "
                                        + "and decimal values are accepted.",
                                new Object[] { keyValueSplit[1], MetricsConfigurationManager.MP_HISTOGRAM_BUCKET_PROP });
                    }
                }
            } else {
                //either no value.. or too many values
                continue;
            }

            // LIFO - right most configuration takes precedence
            metricBucketMinMax.addFirst(metricBucketConfiguration);
        }
        return metricBucketMinMax;

    }

}
