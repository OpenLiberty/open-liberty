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
import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionObserver;

/**
 * This class is affiliated with the session manager. It is responsible for
 * driving the session observers
 * mechanism associated with the session manager.
 * 
 * @author aditya
 * 
 */
public class SessionEventDispatcher implements ISessionObserver {

    // ----------------------------------------
    // Private members.
    // ----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    /*
     * A reference to the parent sessionManagers list of session Observers
     */
    private ArrayList _sessionObservers = null;

    /*
     * The String id of the ObserverManager
     */
    private String _id = null;

    private static final String methodClassName = "SessionEventDispatcher";

    // ----------------------------------------
    // Protected constructor
    // ----------------------------------------
    protected SessionEventDispatcher(ArrayList sessionObservers) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.4 3/12/08 09:24:11");
                _loggedVersion = true;
            }
        }
        _sessionObservers = sessionObservers;
        _id = "SessionEventDispatcher";
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------

    /**
     * Method sessionCreated
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionCreated(com.ibm.wsspi.session.ISession)
     */
    public void sessionCreated(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionCreated", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionCreated(session);
        }
    }

    /**
     * Method sessionAccessed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionAccessed(com.ibm.wsspi.session.ISession)
     */
    public void sessionAccessed(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionAccessed", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionAccessed(session);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAccessUnknownKey(java.lang
     * .Object)
     */
    public void sessionAccessUnknownKey(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionAccessUnknownKey", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }

        // ArrayList sessionObservers = null;
        // synchronized(_sessionObservers) {
        // sessionObservers = (ArrayList)_sessionObservers.clone();
        // }

        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionAccessUnknownKey(key);
        }

    }

    /**
     * Method sessionDestroyed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionDestroyed(com.ibm.wsspi.session.ISession)
     */
    public void sessionDestroyed(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "sessionDestroyed", "sessionid=" + session.getId());
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionDestroyed", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionDestroyed(session);
        }
    }

    public void sessionDestroyedByTimeout(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "sessionDestroyedByTimeout", "sessionid=" + session.getId());
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionDestroyedByTimeout", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionDestroyedByTimeout(session);
        }
    }

    /**
     * Method sessionReleased
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionReleased(com.ibm.wsspi.session.ISession)
     */
    public void sessionReleased(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionReleased", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionReleased(session);
        }
    }

    /**
     * Method sessionDidActivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionDidActivate(com.ibm.wsspi.session.ISession)
     */
    public void sessionDidActivate(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionDidActivate", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionDidActivate(session);
        }
    }

    /**
     * Method sessionWillPassivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionWillPassivate(com.ibm.wsspi.session.ISession)
     */
    public void sessionWillPassivate(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionWillPassivate", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionWillPassivate(session);
        }

    }

    /**
     * Method sessionSynched
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionFlushed(com.ibm.wsspi.session.ISession)
     */
    public void sessionFlushed(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionFlushed", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionFlushed(session);
        }
    }

    /**
     * Method getId
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#getId()
     */
    public String getId() {
        return _id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAffinityBroke(com.ibm.wsspi
     * .session.ISession)
     */
    public void sessionAffinityBroke(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionAffinityBroke", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionAffinityBroke(session);
        }
    }

    public void sessionCacheDiscard(Object value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionCacheDiscard", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionCacheDiscard(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionLiveCountInc(java.lang.Object
     * )
     */
    public void sessionLiveCountInc(Object value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionLiveCountInc", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionLiveCountInc(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionLiveCountDec(java.lang.Object
     * )
     */
    public void sessionLiveCountDec(Object value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionLiveCountDec", "sessionObservers.size()=" + _sessionObservers.size());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionLiveCountDec(value);
        }
    }
    
    /**
     * Method sessionCreated
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#sessionIdChanged(com.ibm.wsspi.session.ISession)
     */
    public void sessionIdChanged(String oldId, ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionIdChanged", "application " + session.getContextId() + ":: from " + oldId + " to " + session.getId());
        }
        if (_sessionObservers == null || _sessionObservers.size() < 1) {
            return;
        }
        ISessionObserver sessionObserver = null;
        for (int i = 0; i < _sessionObservers.size(); i++) {
            sessionObserver = (ISessionObserver) _sessionObservers.get(i);
            sessionObserver.sessionIdChanged(oldId, session);
        }
    }
}
