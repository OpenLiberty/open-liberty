/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.request.timing.ejb.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.config.RequestTimingConfigParser;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.ws.request.timing.config.TimingConfigGroup;

/**
 * Parses {@code <ejbTiming>} child elements from the {@code requestTiming}
 * configuration.
 *
 * <p>Each {@code <ejbTiming>} element may specify:
 * <ul>
 * <li>{@code requestType} — {@code timer}, {@code async}, {@code remote},
 * {@code mdb}, or {@code *} (default, all types).</li>
 * <li>{@code appName} — application (EAR) name filter, default {@code *}</li>
 * <li>{@code moduleName} — EJB module (JAR) name filter, default {@code *}</li>
 * <li>{@code beanName} — EJB component name filter, default {@code *}</li>
 * <li>{@code methodName} — method name filter, default {@code *}</li>
 * <li>{@code slowRequestThreshold} — duration after which a request is considered slow,
 * default is the {@code requestTiming} slow request threshold</li>
 * <li>{@code hungRequestThreshold} — duration after which a request is considered hung,
 * default is the {@code requestTiming} hung request threshold</li>
 * <li>{@code enableThreadDumps} — whether thread dumps are created when a hung request
 * is detected, default {@code true}</li>
 * <li>{@code interruptHungRequests} — whether a hung request is interrupted,
 * default {@code false}</li>
 * </ul>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM",
           service = { RequestTimingConfigParser.class })
public class ConfigParser implements RequestTimingConfigParser {

    private static final TraceComponent tc = Tr.register(ConfigParser.class);

    /** Name of the server.xml child element handled by this parser. */
    public static final String ELEMENT_NAME = "ejbTiming";

    /** Attribute names. */
    public static final String ATTR_REQUEST_TYPE = "requestType";
    public static final String ATTR_APP_NAME = "appName";
    public static final String ATTR_MODULE_NAME = "moduleName";
    public static final String ATTR_BEAN_NAME = "beanName";
    public static final String ATTR_METHOD_NAME = "methodName";

    /** requestType values. */
    public static final String TYPE_TIMER = "timer";
    public static final String TYPE_ASYNC = "async";
    public static final String TYPE_REMOTE = "remote";
    public static final String TYPE_MDB = "mdb";
    public static final String TYPE_ALL = "*";

    /** Probe event types; must match the EVENT_TYPE of the EJB*TransformDescriptor classes in io.openliberty.request.probe.ejb.internal. */
    public static final String EVENT_TYPE_TIMER = "websphere.ejb.timer";
    public static final String EVENT_TYPE_ASYNC = "websphere.ejb.async";
    public static final String EVENT_TYPE_REMOTE = "websphere.ejb.remote";
    public static final String EVENT_TYPE_MDB = "websphere.ejb.mdb";

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    /**
     * Called by {@code RequestTimingService} on every configuration update.
     */
    @Override
    public TimingConfigGroup parseConfiguration(List<Dictionary<String, Object>> configElementList,
                                                long defaultSlowRequestThreshold,
                                                long defaultHungRequestThreshold,
                                                boolean defaultInterruptHungRequest,
                                                boolean defaultEnableThreadDumps) {
        final List<Timing> slowTimings = new LinkedList<>();
        final List<Timing> hungTimings = new LinkedList<>();

        for (Dictionary<String, Object> configElement : configElementList) {
            try {
                parseElement(configElement,
                             defaultSlowRequestThreshold,
                             defaultHungRequestThreshold,
                             defaultInterruptHungRequest,
                             defaultEnableThreadDumps,
                             slowTimings,
                             hungTimings);
            } catch (Exception e) {
                // FFDC injected
            }
        }

        return new TimingConfigGroup() {
            @Override
            public List<Timing> getSlowRequestTimings() {
                return slowTimings;
            }

            @Override
            public List<Timing> getHungRequestTimings() {
                return hungTimings;
            }
        };
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void parseElement(Dictionary<String, Object> el,
                              long defaultSlow, long defaultHung,
                              boolean defaultInterrupt, boolean defaultDumps,
                              List<Timing> slowList, List<Timing> hungList) {
        String pid = el.get(Constants.SERVICE_PID) != null ? el.get(Constants.SERVICE_PID).toString() : null;

        long slowThreshold = readLong(el, RequestTimingConstants.RT_SLOW_REQUEST_THRESHOLD, defaultSlow);
        long hungThreshold = readLong(el, RequestTimingConstants.RT_HUNG_REQUEST_THRESHOLD, defaultHung);
        boolean interrupt = readBoolean(el, RequestTimingConstants.RT_INTERRUPT_HUNG_REQUEST, defaultInterrupt);
        boolean threadDump = readBoolean(el, RequestTimingConstants.RT_ENABLE_THREAD_DUMPS, defaultDumps);

        // Context info pattern [appName, moduleName, beanName, methodName]
        String[] contextInfo = new String[] {
                                              readAttr(el, ATTR_APP_NAME),
                                              readAttr(el, ATTR_MODULE_NAME),
                                              readAttr(el, ATTR_BEAN_NAME),
                                              readAttr(el, ATTR_METHOD_NAME)
        };

        // requestType may expand to multiple event types
        String requestType = readAttr(el, ATTR_REQUEST_TYPE);
        List<String> eventTypes = resolveEventTypes(requestType);

        for (String eventType : eventTypes) {
            slowList.add(new Timing(pid, eventType, contextInfo, slowThreshold, false, true));
            hungList.add(new Timing(pid, eventType, contextInfo, hungThreshold, interrupt, threadDump));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsed ejbTiming element",
                     new Object[] { eventTypes, contextInfo, slowThreshold, hungThreshold });
        }
    }

    /**
     * Expands a {@code requestType} value to the set of probe event type strings
     * it covers.
     */
    private List<String> resolveEventTypes(String requestType) {
        List<String> types = new ArrayList<>();
        if (TYPE_ALL.equals(requestType)) {
            types.add(EVENT_TYPE_TIMER);
            types.add(EVENT_TYPE_ASYNC);
            types.add(EVENT_TYPE_REMOTE);
            types.add(EVENT_TYPE_MDB);
        } else if (TYPE_ASYNC.equals(requestType)) {
            types.add(EVENT_TYPE_ASYNC);
        } else if (TYPE_MDB.equals(requestType)) {
            types.add(EVENT_TYPE_MDB);
        } else if (TYPE_REMOTE.equals(requestType)) {
            types.add(EVENT_TYPE_REMOTE);
        } else if (TYPE_TIMER.equals(requestType)) {
            types.add(EVENT_TYPE_TIMER);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // The metatype restricts requestType to the values above, so this should not occur
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Ignoring unsupported ejbTiming requestType : " + requestType);
        }
        return types;
    }

    // -----------------------------------------------------------------------
    // Config attribute helpers
    // -----------------------------------------------------------------------

    private static String readAttr(Dictionary<String, Object> el, String key) {
        Object v = el.get(key);
        return (v != null) ? v.toString() : "*";
    }

    private static long readLong(Dictionary<String, Object> el, String key, long defaultValue) {
        Object v = el.get(key);
        return (v != null) ? Long.parseLong(v.toString()) : defaultValue;
    }

    private static boolean readBoolean(Dictionary<String, Object> el, String key, boolean defaultValue) {
        Object v = el.get(key);
        return (v != null) ? Boolean.parseBoolean(v.toString()) : defaultValue;
    }
}
