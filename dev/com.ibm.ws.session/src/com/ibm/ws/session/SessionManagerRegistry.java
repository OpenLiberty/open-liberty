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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IGenericSessionManager;

public class SessionManagerRegistry {
    // ----------------------------------------
    // Private members.
    // ----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    private Map _genericSessionManagers = null;

    private static final String methodClassName = "SessionManagerRegistry";

    private static SessionManagerRegistry registry;

    private static final int GET_SESSION_MANAGER = 0;
    private static final int REGISTER_SESSION_MANAGER = 1;
    private static final int UNREGISTER_SESSION_MANAGER = 2;
    private static final String methodNames[] = { "getSessionManager", "registerSessionManager", "unregisterSessionManager" };

    // ----------------------------------------
    // Class Constructor
    // ----------------------------------------
    public SessionManagerRegistry() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 3/12/08 09:24:04");
                _loggedVersion = true;
            }
        }
        _genericSessionManagers = new Hashtable();
    }

    /**
     * Get the sessionManagerRegistry singleton
     * 
     * @return SessionManagerRegistry
     */
    public synchronized static SessionManagerRegistry getSessionManagerRegistry() {
        if (registry == null)
            registry = new SessionManagerRegistry();
        return registry;
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------

    /**
     * Method getSessionManager
     */
    public synchronized IGenericSessionManager getSessionManager(Object registryKey) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_SESSION_MANAGER], "registryKey= " + registryKey);
        }
        IGenericSessionManager sessionManager = (IGenericSessionManager) _genericSessionManagers.get(registryKey);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_SESSION_MANAGER], "sessionManager= " + sessionManager);
        }
        return sessionManager;
    }

    /**
     * Method registerSessionManager
     * <p>
     * 
     * @param key
     * @param sessionMgr
     */
    public synchronized void registerSessionManager(Object key, IGenericSessionManager sessionMgr) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[REGISTER_SESSION_MANAGER], "registryKey= " + key + " sessionMgr:" + sessionMgr);
        }
        _genericSessionManagers.put(key, sessionMgr);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REGISTER_SESSION_MANAGER]);
        }
    }

    /**
     * Method registerSessionManager
     * <p>
     * 
     * @param key
     * @param sessionMgr
     */
    public synchronized void unregisterSessionManager(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[UNREGISTER_SESSION_MANAGER], "registryKey= " + key);
        }
        _genericSessionManagers.remove(key);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[UNREGISTER_SESSION_MANAGER]);
        }
    }

    /**
     * Method getSessionManagers
     * 
     * @return Enumeration of IGenericSessionManager
     */
    public Enumeration getSessionManagers() {
        return ((Hashtable) _genericSessionManagers).elements();
    }

}