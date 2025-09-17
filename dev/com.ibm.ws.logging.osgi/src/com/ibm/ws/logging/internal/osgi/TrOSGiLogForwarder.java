/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.logging.ResourceBundleSupport;

public class TrOSGiLogForwarder implements SynchronousLogListener, SynchronousBundleListener {
    private static final TraceComponent _tc = Tr.register(TrOSGiLogForwarder.class,OsgiLogConstants.TRACE_GROUP, OsgiLogConstants.MESSAGE_BUNDLE);

    final static class OSGiTraceComponent extends TraceComponent implements ResourceBundleSupport {
        private final String ffdcMe;

        protected OSGiTraceComponent(String logName, String[] groups, String ffdcMe) {
            // Purposely pass in null for the Class object so that we don't try to instrument this class
            // since all OSGiTraceComponent would have this class as its associated Class
            // Because we are passing in a null Class, this class also implements ResourceBundleSupport
            // in order to provide a Class to use for resolving the ResourceBundle
            super("LogService-" + logName, null, groups, OsgiLogConstants.MESSAGE_BUNDLE);
            this.ffdcMe = ffdcMe;
        }

        public String getFfdcMe() {
            return ffdcMe;
        }

        @Override
        public Class<?> getClassForResourceBundle() {
            return TrOSGiLogForwarder.class;
        }
    }

    public static final int LOG_EVENT = -5;

    private static final Object COULD_NOT_OBTAIN_LOCK_EXCEPTION = "Could not obtain lock";
    private static final String COULD_NOT_GET_SERVICE_FROM_REF = "could not get service from ref";
    private static final String COULD_NOT_OBTAIN_ALL_REQ_DEPS = "could not obtain all required dependencies";
    private static final String SERVICE_NOT_AVAILABLE = "service not available from service registry for servicereference";
    private static final String CANNOT_BE_CALLED_ON_NULL_OBJECT = "cannot be called on null object";

    private final Map<Bundle, OSGiTraceComponent> traceComponents = new ConcurrentHashMap<Bundle, OSGiTraceComponent>();

    public TrOSGiLogForwarder() {
        // force LogEntry class load
        LogEntry.class.getName();
    }

