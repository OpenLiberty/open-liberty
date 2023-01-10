/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.ws.session.http.AbstractHttpSession;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.servlet.session.IBMSessionExt;
import com.ibm.wsspi.session.ISession;

/**
 * The WAS Http Session adaptation.  Extends the core HttpSessionImpl and adds WAS-specific function
 * <p> 
 * Since Servlet 6.0 : 
 *      Common methods moved up from SessionData. 
 */
public abstract class AbstractSessionData extends AbstractHttpSession implements IBMSessionExt {

    protected LinkedList lockList;
    protected HashMap locks = null;
    protected String pathInfoForAppSession = null;
    String sipCookieInfo = null;
    protected SessionContext _sessCtx;
    protected String appName;
    private boolean affinityEstablished = false;
    protected IBMApplicationSession mIBMApplicationSession = null;
    protected final static WebSphereRuntimePermission invalidateAllPerm = new WebSphereRuntimePermission("accessInvalidateAll");
    private static final String methodClassName = "AbstractSessionData";

    private final static int PUT_SESSION_VALUE = 0;
    private final static int GET_SESSION_VALUE = 1;
    private final static int REMOVE_SESSION_VALUE = 2;
    private final static int SET_IBM_APPLICATION_SESSION = 3;

    private final static String methodNames[] = { "putSessionValue", "getSessionValue", "removeSessionValue", "setIBMApplicationSession" };
    
