/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.common;

import java.util.Enumeration;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.store.common.internal.LoggingUtil;
import com.ibm.ws.session.store.memory.MemoryStore;
import com.ibm.wsspi.session.ILoader;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStoreCallback;

public abstract class BackedStore extends MemoryStore {

    private ILoader _loader;
    protected boolean remoteInvalReceived = false;
    private static final String methodClassName = "BackedStore";

    private static final int GET_SESSION = 0;
    private static final int STOP = 1;
    private static final int CREATE_SESSION = 2;
    private static final int REMOVE_FROM_MEMORY = 3;

    private static final String methodNames[] = { "getSession", "stop", "createSession", "removeFromMemory" };

    /**
     * Session store service that created this backed store.
     */
    private final SessionStoreService sessionStoreService;

    /*
     * constructor
     */
    public BackedStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, SessionStoreService sessionStoreService) {
        super(smc, storeId, sc, storeHelper);
        this.sessionStoreService = sessionStoreService;
    }

    /*
     * getSession that matches the id and version
     */
    public ISession getSession(String id, int version, boolean isSessionAccess) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_SESSION], "id = " + id);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION], "Store = " + this);
        }
        if (inProcessOfStopping) {
            throwException("SessionContext.accessWhenStop");
        }
        BackedSession session = ((BackedHashMap) _sessions).getSession(id, version, isSessionAccess);//why not call this -> ((BackedHashMap)_sessions).getSession(id, version);
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_SESSION], "session = " + session);
        }
        return session;
    }

    /*
     * isPresent - determine if the input id is in use by any other app
     */
    public boolean isPresentInExternalStore(String id) {
        if (_sessions != null) {
            return ((BackedHashMap) _sessions).isPresent(id);
        }
        return false;
    }

    /*
     * setLoader
     */
    public void setLoader(ILoader loader) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            String s = loader + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setLoader", s);
        }
        _loader = loader;
    }

    /*
     * getLoader
     */
    public ILoader getLoader() {
        return _loader;
    }

    /*
     * setStoreCallback
     */
    public void setStoreCallback(IStoreCallback callback) {
        super.setStoreCallback(callback);
        ((BackedHashMap) _sessions).setStoreCallback(callback);//need to set the Store Callback on the LRU Hashmap as well.
    }

    /*
     * removeSession - remove the session from the store
     */
    public void removeSession(String id) {
        ((BackedHashMap) _sessions).removePersistedSession(id);
    }

    /*
     * runTimeBasedWrites - persist sessions that have changes since last run
     */
    public void runTimeBasedWrites() {
        ((BackedHashMap) _sessions).doTimeBasedWrites(false);
    }

    /*
     * runInvalidation - perform background invalidation of timed out sessions
     */
    public void runInvalidation() {
        ((BackedHashMap) _sessions).cleanUpCache(System.currentTimeMillis());
        ((BackedHashMap) _sessions).performInvalidation();
        /*
         * if (_isApplicationSessionStore) {
         * ((BackedHashMap)_sessions).performAppSessionInvalidation();
         * } else {
         * ((BackedHashMap)_sessions).performInvalidation();
         * }
         */
    }

    /*
     * method toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("# BackedStore # \n { ").
                        append("\n _storeId=").append(_storeId);
        if (_sessions != null) {
            sb.append("\n _sessions=").append(_sessions.size());
        } else {
            sb.append("\n _sessions=null");
        }
        sb.append("\n } \n");
        return sb.toString();
    }

    /*
     * getSessions() - return the BackedHashMap reference
     */
    public BackedHashMap getSessions() {
        return (BackedHashMap) _sessions;
    }

    /*
     * getFromMemory - returns the session from memory or null
     * if session doesn't exist in memory. Session could still
     * exist in the persistent store even if we return null.
     */

    public Object getFromMemory(Object key) {
        return ((BackedHashMap) _sessions).superGet(key);
    }

    /*
     * stop - performs shutdown processing
     */
    public synchronized void stop() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[STOP]);
        }
        inProcessOfStopping = true;

        sessionStoreService.setCompletedPassivation(false); // 128284
        //This is only executed for Time-based writes ... not for manual or End-Of-Service writes
        if (_smc.getEnableTimeBasedWrite()) {
            ((BackedHashMap) _sessions).doTimeBasedWrites(true);
        }

        // We'll get here for:
        //  1) Stop server
        //  2) Stop Application/ear
        //  3) Restart Application/ear

        // Look at all cached sessions and passivate them if necessary
        Enumeration vEnum = tableKeys();
        while (vEnum.hasMoreElements()) {
            String id = (String) vEnum.nextElement();
            BackedSession s = (BackedSession) ((BackedHashMap) _sessions).superGet(id);
            _storeCallback.sessionWillPassivate(s);
            //in case you update the session during passivation
            if (_smc.getPersistSessionAfterPassivation()) {
                //Need to also write the session if passivate updated any attributes.
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[STOP], "Persisting the session after it was passivated. " + s.getAppNameAndID());
                }
                s.removingSessionFromCache = true;
                s.sync();
                //sync calls this: ((BackedHashMap)_sessions).put(id, s);
                s.removingSessionFromCache = false;
            }
        }

        sessionStoreService.setCompletedPassivation(true); // 128284

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[STOP]);
        }
    }

    /*
     * createSession - ensures that a newly generated id is not a duplicate and
     * also ensures if another thread just created the session, we return it rather
     * than creating another.
     */
    public ISession createSession(String sessionId, boolean newId) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        //need some way to checkForDuplicateCreatedIds or synchronize on some object or something ... look at in-memory
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("id = ").append(sessionId).append(appNameForLogging);
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[CREATE_SESSION], sb.toString());
        }
        if (inProcessOfStopping) {
            throwException("SessionContext.createWhenStop");
        }
        BackedSession session = null;
        session = createSessionObject(sessionId);
        session.updateLastAccessTime(session.getCreationTime());
        session.setInsert(); // flag new session for insertion into store
        session.setMaxInactiveInterval((int) _smc.getSessionInvalidationTime());
        session.setUserName(ANONYMOUS_USER);
        _sessions.put(sessionId, session);
        //        if (session.duplicateIdDetected) { // could happen on zOS if key is defined incorrectly - perhaps doesn't include appname
        //                                           // other than that, this really should never happen - duplicate ids should not be generated
        //            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.WARNING, methodClassName, methodNames[CREATE_SESSION], "Store.createSessionIdAlreadyExists");
        //            session = null;
        //        }

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[CREATE_SESSION], "session = " + session);
        }
        return session;
    }

    /*
     * PK68691
     * removeFromMemory
     * Removes session from Memory if persistence is enabled, otherwise no op
     *
     * @see com.ibm.wsspi.session.IStore#removeFromMemory(java.lang.String)
     */
    public void removeFromMemory(String id) {
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOVE_FROM_MEMORY], id);
        }

        BackedSession sess = (BackedSession) ((BackedHashMap) _sessions).superGet(id);
        if (sess != null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE))
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_FROM_MEMORY],
                                                        "removing from memory");
            synchronized (sess) {
                BackedSession sess1 = (BackedSession) ((BackedHashMap) _sessions).accessObject(id);
                if (sess == sess1) {
                    Object removedEntry = ((BackedHashMap) _sessions).superRemove(id);
                    if (removedEntry != null) {
                        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE))
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_FROM_MEMORY],
                                                                    "successfully removed from memory");
                    }
                }
            }
        }

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER))
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOVE_FROM_MEMORY]);
    } // end "removeFromMemory"

    /**
     * Override this with implementation that invokes super.remoteInvalidate
     */
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        remoteInvalReceived = true;
    }

    public abstract BackedSession createSessionObject(String sessionId);

    public void updateSessionId(String oldId, ISession newSession) {
        super.updateSessionId(oldId, newSession);

        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, "updateSessionId", oldId);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "updateSessionId", "New session information: creation time = "
            + newSession.getCreationTime() + " last access time = " + newSession.getLastAccessedTime() +" maxInactiveInterval = "
            		+ newSession.getMaxInactiveInterval() + " user name = " + newSession.getUserName());
        }
        removeSession(oldId);
        createSessionObject(newSession.getId());
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER))
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, "updateSessionId", newSession.getId());
    }
}
