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
import java.util.logging.Level;

import javax.servlet.http.HttpSession;

import com.ibm.ws.session.store.memory.MemorySession;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.session.IGenericSessionManager;

/*
 Factory for returning session context's to the servlet engine
 */
public class SessionContextRegistry {

    protected static Hashtable scrSessionContexts = null;
    protected static boolean initialized = false;
    protected static SessionContextRegistry scrInstance = null;
    protected static boolean _globalSessionContext = false;
    // constants for invalidateAll SPI processing
    public static final String NO_BACKEND_UPDATE_FLAG = "*";
    public static final String UTF8 = "UTF-8";

    private static final String methodClassName = "SessionContextRegistry";

    protected static final int INITIALIZE = 0;
    protected static final int GET_SESSION_CONTEXT = 1;
    protected static final int CREATE_SESSION_CONTEXT = 2;
    protected static final int INVALIDATE_ALL = 3;

    protected static final String methodNames[] = { "initialize", "getSessionContext", "createSessionContext", "invalidateAll" };

    protected final com.ibm.ws.webcontainer.httpsession.SessionManager smgr;
    
    /*
     * Constructor
     */
    public SessionContextRegistry(com.ibm.ws.webcontainer.httpsession.SessionManager smgr) {
        // Don't leave null because could result in NPE
        // Create with size of 1 to minimize storage usage if no webapps are started
        scrSessionContexts = new Hashtable(1);
        this.smgr = smgr;
    }

    /*
     * Gets an Instance of this class
     */
    public static SessionContextRegistry getInstance() {
        /*-
         * lazy-load from subclasses is complicated. 
         * See the Double-Checked Locking (DCL) and
         * Initialization on Demand Holder (IODH) idioms.
         * Instead, initialize global instance during initialize().
         */
        return scrInstance;
    }

    /*
     * initialize
     */
    protected synchronized void initialize() {
        if (initialized)
            return;
        scrSessionContexts = new Hashtable();
        scrInstance = this; // see com.ibm.ws.session.SessionData#invalidateAll(boolean)
        SessionManagerConfig smc = getServerSMC();

        _globalSessionContext = SessionManagerConfig.getServlet21SessionCompatibility();
        if (_globalSessionContext) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, methodNames[INITIALIZE], "SessionContextRegistry.globalSessionsEnabled");

