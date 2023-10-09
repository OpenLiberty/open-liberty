package io.openliberty.microprofile.metrics30.setup.config;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PropertyConfiguration {

    protected String metricName;

    public String getMetricNameGrouping() {
        return metricName;
    }

    /**
     * Given a metric name and a collection of PropertyConfiguration objects, will return a match to the
     * metric name if it exists, otherwise a null is returned.
     * 
     * @param <T> extends PropertyConfiguration
     * @param configs Collection of T
     * @param metricName the metric name to find a matching configuration for
     * @return the matching configuration or null if non exists.
     */
    public static <T extends PropertyConfiguration> T matches(Collection<T> configs, String metricName) {
        for (PropertyConfiguration histoConfig : configs) {

            if (histoConfig.getMetricNameGrouping().contentEquals("*")) {
                return (T) histoConfig;
            }

            Pattern p = Pattern.compile(histoConfig.getMetricNameGrouping());
            Matcher m = p.matcher(metricName.trim());

            if (m.matches()) {
                return (T) histoConfig;
            }

        }
        return null;
    }

}
