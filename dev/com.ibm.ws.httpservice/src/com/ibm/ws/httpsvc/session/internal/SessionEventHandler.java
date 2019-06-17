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

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventHandler;
import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Event handler for the HTTP session code.
 */
public class SessionEventHandler implements EventHandler {

    /** trace variable */
    private static final TraceComponent tc = Tr.register(SessionEventHandler.class);

    protected static final Topic PURGE_EVENT = new Topic("com/ibm/ws/httpsvc/session/PURGE");

    /** Reference to the session manager */
    private SessionManager sessionMgr = null;

    /**
     * Constructor.
     */
    public SessionEventHandler() {
        // nothing
    }

    /**
     * DS method to activate this component.
     * 
     * @param context
     */
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating the session event handler");
        }
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating the session event handler");
        }
    }

    /**
     * DS method for setting the session manager service reference.
     * 
     * @param mgr
     */
    protected void setSessionMgr(SessionManager mgr) {
        this.sessionMgr = mgr;
    }

    /**
     * DS method for removing the session manager service reference.
     * 
     * @param mgr
     */
    protected void unsetSessionMgr(SessionManager mgr) {
        if (mgr == this.sessionMgr) {
            this.sessionMgr = null;
        }
    }

    /*
     * @see com.ibm.websphere.eventengine.EventHandler#handleEvent(com.ibm.websphere.eventengine.Event)
     */
    @Override
    public void handleEvent(Event event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Received event: " + event.getTopic());
        }
        // if (event.getTopic().equals(PURGE_EVENT)) {
        this.sessionMgr.startPurge();
        // }
    }

}
