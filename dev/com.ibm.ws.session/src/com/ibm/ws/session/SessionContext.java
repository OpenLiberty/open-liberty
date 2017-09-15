/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.ibm.websphere.servlet.session.IBMSessionListener;
import com.ibm.ws.session.http.HttpSessionAttributeObserver;
import com.ibm.ws.session.store.memory.MemoryStore;
import com.ibm.ws.session.store.memory.SessionSimpleHashMap;
import com.ibm.ws.session.utils.IDGeneratorImpl;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.session.IGenericSessionManager;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionAffinityManager;
import com.ibm.wsspi.session.ISessionManagerCustomizer;
import com.ibm.wsspi.session.ISessionObserver;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStorer;
import com.ibm.wsspi.session.ITimer;
import com.ibm.wsspi.session.SessionAffinityContext;


public class SessionContext {

    public SessionManagerConfig _smc;

    /**
     * @return the _smc
     */
    public SessionManagerConfig get_smc() {
        return _smc;
    }

    private final SessionStoreService sessionStoreService;
    public SessionApplicationParameters _sap;
    protected IGenericSessionManager _coreHttpSessionManager = null;
    public IGenericSessionManager _coreHttpAppSessionManager = null;
    protected ISessionAffinityManager _sam = null;
    protected boolean isSIPApplication = false;

    protected boolean sessionAttributeListener = false;
    protected boolean sessionListener = false;

    protected ArrayList mHttpSessionAttributeListeners = new ArrayList();
    protected ArrayList mHttpSessionListeners = new ArrayList();
    protected ArrayList mHttpSessionIdListeners = new ArrayList();

    protected ArrayList mHttpSessionAttributeListenersJ2eeNames = new ArrayList();
    protected ArrayList mHttpSessionListenersJ2eeNames = new ArrayList();
    protected ArrayList mHttpSessionIdListenersJ2eeNames = new ArrayList();

    /* The InetAddress of this host */
    static protected InetAddress mLocalHost;

    // To keep track of number of web modules have access to this
    // store's session context. This will help to determine when to destroy
    // session context when session sharing is used.
    private int _refCount = 1;
    protected static WSThreadLocal currentThreadSacHashtable = null;
    private static final String methodClassName = "SessionContext";
    static final String ANONYMOUS_USER = "anonymous";
    protected WasHttpSessionObserver wasHttpSessionObserver;
    HttpSessionAttributeObserver wasHttpSessionAttributeObserver;
    protected ITimer _invalidator = null;
    protected IStorer _storer = null;

    // PM03375: Use separate observer and listener array for app sessions for app sessions
    protected WasHttpAppSessionObserver wasHttpAppSessionObserver = null;
    protected ArrayList mHttpAppSessionListeners = null;

    private static final String cmvcInfo = "CMVC Version 1.9 6/24/07 11:12:24";
    private static boolean _loggedVersion = false;

    protected static final int CREATE_CORE_SESSION_MANAGER = 0;
    protected static final int GET_REQUESTED_SESSION_ID = 1;
    protected static final int GET_IHTTP_SESSION = 2;
    protected static final int SESSION_PRE_INVOKE = 3;
    protected static final int SESSION_POST_INVOKE = 4;
    protected static final int STOP = 5;
    protected static final int STOP_LISTENERS = 6;
    protected static final int IS_VALID = 7;
    protected static final int ENCODE_URL = 8;
    protected static final int SHOULD_ENCODE_URL = 9;
    protected static final int IS_PROTOCOL_SWITCH = 10;
    protected static final int GET_LOCAL_HOST = 11;
    protected static final int ADD_HTTP_SESSION_ATTRIBUTE_LISTENER = 12;
    protected static final int ADD_HTTP_SESSION_LISTENER = 13;
    protected static final int GET_SESSION_AFFINITY_CONTEXT = 14;
    protected static final int CHECK_SECURITY = 15;
    protected static final int INVALIDATE = 16;
    protected static final int CHECK_SESSSIONID_IS_RIGHT_LENGTH = 17;
    protected static final int LOCK_SESSION = 18;
    protected static final int UNLOCK_SESSION = 19;
    protected static final int ADD_HTTP_SESSION_ID_LISTENER = 20;

    protected static final String methodNames[] = { "createCoreSessionManager", "getRequestedSessionId", "getIHttpSession", "sessionPreInvoke", "sessionPostInvoke", "stop",
                                                   "stopListeners", "isValid", "encodeURL", "shouldEncodeURL", "isProtocolSwitch", "getLocalHost",
                                                   "addHttpSessionAttributeListener",
                                                   "addHttpSessionListener", "getSessionAffinityContext", "checkSecurity", "invalidate", "checkSessionIdIsRightLength",
                                                   "lockSession", "unlockSession", "addHttpSessionIdListener" };

    /*
     * constructor
     */
    public SessionContext(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        this(smc,sap,sessionStoreService,false);
    }
    
