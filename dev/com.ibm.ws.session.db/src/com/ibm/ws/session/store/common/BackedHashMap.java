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

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.utils.LRUHashMap;
import com.ibm.wsspi.session.IStore;

/*
 *  This is the "hashtable" abstract class for persistent sessions.
 *  It extends LURHashMap and is extended by DatabaseHashMap and MTMHashMap.
 *  Extenders must implements the abstract methods at the end of this file.
 */

public abstract class BackedHashMap extends LRUHashMap {

    private final IStore _iStore;
    //Hashtable to handle lastAccess time for manual & time-based writes
    public Hashtable cachedLastAccessedTimes = null;

    final SessionManagerConfig _smc;

    private static final long serialVersionUID = -4653089886686024589L;
    private static final String methodClassName = "BackedHashMap";

    //Map used to keep track of recently Invalidated Ids
    Map recentInvalidIds = null;

    //this is used to help us determine whether to update the version id on failover.
    protected boolean _isApplicationSessionHashMap = false;

    private final static int GET_SESSION = 0;
    private final static int RETRIEVE_SESSION = 1;
    private final static int GET = 2;
    private final static int UPDATE_SESSION = 3;
    private final static int UPDATE_CACHE_ID = 4;
    private final static int PUT = 5;
    private final static int PUT_NO_REPLACE = 6;
    private final static int SUPER_REMOVE = 7;
    private final static int SUPER_GET = 8;
    private final static int SUPER_PUT = 9;
    private final static int PASSIVATE_SESSION = 10;
    private final static int DO_SCHEDULED_INVALIDATION = 11;
    private final static int HANDLE_DISCARDED_CACHE_ITEMS = 12;
    private final static int CLEAN_UP_CACHE = 13;
    private final static int DO_TIME_BASED_WRITES = 14;

    private final static String methodNames[] = { "getSession", "retrieveSession", "get", "updateSession", "updateCacheId",
                                                 "put", "putNoReplace", "superRemove", "superGet", "superPut",
                                                 "passivateSession", "doScheduledInvalidation", "handleDiscardedCacheItems", "cleanUpCache", "doTimeBasedWrites" };

    /*
     * Constructor
     */
    public BackedHashMap(IStore store, SessionManagerConfig smc) {
        super(smc.getInMemorySize());
        _smc = smc;
        _iStore = store;
        setStoreCallback(_iStore.getStoreCallback());
        if (!_smc.getEnableEOSWrite()) {
            cachedLastAccessedTimes = new Hashtable<String, Long>(smc.getInMemorySize());
        }
        if (_smc.getCheckRecentlyInvalidList()) {
            setRecentInvalTable();
        }
    }

    public Hashtable<String, Long> copyAndClearCachedLastAccessedTimes() {
        @SuppressWarnings("unchecked")
        Hashtable<String, Long> copy = (Hashtable<String, Long>) cachedLastAccessedTimes.clone();
        cachedLastAccessedTimes.clear();
        return copy;
    }

    public BackedSession getSessionRetrievalTrue(String id, int versionId, boolean isSessionAccess) {
        //Force the retrieval of the session from the backend (only used for appSessions)
        return getSession(id, versionId, isSessionAccess, true);
    }

    BackedSession getSession(String id, int versionId, boolean isSessionAccess) {
        //A normal call - do not force a retrieval (only used for appSessions)
        return getSession(id, versionId, isSessionAccess, false);
    }

