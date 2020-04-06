/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.internal.osgi;

import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.EQUINOX_METATYPE_BSN;
import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.LOGGER_EVENTS;
import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.LOG_SERVICE_GROUP;
import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.TRACE_SPEC_OSGI_EVENTS;
import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.TRACE_ENABLED;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCConfigurator;

/**
 * This class is instantiated during RAS bundle activation. It registers itself
 * as a ManagedService with the Config service in order to receive updates to
 * the RAS Tr configuration.
 */
public class LoggingConfigurationService implements ManagedService {
    private static final TraceComponent tc = Tr.register(LoggingConfigurationService.class,OsgiLogConstants.TRACE_GROUP,OsgiLogConstants.MESSAGE_BUNDLE);

    /** PID: identifies bundle to ConfigAdminService */
    public static final String RAS_TR_CFG_PID = "com.ibm.ws.logging";

    /** reference to registered RAS config service */
    private ServiceRegistration<ManagedService> configRef = null;

    protected BundleContext context;

    /*
     * Indicates whether instrumentation agent is available to implement dynamic config changes
     */
    private final boolean instrumentation;

    private final LoggerAdmin loggerAdmin;

    private final Map<String, Map<String, LogLevel>> contextLogLevels = Collections.synchronizedMap(new HashMap<String, Map<String, LogLevel>>());

    /**
     * Constructor.
     * 
     * @param context
     */
    public LoggingConfigurationService(BundleContext context, boolean instrumentationActive) {
        this.context = context;
        this.instrumentation = instrumentationActive;

        // Register this as a "ManagedService" to get calls when the config is
        // updated after we've taken care of merging config manually via
        // getConfiguration
        configRef = context.registerService(ManagedService.class, this, defaultProperties());

        TrConfigurator.setInstrumentation(instrumentation);

        loggerAdmin = getService(LoggerAdmin.class, context);
        configureLoggerAdmin();
    }

    <T> T getService(Class<T> type, BundleContext context) {
        ServiceReference<T> ref = context.getServiceReference(type);
        if (ref == null) {
            return null;
        } else {
            return context.getService(ref);
        }
    }