    protected AbstractSessionData(ISession session) {
        super(session);
        
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor (ISession)");
        }
    }
    
    protected AbstractSessionData(ISession session, SessionContext sessCtx, ServletContext servCtx) {
        super(session);
        _sessCtx = sessCtx;
        appName = _sessCtx.getAppName();
        setServletContext(servCtx);
        
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor (ISession, SessionContext, ServletContext)");
        }
    }
    
    /*
     * invalidateAll(boolean)
     * 
     * @see com.ibm.wsspi.servlet.session.IBMSessionExt#invalidateAll(boolean)
     */
    public void invalidateAll(boolean remote) throws SecurityException {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String logString = "(" + remote + ") for app " + appName + " id " + getId();
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "invalidateAll", logString);
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(invalidateAllPerm);
        }
        SessionContextRegistry.getInstance().invalidateAll(getId(), _sessCtx.getAppName(), this, remote, false); // false indicates request was not remotely initiated

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "invalidateAll");
        }
    }

    /*
     * invalidateAll()
     * 
     * @see com.ibm.wsspi.servlet.session.IBMSessionExt#invalidateAll()
     * Calls invalidateAll(false)
     */
    public void invalidateAll() throws SecurityException {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "invalidateAll", "will default to invalidateAll(false)");
        }
        invalidateAll(false);
    }

    /*
     * For Session Security Integration feature.
     * Gets the user name associated with the Session. The user name could be
     * anonymous (the default that indicates an unregistered visitor), or it could
     * have been set when it was authenticated by the HTTP server or by an
     * application.
     */

    public String getUserName() {
        return getISession().getUserName();
    }

    /*
     * For Session Security Integration feature.
     * Sets the user name associated with the Session.
     */
    public void setUser(String value) {
        getISession().setUserName(value);
    }

    /*
     * For Manual Update, app calls sync() to persist session
     * 
     * @see com.ibm.websphere.servlet.session.IBMSession#sync()
     */
    public void sync() {
        getISession().flush();
    }

    /*
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String pName) {
        crossoverCheck("getAttribute");
        return getSessionValue(pName);
    }


    /*
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String,
     * java.lang.Object)
     */
    public void setAttribute(String pName, Object pValue) {
        crossoverCheck("setAttribute");
        if (pName != null && pValue == null)
            removeAttribute(pName);
        else
            putSessionValue(pName, pValue);
    }

    /*
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String pName) {
        crossoverCheck("removeAttribute");
        removeSessionValue(pName);
    }

    /*
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        crossoverCheck("invalidate");
        super.invalidate();
    }

    /*
     * putSessionValue - handles special security property
     */
    protected void putSessionValue(String pName, Object value) {
        if (pName == null) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[PUT_SESSION_VALUE], "SessionData.putValErr1");
            return;
        }
        if (value == null) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[PUT_SESSION_VALUE], "SessionData.putValErr2", pName);
            return;
        }
        super.setAttribute(pName, value);

    }

    protected Object getSessionValue(String pName) {
        return super.getAttribute(pName);
    }

    /*
     * removeSessionValue - handles special security property
     */
    protected void removeSessionValue(String pName) {
        super.removeAttribute(pName);
    }

    /*
     * Next 4 methods are for the
     * serialized session access feature
     */

    public LinkedList getLockList() {
        if (lockList == null) {
            synchronized (this) {
                lockList = new LinkedList();
            }
        }
        return lockList;
    }

    public void setSessionLock(Object tid, Object lock) {
        // cmd 275148 - create HashMap if still null
        synchronized (this) {
            if (locks == null) {
                locks = new HashMap(5);
            }
            // cmd 275148
            locks.put(tid, lock);
        }
    }

    public Object getSessionLock(Object tid) {
        if (locks == null)
            return null; // cmd 275148
        synchronized (this) {
            return locks.remove(tid);
        }
    }

    public void clearSessionLocks() {
        if (locks != null) {
            synchronized (this) {
                locks.clear();
            }
        }
    }

    /*
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        getISession().writeExternal(out);
    }

    /*
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        getISession().readExternal(in);
    }

    /*
     * set the IBM Application session
     */
    public void setIBMApplicationSession(IBMApplicationSession appSession) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SET_IBM_APPLICATION_SESSION], "for app " + appName + " IBMApplicationSession "
                            + appSession);
        }
        mIBMApplicationSession = appSession;
    }

    /*
     * Get the IBM application session - will create if necessary
     * 
     * @see
     * com.ibm.websphere.servlet.session.IBMSession#getIBMApplicationSession()
     */
    public IBMApplicationSession getIBMApplicationSession() {
        return getIBMApplicationSession(true);
    }

    /*
     * Get the IBM application session - create boolean controls whether one will
     * be created
     * 
     * @see
     * com.ibm.websphere.servlet.session.IBMSession#getIBMApplicationSession(boolean
     * )
     */
    public IBMApplicationSession getIBMApplicationSession(boolean create) {
        return null;
    }

    /*
     * Get the IBM application session - create boolean controls whether one will
     * be created
     * 
     * @see
     * com.ibm.websphere.servlet.session.IBMSession#getIBMApplicationSession(boolean
     * )
     */
    public IBMApplicationSession getIBMApplicationSession(boolean create, String logicalServerName) {
        return null;
    }

    /*
     * Get the session context for this session
     */
    public SessionContext getSessCtx() {
        return _sessCtx;
    }

    /*
     * set pathinfo for use in getIBMApplicationSession above
     */
    public void setPathInfo(String pathInfo) {
        pathInfoForAppSession = pathInfo;
    }

    public void setSIPCookieInfo(HttpServletRequest _request) {
        String sipLogicalServerName = null;
        // only handle 1 ibmappid cookie
        // LIBERTY TODO: Disabled sip cookie because it uses IExtendedRequest
        byte[] byteSipLogicalServerName = null;// ((IExtendedRequest)_request).getCookieValueAsBytes(SessionManagerConfig.sipSessionCookieName);
        if (byteSipLogicalServerName != null) {
            sipLogicalServerName = new String(byteSipLogicalServerName);
        }
        if (sipLogicalServerName != null && sipLogicalServerName.length() > 0) {
            sipCookieInfo = sipLogicalServerName;
        }
    }

    /*
     * Perform crossover checking
     */
    protected void crossoverCheck(String method) {
        if (_sessCtx._smc.isDebugSessionCrossover()) {
            if (_sessCtx.crossoverCheck(this)) {
                Object parms[] = new Object[] { appName, getId(), method, _sessCtx.getCurrentSessionId() };
                // Needed to create a LogRecord so we could have parameters and a
                // throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, "crossoverCheck", "SessionContext.CrossoverOnReference", parms,
                                                  new SessionCrossoverStackTrace());
            }
        }
    }

    /*
     * Set whether zOS servant affinity has been established
     */
    public void setAffinityEstablished(boolean val) {
        affinityEstablished = val;
    }

    /*
     * Get whether zOS servant affinity has been established
     */
    public boolean isAffinityEstablished() {
        return affinityEstablished;
    }

    /*
     * Determine if this is an overflow (i.e. throw-away) session
     * 
     * @see com.ibm.ws.httpsession.HttpSessionImpl#isOverflow()
     */
    public boolean isOverflow() {
        return getISession().isOverflow();
    }
}
