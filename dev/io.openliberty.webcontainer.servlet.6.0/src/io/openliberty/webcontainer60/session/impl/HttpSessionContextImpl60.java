/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.session.impl;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException;
import com.ibm.ws.security.core.SecurityContext;
import com.ibm.ws.session.SessionAffinityManager;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionCrossoverStackTrace;
import com.ibm.ws.session.SessionData;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.store.memory.MemoryStore;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.session.impl.HttpSessionImpl;
import com.ibm.ws.webcontainer31.session.impl.HttpSessionContext31Impl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.SessionAffinityContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import io.openliberty.session.SessionData60;

public class HttpSessionContextImpl60 extends HttpSessionContext31Impl {
    private static final String methodClassName = "HttpSessionContextImpl60";


    /**
     * @param smc
     * @param sap
     * @param sessionStoreService
     */
    public HttpSessionContextImpl60(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService);
    }

    /*
     * createSessionObject
     */
    public Object createSessionObject(ISession isess, ServletContext servCtx) {
        return new HttpSessionImpl60(isess, this, servCtx);
    }


    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer31.session.IHttpSessionContext31#generateNewId(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
     * javax.servlet.http.HttpSession)
     * 
     * Update for SessionData60
     */
    public HttpSession generateNewId(HttpServletRequest request, HttpServletResponse response, HttpSession existingSession) {
        SessionAffinityContext sac = getSessionAffinityContext(request);
        HttpSession session = (HttpSession) _coreHttpSessionManager.generateNewId(request, response, sac, ((SessionData60) existingSession).getISession());
        return session;
    }

    /*
     * Duplicate and override to support SessionData60
     */
    
    public boolean isValid(HttpSession sess, HttpServletRequest req, boolean create) {
        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[IS_VALID]);
        }

        ISession isess = ((SessionData60) sess).getISession();
        boolean valid = isess.isValid();
        if (valid)
        {
            if (_smc.getIntegrateSecurity())
            {
                try {
                    checkSecurity((SessionData60) sess, req); // PK01801 check security here - 
                    // may result in UnauthorizedSessionRequestException
                } 
                catch (UnauthorizedSessionRequestException unauthException) {
                    if (_smc.getInvalidateOnUnauthorizedSessionRequestException()) {
                        valid = false; //let the getSession code handle the invalidation
                    } else {
                        if (create || _smc.getThrowSecurityExceptionOnGetSessionFalse()) {
                            throw unauthException;
                        } else {
                            //don't throw the exception if they just requested it with getSession(false)
                            valid=false;
                        }
                    }
                }
            }
            if (valid && (_smc.isDebugSessionCrossover()) && (crossoverCheck(req, sess) )) { //PK80539
                valid = false;
                Object parms[] = new Object[] { getAppName(), sess.getId(), getCurrentSessionId() };
                // Needed to create a LogRecord so we could have parameters and a
                // throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, methodNames[IS_VALID], "SessionContext.CrossoverOnRetrieve", parms,
                                                  new SessionCrossoverStackTrace());
            }
        }
        if (!valid) // if the session is not valid, then most likely we were processing it on the current thread and then it was invalidated by another thread.
        {
            ((IExtendedRequest) req).setSessionId(null);
            SessionAffinityContext sac = getSessionAffinityContext(req);
            sac.setResponseSessionID(null);
            /* PM73188 - Fixing regression from PM87133. Some customers were seeing a negative active count due to isValid being called multiple times in a row.
             * If the customer still needs this setting on, then they need to set ModifyActiveCountOnInvalidatedSession="true" in server.xml.
             */
            boolean active = isess.getRefCount() > 0;
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
            {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[IS_VALID], "Still in the service method " + active);
            }
            if (!active && _smc.getModifyActiveCountOnInvalidatedSession()) {
                _coreHttpSessionManager.getIStore().getStoreCallback().sessionReleased(isess); // PM87133, since the session is no longer valid, it's also no longer active, we need to decrement active count
            }
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[IS_VALID], "" + valid);
        }
        return valid;
    }

    /*
     * Duplicate and override in order to use SessionData60
     */
    protected HttpSession getIHttpSession(HttpServletRequest _request, HttpServletResponse _response, boolean create, boolean cacheOnly) {

        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_IHTTP_SESSION], "createIfAbsent = " + create);
        }

        HttpSession session = null;

        SessionAffinityContext sac = getSessionAffinityContext(_request);

        if (!sac.isFirstSessionIdValid() && !sac.isAllSessionIdsSetViaSet()) { //PM89885
            List allSessionIds = _sam.getAllCookieValues(_request);
            sac.setAllSessionIds(allSessionIds);
            _sam.setNextId(sac); // we got rid of the first one in setAllSessionIds, get the next one so that we have an id to work with
        }

        String id = _sam.getInUseSessionID(_request, sac);

        /*
         * PK68691 retrieve header $WSFO set by Plugin. This header indicates the
         * failover request. Session manager will drop the in-memory session and
         * retrieves the latest session copy from the backend if the incoming
         * request is the failover one.
         */
        if ( (id != null) && (Boolean.valueOf(_request.getHeader("$WSFO")).booleanValue()) ) {
            IStore iStore = _coreHttpSessionManager.getIStore();
            iStore.removeFromMemory( id );
        }

        if (id != null) {
            if ( (!cacheOnly) || (_coreHttpSessionManager.getIStore().getFromMemory(id) != null)) {
                session = (HttpSession) _coreHttpSessionManager.getSession(_request, _response, sac, false); // don't create here
            }
        }

        if (session != null)
        { // we got existing session
            id = _sam.getInUseSessionID(_request, sac); // cmd 408029 - id may have
            // changed if we received
            // multiple session cookies
            if (session.getMaxInactiveInterval() == 0)
            {
                // Max Inact of 0 implies session is invalid -- set by remote
                // invalidateAll processing
                // we expect invalidator thread to clean it up, but if app requests the
                // session before that
                // happens, invalidate it here so it isn't given back out to app.
                session.invalidate();
                session = null;
            }
            else if (!id.equals(session.getId()))
            { // always do basic crossover check
                Object parms[] = new Object[] { getAppName(), session.getId(), id };
                // Needed to create a LogRecord so we could have parameters and a
                // throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, methodNames[GET_IHTTP_SESSION], "SessionContext.CrossoverOnRetrieve",
                                                  parms, new SessionCrossoverStackTrace());
                session = null; // don't give out wrong session, but if create
                // is true, we'll continue to create new session
            }
            else if (_smc.isDebugSessionCrossover() && (crossoverCheck(_request, session)))
            { // crossover detection
                // must be enabled by DebugSessionCrossover property
                Object parms[] = new Object[] { _sap.getAppName(), session.getId(), getCurrentSessionId() };
                // Needed to create a LogRecord so we could have parameters and a
                // throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, methodNames[GET_IHTTP_SESSION], "SessionContext.CrossoverOnRetrieve",
                                                  parms, new SessionCrossoverStackTrace());
                session = null;
            }
        }

        boolean createdOnThisRequest = false;
        if ((session == null) && create)
        {
            // PK80439: Validate that session id meets length requirements
            boolean reuseId = shouldReuseId(_request,sac) && 
                            checkSessionIdIsRightLength(_sam.getInUseSessionID(_request, sac)); 
            session = (HttpSession) _coreHttpSessionManager.createSession(_request, _response, sac, reuseId);
            createdOnThisRequest = true;
        }

        SessionData60 sd = (SessionData60) session;


        if (sd != null) {
            // security integration stuff
            if (_smc.getIntegrateSecurity()) {
                SecurityCheckObject securityCheckObject = doSecurityCheck(sd, _request, create);
                if (securityCheckObject.isDoSecurityCheckAgain()) {
                    boolean reuseId = shouldReuseId(_request,sac) && 
                                    checkSessionIdIsRightLength(_sam.getInUseSessionID(_request, sac)); 
                    session = (HttpSession) _coreHttpSessionManager.createSession(_request, _response, sac, reuseId);
                    sd = (SessionData60)session;
                    createdOnThisRequest = true;
                    securityCheckObject = doSecurityCheck(sd, _request, create); //shouldn't have an issue with the session being owned by someone else since we invalidated the previous session and created a brand new session
                }
                sd = securityCheckObject.getSessionObject();
            }
            // cmd 372189 save pathinfo in case it contains ibmappid (for SIP) and app
            // calls sess.getIBMApplicationSession()
            // Note: concurrent requests should have same ibmappid encoded so we
            // should be ok even with concurrent requests
            if (isSIPApplication)
            {
                sd.setSIPCookieInfo(_request);
                sd.setPathInfo(_request.getPathInfo());
                // this needs to be called here after the pathInfo has been set up
                // only want to call this when we create a session since the logicalname
                // won't change
                // changed "if (createdOnThisRequest) {" to
                // "if (!_response.isCommitted()) {"
                // if the server shuts down, the logicalname does change, and we may
                // retrieve the session from the backend.
                // Therefore, we should always create the sip cookie (although, we don't
                // want to throw an exception if the response is committed)
                if (!_response.isCommitted())
                {
                    //setSIPCookieIfApplicable(_request, _response, sd);
                    //PMDINH do nothing from parent setSIPCookieIfApplicable
                }
            }
        }

        if (_sap.getAllowDispatchRemoteInclude())
        {
            ((SessionAffinityManager) _sam).setSessionId(_request, sac); // sets the
            // full
            // session id
            // (includes
            // cacheid/cloneids)
            // on the
            // request
            // for RRD
        }

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_IHTTP_SESSION]);
        }
        return sd;

    }


    //Duplicate due to new SessionData60
    protected SecurityCheckObject doSecurityCheck(SessionData60 sd, HttpServletRequest _request, boolean create) {
        SecurityCheckObject securityCheckObject = new SecurityCheckObject();
        if (sd.isNew()) { // set user name
            String userName = null;
            if ((CollaboratorHelperImpl.getCurrentSecurityCollaborator(sd.getServletContext()) != null)  &&
                            (WSSecurityHelper.isServerSecurityEnabled()))
            {
                userName = getUser();
            }
            else {
                userName = _request.getRemoteUser();
            }
            if (userName != null) {
                sd.setUser(userName);
            }
        } else {  // not new....check security
            try {
                checkSecurity(sd, _request);
            }
            catch (UnauthorizedSessionRequestException unauthException) {
                if (_smc.getInvalidateOnUnauthorizedSessionRequestException()) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "doSecurityCheck", "UnauthorizedSessionRequestException thrown - invalidating session");
                    }
                    sd.invalidate();
                    sd = null;
                    securityCheckObject.setDoSecurityCheckAgain(create);
                } else {
                    if (create || _smc.getThrowSecurityExceptionOnGetSessionFalse()) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "doSecurityCheck", "throwing an UnauthorizedSessionRequestException");
                        }
                        throw unauthException;
                    } else {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "doSecurityCheck", "getSession(false) can not access the session due to an UnauthorizedSessionRequestException");
                        }
                        //don't throw the exception if they just requested it with getSession(false)
                        sd = null;
                    }
                }
            }
        }
        securityCheckObject.setSessionObject(sd);
        return securityCheckObject;
    }

    private static class SecurityCheckObject {
        private SessionData60 sd=null;
        private boolean doSecurityCheckAgain=false;

        SecurityCheckObject() {}

        SessionData60 getSessionObject() {
            return sd;
        }

        boolean isDoSecurityCheckAgain() {
            return doSecurityCheckAgain;
        }

        void setSessionObject(SessionData60 sd) {
            this.sd=sd;
        }

        void setDoSecurityCheckAgain(boolean b) {
            this.doSecurityCheckAgain=b;
        }

    }

    /*
     * Can't user parent getUser somehow, even change its modifier to protected getUser
     */
    private String getUser() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return SecurityContext.getUser();
            }
        });
    }

    protected void checkSecurity(SessionData60 s, HttpServletRequest req) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CHECK_SECURITY]);
        }

        String auth = null;
        String owner = s.getUserName();

        if ((CollaboratorHelperImpl.getCurrentSecurityCollaborator(s.getServletContext()) != null)  &&
                        (WSSecurityHelper.isServerSecurityEnabled()))
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
            {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[CHECK_SECURITY], "calling getUser");
            }
            auth = getUser();
        }
        else
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
            {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[CHECK_SECURITY], "calling getRemoteUser");
            }
            auth = req.getRemoteUser();
        }
        if (auth == null)
        {
            auth = MemoryStore.ANONYMOUS_USER;
        }
        // the request comes in under an authentication that's different
        // than who created and/or currently owns the session
        if (auth!=null && owner!=null && !((_smc.getSecurityUserIgnoreCase() && auth.equalsIgnoreCase(owner)) || auth.equals(owner))) 
        {
            // PM04304: if SecurityUserIgnoreCase is enabled then compare author and owner ignoring case
            // a user who created a session while unauthenticated
            // has now been authenticated
            // change the owner of the session
            if (owner.equals(MemoryStore.ANONYMOUS_USER))
            {
                s.setUser(auth);
            }
            else
            {
                // a session owned by an authenticated user is accessed
                // with the wrong authentication
                if (!((IExtendedRequest) req).getRunningCollaborators())
                {
                    // only throw this exception if we're NOT running collaborators...
                    // collaborators will be treated as trusted system code, so they will
                    // get the session even if security integration check fails.
                    Object params[] = { auth, owner };
                    if (!_smc.getInvalidateOnUnauthorizedSessionRequestException()) { //PM93356 (SCWI 105957) only log if InvalidateOnUnauthorizedSessionRequestException is not set
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[CHECK_SECURITY], "SessionContext.unauthAccessError", params);
                    }          
                    if (_smc.isDebugSessionCrossover()) {
                        new Throwable("Throw exception to find out who sent the unauthorized request !!!").printStackTrace(System.out);  
                    }          
                    UnauthorizedSessionRequestException usre;
                    ResourceBundle rb = LoggingUtil.SESSION_LOGGER_CORE.getResourceBundle();
                    if (rb != null)
                    {
                        String msg = MessageFormat.format(rb.getString("SessionContext.unauthAccessError"), params);
                        usre = new UnauthorizedSessionRequestException(msg);
                    }
                    else
                    {
                        usre = new UnauthorizedSessionRequestException();
                    }
                    //I believe that this should be true for all requests, but since it is being added for an apar, 712446, I will only add it around the custom property
                    if (!_smc.getThrowSecurityExceptionOnGetSessionFalse() && !_smc.getInvalidateOnUnauthorizedSessionRequestException()) {
                        s.getISession().decrementRefCount();
                    }
                    throw usre;
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CHECK_SECURITY]);
        }
    }

    /*
     * Override the SessionContext.sessionPostInvoke due to SessionData60
     */
    public void sessionPostInvoke(HttpSession sess) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SESSION_POST_INVOKE]);
        }
        SessionData60 s = (SessionData60) sess;

        if (_smc.getAllowSerializedSessionAccess()) {
            unlockSession(sess);
        }
        if (s != null) {
            synchronized (s) {
                SessionAffinityContext sac = null;
                _coreHttpSessionManager.releaseSession(s.getISession(), sac);
                if (_coreHttpAppSessionManager != null) {
                    // try and get the Application Session in memory ... if it is there,
                    // make sure you update the backend via releaseSession
                    ISession iSess = (ISession) _coreHttpAppSessionManager.getIStore().getFromMemory(s.getId());
                    if (iSess != null) {
                        // iSess.decrementRefCount();
                        _coreHttpAppSessionManager.releaseSession(iSess, sac);
                    }
                }
            }
        }

        if (_smc.isDebugSessionCrossover()) {
            currentThreadSacHashtable.set(null);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SESSION_POST_INVOKE]);
        }
    }

    /*
     * Override HttpSessionContextImple due to new SessionData60
     */
    public void unlockSession(HttpSession sess) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[UNLOCK_SESSION]);
        }
        SessionData60 session= (SessionData60)sess;
        Object obj = session.getSessionLock(Thread.currentThread());
        if (obj!=null) {
            LinkedList linkList = session.getLockList();
            synchronized(linkList) {
                try {
                    linkList.remove(obj);
                    if (linkList.size()>0) {
                        Object nextLock = linkList.getFirst();
                        if (nextLock !=null) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[UNLOCK_SESSION], "notify after postInvoke");
                            }
                            synchronized(nextLock) {
                                nextLock.notify();
                            }
                        }
                    } else {
                        session.clearSessionLocks();
                    }
                } catch (Exception e) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[UNLOCK_SESSION], "failed to unlock session", e);
                    }
                }
            }
        }                   
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[UNLOCK_SESSION]);
        }
    }
}
