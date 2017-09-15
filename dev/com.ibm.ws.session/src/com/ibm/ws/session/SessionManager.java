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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.session.utils.IIDGenerator;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IGenericSessionManager;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionAffinityManager;
import com.ibm.wsspi.session.ISessionManagerCustomizer;
import com.ibm.wsspi.session.ISessionObserver;
import com.ibm.wsspi.session.ISessionStateObserver;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStoreCallback;
import com.ibm.wsspi.session.IStorer;
import com.ibm.wsspi.session.SessionAffinityContext;

/*
 * This is the implementation of the base Session Manager class. It drives all the registered
 * observers. It is responsible for creating the ISession objects and allows the sessionManagerRegistry
 * to customize its function.
 * @author aditya
 *
 */

public class SessionManager implements IGenericSessionManager, ISessionManagerCustomizer {
    // ----------------------------------------
    // Private members.
    // ----------------------------------------
    // XD vars
    private final int _numOfPartitions = 10;
    /*
     * int replicationType and replicationInterval in seconds(only applicable to
     * time based writes)
     */
    // from XD's class SessionConstants.TIME_BASED_WRITE public static final int
    // TIME_BASED_WRITE = 1;
    private final int _replicationType = 1;
    private final int _replicationInterval = 10;
    /*
     * flag for extension to share across apps
     */
    private final boolean _shareAcrossApps = false;
    // done XD vars

    // ----------------------------------------
    // protected members.
    // ----------------------------------------
    /*
     * For logging the CMVC file version once.
     */
    protected static boolean _loggedVersion = false;

    /*
     * A reference to the IDGenerator that will be used to create session ids.
     */
    protected IIDGenerator _idGenerator = null;

    /*
     * A reference to the external Store. The store conform to the IStore
     * interface.
     * TODO Also a store may drive invalidations or may accept
     * invalidations driven by the Session Manager.
     */
    protected IStore _store = null;

    /*
     * A reference to a list of registered _sessionObservers and
     * _sessionStateObservers.
     */
    protected ArrayList _sessionObservers = new ArrayList();
    protected ArrayList _sessionStateObservers = new ArrayList();

    /*
     * String id of this session manager.
     */
    protected String _ID;

    /*
     * ISessionStoreCallBackContext
     */
    protected IStoreCallback _storeCallback = null;

    /*
     * Session State Observer manager that drives events to all state observers
     */
    protected ISessionStateObserver _sessionStateEventDispatcher = null;

    /*
     * Session Observer manager that drives events to all session observers
     */
    protected ISessionObserver _sessionEventDispatcher = null;

    /*
     * int sessionTimeout This value applies to all sessions in this webmodule.
     * Default is 30 minutes, unless changed and set explicitly in the web.xml.
     * Units are in seconds.
     */
    protected int _sessionTimeout = 30 * 60;

    /*
     * This determines when updates are sent to the external store
     */
    protected IStorer _storer = null;

    /*
     * boolean true or false depending on whether this session manager is shared
     * across webapps
     */
    protected boolean _shareAcrossWebApps;

    /*
     * ObjectName of the stats Module associated with this session manager
     */
    protected ObjectName _statsModuleObjectName = null;

    // Used for logging
    protected static final String methodClassName = "SessionManager";
    /*
     * ISessionAffinityManager object
     */
    protected ISessionAffinityManager _sam;
    /*
     * A reference to the servletContext
     */
    protected ServletContext _servletContext = null;
    /*
     * IProtocolAdapter object
     */
    public IProtocolAdapter _adapter = null;

    private static final int CREATE_ISESSION = 0;
    private static final int SHUTDOWN = 1;
    private static final String methodNames[] = { "createISession", "shutdown" };
    /*
     * createSync - used to synchronize session creation when
     * we are reusing an input session id. This prevents concurrent
     * threads from creating multiple sessions with the same id.
     */
    private final HashMap createSync = new HashMap();

    // really is an ApplicationSessionManager
    protected SessionManager appSessionMgr = null;
    
    //APAR PI60026 - Duplicated in MemoryStore.java
    private static final String overflowId = "overflowed-session";