    /**
     * Stop this service and free any allocated resources when the owning bundle
     * is being stopped.
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stopping the Logging managed service");
        }
        // disconnect from the config admin
        this.configRef.unregister();
        this.configRef = null;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public synchronized void updated(Dictionary properties) throws ConfigurationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "properties updated " + properties);

        if (properties == null) {
            return;
        }

        Map<String, Object> newMap = null;
        if (properties instanceof Map) {
            newMap = (Map<String, Object>) properties;
        } else {
            newMap = new HashMap<String, Object>();
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                newMap.put(key, properties.get(key));
            }
        }

        // Update Tr and/or FFDC configurations.
        // --> of concern is changing the log directory.
        TrConfigurator.update(newMap);
        FFDCConfigurator.update(newMap);
        configureLoggerAdmin();
    }

    private void configureLoggerAdmin() {
        if (loggerAdmin == null) {
            return;
        }
        Map<String, Map<String, LogLevel>> newContextLogLevels = new HashMap<>();
        String traceSpec = TrConfigurator.getEffectiveTraceSpec();
        String[] specs =  traceSpec.split(":");
        for (String spec : specs) {
            String[] comps = spec.split("=");
            if (comps.length >= 2) {
                String comp = comps[0];
                LogLevel logLevel = mapLogLevel(comps[1]);
                String enabled = (comps.length > 2) ? comps[2] : TRACE_ENABLED;
                if (logLevel != null && TRACE_ENABLED.equalsIgnoreCase(enabled)) {
                    if (LOG_SERVICE_GROUP.equalsIgnoreCase(comp)) {
                        // have logservice group set the root logger for all
                        add(newContextLogLevels, null, Logger.ROOT_LOGGER_NAME, logLevel, true);
                    } else if (comp.equals(TRACE_SPEC_OSGI_EVENTS)) {
                        // map events to the root context and use the "Events" as the logger name (override)
                        add(newContextLogLevels, null, LOGGER_EVENTS, logLevel, true);
                    } else {
                        if (comp.indexOf('*') == -1) {
                            // only do fully qualified BSNs
                            add(newContextLogLevels, comp, Logger.ROOT_LOGGER_NAME, logLevel, true);
                        }
                        if (logLevel.implies(LogLevel.DEBUG)) {
                            // If any level is set to debug then enable all events, but
                            // don't override explicit configured Events logger configs.
                            add(newContextLogLevels, null, LOGGER_EVENTS, logLevel, false);
                        }
                    }
                }
            }
        }

        // Equinox metatype trace is annoying and largely useless;
        // if not set explicitly then set its annoying root logger to warn
        add(newContextLogLevels, EQUINOX_METATYPE_BSN, Logger.ROOT_LOGGER_NAME, LogLevel.WARN, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configuring loggers: " + newContextLogLevels);
        }

        synchronized (contextLogLevels) {
            if (!contextLogLevels.equals(newContextLogLevels)) {
                contextLogLevels.putAll(newContextLogLevels);
                for (Iterator<Entry<String, Map<String, LogLevel>>> iEntries = contextLogLevels.entrySet().iterator(); iEntries.hasNext();) {
                    Entry<String, Map<String, LogLevel>> entry = iEntries.next();
                    if (!newContextLogLevels.containsKey(entry.getKey())) {
                        // clear out the existing context
                        loggerAdmin.getLoggerContext(entry.getKey()).setLogLevels(Collections.<String, LogLevel> emptyMap());
                        // remove the key
                        iEntries.remove();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Removed: " + entry.getKey() + " - " + entry.getValue());
                        }
                    } else {
                        // replace existing values in the logger context
                        loggerAdmin.getLoggerContext(entry.getKey()).setLogLevels(entry.getValue());
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Configured: " + entry.getKey() + " - " + entry.getValue());
                        }
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Log levels already configured " + newContextLogLevels);
                }
            }
        }
    }

    private static void add(Map<String, Map<String, LogLevel>> contextLogLevels, String contextName, String loggerName, LogLevel logLevel, boolean replace) {
        Map<String, LogLevel> logLevels = contextLogLevels.get(contextName);
        if (logLevels == null) {
            logLevels = new HashMap<>();
            contextLogLevels.put(contextName, logLevels);
        }
        if (replace || !logLevels.containsKey(loggerName)) {
            logLevels.put(loggerName, logLevel);
        }
    }

    private final static Map<String, LogLevel> traceMapToLevel = new HashMap<>();
    static {
        // alias {all, dump}
        traceMapToLevel.put("all", LogLevel.TRACE);
        traceMapToLevel.put("dump", LogLevel.TRACE);

        // alias {finest, debug}
        traceMapToLevel.put("finest", LogLevel.DEBUG);
        traceMapToLevel.put("debug", LogLevel.DEBUG);

        // alias {finer, entryexit}
        traceMapToLevel.put("finer", LogLevel.INFO);
        traceMapToLevel.put("entryexit", LogLevel.INFO);

        // alias {fine, event}
        traceMapToLevel.put("fine", LogLevel.INFO);
        traceMapToLevel.put("event", LogLevel.INFO);

        traceMapToLevel.put("detail", LogLevel.INFO);

        traceMapToLevel.put("info", LogLevel.INFO);

        traceMapToLevel.put("audit", LogLevel.INFO); // odd one; audit means something else in OSGI

        traceMapToLevel.put("warning", LogLevel.WARN);

        // alias {severe, error}
        traceMapToLevel.put("severe", LogLevel.ERROR);
        traceMapToLevel.put("error", LogLevel.ERROR);

        traceMapToLevel.put("fatal", LogLevel.ERROR);

        traceMapToLevel.put("off", LogLevel.AUDIT); // odd one; OSGi cannot disable audit messages
    }

    private LogLevel mapLogLevel(String logLevelSpec) {
        return traceMapToLevel.get(logLevelSpec.toLowerCase());
    }

    protected static Hashtable<String, String> defaultProperties() {
        Hashtable<String, String> ht = new Hashtable<String, String>();
        ht.put(org.osgi.framework.Constants.SERVICE_PID, RAS_TR_CFG_PID);
        return ht;
    }
}
