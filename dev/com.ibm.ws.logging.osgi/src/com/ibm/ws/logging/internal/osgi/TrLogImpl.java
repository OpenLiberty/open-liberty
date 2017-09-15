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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class TrLogImpl implements ServiceFactory<LogReaderService> {
    private static final TraceComponent myTc = Tr.register(TrLogImpl.class);

    enum LogEvent {
        ERROR("LOG_ERROR"),
        WARNING("LOG_WARNING"),
        INFO("LOG_INFO"),
        DEBUG("LOG_DEBUG"),
        OTHER("LOG_OTHER");

        private final String topic;

        LogEvent(String message) {
            topic = "org/osgi/service/log/LogEntry/" + message;
        }

        public static String getTopic(int level) {
            switch (level) {
                case LogService.LOG_ERROR:
                    return ERROR.topic;
                case LogService.LOG_WARNING:
                    return WARNING.topic;
                case LogService.LOG_INFO:
                    return INFO.topic;
                case LogService.LOG_DEBUG:
                    return DEBUG.topic;
                default:
                    return OTHER.topic;
            }
        }
    }

    /** Lazy initialization of readers: wait until first registration of reader. */
    final static class ReaderHolder {
        protected final static ConcurrentHashMap<ServiceRegistration<LogReaderService>, TrLogReaderServiceImpl> readers =
                        new ConcurrentHashMap<ServiceRegistration<LogReaderService>, TrLogReaderServiceImpl>();
    }

    private final ExecutorService executorService;

    private volatile EventAdmin eventService = null;
    private volatile boolean hasReaders = false;

    protected TrLogImpl() {
        // Use one thread for preserving event ordering.
        executorService = Executors.newSingleThreadExecutor(new TrLogThreadFactory("TrLogEvent"));

        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(myTc, "TrLogImpl created", this);
    }

    protected void stop() {
        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(myTc, "stopping TrLogImpl", this);

        executorService.shutdown();
        ReaderHolder.readers.clear();
    }

    protected void setEventAdmin(EventAdmin eventService) {
        this.eventService = eventService;

        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(myTc, "set event admin", this, eventService);
    }

    /**
     * Publish the provided log entry. If the level is ERROR or WARNING, the logEntry is
     * published as a LOG_EVENT, as required by the spec, if there are readers or the
     * event service is present.
     * 
     * @param logEntry LogEntry to publish
     */
    public void publishLogEntry(final TrLogEntry logEntry) {
        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(logEntry.getBundle(), myTc, "publishLogEntry", this);

        // If there are registered readers or the event service is present,
        // queue async task to deliver notifications
        if (hasReaders || eventService != null) {
            // Use final/local variables as a snapshot of available services
            final EventAdmin localService = eventService;
            final Collection<LogListener> listeners;

            if (hasReaders) {
                // Take a snapshot of listeners to be notified
                listeners = new ArrayList<LogListener>();
                for (TrLogReaderServiceImpl impl : ReaderHolder.readers.values()) {
                    listeners.addAll(impl.getListeners());
                }
            } else {
                listeners = Collections.emptyList();
            }

            // Queue to publish to the snapshot of listeners and the eventAdmin service
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    logEntry.publish(listeners, localService);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized LogReaderService getService(Bundle bundle, ServiceRegistration<LogReaderService> registration) {
        hasReaders = true;
        TrLogReaderServiceImpl rService = new TrLogReaderServiceImpl(this);
        TrLogReaderServiceImpl tmp = ReaderHolder.readers.put(registration, rService);

        LogReaderService result = (tmp == null ? rService : tmp);

        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(bundle, myTc, "getService", this, result);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void ungetService(Bundle bundle, ServiceRegistration<LogReaderService> registration, LogReaderService service) {
        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(bundle, myTc, "ungetService", this, service);

        ReaderHolder.readers.remove(registration);
        hasReaders = !ReaderHolder.readers.isEmpty();
    }

    /**
     * @return Enumeration of elements (for empty entry list)
     */
    public Enumeration<Object> getRecentEntries() {
        return EMPTY_ENUMERATION;
    }

    /**
     * @return
     */
    public boolean hasReaders() {
        return hasReaders;
    }

    /**
     * @return
     */
    public EventAdmin getEventAdmin() {
        return eventService;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[readers=" + hasReaders
               + ",eventService=" + eventService
               + "]";
    }

    /**
     * Empty enumeration.
     */
    final static Enumeration<Object> EMPTY_ENUMERATION = new Enumeration<Object>() {

        /** {@inheritDoc} */
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public Object nextElement() {
            return null;
        }
    };

    /**
     * The thread factory used to create threads for the
     * executor service.
     */
    protected final static class TrLogThreadFactory implements ThreadFactory {

        /**
         * The thread group to associate with newly created threads.
         */
        private final ThreadGroup threadGroup;

        /**
         * Create a thread factory that creates threads associated with a
         * named thread group.
         * 
         * @param threadGroupName
         *            the name of the thread group to create
         */
        TrLogThreadFactory(final String threadGroupName) {
            threadGroup = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
                @Override
                public ThreadGroup run() {
                    return new ThreadGroup(threadGroupName);
                }
            });
        }

        /**
         * Create a new thread.
         * 
         * @param runnable
         *            the task to run
         */
        @Override
        public Thread newThread(final Runnable runnable) {
            return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                @Override
                public Thread run() {
                    Thread t = new Thread(threadGroup, runnable);
                    t.setDaemon(true);
                    t.setName(threadGroup.getName() + "-" + t.getName());
                    t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                }
            });
        }
    }
}