            if (LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.WARNING)) {
                if (smc.getEnableTimeBasedWrite()) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[INITIALIZE], "SessionContextRegistry.globalSessionTBWWarning");
                }
                if (smc.isUsingMemtoMem()) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[INITIALIZE], "SessionContextRegistry.globalSessionM2MWarning");
                }
            }
        }

        initialized = true;
    }

    /*
     * To get at TrackerData
     */
    public static String getTrackerData() {

        // loop through all the contexts
        Enumeration vEnum = scrSessionContexts.elements();

        StringBuffer bigStrbuf = new StringBuffer();

        bigStrbuf.append("<center><h3>Session Tracking Internals</h3></center>" + "<UL>\n");

        while (vEnum.hasMoreElements()) {
            SessionContext localContext = (SessionContext) vEnum.nextElement();
            String contextData = localContext.toHTML();
            bigStrbuf.append(contextData).append("</UL>\n");

        }

        return (bigStrbuf.toString());
    }

    /*
     * removes the sessioncontext from the registry
     */
    public static synchronized void remove(String appname) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "remove", appname);
        }
        scrSessionContexts.remove(appname);
    }

    /*
     * returns all session contexts as Enumeration
     */
    public static Enumeration getScrSessionContexts() {
        return scrSessionContexts.elements();
    }

    public SessionManagerConfig getServerSMC() {
        return this.smgr.getServerConfig();
    }

    /*
     * invalidateAll support added for LIDB3477.20 -- added for portal requirement
     */
    public void invalidateAll(String sessionId, String appName, SessionData sd, boolean goRemote, boolean fromRemote) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("for app ").append(appName).append(" id ").append(sessionId).append(" goRemote ").append(goRemote).append(" fromRemote ").append(fromRemote);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[INVALIDATE_ALL], sb.toString());
        }

        boolean backendUpdate = true;
        if (sessionId.startsWith(NO_BACKEND_UPDATE_FLAG)) { // special check to prevent each zOS servant from doing db query
            backendUpdate = false;
            sessionId = sessionId.substring(1); // remove flag
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[INVALIDATE_ALL], "setting backendUpdate to false and removed flag - sessionId: "
                                                                                                               + sessionId);
            }
        }
        // we invalidate on this server before broadcast so that in a cluster with
        // persistent
        // session's, we'll do the actual invalidation and removal from the back-end
        // here.

        if ((sd != null) && (sd.getISession() != null) && (!((MemorySession) sd.getISession()).isInvalInProgress())) { // sd will be null if fromRemote is true
            sd.invalidate(); // take care of this session first -- the session that
                             // was used to call invalidateAll
        }
        Enumeration vEnum = SessionContextRegistry.getScrSessionContexts();
        while (vEnum.hasMoreElements()) {
            SessionContext context = (SessionContext) vEnum.nextElement();
            if (fromRemote) {
                context.remoteInvalidate(sessionId, backendUpdate);
            } else {
                if (sd.getSessCtx() != context) {
                    context.invalidate(sessionId);
                }
            }
        }
        /* Change this to handle ApplicationSessions */
        if (SessionManagerConfig.getUsingApplicationSessionsAndInvalidateAll()) {
            Enumeration vEnumManagers = SessionManagerRegistry.getSessionManagerRegistry().getSessionManagers();
            while (vEnumManagers.hasMoreElements()) {
                IGenericSessionManager manager = (IGenericSessionManager) vEnumManagers.nextElement();
                callInvalidateAllOnApplicationSessionManager(manager, sessionId, backendUpdate, fromRemote);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INVALIDATE_ALL]);
        }
    }

    protected void callInvalidateAllOnApplicationSessionManager(IGenericSessionManager manager, String sessionId, boolean backendUpdate, boolean fromRemote) {
        // no op
        // overwritten in WsSessionContextRegistry
    }

    // Tells us whether security is enabled. Returns false.
    // cannot cache whether security is enabled in lWAS
    //    protected boolean getWsSecurityEnabled() {
    //        return false;
    //    }

    /*
     * Sets the properties in the SMC based upon the properties passed in.
     * Sets the hideSessionValues property if security is turned on and we haven't
     * already set the property.
     * Sets a value on the SessionContext to handle crossover
     */
    // public void setPropertiesInSMC(SessionManagerConfig _smc, Properties
    // webContainerProperties, Properties sessionManagerProperties) {
    public void setPropertiesInSMC(SessionManagerConfig _smc) {
        /*
         * cannot cache whether security is enabled in lWAS
         * boolean securityEnabled = getWsSecurityEnabled();
         * if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
         * LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setPropertiesInSMC", "smcServerSecurity=" + securityEnabled);
         * }
         * _smc.setServerSecurityEnabled(securityEnabled);
         */
        /*- The following logic exists on tWAS, but serves no purpose because hideSessionValues=true by default 
        if (!SessionManagerConfig.isHideSessionValuesPropertySet() && securityEnabled) {
            // we still need to hide the session if security is on and prop wasn't set
            SessionManagerConfig.setHideSessionValues(true);
        }
         */

        if (_smc.isDebugSessionCrossover() && SessionContext.currentThreadSacHashtable == null) {
            SessionContext.currentThreadSacHashtable = new WSThreadLocal();
        }

    }

    /*
     * gets the HttpSession by passing in the correct virtualHost, contextRoot,
     * and sessionId
     * this is overrideen in the WsSessionContextRegistry
     * this is only called by the ConvergedAppUtils class
     */
    public HttpSession getHttpSessionById(String virtualHost, String contextRoot, String sessionId) {
        // overridden in WsSessionContextRegistry
        return null;
    }
}
