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

import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionObserver;
import com.ibm.wsspi.session.ISessionStateObserver;
import com.ibm.wsspi.session.IStoreCallback;

/**
 * @author aditya
 * 
 */
public class StoreCallback implements IStoreCallback {

    // ----------------------------------------
    // Private members.
    // ----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    /*
     * A reference to the parent sessionManager
     */
    private SessionManager _sessionManager = null;

    /*
     * Session State Observer manager that drives events to all state observers
     */
    private ISessionStateObserver _sessionStateEventDispatcher = null;

    /*
     * Session Observer manager that drives events to all session observers
     */
    private ISessionObserver _sessionEventDispatcher = null;

    private static final String methodClassName = "StoreCallback";

    // ----------------------------------------
    // Protected constructor
    // ----------------------------------------
    protected StoreCallback(SessionManager sessionManager) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.7 5/30/08 16:34:49");
                _loggedVersion = true;
            }
        }
        _sessionManager = sessionManager;
        _sessionEventDispatcher = sessionManager.getSessionEventDispatcher();
        _sessionStateEventDispatcher = sessionManager.getSessionStateEventDispatcher();
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------

    /**
     * Method registerSessionObserver
     * <p>
     * 
     * @param observer
     * @see com.ibm.wsspi.session.IStoreCallback#registerSessionObserver(com.ibm.wsspi.session.ISessionObserver)
     */
    public void registerSessionObserver(ISessionObserver observer) {
        _sessionManager.registerSessionObserver(observer);
    }

    /*
     * Session related callback methods
     */

    /**
     * Method sessionDidActivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.IStoreCallback#sessionDidActivate(com.ibm.wsspi.session.ISession)
     */
    public void sessionDidActivate(ISession session) {
        _sessionEventDispatcher.sessionDidActivate(session);

    }

    /**
     * Method sessionWillPassivate
     * <p>
     * 
     * @see com.ibm.wsspi.session.IStoreCallback#sessionWillPassivate(com.ibm.wsspi.session.ISession)
     */
    public boolean sessionWillPassivate(ISession session) {
        _sessionEventDispatcher.sessionWillPassivate(session);
        return true;
    }

    /**
     * Method sessionWillInvalidate
     * <p>
     * 
     * @see com.ibm.wsspi.session.IStoreCallback#sessionInvalidated(com.ibm.wsspi.session.ISession)
     */
    public boolean sessionInvalidated(ISession session) {
        _sessionEventDispatcher.sessionDestroyed(session);
        return true;
    }

    public boolean sessionInvalidatedByTimeout(ISession session) {
        _sessionEventDispatcher.sessionDestroyedByTimeout(session);
        return true;
    }

    public void sessionAffinityBroke(ISession session) {
        _sessionEventDispatcher.sessionAffinityBroke(session);
    }

    public void sessionCacheDiscard(Object value) {
        _sessionEventDispatcher.sessionCacheDiscard(value);
    }

    public void sessionLiveCountInc(Object value) {
        _sessionEventDispatcher.sessionLiveCountInc(value);
    }

    public void sessionLiveCountDec(Object value) {
        _sessionEventDispatcher.sessionLiveCountDec(value);
    }

    /*
     * Session attributes and internals related callback methods.
     */

    // XD methods
    public void sessionAttributeSet(ISession session, Object name, Object oldValue, Object newValue) {
        _sessionStateEventDispatcher.sessionAttributeSet(session, name, oldValue, Boolean.FALSE, newValue, Boolean.FALSE);

    }

    public void sessionAttributeRemoved(ISession session, Object name, Object value) {
        _sessionStateEventDispatcher.sessionAttributeRemoved(session, name, value, Boolean.FALSE);

    }

    // end of XD methods

    /**
     * Method sessionAttributeSet
     * <p>
     * 
     * @param session
     * @param name
     * @param oldValue
     * @param newValue
     * @see com.ibm.wsspi.session.IStoreCallback#sessionAttributeSet(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeSet(ISession session, Object name, Object oldValue, Boolean oldIsListener, Object newValue, Boolean newIsListener) {
        _sessionStateEventDispatcher.sessionAttributeSet(session, name, oldValue, oldIsListener, newValue, newIsListener);

    }

    /**
     * Method sessionAttributeRemoved
     * <p>
     * 
     * @param session
     * @param name
     * @param value
     * @see com.ibm.wsspi.session.IStoreCallback#sessionAttributeRemoved(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeRemoved(ISession session, Object name, Object value, Boolean oldIsBindingListener) {
        _sessionStateEventDispatcher.sessionAttributeRemoved(session, name, value, oldIsBindingListener);

    }

    /**
     * Method sessionAttributeAccessed
     * <p>
     * 
     * @param session
     * @param name
     * @param value
     * @see com.ibm.wsspi.session.IStoreCallback#sessionAttributeAccessed(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeAccessed(ISession session, Object name, Object value) {
        _sessionStateEventDispatcher.sessionAttributeAccessed(session, name, value);

    }

    /**
     * Method sessionUserNameSet
     * <p>
     * 
     * @param session
     * @param oldUserName
     * @param newUserName
     * @see com.ibm.wsspi.session.IStoreCallback#sessionUserNameSet(com.ibm.wsspi.session.ISession, java.lang.String, java.lang.String)
     */
    public void sessionUserNameSet(ISession session, String oldUserName, String newUserName) {
        _sessionStateEventDispatcher.sessionUserNameSet(session, oldUserName, newUserName);

    }

    /**
     * Method sessionLastAccessTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newaccess
     * @see com.ibm.wsspi.session.IStoreCallback#sessionLastAccessTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionLastAccessTimeSet(ISession session, long old, long newaccess) {
        _sessionStateEventDispatcher.sessionLastAccessTimeSet(session, old, newaccess);

    }

    /**
     * Method sessionMaxInactiveTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newval
     * @see com.ibm.wsspi.session.IStoreCallback#sessionMaxInactiveTimeSet(com.ibm.wsspi.session.ISession, int, int)
     */
    public void sessionMaxInactiveTimeSet(ISession session, int old, int newval) {
        _sessionStateEventDispatcher.sessionMaxInactiveTimeSet(session, old, newval);

    }

    /**
     * Method sessionExpiryTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newone
     * @see com.ibm.wsspi.session.IStoreCallback#sessionExpiryTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionExpiryTimeSet(ISession session, long old, long newone) {
        _sessionStateEventDispatcher.sessionExpiryTimeSet(session, old, newone);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.IStoreCallback#sessionFlushed(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionFlushed(ISession session) {
        _sessionEventDispatcher.sessionFlushed(session);
    }

    public SessionManager getSessionManager() {
        return _sessionManager;
    }
    
    public void sessionReleased(ISession session) { //Introduced by PM66889 in tWAS, adding in as part of PM87133
        _sessionEventDispatcher.sessionReleased(session);
    }

}