    OSGiTraceComponent getTraceComponent(Bundle b) {
        OSGiTraceComponent tc = traceComponents.get(b);
        if (tc != null) {
            return tc;
        }
        synchronized (traceComponents) {
            tc = traceComponents.get(b);
            if (tc == null) {
                String bsn = b.getSymbolicName();
                long id = b.getBundleId();
                String ffdcMe;
                if (bsn == null) {
                    // if the bundle doesn't have a symbolic name, make something up
                    bsn = "osgi-bundle";
                    ffdcMe = bsn + "-" + id;
                } else {
                    ffdcMe = bsn + "-" + b.getVersion();
                }
                String logName = id + "-" + bsn;
                List<String> groups = new ArrayList<>(5);

                groups.add(bsn);

                String groupHeader = b.getHeaders("").get("WS-TraceGroup");
                if (groupHeader != null) {
                    groups.add(groupHeader);
                }

                groups.add(LOG_SERVICE_GROUP);
                groups.add(TRACE_SPEC_OSGI_EVENTS);

                if (b.getBundleId() == 0) {
                    // Add all the trace groups from the Framework
                    groups.addAll(getEquinoxTraceGroups(b));
                }

                tc = new OSGiTraceComponent(logName, groups.toArray(new String[0]), ffdcMe);
                traceComponents.put(b, tc);
                TrConfigurator.registerTraceComponent(tc);
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.event(_tc, "Created OSGiTraceComponent: " + tc);
                }
            }
            return tc;
        }
    }

    private Collection<String> getEquinoxTraceGroups(Bundle b) {
        // the .options resource contains all the trace group keys
        // that Equinox uses.
        Properties options = new Properties();
        try {
            options.load(b.getResource(".options").openStream());
        } catch (IOException e) {
            // auto FFDC
        }
        return options.stringPropertyNames();
    }

	@Override
    public void logged(LogEntry le) {
        if (currentlyLogging.get() == Boolean.TRUE) {
            // using == to avoid null checks
            // detected recursion into logged; return without logging
            return;
        }
        currentlyLogging.set(Boolean.TRUE);
        try {
            loggedImpl(le);
        } finally {
            currentlyLogging.set(null);
        }
    }

    private void loggedImpl(LogEntry logEntry) {
        boolean isAnyTraceEnabled = TraceComponent.isAnyTracingEnabled();
        Bundle b = logEntry.getBundle();
        if (b == null) {
            // This is possible in rare conditions;
            // For example log entries for service events when the service is unregistered
            // before we could get the bundle
            return;
        }
        OSGiTraceComponent tc = getTraceComponent(b);
        
        try {
            if (logEntry.getLogLevel() != LogLevel.ERROR) {
                // check for events specifically to log them with Tr.event
                if (logEntry.getLoggerName() != null && logEntry.getLoggerName().startsWith(LOGGER_EVENTS_PREFIX))  {
                    if (isAnyTraceEnabled && tc.isEventEnabled()) {
                        Tr.event(b, tc, logEntry.getMessage(), getObjects(logEntry, false));
                    }
                    return;
                }
            }
            switch (logEntry.getLogLevel()) {
                default:
                case AUDIT:
                    Tr.audit(tc, "OSGI_AUDIT_MSG", getObjects(logEntry, true));
                    break;
                case DEBUG:
                    Tr.debug(b, tc, logEntry.getMessage(), getObjects(logEntry, false));
                    break;
                case INFO:
                    if(shouldBeLogged(logEntry, tc)) {
                        Tr.info(tc, "OSGI_MSG001", getObjects(logEntry, true));
                    }
                    break;
                case WARN:
                    if(shouldBeLogged(logEntry, tc)) {
                        Tr.warning(tc, "OSGI_WARNING_MSG", getObjects(logEntry, true));
                    }
                    break;
                case ERROR:
                    Throwable t = logEntry.getException();
                    // BundleException's have good translated messages, so if
                    // there's no cause that might provide additional relevant
                    // information (e.g., NoClassDefFoundError), then just print
                    // the message.
                    if (t instanceof BundleException && t.getMessage() != null && t.getCause() == null) {
                        Tr.error(tc, "OSGI_BUNDLE_EXCEPTION", t.getMessage());
                    } else if (shouldBeLogged(t, tc, logEntry)) {
                        Tr.error(tc, "OSGI_ERROR_MSG", getObjects(logEntry, true));
                    }
                    break;
                case TRACE:
                    Tr.dump(tc, logEntry.getMessage(), getObjects(logEntry, false));
                    break;

            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, tc.getFfdcMe(), "log", logEntry);
        }
    }

    /**
     * Analyze available fields from the LogEntry, and make a suitable object array
     * for passing to trace.
     * 
     * @param logEntry      the log entry
     * @param translatedMsg Include the entry's log message in the list of objects
     *                      for inclusion in translated/formatted messages
     * @return Object array for trace
     */
    Object[] getObjects(LogEntry logEntry, boolean translatedMsg) {
        ArrayList<Object> list = new ArrayList<Object>(5);

        if (translatedMsg && logEntry.getMessage() != null) {
            list.add(logEntry.getMessage());
        }

        if (!translatedMsg) {
            String loggerName = logEntry.getLoggerName();
            if (loggerName != null) {
                list.add(String.format("LoggerName:%s", loggerName));
            }
        }

        ServiceReference<?> sr = logEntry.getServiceReference();
        if (sr != null) {
            String sString = String.format("ServiceRef:%s(id=%s, pid=%s)",
                    java.util.Arrays.asList((String[]) sr.getProperty("objectClass")), sr.getProperty("service.id"),
                    sr.getProperty("service.pid"));
            list.add(sString);
        }

        Throwable t = logEntry.getException();
        if (t != null) {
            list.add(t);
        }

        Object event = ((ExtendedLogEntry) logEntry).getContext();
        if (event instanceof EventObject) {
            String sString = String.format("Event:%s", event.toString());
            list.add(sString);
        }

        if (translatedMsg) {
            while (list.size() < 4)
                // 4 parameters in formatted message
                list.add("");
        }

        return list.toArray();
    }

    /*
     * Check to see if this exception should be squelched.
     */
    private boolean shouldBeLogged(Throwable t, OSGiTraceComponent tc, LogEntry logEntry) {
        while (t != null) {
            if (t instanceof IllegalStateException && COULD_NOT_OBTAIN_LOCK_EXCEPTION.equals(t.getMessage())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "DS could not obtain a lock. This is not an error, but may indicate high system load",
                            getObjects(logEntry, false));
                }
                return false;
            }
            t = t.getCause();
        }
        return true;
    }

    @Override
    public void bundleChanged(BundleEvent e) {
        if (e.getType() == BundleEvent.UNINSTALLED) {
            traceComponents.remove(e.getBundle());
        }
    }
    
    /*
     * Squelch info / warnings related to circular references
     */
    private boolean shouldBeLogged(LogEntry logEntry, OSGiTraceComponent tc) {
        String message = logEntry.getMessage().toLowerCase();
        if(message.contains(COULD_NOT_GET_SERVICE_FROM_REF) ||
                message.contains(COULD_NOT_OBTAIN_ALL_REQ_DEPS) ||
                message.contains(SERVICE_NOT_AVAILABLE) ||
                message.contains(CANNOT_BE_CALLED_ON_NULL_OBJECT)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "This is not an error, but may indicate high system load - " + logEntry.getMessage(),
                        getObjects(logEntry, false));
            }
            return false;
        }
        return true;
    }

    final ThreadLocal<Boolean> currentlyLogging = new ThreadLocal<>();
    
}
