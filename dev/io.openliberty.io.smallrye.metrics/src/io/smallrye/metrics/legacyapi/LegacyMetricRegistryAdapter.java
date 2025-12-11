/*
 *   Copyright 2020, 2024 Red Hat, Inc, and individual contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
/*
 * Liberty changes are enclosed by LIBERTY CHANGE START and LIBERTY CHANGE END
 */
package io.smallrye.metrics.legacyapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.smallrye.metrics.setup.ApplicationNameResolver;
import io.smallrye.metrics.setup.config.MetricsConfigurationManager;

public class LegacyMetricRegistryAdapter implements MetricRegistry {

    private static final String CLASS_NAME = LegacyMetricRegistryAdapter.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private final String scope;
    private final MeterRegistry registry;

    public static final String MP_APPLICATION_NAME_TAG = "mp_app";

    public static final String MP_SCOPE_TAG = "mp_scope";

    protected static final String MP_APPLICATION_NAME_VAR = "mp.metrics.appName";

    protected static final String MP_DEFAULT_APPLICATION_NAME_VAR = "mp.metrics.defaultAppName";
    protected volatile static io.micrometer.core.instrument.Tag DEFAULT_APP_NAME_TAG = null;

    /*
     * Set by user on the server-level with MP Config property mp.metrics.defaultAppName
     */
    private final String defaultAppNameValue;

    private final Map<MetricDescriptor, MeterHolder> constructedMeters = new ConcurrentHashMap<>();
    private final Map<String, MpMetadata> metadataMap = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<String, io.micrometer.core.instrument.Tag> applicationMPConfigAppNameTagCache;

    // LIBERTY CHANGE START
    /*
     * Liberty change. Previously, the second param type of ConcurrentLinkedQueue lead to memory leak.
     */
    protected final ConcurrentHashMap<String, Set<MetricID>> applicationMap;
    // LIBERTY CHANGE END

    protected final ApplicationNameResolver appNameResolver;

    protected final boolean isAppnameResolverPresent;

    private MemberToMetricMappings memberToMetricMappings;

    protected static io.micrometer.core.instrument.Tag[] SERVER_LEVEL_MPCONFIG_APPLICATION_NAME_TAG = null;

    protected static final String GLOBAL_TAG_MALFORMED_EXCEPTION = "Malformed list of Global Tags. Tag names "
                                                                   + "must match the following regex [a-zA-Z_][a-zA-Z0-9_]*." + " Global Tag values must not be empty."
                                                                   + " Global Tag values MUST escape equal signs `=` and commas `,`" + " with a backslash `\\` ";

    protected static final String GLOBAL_TAGS_VARIABLE = "mp.metrics.tags";

    /**
     * This static Tag[] represents the server level global tags retrieved from MP Config for
     * mp.metrics.tags. This value will be 'null' when not initialized. If during initialization and no
     * global tag has been resolved this will be to an array of size 0. Using an array of size 0 is to
     * represent that an attempt on start up was made to resolve the value, but none was found. This
     * prevents later instantiations of MetricRegistry to avoid attempting to resolve the MP Config
     * value for the slight performance boon.
     *
     * This server level value will not change at all throughout the life time of the server as it is
     * defined by env vars or sys props.
     */
    protected static io.micrometer.core.instrument.Tag[] SERVER_LEVEL_MPCONFIG_GLOBAL_TAGS = null;

    public MeterRegistry getPrometheusMeterRegistry() {
        return registry;
    }

    /**
     * Associates a metric's MetricID to a specific application if an application name can be resolved.
     *
     * @param metricDescriptor MetricDescriptor of metric
     */
    public void addNameToApplicationMap(MetricDescriptor metricDescriptor) {
        if (isAppnameResolverPresent)
            addNameToApplicationMap(metricDescriptor.toMetricID(), appNameResolver.getApplicationName());
    }

    /**
     * Associates a metric's MetricID to a specific application if an application name can be resolved.
     *
     * @param MetricID MetricID of metric
     */
    public void addNameToApplicationMap(MetricID MetricID) {
        if (isAppnameResolverPresent)
            addNameToApplicationMap(MetricID, appNameResolver.getApplicationName());
    }

