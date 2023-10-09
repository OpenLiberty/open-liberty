package io.openliberty.microprofile.metrics30.setup.config;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricPercentileConfiguration extends PropertyArrayConfiguration<Double> {
    private boolean isDisabled = false;

    private static final String CLASS_NAME = MetricPercentileConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public MetricPercentileConfiguration(String metricName, Double[] percentileValues) {
        super(metricName, percentileValues);
    }

    public MetricPercentileConfiguration(String metricName, boolean isDisabled) {
        super(metricName, null);
        this.isDisabled = isDisabled;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    /**
     * 
     * Parse the `mp.metrics.distribution.percentile` property.
     * syntax of {@code <metric_name>=<value-1>,<value-2>,...,<value-n>}
     * No values supplied to a metric name disables percentile output.
     * Can use wild card `*` at the end of metric name (e.g. demo.app.*)
     * 
     * @param input MP Config value
     * @return Collection of {@link MetricPercentileConfiguration} objects
     */

    public static Collection<MetricPercentileConfiguration> parseMetricPercentiles(String input) {

        ArrayDeque<MetricPercentileConfiguration> metricPercentileConfigCollection = new ArrayDeque<MetricPercentileConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            MetricPercentileConfiguration metricPercentileConfiguration;

            // empty value - disabled
            if (keyValueSplit.length == 1) {
                metricPercentileConfiguration = new MetricPercentileConfiguration(metricName, true);
            } else {
                // Parse values of percentile - ensure value is 0 <= x <= 1.0
                Double[] percentileValues = Arrays.asList(keyValueSplit[1].split(",")).stream().map(s -> {

                    if (s.matches("[0][.][0-9]+")) {
                        return Double.parseDouble(s);
                    } else {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.logp(Level.FINER, CLASS_NAME, null,
                                    "The value \"{0}\" is invalid for the \"{1}\" property. Only values 0.0-1.0 inclusively are accepted.",
                                    new Object[] { s, MetricsConfigurationManager.MP_PERCENTILES_PROP });
                        }
                        return null;
                    }

                }).filter(d -> d != null && d >= 0.0 && d <= 1.0).toArray(Double[]::new);

                Arrays.sort(percentileValues);

                metricPercentileConfiguration = new MetricPercentileConfiguration(metricName, percentileValues);
            }

            // LIFO - right most configuration takes precedence
            metricPercentileConfigCollection.addFirst(metricPercentileConfiguration);
        }
        return metricPercentileConfigCollection;

    }

}