    // ----------------------------------------
    // Protected constructor
    // ----------------------------------------
    /**
     * Class constructor
     * 
     * @param id
     *            String name of this session manager
     */
    public SessionManager(String id) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.3 3/13/07 12:00:50");
                _loggedVersion = true;
            }
        }
        _ID = id;
        _sessionStateEventDispatcher = new SessionStateEventDispatcher(_sessionStateObservers);
        _sessionEventDispatcher = new SessionEventDispatcher(_sessionObservers);
        _storeCallback = new StoreCallback(this);
    }

    protected void setAppSessionMgr(SessionManager asm) {
        appSessionMgr = asm;
    }

    public SessionManager getAppSessionMgr() {
        return appSessionMgr;
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------

    /**
     * Method createISession
     * <p>
     * Called by the createSession method and the getSession method (when the create boolean is true) This method does the actual create and returns the ISession. The id provided
     * is
     * in advisory capability only i.e.If there is another webapp that is using this id or if we have been told to reuse the id, then we can reuse it. If not we need to create a
     * new
     * one.
     * 
     * @param id
     * @param sessionVersion
     * @param reuseTheId
     *            A boolean which tells us whether to reuse the same id when we
     *            create the session
     * @return Object
     */
    protected ISession createISession(String id, int sessionVersion, boolean reuseTheId) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CREATE_ISESSION], "id= " + id);
        }
        ISession iSession = null;
        boolean created = true; // set to false if we find another thread just
                                // created this session

        // uses the same id to create if current id exists and isIdReuse is
        // specified or RRD request
        if ((null==id) || (!reuseTheId && !_store.idExists(id)) || (reuseTheId && id.equals(overflowId))){
            // creating a session with a new id
            while (iSession == null) {
                id = _idGenerator.getID();
                iSession = _store.createSession(id, true);
            }
        } else { // reusing input id...must synchronize to ensure multiple threads don't
                 // create
            String createLock = null;
            synchronized (this) {
                createLock = (String) createSync.get(id);
                if (createLock == null) {
                    createLock = id;
                    createSync.put(id, createLock);
                }
            }
            synchronized (createLock) {
                iSession = (ISession) _store.getFromMemory(id);
                if (iSession == null) { // normal case
                    iSession = _store.createSession(id, false);
                    if (sessionVersion != -1) {
                        iSession.setVersion(sessionVersion);
                    }
                    createSync.remove(id);
                } else { // very rare case - another thread just created so return same session
                    createSync.remove(id);
                    created = false;
                }
            } // end of sync on createLock
        }

        /*- PM46948 A problem can occur when simultaneous requests for the same session are being processed 
         * on two or more servers and session persistence is enabled. This scenario means that 
         * session affinity is broken.  By spec, session affinity should be maintained.
         * 
         * In tWAS, we handle this case anyways because we do not want to return a null.  In this situation, 
         * a duplicate id on the backend is detected by this JVM, hence _store.createSession returned null for iSession.
         * 
         * In lWAS, we currently cannot distinguish duplicate key exceptions from other problems,
         * so BackedStore#createSession does not return null. As a result, we do not encounter an NPE,
         * but we also don't handle this scenario as gracefully as tWAS. PM46948 should be ported to lWAS
         * if the session manager can detect duplicate key exceptions in the future.
         */

        iSession.incrementRefCount();

        if (created) {
            iSession.setMaxInactiveInterval(_sessionTimeout);
            _sessionEventDispatcher.sessionCreated(iSession);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CREATE_ISESSION]);
        }
        return iSession;
    }
    
    @Override
    // IGenericSessionManager
    public Object generateNewId(HttpServletRequest req, HttpServletResponse resp, SessionAffinityContext sac, ISession iSession) {
        Object session = updateSessionId(req, resp, sac, iSession);
        return session;
    }

    // XD methods
    /**
     * Method createSession
     * <p>
     * Called by the HTTPSessionManager in order to create a session. The id provided is in advisory capability only i.e.If there is another webapp that is using this id, then we
     * can
     * reuse it. If not we need to create a new one. If the session id is in use by another webapp, then we need to reuse the requested sessionVersion.
     * 
     * @param id
     * @param sessionVersion
     * @return Object
     */
    public Object createSession(String id, int sessionVersion, Object correlator) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            // LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName,
            // "createSession", new Object[] {id, new Integer(sessionVersion),
            // correlator});
        }
        ISession iSession = null;

        if ((null == id) || !_store.idExists(id, correlator)) {
            id = _idGenerator.getID();
            iSession = _store.createSession(id, correlator);
        } else {
            iSession = _store.createSession(id, correlator);
            if (sessionVersion != -1) {
                iSession.setVersion(sessionVersion);
            }
        }

        iSession.incrementRefCount();
        iSession.setMaxInactiveInterval(_sessionTimeout);

        _sessionEventDispatcher.sessionCreated(iSession);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "createSession", "iSession = " + iSession);
        }
        return iSession;
    }

    public boolean needToRedirect(Object correlator) {
        return _store.needToRedirect(correlator);
    }

    /**
     * Method setExternalStore
     * <p>
     * Sets the _externalStore.
     * 
     * @param store
     */
    public void setExternalStore(IStore store) {
        _store = store;
        _store.setStoreCallback(_storeCallback);
    }

    /**
     * Method getExternalStore
     * <p>
     * Gets the _externalStore.
     */
    // same as getIStore
    public IStore getExternalStore() {
        return _store;
    }

    /**
     * Method setIdGenerator
     * <p>
     * 
     * @param idGenerator
     *            The idGenerator to set.
     */
    // same as setIDGenerator
    public void setIdGenerator(IIDGenerator idGenerator) {
        _idGenerator = idGenerator;
    }

    /**
     * Method setStorer
     * <p>
     * 
     * @param storer
     */
    // same as registerStorer
    public void setStorer(IStorer storer) {
        _storer = storer;
    }

    /**
     * Sets the stats Module object name
     * <p>
     * 
     * @param moduleObjectName
     */
    // same as registerStatsModuleObjectName
    public void setStatsModuleObjectName(ObjectName moduleObjectName) {
        _statsModuleObjectName = moduleObjectName;;
    }

    // end XD methods

    /**
     * This method is invoked by the HttpSessionManagerImpl to get at the session
     * or related session information. If the getSession is a result of a
     * request.getSession
     * call from the application, then the isSessionAccess boolean is set to true.
     * Also if the version number is available, then it is provided. If not, then
     * a value
     * of -1 is passed in. This can happen either in the case of an incoming
     * request
     * that provides the session id via URL rewriting but does not provide the
     * version/clone
     * info or in the case of a isRequestedSessionIDValid call.
     * <p>
     * 
     * @param id
     * @param version
     * @param isSessionAccess
     *            tells us if the request came from a session access (user request)
     * @return Object
     */
    public Object getSession(String id, int version, boolean isSessionAccess, Object xdCorrelator) {
        return getSession(id, version, isSessionAccess, false, xdCorrelator);
    }

    // forceSessionRetrieval can only be true when using applicationSessions
    protected Object getSession(String id, int version, boolean isSessionAccess, boolean forceSessionRetrieval, Object xdCorrelator) {
        if (isSessionAccess) {
            if (version == -1) {
                _store.refreshSession(id, xdCorrelator);
            } else {
                _store.refreshSession(id, version, xdCorrelator);
            }
        }

        ISession iSession = getSessionFromStore(id, version, isSessionAccess, forceSessionRetrieval, xdCorrelator);
        if (iSession != null) {
            if (isSessionAccess) {
                boolean stillValid = _store.checkSessionStillValid(iSession, iSession.getLastAccessedTime());
                if (stillValid) {
                    iSession.incrementRefCount();
                    _sessionEventDispatcher.sessionAccessed(iSession);
                } else {
                    iSession = null;
                }
            }
        } else {
            if (isSessionAccess) {
                _sessionEventDispatcher.sessionAccessUnknownKey(id);
            }
        }
        return iSession;
    }

    protected ISession getSessionFromStore(String id, int version, boolean isSessionAccess, boolean forceSessionRetrieval, Object xdCorrelator) {
        // forceSessionRetrieval only used for appSessions. This method is
        // overwritten in ApplicationSessionManager
        return _store.getSession(id, version, isSessionAccess, xdCorrelator);
    }

    /**
     * Method releaseSession
     * <p>
     * 
     * @param sessionObject
     * @param affinityContext
     *            associated with a given request.
     */
    @Override
    public void releaseSession(Object sessionObject, SessionAffinityContext affinityContext) {
        ISession session = (ISession) sessionObject;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "releaseSession", "sessionID = " + session.getId());
        }
        // XD change: both the decrement of the ref count, and the potential check
        // of the ref count
        // in _store.releaseSession(session) need to be synchronized in case of
        // multi-threaded access
        synchronized (session) {
            session.decrementRefCount();

            if (session.isValid()) {
                session.updateLastAccessTime();
                session.setIsNew(false);
                boolean fromCookie = false; // not always used ... if affinityContext is
                                            // null, just default to false
                if (affinityContext != null) {
                    fromCookie = affinityContext.isRequestedSessionIDFromCookie();
                }
                _storer.storeSession(session, fromCookie);
            }

            _store.releaseSession(session);
            _sessionEventDispatcher.sessionReleased(session);
        }
    }

    /**
     * Method shutdown
     * <p>
     * 
     */
    @Override
    public void shutdown() {
        ArrayList mbeanSvrs = MBeanServerFactory.findMBeanServer(null);
        if (mbeanSvrs != null && mbeanSvrs.size() > 0 && _statsModuleObjectName != null) {
            MBeanServer svr = (MBeanServer) mbeanSvrs.get(0);

            try {
                svr.unregisterMBean(_statsModuleObjectName);
            } catch (InstanceNotFoundException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.SessionManager.shutdown", "263", this);
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[SHUTDOWN], "CommonMessage.exception", e);
            } catch (MBeanRegistrationException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.session.SessionManager.shutdown", "265", this);
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[SHUTDOWN], "CommonMessage.exception", e);
            }
        }
    }

    /**
     * Method getID
     * <p>
     * 
     * @return String Returns the iD.
     */
    @Override
    public String getID() {
        return _ID;
    }

    /**
     * Method setID
     * <p>
     * 
     * @param id
     *            The iD to set.
     */
    @Override
    public void setID(String id) {
        _ID = id;
        if (_store != null) {
            _store.setID(id);
        }
    }

    /**
     * Method setSessionTimeout
     * <p>
     * 
     * @param i
     */
    @Override
    public void setSessionTimeout(int i) {
        _sessionTimeout = i;

    }

    /**
     * Method registerSessionObserver
     * <p>
     * 
     * @param observer
     */
    @Override
    public void registerSessionObserver(ISessionObserver observer) {
        synchronized (_sessionObservers) {
            _sessionObservers.add(observer);
        }
    }

    /**
     * Method getSessionEventDispatcher
     * <p>
     * 
     * @return ISessionObserver
     */
    public ISessionObserver getSessionEventDispatcher() {
        return _sessionEventDispatcher;
    }

    /**
     * Method registerSessionStateObservers
     * <p>
     * 
     * @param observer
     */
    @Override
    public void registerSessionStateObserver(ISessionStateObserver observer) {
        synchronized (_sessionStateObservers) {
            _sessionStateObservers.add(observer);
        }
    }

    /**
     * Method getSessionStateEventDispatcher
     * <p>
     * 
     * @return ISessionStateObserver
     */
    public ISessionStateObserver getSessionStateEventDispatcher() {
        return _sessionStateEventDispatcher;
    }

    /**
     * Returns the boolean state of _shareAcrossWebApps
     * <p>
     * 
     * @return boolean
     */
    @Override
    public boolean isSharedAcrossWebApps() {
        return _shareAcrossWebApps;
    }

    /**
     * Mutator for the _sharedAcrossWebApps instance
     * variable
     * <p>
     * 
     * @param share
     */
    @Override
    public void setSharedAcrossWebApps(boolean share) {
        _shareAcrossWebApps = share;
    }

    /**
     * Accessor for the _statsModuleObjectName variable
     * <p>
     * 
     * @return _statsModuleObjectName
     */
    public ObjectName getStatsModuleObjectName() {
        return _statsModuleObjectName;
    }

    /**
     * Accessor for the ISessionManagerCustomizer
     */
    @Override
    public ISessionManagerCustomizer getSessionManagerCustomizer() {
        return this;
    }

    /**
     * Method getSession
     * <p>
     * This method is called by the RequestWrapper when a getSession is done by the application. Note that the requestWrapper holds on to a reference of the returned session
     * object.
     * 
     * @see com.ibm.wsspi.session.IGenericSessionManager#getSession(ServletRequest, ServletResponse, SessionAffinityContext,boolean)
     */
    // not called from XD
    @Override
    public Object getSession(ServletRequest request, ServletResponse response, SessionAffinityContext affinityContext, boolean create) {

        /*
         * Check to see if the request provides a JSESSIONID cookie
         */
        String sessionID = _sam.getInUseSessionID(request, affinityContext);
        int sessionVersion = _sam.getInUseSessionVersion(request, affinityContext); // affinityContext.getRequestedSessionVersion();
        ISession session = null;

        /*
         * If the sessionID is not null, this may be a request with an existing
         * session.
         */
        if (sessionID != null) {
            session = (ISession) getSession(sessionID, sessionVersion, true, null);
            if (session == null && affinityContext.isRequestedSessionIDFromCookie() // perform allIds call and reset sac
                            && !affinityContext.isAllSessionIdsSetViaSet()){  //PM89885 add another check for isAllSessionIdsSetViaSet since we may have done a set in HttpSessionContextImpl.getIHttpSession
                List allSessionIds = _sam.getAllCookieValues(request);
                affinityContext.setAllSessionIds(allSessionIds);
            }
        }

        while ((session == null) && (_sam.setNextId(affinityContext))) {
            sessionID = _sam.getInUseSessionID(request, affinityContext);
            sessionVersion = _sam.getInUseSessionVersion(request, affinityContext); // affinityContext.getRequestedSessionVersion();
            session = (ISession) getSession(sessionID, sessionVersion, true, null);
        }

        /*
         * If the session is null, create a new one if required. The session manager
         * will check
         * to see if an alternate webmodule has a session created using
         * this session id. If not, the session object returned may have a different
         * session id from the one supplied.
         */
        if ((session == null) && create) {
            boolean reuseThisID = false;
            if (_store.getShouldReuseId() || affinityContext.isResponseIdSet() || affinityContext.isRequestedSessionIDFromSSL()) {
                reuseThisID = true;
            }
            session = createISession(sessionID, sessionVersion, reuseThisID);
        }

        return adaptAndSetCookie(request, response, affinityContext, session);

    }

    /*
     * Method used to create the Session from a request
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.IGenericSessionManager#createSession(javax.servlet
     * .ServletRequest, javax.servlet.ServletResponse,
     * com.ibm.wsspi.session.SessionAffinityContext, boolean)
     */
    @Override
    public Object createSession(ServletRequest req, ServletResponse res, SessionAffinityContext sac, boolean reuseTheId) {
        String id = _sam.getInUseSessionID(req, sac);
        int version = _sam.getInUseSessionVersion(req, sac); // sac.getRequestedSessionVersion();
        ISession isess = createISession(id, version, reuseTheId);
        return adaptAndSetCookie(req, res, sac, isess);
    }
    
    private Object updateSessionId(ServletRequest req, ServletResponse res, SessionAffinityContext sac, ISession sess) {
        String oldId = sess.getId();
        String id = _idGenerator.getID();
        int version = _sam.getInUseSessionVersion(req, sac); // sac.getRequestedSessionVersion();
        updateSessionIdInObject(sess, id, version);
        _sessionEventDispatcher.sessionIdChanged(oldId, sess);
        return adaptAndSetCookie(req, res, sac, sess);
    }
    
    private void updateSessionIdInObject(ISession sess, String id, int version) {
        String oldId = sess.getId();
        sess.setId(id);
        _store.updateSessionId(oldId, sess);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "updateSessionIdInObject", "updated session id from " + oldId + " to " + id + " _store = " + _store.toString());
        }
    }

    /*
     * Method used to get the HttpSession object, do some crossover checking, and
     * then
     * set the cookie.
     */
    protected Object adaptAndSetCookie(ServletRequest req, ServletResponse res, SessionAffinityContext sac, ISession isess) {
        Object adaptedSession = null;
        if (isess != null) {
            adaptedSession = _adapter.adapt(isess);
            sac.setResponseSessionID(isess.getId());
            sac.setResponseSessionVersion(isess.getVersion());
            _sam.setCookie(req, res, sac, adaptedSession);
        }
        return adaptedSession;
    }

    /*
     * Accessor for the ISessionAffinityManager
     * 
     * @see com.ibm.wsspi.session.IGenericSessionManager#getAffinityManager()
     */
    @Override
    public ISessionAffinityManager getAffinityManager() {
        return _sam;
    }

    /*
     * Accessor for the IStore
     * 
     * @see com.ibm.wsspi.session.IGenericSessionManager#getIStore()
     */
    @Override
    public IStore getIStore() {
        return _store;
    }

    /*
     * Accessor for the ISession
     * Called when doing invalidate
     * 
     * @see
     * com.ibm.wsspi.session.IGenericSessionManager#getISession(java.lang.String)
     */
    // not called from XD
    @Override
    public ISession getISession(String id) {
        return (ISession) getSession(id, 0, false, null);
    }

    /*
     * Accessor for the Session
     * Called when doing a getHttpSessionById
     * 
     * @see
     * com.ibm.wsspi.session.IGenericSessionManager#getSession(java.lang.String)
     */
    @Override
    public Object getSession(String id) {
        return getSession(id, true);
    }

    @Override
    public Object getSession(String id, boolean sessionAccess) {

        ISession isess = null;
        Object adaptedSession = null;
        if (id != null) {
            isess = (ISession) getSession(id, 0, sessionAccess, null); // we don't
                                                                       // care about
                                                                       // version,
                                                                       // pass 0
        }
        if (isess != null) {
            adaptedSession = _adapter.adapt(isess);
        }

        return adaptedSession;
    }

    /**
     * Method isRequestedSessionIDValid
     * <p>
     * 
     * @param sessionID
     * @param version
     * @return boolean
     * @see com.ibm.wsspi.session.IGenericSessionManager#isRequestedSessionIDValid(String)
     */
    @Override
    public boolean isRequestedSessionIDValid(String sessionID, int version) {
        boolean isValid = false;
        if (sessionID != null) {
            ISession session = (ISession) getSession(sessionID, version, false, null);
            if (session != null) {
                isValid = session.isValid();
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "isRequestedSessionIDValid", "" + isValid);
        }
        return isValid;
    }

    /*
     * Method which sets the store, storecallback, and does any Observer
     * registrations
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#registerStore(com.ibm.wsspi
     * .session.IStore)
     */
    @Override
    public void registerStore(IStore store) {
        _store = store;
        _store.setStoreCallback(_storeCallback);
        if (store instanceof ISessionObserver) {
            this.registerSessionObserver((ISessionObserver) store);
        }
        if (store instanceof ISessionStateObserver) {
            this.registerSessionStateObserver((ISessionStateObserver) store);
        }
    }

    /*
     * Mutator for _storer
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#registerStorer(com.ibm.
     * wsspi.session.IStorer)
     */
    @Override
    public void registerStorer(IStorer storer) {
        _storer = storer;
    }

    public IStorer getStorer() {
        return _storer;
    }

    /*
     * Mutator for _idGenerator
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#setIDGenerator(com.ibm.
     * wsspi.session.IIDGenerator)
     */
    @Override
    public void setIDGenerator(IIDGenerator IDGenerator) {
        _idGenerator = IDGenerator;
    }

    /*
     * Mutator for _servletContext
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#setServletContext(javax
     * .servlet.ServletContext)
     */
    @Override
    public void setServletContext(ServletContext context) {
        _servletContext = context;
    }

    /*
     * Mutator for _sam - SessionAffinityManager
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#registerAffinityManager
     * (com.ibm.wsspi.session.ISessionAffinityManager)
     */
    @Override
    public void registerAffinityManager(ISessionAffinityManager manager) {
        _sam = manager;
    }

    /*
     * Mutator for _statsModuleObjectName
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#registerStatsModuleObjectName
     * (javax.management.ObjectName)
     */
    @Override
    public void registerStatsModuleObjectName(ObjectName moduleObjectName) {
        _statsModuleObjectName = moduleObjectName;
    }

    /*
     * Mutator for _adapter
     * 
     * @see
     * com.ibm.wsspi.session.ISessionManagerCustomizer#setAdapter(com.ibm.wsspi
     * .session.IProtocolAdapter)
     */
    @Override
    public void setAdapter(IProtocolAdapter adapter) {
        _adapter = adapter;
    }

    @Override
    public IProtocolAdapter getAdapter() {
        return _adapter;
    }

    // XD methods
    // only used by HttpSessionManagerImpl in XD
    @Override
    public IProtocolAdapter getProtocolAdapter() {
        return null;
    }

    // only used by HttpSessionManagerImpl in XD
    @Override
    public boolean isRequestedSessionIDValid(ServletRequest request, String sessionID, int version) {
        return false;
    }

    // only used by HttpSessionManagerImpl in XD
    @Override
    public boolean needToRedirect(ServletRequest req, SessionAffinityContext affinityContext, Object session) {
        return false;
    }

    // only used by HttpSessionManagerImpl in XD
    @Override
    public void createProtocolAdapter(Properties props) {}

    // only used by HttpSessionManagerImpl in XD
    @Override
    public void registerListeners(ClassLoader classloader, ArrayList list) {}
    // end of XD methods
}