    /*
     * getSession
     */
    BackedSession getSession(String id, int versionId, boolean isSessionAccess, boolean retrieval) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        //id coming in does NOT have the Cache ID at the front.
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("{").append(id).append(",").append(versionId).append("}");
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_SESSION], sb.toString());
        }

        BackedSession sess = null;

        //incase of ssl, sessionid can go upto ???. So do this check only for
        //non-ssl id's. For ssl id session tracking, even when request is not
        //over ssl, this if is skipped which will result in database read
        //for non-websphere-session id's.
        if (!_smc.useSSLId() && id.length() != SessionManagerConfig.getSessionIDLength()) {
            return null;
        }

        //Check if session is in cache...
        //accessObject will return the session from cache and also
        //updates LRU cache
        sess = (BackedSession) accessObject(id);
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (sess == null) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION], "session not found in cache " + id);
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION],
                                                    "requested version: " + versionId + "; object's version: " + sess.getVersion());
            }
        }

        //used to determine if we need to update the version id when version already out of sync
        //if the lastAccessedTimeOnLocalCopy equals the lastAccessedTimeOnTheBackendCopy, we know another webapp updated the version
        //and therefore, we won't have to increment the version again, but we will bring the current session version in sync with the latest version
        long lastAccessTimeOnLocalCopy = 0;

        if (sess != null) {

            long nowTime = System.currentTimeMillis();

            boolean isValid = isValidCopy(sess, nowTime, versionId);

            if (isValid) {
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION], "valid cache copy " + id);
                }
                //
                // Possibly update last access now.
                // we do it now before returning to make sure a inval thread
                // does not time us out before we do anything with this session

                //  Cases are:
                //    Write at EOS Mode - always update lastaccess now
                //                        (updated for scheduled invalidation now since we need to update
                //                         the db since another JVM checks the db for non-expired sessions
                //                         in order to determine if the sessionid should be reused)
                //
                //    Manual Update - never write lastaccess now since it will always get written when
                //                    the invalidation thread runs.  Note that the lastaccess times
                //                    still get written at invalidation time when scheduled Invalidation
                //                    is configured.
                //
                //    Time Based  - never write lastaccess now
                //                  since it will always get written when the periodic write occurs.
                //                   Note that the asyncUpd flag is "on" for TimeBasedWrite.
                //
                //
                if (isSessionAccess) {
                    if (_smc.getEnableEOSWrite() && !_smc.getScheduledInvalidation()) {
                        int rowsRet = overQualLastAccessTimeUpdate(sess, nowTime);
                        // if rowsRet is 0, means an inval thread beat us to it,
                        // need to return null in this case

                        if (rowsRet == 0 && !sess.needToInsert) {
                            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION], "session may be invalid in cache " + id);
                            }

                            synchronized (sess) {
                                BackedSession sess1 = (BackedSession) accessObject(id);
                                if (sess == sess1) {
                                    superRemove(id);
                                }
                            }
                            // Don't want to pasivate as session has already been invalidated
                            sess = null;
                        }
                    } else {
                        synchronized (sess) {
                            sess.updateLastAccessTime(nowTime);
                        }
                    }
                }

                // don't need to update the browser token (i.e. cookie)
                if (sess != null) {
                    if (retrieval) { //retrieval is set if we NEED to search the backend
                        //Cache copy might not be current, remove it from the cache and
                        //read it from external store
                        synchronized (sess) {
                            BackedSession sess1 = (BackedSession) accessObject(sess.getId());
                            if (sess == sess1) {
                                superRemove(sess.getId());
                            }
                        }
                    } else {
                        return sess;
                    }
                }

            } else {
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SESSION], "Cache copy is out of date or invalid " + id);
                }

                //Cache copy is invalid,  remove it from the cache and
                //read it from external store
                synchronized (sess) {
                    BackedSession sess1 = (BackedSession) accessObject(sess.getId());
                    if (sess == sess1) {
                        superRemove(sess.getId());
                    }
                    //lastAccessTimeOnLocalCopy = sess.getCurrentAccessTime();
                }
            }
        }

        //sess = retrieveSession(id, versionId, lastAccessTimeOnLocalCopy);
        sess = retrieveSession(id, versionId, sess);

        return sess;
    }

    /*
     * isValidCopy - determines if the cached session is still valid
     */
    boolean isValidCopy(BackedSession sess, long nowTime, int versionId) {
        synchronized (sess) {
            if (!sess.isValid())
                return false;

            int maxTime = sess.getMaxInactiveInterval();
            boolean accBeforeTO = (sess.getCurrentAccessTime() >= nowTime - (1000 * (long) maxTime));//PK90548
            boolean cacheIdMatch = (sess.getVersion() >= versionId);

            return (cacheIdMatch && (accBeforeTO || maxTime == -1));
        }
    }

    /*
     * retrieveSession - from the backend store
     */
    BackedSession retrieveSession(String id, int versionId, BackedSession inMemSess) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        boolean useTheInMemSess = false;
        long lastAccessedTimeOnLocalCopy = 0;
        if (inMemSess != null && inMemSess.isValid()) {
            lastAccessedTimeOnLocalCopy = inMemSess.getCurrentAccessTime();
        }
        long lastAccessTimeOnBackendCopy = 0;

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("{").append(id).append(",").append(versionId).append(",").append(lastAccessedTimeOnLocalCopy).append("}");
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[RETRIEVE_SESSION], sb.toString());
        }
        BackedSession sess = null;
        sess = (BackedSession) superGet(id);
        if (sess != null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RETRIEVE_SESSION], "Found session in local cache.");
            }
            return sess;
        }

        if (isPresentInRecentlyInvalidatedList(id)) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RETRIEVE_SESSION], "null - Id present in recently invalidated list.");
            }
            return null;
        }

        sess = readFromExternal(id);

        //Make sure only one copy of session object is in cache
        //and the same copy is returned to concurrent requests
        //entering here.
        boolean cacheIdUpdate = false;
        if (sess != null) {
            lastAccessTimeOnBackendCopy = sess.getCurrentAccessTime();
            BackedSession thrownOutSess = null;
            synchronized (this) {
                BackedSession ds = (BackedSession) superGet(id);
                if (ds == null) {
                    if (!_isApplicationSessionHashMap) { // don't update versionId for appSessions as this could update it twice.  We don't have a good solution for appSessions yet.
                        cacheIdUpdate = true;
                    }
                    //If it is Time-based or manual writes, we could have not updated the backend yet.  If we retrieve an
                    //old version from the backend, have another server update the backend with this version #, then we
                    //update the backend with this version #, both servers could think that they have a valid id in their
                    //local cache.  We want to do the lastAccessTimes check for endOfService writes so that we don't thrash
                    //on a failover.
                    //PK47847 allows us to check the lastAccessTimes for !EOS writes.  The customer should be on one machine or have their times set to be identical.
                    //before we always updated the cache ... must maintain behavior
                    if (_smc.getEnableEOSWrite() || _smc.getOptimizeCacheIdIncrements()) {
                        if (lastAccessedTimeOnLocalCopy == lastAccessTimeOnBackendCopy ||
                                ((lastAccessedTimeOnLocalCopy > lastAccessTimeOnBackendCopy) && _smc.getOptimizeCacheIdIncrements())) {
                            //use the in-memory copy && don't update the cache
                            //when the lastAccessedTimeOnLocalCopy>lastAccessTimeOnBackendCopy we
                            //only want to use the in-memory copy (even when using EOS) when the property is set
                            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[RETRIEVE_SESSION], "Using in-memory session instead of retrieved.");
                            }
                            cacheIdUpdate = false;
                            sess = inMemSess;
                            useTheInMemSess = true;
                        }
                    }
                    thrownOutSess = (BackedSession) superPut(id, sess);
                    //update to current version ... if cacheIdUpdate is true, we will increment from this value
                    sess.setVersion(versionId);
                    sess.setIsNew(false);
                } //end if (ds=null)
                else {
                    //Make sure we have only one instance
                    sess = ds;
                }
            }
            if (thrownOutSess != null && !useTheInMemSess)
                this.passivateSession(thrownOutSess);
        } else {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RETRIEVE_SESSION], "Session is not found on backend");
            }
            return null;
        }

        synchronized (sess) {
            long nowTime = System.currentTimeMillis();
            long prevLastAccess = sess.getCurrentAccessTime(); // cmd 200713

            //Update the row with current timestamp
            int rowCount = -1;
            if (prevLastAccess < nowTime) {
                rowCount = updateLastAccessTime(sess, nowTime);
            } else {
                //if this request was served by an another clone on a different
                //machine then it is possible that nowTime of system is less
                //than lastaccesstime in session, if there  is time diff between
                //boxes
                nowTime = prevLastAccess;
            }

            if (rowCount == 0) {
                superRemove(sess.getId());
                sess = null;
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RETRIEVE_SESSION], "null - updateLastAccessTime failed.");
                }
                return sess;
            }

            sess.updateLastAccessTime(nowTime);
            sess.setLastWriteLastAccessTime(nowTime);

            sess.initSession(_iStore);
            if ((!useTheInMemSess) &&
                    (sess.getSwappableListeners(BackedSession.HTTP_SESSION_ACTIVATION_LISTENER) ||
                    SessionManagerConfig.is_zOS())) { // cmd 420285 must call for zOS to re-establish affinity
                getStoreCallback().sessionDidActivate(sess); // call activation listeners
            }
        }

        if (cacheIdUpdate) {
            updateCacheId(sess);
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[RETRIEVE_SESSION], "session found");
        }
        return sess;
    }

    /*
     * get
     */
    public Object get(Object key, int version) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET], key + " " + version);
        }

        String id = (String) key;
        Object obj = getSession(id, version, true); //update the last access time?

        return obj;

    }

    /*
     * updateSession - updates an existing session
     */
    public void updateSession(BackedSession backedSess) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[UPDATE_SESSION]);
        }

        if (!backedSess.isValid()) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_SESSION], "session " + backedSess.getId()
                                                                                                              + " has been invalidated since last access, clean up and return ");
            }

            if (backedSess.appDataChanges != null)
                backedSess.appDataChanges.clear();
            if (backedSess.appDataRemovals != null)
                backedSess.appDataRemovals.clear();
            return;
        }

        if (cachedLastAccessedTimes != null) { // manual write or time based writes
            if (backedSess.cacheLastAccessedTime) {
                cachedLastAccessedTimes.put(backedSess.getId(), new Long(backedSess.getCurrentAccessTime()));
                return; // only cache last access time to be written prior to inval thread
            } else {
                cachedLastAccessedTimes.remove(backedSess.getId());
            }
        }

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_SESSION], "Do the write!");
        }

        boolean propHit = false;

        if (_smc.writeAllProperties()) {
            propHit = true;
        }

        if (!propHit && backedSess.appDataChanges != null) {
            propHit = !backedSess.appDataChanges.isEmpty();
        }

        if (!propHit && backedSess.appDataRemovals != null) {
            propHit = !backedSess.appDataRemovals.isEmpty();
        }

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_SESSION], "propHit is: " + propHit);
            if (propHit) {
                String changes = (backedSess.appDataChanges == null ? "null" : backedSess.appDataChanges.keySet().toString());
                String removals = (backedSess.appDataRemovals == null ? "null" : backedSess.appDataRemovals.keySet().toString());
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[UPDATE_SESSION], "prop hit " + propHit + " app changes " + changes + " app removals "
                                                                                                              + removals);
            }
        }

        boolean writeSucc = persistSession(backedSess, propHit);

        backedSess.update = null;
        if (writeSucc) {

            backedSess.userWriteHit = false;
            backedSess.maxInactWriteHit = false;
            backedSess.listenCntHit = false;
            // PK32205: Clear appDataChanges and appDataRemovals only if it
            // is not a MR configuration. In MR configuration these are
            // appropriately cleared in handlePropertyHits
            if (!_smc.isUsingMultirow()) {
                if (backedSess.appDataChanges != null)
                    backedSess.appDataChanges.clear();
                if (backedSess.appDataRemovals != null)
                    backedSess.appDataRemovals.clear();
            }
        }
    }

    /*
     * updateCacheId
     */
    void updateCacheId(BackedSession backedSess) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[UPDATE_CACHE_ID]);
        }

        backedSess.getStoreCallback().sessionAffinityBroke(backedSess);

        int i = backedSess.getVersion();
        // if at all 9's, reset
        if (i == 9999) {
            backedSess.setVersion(BackedSession.initialCacheId);
        } else {
            i++;
            backedSess.setVersion(i);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[UPDATE_CACHE_ID]);
        }
    }

    /*
     * put - insert and/or updates a session as appropriate
     */
    public Object put(Object key, Object value) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[PUT], "key = " + key);
        }
        String id = (String) key;
        BackedSession backedSess = (BackedSession) value;
        // in the unlikely case the session is created and invalidated on the same http request
        if (!backedSess.isValid()) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PUT], "Session is not valid.  Returning null");
            }
            return null;
        }

        // needToInsert is set to false when successfully persisted.  Will stay true if insertSession fails because db is down.
        if (backedSess.needToInsert) {
            insertSession(backedSess);
            if (backedSess.isNew()) {
                if (!backedSess.removingSessionFromCache) {
                    //                if ((!backedSess.removingSessionFromCache) && (!backedSess.duplicateIdDetected)) { // since duplicate keys can't be detected in lWAS, backedSess.duplicateIdDetected is always true
                    BackedSession thrownOutSess = (BackedSession) superPut(id, backedSess);
                    if (thrownOutSess != null)
                        this.passivateSession(thrownOutSess);
                }
            } else {
                updateSession(backedSess);
            }
        } else {
            updateSession(backedSess);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[PUT]);
        }
        return null;
    }

    /*
     * superRemove - remove from live cache
     */
    public Object superRemove(Object id) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SUPER_REMOVE]);
        }

        Object o = super.remove(id);
        if (o != null) {
            removeFromRecentlyInvalidatedList((String) id);
        }
        return o;
    }

    /*
     * superGet - get from live cache
     */
    public Object superGet(Object id) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SUPER_GET], "id = " + id);
        }

        Object sess = super.get(id);
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (sess == null) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[SUPER_GET], "returned NULL");
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[SUPER_GET], "returned a SESSION: ");
            }
        }

        return sess;
    }

    /*
     * superPut - Put into live cache
     */
    public Object superPut(Object key, Object value) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SUPER_PUT], "" + key);
        }
        BackedSession sess = (BackedSession) super.put(key, value);
        //The LRU HashMap handles the PMI counters for CacheDiscards and MemoryCount
        return sess;
    }

    /*
     * Method used to keep track of ids that have been recently invalidated. If the checkRecentlyInvalidList
     * property is set and we know that it has been recently invalidated, we will not query the backend for
     * this session.
     */
    protected void addToRecentlyInvalidatedList(String id) {
        if (!_smc.getCheckRecentlyInvalidList())
            return;
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "addToRecentlyInvalidatedList", "Adding to recently InvalidatedList");
        }
        synchronized (recentInvalidIds) {
            recentInvalidIds.put(id, id);
        }
    }

    /*
     * Method used to remove ids from recently invalidated list
     */
    public void removeFromRecentlyInvalidatedList(String id) {
        if (!_smc.getCheckRecentlyInvalidList())
            return;
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "removeFromRecentlyInvalidatedList", "Removing From recently InvalidatedList");
        }
        synchronized (recentInvalidIds) {
            recentInvalidIds.remove(id);
        }
    }

    /*
     * Method used to tell us if the given id is in the recentlyInvalidatedList
     */
    protected boolean isPresentInRecentlyInvalidatedList(String id) {
        if (!_smc.getCheckRecentlyInvalidList())
            return false;
        return recentInvalidIds.containsKey(id);
    }

    /*
     * Method used to set the size of the recentInvalidatedList
     * If the tableSize is <=1, we will automatically not check this list
     */
    void setRecentInvalTable() {
        int tableSize = _smc.getInMemorySize();
        if (tableSize > 1)
            recentInvalidIds = new LRUHashMap(tableSize / 2);
        else
            _smc.setCheckRecentlyInvalidList(false);
    }

    /*
     * passivateSession - writes session to persistent store and does
     * sessionWillPassivate callback
     */
    private void passivateSession(BackedSession sess) {
        synchronized (sess) { // Keep PropertyWriter thread from sneaking in..

            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PASSIVATE_SESSION], "check for Passivation Listener for session");
            }

            // note that sessionCacheDiscard was called by LRUHashMap.put when he had to eject the oldest
            getStoreCallback().sessionWillPassivate(sess);

            if (_smc.getEnableTimeBasedWrite() || _smc.getPersistSessionAfterPassivation()) {
                // Since removing from the cache... Need to write the session to the database
                //  Don't bother checking if this session is in the service method
                //  as it is the oldest entry in the cache.
                // Need to also write the session if passivate updated any attributes.
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    if (_smc.getEnableTimeBasedWrite()) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PASSIVATE_SESSION],
                                                            "Removing oldest entry from cache with TimeBasedWrits. Write it! " + sess.getId() + " " + getAppName());
                    } else {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PASSIVATE_SESSION],
                                                            "Persisting the session after it was passivated. " + sess.getId() + " " + getAppName());
                    }
                }
                sess.removingSessionFromCache = true;
                sess.sync();
                sess.removingSessionFromCache = false;
            }
        }
    }

    /*
     * listenerFlagUpdate
     * This method ensures that the session is flagged as a listener if the the app
     * has an HttpSessionListener. We need to know so we can call sessionDestroyed
     * at invalidation time.
     *
     * The HTTP_SESSION_BINDING_LISTENER constant is overloaded and used for both
     * HttpSession listener and Binding listener.
     */
    public void listenerFlagUpdate(BackedSession d2) {

        if (_iStore.isHttpSessionListener()) {
            switch (d2.listenerFlag) {
                case BackedSession.HTTP_SESSION_NO_LISTENER:
                    d2.listenerFlag = BackedSession.HTTP_SESSION_BINDING_LISTENER;
                    break;
                case BackedSession.HTTP_SESSION_ACTIVATION_LISTENER:
                    d2.listenerFlag = BackedSession.HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER;
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * doScheduledInvalidation - determines if background invalidation processing should proceed.
     * Returns true if:
     * 1) Scheduled invalidation feature is not enabled (normal case as I don't think schduled inval is really popular)
     * 2) Scheduled invalidation is enabled AND we've received a remove invalidation from invalidateAll(true) AND the
     * DoRemoteInvalidations property is true. (Highly unlikely scenario)
     * 3) Scheduled invalidation is enabled and it is currently one of the 2 scheduled invalidation hours.
     */
    protected boolean doScheduledInvalidation() {
        boolean scheduledInvalidationEnabled = _smc.getScheduledInvalidation();
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[DO_SCHEDULED_INVALIDATION], "scheduledInvalidationEnabled is " + scheduledInvalidationEnabled);
        }

        if (!scheduledInvalidationEnabled || (((BackedStore) getIStore()).remoteInvalReceived && SessionManagerConfig.isDoRemoteInvalidations())) { // then we always do invalidation
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[DO_SCHEDULED_INVALIDATION], "returning true because scheduled invalidation is not enabled");
            }
            return true;
        }

        Calendar current = Calendar.getInstance();
        int currentHour = current.get(Calendar.HOUR_OF_DAY);

        int scheduledHour1 = _smc.getInvalTime1();
        int scheduledHour2 = _smc.getInvalTime2();
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_SCHEDULED_INVALIDATION], "currentHour:" + currentHour);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_SCHEDULED_INVALIDATION], "scheduled hours are " + scheduledHour1 + " and "
                                                                                                                     + scheduledHour2);
        }
        if (currentHour == scheduledHour1 || currentHour == scheduledHour2) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[DO_SCHEDULED_INVALIDATION], "returning true because current hour matches scheduled hour");
            }
            return true;
        } else {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[DO_SCHEDULED_INVALIDATION],
                                                       "returning false because it is not currently one of the scheduled hours");
            }
            return false;
        }
    }

    /*
     * getAppName
     */
    public String getAppName() {
        return _iStore.getId();
    }

    /*
     * tableKeys
     */
    Enumeration tableKeys() {
        Set keys = keySet();
        final Iterator iter = keys.iterator();
        return new Enumeration() {
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public Object nextElement() {
                return iter.next();
            }
        };
    }

    /*
     * handleDiscardedCacheItems - removes sessions from cache and calls sessionCacheDiscard
     */
    public void handleDiscardedCacheItems(Enumeration vEnum) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[HANDLE_DISCARDED_CACHE_ITEMS]);
        }

        while (vEnum.hasMoreElements()) {
            Object id = vEnum.nextElement();
            Object o = superRemove(id); //returns the session
            if (o != null) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[HANDLE_DISCARDED_CACHE_ITEMS], "removed session " + id + " for app "
                                                                                                                                + getAppName());
                }
                getStoreCallback().sessionCacheDiscard(o);
            }
        }
    }

    /*
     * hasTimedOut - check if given session has timed out
     */
    private boolean hasTimedOut(BackedSession d, long now) {
        boolean timedOut = false;
        int timeout = d.getMaxInactiveInterval();

        //Zero means we just read the session from database
        if (timeout > 0) {
            long lastTouch = d.getCurrentAccessTime();
            long invalidLastTouch = now - (1000 * (long) timeout);//PK90548

            if (lastTouch <= invalidLastTouch) {
                timedOut = true;
            }
        }

        return timedOut;
    }

    /*
     * cleanUpCache
     */
    protected Enumeration cleanUpCache(long now) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[CLEAN_UP_CACHE]);
        }

        Vector timedOutSessions = new Vector();

        Enumeration sessionIds = tableKeys();
        while (sessionIds.hasMoreElements()) {
            String id = (String) sessionIds.nextElement();
            BackedSession backedSess = (BackedSession) superGet(id);

            if (backedSess != null) {
                try {
                    if (hasTimedOut(backedSess, now)) {
                        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[CLEAN_UP_CACHE], "adding to inval list " + backedSess.getId());
                        }
                        timedOutSessions.addElement(backedSess.getId());
                    }

                } catch (Throwable t) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.common.BackedHashMap.cleanUpCache", "951", backedSess);
                    timedOutSessions.addElement(backedSess.getId());
                }
            }
        }

        Enumeration timedOutEnum = timedOutSessions.elements();
        handleDiscardedCacheItems(timedOutEnum);
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[CLEAN_UP_CACHE], timedOutSessions.elements());
        }
        return timedOutSessions.elements();
    }

    /*
     * getAppDataTablesPerThread - returns the boolean
     * only true for mulitrow db if other conditions are met - see constructor for DatabaseHashMapMR
     */
    public abstract boolean getAppDataTablesPerThread();

    /*
     * getIStore - this is set in the constructor
     */
    public IStore getIStore() {
        return _iStore;
    }

    public void setIsApplicationSessionHashMap(boolean b) {
        _isApplicationSessionHashMap = b;
    }

    /*
     * doTimeBasedWrites - called periodically when time-based-writes is configured
     */
    public void doTimeBasedWrites(boolean forceWrite) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[DO_TIME_BASED_WRITES]);
        }

        BackedSession cachedSession;
        Object currentKey;

        if (forceWrite) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES], "Entered for forceWrite request");
            }
        }

        long now = System.currentTimeMillis();
        long writeInterval = _smc.getPropertyWriterInterval() * 1000; //convert seconds to milliseconds

        Enumeration vEnum = tableKeys();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES], "!!!!!!!!!!PropertyWriteThread Loop STARTS Here!!!!!!!!!!!!! ");
        }
        while (vEnum.hasMoreElements()) {
            //LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodName, "Look at next Cache Element");
            currentKey = (Object) vEnum.nextElement();
            cachedSession = (BackedSession) super.get(currentKey);
            if (cachedSession != null) {
                //LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodName, "Cache element NOT NULL");

                long lastWrite = cachedSession.getLastWriteTime();
                long lastAccess = cachedSession.getCurrentAccessTime();

                //  English Translation for the test below:
                //
                //    If lastWrite is -1
                //       then the session must have just been read into
                //       the cache... Don't know when the lastWrite was or
                //       if something has changed since the read.. Therefore
                //       do the write.  This shouldn't happen often.
                //
                //    If the Write Interval has elapsed and the session has been
                //       changed then do the write. This should be the normal case.
                //

                if ((lastWrite == -1) || ((((now - lastWrite) > writeInterval) || forceWrite) && (lastAccess > lastWrite))) {

                    /*
                     * commented out in old code
                     * // LoggingUtil.SESSION_LOGGER_WAS.debug(SessionContext.tc, SessionConstants.doTimeBasedWrites + " Passed test... consider doing the write");
                     * end of commented out code
                     */

                    // Hold synchronized lock while we check if this session is
                    // executing in the servlet service method.  New request threads
                    // for this session will call the incrementInServiceMethodCount()
                    // method.  That method also does a "synchronized" on the session
                    // object, therefore guaranteeing that no new requests will update the
                    // session object until this write completes.

                    synchronized (cachedSession) {

                        //  Now that we have the session Object locked, check that the
                        //  session is still valid.  It is possible that the servlet
                        //  called the session.invalidate() method under a different
                        //  thread after we got the keys from the cache.  Since
                        //  sessionData.invalidate() does a sync on the session, it is now
                        //  safe to check...
                        //
                        //  Also, account for the case where the session was aged out
                        //  of the cache after we got the keys above...

                        //  We will never get here for a Timed Out invalidation since
                        //  the Invalidation time must be at least twice the Write Interval
                        //  and the write will always be done before an invalidation condition
                        //  can occur.

                        //cachedSession = (DatabaseSessionData) ((BackedHashtable) mSessions).superGet(currentKey);

                        if (cachedSession.isValid()) {
                            String id = cachedSession.getId();
                            //if the refCount!=0, it is still active in the service method
                            if (cachedSession.getRefCount() != 0) {
                                // skip the entry.. The next time this thread runs
                                // it will get tried again...
                                cachedSession.deferWriteUntilNextTick++;
                                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES],
                                                                        "Defer write until next time since session is in the service method "
                                                                                        + cachedSession.deferWriteUntilNextTick + " " + id);
                                }
                                // Failed to write on 5 tries. Force the write to database!!
                                if (cachedSession.deferWriteUntilNextTick > 5) {
                                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES],
                                                                            "Failed to write on 5 tries. Force the write to database!! " + " " + id);
                                    }
                                    cachedSession.setLastWriteTime(now);
                                    // cachedSession.sync();
                                    cachedSession.flush();
                                    cachedSession.deferWriteUntilNextTick = 0;
                                }
                            } else {
                                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES],
                                                                        "Do the session Write and update lastWriteTime " + id);
                                }

                                // It is important to note that lastWriteTime is being
                                // updated to the current time (now), though lastAccess may
                                // contain a value up to Now-WriteInterval.  After this
                                // write completes it is possible that the Session is
                                // used again.  The Write of LastAccess will not take
                                // place again until WriteInterval elapses again.  So, in order
                                // for this session not to be inadvertently invalidated
                                // by a different clone, it is necessary that the invalidation
                                // time be AT LEAST 2 TIMES the WriteInterval.  This should
                                // be enforced in the GUI.

                                cachedSession.setLastWriteTime(now);
                                cachedSession.flush(); // do the write
                                cachedSession.deferWriteUntilNextTick = 0;
                            }
                        } else {
                            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES], "Session no longer in Cache!");
                            }
                        }
                    } //end of synchronized
                } else {
                    //Database Write did NOT occur
                    //LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodName, "Database Write did NOT occur!");
                }
            } else {
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES], "The Cache element is NULL");
                }
            }
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[DO_TIME_BASED_WRITES], "!!!!!!!!!! PropertyWriteThread Loop ENDS Here!!!!!!!!!!!!!");
        }
    } // end of method

    /*
     * abstract methods to be implemented by extenders of this class
     * See DatabaseHashMap and MTMHashMap for details
     */
    protected abstract void removePersistedSession(String id);

    protected abstract int updateLastAccessTime(BackedSession sess, long nowTime);

    protected abstract int overQualLastAccessTimeUpdate(BackedSession sess, long nowTime);

    protected abstract BackedSession readFromExternal(String id);

    protected abstract Object loadOneValue(String id, BackedSession bs);

    protected abstract void insertSession(BackedSession d2);

    protected abstract boolean persistSession(BackedSession d2, boolean propHit);

    protected abstract boolean isPresent(String id);

    protected abstract void performInvalidation();
    //protected abstract void performAppSessionInvalidation();

} // end of class
