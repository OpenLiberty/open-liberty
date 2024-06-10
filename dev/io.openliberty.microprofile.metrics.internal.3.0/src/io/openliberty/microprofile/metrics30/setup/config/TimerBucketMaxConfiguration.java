package io.openliberty.microprofile.metrics30.setup.config;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimerBucketMaxConfiguration extends PropertySingleValueConfiguration<Duration> {

    private static final String CLASS_NAME = TimerBucketMaxConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public TimerBucketMaxConfiguration(String metricName, Duration value) {
        super(metricName, value);
    }

    public static Collection<TimerBucketMaxConfiguration> parse(String input) {

        ArrayDeque<TimerBucketMaxConfiguration> sloMinConfigCollection = new ArrayDeque<TimerBucketMaxConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            TimerBucketMaxConfiguration sloMinConfiguration = null;
            Duration dur = null;
            // metricGroup=<blank> == invalid
            if (keyValueSplit.length == 2) {

                String s = keyValueSplit[1];

                if (s.matches("[0-9]+ms")) {
                    String val = s.substring(0, s.length() - 2);
                    dur = Duration.ofMillis(Long.parseLong(val));
                } else if (s.matches("[0-9]+s")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofSeconds(Long.parseLong(val));
                } else if (s.matches("[0-9]+m")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofMinutes(Long.parseLong(val));
                } else if (s.matches("[0-9]+h")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofHours(Long.parseLong(val));
                } else if (s.matches("[0-9]+")) {
                    dur = Duration.ofMillis(Long.parseLong(s));
                } else {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, null,
                                "The value \"{0}\" is invalid for the \"{1}\" property. Only integer values with an "
                                                                 + "optional time unit (e.g. ms,s,m,h) are accepted.",
                                new Object[] { s, MetricsConfigurationManager.MP_TIMER_BUCKET_PROP });
                }

            } else {
                //either no value.. or too many values through improper syntax
                continue;
            }

            if (dur != null) {
                sloMinConfiguration = new TimerBucketMaxConfiguration(metricName, dur);

                // LIFO - right most configuration takes precedence
                sloMinConfigCollection.addFirst(sloMinConfiguration);
            }

        }
        return sloMinConfigCollection;

    }

}
