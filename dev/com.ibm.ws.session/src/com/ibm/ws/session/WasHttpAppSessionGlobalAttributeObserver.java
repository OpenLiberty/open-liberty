/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session;

import java.util.ArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.ibm.ws.session.http.HttpSessionAttributeObserver;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;

/**
 * This implementation of the ISessionStateObserver drives the
 * sessionAttributeListener invocation
 * mechanism by registering for callbacks with the base session manager.
 */
public class WasHttpAppSessionGlobalAttributeObserver extends HttpSessionAttributeObserver {

    private static final String methodClassName = "WasHttpAppSessionAttributeObserver";

    // ----------------------------------------
    // Public Constructor
    // ----------------------------------------
    public WasHttpAppSessionGlobalAttributeObserver(ArrayList listeners, IProtocolAdapter adapter) {
        super(listeners, adapter);
    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Method sessionAttributeSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeSet(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeSet(ISession session, Object name, Object oldValue, Boolean oldIsListener, Object newValue, Boolean newIsListener) {

        HttpSession httpsession = (HttpSession) _adapter.adapt(session);
        HttpSessionBindingEvent addEvent = null; // only init if necessary
        HttpSessionBindingEvent replaceEvent = null; // only init this if
                                                     // necessary..done below

        // do binding listeners first to be consistent with v6.1
        if ((oldValue != null) && (oldIsListener.booleanValue())) {
            replaceEvent = new HttpSessionBindingEvent(httpsession, (String) name, oldValue);
            // if (oldValue instanceof HttpSessionBindingListener)
            ((HttpSessionBindingListener) oldValue).valueUnbound(replaceEvent);
        }
        if ((newValue != null) && (newIsListener.booleanValue())) { // (newValue instanceof HttpSessionBindingListener) ) {
            addEvent = new HttpSessionBindingEvent(httpsession, (String) name, newValue);
            ((HttpSessionBindingListener) newValue).valueBound(addEvent);
        }
    }

    /**
     * Method sessionAttributeRemoved
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeRemoved(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeRemoved(ISession session, Object name, Object value, Boolean oldIsBindingListener) {
        HttpSessionAttributeListener listener = null;
        HttpSessionBindingEvent event = null;
        HttpSession httpsession = (HttpSession) _adapter.adapt(session);

        event = new HttpSessionBindingEvent(httpsession, (String) name, value);
        if (value instanceof HttpSessionBindingListener) {
            if (oldIsBindingListener.booleanValue()) {
                ((HttpSessionBindingListener) value).valueUnbound(event);
            }
        }
    }

}
