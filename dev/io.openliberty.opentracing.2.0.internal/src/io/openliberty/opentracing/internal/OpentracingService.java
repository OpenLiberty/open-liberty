/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.opentracing.internal.filters.ExcludeFilter;
import io.openliberty.opentracing.internal.filters.ExcludePathFilter;
import io.openliberty.opentracing.internal.filters.SpanFilter;
import io.openliberty.opentracing.internal.filters.SpanFilterType;
import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * Opentracing configuration and static utilities such as running filters on a URI.
 */
@Component(configurationPid = "com.ibm.ws.opentracing", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class OpentracingService {

    private static final TraceComponent tc = Tr.register(OpentracingService.class);
    private static String excludeFilterString = null;

    private static final String MP_METRICS_ENDPOINT = "/metrics";
    private static final String MP_METRICS_BASE_ENDPOINT = "/metrics/base|/metrics/base/.*";
    private static final String MP_METRICS_VENDOR_ENDPOINT = "/metrics/vendor|/metrics/vendor/.*";
    private static final String MP_METRICS_APPLICATION_ENDPOINT = "/metrics/application|/metrics/application/.*";
    private static final String MP_HEALTH_ENDPOINT = "/health";
    private static final String MP_OPENAPI_ENDPOINT = "/openapi";

    /**
     * List of all active span filters.
     */
    private static volatile SpanFilter[] allFilters = new SpanFilter[0];

    /**
     * First configuration loaded.
     *
     * @param map Configuration properties.
     */
    @Activate
    protected void activate(Map<String, Object> map) {
        modified(map);
    }

    /**
     * Configuration dynamically updated.
     *
     * @param map Configuration properties.
     */
    @Modified
    protected void modified(Map<String, Object> map) {

        String filterString = OpentracingConfiguration.getServerSkipPattern();
        updateFilters(filterString);
    }

    private static void updateFilters(String filterString) {
        excludeFilterString = filterString;

        // Build up the list of filters in a local list, then convert that to an array
        // and assign to the static reference. This is done to avoid creating an iterator
        // in the main path in `process`.
        List<SpanFilter> filters = new ArrayList<SpanFilter>();

        // Pre-defined exclude filters for MicroProfile endpoints
        processFilters(filters, MP_METRICS_ENDPOINT, "excludeSpans", ExcludeFilter.class);
        processFilters(filters, MP_METRICS_BASE_ENDPOINT, "excludeSpans", ExcludeFilter.class);
        processFilters(filters, MP_METRICS_VENDOR_ENDPOINT, "excludeSpans", ExcludeFilter.class);
        processFilters(filters, MP_METRICS_APPLICATION_ENDPOINT, "excludeSpans", ExcludeFilter.class);
        processFilters(filters, MP_HEALTH_ENDPOINT, "excludeSpans", ExcludeFilter.class);
        processFilters(filters, MP_OPENAPI_ENDPOINT, "excludeSpans", ExcludeFilter.class);

        // Exclude filters
        // Use ExcludePathFilter here because MicroProfile OpenTracing specification does not support
        // multiple applications
        if (filterString != null) {
            processFilters(filters, filterString, "excludeSpans", ExcludePathFilter.class);
        }

        // Include filters
//      processFilters(filters, map, configAdmin, "includeSpans", IncludeFilter.class);

        SpanFilter[] finalFilters = new SpanFilter[filters.size()];
        filters.toArray(finalFilters);

        allFilters = finalFilters;

    }

    // Make sure ConfigProviderResolver is started
    @Reference(service = ConfigProviderResolver.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setConfigProvider(ConfigProviderResolver configResolver) {}

    /**
     * Check the configuration for filters of a particular type.
     *
     * @param filters     The resulting list of filters.
     * @param map         Configuration properties.
     * @param configAdmin Service to get child configurations.
     * @param childNames  The name of the configuration element to check for.
     * @param impl        The filter class to instantiate if an element is found.
     */
    private static void processFilters(List<SpanFilter> filters, String pattern, String childNames, Class<? extends SpanFilter> impl) {

        final String methodName = "processFilters";

        try {
            SpanFilterType type = SpanFilterType.INCOMING;
            boolean ignoreCase = false;
            boolean regex = true;

            SpanFilter filter = (SpanFilter) Class.forName(impl.getName()).getConstructor(String.class, SpanFilterType.class, boolean.class,
                                                                                          boolean.class).newInstance(pattern, type,
                                                                                                                     ignoreCase, regex);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "filter " + filter);
            }

            filters.add(filter);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException
                        | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Return true if a span for the specified URI and type should be included.
     *
     * @param uri  The URI of the request.
     * @param type The type of the request.
     * @return true if a span for the specified URI and type should be included.
     */
    public static boolean process(final URI uri, final String path, final SpanFilterType type) {
        final String methodName = "process";

        String newExcludeFilterString = OpentracingConfiguration.getServerSkipPattern();
        if (!compare(excludeFilterString, newExcludeFilterString)) {
            updateFilters(newExcludeFilterString);
        }

        boolean result = true;

        // Copy the static reference locally so that it doesn't matter if the static list
        // is updated while we're processing since that will just overwrite the reference
        final SpanFilter[] filters = allFilters;

        for (int i = 0; i < filters.length; i++) {
            result = filters[i].process(result, uri, path, type);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "filter " + filters[i] + " set result to " + result);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Checking to see if this request should be excluded. request uri=" + uri + ", path=" + path + " result=" + (result ? "INCLUDE" : "EXCLUDE"));
        }

        return result;

    }

    /**
     * "An Tags.ERROR tag SHOULD be added to a Span on failed operations.
     * It means for any server error (5xx) codes. If there is an exception
     * object available the implementation SHOULD also add logs event=error
     * and error.object=<error object instance> to the active span."
     * https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing.asciidoc#server-span-tags
     *
     * @param span      The span to add the information to.
     * @param exception Optional exception details.
     */
    public static void addSpanErrorInfo(Span span, Throwable exception) {
        String methodName = "addSpanErrorInfo";

        span.setTag(Tags.ERROR.getKey(), true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " error", Boolean.TRUE);
        }

        if (exception != null) {
            Map<String, Object> log = new HashMap<>();
            // https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table
            log.put("event", "error");

            // Throwable implements Serializable so all exceptions are serializable
            log.put("error.object", exception);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " adding log entry", log);
            }

            span.log(log);
        }
    }

    private static boolean compare(String str1, String str2) {
        if (str1 == null || str2 == null)
            return str1 == str2;
        return str1.equals(str2);
    }

}
