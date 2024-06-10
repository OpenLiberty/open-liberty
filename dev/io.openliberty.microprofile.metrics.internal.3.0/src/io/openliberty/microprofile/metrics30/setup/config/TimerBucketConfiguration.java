package io.openliberty.microprofile.metrics30.setup.config;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimerBucketConfiguration extends PropertyArrayConfiguration<Duration> {

    private static final String CLASS_NAME = TimerBucketConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public TimerBucketConfiguration(String metricName, Duration[] values) {
        super(metricName, values);
    }

    public static Collection<TimerBucketConfiguration> parse(String input) {

        ArrayDeque<TimerBucketConfiguration> metricSLOConfiguration = new ArrayDeque<TimerBucketConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            TimerBucketConfiguration metricBucketConfiguration = null;

            // metricGroup=<blank> == invalid
            if (keyValueSplit.length == 1) {
                continue;
            } else {
                // Parse values
                Duration[] arrDuration = Arrays.asList(keyValueSplit[1].split(",")).stream().map(s -> {
                    s = s.trim();
                    if (s.matches("[0-9]+ms")) {
                        String val = s.substring(0, s.length() - 2);
                        return Duration.ofMillis(Long.parseLong(val));
                    } else if (s.matches("[0-9]+s")) {
                        String val = s.substring(0, s.length() - 1);
                        return Duration.ofSeconds(Long.parseLong(val));
                    } else if (s.matches("[0-9]+m")) {
                        String val = s.substring(0, s.length() - 1);
                        return Duration.ofMinutes(Long.parseLong(val));
                    } else if (s.matches("[0-9]+h")) {
                        String val = s.substring(0, s.length() - 1);
                        return Duration.ofHours(Long.parseLong(val));
                    } else if (s.matches("[0-9]+")) {
                        return Duration.ofMillis(Long.parseLong(s));
                    } else {
                        LOGGER.logp(Level.WARNING, CLASS_NAME, null,
                                    "The value \"{0}\" is invalid for the \"{1}\" property. Only integer values with an "
                                                                     + "optional time unit (e.g. ms,s,m,h) are accepted.",
                                    new Object[] { s, MetricsConfigurationManager.MP_TIMER_BUCKET_PROP });
                        return null;
                    }
                }).filter(s -> s != null).toArray(Duration[]::new);

                Arrays.sort(arrDuration);

                metricBucketConfiguration = new TimerBucketConfiguration(metricName, arrDuration);
            }

            // LIFO - right most configuration takes precedence
            metricSLOConfiguration.addFirst(metricBucketConfiguration);
        }
        return metricSLOConfiguration;

    }

}
