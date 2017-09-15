/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.logging.internal.osgi.TrLogImpl.LogEvent;

/**
 *
 */
public class TrLogEntry implements LogEntry, FFDCSelfIntrospectable {

    protected final TrLogImpl logImpl;
    final long timestamp;
    final int level;
    final String msg;
    final Bundle b;
    final ServiceReference<?> sr;
    final Throwable t;
    final EventObject event;

    transient String toString;

    protected TrLogEntry(TrLogImpl logImpl,
                         long timestamp, int level, String msg, Bundle b,
                         ServiceReference<?> sr, Throwable t, EventObject event) {
        this.logImpl = logImpl;
        this.timestamp = timestamp;
        this.level = level;
        this.msg = msg;
        this.b = b;
        this.sr = sr;
        this.t = t;
        this.event = event;
    }

    /** {@inheritDoc} */
    @Override
    public Bundle getBundle() {
        return b;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceReference getServiceReference() {
        return sr;
    }

    /** {@inheritDoc} */
    @Override
    public int getLevel() {
        return level;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return msg;
    }

    /** {@inheritDoc} */
    @Override
    public Throwable getException() {
        return t;
    }

    /** {@inheritDoc} */
    @Override
    public long getTime() {
        return timestamp;
    }

    /**
     * Analyze available fields from the LogEntry, and make a suitable object
     * array for passing to trace.
     * 
     * @param translatedMsg
     *            Include the entry's log message in the list of objects for
     *            inclusion in translated/formatted messages
     * @return Object array for trace
     */
    Object[] getObjects(boolean translatedMsg) {
        ArrayList<Object> list = new ArrayList<Object>(5);

        if (translatedMsg && msg != null) {
            list.add(msg);
        }

        if (translatedMsg && b != null) {
            String bString = String.format("Bundle:%s(id=%d)", b.getSymbolicName(), b.getBundleId());
            list.add(bString);
        }

        if (sr != null) {
            String sString = String.format("ServiceRef:%s(id=%s, pid=%s)",
                                           java.util.Arrays.asList((String[]) sr.getProperty("objectClass")),
                                           sr.getProperty("service.id"),
                                           sr.getProperty("service.pid"));
            list.add(sString);
        }

        if (t != null) {
            list.add(t);
        }

        if (event != null) {
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

    @Override
    public String toString() {
        if (toString != null)
            return toString;

        StringBuilder str = new StringBuilder(this.getClass().getSimpleName());
        str.append("[").append(msg);
        str.append(",Bundle:").append(b.getSymbolicName());
        str.append("(").append(b.getBundleId()).append(")");

        if (sr != null) {
            str.append(",ServiceRef:");
            str.append(java.util.Arrays.asList((String[]) sr.getProperty("objectClass")));
            str.append("(id=").append(sr.getProperty("service.id"));
            str.append(",pid=").append(sr.getProperty("service.pid"));
            str.append(")");
        }
        if (event != null) {
            str.append(",Event:").append(event.toString());
        }
        str.append("]");

        toString = str.toString();
        return toString;
    }

    /** {@inheritDoc} */
    @Override
    public String[] introspectSelf() {
        Object[] objs = getObjects(true);
        ArrayList<String> strs = new ArrayList<String>(objs.length);
        for (Object o : objs) {
            String s = o.toString();
            if (s.length() > 0) {
                strs.add(s);
            }
        }
        return strs.toArray(new String[strs.size()]);
    }

    public void publish(Collection<LogListener> listeners, EventAdmin service) {
        // Notify listeners
        for (LogListener l : listeners) {
            try {
                l.logged(this);
            } catch (Exception e) {
                FFDCFilter.processException(e, this.getClass().getName(), "run-1", new Object[] { l, this });
            }
        }

        // publish event
        if (service != null) {
            HashMap<String, Object> map = new HashMap<String, Object>();

            map.put("log.level", level);
            map.put("log.entry", this);
            map.put("message", msg);
            map.put("timestamp", timestamp);

            map.put("bundle", b);
            map.put("bundle.id", b.getBundleId());
            String symName = b.getSymbolicName();
            if (symName != null) {
                map.put("bundle.symbolicName", symName);
            }

            if (t != null) {
                map.put("exception.class", t.getClass());
                map.put("exception.message", t.getMessage());
                map.put("exception", t);
            }

            if (sr != null) {
                map.put("service", sr);
                map.put("service.id", sr.getProperty("service.id"));
                Object pid = sr.getProperty("service.pid");
                if (pid != null) {
                    map.put("service.pid", pid);
                }
                map.put("service.objectClass", sr.getProperty("service.objectClass"));
            }

            Event event = new Event(LogEvent.getTopic(level), map);
            try {
                service.postEvent(event);
            } catch (Exception e) {
                FFDCFilter.processException(e, this.getClass().getName(), "run-2", new Object[] { this, event });
            }
        }
    }
}