    /**
     * Adds the MetricID to an application map given the application name.
     * This map is not a complete list of metrics owned by an application,
     * produced metrics are managed in the MetricsExtension
     *
     * @param metricID metric ID of metric that was added
     * @param appName  applicationName
     */
    public void addNameToApplicationMap(MetricID metricID, String appName) {
        final String METHOD_NAME = "addNameToApplicationMap";

        /*
         * - Base metrics (or vendor metrics)
         * - Vendor does not support multi-application deployment and/or
         * no AppnameResolver was provided.
         */
        if (appName == null)
            return;

        // LIBERTY CHANGE START
        /*
         * Liberty change. Previously, the second param type of ConcurrentLinkedQueue lead to memory leak.
         */

        Set<MetricID> set = applicationMap.get(appName);
        if (set == null) {
            Set<MetricID> newSet = Collections.newSetFromMap(new ConcurrentHashMap<MetricID, Boolean>());
            set = applicationMap.putIfAbsent(appName, newSet);
            if (set == null)
                set = newSet;
        }
        set.add(metricID);
        // LIBERTY CHANGE END
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
                        String.format("Mapped MetricID [id= %s] to application \"%s\"", metricID, appName));
        }
    }

    public void unRegisterApplicationMetrics() {
        unRegisterApplicationMetrics(appNameResolver.getApplicationName());
    }

    public void unRegisterApplicationMetrics(String appName) {

        /*
         * - Base metrics (or vendor metrics)
         * - Vendor does not support multi-application deployment and/or
         * no AppnameResolver was provided.
         */
        if (appName == null) {
            return;
        }
        // LIBERTY CHANGE START
        /*
         * Liberty change. Previously, the second Parameter type of ConcurrentLinkedQueue lead to memory leak.
         */
        Set<MetricID> set = applicationMap.remove(appName);
        // LIBERTY CHANGE END
        if (set != null) {
            for (MetricID metricID : set) {
                remove(metricID);
            }
        }

        MetricsConfigurationManager.getInstance().removeConfiguration(appName);

    }

    public LegacyMetricRegistryAdapter(String scope, MeterRegistry registry, ApplicationNameResolver appNameResolver) {
        /*
         * Note: if ApplicationNameResolver is passed through as Java Reflection Proxy object,
         * can only be checked if its is "null".
         * Trying any other operations would lead to an Exception (i.e. equals())
         */
        if (appNameResolver == null) {
            this.appNameResolver = ApplicationNameResolver.DEFAULT;
            isAppnameResolverPresent = false;
        } else {
            this.appNameResolver = appNameResolver;
            isAppnameResolverPresent = true;
        }

        this.scope = scope;
        this.registry = registry;

        applicationMPConfigAppNameTagCache = new ConcurrentHashMap<String, io.micrometer.core.instrument.Tag>();

        // LIBERTY CHANGE START
        /*
         * Liberty change. Previously, the second param type of ConcurrentLinkedQueue lead to memory leak.
         */
        applicationMap = new ConcurrentHashMap<String, Set<MetricID>>();
        // LIBERTY CHANGE END

        defaultAppNameValue = resolveMPConfigDefaultAppNameTag();

        resolveMPConfigGlobalTagsByServer();

        if (scope != BASE_SCOPE && scope != VENDOR_SCOPE) {
            memberToMetricMappings = new MemberToMetricMappings();
        }
    }

    private synchronized io.micrometer.core.instrument.Tag[] resolveMPConfigGlobalTagsByServer() {

        final String METHOD_NAME = "resolveMPConfigGlobalTagsByServer";
        if (SERVER_LEVEL_MPCONFIG_GLOBAL_TAGS == null) {

            // Using MP Config to retreive the mp.metrics.tags Config value
            Optional<String> globalTags = ConfigProvider.getConfig().getOptionalValue(GLOBAL_TAGS_VARIABLE,
                                                                                      String.class);

            if (globalTags.isPresent()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD_NAME, String.format(
                                                                                   "MicroProfile Config value for \"%s\" resolved to be: %s", GLOBAL_TAGS_VARIABLE,
                                                                                   globalTags.get()));
                }
            }

            // evaluate if there exists tag values or set tag[0] to be null for no value;
            SERVER_LEVEL_MPCONFIG_GLOBAL_TAGS = (globalTags.isPresent()) ? parseGlobalTags(globalTags.get()) : new io.micrometer.core.instrument.Tag[0];
        }
        return (SERVER_LEVEL_MPCONFIG_GLOBAL_TAGS.length == 0) ? null : SERVER_LEVEL_MPCONFIG_GLOBAL_TAGS;
    }

    /**
     * This will return server level global tag i.e defined in env var or sys props
     *
     * Will return null if no MP Config value is set for the mp.metrics.tags on the server level
     *
     * @return Tag[] The server wide global tag; can return null
     */
    private static io.micrometer.core.instrument.Tag[] parseGlobalTags(String globalTags) {
        if (globalTags == null || globalTags.length() == 0) {
            return null;
        }
        String[] kvPairs = globalTags.split("(?<!\\\\),");

        io.micrometer.core.instrument.Tag[] arrayOfTags = new io.micrometer.core.instrument.Tag[kvPairs.length];
        int count = 0;
        for (String kvString : kvPairs) {

            if (kvString.length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String[] keyValueSplit = kvString.split("(?<!\\\\)=");

            if (keyValueSplit.length != 2 || keyValueSplit[0].length() == 0 || keyValueSplit[1].length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String key = keyValueSplit[0];
            String value = keyValueSplit[1];

            if (!key.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new IllegalArgumentException("Invalid Tag name. Tag names must match the following regex " + "[a-zA-Z_][a-zA-Z0-9_]*");
            }
            value = value.replace("\\,", ",");
            value = value.replace("\\=", "=");

            arrayOfTags[count] = io.micrometer.core.instrument.Tag.of(key, value);
            count++;
        }
        return arrayOfTags;
    }

    /**
     * Combines metric tags with tag defined for MP config mp.metrics.appname property.
     *
     * @param tags the application tags to be merged with the MP Config mp.metrics.appName tag
     * @return combined Tag array of the MP Config mp.metrics.appName tag with application tags; can return null
     */
    private Tags combineApplicationTagsWithMPConfigAppNameTag(Tags tags) {
        io.micrometer.core.instrument.Tag mpConfigAppTag = resolveMPConfigAppNameTag();

        if (mpConfigAppTag != null) {
            Map<String, String> tagMap = new HashMap<String, String>();
            tagMap.put(mpConfigAppTag.getKey(), mpConfigAppTag.getValue());

            /*
             * Application Metric tags are put into the map second
             * this will over write any conflicting tags. This is similar
             * to the old behaviour when MetricID auto-resolved MP Config tags
             * it would resolve MP COnfig tags first then add application tags
             */
            for (io.micrometer.core.instrument.Tag tag : tags) {
                tagMap.put(tag.getKey(), tag.getValue());
            }

            Tags result = Tags.empty();
            for (Entry<String, String> entry : tagMap.entrySet()) {
                result = result.and(entry.getKey(), entry.getValue());
            }

            tags = result;

        }
        return tags;

    }

    private String resolveMPConfigDefaultAppNameTag() {

        Optional<String> configVal = ConfigProvider.getConfig().getOptionalValue(MP_DEFAULT_APPLICATION_NAME_VAR,
                                                                                 String.class);

        return (configVal.isPresent()) ? configVal.get().trim() : null;
    }

    /**
     * This method will retrieve cached tag values for the mp.metrics.appName or resolve it and cache it
     *
     * @return The application level MP Config mp.metrics.appName tag of the application; Or if it exists the server level
     *         value; Or null
     */
    private synchronized io.micrometer.core.instrument.Tag resolveMPConfigAppNameTag() {

        String appName = appNameResolver.getApplicationName();

        /*
         * If appName is null then we aren't running in an application context.
         * This is possible when resolving metrics for BASE or VENDOR.
         *
         * Since we're using a ConcurrentHashMap, can't store a null key and don't want
         * to risk making up a key a user might use as their appName. So we'll call two methods
         * that are similar. resolveAppTagByServer() will, however, store to a static array.
         *
         */
        io.micrometer.core.instrument.Tag tag = (appName == null) ? resolveMPConfigAppNameTagByServer() : resolveMPConfigAppNameTagByApplication(appName);

        return (tag != null) ? tag : (defaultAppNameValue != null) ? io.micrometer.core.instrument.Tag.of(MP_APPLICATION_NAME_TAG, defaultAppNameValue) : null;
    }

    /**
     * This will return server level application tag
     * i.e defined in env var or sys props
     *
     * Will return null if no MP Config value is set
     * for the mp.metrics.appName on the server level
     *
     * @return Tag The server wide application tag; can return null
     */
    private synchronized io.micrometer.core.instrument.Tag resolveMPConfigAppNameTagByServer() {
        if (SERVER_LEVEL_MPCONFIG_APPLICATION_NAME_TAG == null) {
            SERVER_LEVEL_MPCONFIG_APPLICATION_NAME_TAG = new io.micrometer.core.instrument.Tag[1];

            //Using MP Config to retrieve the mp.metrics.appName Config value
            Optional<String> applicationName = ConfigProvider.getConfig().getOptionalValue(MP_APPLICATION_NAME_VAR,
                                                                                           String.class);

            //Evaluate if there exists a tag value or set tag[0] to be null for no value;
            SERVER_LEVEL_MPCONFIG_APPLICATION_NAME_TAG[0] = (applicationName.isPresent()) ? io.micrometer.core.instrument.Tag.of(MP_APPLICATION_NAME_TAG,
                                                                                                                                 applicationName.get()) : null;
        }
        return SERVER_LEVEL_MPCONFIG_APPLICATION_NAME_TAG[0];
    }

    /**
     * This will return the MP Config value for
     * mp.metrics.appName for the application
     * that the current TCCL is running for
     *
     * @param appName the application name to look up from cache
     * @return Tag The mp.metrics.appName MP Config value associated to the appName; can return null if non exists
     */
    private synchronized io.micrometer.core.instrument.Tag resolveMPConfigAppNameTagByApplication(String appName) {
        //Return cached value
        if (!applicationMPConfigAppNameTagCache.containsKey(appName)) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            //Using MP Config to retreive the mp.metrics.appName Config value
            Optional<String> applicationName = ConfigProvider.getConfig(classLoader).getOptionalValue(MP_APPLICATION_NAME_VAR,
                                                                                                      String.class);

            /*
             * Evaluate if there exists a tag value. If there is not then we must create an "invalid" Tag to represent no value
             * resolved.
             * This is used later to return a null value.
             * This is due the use of ConcurrentHashMap and we cannot set a null key.
             */
            io.micrometer.core.instrument.Tag appTag = (applicationName.isPresent()) ? io.micrometer.core.instrument.Tag.of(MP_APPLICATION_NAME_TAG,
                                                                                                                            applicationName.get()) : io.micrometer.core.instrument.Tag.of("null",
                                                                                                                                                                                          "null");

            //Cache the value
            applicationMPConfigAppNameTagCache.put(appName, appTag);
        }

        //Perhaps we don't really need a concurrent hashmap.. so we can avoid this.
        io.micrometer.core.instrument.Tag returnTag;
        return ((returnTag = applicationMPConfigAppNameTagCache.get(appName)).getKey().equals("null")) ? null : returnTag;
    }

    public LegacyMetricRegistryAdapter(String scope, MeterRegistry registry) {
        this(scope, registry, ApplicationNameResolver.DEFAULT);
    }

    @Override
    public Counter counter(String name) {
        return internalCounter(internalGetMetadata(name),
                               new MetricDescriptor(name, withAppTags()));
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalCounter(internalGetMetadata(name),
                               new MetricDescriptor(name, unifiedTags));
    }

    @Override
    public Counter counter(MetricID metricID) {
        String name = metricID.getName();
        return internalCounter(internalGetMetadata(name),
                               new MetricDescriptor(name, withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Counter counter(Metadata metadata) {
        return internalCounter(internalGetMetadata(metadata),
                               new MetricDescriptor(metadata.getName(), withAppTags()));
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalCounter(internalGetMetadata(metadata),
                               new MetricDescriptor(metadata.getName(), unifiedTags));
    }

    private void validateTagNamesMatch(MetricDescriptor id) {
        /*
         * Check that if there are metrics registered with same metric name that the tag
         * set is the same. Specifically that the tag key values are the same. Values
         * can differ.
         */
        for (MetricDescriptor md : constructedMeters.keySet()) {
            if (md.name().equals(id.name) && !id.isTagNamesMatch(md.tags())) {
                throw new IllegalArgumentException(String.format(
                                                                 "The set of tags names provided do not match those of an existing metric with the same name. Provided = %s Existing = %s ",
                                                                 id.tags.toString(), md.tags.toString()));
            }
        }
    }

    CounterAdapter internalCounter(MpMetadata metadata, MetricDescriptor id) {

        validateTagNamesMatch(id);

        CounterAdapter result = checkCast(CounterAdapter.class, metadata,
                                          constructedMeters.computeIfAbsent(id, k -> new CounterAdapter()));

        //LIBERTY CHANGE START
        /*
         * Check if metric is an Openliberty connection pool metric.
         * Do not associate with application if true.
         */
        if (!isLibertyVendorConnectionPoolMetricCounter(id.name())) {
            addNameToApplicationMap(id);
        }
        //LIBERTY CHANGE END

        return result.register(metadata, id, registry, scope, resolveMPConfigGlobalTagsByServer());
    }

    /**
     * This is specifically used for runtimes which may need use of a functional counter.
     * For example, the runtime may want to implement a vendor specific counter metric which
     * relies on values obtained from a Mbeans or MXbeans.
     *
     * @param <T>      object type
     * @param metadata metadata of metric
     * @param obj      object to apply ToDoubleFunction
     * @param func     ToDoubleFunction
     * @param tags     tags of metric
     * @return The functional counter
     */
    public <T> Counter counter(Metadata metadata, T obj, ToDoubleFunction<T> func, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalCounter(internalGetMetadata(metadata), obj, func,
                               new MetricDescriptor(metadata.getName(), unifiedTags));
    }

    <T> FunctionCounterAdapter<T> internalCounter(MpMetadata metadata, T obj, ToDoubleFunction<T> func, MetricDescriptor id) {

        validateTagNamesMatch(id);

        FunctionCounterAdapter<T> result = checkCast(FunctionCounterAdapter.class, metadata,
                                                     constructedMeters.computeIfAbsent(id, k -> new FunctionCounterAdapter(obj, func)));
        //LIBERTY CHANGE START
        /*
         * Check if metric is an Openliberty connection pool metric.
         * Do not associate with application if true.
         */
        if (!isLibertyVendorConnectionPoolMetricCounter(id.name())) {
            addNameToApplicationMap(id);
        }
        //LIBERTY CHANGE END
        return result.register(metadata, id, registry, scope, resolveMPConfigGlobalTagsByServer());
    }

    public <T> Gauge<Double> gauge(String name, T o, ToDoubleFunction<T> f) {
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, withAppTags()), o, f);
    }

    public <T> Gauge<Double> gauge(String name, T o, ToDoubleFunction<T> f, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, unifiedTags), o, f);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String name, T o, Function<T, R> f, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, unifiedTags), o, f);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T o, Function<T, R> f) {
        String name = metricID.getName();
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, withAppTags()), o, f);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T o, Function<T, R> f, Tag... tags) {
        String name = metadata.getName();
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalGauge(internalGetMetadata(metadata),
                             new MetricDescriptor(name, unifiedTags), o, f);
    }

    @SuppressWarnings("unchecked")
    <T> GaugeAdapter<Double> internalGauge(MpMetadata metadata, MetricDescriptor id, T obj, ToDoubleFunction<T> f) {
        validateTagNamesMatch(id);
        GaugeAdapter.DoubleFunctionGauge<T> result = checkCast(GaugeAdapter.DoubleFunctionGauge.class, metadata,
                                                               constructedMeters.computeIfAbsent(id, k -> new GaugeAdapter.DoubleFunctionGauge<>(obj, f)));
        //LIBERTY CHANGE START
        /*
         * Check if metric is an Openliberty connection pool metric.
         * Do not associate with application if true.
         */
        if (!isLibertyVendorConnectionPoolMetricGauge(id.name())) {
            addNameToApplicationMap(id);
        }
        //LIBERTY CHANGE END
        return result.register(metadata, id, registry, scope, resolveMPConfigGlobalTagsByServer());
    }

    //LIBERTY CHANGE START
    /*
     * Checks if the metric being registered matches one of the connection pool vendor metrics provided by Liberty.
     */
    private boolean isLibertyVendorConnectionPoolMetricCounter(String metricName) {
        if (metricName.equalsIgnoreCase("connectionpool.create.total") ||
            metricName.equalsIgnoreCase("connectionpool.destroy.total") ||
            metricName.equalsIgnoreCase("connectionpool.queuedRequests.total") ||
            metricName.equalsIgnoreCase("connectionpool.usedConnections.total")) {
            return true;
        }
        return false;
    }

    /*
     * Checks if the metric being registered matches one of the connection pool vendor metrics provided by Liberty.
     */
    private boolean isLibertyVendorConnectionPoolMetricGauge(String metricName) {
        if (metricName.equalsIgnoreCase("connectionpool.managedConnections") ||
            metricName.equalsIgnoreCase("connectionpool.connectionHandles") ||
            metricName.equalsIgnoreCase("connectionpool.freeConnections") ||
            metricName.equalsIgnoreCase("connectionpool.waitTime.total") ||
            metricName.equalsIgnoreCase("connectionpool.inUseTime.total")) {
            return true;
        }
        return false;
    }
    //LIBERTY CHANGE END

    @SuppressWarnings("unchecked")
    <T, R extends Number> GaugeAdapter<R> internalGauge(MpMetadata metadata, MetricDescriptor id, T obj, Function<T, R> f) {
        validateTagNamesMatch(id);
        GaugeAdapter.FunctionGauge<T, R> result = checkCast(GaugeAdapter.FunctionGauge.class, metadata,
                                                            constructedMeters.computeIfAbsent(id, k -> new GaugeAdapter.FunctionGauge<>(obj, f)));
        //LIBERTY CHANGE START
        /*
         * Check if metric is an Openliberty connection pool metric.
         * Do not associate with application if true.
         */
        if (!isLibertyVendorConnectionPoolMetricGauge(id.name())) {
            addNameToApplicationMap(id);
        }
        //LIBERTY CHANGE END
        return result.register(metadata, id, registry, scope, resolveMPConfigGlobalTagsByServer());
    }

    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> f) {
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, withAppTags()), f);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> f, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, unifiedTags), f);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> f) {
        String name = metricID.getName();
        return internalGauge(internalGetMetadata(name),
                             new MetricDescriptor(name, withAppTags()), f);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> f, Tag... tags) {
        String name = metadata.getName();
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalGauge(internalGetMetadata(metadata),
                             new MetricDescriptor(name, unifiedTags), f);
    }

    @SuppressWarnings("unchecked")
    <T extends Number> GaugeAdapter<T> internalGauge(MpMetadata metadata, MetricDescriptor id, Supplier<T> f) {
        validateTagNamesMatch(id);
        GaugeAdapter<T> result = checkCast(GaugeAdapter.NumberSupplierGauge.class, metadata,
                                           constructedMeters.computeIfAbsent(id, k -> new GaugeAdapter.NumberSupplierGauge<T>(f)));
        //LIBERTY CHANGE START
        /*
         * Check if metric is an Openliberty connection pool metric.
         * Do not associate with application if true.
         */
        if (!isLibertyVendorConnectionPoolMetricGauge(id.name())) {
            addNameToApplicationMap(id);
        }
        //LIBERTY CHANGE END
        return result.register(metadata, id, registry, scope, resolveMPConfigGlobalTagsByServer());
    }

    void bindAnnotatedGauge(AnnotatedGaugeAdapter adapter) {
        MetricDescriptor id = new MetricDescriptor(adapter.name(), adapter.tags());
        AnnotatedGaugeAdapter oops = checkCast(AnnotatedGaugeAdapter.class, adapter.getMetadata(),
                                               constructedMeters.putIfAbsent(id, adapter));
        if (oops == null) {
            metadataMap.put(adapter.name(), adapter.getMetadata());
            adapter.register(id, registry);
        } else {
            throw new IllegalArgumentException(String.format("Gauge %s already exists. (existing='%s', new='%s')",
                                                             adapter.getId(), oops.getTargetName(), adapter.getTargetName()));
        }
    }

    @Override
    public Histogram histogram(String name) {
        return internalHistogram(internalGetMetadata(name),
                                 new MetricDescriptor(name, withAppTags()));
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalHistogram(internalGetMetadata(name),
                                 new MetricDescriptor(name, unifiedTags));
    }

    @Override
    public Histogram histogram(MetricID metricID) {
        String name = metricID.getName();
        return internalHistogram(internalGetMetadata(name),
                                 new MetricDescriptor(name, withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return internalHistogram(internalGetMetadata(metadata),
                                 new MetricDescriptor(metadata.getName(), withAppTags()));
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalHistogram(internalGetMetadata(metadata),
                                 new MetricDescriptor(metadata.getName(), unifiedTags));
    }

    HistogramAdapter internalHistogram(MpMetadata metadata, MetricDescriptor id) {
        validateTagNamesMatch(id);
        HistogramAdapter result = checkCast(HistogramAdapter.class, metadata,
                                            constructedMeters.computeIfAbsent(id, k -> new HistogramAdapter()));
        addNameToApplicationMap(id);
        return result.register(metadata, id, scope, resolveMPConfigGlobalTagsByServer());
    }

    @Override
    public Timer timer(String name) {
        return internalTimer(internalGetMetadataTimers(name),
                             new MetricDescriptor(name, withAppTags()));
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalTimer(internalGetMetadataTimers(name),
                             new MetricDescriptor(name, unifiedTags));
    }

    @Override
    public Timer timer(MetricID metricID) {
        String name = metricID.getName();
        return internalTimer(internalGetMetadataTimers(name),
                             new MetricDescriptor(name, withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Timer timer(Metadata metadata) {
        return internalTimer(internalGetMetadataTimers(metadata),
                             new MetricDescriptor(metadata.getName(), withAppTags()));
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        /*
         * Verify tags before internalGetMetadata().
         * The call withAppTags() can throw an IAE.
         * Don't want to have had created metadata
         * and have it put into the map before that.
         */
        Tags unifiedTags = withAppTags(tags);
        return internalTimer(internalGetMetadataTimers(metadata),
                             new MetricDescriptor(metadata.getName(), unifiedTags));
    }

    TimerAdapter internalTimer(MpMetadata metadata, MetricDescriptor id) {
        validateTagNamesMatch(id);
        TimerAdapter result = checkCast(TimerAdapter.class, metadata,
                                        constructedMeters.computeIfAbsent(id, k -> new TimerAdapter(registry)));
        addNameToApplicationMap(id);
        return result.register(metadata, id, scope, resolveMPConfigGlobalTagsByServer());
    }

    @Override
    public Metric getMetric(MetricID metricID) {
        return constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> asType) {
        return asType.cast(constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray()))));
    }

    @Override
    public Counter getCounter(MetricID metricID) {
        return (Counter) constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Gauge<?> getGauge(MetricID metricID) {
        return (Gauge<?>) constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Histogram getHistogram(MetricID metricID) {
        return (Histogram) constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Timer getTimer(MetricID metricID) {
        return (Timer) constructedMeters.get(new MetricDescriptor(metricID.getName(), withAppTags(metricID.getTagsAsArray())));
    }

    @Override
    public Metadata getMetadata(String name) {
        return metadataMap.get(name);
    }

    @Override
    public boolean remove(String name) {

        boolean isRemoveSuccess = false;
        for (Map.Entry<MetricDescriptor, MeterHolder> e : constructedMeters.entrySet()) {
            if (e.getKey().name().equals(name)) {
                isRemoveSuccess = internalRemove(e.getKey());
            }
        }
        return isRemoveSuccess;

    }

    @Override
    public boolean remove(MetricID metricID) {
        return internalRemove(new MetricDescriptor(metricID));
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {
        for (Map.Entry<MetricDescriptor, MeterHolder> e : constructedMeters.entrySet()) {
            MetricID mid = e.getKey().toMetricID();
            if (metricFilter.matches(mid, e.getValue())) {
                internalRemove(e.getKey());
            }
        }
    }

    boolean internalRemove(MetricDescriptor match) {
        final String METHOD_NAME = "internalRemove";

        MeterHolder holder = constructedMeters.remove(match);

        if (holder != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                            String.format("Removed metric with [id: %s]", match.toMetricID().toString()));
            }

            io.micrometer.core.instrument.Meter meter = Metrics.globalRegistry.remove(holder.getMeter());
            if (meter != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD_NAME, String.format("Removed from the Micrometer global registry a meter with MeterId [id= %s]",
                                                                                   meter.getId()));
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD_NAME, String.format(
                                                                                   "Attempted to remove a meter with the corresponding MetricID [id= %s] from the Micrometer global registry, but does not exist.",
                                                                                   match.toMetricID()));
                }
            }

            // Remove associated metadata if this is the last MP Metric left with that name
            if (constructedMeters.keySet().stream().noneMatch(id -> id.name.equals(match.name))) {
                metadataMap.remove(match.name);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD_NAME,
                                String.format("Removed metadata for [name: %s]", match.name));
                }
            }
        }
        return holder != null;
    }

    @Override
    public SortedSet<String> getNames() {
        return new TreeSet<>(metadataMap.keySet());
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        SortedSet<MetricID> out = new TreeSet<>();
        for (MetricDescriptor key : constructedMeters.keySet()) {
            out.add(key.toMetricID());
        }
        return out;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return getMetrics(Gauge.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return getMetrics(Counter.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        return getMetrics(Histogram.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return getMetrics(Timer.class, metricFilter);
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter filter) {
        SortedMap<MetricID, Metric> out = new TreeMap<>();
        for (Map.Entry<MetricDescriptor, MeterHolder> e : constructedMeters.entrySet()) {
            MetricID mid = e.getKey().toMetricID();
            if (filter.matches(mid, e.getValue())) {
                out.put(e.getKey().toMetricID(), e.getValue());
            }
        }
        return out;
    }

    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> ofType, MetricFilter filter) {
        SortedMap<MetricID, T> out = new TreeMap<>();
        for (Map.Entry<MetricDescriptor, MeterHolder> e : constructedMeters.entrySet()) {
            if (ofType.isAssignableFrom(e.getValue().getClass())) {
                MetricID mid = e.getKey().toMetricID();
                if (filter.matches(mid, e.getValue())) {
                    out.put(e.getKey().toMetricID(), (T) e.getValue());
                }
            }
        }
        return out;
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        SortedMap<MetricID, Metric> out = new TreeMap<>();
        for (Map.Entry<MetricDescriptor, MeterHolder> e : constructedMeters.entrySet()) {
            out.put(e.getKey().toMetricID(), e.getValue());
        }
        return out;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(metadataMap);
    }

    @Override
    public String getScope() {
        return scope;
    }

    /**
     * Must be called before any internalGetMetadata calls
     * We may throw an IllegalArgumentException. So we don't
     * want metadata to be registered if it was not necessary.
     *
     * @param tags Tags to be combined with
     * @return tags combined with global tags and mp_app if available
     */
    public Tags withAppTags(Tag... tags) {

        Tags out = Tags.empty();

        if (tags != null) {
            for (Tag t : tags) {
                /*
                 * Need to check if tags being passed in are
                 * 'mp_scope' or 'mp_app'; throw IAE as per spec
                 *
                 * mp_scope is provided to micrometer registry
                 * during metric/meter registration in the adapters
                 *
                 * mp_app is resolved with the resolveMPConfigAppNameTag()
                 * logic
                 */
                if (t.getTagName().equals(MP_APPLICATION_NAME_TAG)
                    || t.getTagName().equals(MP_SCOPE_TAG)) {
                    throw new IllegalArgumentException("Can not use "
                                                       + "reserved tag names: \"mp_scope\" "
                                                       + "or \"mp_app\"");
                }
                out = out.and(t.getTagName(), t.getTagValue());
            }
        }

        out = combineApplicationTagsWithMPConfigAppNameTag(out);

        return out;
    }

    public Tag[] scopeTagsLegacy() {
        return new Tag[] { new Tag("scope", this.scope) };
    }

    private MpMetadata internalGetMetadata(String name) {

        MpMetadata result = metadataMap.computeIfAbsent(name, k -> new MpMetadata(name));

        /*
         * Check that metadata of metric being registered/retrieved matches existing
         * existing metadata (if it exists)
         */
        if (!result.equals(MpMetadata.sanitize(new MpMetadata(name)))) {
            throw new IllegalArgumentException(String.format("Existing metadata (%s) does not match with supplied metadata (%s)",
                                                             result.toString(), new MpMetadata(name).toString()));
        }

        return result;
    }

    /*
     * Temporary work around due to https://github.com/eclipse/microprofile-metrics/issues/760
     */
    private MpMetadata internalGetMetadata(Metadata metadata) {
        MpMetadata result = metadataMap.computeIfAbsent(metadata.getName(), k -> MpMetadata.sanitize(metadata));

        /*
         * Check that metadata of metric being registered/retrieved matches existing
         * existing metadata (if it exists)
         */
        if (!result.equals(MpMetadata.sanitize(metadata))) {
            throw new IllegalArgumentException(String.format("Existing metadata (%s) does not match with supplied metadata (%s)",
                                                             result.toString(), metadata.toString()));
        }

        return result;
    }

    /*
     * Temporary work around due to https://github.com/eclipse/microprofile-metrics/issues/760
     */
    private MpMetadata internalGetMetadataTimers(String name) {

        MpMetadata result = metadataMap.computeIfAbsent(name, k -> new MpMetadata(name));

        /*
         * Check that metadata of metric being registered/retrieved matches existing
         * existing metadata (if it exists)
         */
        if (!result.equalsTimers(MpMetadata.sanitize(new MpMetadata(name)))) {
            throw new IllegalArgumentException(String.format("Existing metadata (%s) does not match with supplied metadata (%s)",
                                                             result.toString(), new MpMetadata(name).toString()));
        }

        return result;
    }

    private MpMetadata internalGetMetadataTimers(Metadata metadata) {
        MpMetadata result = metadataMap.computeIfAbsent(metadata.getName(), k -> MpMetadata.sanitize(metadata));

        /*
         * Check that metadata of metric being registered/retrieved matches existing
         * existing metadata (if it exists)
         */
        if (!result.equalsTimers(MpMetadata.sanitize(metadata))) {
            throw new IllegalArgumentException(String.format("Existing metadata (%s) does not match with supplied metadata (%s)",
                                                             result.toString(), metadata.toString()));
        }

        return result;
    }

    <T> T checkCast(Class<T> type, MpMetadata metadata, MeterHolder o) {
        try {
            return type.cast(o);
        } catch (ClassCastException cce) {
            throw new IllegalStateException(String.format("Metric (%s) already defined using a different type (%s)",
                                                          metadata.name, o.getMeter().getId().getType()), cce);
        }
    }

    public MemberToMetricMappings getMemberToMetricMappings() {
        return memberToMetricMappings;
    }

}
