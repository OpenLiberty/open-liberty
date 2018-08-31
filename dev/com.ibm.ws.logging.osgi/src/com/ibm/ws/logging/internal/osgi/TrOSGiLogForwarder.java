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
    private static final String LOGGER_FRAMEWORK_EVENT = "Events.Framework";
    private static final String LOGGER_BUNDLE_EVENT = "Events.Bundle";
    private static final String LOGGER_SERVICE_EVENT = "Events.Service";

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

    Map<Bundle, OSGiTraceComponent> traceComponents = new ConcurrentHashMap<Bundle, OSGiTraceComponent>();

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

                // TODO: MORE ICK! the metatype bundle trace really is obnoxious...
                if (bsn.endsWith("org.eclipse.equinox.metatype")) {
                    tc = new OSGiTraceComponent(logName, this.getClass(), new String[] { bsn }, ffdcMe);
                } else {
                    String group = b.getHeaders("").get("WS-TraceGroup");
                    if (group == null) {
                        tc = new OSGiTraceComponent(logName, this.getClass(),
                                new String[] { bsn, OsgiLogConstants.LOG_SERVICE_GROUP }, ffdcMe);
                    } else {
                        tc = new OSGiTraceComponent(logName, this.getClass(),
                                new String[] { bsn, group, OsgiLogConstants.LOG_SERVICE_GROUP }, ffdcMe);
                    }
                }
                traceComponents.put(b, tc);
                TrConfigurator.registerTraceComponent(tc);
            }
            return tc;
        }
    }

    @Override
    public void logged(LogEntry le) {
        boolean isAnyTraceEnabled = TraceComponent.isAnyTracingEnabled();
        ExtendedLogEntry logEntry = (ExtendedLogEntry) le;
        Bundle b = logEntry.getBundle();
        OSGiTraceComponent tc = getTraceComponent(b);
        try {
            if (logEntry.getContext() instanceof EventObject) {
                // for event objects we have to check the logger name to know if we
                // should log or not
                String loggerName = logEntry.getLoggerName();
                switch (loggerName) {
                    case LOGGER_FRAMEWORK_EVENT:
                        if (LogLevel.ERROR == le.getLogLevel()) {
                            // this will be handled as an error below
                            break;
                        }
                    case LOGGER_BUNDLE_EVENT:
                    case LOGGER_SERVICE_EVENT:
                        if (isAnyTraceEnabled && tc.isEventEnabled()) {
                            Tr.event(b, tc, logEntry.getMessage(), getObjects(logEntry, false));
                        }
                        // return now to prevent double logging
                        return;
                    default:
                        break;
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
                        Tr.info(tc, "OSGI_MSG001", getObjects(logEntry, true));
                    }
                    break;
    
                case WARN:
                    Tr.warning(tc, "OSGI_WARNING_MSG", getObjects(logEntry, true));
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

        Bundle b = logEntry.getBundle();
        if (translatedMsg && b != null) {
            String bString = String.format("Bundle:%s(id=%d)", b.getSymbolicName(), b.getBundleId());
            list.add(bString);
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
            while (list.size() < 5)
                // 5 parameters in formatted message
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
                            getObjects(logEntry, true));
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
}
