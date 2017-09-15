/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.session.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.event.ScheduledEventService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Utility class that handles mapping requests to sessions and creating
 * new sessions if required.
 */
public class SessionManager {

    /** Debug variable */
    private static final TraceComponent tc = Tr.register(SessionManager.class);

    private final Object timerLock = new TimerLock();
    private ScheduledEventService scheduler = null;
    private ScheduledFuture<?> future = null;
    private Map<ServletContext, Map<String, SessionImpl>> groupings = null;
    private IDGenerator idgen = null;
    private long purgeInterval = 600L; // default to 10 minutes (in seconds)
    /** Server level scoped session configuration */
    private SessionConfig myConfig = null;

    /**
     * Constructor.
     */
    public SessionManager() {
        this.groupings = new HashMap<ServletContext, Map<String, SessionImpl>>();
        this.idgen = new IDGenerator();
        this.myConfig = new SessionConfig();
    }

    /**
     * DS method to activate this component.
     * 
     * @param context
     */
    public void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating");
        }
        processConfig(context.getProperties());
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param context
     */
    public void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating");
        }
        if (null != this.future) {
            synchronized (this.timerLock) {
                if (null != this.future) {
                    this.future.cancel(true);
                    this.future = null;
                }
            } // end-sync
        }
        // TODO purge the groupings/sessions maps, or dump to disk once
        // persistence is added
    }

    /**
     * DS method for runtime updates to configuration without stopping and
     * restarting the component.
     * 
     * @param properties
     */
    protected void modified(Map<?, ?> properties) {
        if (properties instanceof Dictionary) {
            processConfig((Dictionary<?, ?>) properties);
        } else {
            Dictionary<?, ?> newconfig = new Hashtable<Object, Object>(properties);
            processConfig(newconfig);
        }
    }

    /**
     * Method called when the properties for the session manager have been
     * found or udpated.
     * 
     * @param props
     */
    public void processConfig(Dictionary<?, ?> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Session manager configuration updated");
        }
        this.myConfig.updated(props);

        String value = (String) props.get("purge.interval");
        if (null != value) {
            try {
                // convert the configuration in seconds to runtime milliseconds
                this.purgeInterval = Long.parseLong(value.trim());
            } catch (NumberFormatException nfe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Ignoring incorrect purge interval [" + value + "]", nfe.getMessage());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Config: purge interval [" + this.purgeInterval + "]");
        }
    }

    /**
     * Once sessions are actively being used, this is used to start the
     * background invalidation timer that periodically scans for expired
     * sessions that should be purged from storage.
     */
    private void startPurgeTimer() {
        if (null != this.future || null == this.scheduler) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Creating repeating purge event");
        }
        this.future = this.scheduler.schedule(SessionEventHandler.PURGE_EVENT, this.purgeInterval, this.purgeInterval, TimeUnit.SECONDS);
    }

    /**
     * DS method for setting the timer service reference.
     * 
     * @param ref
     */
    protected void setScheduledEventService(ScheduledEventService ref) {
        synchronized (this.timerLock) {
            this.scheduler = ref;
        }
    }

    /**
     * DS method for removing the timer service reference.
     * 
     * @param mgr
     */
    protected void unsetScheduledEventService(ScheduledEventService ref) {
        synchronized (this.timerLock) {
            if (ref == this.scheduler) {
                this.scheduler = null;
            }
        }
    }

    /**
     * Method used by the recurring session purge event to scan for and
     * discard expired sessions. This prevents memory build up when sessions
     * are no longer queried by clients.
     */
    protected void startPurge() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEventEnabled()) {
            Tr.event(tc, "Running purge of expired sessions");
        }
        try {
            List<SessionImpl> toPurge = new ArrayList<SessionImpl>();
            for (Map<String, SessionImpl> sessions : this.groupings.values()) {
                synchronized (sessions) {
                    // scan for all expired sessions
                    for (SessionImpl session : sessions.values()) {
                        if (session.checkExpiration(false)) {
                            toPurge.add(session);
                        }
                    }
                    // now remove those sessions from the map (outside the
                    // iteration loop)
                    for (SessionImpl session : toPurge) {
                        sessions.remove(session.getId());
                    }
                } // end-sync

                // now iterate that list outside the lock (involves calling
                // session listeners)
                for (SessionImpl session : toPurge) {
                    // if the session is still "valid" then we need to call
                    // invalidate now
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Purging session; " + session);
                    }
                    if (!session.isInvalid()) {
                        session.invalidate();
                    }
                }
                toPurge.clear();
            } // end-grouping-loop
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName(), "purge");
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error while running purge scan; " + t);
            }
        }
    }

    /**
     * Access the appropriate session configuration information for a given
     * context. If config does not exist for a given application, then the
     * common manager level config is returned.
     * 
     * @param info
     * @return SessionConfig
     */
    public SessionConfig getSessionConfig(SessionInfo info) {
        return this.myConfig;
    }

    /**
     * Access, and possibly create if not found, a session for the given
     * client information.
     * 
     * @param info
     * @param create
     * @return SessionImpl, null if not found and create flag is false
     */
    public SessionImpl getSession(SessionInfo info, boolean create) {
        ServletContext key = info.getContext();
        Map<String, SessionImpl> sessions = this.groupings.get(key);
        if (null == sessions) {
            synchronized (this.groupings) {
                sessions = this.groupings.get(key);
                if (null == sessions) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Creating session group: " + key);
                    }
                    sessions = new HashMap<String, SessionImpl>();
                    this.groupings.put(key, sessions);
                }
            } // end-sync
        }
        SessionImpl session = null;
        String id = info.getID();
        if (null != id) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getSession: existing id=" + id);
            }
            session = sessions.get(id);
            if (null != session) {
                // check expiration on existing session
                if (session.checkExpiration(true)) {
                    if (!session.isInvalid()) {
                        // if it wasn't already invalidated, do so now
                        session.invalidate();
                    }
                    session = null;
                }
            }
            id = null;
        } // check-existing

        // if we don't have a session now (could have been expired above)
        // and the create flag is true, make a new one now
        if (null == session && create) {
            id = this.idgen.getID();
            session = new SessionImpl(id, info.getContext());
            synchronized (sessions) {
                sessions.put(id, session);
            } // end-sync

            // created a session, make sure the purge timer is running
            if (null == this.future) {
                synchronized (this.timerLock) {
                    startPurgeTimer();
                } // end-sync
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getSession: " + session);
        }
        return session;
    }

    private static class TimerLock extends Object {
        protected TimerLock() {
            // nothing to do
        }

        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Timer service lock";
        }
    }
}
