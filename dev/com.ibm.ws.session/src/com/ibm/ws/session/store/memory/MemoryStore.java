/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.memory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionManagerRegistry;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IGenericSessionManager;
import com.ibm.wsspi.session.ILoader;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStoreCallback;
import com.ibm.wsspi.session.ITimer;

/*
 * The memory store implementation.
 * Persistent stores extend this class.
 */
public class MemoryStore implements IStore {

    public HashMap _sessions = null;

    // anonymous user string
    public static final String ANONYMOUS_USER = "anonymous";

    protected String _storeId = null;
    protected IStoreCallback _storeCallback;
    protected MemoryStoreHelper _storeHelper;
    protected ITimer _invalidator;
    protected SessionManagerConfig _smc;
    private MemorySession overflowSession;
    private static final String overflowId = "overflowed-session";
    protected ServletContext _servletContext = null;
    private boolean httpSessListener = false;
    private boolean _removeAttrOnInvalidate = false;
    protected boolean distributableWebApp;
    // boolean value set during stop to prevent creation or access of a session
    // during a stop
    protected boolean inProcessOfStopping = false;

    private static final String methodClassName = "MemoryStore";
    protected String appNameForLogging = "";

    protected SessionStatistics _sessionStatistics;

    protected boolean _isApplicationSessionStore = false;
    

    private static final int CREATE_SESSION = 0;
    private static final int GET_SESSION = 1;
    private static final int ID_EXISTS = 2;
    private static final int RUN_INVALIDATION = 3;
    private static final int INVALIDATE_ALL_MEMORY_SESSIONS = 4;
    private static final int REMOVE_INVALIDATE = 5;
    private static final int CHECK_SESSION_STILL_VALID = 6;

    private static final String methodNames[] = { "createSession", "getSession", "idExists", "runInvalidation", "invalidateAllMemorySessions", "remoteInvalidate",
                                                 "checkSessionStillValid" };

    
    
    /* Public constructor */

