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
import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.ibm.ws.session.http.HttpSessionImpl;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;

public class WasHttpAppSessionGlobalObserver extends WasHttpSessionObserver {

    // For logging.
    private static final String methodClassName = "WasHttpAppSessionObserver";

    /*
     * constructor
     */
    public WasHttpAppSessionGlobalObserver(ArrayList listeners, IProtocolAdapter adapter) {
        super(listeners, adapter);
    }
    
    // call new super constructor on WasHttpSessionObserver
    public WasHttpAppSessionGlobalObserver(ArrayList listeners, ArrayList idListeners, IProtocolAdapter adapter) {
        super(listeners, idListeners, adapter);
    }

    public void sessionDestroyed(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SESSION_DESTROYED], "sessionid=" + session.getId());
        }

        ArrayList attributes = session.getListenerAttributeNames();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SESSION_DESTROYED], "attributes.size()=:" + attributes.size());
        }

        if (_sessionListeners.size() > 0 || attributes.size() > 0) {
            HttpSession httpsession = (HttpSessionImpl) _adapter.adapt(session);
            HttpSessionEvent event = new HttpSessionEvent(httpsession);
            HttpSessionListener listener = null;

            /*
             * for (int i = 0; i < _sessionListeners.size(); i++) {
             * listener = (HttpSessionListener) _sessionListeners.get(i);
             * if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.
             * SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
             * LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName,
             * methodNames[SESSION_DESTROYED], "Calling sessionDestroyed on listener:"
             * + listener);
             * }
             * listener.sessionDestroyed(event);
             * }
             */

            if (attributes.size() != 0) {
                Object value = null;
                String name = null;
                for (int i = 0; i < attributes.size(); i++) {
                    name = (String) attributes.get(i);
                    value = session.getAttribute(name);
                    if (null != value) {
                        // we know these are all binding listeners
                        HttpSessionBindingListener bindingListener = (HttpSessionBindingListener) value;
                        HttpSessionBindingEvent bindingEvent = new HttpSessionBindingEvent(httpsession, name);
                        bindingListener.valueUnbound(bindingEvent);
                    }
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SESSION_DESTROYED]);
        }

    }

    /**
     * Method sessionDidActivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionDidActivate(com.ibm.wsspi.session.ISession)
     */
    public void sessionDidActivate(ISession session) {
        HttpSession httpsession = (HttpSessionImpl) _adapter.adapt(session);
        HttpSessionEvent event = new HttpSessionEvent(httpsession);

        Enumeration enum1 = session.getAttributeNames();
        String attrName;
        Object attr;
        while (enum1.hasMoreElements()) {
            attrName = (String) enum1.nextElement();
            attr = session.getAttribute(attrName);
            if (attr instanceof HttpSessionActivationListener) {
                ((HttpSessionActivationListener) attr).sessionDidActivate(event);
            }
        }
    }

    /**
     * Method sessionWillPassivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionWillPassivate(com.ibm.wsspi.session.ISession)
     */
    public void sessionWillPassivate(ISession session) {
        HttpSession httpsession = (HttpSessionImpl) _adapter.adapt(session);
        HttpSessionEvent event = new HttpSessionEvent(httpsession);

        Enumeration enum1 = session.getAttributeNames();
        String attrName;
        Object attr;
        while (enum1.hasMoreElements()) {
            attrName = (String) enum1.nextElement();
            attr = session.getAttribute(attrName);
            if (attr instanceof HttpSessionActivationListener) {
                ((HttpSessionActivationListener) attr).sessionWillPassivate(event);
            }
        }
    }
}
