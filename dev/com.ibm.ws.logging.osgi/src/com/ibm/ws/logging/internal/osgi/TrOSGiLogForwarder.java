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
package com.ibm.ws.logging.internal.osgi;

import static com.ibm.ws.logging.internal.osgi.OsgiLogConstants.*;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Map;
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

public class TrOSGiLogForwarder implements SynchronousLogListener, SynchronousBundleListener {
    private static final TraceComponent _tc = Tr.register(TrOSGiLogForwarder.class);

    final static class OSGiTraceComponent extends TraceComponent {
        private final String ffdcMe;

        protected OSGiTraceComponent(String logName, Class<?> aClass, String[] groups, String ffdcMe) {
            super("LogService-" + logName, aClass, groups, OsgiLogConstants.MESSAGE_BUNDLE);
            this.ffdcMe = ffdcMe;
        }

        public String getFfdcMe() {
            return ffdcMe;
        }
    }

    public static final int LOG_EVENT = -5;

    private static final Object COULD_NOT_OBTAIN_LOCK_EXCEPTION = "Could not obtain lock";
	private static final String COULD_NOT_GET_SERVICE_FROM_REF = "could not get service from ref";
	private static final String COULD_NOT_OBTAIN_ALL_REQ_DEPS = "could not obtain all required dependencies";
	private static final String SERVICE_NOT_AVAILABLE = "service not available from service registry for servicereference";

    private final Map<Bundle, OSGiTraceComponent> traceComponents = new ConcurrentHashMap<Bundle, OSGiTraceComponent>();

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
                String group = b.getHeaders("").get("WS-TraceGroup");
                String[] groups;
                if (group == null) {
                    groups = new String[] { bsn, LOG_SERVICE_GROUP, TRACE_SPEC_OSGI_EVENTS };
                } else {
                    groups = new String[] { bsn, group, LOG_SERVICE_GROUP, TRACE_SPEC_OSGI_EVENTS };
                }
                tc = new OSGiTraceComponent(logName, this.getClass(), groups, ffdcMe);
                traceComponents.put(b, tc);
                TrConfigurator.registerTraceComponent(tc);
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.event(_tc, "Created OSGiTraceComponent: " + tc);
                }
            }
            return tc;
        }
    }

    @Override
    public void logged(LogEntry le) {
        boolean isAnyTraceEnabled = TraceComponent.isAnyTracingEnabled();
        ExtendedLogEntry logEntry = (ExtendedLogEntry) le;
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
                    if (tc.isAuditEnabled()) {
                        Tr.audit(tc, "OSGI_AUDIT_MSG", getObjects(logEntry, true));
                    }
                    break;
                case DEBUG:
                    if (isAnyTraceEnabled && tc.isDebugEnabled()) {
                        Tr.debug(b, tc, logEntry.getMessage(), getObjects(logEntry, false));
                    }
                    break;
    
                case INFO:
                    if (tc.isInfoEnabled()) {
                        if(shouldBeLogged(logEntry, tc)) {
                            Tr.info(tc, "OSGI_MSG001", getObjects(logEntry, true));
                        }
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
                    if (isAnyTraceEnabled && tc.isDumpEnabled()) {
                        Tr.dump(tc, logEntry.getMessage(), getObjects(logEntry, false));
                    }
                    break;

            }
        } catch (Throwable t2) {
            FFDCFilter.processException(t2, tc.getFfdcMe(), "log", logEntry);
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
    Object[] getObjects(ExtendedLogEntry logEntry, boolean translatedMsg) {
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

        Object event = logEntry.getContext();
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
    private boolean shouldBeLogged(Throwable t, OSGiTraceComponent tc, ExtendedLogEntry logEntry) {
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
    private boolean shouldBeLogged(ExtendedLogEntry logEntry, OSGiTraceComponent tc) {
        String message = logEntry.getMessage().toLowerCase();
        if(message.contains(COULD_NOT_GET_SERVICE_FROM_REF) ||
                message.contains(COULD_NOT_OBTAIN_ALL_REQ_DEPS) ||
                message.contains(SERVICE_NOT_AVAILABLE)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "This is not an error, but may indicate high system load - " + logEntry.getMessage(),
                        getObjects(logEntry, false));
            }
            return false;
        }
        return true;
    }
}
