/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException;
import com.ibm.ws.security.core.SecurityContext;
import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionAffinityManager;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionCrossoverStackTrace;
import com.ibm.ws.session.SessionData;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.store.memory.MemoryStore;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.wsspi.session.IGenericSessionManager;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionAffinityManager;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.SessionAffinityContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class HttpSessionContextImpl extends SessionContext implements IHttpSessionContext
{
  private static final String methodClassName = "HttpSessionContextImpl";

  public HttpSessionContextImpl(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService)
  {
    super(smc, sap, sessionStoreService);
    // TODO Auto-generated constructor stub
  }
  
  public HttpSessionContextImpl(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService, boolean removeAttrOnInvalidate)
  {
    super(smc, sap, sessionStoreService, removeAttrOnInvalidate );    
  }
  
  /*
   *  lock session for serialized session access feature
   *  overrides empty method in SessionContext.java
   */
  public void lockSession(HttpServletRequest req, HttpSession sess) {
      String isNested = (String) req.getAttribute("com.ibm.servlet.engine.webapp.dispatch_nested");
      if (isNested == null || (isNested.equalsIgnoreCase("false"))) {
    
          try {
              long syncSessionTimeOut = (long)_smc.getSerializedSessionAccessMaxWaitTime() * 1000L; //convert to millisecond
              long syncSessionTimeOutNanos = syncSessionTimeOut * 1000000L; //convert to nanosecond

              // if session exits, start locking procedures
              if (sess != null) {
                  Object lock = new Object(); //create a new lock object for this request; 
                  LinkedList ll = ((SessionData)sess).getLockList(); //gets the linked lists of lock objects for this session;
                  
                  // PK09786 BEGIN -- Always synchronize on linklist before lock to avoid deadlock 
                  synchronized (ll) {
                     ((SessionData)sess).setSessionLock(Thread.currentThread(), lock); //adds thread to WsSession locks hashtable so we know who to notify in PostInvoke
                     ll.addLast(lock); 
                  }       //PK19389 when another thread is in sessionPostInvoke, trying to lock linkedlist in order to notify the thread in lock.wait()
                  if (ll.size() > 1) {
                      long before = System.nanoTime();
                      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                          LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[LOCK_SESSION], "waiting...");
                      }
                      synchronized (lock) {
                          lock.wait(syncSessionTimeOut);
                      }
                      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                          LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[LOCK_SESSION], "Done waiting.");
                      }
                      long after = System.nanoTime();

                      synchronized (ll) {
                          // if timed out
                          if ((after - syncSessionTimeOutNanos) >= before) {
                              if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                  LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[LOCK_SESSION], "notified after wait timed out");
                              }
                              // 118672 - we want to fail if we aren't supposed to get the access session
                              if (!_smc.getAccessSessionOnTimeout()) {
                                  ll.remove(lock);
                                  LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[LOCK_SESSION], "WsSessionContext.timeOut");
                                  throw new RuntimeException("Session Lock time outException");
                              }
                          }
                      }
                  }
              // PK09786 END
              }
              if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                  LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[LOCK_SESSION], sess);
              }
              //return sess;
          } catch (InterruptedException e) {
              com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.webcontainer.session.impl.HttpSessionContextImpl.lockSession", "133", this);
              LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[LOCK_SESSION], "CommonMessage.exception", e);
          }
      } //close if(isNested...)
  }
  
  /*
   *  unlock session for serialized session access feature
   *  overrides empty method in SessionContext.java
   */
  public void unlockSession(HttpSession sess) {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
          LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[UNLOCK_SESSION]);
      }
      SessionData session= (SessionData)sess;
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

  /*
   * Called by webcontainer to do prilimary session setup, once per webapp per
   * request.
   */
  public HttpSession sessionPreInvoke(HttpServletRequest req, HttpServletResponse res)
  {
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SESSION_PRE_INVOKE]);
    }

    // getSessionAffinityContext(req); // this call will create sac if necessary

    // PK01801 Due to componentization changed in webcontainer for v7, must make
    // this
    // check and call setRunningCollaborators(true) here for
    // sessionPreInvoke..to prevent
    // possible UnauthorizedSessionRequestException from being generated by
    // collaborator.
    if (_smc.getIntegrateSecurity() && req instanceof IExtendedRequest)
    {
      ((IExtendedRequest) req).setRunningCollaborators(true); // PK01801
    }

    HttpSession sess = null;

    try { //start SCWI 106607 (PM86470) - add try/catch block
        sess = getIHttpSession(req, res, false, _smc.getOnlyCheckInCacheDuringPreInvoke()); // false
        // ->
        // don't
        // create
        if (_smc.getIntegrateSecurity() && req instanceof IExtendedRequest)
        {
            ((IExtendedRequest) req).setRunningCollaborators(false); // PK01801 set
            // back to false
        }

        // lock session if serialization feature enabled
        if (_smc.getAllowSerializedSessionAccess())
        {
            lockSession(req, sess);
        }

    } catch (IllegalStateException e) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[SESSION_PRE_INVOKE], "IllegalStateException occurred getting the session during preinvoke possibly due to a timing window.  Continuing with the request", e);
        }
    } // end SCWI 106607 (PM86470)
    
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SESSION_PRE_INVOKE]);
    }
    return sess;
  }

  /*
   * isValid - called by webcontainer to ensure its ok to give out the session
   * he has cached for the request
   */
  public boolean isValid(HttpSession sess, HttpServletRequest req, boolean create)
  {
    // create local variable - JIT performance improvement
    final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[IS_VALID]);
    }

    ISession isess = ((SessionData) sess).getISession();
    boolean valid = isess.isValid();
    if (valid)
    {
      if (_smc.getIntegrateSecurity())
      {
          try {
              checkSecurity((SessionData)sess, req); // PK01801 check security here - 
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
      if (_smc.getModifyActiveCountOnInvalidatedSession()) {
          _coreHttpSessionManager.getIStore().getStoreCallback().sessionReleased(isess); // PM87133, since the session is no longer valid, it's also no longer active, we need to decrement active count
      }
    }
    if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[IS_VALID], "" + valid);
    }
    return valid;
  }
  
  // PK80539: New crossoverCheck method implemented for PK80539
  public boolean crossoverCheck(HttpServletRequest req, HttpSession session) {
      boolean collab = false;
      if (req != null) {
          collab = ((IExtendedRequest) req).getRunningCollaborators();
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
              LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "crossoverCheck", "" + collab);
      }

      if (!collab) {
          if (!session.isNew()) { // don't check new sessions cuz incoming id may be invalid
              String inUseSessionId = getCurrentSessionId();
              if (inUseSessionId != null) { // we're on dispatch thread, not user thread or tbw/inval thread
                  if (!(inUseSessionId.equals(session.getId()))) {
                      return true;
                  }
              }
          }
      }//PK80539 exit 
      return false;
  }

  /*
   * SessionAffinityContext keeps track of incoming and outgoing cache id,
   * session id, and clone ids. If cookies are enabled, we may have multiple
   * sac's on a request if it is dispatched to other apps that have a different
   * cookie name, cookie path, cookie domain combination.
   */
  protected SessionAffinityContext getSessionAffinityContext(HttpServletRequest _request)
  {

    Hashtable sacHashtable = (Hashtable) ((IExtendedRequest) _request).getSessionAffinityContext();

    if (sacHashtable == null)
    {
      sacHashtable = new Hashtable();
      ((IExtendedRequest) _request).setSessionAffinityContext((Object) sacHashtable);
      if (_smc.isDebugSessionCrossover())
      {
        currentThreadSacHashtable.set(sacHashtable);
      }
    }

    String sacKey = _smc.getSessionAffinityContextKey();
    SessionAffinityContext sac = (SessionAffinityContext) sacHashtable.get(sacKey);

    if (sac != null)
    {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_SESSION_AFFINITY_CONTEXT], "Found sac on request for key " + sacKey);
      }
    }
    else
    {
      sac = _sam.analyzeRequest(_request);
      sacHashtable.put(sacKey, sac);
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_SESSION_AFFINITY_CONTEXT], "Created sac and stored on request for key " + sacKey);
      }
    }
    return sac;
  }

  /*
   * get the requested session id
   */
  public String getRequestedSessionId(HttpServletRequest _request)
  {
    SessionAffinityContext sac = getSessionAffinityContext(_request);
      
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
      LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_REQUESTED_SESSION_ID], sac.isFirstSessionIdValid());
    }

    String reqSessId = sac.getRequestedSessionID();

    // if we have multiple session ids, the "requested" one is either the
    // first one that is valid, or if none are valid, the first one in the list
    boolean idIsValid = _coreHttpSessionManager.isRequestedSessionIDValid(reqSessId, 0);

    // getSession may or may not have been called. We need to check if the first
    // id is valid and if not, then check if we need to update the allSessionIds
    // list
    if (reqSessId != null && !idIsValid && !sac.isAllSessionIdsSetViaSet()
                    || !sac.isAllSessionIdsSetViaSet() && !sac.isFirstSessionIdValid()) { //PM89885 added || condition
      List allSessionIds = _sam.getAllCookieValues(_request);
      sac.setAllSessionIds(allSessionIds);
    }

    if (sac.getNumSessionIds() > 1)
    {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_REQUESTED_SESSION_ID], "multiple incoming ids");
      }
      while (!idIsValid && _sam.setNextId(sac))
      {
        reqSessId = sac.getRequestedSessionID();
        idIsValid = _coreHttpSessionManager.isRequestedSessionIDValid(reqSessId, 0);
      }
      if (!idIsValid)
      {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
          LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_REQUESTED_SESSION_ID], "none valid - return first");
        }
        reqSessId = sac.getFirstRequestedSessionID();
      }
    } else if (!idIsValid && !sac.isFirstSessionIdValid()){
        // Only session available is the one sent from the client so return that one
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
          LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_REQUESTED_SESSION_ID], "return the only one id sent from client");
        }
        reqSessId = sac.getFirstRequestedSessionID();
    }
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_REQUESTED_SESSION_ID], reqSessId);
    }
    return reqSessId;
  }

  /*
   * Determine if the requested session id is valid. We will get the session
   * passed in if it exists -- we know that getSession has been called
   */
  public boolean isRequestedSessionIdValid(HttpServletRequest _request, HttpSession sess)
  {

    SessionAffinityContext sac = getSessionAffinityContext(_request);
    String sessionId = sac.getRequestedSessionID();

    if ((sessionId != null) && (sess != null) && (!sess.isNew() || !_smc.checkSessionNewOnIsValidRequest()) && sessionId.equals(sess.getId()))
    {
      return true;
    }
    return false;
  }

  /*
   * determine if the requested session id is from a cookie
   */
  public boolean isRequestedSessionIdFromCookie(HttpServletRequest _request)
  {

    SessionAffinityContext sac = getSessionAffinityContext(_request);
    return sac.isRequestedSessionIDFromCookie();
  }

  /*
   * Get the session. Look for existing session only in cache if cacheOnly is
   * true. Only called this way from sessionPreInvoke as a performance
   * optimization. Won't create session on this call either. If create is true
   * (called from app), we'll create the session if it doesn't exist.
   */
  protected HttpSession getIHttpSession(HttpServletRequest _request, HttpServletResponse _response, boolean create, boolean cacheOnly)
  {
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

    SessionData sd = (SessionData) session;


    if (sd != null) {
        // security integration stuff
        if (_smc.getIntegrateSecurity()) {
            SecurityCheckObject securityCheckObject = doSecurityCheck(sd, _request, create);
            if (securityCheckObject.isDoSecurityCheckAgain()) {
                boolean reuseId = shouldReuseId(_request,sac) && 
                        checkSessionIdIsRightLength(_sam.getInUseSessionID(_request, sac)); 
                session = (HttpSession) _coreHttpSessionManager.createSession(_request, _response, sac, reuseId);
                sd = (SessionData)session;
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
          setSIPCookieIfApplicable(_request, _response, sd);
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
  
  private SecurityCheckObject doSecurityCheck(SessionData sd, HttpServletRequest _request, boolean create) {
      SecurityCheckObject securityCheckObject = new SecurityCheckObject();
      if (sd.isNew()) { // set user name
          String userName = null;
          if ((CollaboratorHelperImpl.getCurrentSecurityCollaborator(sd.getServletContext()) != null)  &&
                          (WSSecurityHelper.isServerSecurityEnabled()))
          {
              userName = SecurityContext.getUser();
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

  /*
   * This method called by webcontainer when app requests session. We always
   * pass false as cacheOnly parm; we need to search for existing session in
   * cache and persistent stores when the app requests session
   */
  public HttpSession getIHttpSession(HttpServletRequest _request, HttpServletResponse _response, boolean create)
  {
    return getIHttpSession(_request, _response, create, false);
  }

    /*
     * Get the session. Look for existing session
     * Only called this way from sessionPreInvoke as a performance
     * optimization. Won't create session on this call either.
     */
    public String getSessionUserName(HttpServletRequest request, HttpServletResponse response)
    {
        HttpSession session = null;

        SessionAffinityContext sac = getSessionAffinityContext(request);

        if (!sac.isFirstSessionIdValid() && !sac.isAllSessionIdsSetViaSet()) { //PM89885
            @SuppressWarnings("rawtypes")
            List allSessionIds = _sam.getAllCookieValues(request);
            sac.setAllSessionIds(allSessionIds);
            _sam.setNextId(sac); // we got rid of the first one in setAllSessionIds, get the next one so that we have an id to work with
        }

        String id = _sam.getInUseSessionID(request, sac);

        if (id != null) {
            session = (HttpSession) _coreHttpSessionManager.getSession(request, response, sac, false); // don't create here
        }
        if( session != null ){
            SessionData sd = (SessionData) session;
            return sd.getUserName();            
        } else{
            return null;
        }
    }
  
  /*
   * determine if the requested session id is from a url
   */
  public boolean isRequestedSessionIdFromUrl(HttpServletRequest req)
  {
    SessionAffinityContext sac = getSessionAffinityContext(req);
    return sac.isRequestedSessionIDFromURL();
  }

  /*
   * This method is called only when sesssion security integration is on. It is
   * called from getIHttpSession (before giving session out to webcontainer) and
   * from isValid, which is called by webcontainer prior to giving out cached
   * session. This ensures we can safely give out session to collaborators while
   * denying applications with the UnauthorizedSessionRequestException.
   */
  protected void checkSecurity(SessionData s, HttpServletRequest req)
  {
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
      auth = SecurityContext.getUser();
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
   * shouldReuseId Determines if we have a session ID that should be reused
   * 
   * If cookies are enabled on the server, we have a SessionAffinityContext for
   * each CookieName + Cookie Path + Cookie Domain combination. When there are
   * multiple apps involved in a single request, and the cookie settings are
   * different, each app gets its own cookie and therefore gets its own session
   * id.
   * 
   * If URL Rewriting is enabled (without cookies) on a server, then there is
   * only one sac for the request since there is only 1 url rewrite identifier
   * per server (default jsessionid).
   * 
   * If both cookies and URL rewriting is enabled on the server, and the apps
   * have differnt cookie attributes, things get complicated. We should use
   * different ids for each app if we know that the client supports cookies. If
   * we are unable to determine whether the client supports cookies we need to
   * look in other sacs, and if the response id is set, reuse it.
   */
  protected boolean shouldReuseId(HttpServletRequest _request, SessionAffinityContext sac)
  {
    boolean reuseId = sac.isResponseIdSet() || sac.isRequestedSessionIDFromSSL();
    if (!reuseId)
    {
      if (_smc.getEnableUrlRewriting())
      {
        boolean useCookies = _smc.getEnableCookies() && _request.getHeader("Cookie") != null;
        if (!useCookies)
        { // Either the server doesn't support cookies, or we didn't get
          // any cookies from the client, so we have to assume url-rewriting.
          // Note that the client may indeed support cookies but we have no
          // way to know for sure.
          Object objSacHashtable = ((IExtendedRequest) _request).getSessionAffinityContext();
          Hashtable sacHashtable = (Hashtable) objSacHashtable;
          if (sacHashtable.size() > 1)
          { // multiple sacs on request -- can only happen if one or more apps
            // in dispatch
            // chain support cookies.
            Enumeration sacEnum = sacHashtable.elements();
            SessionAffinityContext otherSac;
            String otherResponseId;
            // look through sacs and take the first response id we find
            while (sacEnum.hasMoreElements())
            {
              otherSac = (SessionAffinityContext) sacEnum.nextElement();
              otherResponseId = otherSac.getResponseSessionID();
              if (otherResponseId != null)
              {
                sac.setResponseSessionID(otherResponseId);
                reuseId = true;
                break;
              }
            }
          }
        }
      }
    }
    // We must check SessionManagerConfig.isIdReuse() at the end to ensure the
    // response id is
    // properly set in the current sac.
    return (reuseId || SessionManagerConfig.isIdReuse());
  }

  //SIP specific method do encode the httpsession id in the url from a ConvergedHttpSession
  //passing a session object and no request object to the encodeUrl(Session, Request, url) method
  public String encodeURLForSipConvergedApps(HttpSession session, String url) {
      return encodeURL(session, null, url);
  }
  
  /*
   * encodeURL
   */
  public String encodeURL(HttpSession sess, HttpServletRequest req, String url)
  {
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      String traceUrl = url; // PK11017
      if ((SessionManagerConfig.isHideSessionValues()) && (traceUrl != null))
      {
        int x = traceUrl.indexOf("?");
        if (x != -1)
        {
          traceUrl = traceUrl.substring(0, x);
        }
      }
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ENCODE_URL], traceUrl);
      }
    }
    if (sess == null)
    {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], null);
      }
      return null;
    }

    if (req==null) { //this is coming from the SIP request - use the session object
        url=((SessionAffinityManager)_sam).encodeURL(sess, url);
    } else {
        SessionAffinityContext sac = getSessionAffinityContext(req);
        if (shouldEncodeURL(url, req))
        {
            if (_smc.isDebugSessionCrossover() && crossoverCheck(req, sess))
            {
                Object parms[] = new Object[] { getAppName(), sess.getId(), getCurrentSessionId() };
                // Needed to create a LogRecord so we could have parameters and a
                // throwable in the same log
                LoggingUtil.logParamsAndException(LoggingUtil.SESSION_LOGGER_CORE, Level.SEVERE, methodClassName, methodNames[ENCODE_URL], "SessionContext.CrossoverOnReturn", parms,
                                                  new SessionCrossoverStackTrace());
            }
            else
            {
                url = _sam.encodeURL(req, url, sac);
            }
        }
    }

    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
    {
      String traceUrl = url; // PK11017
      if ((SessionManagerConfig.isHideSessionValues()) && (traceUrl != null))
      {
        int x = traceUrl.indexOf("?");
        if (x != -1)
        {
          traceUrl = traceUrl.substring(0, x);
        }
      }
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
      {
        LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENCODE_URL], "new url is " + traceUrl);
      }
    }
    return url;
  }

  /*
   * createAffinityManager - create the affinity manager
   */
  public ISessionAffinityManager createSessionAffinityManager(SessionManagerConfig smc, SessionContext sctx, IStore istore)
  {
    return new SessionAffinityManagerImpl(smc, sctx, istore);
  }

  /*
   * createSessionObject
   */
  public Object createSessionObject(ISession isess, ServletContext servCtx)
  {
    return new HttpSessionImpl(isess, this, servCtx);
  }

  /*
   * create the core session manager
   */
  public IGenericSessionManager createCoreSessionManager()
  {
    return super.createCoreSessionManager();
  }
  
  @Override
  protected MemoryStoreHelper createStoreHelper(ServletContext sc) {
      return new MemoryStoreHelperImpl(sc);
  }
 
  public SessionManagerConfig getWASSessionConfig()
  {
    // TODO Auto-generated method stub
    return super._smc;
  }

  // PK80439: Check that id is of exact length permitted as determined by the session length 
  // custom property
  private boolean checkSessionIdIsRightLength( String sessionIdOnly )
  {
      boolean correctLength = true;
      boolean forceSessionIdLengthCheck = _smc.getForceSessionIdLengthCheck();

      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
          LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CHECK_SESSSIONID_IS_RIGHT_LENGTH], sessionIdOnly );
      }
          
      if ( (sessionIdOnly == null) || (! forceSessionIdLengthCheck) )
      {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
              LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CHECK_SESSSIONID_IS_RIGHT_LENGTH], "no check" );
          }
          return true;
      }

      // 1. The session id length matches exactly the expected length.
      if ( sessionIdOnly.length() != SessionManagerConfig.getSessionIDLength() )
      {
          String msg = " Incoming id " + sessionIdOnly + " (" + sessionIdOnly.length() + ")" + 
                       " failed length check against " + SessionManagerConfig.getSessionIDLength();
          LoggingUtil.SESSION_LOGGER_CORE.logp
              ( Level.WARNING, methodClassName, methodNames[CHECK_SESSSIONID_IS_RIGHT_LENGTH], 
                "CommonMessage.miscData" , msg );
          correctLength = false;
      }
      
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
          LoggingUtil.SESSION_LOGGER_CORE.exiting( methodClassName, methodNames[CHECK_SESSSIONID_IS_RIGHT_LENGTH], correctLength );
      }

      return correctLength;
      
  } // end "checkSessionIdIsRightLength"

  private static class SecurityCheckObject {
      private SessionData sd=null;
      private boolean doSecurityCheckAgain=false;
      
      SecurityCheckObject() {}
      
      SessionData getSessionObject() {
          return sd;
      }
      
      boolean isDoSecurityCheckAgain() {
          return doSecurityCheckAgain;
      }
      
      void setSessionObject(SessionData sd) {
          this.sd=sd;
      }
      
      void setDoSecurityCheckAgain(boolean b) {
          this.doSecurityCheckAgain=b;
      }
      
  }

}