    public MemoryStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper) {
        _smc = smc;
        _storeId = storeId;
        _servletContext = sc;
        _storeHelper = storeHelper;
        if (_smc.isUsingMemory()) {
            int _initialTableSize = smc.getInMemorySize();
            boolean _allowOverflow = smc.getEnableOverflow();
            if (!_allowOverflow) {
                overflowSession = new MemorySession(this, overflowId, _storeCallback);
                overflowSession.setOverflow(); // sets to true
                overflowSession.setIsValid(false);
            }
            _sessions = new SessionSimpleHashMap(this, _initialTableSize, _allowOverflow);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            appNameForLogging = " AppName=" + _storeId;
        }
    }

    public MemoryStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, boolean isApplicationSessionStore) {
        this(smc, storeId, sc, storeHelper);
        _isApplicationSessionStore = isApplicationSessionStore;
    }
    
    public MemoryStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, boolean isApplicationSessionStore ,boolean removeAttrOnInvalidate) {
        _smc = smc;
        _storeId = storeId;
        _servletContext = sc;
        _storeHelper = storeHelper;
        _removeAttrOnInvalidate = removeAttrOnInvalidate;
        if (_smc.isUsingMemory()) {
            int _initialTableSize = smc.getInMemorySize();
            boolean _allowOverflow = smc.getEnableOverflow();
            if (!_allowOverflow) {
                if(!removeAttrOnInvalidate)
                    overflowSession = new MemorySession(this, overflowId, _storeCallback);
                else{
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "MemoryStore", "Create MemorySession with _removeAttrOnInvalidate -->" + _removeAttrOnInvalidate );
                    }
                    overflowSession = new MemorySession(this, overflowId, _storeCallback,removeAttrOnInvalidate);
                }
                
                overflowSession.setOverflow(); // sets to true
                overflowSession.setIsValid(false);
            }
            _sessions = new SessionSimpleHashMap(this, _initialTableSize, _allowOverflow);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            appNameForLogging = " AppName=" + _storeId;
        }
        _isApplicationSessionStore = isApplicationSessionStore;
        
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setID(java.lang.String)
     */
    @Override
    public void setID(String storeId) {
        // never called ... removed trace
        _storeId = storeId;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#createSession(java.lang.String)
     */
    @Override
    public ISession createSession(String id, boolean newId) {
        // assert (id!=null);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("{").append(id).append(",").append(newId).append("} ").append(appNameForLogging);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CREATE_SESSION], sb.toString());
        }
        MemorySession sess = null;

        // need to test on a different language
        // try creating a temporary logger with the old resource bundle, and then
        // changing the environment to French or something
        if (inProcessOfStopping) {
            throwException("SessionContext.createWhenStop");
        }
        if ((!_smc.getEnableOverflow()) && (_sessions.size() >= _smc.getInMemorySize())) {
            // if we know we're in an overflow situation, don't bother
            // creating a new session
            if (_sessionStatistics != null) {
                _sessionStatistics.incNoRoomForNewSession();
            }
            sess = overflowSession;
        }
        // We expect the second part of check below - _sessions.get(id) == null --
        // to pass unless
        // the id generator generates a dup. This really should never happen.
        // The synchronization in SessionManager.createISession will prevent
        // multiple threads
        // with the same incoming id from both getting into this code. We're no
        // longer using
        // the newId parm...it could be removed, but its now part of the IStore
        // interface.
        if ((sess == null) && (_sessions.get(id) == null)) {
            if(!_removeAttrOnInvalidate)
                sess = new MemorySession(this, id, _storeCallback);
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[CREATE_SESSION], "Create MemorySession with _removeAttrOnInvalidate -->" + _removeAttrOnInvalidate );
                }
                sess = new MemorySession(this, id, _storeCallback, _removeAttrOnInvalidate);
            }
            sess.setUserName(ANONYMOUS_USER);
            // since this was a newly created session, we need to update the
            // lastAccessTime to the create time
            sess.updateLastAccessTime(sess.getCreationTime());
            try {
                _sessions.put(id, sess);
            } catch (TooManySessionsException tmse) {
                // could catch this exception if multiple threads get past the
                // first overflow check and then attempt to do the sessions.put
                sess = overflowSession;
            }
        }

        // Returns a new session if everything is good
        // Returns an overflowSession is overflow is not allowed and we've already
        // hit the maximum number in memory
        // Returns null if there was an existing session with this key - should
        // never happen

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CREATE_SESSION], sess);
        }
        return sess;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#getSession(java.lang.String, int)
     */
    public ISession getSession(String id, int version, boolean isSessionAccess) {
        // create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("{").append(id).append(",").append(version).append("} ").append(appNameForLogging);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_SESSION], sb.toString());
        }
        if (inProcessOfStopping) {
            throwException("SessionContext.accessWhenStop");
        }
        ISession isess = (ISession) _sessions.get(id);
        if (isess != null && isSessionAccess) {
            ((MemorySession) isess).updateLastAccessTime(System.currentTimeMillis());
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_SESSION], isess);
        }
        return isess;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#idExists(java.lang.String)
     */
    @Override
    public boolean idExists(String id) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ID_EXISTS], id);
        }

        Object sessInUse;
        boolean found = false;

        SessionManagerRegistry _sessionManagerRegistry = SessionManagerRegistry.getSessionManagerRegistry();
        // Since the session manager can be configured per application, we need to
        // look at
        // each entry individually since some of the contexts can have database/mtm
        // persistence
        // enabled while others are using memory

        // Look in memory first for improved performance
        Enumeration vEnum = _sessionManagerRegistry.getSessionManagers();
        IStore tempStore;
        while (vEnum.hasMoreElements() && !found) {
            IGenericSessionManager sm = (IGenericSessionManager) vEnum.nextElement();
            if (sm == null) // || sm == this)
                continue;
            tempStore = sm.getIStore();
            if (tempStore == this)
                continue; // don't bother checking our own store
            sessInUse = tempStore.getFromMemory(id);
            if (sessInUse != null)
                found = true;
        }

        if (found) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                String s = "Found in Cache/Mem";
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ID_EXISTS], s);
            }
            return found;
        }

        // Check our current external store first.
        // This is probably the most likely
        // Also, if the sessionId has been recently invalidated and
        // checkRecentlyInvalidList is true, we should return true without checking
        // other backends.
        found = this.isPresentInExternalStore(id);
        if (found) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ID_EXISTS], Boolean.valueOf(found));
            }
            return found;
        }

        // Search the persistent stores for the sessionid
        Enumeration enum3 = _sessionManagerRegistry.getSessionManagers();
        while (enum3.hasMoreElements()) {
            IGenericSessionManager sm = (IGenericSessionManager) enum3.nextElement();
            if (sm == null) // || sm == this)
                continue;
            else {
                tempStore = sm.getIStore();
                if (tempStore == this)
                    continue; // don't bother checking our own store as we already checked
                              // it above
                found = tempStore.isPresentInExternalStore(id);
                if (found) {
                    break;
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String s = "After persistent store scan returning: " + found;
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ID_EXISTS], s);
        }

        return found;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setLoader(com.ibm.wsspi.session.ILoader)
     */
    @Override
    public void setLoader(ILoader loader) {
        // in-Memory impl - do nothing
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setStoreCallback(com.ibm.wsspi.session.
     * IStoreCallback)
     */
    @Override
    public void setStoreCallback(IStoreCallback callback) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = callback + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setStoreCallback", s);
        }
        _storeCallback = callback;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#getStoreCallback()
     */
    @Override
    public IStoreCallback getStoreCallback() {
        return _storeCallback;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setShareAcrossWebapp(boolean)
     */
    @Override
    public void setShareAcrossWebapp(boolean flag) {
        // not used by WAS
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setNumOfPartitions(int)
     */
    @Override
    public void setNumOfPartitions(int num) {
        // not used by WAS
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setMaxInactiveInterval(int)
     */
    @Override
    public void setMaxInactiveInterval(int interval) {
        // never called ... removed trace
        _smc.setSessionInvalidationTime(interval);
    }

    /*
     * @see com.ibm.wsspi.session.IStore#getId()
     */
    @Override
    public String getId() {
        return _storeId;
    }

    /*
     * removeSessions
     * removes session from HashMap for in-memory
     * overridden for persistent stores
     * 
     * @see com.ibm.wsspi.session.IStore#removeSession(java.lang.String)
     */
    @Override
    public void removeSession(String id) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = id + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "removeSession", s);
        }
        Object removedEntry = _sessions.remove(id);
        if (removedEntry != null) {
            _storeCallback.sessionLiveCountDec(removedEntry);
        }
    }

    /*
     * @see com.ibm.wsspi.session.IStore#runInvalidation()
     * method called by the Invalidator to perform
     * background Invalidation Processing
     */
    @Override
    public void runInvalidation() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[RUN_INVALIDATION], appNameForLogging);
        }
        /*
         * if (_isApplicationSessionStore) {
         * runApplicationStoreInvalidation();
         * } else {
         */
        Set keySet = _sessions.keySet();
        if (keySet.size() == 0) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[RUN_INVALIDATION], "no sessions in memory store - " + appNameForLogging);
            }
            return;
        }
        long nowTime = System.currentTimeMillis();
        Iterator iter = keySet.iterator();
        try {
            //setThreadContext threw a NPE because we were trying to get the config from within getModuleMetaData and it was returning null
            //this only happens after the app has been shutdown.  There was a small timing window where this was possible.
            //Anyway, we're going to check for isInProcessOfStopping, but there could still be a timing window.  So, we are going to call a different
            //setThreadContext method that will not log the errors and we will log it here only if we are not in the process of stopping  
            if (this.isInProcessOfStopping()) { 
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[RUN_INVALIDATION], "application in the process of stopping - " + appNameForLogging);
                }
                return;
            }
            // PK99859: Set the thread context
            try {
                setThreadContextDuringRunInvalidation();
            } catch (RuntimeException e) {
                if (!this.isInProcessOfStopping()) {
                    //the exception is only expected if we are in the process of stopping the application
                    //therefore, if we have not stopped the app, report the error
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, MemoryStore.class.getSimpleName(), methodNames[RUN_INVALIDATION], "CommonMessage.exception", e);
                }
                return;
            }
            while (iter.hasNext()) {
                String key = (String) iter.next();
                ISession s = (ISession) _sessions.get(key);

                // sync on the session and check if its active...
                if (s != null) {
                    synchronized (s) {
                        if (s.isValid()) {
                            if (s.getMaxInactiveInterval() != -1) {
                                long currentAccessTime = s.getCurrentAccessTime(); // currentAccessTime
                                                                                   // updated on
                                                                                   // session
                                                                                   // access
                                                                                   // lastAccessedTime
                                                                                   // updated at
                                                                                   // releaseSession
                                long maxinact = 1000 * (long) s.getMaxInactiveInterval();
                                boolean active = s.getRefCount() > 0;
                                if (_isApplicationSessionStore) {
                                    // the RefCount is not correct when dealing with an
                                    // applicationSessionStore
                                    s.setRefCount(0);
                                    active = false;
                                }
                                boolean timedOut = (currentAccessTime <= nowTime - maxinact);
                                /*
                                 * invalidate if:
                                 * session has timedOut AND (is not active OR (the Invalidation
                                 * Multiple has not been set to 0
                                 * and the session is that many times the invalidation interval)
                                 * 
                                 * The default value for the invalidation interval is 3.
                                 * 
                                 * PK03711 removed check for active and always invalidated
                                 * timedout sessions for v6.1 and earlier
                                 * v7 CTS defect 391577 forced us to put this code back, so we
                                 * introduced the InvalidateIfActive property
                                 * In the service stream, the ForceSessionInvalidationMultiple was
                                 * used, so we are using the same property
                                 */
                                if ((timedOut)
                                    && ((!active) || ((_smc.getForceSessionInvalidationMultiple() != 0) && (currentAccessTime <= nowTime
                                                                                                                                 - (_smc.getForceSessionInvalidationMultiple() * maxinact))))) {

                                    _storeCallback.sessionInvalidatedByTimeout(s);
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                        String message = "Going to invalidate session with id=" + s.getId();
                                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[RUN_INVALIDATION], message);
                                    }
                                    s.invalidate();
                                }
                            }
                        } // isValid
                    }
                }
            } // end "while"
        } finally {
            // PK99859: Unset the thread context
            unsetThreadContext();
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[RUN_INVALIDATION]);
        }
    }

    /*
     * @see com.ibm.wsspi.session.IStore#runTimeBasedWrites()
     */
    @Override
    public void runTimeBasedWrites() {
        // do nothing for in-memory
    }

    @Override
    public void setThreadContext() {
        if (_storeHelper != null) {
            _storeHelper.setThreadContext();
        }
    }
    
    protected void setThreadContextDuringRunInvalidation() {
        if (_storeHelper != null) {
            _storeHelper.setThreadContextDuringRunInvalidation();
        }
    }

    @Override
    public void unsetThreadContext() {
        if (_storeHelper != null) {
            _storeHelper.unsetThreadContext();
        }
    }

    /*
     * called when stopping an in-memory server only
     * This method invalidates all sessions so that the appropriate listeners can
     * be called
     */
    private void invalidateAllMemorySessions() {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[INVALIDATE_ALL_MEMORY_SESSIONS], appNameForLogging);
        }

        Set s = _sessions.keySet();
        if (s != null) {
            for (Iterator it = s.iterator(); it.hasNext();) {
                MemorySession ms = (MemorySession) _sessions.get(it.next());
                ms.internalInvalidate(false);
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INVALIDATE_ALL_MEMORY_SESSIONS]);
        }
    }

    /*
     * tableKeys()
     * returns id's of all sessions
     */
    public Enumeration tableKeys() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "tableKeys", appNameForLogging);
        }
        Set keys = (_sessions != null) ? _sessions.keySet() : (new HashMap(1)).keySet();

        final Iterator iter = keys.iterator();
        return new Enumeration() {

            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            @Override
            public Object nextElement() {
                return iter.next();
            }
        };
    }

    /*
     * getFromMemory
     * Returns the session from memory
     */
    @Override
    public Object getFromMemory(Object key) {
        return _sessions.get(key);
    }

    /*
     * Accessor for the SessionManagerConfig object
     */
    public SessionManagerConfig getSessionManagerConfig() {
        return _smc;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#getSessionStatistics()
     * Accessor for the sessionStatistics object
     */
    @Override
    public SessionStatistics getSessionStatistics() {
        return _sessionStatistics;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setSessionStatistics(com.ibm.ws.session.
     * SessionStatistics)
     * Setter for the sessionStatistics object
     */
    @Override
    public void setSessionStatistics(SessionStatistics statistics) {
        _sessionStatistics = statistics;
    }

    /*
     * This memory store is shutting down, so we invalidate all sessions.
     * Overridden by persistent stores -- we don't invalidate persistent sessions
     */
    @Override
    public synchronized void stop() {
        inProcessOfStopping = true;
        this.invalidateAllMemorySessions();
    }

    /*
     * This is required to tell the session that the app is in the process of
     * stopping.
     * This is useful when we try and convert an EJB object during passivation. We
     * can't
     * do this, so this prevents us from throwing an Exception.
     */
    public boolean isInProcessOfStopping() {
        return inProcessOfStopping;
    }

    /*
     * Called as a result of an InvalidateAll(true) call on another JVM.
     * We don't really invalidate, just set max inactive interval to 0 so that
     * the next run of the invalidator will whack it.
     */
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        // backendUpdate not used here...there is no backend for in-memory
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("{").append(sessionId).append(",").append(backendUpdate).append("} ").append(appNameForLogging);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[REMOVE_INVALIDATE], sb.toString());
        }
        ISession iSess = (ISession) _sessions.get(sessionId);
        if (iSess != null) {
            iSess.setMaxInactiveInterval(0); // let invalidator whack it
        }
    }

    /*
     * @see
     * com.ibm.wsspi.session.IStore#isPresentInExternalStore(java.lang.String)
     */
    @Override
    public boolean isPresentInExternalStore(String id) {
        // There is no external store for the in Memory case
        return false;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#getShouldReuseId()
     */
    @Override
    public boolean getShouldReuseId() {
        return SessionManagerConfig.isIdReuse();
    }

    /*
     * @see com.ibm.wsspi.session.IStore#releaseSession(ISession)
     */
    @Override
    public void releaseSession(ISession session) {
        // do nothing....SessionManager.releaseSession does what we need
        return;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#refreshSession(String)
     */
    @Override
    public void refreshSession(String sessionID) {
        // in-Memory impl - do nothing
        return;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#refreshSession(String, int)
     */
    @Override
    public void refreshSession(String sessionID, int currentVersion) {
        // in-Memory impl - do nothing
        return;
    }

    /*
     * Method for accessing the httpSessListener variable which tells us if our
     * store has any HttpSession listeners associated with it
     * 
     * @see com.ibm.wsspi.session.IStore#isHttpSessionListener()
     */
    @Override
    public boolean isHttpSessionListener() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = httpSessListener + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "isHttpSessionListener", s);
        }
        return httpSessListener;
    }

    /*
     * Mutator for the httpSessListener object which tells us if our store has any
     * HttpSession listeners
     * 
     * @see com.ibm.wsspi.session.IStore#setHttpSessionListener(boolean)
     */
    @Override
    public void setHttpSessionListener(boolean b) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = b + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setHttpSessionListener", s);
        }
        httpSessListener = b;
    }

    // Method to retrieve information from the store for output
    // Nothing extra to show in-memory ... this is overwritten in the MTMStore
    public String toHTML() {
        return "";
    }

    /*
     * Method to throw a runtime Exception. This is used when we are either trying
     * to create or access a session
     * while the SessionManager is in the process of stopping.
     */
    public void throwException(String errorString) {
        ResourceBundle rb = ResourceBundle.getBundle(LoggingUtil.SESSION_LOGGER_CORE.getResourceBundleName());
        String msg = (String) rb.getObject(errorString);
        RuntimeException re = new RuntimeException(msg);
        throw re;
    }

    @Override
    public String getAffinityToken(String id, String appName) {
        return null; // not supported for memory
    }

    /*
     * @see com.ibm.wsspi.session.IStore#setIsDistributable(boolean)
     */
    @Override
    public void setDistributable(boolean b) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setDistributable", b + appNameForLogging);
        }
        distributableWebApp = b;
    }

    /*
     * @see com.ibm.wsspi.session.IStore#isDistributable()
     */
    @Override
    public boolean isDistributable() {
        return distributableWebApp;
    }

    // XD methods - don't need to implement
    // only called from an XD specific method in SessionManager
    @Override
    public ISession createSession(String id, Object multiProtocolCorrelator) {
        return null;
    }

    @Override
    public ISession getSession(String id, Object multiProtocolCorrelator) {
        return null;
    }

    @Override
    public boolean idExists(String id, Object multiProtocolCorrelator) {
        return idExists(id);
    }

    @Override
    public void refreshSession(String sessionID, Object multiProtocolCorrelator) {}

    @Override
    public void refreshSession(String sessionID, int currentVersion, Object multiProtocolCorrelator) {}

    @Override
    public void shutdown() {}

    @Override
    public ISession getSession(String id, int version, boolean isSessionAccess, Object xdCorrelator) {
        return getSession(id, version, isSessionAccess);
    }

    @Override
    public boolean needToRedirect(Object multiProtocolCorrelator) {
        return false;
    }

    // end of XD methods

    /*
     * PK68691
     * removeFromMemory
     * Removes an in-memory session from HashMap
     * 
     * @see com.ibm.wsspi.session.IStore#removeFromMemory(java.lang.String)
     */

    @Override
    public void removeFromMemory(String id) {
        //no-op for in-memory sessions - can be overridden in BackedStore
    }

    /*
     * Checks that the session is still valid. Invalidates it if needed.
     * 
     * @see com.ibm.wsspi.session.IStore#checkSessionStillValid(com.ibm.wsspi.session.ISession, long, long)
     */
    @Override
    public boolean checkSessionStillValid(ISession s, long accessedTime) {
        final String methodName = "checkSessionStillValid";
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CHECK_SESSION_STILL_VALID]);
        }
        long nowTime = System.currentTimeMillis();
        boolean rc = true;
        synchronized (s) {
            if (s.isValid()) {
                if (s.getMaxInactiveInterval() != -1) {
                    long maxinact = 1000 * (long) s.getMaxInactiveInterval();
                    boolean active = s.getRefCount() > 0;
                    if (_isApplicationSessionStore) {
                        //the RefCount is not correct when dealing with an applicationSessionStore
                        s.setRefCount(0);
                        active = false;
                    }
                    boolean timedOut = (accessedTime <= nowTime - maxinact);
                    /*
                     * invalidate if:
                     * session has timedOut AND (is not active OR (the Invalidation Multiple has not been set to 0
                     * and the session is that many times the invalidation interval)
                     * 
                     * The default value for the invalidation interval is 3.
                     * 
                     * PK03711 removed check for active and always invalidated timedout sessions for v6.1 and earlier
                     * v7 CTS defect 391577 forced us to put this code back, so we introduced the InvalidateIfActive property
                     * In the service stream, the ForceSessionInvalidationMultiple was used, so we are using the same property
                     */
                    if ((timedOut) && ((!active) ||
                            ((_smc.getForceSessionInvalidationMultiple() != 0) &&
                                    (accessedTime <= nowTime - (_smc.getForceSessionInvalidationMultiple() * maxinact))))) {
                        if (maxinact != 0) {
                            _storeCallback.sessionInvalidatedByTimeout(s);
                        }
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            String message = "Going to invalidate session with id=" + s.getId();
                            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[CHECK_SESSION_STILL_VALID], message);
                        }
                        s.invalidate();
                        rc = false;
                    }
                }
            } else { //isValid
                rc = false;
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CHECK_SESSION_STILL_VALID]);
        }
        return rc;
    }
    
    
    public void updateSessionId(String oldId, ISession newSession) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "updateSessionId", oldId);
        }
        removeSession(oldId);
        _sessions.put(newSession.getId(),  newSession);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "updateSessionId", newSession.getId());
        }
    }

}
