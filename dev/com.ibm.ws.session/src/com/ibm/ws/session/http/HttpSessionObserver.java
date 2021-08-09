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
package com.ibm.ws.session.http;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionObserver;

/**
 * This implementation of the ISessionObserver drives the sessionListener
 * invocation
 * mechanism by registering for callbacks with the base session manager.
 */
public class HttpSessionObserver implements ISessionObserver {

    // ----------------------------------------
    // Private Members
    // ----------------------------------------

    /*
     * For logging.
     */
    private static final String methodClassName = "HttpSessionObserver";
    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    /*
     * A reference to the parent HttpSessionManagers arrayList of
     * session Listeners.
     */
    protected ArrayList _sessionListeners;
    
    /*
     * A reference to the parent HttpSessionManagers arrayList of
     * session id listeners.
     */
    protected ArrayList _sessionIdListeners;

    /*
     * A reference to the _adapter object that is used by the parent Http session
     * manager
     * to transform an IManagedSession object to a protocol specific session
     * object.
     */
    protected IProtocolAdapter _adapter = null;

    protected static final int SESSION_DESTROYED = 0;
    protected static final String methodNames[] = { "sessionDestroyed" };

    // ----------------------------------------
    // Public Constructor
    // ----------------------------------------
    public HttpSessionObserver(ArrayList listeners, IProtocolAdapter adapter) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 3/12/08 09:24:17");
                _loggedVersion = true;
            }
        }
        _sessionListeners = listeners;
        _adapter = adapter;
    }
    
    // ----------------------------------------
    // Public Constructor
    // ----------------------------------------
    public HttpSessionObserver(ArrayList listeners, ArrayList idListeners, IProtocolAdapter adapter) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 3/12/08 09:24:17");
                _loggedVersion = true;
            }
        }
        _sessionListeners = listeners;
        _sessionIdListeners = idListeners;
        _adapter = adapter;
    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------
    /**
     * Method sessionCreated
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionCreated(com.ibm.wsspi.session.ISession)
     */
    public void sessionCreated(ISession session) {
        HttpSession httpsession = (HttpSessionImpl) _adapter.adapt(session);
        HttpSessionEvent event = new HttpSessionEvent(httpsession);
        HttpSessionListener listener = null;

        // synchronized (_sessionListeners) {
        for (int i = 0; i < _sessionListeners.size(); i++) {
            listener = (HttpSessionListener) _sessionListeners.get(i);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionCreated", "Calling sessionCreated on listener:" + listener);
            }
            if (!session.isOverflow()) { //PK85530
                listener.sessionCreated(event);
            }
        }
        // }

    }

    /**
     * Method sessionAccessed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionAccessed(com.ibm.wsspi.session.ISession)
     */
    public void sessionAccessed(ISession session) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAccessUnknownKey(java.lang
     * .Object)
     */
    public void sessionAccessUnknownKey(Object key) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionAffinityBroke(ISession)
     */
    public void sessionAffinityBroke(ISession session) {}

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionCacheDiscard(java.lang.Object
     * )
     */
    public void sessionCacheDiscard(Object value) {}

    public void sessionLiveCountInc(Object value) {}

    public void sessionLiveCountDec(Object value) {}

    /**
     * Method sessionDestroyed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionDestroyed(com.ibm.wsspi.session.ISession)
     */
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

            // synchronized (_sessionListeners) {
            for (int i = _sessionListeners.size() - 1; i >= 0; i--) {
                listener = (HttpSessionListener) _sessionListeners.get(i);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SESSION_DESTROYED], "Calling sessionDestroyed on listener:" + listener);
                }
                try {
                    listener.sessionDestroyed(event);
                } catch (Exception e) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SESSION_DESTROYED], e.toString() + " thrown by listener: " +  listener);
                    }
                }
            }
            // }

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

    public void sessionDestroyedByTimeout(ISession session) {
        // sessionDestroyed should be called when we invalidate on a timeout anyway.
        // We need to implement
        // this method for observers that care to differentiate (like a PMI
        // counter), but HttpSession Observers shouldn't use this
        // and would throw an IllegalStateException for any listeners if the session
        // was already invalidated.
        // sessionDestroyed(session);
    }

    /**
     * Method sessionReleased
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionReleased(com.ibm.wsspi.session.ISession)
     */
    public void sessionReleased(ISession session) {

    }

    /**
     * Method sessionFlushed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionFlushed(com.ibm.wsspi.session.ISession)
     */
    public void sessionFlushed(ISession session) {

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

    /**
     * Method getId
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#getId()
     */
    public String getId() {
        return "HttpSessionObserver";
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wsspi.session.ISessionObserver#sessionIdChanged(java.lang.String, com.ibm.wsspi.session.ISession)
     */
    @Override
    public void sessionIdChanged(String oldId, ISession session) {
        HttpSession httpsession = (HttpSessionImpl) _adapter.adapt(session);
        HttpSessionEvent event = new HttpSessionEvent(httpsession);
        HttpSessionIdListener listener = null;

        // synchronized (_sessionListeners) {
        for (int i = 0; i < _sessionIdListeners.size(); i++) {
            listener = (HttpSessionIdListener) _sessionIdListeners.get(i);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionIdChanged", "Calling sessionIdChanged on listener:" + listener);
            }
            listener.sessionIdChanged(event,  oldId);
        }
    }
}
