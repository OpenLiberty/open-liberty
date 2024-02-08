package io.openliberty.microprofile.metrics30.setup.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.ConfigProvider;

//import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

//@@@@@@@@@@@@@ This is the only use of the bnd new imports.
//import io.smallrye.metrics.SharedMetricRegistries;
//import io.smallrye.metrics.setup.ApplicationNameResolver;

//import io.openliberty.microprofile.metrics30.setup.ApplicationNameResolver;
//import io.openliberty.microprofile.metrics30.SharedMetricRegistries;
//import io.openliberty.microprofile.metrics30.SharedMetricRegistries;
//import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class MetricsConfigurationManager {

    private static final String CLASS_NAME = MetricsConfigurationManager.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    static final String MP_PERCENTILES_PROP = "mp.metrics.distribution.percentiles";
    static final String MP_HISTOGRAM_BUCKET_PROP = "mp.metrics.distribution.histogram.buckets";
    static final String MP_TIMER_BUCKET_PROP = "mp.metrics.distribution.timer.buckets";
    static final String MP_DEFAULT_BUCKET_PROP = "mp.metrics.distribution.percentiles-histogram.enabled";
    static final String MP_HISTOGRAM_MAX_CONFIG = "mp.metrics.distribution.histogram.max-value";
    static final String MP_HISTOGRAM_MIN_CONFIG = "mp.metrics.distribution.histogram.min-value";
    static final String MP_TIMER_MAX_CONFIG = "mp.metrics.distribution.timer.max-value";
    static final String MP_TIMER_MIN_CONFIG = "mp.metrics.distribution.timer.min-value";

    private static MetricsConfigurationManager instance;

    private MetricsConfigurationManager() {
    };

    private volatile Map<String, Collection<MetricPercentileConfiguration>> percentilesConfigMap = new HashMap<String, Collection<MetricPercentileConfiguration>>();

    private volatile Map<String, Collection<HistogramBucketConfiguration>> histogramBucketsConfigMap = new HashMap<String, Collection<HistogramBucketConfiguration>>();;

    private volatile Map<String, Collection<TimerBucketConfiguration>> timerBucketsConfigMap = new HashMap<String, Collection<TimerBucketConfiguration>>();

    private volatile Map<String, Collection<DefaultBucketConfiguration>> defaultBucketConfigMap = new HashMap<String, Collection<DefaultBucketConfiguration>>();

    private volatile Map<String, Collection<HistogramBucketMaxConfiguration>> defaultHistogramBucketMaxConfig = new HashMap<String, Collection<HistogramBucketMaxConfiguration>>();
    private volatile Map<String, Collection<HistogramBucketMinConfiguration>> defaultHistogramBucketMinConfig = new HashMap<String, Collection<HistogramBucketMinConfiguration>>();

    private volatile Map<String, Collection<TimerBucketMaxConfiguration>> defaultTimerBucketMaxConfig = new HashMap<String, Collection<TimerBucketMaxConfiguration>>();
    private volatile Map<String, Collection<TimerBucketMinConfiguration>> defaultTimerBucketMinConfig = new HashMap<String, Collection<TimerBucketMinConfiguration>>();

    public static synchronized MetricsConfigurationManager getInstance() {
        if (instance == null) {
            instance = new MetricsConfigurationManager();
        }
        return instance;
    }

    /**
     * Returns the matching {@link MetricPercentileConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link MetricPercentileConfiguration} object if it exists, null otherwise
     */
    public synchronized MetricPercentileConfiguration getPercentilesConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<MetricPercentileConfiguration> computedValues = percentilesConfigMap.computeIfAbsent(appName, f -> {
            Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_PERCENTILES_PROP, String.class);

            return (input.isPresent()) ? MetricPercentileConfiguration.parseMetricPercentiles(input.get()) : null;

        });

        if (computedValues != null && computedValues.size() != 0) {
            MetricPercentileConfiguration retVal = MetricPercentileConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{0}\" with values: {1} ",
                            new Object[] { MP_PERCENTILES_PROP, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }

    /**
     * Returns the matching {@link HistogramBucketConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link HistogramBucketConfiguration} object if it exists, null otherwise
     */
    public synchronized HistogramBucketConfiguration getHistogramBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<HistogramBucketConfiguration> computedValues = histogramBucketsConfigMap.computeIfAbsent(appName,
                                                                                                            f -> {
                                                                                                                Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_HISTOGRAM_BUCKET_PROP,
                                                                                                                                                                                     String.class);

                                                                                                                return (input.isPresent()) ? HistogramBucketConfiguration.parse(input.get()) : null;

                                                                                                            });

        if (computedValues != null && computedValues.size() != 0) {
            HistogramBucketConfiguration retVal = HistogramBucketConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_HISTOGRAM_BUCKET_PROP, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }

    /**
     * Returns the matching {@link TimerBucketConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link TimerBucketConfiguration} object if it exists, null otherwise
     */
    public synchronized TimerBucketConfiguration getTimerBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<TimerBucketConfiguration> computedValues = timerBucketsConfigMap.computeIfAbsent(appName, f -> {
            Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_TIMER_BUCKET_PROP, String.class);

            return (input.isPresent()) ? TimerBucketConfiguration.parse(input.get()) : null;

        });

        if (computedValues != null && computedValues.size() != 0) {
            TimerBucketConfiguration retVal = TimerBucketConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_TIMER_BUCKET_PROP, metricName, retVal });
            }
            return retVal;
        }

        return null;
    }

    /**
     * Returns the matching {@link DefaultBucketConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link DefaultBucketConfiguration} object if it exists, null otherwise
     */
    public synchronized DefaultBucketConfiguration getDefaultBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<DefaultBucketConfiguration> computedValues = defaultBucketConfigMap.computeIfAbsent(appName, f -> {
            Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_DEFAULT_BUCKET_PROP, String.class);

            return (input.isPresent()) ? DefaultBucketConfiguration.parse(input.get()) : null;

        });

        if (computedValues != null && computedValues.size() != 0) {
            DefaultBucketConfiguration retVal = DefaultBucketConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_DEFAULT_BUCKET_PROP, metricName, retVal });
            }
            return retVal;
        }

        return null;

    }

    /**
     * Returns the matching {@link HistogramBucketMaxConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link HistogramBucketMaxConfiguration} object if it exists, null otherwise
     */
    public synchronized HistogramBucketMaxConfiguration getDefaultHistogramMaxBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<HistogramBucketMaxConfiguration> computedValues = defaultHistogramBucketMaxConfig.computeIfAbsent(appName, f -> {
            Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_HISTOGRAM_MAX_CONFIG,
                                                                                 String.class);

            return (input.isPresent()) ? HistogramBucketMaxConfiguration.parse(input.get()) : null;

        });

        if (computedValues != null && computedValues.size() != 0) {
            HistogramBucketMaxConfiguration retVal = HistogramBucketMaxConfiguration.matches(computedValues,
                                                                                             metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_HISTOGRAM_MAX_CONFIG, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }

    /**
     * Returns the matching {@link HistogramBucketMinConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link HistogramBucketMinConfiguration} object if it exists, null otherwise
     */
    public synchronized HistogramBucketMinConfiguration getDefaultHistogramMinBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<HistogramBucketMinConfiguration> computedValues = defaultHistogramBucketMinConfig.computeIfAbsent(appName, f -> {
            Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_HISTOGRAM_MIN_CONFIG,
                                                                                 String.class);

            return (input.isPresent()) ? HistogramBucketMinConfiguration.parse(input.get()) : null;

        });

        if (computedValues != null && computedValues.size() != 0) {
            HistogramBucketMinConfiguration retVal = HistogramBucketMinConfiguration.matches(computedValues,
                                                                                             metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_HISTOGRAM_MIN_CONFIG, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }

    /**
     * Returns the matching {@link TimerBucketMaxConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link TimerBucketMaxConfiguration} object if it exists, null otherwise
     */
    public synchronized TimerBucketMaxConfiguration getDefaultTimerMaxBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<TimerBucketMaxConfiguration> computedValues = defaultTimerBucketMaxConfig.computeIfAbsent(appName,
                                                                                                             f -> {
                                                                                                                 Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_TIMER_MAX_CONFIG,
                                                                                                                                                                                      String.class);

                                                                                                                 return (input.isPresent()) ? TimerBucketMaxConfiguration.parse(input.get()) : null;

                                                                                                             });

        if (computedValues != null && computedValues.size() != 0) {
            TimerBucketMaxConfiguration retVal = TimerBucketMaxConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_TIMER_MAX_CONFIG, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }

    /**
     * Returns the matching {@link TimerBucketMinConfiguration} object if it exists, null otherwise
     *
     * @param metricName the metric name to check configuration against
     * @return the matching {@link TimerBucketMinConfiguration} object if it exists, null otherwise
     */
    public synchronized TimerBucketMinConfiguration getDefaultTimerMinBucketConfiguration(String metricName) {

        String appName = getApplicationName();

        Collection<TimerBucketMinConfiguration> computedValues = defaultTimerBucketMinConfig.computeIfAbsent(appName,
                                                                                                             f -> {
                                                                                                                 Optional<String> input = ConfigProvider.getConfig().getOptionalValue(MP_TIMER_MIN_CONFIG,
                                                                                                                                                                                      String.class);

                                                                                                                 return (input.isPresent()) ? TimerBucketMinConfiguration.parse(input.get()) : null;

                                                                                                             });

        if (computedValues != null && computedValues.size() != 0) {
            TimerBucketMinConfiguration retVal = TimerBucketMinConfiguration.matches(computedValues, metricName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, null,
                            "Returning \"{0}\" configuration for metric:\"{1}\" with values: {2} ",
                            new Object[] { MP_TIMER_MIN_CONFIG, metricName, retVal });
            }
            return retVal;
        }
        return null;

    }
//
//    /**
//     *
//     * @return the application name if it can be resolved, null otherwise
//     */
//    private String getApplicationName() {
//        String appName = null;
//        if (applicationName != null) {
//            appName = applicationName;
//        }
//        return appName;
//    }
//

    /**
     * Leveraging the Thread Context Class Loader to resolve the application name from the component metadata
     *
     * @return String the application name; can be null
     */
    private String getApplicationName() {
        com.ibm.ws.runtime.metadata.ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (metaData != null) {
            com.ibm.websphere.csi.J2EEName name = metaData.getJ2EEName();
            if (name != null) {
                return name.getApplication();
            }
        }
        return null;
    }

}
