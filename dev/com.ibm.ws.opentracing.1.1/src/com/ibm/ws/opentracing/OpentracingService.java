/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.opentracing.filters.SpanFilter;
import com.ibm.ws.opentracing.filters.SpanFilterType;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * Opentracing configuration and static utilities such as running filters on a URI.
 */
@Component(configurationPid = "com.ibm.ws.opentracing", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class OpentracingService {

    private static final TraceComponent tc = Tr.register(OpentracingService.class);

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

        // Build up the list of filters in a local list, then convert that to an array
        // and assign to the static reference. This is done to avoid creating an iterator
        // in the main path in `process`.
        List<SpanFilter> filters = new ArrayList<SpanFilter>();

        /*
         * Removing filter processing until microprofile spec for it is approved. Expect to add this code
         * back in in 1Q18 - smf
         */
//        processFilters(filters, map, configAdmin, "excludeSpans", ExcludeFilter.class);
//        processFilters(filters, map, configAdmin, "includeSpans", IncludeFilter.class);

        SpanFilter[] finalFilters = new SpanFilter[filters.size()];
        filters.toArray(finalFilters);

        allFilters = finalFilters;
    }

    /**
     * Check the configuration for filters of a particular type.
     *
     * @param filters The resulting list of filters.
     * @param map Configuration properties.
     * @param configAdmin Service to get child configurations.
     * @param childNames The name of the configuration element to check for.
     * @param impl The filter class to instantiate if an element is found.
     *            /
     *            private void processFilters(List<SpanFilter> filters, Map<String, Object> map, ConfigurationAdmin configAdmin, String childNames, Class<? extends SpanFilter>
     *            impl) {
     *
     *            final String methodName = "processFilters";
     *
     *            String[] children = (String[]) map.get(childNames);
     *            if (children != null) {
     *            for (String child : children) {
     *            try {
     *            Configuration config = configAdmin.getConfiguration(child, null);
     *            Dictionary<String, Object> childProperties = config.getProperties();
     *
     *            String pattern = (String) childProperties.get("pattern");
     *            SpanFilterType type = SpanFilterType.valueOf(((String) childProperties.get("type")).trim());
     *            boolean ignoreCase = (Boolean) childProperties.get("ignoreCase");
     *            boolean regex = (Boolean) childProperties.get("regex");
     *
     *            SpanFilter filter = (SpanFilter) Class.forName(impl.getName()).getConstructor(String.class, SpanFilterType.class, boolean.class,
     *            boolean.class).newInstance(pattern, type,
     *            ignoreCase, regex);
     *
     *            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
     *            Tr.debug(tc, methodName, "filter " + filter);
     *            }
     *
     *            filters.add(filter);
     *            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException
     *            | InvocationTargetException | IOException e) {
     *            throw new IllegalStateException(e);
     *            }
     *            }
     *            }
     *            }
     */

    /**
     * Return true if a span for the specified URI and type should be included.
     *
     * @param uri The URI of the request.
     * @param type The type of the request.
     * @return true if a span for the specified URI and type should be included.
     */
    public static boolean process(final URI uri, final SpanFilterType type) {
        final String methodName = "process";

        boolean result = true;

        // Copy the static reference locally so that it doesn't matter if the static list
        // is updated while we're processing since that will just overwrite the reference
        final SpanFilter[] filters = allFilters;

        for (int i = 0; i < filters.length; i++) {
            result = filters[i].process(result, uri, type);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "filter " + filters[i] + " set result to " + result);
            }
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
     * @param span The span to add the information to.
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
}