    /*
     * constructor
     */
    public SessionContext(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService, boolean removeAttrOnInvalidate) {
        if (!_loggedVersion) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", cmvcInfo + ", removeAttrOnInvalidate -->"+ removeAttrOnInvalidate);
            _loggedVersion = true;
        }
        _smc = smc;
        _sap = sap;
        this.sessionStoreService = sessionStoreService;
        if(!removeAttrOnInvalidate){
            _coreHttpSessionManager = createCoreSessionManager();
        }
        else{
            _coreHttpSessionManager = createCoreSessionManager(removeAttrOnInvalidate);
        }
        if (sap.getHasApplicationSession()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "Creating an appSessionContext for " + _sap.getAppName());
            }
            _coreHttpAppSessionManager = createCoreAppSessionManager();
            if (_coreHttpAppSessionManager == null) {
                // There was a problem creating the coreAppSessionManager. Just set the
                // HasApplicationSession value to false.
                sap.setHasApplicationSession(false);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "Setting hasApplicationSession to false for " + sap.getAppName());
                }
            }
        }
    }

    public IGenericSessionManager createCoreAppSessionManager() {
        // to be overwritten in WsSessionContext
        return null;
    }

    /*
     * create the core session manager
     */
    public IGenericSessionManager createCoreSessionManager() {
        return this.createCoreSessionManager(false);
    }
    /*
     * create the core session manager
     */
    public IGenericSessionManager createCoreSessionManager(boolean removeAttrOnInvalidate) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CREATE_CORE_SESSION_MANAGER], "appName = " + _sap.getAppName());
        }

        String _sessionManagerID = _sap.getAppName();
        /*
         * create the session manager
         */
        IGenericSessionManager _sessionManager = new SessionManager(_sessionManagerID);
        /*
         * Get a handle to the SessionManagerCustomizer and
         * customize the session manager.
         */
        ISessionManagerCustomizer sessionMgrCustomizer = _sessionManager.getSessionManagerCustomizer();

        // servlet context
        sessionMgrCustomizer.setServletContext(_sap.getServletContext());

        // id generator
        sessionMgrCustomizer.setIDGenerator(new IDGeneratorImpl(SessionManagerConfig.getSessionIDLength()));

        // cookie config
        SessionCookieConfigImpl sessionCookieConfig = _sap.getSessionCookieConfig();
        if (sessionCookieConfig != null) {
            _smc.updateCookieInfo(sessionCookieConfig);
        }

        // tracking mode
        EnumSet<SessionTrackingMode> trackingMode = _sap.getSessionTrackingModes();
        if (trackingMode != null) {
            _smc.updateTrackingMode(trackingMode);
        }

        // session timeout
        long sessionTimeout = _sap.getSessionTimeout();
        if (sessionTimeout == 0) { // no value from web.xml
            sessionTimeout = _smc.getSessionInvalidationTime();
        }
        sessionTimeout = checkMinimumInvalidation(sessionTimeout);
        sessionMgrCustomizer.setSessionTimeout((int) sessionTimeout);

        // protocol adapter
        IProtocolAdapter adapter = new WasHttpSessionAdapter(this, _sap.getServletContext());
        sessionMgrCustomizer.setAdapter(adapter);

        // session listeners
        // only call new constructor if we actually have SessionidListeners
        if( mHttpSessionIdListeners == null ) {
            wasHttpSessionObserver = new WasHttpSessionObserver(mHttpSessionListeners, adapter);
        } else {
            wasHttpSessionObserver = new WasHttpSessionObserver(mHttpSessionListeners, mHttpSessionIdListeners, adapter);
        }
       
        sessionMgrCustomizer.registerSessionObserver(wasHttpSessionObserver);

        // session attribute listeners
        sessionMgrCustomizer.registerSessionStateObserver(new HttpSessionAttributeObserver(mHttpSessionAttributeListeners, adapter));

        // store
        IStore _store = null;
        if(!removeAttrOnInvalidate){
               _store = createStore(_smc, _sessionManagerID, _sap.getServletContext(), false);
        }
        else{
            _store = createStore(_smc, _sessionManagerID, _sap.getServletContext(), false , removeAttrOnInvalidate);
        }
        _store.setDistributable(_sap.getDistributableWebApp());
        sessionMgrCustomizer.registerStore(_store);

        // invalidator
        _invalidator = createInvalidator();
        int reaperInterval = getReaperInterval(sessionTimeout);
        _invalidator.start(_store, reaperInterval);

        // storer - handles manual write, eos, and time based differences
        _storer = createStorer(_smc, _store);
        sessionMgrCustomizer.registerStorer(_storer);

        // affinity manager
        ISessionAffinityManager isam = createSessionAffinityManager(_smc, this, _store);
        sessionMgrCustomizer.registerAffinityManager(isam);
        _sam = isam;

        // zOS servant affinity
        ISessionObserver servantAffinityManager = getServantAffinityManager(_smc, adapter, _sap.getAppName());
        if (servantAffinityManager != null) {
            sessionMgrCustomizer.registerSessionObserver(servantAffinityManager);
        }

        SessionManagerRegistry.getSessionManagerRegistry().registerSessionManager(_sessionManagerID, _sessionManager);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CREATE_CORE_SESSION_MANAGER], "_sessionManager =" + _sessionManager);
        }
        return _sessionManager;
    }

    /*
     * create the invalidator
     */
    /*provides option to let ScheduledExecutorService control thread pool instead with a session property*/
    public ITimer createInvalidator() {
        if (_smc.getUseSeparateSessionInvalidatorThreadPool()){
            //START PM74718
            SessionInvalidatorWithThreadPool invalidator = new SessionInvalidatorWithThreadPool();
            invalidator.setDelay(_smc.getDelayForInvalidationAlarmDuringServerStartup()); 
            //END PM74718
            return invalidator;
        }
        else{
            SessionInvalidator invalidator = new SessionInvalidator();
            invalidator.setDelay(_smc.getDelayForInvalidationAlarmDuringServerStartup()); 
            return invalidator;
        }
    }
    /** Called to construct instances of <code>IStore</code>
     * 
     * @param sc the Servlet context
     * @return a new instance of <code>MemoryStoreHelper</code>, or null if no instance should be used
     * @see #createStore(SessionManagerConfig, String, ServletContext, boolean)
     */
    protected MemoryStoreHelper createStoreHelper(ServletContext sc) {
        return null;
    }

    /*
     * create the store
     */
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc) {
        return this.createStore(smc, smid, sc, false, false);
    }

    /*
     * create the store
     */
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, boolean applicationSessionStore) {
        return this.createStore(smc, smid, sc, applicationSessionStore, false);
    }

    /*
     * create the store
     */
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, boolean applicationSessionStore , boolean removeAttrOnInvalidate) {
        IStore store = null;
        MemoryStoreHelper storeHelper = this.createStoreHelper(sc);
        SessionStoreService service = this.sessionStoreService;
        if (service != null) {
            store = service.createStore(smc, smid, sc, storeHelper, _sap.getAppClassLoader(), applicationSessionStore);
        }
        if (store == null) {
            if(!removeAttrOnInvalidate)
                store = new MemoryStore(smc, smid, sc, storeHelper, applicationSessionStore);
            else{
                store = new MemoryStore(smc, smid, sc, storeHelper, applicationSessionStore,removeAttrOnInvalidate);
            }
        }
        return store;
    }

    /*
     * create the storer
     */
    public IStorer createStorer(SessionManagerConfig smc, IStore store) {
        IStorer storer = null;
        if (smc.getEnableTimeBasedWrite()) {
            storer = new TBWSessionStorer(store, 10); // 10 seconds is fixed - not configurable
        } else if (smc.getEnableManualWrite()) {
            storer = new ManualSessionStorer();
        } else {
            storer = new EOSSessionStorer();
        }
        return storer;
    }

    /*
     * getServantAffinityManager - returns null, overridden in WsSessionContext
     */
    public ISessionObserver getServantAffinityManager(SessionManagerConfig smc, IProtocolAdapter adapter, String appName) {
        return null;
    }

    /*
     * createAffinityManager - create the affinity manager
     */
    public ISessionAffinityManager createSessionAffinityManager(SessionManagerConfig smc, SessionContext sctx, IStore istore) {
        return new SessionAffinityManager(smc, sctx, istore);
    }

    /*
     * createSessionObject
     */
    public Object createSessionObject(ISession isess, ServletContext servCtx) {
        return new SessionData(isess, this, servCtx);
    }

    /*
     * Method used for Time Based Writes ... overriden in WebSphere code
     */
    public long checkMinimumInvalidation(long invalTime) {
        return invalTime;
    }

    /*
     * Calculates the interval at which the invalidator (aka reaper) will run.
     * If the HttpSessionReaperPollInterval property is set to 30 or more, use it.
     * Otherwise, algorithm is based on the session timeout with some randomness
     * to spread out the workload
     */
    public int getReaperInterval(long timeout) {

        int rpi = (int) _smc.getReaperPollInterval();
        if (rpi < 30) { // prop not set or too small
            int randomLessThanThirty = (int) (System.currentTimeMillis() % 30);
            if (timeout <= 120) { // up to 2 minutes
                rpi = 60 - randomLessThanThirty; // reaper runs every 30 to 60 seconds
            } else if (timeout <= 900) { // up to 15 minutes
                rpi = 60 + randomLessThanThirty; // reaper runs every 60 to 90 seconds
            } else if (timeout <= 1800) { // up to 30 minutes
                rpi = 120 + (2 * randomLessThanThirty); // reaper runs every 2 to 3
                // minutes
            } else { // more than 30 minutes
                rpi = 240 + (2 * randomLessThanThirty); // reaper runs every 4 to 5
                // minutes
            }
            _smc.setReaperPollInterval(rpi);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getReaperInterval", "returning " + rpi);
        }
        return rpi;
    }

    protected void setSIPCookieIfApplicable(HttpServletRequest _request, HttpServletResponse response, SessionData sd) {
        // do nothing ... implementation in WsSessionContext
    }

    /*
     * Called by webcontainer to do session cleanup, once per webapp per request.
     * If multiple apps are involved due to forwards/includes, multiple
     * sessionPostInvoke
     * calls are all made at then end of the request. We do NOT call
     * sessionPostInvoke
     * immediately when a dispatch ends. Therefore, some of this processing - such
     * as
     * calling unlockSession or setting the crossover threadlocal to null - may be
     * done
     * multiple times. This is ok.
     */
    public void sessionPostInvoke(HttpSession sess) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SESSION_POST_INVOKE]);
        }
        SessionData s = (SessionData) sess;

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
     * getCurrentSessionId
     * Used to get the session id to use for crossover checking. Since we may
     * have multiple session ids for a single request due to different cookie
     * names
     * across apps, we need to store the sacHashtable in the threadlocal.
     */
    public String getCurrentSessionId() {
        String currentId = null;
        Hashtable ht = (Hashtable) currentThreadSacHashtable.get();
        if (ht != null) {
            SessionAffinityContext sac = (SessionAffinityContext) ht.get(_smc.getSessionAffinityContextKey());
            if (sac != null) {
                currentId = _sam.getInUseSessionID(null, sac);
                // Make sure this is set if nothing was passed in.
                if (currentId == null) {
                    currentId = "NONE";
                }
            }
        }
        // If null is returned, this is considered to be a user spawned thread.
        return currentId;
    }

    /*
     * For serialize session access feature...overridden in WsSessionContext
     */
    public void lockSession(HttpServletRequest req, HttpSession sess) {}

    /*
     * For serialize session access feature...overridden in WsSessionContext
     */
    public void unlockSession(HttpSession sess) {}

    /*
     * shuts down this session manager
     */
    public synchronized void stop(String J2EEName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[STOP]);
        }

        decrementRefCount();
        if (this._refCount > 0) {
            if (_sap.getAppName() == "GLOBAL_HTTP_SESSION_CONTEXT") { // only call if global context
                stopListeners(J2EEName); // for global sessions, might have to stop some
                // listeners...
            }
            // shared sessions -- another app is still using this SessionContext
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[STOP], "Reference count is not zero, so returning without stopping");
            }
            return;
        }

        String coreManagerId = _coreHttpSessionManager.getID();
        // invalidateAllMemorySessions for in memory, and writes out sessions not
        // written out and passivates for persistent cases
        _coreHttpSessionManager.getIStore().stop();
        if (_coreHttpAppSessionManager != null) {
            // done above - boolean cleanUpAppSessionManager =
            // ((ApplicationSessionManager)_coreHttpAppSessionManager).removeSessionManager(_coreHttpSessionManager.getID());
            // REG - might want to move it back up
            // we need to remove the session manager so we don't try to access the
            // session while stopping
            cleanUpAppSessionManager(coreManagerId, J2EEName);
        }

        _invalidator.stop(); // stop invalidator thread
        if (_storer instanceof ITimer) { // stop tbw thread
            ((ITimer) _storer).stop();
        }

        SessionContextRegistry.remove(_sap.getAppName());
        // need to update the SessionManagerRegistry to free up memory
        SessionManagerRegistry.getSessionManagerRegistry().unregisterSessionManager(_sap.getAppName());

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[STOP]);
        }
    }

    protected void cleanUpAppSessionManager(String coreManagerId, String J2EEName) {
        // no op
        // overwritten in WsSessionContext
    }

    /*
     * Reload - same as stop
     */
    public void reload(String J2EEName) {
        stop(J2EEName);
    }

    /*
     * Remove listeners from the list.
     * Only called for global sessions. When an enterprise app
     * is stopped, that apps listeners need to be removed because
     * the global context may stick around.
     */
    public void stopListeners(String stoppingJ2eename) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[STOP_LISTENERS], "stopListeners for " + stoppingJ2eename);
        }

        if (stoppingJ2eename != null) { // should never be null, but make sure
            // do mHttpSessionListeners first
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS],
                                                     "mHttpSessListenersJ2eeNames.size is " + mHttpSessionListenersJ2eeNames.size());
            }
            synchronized (mHttpSessionListeners) {
                for (int x = mHttpSessionListenersJ2eeNames.size() - 1; x >= 0; x--) {
                    String listenerJ2eeName = (String) mHttpSessionListenersJ2eeNames.get(x);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS], "listenerJ2eeName is " + listenerJ2eeName);
                    }
                    if (stoppingJ2eename.equals(listenerJ2eeName)) {
                        mHttpSessionListeners.remove(x);
                        mHttpSessionListenersJ2eeNames.remove(x);
                    }
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS], "stopping http session listener for: " + stoppingJ2eename);
                    }
                }
                if (mHttpSessionListeners.size() == 0) {
                    sessionListener = false;
                    _coreHttpSessionManager.getIStore().setHttpSessionListener(false);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS], "stopped all http session listeners");
                    }
                }
                if (wasHttpSessionObserver.getDoesContainIBMSessionListener()) {
                    // if we used to contain an IBMSessionListener, checking remaining
                    // listeners to see if we need to update the boolean
                    boolean mIBMSessionListenerImplemented = isIBMSessionListenerImplemented(mHttpSessionListeners);
                    if (!mIBMSessionListenerImplemented) {
                        wasHttpSessionObserver.setDoesContainIBMSessionListener(false);
                    }
                }
            }
            // now do mHttpSessionAttributeListeners

            for (int x = mHttpSessionAttributeListenersJ2eeNames.size() - 1; x >= 0; x--) {
                String listenerJ2eeName = (String) mHttpSessionAttributeListenersJ2eeNames.get(x);
                if (stoppingJ2eename.equals(listenerJ2eeName)) {
                    mHttpSessionAttributeListeners.remove(x);
                    mHttpSessionAttributeListenersJ2eeNames.remove(x);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS], "stopping http session attribute listener for: "
                                                                                                                       + stoppingJ2eename);
                    }
                }
            }
            if (mHttpSessionAttributeListeners.size() == 0) {
                sessionAttributeListener = false;
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[STOP_LISTENERS], "stopped all http session attribute listeners");
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[STOP_LISTENERS]);
        }
    }

    /*
     * Returns true if the specified URL should be encoded to include
     * the session id, false if not.
     */
    public boolean shouldEncodeURL(String pURL, HttpServletRequest pRequest) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SHOULD_ENCODE_URL]);
        }

        // per spec - For example, if the browser supports cookies, or session
        // tracking is turned off, URL encoding is unnecessary.
        // 6.1 and previous releases always encoded the url if it was set ... if a
        // customer wants this functionality, they should set the alwaysEncodeURL
        // property
        if (!SessionManagerConfig.isAlwaysEncodeURL() && _smc.getEnableCookies() && pRequest.getHeader("Cookie") != null) {
            if (SessionManagerConfig.checkSessionCookieNameOnEncodeURL()) {
                Cookie[] cookies = pRequest.getCookies();
                for (int i = 0; i < cookies.length; i++ ) {
                    if (cookies[i].getName().equals(_smc.getSessionCookieName())) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "false - A http session cookie was found");
                        }                                            
                        return false;
                    }
                } 
            } else {            
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "false - A cookie was found");
                }
                return false;
            }
        }

        if (!_smc.getEnableUrlRewriting()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "false - encodeOff");
            }
            return false;
        }

        if (!isProtocolSwitch(pURL, pRequest)) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "true - encodeProtocolSwitchOff");
            }
            return true;
        } else {
            if (_smc.getEnableUrlProtocolSwitchRewriting()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "true - encodeProtocolSwitchOn");
                }
                return true;
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SHOULD_ENCODE_URL], "false");
                }
                return false;
            }
        }
    }

    /*
     * Returns true if the specified URL is a switch between HTTP and
     * HTTPS (or vice versa), going back to this IP address.
     */
    private boolean isProtocolSwitch(String pURL, HttpServletRequest pRequest) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[IS_PROTOCOL_SWITCH]);
        }
        // Extract the protocol
        int ix = pURL.indexOf("://");
        if (ix < 0)
            return false;
        String protocol = pURL.substring(0, ix);

        // See if it's a switch between HTTP and HTTPS
        if (!((protocol.equalsIgnoreCase("http") && pRequest.getScheme().equalsIgnoreCase("https")) || (protocol.equalsIgnoreCase("https") && pRequest.getScheme().equalsIgnoreCase("http")))) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[IS_PROTOCOL_SWITCH], "false - No protocol switch.");
            }
            return false;
        }

        // Extract the host by searching for the first ':' or '/'
        int start = ix + 3;
        int end = start;
        int len = pURL.length();
        for (; end < len; end++) {
            char ch = pURL.charAt(end);
            if (ch == ':' || ch == '/')
                break;
        }
        String host = pURL.substring(start, end);

        // Get the IP address of the host
        InetAddress hostaddr;
        try {
            hostaddr = InetAddress.getByName(host);
        } catch (UnknownHostException exc) {
            com.ibm.ws.ffdc.FFDCFilter.processException(exc, "com.ibm.ws.session.SessionContext.isProtocolSwitch", "1760", this);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[IS_PROTOCOL_SWITCH], "CommonMessage.exception", exc);
            return false;
        }

        // See if it is not the same as this host
        if (!hostaddr.equals(getLocalHost())) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[IS_PROTOCOL_SWITCH], "false - Host Addresses are not equal");
            }
            return false;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[IS_PROTOCOL_SWITCH], "true");
        }
        return true;
    }

    /*
     * Returns the InetAddress of this host
     */
    protected InetAddress getLocalHost() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_LOCAL_HOST]);
        }
        if (mLocalHost == null) {
            synchronized (this) {
                try {
                    mLocalHost = (InetAddress) AccessController.doPrivileged(new PrivilegedExceptionAction()
                    {
                        @Override
                        public Object run() throws UnknownHostException
                    {
                        return InetAddress.getLocalHost();
                    }
                    });
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_LOCAL_HOST], "InetAddress = " + mLocalHost);
                    }
                } catch (PrivilegedActionException pae) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(pae, "com.ibm.ws.session.SessionContext.getLocalHost", "423", this);
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, methodClassName, methodNames[GET_LOCAL_HOST], "", pae);
                }
            }
        }
        return mLocalHost;
    }

    /*
     * returns boolean indicating whether Session Security Integration feature is
     * enabled
     */
    public boolean getIntegrateWASSecurity() {
        return _smc.getIntegrateSecurity();
    }

    /*
     * Adds a list of Session Attribute Listeners
     * For shared session context or global sesions, we call
     * this method to add each app's listeners.
     */
    public void addHttpSessionAttributeListener(ArrayList al, String j2eeName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ADD_HTTP_SESSION_ATTRIBUTE_LISTENER], "addHttpSessionAttributeListener:" + al);
        }
        if (j2eeName != null) {
            addToJ2eeNameList(j2eeName, al.size(), mHttpSessionAttributeListenersJ2eeNames);
        }
        mHttpSessionAttributeListeners.addAll(al);
        if (mHttpSessionAttributeListeners.size() > 0) {
            sessionAttributeListener = true;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ADD_HTTP_SESSION_ATTRIBUTE_LISTENER], "addHttpSessionAttributeListener:" + al);
        }
    }

    /*
     * Adds a list of Session Listeners
     * For shared session context or global sesions, we call
     * this method to add each app's listeners.
     */
    public void addHttpSessionListener(ArrayList al, String j2eeName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ADD_HTTP_SESSION_LISTENER], "addHttpSessionListener:" + al);
        }
        if (j2eeName != null) {
            addToJ2eeNameList(j2eeName, al.size(), mHttpSessionListenersJ2eeNames);
        }
        synchronized (mHttpSessionListeners) {
            mHttpSessionListeners.addAll(al);
            if (mHttpSessionListeners.size() > 0) {
                sessionListener = true;
                _coreHttpSessionManager.getIStore().setHttpSessionListener(true);
                boolean mIBMSessionListenerImplemented = isIBMSessionListenerImplemented(al); // PQ81248
                if (mIBMSessionListenerImplemented) {
                    wasHttpSessionObserver.setDoesContainIBMSessionListener(true);
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ADD_HTTP_SESSION_LISTENER], "addHttpSessionListener:" + al);
        }
    }

    /*
     * Keeps track of J2EE names of listeners. We only do this for global
     * sessions.
     * In this case, one Enterprise (J2EE) app may be stopped, so we need to stop
     * listeners
     * for that app.
     */
    private void addToJ2eeNameList(String j2eeName, int size, ArrayList listenerJ2eeNames) {
        int start = listenerJ2eeNames.size();
        int end = start + size;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("starting at ").append(start).append(" going to ").append(end).append(" for ").append(j2eeName);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "addToJ2eeNameList", sb.toString());
        }
        for (int x = start; x < end; x++) {
            listenerJ2eeNames.add(j2eeName);
        }
    }

    /*
     * IBM Extension to HttpSessionListener to tell app when a session is removed
     * from cache
     */
    public boolean isIBMSessionListenerImplemented(ArrayList listeners) {
        for (int i = 0; i < listeners.size(); i++) {
            HttpSessionListener listener = (HttpSessionListener) listeners.get(i);
            if ((listener != null) && (listener instanceof IBMSessionListener)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Increment the refcount for this SessionContext. Only greater than 1 for
     * shared session context or global sessions.
     */
    public synchronized void incrementRefCount() {
        _refCount++;
    }

    /*
     * Increment the refcount for this SessionContext. Only greater than 1 for
     * shared session context or global sessions.
     */
    public synchronized void decrementRefCount() {
        _refCount--;
    }

    /*
     * Determine if this SessionContext is still valid based on refcount
     */
    public boolean isValid() {
        return _refCount > 0;
    }

    /*
     * Return context details in HTML format
     */
    public String toHTML() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "toHTML");
        }
        StringBuffer strbuf = new StringBuffer();
        String appName = _sap.getJ2EEName();
        String globalScopeComment;
        MemoryStore ms = (MemoryStore) _coreHttpSessionManager.getIStore();
        int size = 0;
        if (ms._sessions != null) {
            size = ms._sessions.size();
        }
        globalScopeComment = "<b> (for this webapp) : </b> ";
        strbuf.append("<center><h3>J2EE NAME(AppName#WebModuleName):: " + appName + " </h3></center>" + "<UL>\n");
        strbuf.append("<b>  cloneId</b> : ");
        strbuf.append(((SessionAffinityManager) _sam).getCloneId());
        strbuf.append("<BR>");
        strbuf.append("<BR>");
        strbuf.append("<b>  Number of sessions in memory: </b>");
        strbuf.append(globalScopeComment);
        strbuf.append(size);
        strbuf.append("<BR>");
        if (_smc.isUsingMemory()) {
            strbuf.append("<b>  use overflow</b> : ");
            strbuf.append(_smc.getEnableOverflow());
            strbuf.append("<BR>");
            if (_smc.getEnableOverflow()) {
                strbuf.append("<b>  overflow size</b> ");
                strbuf.append(globalScopeComment);
                strbuf.append(((SessionSimpleHashMap) ms._sessions).getOverflowSize());
                strbuf.append("<BR>");
            }
        }
        strbuf.append("<b>  Invalidation alarm poll interval (for this webapp) </b> : ");
        strbuf.append(_smc.getInvalidationCheckInterval());
        strbuf.append("<BR>");
        strbuf.append("<b> Max invalidation timeout (for this webapp) </b> : ");
        strbuf.append(_smc.getSessionInvalidationTime());
        strbuf.append("<BR>");
        strbuf.append("<b> Using Cookies </b> : ");
        strbuf.append(_smc.getEnableCookies());
        strbuf.append("<BR>");
        strbuf.append("<b> Using URL Rewriting </b> : ");
        strbuf.append(_smc.getEnableUrlRewriting());
        strbuf.append("<BR>");
        strbuf.append("<b> use SSLId </b> : ");
        strbuf.append(_smc.useSSLId());
        strbuf.append("<BR>");
        strbuf.append("<b> URL Protocol Switch Rewriting </b> : ");
        strbuf.append(_smc.getEnableUrlProtocolSwitchRewriting());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie Name </b> : ");
        strbuf.append(_smc.getSessionCookieName());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie Comment </b> : ");
        strbuf.append(_smc.getSessionCookieComment());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie Domain </b> : ");
        strbuf.append(_smc.getSessionCookieDomain());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie Path </b> : ");
        strbuf.append(_smc.getSessionCookiePath());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie MaxAge </b> : ");
        strbuf.append(_smc.getSessionCookieMaxAge());
        strbuf.append("<BR>");
        strbuf.append("<b> Session Cookie Secure </b> : ");
        strbuf.append(_smc.getSessionCookieSecure());
        strbuf.append("<BR>");
        strbuf.append("<b> Maximum in memory table size </b> : ");
        strbuf.append(_smc.getInMemorySize());
        strbuf.append("<BR>");
        strbuf.append("<b> current time </b> : ");
        strbuf.append(new Date(System.currentTimeMillis()).toString());
        strbuf.append("<BR>");
        strbuf.append("<b>  integrateWASSec</b> :");
        strbuf.append(_smc.getIntegrateSecurity());
        strbuf.append("<BR><b>Session locking </b>: ");
        strbuf.append(_smc.getAllowSerializedSessionAccess());
        if (_smc.getAllowSerializedSessionAccess()) {
            strbuf.append("<BR><b>Session locking timeout</b>: ");
            strbuf.append(_smc.getSerializedSessionAccessMaxWaitTime());
            strbuf.append("<BR><b>Allow access on lock timeout</b>:");
            strbuf.append(_smc.getAccessSessionOnTimeout());
        }
        if (ms.getSessionStatistics() != null) {
            strbuf.append(ms.getSessionStatistics().toHTML());
        }
        strbuf.append(ms.toHTML());
        // .append(this.scPmiData.toHTML())
        // .append(this.mbeanAdapter.toHTML()); //done in WsSessionContext

        return (strbuf.toString());
    }

    /*
     * Returns the app name for this session context. This is normally the virtual
     * host + context root.
     * For shared session context, it is the virtual host + the J2EE app display
     * name.
     * For global sessions, it is a constant string "GLOBAL_HTTP_SESSION_CONTEXT".
     */
    public String getAppName() {
        return _sap.getAppName();
    }

    /*
     * crossoverCheck
     * Returns true if crossover is detected
     * should check of crossover checking is enabled before calling
     * doesn't check new sessions
     * Must be on a dispatch thread - currentThreadSessionId != null
     */
    public boolean crossoverCheck(HttpSession session) {
        /*- PK80539: The crossoverCheck code is moved to a new 2 arguments crossoverCheck method
         *  The one argument crossoverCheck will only be invoked when the request object is not available such as, in SessionData,
         *  which will then enter the new crossoverCheck method with a null request value.
         */
        return crossoverCheck(null, session);
    }

    public boolean crossoverCheck(HttpServletRequest req, HttpSession session) {
        // to be overwritten in HttpSessionContextImpl
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "crossoverCheck", "session crossover cannot be detected");
        }
        return false;
    }

    /*
     * This method is called from invalidateAll for local invalidations.
     */
    public void invalidate(String sessionId) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("for app ").append(_sap.getAppName()).append(" id ").append(sessionId);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[INVALIDATE], sb.toString());
        }
        ISession iSess = _coreHttpSessionManager.getISession(sessionId);

        if (iSess != null) {
            IStore iStore = _coreHttpSessionManager.getIStore();
            try {
                iStore.setThreadContext();
                synchronized (iSess) { // START PM47941
                    if (iSess.isValid()) {
                        iSess.invalidate();
                    }
                } // END PM47941
            } finally {
                iStore.unsetThreadContext();
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INVALIDATE]);
        }
    }

    /*
     * For remote InvalidateAll processing...calls remoteInvalidate method on the
     * store
     */
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        IStore iStore = _coreHttpSessionManager.getIStore();
        ((MemoryStore) iStore).remoteInvalidate(sessionId, backendUpdate);
    }

    // Akaimai requested function - overridden in WsSessionContext
    public void addHttpSessionListener(HttpSessionListener listener, String J2EEName) {
        synchronized (mHttpSessionListeners) {
            mHttpSessionListeners.add(listener);
            mHttpSessionListenersJ2eeNames.add(J2EEName);
            sessionListener = true;
            _coreHttpSessionManager.getIStore().setHttpSessionListener(true);
            // PM03375: Set app listeners
            if (mHttpAppSessionListeners != null)
                mHttpAppSessionListeners.add(listener);
            // PM03375: Set listener for app session manager 
            if (_coreHttpAppSessionManager != null)
                _coreHttpAppSessionManager.getIStore().setHttpSessionListener(true);

            if (listener instanceof IBMSessionListener) {
                wasHttpSessionObserver.setDoesContainIBMSessionListener(true);
                // PM03375: Mark app observer
                // Begin: PM03375: Use separate observer for app sessions
                if (wasHttpAppSessionObserver != null) {
                    wasHttpAppSessionObserver.setDoesContainIBMSessionListener(true);
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ADD_HTTP_SESSION_LISTENER], "Marked " +
                                                                                                                              " app IBM session listener for app observer "
                                                                                                                              + mHttpAppSessionListeners);
                }
                // End: PM03375
            }
        }

    }

    public void sessionCreatedEvent(HttpSessionEvent event) {
        ArrayList listeners = mHttpSessionListeners;
        for (int i = 0; i < listeners.size(); i++) {
            // cmd 174167 - get listener out of ArrayList
            HttpSessionListener listener = (HttpSessionListener) listeners.get(i);
            if (listener != null) {
                listener.sessionCreated(event);
            }
        }
    }

    public void sessionDestroyedEvent(HttpSessionEvent event) {
        //create local variable - JIT performance improvement
        ArrayList listeners = mHttpSessionListeners;
        for (int i = listeners.size() - 1; i >= 0; i--) {
            HttpSessionListener listener = (HttpSessionListener) listeners.get(i);
            if (listener != null) {
                listener.sessionDestroyed(event);
            }
        }
    }

    public void sessionAttributeAddedEvent(HttpSessionBindingEvent event) {
        ArrayList list = mHttpSessionAttributeListeners;
        for (int i = 0; i < list.size(); i++) {
            HttpSessionAttributeListener listener = (HttpSessionAttributeListener) list.get(i);
            if (listener != null) {
                listener.attributeAdded(event);
            }
        }
    }

    public void sessionAttributeReplacedEvent(HttpSessionBindingEvent event) {
        ArrayList list = mHttpSessionAttributeListeners;
        for (int i = 0; i < list.size(); i++) {
            HttpSessionAttributeListener listener = (HttpSessionAttributeListener) list.get(i);
            if (listener != null) {
                listener.attributeReplaced(event);
            }
        }
    }

    public void sessionAttributeRemovedEvent(HttpSessionBindingEvent event) {
        ArrayList list = mHttpSessionAttributeListeners;
        for (int i = 0; i < list.size(); i++) {
            HttpSessionAttributeListener listener = (HttpSessionAttributeListener) list.get(i);
            if (listener != null) {
                listener.attributeRemoved(event);
            }
        }
    }

    public boolean isSessionTimeoutSet() {
        boolean b = _sap.getSessionTimeout() != 0L;
        //if SessionInvalidationTime is explicitly set to 0, we will change it to -1.
        //a value of 0 really means that it was not set in the web.xml!
        return b;
    }

    public int getSessionTimeOut() {
        int i = (int) _smc.getSessionInvalidationTime();
        return i;
    };

    // Akaimai requested functions --End

    // PK34418
    public void addHttpSessionAttributeListener(HttpSessionAttributeListener listener, String J2EEName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ADD_HTTP_SESSION_ATTRIBUTE_LISTENER], "J2EE name is " + J2EEName);
        }
        mHttpSessionAttributeListeners.add(listener);
        mHttpSessionAttributeListenersJ2eeNames.add(J2EEName);
        sessionAttributeListener = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ADD_HTTP_SESSION_ATTRIBUTE_LISTENER]);
        }
    }

    public boolean isSIPApplication() {
        return isSIPApplication;
    }

    public void setSIPApplication(boolean isSIPApplication) {
        this.isSIPApplication = isSIPApplication;
    }
    
    /*
     * Adds a list of Session ID Listeners
     * For shared session context or global sesions, we call
     * this method to add each app's listeners.
     */
    public void addHttpSessionIdListener(ArrayList al, String j2eeName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ADD_HTTP_SESSION_ID_LISTENER], "addHttpSessionIdListener:" + al);
        }
        if (j2eeName != null) {
            addToJ2eeNameList(j2eeName, al.size(), mHttpSessionIdListenersJ2eeNames);
        }
        synchronized (mHttpSessionIdListeners) {
            mHttpSessionIdListeners.addAll(al);
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ADD_HTTP_SESSION_LISTENER], "addHttpSessionListener:" + al);
        }
    }
    
    public void addHttpSessionIdListener(HttpSessionIdListener listener, String J2EEName) {
        synchronized (mHttpSessionIdListeners) {
            mHttpSessionIdListeners.add(listener);
            mHttpSessionIdListenersJ2eeNames.add(J2EEName);
            //sessionListener = true;
            //_coreHttpSessionManager.getIStore().setHttpSessionListener(true);
            // PM03375: Set app listeners
            //if (mHttpSessionIdListeners != null)
            //    mHttpSessionIdListeners.add(listener);
            // PM03375: Set listener for app session manager 
            /*if (_coreHttpAppSessionManager != null)
                _coreHttpAppSessionManager.getIStore().setHttpSessionListener(true);

            if (listener instanceof IBMSessionListener) {
                wasHttpSessionObserver.setDoesContainIBMSessionListener(true);
                // PM03375: Mark app observer
                // Begin: PM03375: Use separate observer for app sessions
                if (wasHttpAppSessionObserver != null) {
                    wasHttpAppSessionObserver.setDoesContainIBMSessionListener(true);
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[ADD_HTTP_SESSION_LISTENER], "Marked " +
                                                                                                                              " app IBM session listener for app observer "
                                                                                                                              + mHttpAppSessionListeners);
                }
                // End: PM03375
            }*/
        }

    }
}
