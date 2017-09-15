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
import com.ibm.wsspi.session.ISessionStateObserver;

/**
 * This class is affiliated with the session manager. It is responsible for
 * driving the state observers
 * mechanism associated with the session manager.
 * 
 * @author aditya
 * 
 */
public class SessionStateEventDispatcher implements ISessionStateObserver {

    // ----------------------------------------
    // Private members.
    // ----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    /*
     * A reference to the parent sessionManagers list of sessionStateObservers
     */
    private ArrayList _sessionStateObservers = null;

    /*
     * The String id of the ObserverManager
     */
    private String _id = null;

    private static final String methodClassName = "SessionStateEventDispatcher";

    // ----------------------------------------
    // Protected constructor
    // ----------------------------------------
    protected SessionStateEventDispatcher(ArrayList sessionStateObservers) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.6 5/30/08 16:34:47");
                _loggedVersion = true;
            }
        }
        _sessionStateObservers = sessionStateObservers;
        _id = "SessionStateEventDispatcher";
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------

    // XD methods
    public void sessionAttributeSet(ISession source, Object key, Object oldValue, Object newValue) {
        sessionAttributeSet(source, key, oldValue, Boolean.FALSE, newValue, Boolean.FALSE);
    }

    public void sessionAttributeRemoved(ISession source, Object key, Object value) {
        sessionAttributeRemoved(source, key, value, Boolean.FALSE);
    }

    // end of XD methods
    /**
     * Method sessionAttributeSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeSet(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeSet(ISession source, Object key, Object oldValue, Boolean oldIsListener, Object newValue, Boolean newIsListener) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionAttributeSet(source, key, oldValue, oldIsListener, newValue, newIsListener);
        }
    }

    /**
     * Method sessionAttributeRemoved
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeRemoved(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeRemoved(ISession source, Object key, Object value, Boolean oldIsBindingListener) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionAttributeRemoved(source, key, value, oldIsBindingListener);
        }

    }

    /**
     * Method sessionAttributeAccessed
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeAccessed(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeAccessed(ISession session, Object key, Object value) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // _sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionAttributeAccessed(session, key, value);
        }

    }

    /**
     * Method sessionUserNameSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionUserNameSet(com.ibm.wsspi.session.ISession, java.lang.String, java.lang.String)
     */
    public void sessionUserNameSet(ISession session, String oldUserName, String newUserName) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionUserNameSet(session, oldUserName, newUserName);
        }
    }

    /**
     * Method sessionLastAccessTimeSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionLastAccessTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionLastAccessTimeSet(ISession session, long old, long newaccess) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionLastAccessTimeSet(session, old, newaccess);
        }

    }

    /**
     * Method sessionMaxInactiveTimeSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionMaxInactiveTimeSet(com.ibm.wsspi.session.ISession, int, int)
     */
    public void sessionMaxInactiveTimeSet(ISession session, int old, int newval) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionMaxInactiveTimeSet(session, old, newval);
        }
    }

    /**
     * Method sessionExpiryTimeSet
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionExpiryTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionExpiryTimeSet(ISession session, long old, long newone) {
        // ArrayList sessionStateObservers = null;

        /*
         * Check to see if there is a non-empty list of sessionStateObservers.
         */
        if (_sessionStateObservers == null || _sessionStateObservers.size() < 1) {
            return;
        }

        // synchronized(_sessionStateObservers) {
        // sessionStateObservers = (ArrayList)_sessionStateObservers.clone();
        // }

        ISessionStateObserver sessionStateObserver = null;
        for (int i = 0; i < _sessionStateObservers.size(); i++) {
            sessionStateObserver = (ISessionStateObserver) _sessionStateObservers.get(i);
            sessionStateObserver.sessionExpiryTimeSet(session, old, newone);
        }
    }

    /**
     * Method getId
     * <p>
     * 
     * @see com.ibm.wsspi.session.ISessionStateObserver#getId()
     */
    public String getId() {
        return _id;
    }

}
