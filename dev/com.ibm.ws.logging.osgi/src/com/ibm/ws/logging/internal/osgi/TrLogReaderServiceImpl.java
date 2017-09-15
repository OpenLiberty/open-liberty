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

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @see LogReaderService
 */
public class TrLogReaderServiceImpl implements LogReaderService {
    private static final TraceComponent myTc = Tr.register(TrLogReaderServiceImpl.class);

    /** Lazy initialization of listeners: wait until first registration of listener. */
    final static class ListenerHolder {
        protected final static ConcurrentHashMap<LogListener, LogListener> listeners = new ConcurrentHashMap<LogListener, LogListener>();
    }

    protected final TrLogImpl logImpl;

    TrLogReaderServiceImpl(TrLogImpl logImpl) {
        this.logImpl = logImpl;
    }

    /** {@inheritDoc} */
    @Override
    public void addLogListener(LogListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(this, myTc, "addLogListener", listener);

        ListenerHolder.listeners.putIfAbsent(listener, listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeLogListener(LogListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && myTc.isDebugEnabled())
            Tr.debug(this, myTc, "removeLogListener", listener);

        ListenerHolder.listeners.remove(listener);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getLog() {
        return logImpl.getRecentEntries();
    }

    /**
     * @return iterator for registered listeners. May only be used by one thread.
     */
    protected Set<LogListener> getListeners() {
        return ListenerHolder.listeners.keySet();
    }

    /**
     * Clean up after the service has been released by a bundle
     * 
     * @see {@link TrLogReaderServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, LogReaderService)}
     */
    protected synchronized void release() {
        ListenerHolder.listeners.clear();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + ListenerHolder.listeners + "]";
    }
}
