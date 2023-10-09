package io.openliberty.microprofile.metrics30.setup.config;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimerBucketMinConfiguration extends PropertySingleValueConfiguration<Duration> {

    private static final String CLASS_NAME = TimerBucketMinConfiguration.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public TimerBucketMinConfiguration(String metricName, Duration value) {
        super(metricName, value);
    }

    public static Collection<TimerBucketMinConfiguration> parse(String input) {

        ArrayDeque<TimerBucketMinConfiguration> sloMinConfigCollection = new ArrayDeque<TimerBucketMinConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            TimerBucketMinConfiguration sloMinConfiguration = null;
            Duration dur;
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
                    dur = Duration.ofSeconds(Long.parseLong(s));
                } else {
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.logp(Level.FINER, CLASS_NAME, null,
                                    "The value \"{0}\" is invalid for the \"{1}\" property. Only integer values with an "
                                                                   + "optional time unit (e.g. ms,s,m,h) are accepted.",
                                    new Object[] { s, MetricsConfigurationManager.MP_TIMER_BUCKET_PROP });
                    }
                    return null;
                }

            } else {
                //either no value.. or too many values through improper syntax
                continue;
            }

            sloMinConfiguration = new TimerBucketMinConfiguration(metricName, dur);

            // LIFO - right most configuration takes precedence
            sloMinConfigCollection.addFirst(sloMinConfiguration);
        }
        return sloMinConfigCollection;

    }

}
