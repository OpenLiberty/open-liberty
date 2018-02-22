/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.cache;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.common.BackedHashMap;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStore;

/**
 * Hash map backed by JCache.
 * TODO most methods are not implemented
 */
public class CacheHashMap extends BackedHashMap {
    private static final long serialVersionUID = 1L; // not serializable, rejects writeObject

    private static final TraceComponent tc = Tr.register(CacheHashMap.class);

    /**
     * Reusable patterns for String.replaceAll
     */
    private static final Pattern COLON = Pattern.compile(":"), PERCENT = Pattern.compile("%"), SLASH = Pattern.compile("/");

    /**
     * Key in the session info cache that is reserved for coordinating invalidation
     */
    static final String INVAL_KEY = ".inval";

    // this is set to true for multirow in DatabaseHashMapMR if additional conditions are satisfied
    boolean appDataTablesPerThread = false;

    CacheStoreService cacheStoreService;
    IStore _iStore;
    SessionManagerConfig _smc;

    final String encodedAppRoot;

    /**
     * For single-cache path, the whole session is store as an entry in this cache.
     * For multi-cache path, separate caches are created per application to store the session properties each as their own cache entry,
     * and this cache only contains information about the session rather than its contents.
     */
    @SuppressWarnings("rawtypes")
    Cache<String, ArrayList> sessionInfoCache;

    /**
     * Per-application session property cache.
     * This cache is used on the entry-per-session property path.
     * Null if all session properties are stored in a single entry of the com.ibm.ws.session.cache cache.
     */
    Cache<String, byte[]> sessionPropertyCache; // Because byte[] does instance-based .equals, it will not be possible to use Cache.replace operations, but we are okay with that.

    @FFDCIgnore(CacheException.class)
    public CacheHashMap(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc);
        this.cacheStoreService = cacheStoreService;
        this._iStore = store;
        this._smc = smc;

        // Build a unique per-application cache name by starting with the application context root and percent encoding
        // the / and : characters (JCache spec does not allow these in cache names)
        // and also the % character (which is necessary because of percent encoding)
        String a = PERCENT.matcher(store.getId()).replaceAll("%25"); // must be done first to avoid replacing % that is added when replacing the others
        a = SLASH.matcher(a).replaceAll("%2F");
        a = COLON.matcher(a).replaceAll("%3A");
        encodedAppRoot = a;

        // Use different cache names depending on whether session property values are all stored in a single entry
        // within this main cache vs as separate entries in other caches.
        // The use of different cache names prevents servers from colliding on the same cache when they have different
        // options selected for how to store session properties.
        String prefix = "true".equals(cacheStoreService.configurationProperties.get("useMultiRowSchema")) // TODO remove single entry cache and codepath
                                        ? "com.ibm.ws.session.info."   // no session property values are kept in this cache
                                        : "com.ibm.ws.session.cache."; // all session property values are stored in a single entry per session
        String cacheName = new StringBuilder(prefix.length() + encodedAppRoot.length()).append(prefix).append(encodedAppRoot).toString();

        sessionInfoCache = cacheStoreService.cacheManager.getCache(cacheName, String.class, ArrayList.class);
        if (sessionInfoCache == null) {
            @SuppressWarnings("rawtypes")
            Configuration<String, ArrayList> config = new MutableConfiguration<String, ArrayList>()
                            .setTypes(String.class, ArrayList.class)
                            .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
            try {
                sessionInfoCache = cacheStoreService.cacheManager.createCache(cacheName, config);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Created a new session info cache", sessionInfoCache);
            } catch (CacheException x) {
                sessionInfoCache = cacheStoreService.cacheManager.getCache(cacheName, String.class, ArrayList.class);
                if (sessionInfoCache == null)
                    throw x;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Using session info cache", sessionInfoCache);
    }

    /**
     * Create a key for a session, of the form: SessionId@Application
     * 
     * @param sessionId the session id
     * @param app the application
     * @return the key
     */
    @Trivial
    static final String createSessionKey(String sessionId, String app) {
        String key = new StringBuilder(sessionId.length() + 1 + app.length())
                        .append(sessionId)
                        .append('@')
                        .append(app)
                        .toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "session key: " + key);
        return key;
    }

    /**
     * Create a key for a session property, of the form: SessionId.PropertyId
     * 
     * @param sessionId the session id
     * @param propertyId the session property
     * @return the key
     */
    @Trivial
    static final String createSessionPropertyKey(String sessionId, String propertyId) {
        String key = new StringBuilder(sessionId.length() + 1 + propertyId.length())
                        .append(sessionId)
                        .append('.')
                        .append(propertyId)
                        .toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "session property key: " + key);
        return key;
    }

    /**
     * Copied from DatabaseHashMap.doInvalidations.
     * this method removes timed out sessions that do not require listener processing
     */
    private void doInvalidations() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        try {
            long now = System.currentTimeMillis();

            // loop through all the candidates eligible for invalidation
            BackedSession pmiStatSession = null; // fake session for pmi stats
            SessionStatistics pmiStats = null;
            for (@SuppressWarnings("rawtypes") Iterator<Cache.Entry<String, ArrayList>> it = sessionInfoCache.iterator(); it.hasNext(); ) {
                @SuppressWarnings("rawtypes")
                Cache.Entry<String, ArrayList> entry = it.next();
                String id = entry == null ? null : entry.getKey();
                if (id != null && !INVAL_KEY.equals(id)) {
                    SessionData sessionData = new SessionData(entry.getValue());
                    long lastAccess = sessionData.getLastAccess();
                    short listenerCnt = sessionData.getListenerCount();
                    int maxInactive = sessionData.getMaxInactiveTime();
                    if ((listenerCnt == 0 || listenerCnt == 2)
                                    && maxInactive >= 0
                                    && maxInactive < (now - lastAccess) / 1000) {
                        if (now + _smc.getInvalidationCheckInterval() * 1000 <= System.currentTimeMillis()) {
                            // If the scan is taking more than pollInterval, just break and
                            // invalidate the sessions so far determined
                            break;
                        }

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "attempt to delete " + id);

                        if (sessionInfoCache.remove(id, entry.getValue())) {
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "deleted " + id);

                            //delete sub rows
                            if (_smc.isUsingMultirow()) {
                                SessionInfo sessionInfo = new SessionInfo(entry.getValue());
                                Set<String> propIds = sessionInfo.getSessionPropertyIds();
                                if (propIds != null && !propIds.isEmpty()) {
                                    HashSet<String> propKeys = new HashSet<String>();
                                    for (String propId : propIds)
                                        propKeys.add(createSessionPropertyKey(id, propId));

                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "deleting", propKeys);

                                    sessionPropertyCache.removeAll(propKeys);
                                }
                            }
                        }

                        /*
                         * We already did cleanUpCache during the invalidation processing. Therefore, the session is no longer
                         * in our cache, and superRemove should return null. We need to create a temporary Session with an
                         * accurate Id and creationTime to send to the PMI counters.
                         */
                        superRemove(id);
                        long createTime = sessionData.getCreationTime();
                        //we don't want to retrieve session, so use a fake one for pmi
                        if (pmiStatSession == null)
                            pmiStatSession = new CacheSession();
                        if (pmiStats == null)
                            pmiStats = _iStore.getSessionStatistics();
                        pmiStatSession.setId(id);
                        pmiStatSession.setCreationTime(createTime);
                        if (pmiStats != null) {
                            pmiStats.sessionDestroyed(pmiStatSession);
                            pmiStats.sessionDestroyedByTimeout(pmiStatSession);
                        }
                    }
                }
            }
        } catch (Exception x) {
            // auto FFDC
        }
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#getAppDataTablesPerThread()
     */
    @Override
    public boolean getAppDataTablesPerThread() {
        return appDataTablesPerThread;
    }

    /**
     * TODO rewrite this. For now, it is copied based on DatabaseHashMap.getValue
     */
    protected Object getValue(String id, BackedSession s) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Object tmp = null;

        if (!s.getId().equals(id))
            throw new IllegalArgumentException(id + " != " + s.getId()); // internal error

        ArrayList<?> value = sessionInfoCache.get(id);

        if (value == null)
            return null;

        SessionData sessionData = new SessionData(value);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, id, sessionData);

        long startTime = System.nanoTime();
        byte[] bytes = sessionData.getBytes();
        if (bytes != null && bytes.length > 0) {
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes));
            try {
                try {
                    tmp = ((CacheStore) getIStore()).getLoader().loadObject(in);
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException x) {
                FFDCFilter.processException(x, getClass().getName(), "96", s);
                throw new RuntimeException(x);
            }
        }

        SessionStatistics pmiStats = getIStore().getSessionStatistics();
        if (pmiStats != null) {
            pmiStats.readTimes(bytes == null ? 0 : bytes.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
        }

        return tmp;
    }

    /**
     * Override in subclass for multi-row.
     */
    boolean handlePropertyHits(BackedSession d2) {
        return true;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#insertSession(com.ibm.ws.session.store.common.BackedSession)
     */
    @Override
    protected void insertSession(BackedSession d2) {
        // TODO rewrite this. For now, it is copied based on DatabaseHashMap.insertSession
        String id = d2.getId();

        listenerFlagUpdate(d2);

        long tmpCreationTime = d2.getCreationTime();
        d2.setLastWriteLastAccessTime(tmpCreationTime);

        SessionData sessionData = new SessionData(tmpCreationTime, d2.getMaxInactiveInterval(), d2.listenerFlag, d2.getUserName());

        if (!sessionInfoCache.putIfAbsent(id, sessionData.getArrayList()))
            throw new IllegalStateException("Cache already contains " + id);

        d2.needToInsert = false;

        removeFromRecentlyInvalidatedList(id);

        d2.update = null;
        d2.userWriteHit = false;
        d2.maxInactWriteHit = false;
        d2.listenCntHit = false;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#isPresent(java.lang.String)
     */
    @Override
    protected boolean isPresent(String id) {
        return sessionInfoCache.containsKey(id);
    }

    /**
     * Copied from DatabaseHashMap.
     * For multirow db, attempts to get the requested attr from the db
     * Returns null if attr doesn't exist or we're not running multirow
     * We consider populatedAppData as well
     * populatedAppData is true when session is new or when the entire session is read into memory
     * in those cases, we don't want to go to the backend to retrieve the attribute
     * 
     * @see com.ibm.ws.session.store.common.BackedHashMap#loadOneValue(java.lang.String, com.ibm.ws.session.store.common.BackedSession)
     */
    @Override
    protected Object loadOneValue(String attrName, BackedSession sess) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Object value = null;
        if (_smc.isUsingMultirow() && !((CacheSession) sess).getPopulatedAppData()) {
            String id = sess.getId();
            String appName = getIStore().getId();

            String key = createSessionPropertyKey(id, attrName);
            byte[] bytes = sessionPropertyCache.get(key);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "byte length", bytes == null ? null : bytes.length);

            if (bytes != null && bytes.length > 0) {
                long startTime = System.nanoTime();

                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes));
                try {
                    try {
                        value = ((CacheStore) getIStore()).getLoader().loadObject(in);
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException x) {
                    FFDCFilter.processException(x, getClass().getName(), "197", sess);
                    throw new RuntimeException(x);
                }

                SessionStatistics pmiStats = getIStore().getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.readTimes(bytes == null ? 0 : bytes.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                }
            }

            // Before returning the value, confirm that the session hasn't expired
            if (!sessionInfoCache.containsKey(id)) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, id + " does not appear to be a valid session for " + appName);
                value = null;
                throw new UnsupportedOperationException(); // TODO implement code path where cache entry for session is expired. Delete the property entries?
            }
        }
        return value;
    }

    /**
     * Attempts to update the last access time ensuring the old value matches.
     * This verifies that the copy we have in cache is still valid.
     *
     * @see com.ibm.ws.session.store.common.BackedHashMap#overQualLastAccessTimeUpdate(com.ibm.ws.session.store.common.BackedSession, long)
     */
    @Override
    protected int overQualLastAccessTimeUpdate(BackedSession sess, long nowTime) {
        String id = sess.getId();

        int updateCount;

        ArrayList<?> oldValue = sessionInfoCache.get(id);
        SessionData sessionData = oldValue == null ? null : new SessionData(oldValue).clone();
        synchronized (sess) {
            if (sessionData == null || sessionData.getLastAccess() != sess.getCurrentAccessTime() || sessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionData.setLastAccess(nowTime);
                ArrayList<?> newValue = sessionData.getArrayList();

                if (sessionInfoCache.replace(id, oldValue, newValue)) {
                    sess.updateLastAccessTime(nowTime);
                    updateCount = 1;
                } else {
                    updateCount = 0;
                }
            }
        }

        return updateCount;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#performInvalidation()
     */
    @Override
    protected void performInvalidation() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        long now = System.currentTimeMillis();

        boolean doInvals = false;
        boolean doCacheInval = doScheduledInvalidation();

        try {
            // handle last acc times for manual update regardless
            // of whether this thread will scan for time outs
            if (!_smc.getEnableEOSWrite()) {
                writeCachedLastAccessedTimes();
            }

            if (doCacheInval) {
                ArrayList<?> oldValue = sessionInfoCache.get(INVAL_KEY);
                if (oldValue == null) {
                    // If we are here, it means this is the first time this web module is
                    // trying to perform invalidation of sessions
                    SessionData sessionData = new SessionData(now, // last access
                                                              -1, // max inactive time,
                                                              (short) 0, // listener count 
                                                              null); // user name
                    ArrayList<Object> newValue = sessionData.getArrayList();
                    sessionInfoCache.put(INVAL_KEY, newValue);
                    doInvals = true;
                } else {
                    SessionData sessionData = new SessionData(oldValue);
                    long lastTime = sessionData.getLastAccess();

                    long lastCheck = now - _smc.getInvalidationCheckInterval() * 1000;

                    //check the value of lastCheck,lastTime,now
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "lastCheck/lastTime/now", lastCheck, lastTime, now);

                    //if no other server tried to process invalidation within the interval, this will be true
                    //similar to updateNukerTimeStamp, but we have an extra check here to test the last access time hasn't changed
                    if (lastCheck >= lastTime || lastTime > now) {
                        sessionData = sessionData.clone();
                        sessionData.setLastAccess(now);
                        ArrayList<Object> newValue = sessionData.getArrayList();
                        doInvals = sessionInfoCache.replace(INVAL_KEY, oldValue, newValue);
                    }
                }

                if (doInvals) {
                    //Process the non-listener sessions first
                    doInvalidations();

                    //Read in all the sessions with listeners that need to be invalidated
                    processInvalidListeners();
                }
            }
        } catch (Throwable t) {
            // auto FFDC
        }
    }

    /**
     * // TODO rewrite this. For now, it is copied based on DatabaseHashMap.insertSession
     *
     * @see com.ibm.ws.session.store.common.BackedHashMap#persistSession(com.ibm.ws.session.store.common.BackedSession, boolean)
     */
    @Override
    protected boolean persistSession(BackedSession d2, boolean propHit) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = d2.getId();

        try {
            // if nothing changed, then just return
            if (!d2.userWriteHit && !d2.maxInactWriteHit && !d2.listenCntHit && _smc.getEnableEOSWrite() && !_smc.getScheduledInvalidation() && !propHit) {
                d2.update = null;
                d2.userWriteHit = false;
                d2.maxInactWriteHit = false;
                d2.listenCntHit = false;
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "no changes");
                return true;
            }

            byte[] objbuf = null;
            if (propHit) {
                if (_smc.isUsingMultirow()) {
                    if (!handlePropertyHits(d2)) {
                        return false;
                    }
                } else {
                    objbuf = serializeAppData(d2);
                }
            }

            long startTimeNS = System.nanoTime();

            for (boolean updated = false; !updated;) {
                ArrayList<?> oldValue = sessionInfoCache.get(id);
                if (oldValue == null)
                    return false;

                SessionData sessionData = new SessionData(oldValue).clone();

                if (d2.userWriteHit) {
                    d2.userWriteHit = false;
                    sessionData.setUser(d2.getUserName());
                }

                if (d2.maxInactWriteHit) {
                    d2.maxInactWriteHit = false;
                    sessionData.setMaxInactiveTime(d2.getMaxInactiveInterval());
                }

                if (d2.listenCntHit) {
                    d2.listenCntHit = false;
                    sessionData.setListenerCount(d2.listenerFlag);
                }

                long time = d2.getCurrentAccessTime();
                if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                    d2.setLastWriteLastAccessTime(time);
                    sessionData.setLastAccess(time);
                }

                if (propHit && !_smc.isUsingMultirow()) {
                    sessionData.setBytes(objbuf);
                }

                if (trace & tc.isDebugEnabled())
                    Tr.debug(this, tc, id, sessionData);

                ArrayList<?> newValue = sessionData.getArrayList();
                updated = sessionInfoCache.replace(id, oldValue, newValue);
            }

            if (objbuf != null && propHit && !_smc.isUsingMultirow()) {
                SessionStatistics pmiStats = _iStore.getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.writeTimes(objbuf.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNS));
                }
            }
        } catch (Exception ee) {
            FFDCFilter.processException(ee, getClass().getName(), "272", d2);
            return false;
        }
        return true;
    }

    /**
     * Copied from DatabaseHashMap.pollForInvalidSessionsWithListeners and DatabaseHashMap.processInvalidListeners.
     * This method determines the set of sessions with session listeners which
     * need to be invalidated and processes them.
     */
    private void processInvalidListeners() {
        final boolean trace = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();

        String appName = getIStore().getId();

        long start = System.currentTimeMillis();

        for (@SuppressWarnings("rawtypes") Iterator<Cache.Entry<String, ArrayList>> it = sessionInfoCache.iterator(); it.hasNext(); ) {
            @SuppressWarnings("rawtypes")
            Cache.Entry<String, ArrayList> entry = it.next();
            String id = entry == null ? null : entry.getKey();
            if (id != null && !INVAL_KEY.equals(id)) {
                SessionData sessionData = new SessionData(entry.getValue());
                long lastAccess = sessionData.getLastAccess();
                short listenerCnt = sessionData.getListenerCount();
                int maxInactive = sessionData.getMaxInactiveTime();
                if ((listenerCnt == 0 || listenerCnt == 2)
                                && maxInactive >= 0
                                && maxInactive < (start - lastAccess) / 1000) {

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "processing " + id);

                    CacheSession s = new CacheSession(this, id, _iStore.getStoreCallback());
                    s.initSession(_iStore);
                    s.setIsValid(true);
                    s.setIsNew(false);
                    s.updateLastAccessTime(lastAccess);
                    s.setCreationTime(sessionData.getCreationTime());
                    s.internalSetMaxInactive(maxInactive);
                    s.internalSetUser(sessionData.getUser());
                    s.setListenerFlag(listenerCnt);

                    long now = System.currentTimeMillis();
                    //handle the subset of session listeners

                    lastAccess = s.getCurrentAccessTime(); // try using lastTouch again..

                    try {
                        // get the session ready and read in any listeners
                        s.setIsNew(false);
                        s.getSwappableListeners(BackedSession.HTTP_SESSION_BINDING_LISTENER);

                        sessionData = sessionData.clone();
                        sessionData.setLastAccess(lastAccess);

                        // only invalidate those which have not been accessed since
                        // check in computeInvalidList
                        if (sessionInfoCache.remove(id, sessionData.getArrayList())) {
                            // return of session done as a result of this call
                            s.internalInvalidate(true);

                            if (_smc.isUsingMultirow()) {
                                SessionInfo sessionInfo = new SessionInfo(entry.getValue());
                                Set<String> propIds = sessionInfo.getSessionPropertyIds();
                                if (propIds != null && !propIds.isEmpty()) {
                                    HashSet<String> propKeys = new HashSet<String>();
                                    for (String propId : propIds)
                                        propKeys.add(createSessionPropertyKey(id, propId));

                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "deleting", propKeys);

                                    sessionPropertyCache.removeAll(propKeys);
                                }
                            }
                        }

                        /*
                         * we don't want to update this on every invalidation with a listener that is processed.
                         * We'll only update this if we're getting close.
                         *
                         * Processing Invalidation Listeners could take a long time. We should update the
                         * NukerTimeStamp so that another server in this cluster doesn't kick off invalidation
                         * while we are still processing. We only want to update the time stamp if we are getting
                         * close to the time when it will expire. Therefore, we are going to do it after we're 1/2 way there.
                         */
                        if ((now + _smc.getInvalidationCheckInterval() * (1000 / 2)) < System.currentTimeMillis()) {
                            updateNukerTimeStamp(appName);
                            now = System.currentTimeMillis();
                        }
                    } catch (Exception e) {
                        FFDCFilter.processException(e, getClass().getName(), "652", s);
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#readFromExternal(java.lang.String)
     */
    @Override
    protected BackedSession readFromExternal(String id) {
        CacheSession sess = null;
        ArrayList<?> value = sessionInfoCache.get(id);
        if (value != null) {
            SessionData sessionData = new SessionData(value);
            sess = new CacheSession(this, id, getIStore().getStoreCallback());
            sess.updateLastAccessTime((Long) sessionData.getLastAccess());
            sess.setCreationTime((Long) sessionData.getCreationTime());
            sess.internalSetMaxInactive((Integer) sessionData.getMaxInactiveTime());
            sess.internalSetUser((String) sessionData.getUser());
            sess.setIsValid(true);
            sess.setListenerFlag((Short) sessionData.getListenerCount());
        }
        return sess;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        //If the app calls invalidate, it may not be removed from the local cache yet.
        superRemove(id);

        sessionInfoCache.remove(id);

        addToRecentlyInvalidatedList(id);
    }

    /**
     * serializeAppData - returns a byte array form of the swappableData
     */
    private byte[] serializeAppData(BackedSession d2) throws Exception {
        // return either the byte array input stream for the app
        // data or a vector of byte array input streams if their is a
        // row for each piece of app data
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        byte[] objbuf = null;

        Map<Object, Object> ht;
        synchronized (d2) {
            ht = d2.getSwappableData();
        }

        // serialize session (app data only) into byte array buffer
        baos = new ByteArrayOutputStream();
        oos = cacheStoreService.serializationService.createObjectOutputStream(baos);
        oos.writeObject(ht);
        oos.flush();
        objbuf = baos.toByteArray();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,  "success - size of byte array is " + objbuf.length);
        }

        oos.close();
        baos.close();

        return objbuf;
    }

    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                        .append('@').append(Integer.toHexString(System.identityHashCode(this)))
                        .append(" for ").append(_iStore.getId())
                        .toString();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#updateLastAccessTime(com.ibm.ws.session.store.common.BackedSession, long)
     */
    @Override
    protected int updateLastAccessTime(BackedSession sess, long nowTime) {
        String id = sess.getId();

        int updateCount = -1;

        while (updateCount == -1) {
            ArrayList<?> oldValue = sessionInfoCache.get(id);
            SessionData sessionData = oldValue == null ? null : new SessionData(oldValue).clone();
            if (sessionData == null || sessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionData.setLastAccess(nowTime);
                ArrayList<Object> newValue = sessionData.getArrayList();
                if (sessionInfoCache.replace(id, oldValue, newValue))
                    updateCount = 1;
            }
        }

        return updateCount;
    }

    /**
     * Copied from DatabaseHashMap.updateNukerTimeStamp.
     * When running in a clustered environment, there could be multiple machines processing invalidation.
     * This method updates the last time the invalidation was run. A server should not try to process invalidation if
     * it was already done within the specified time interval for that app.
     */
    private void updateNukerTimeStamp(String appName) {
        long now = System.currentTimeMillis();
        SessionData sessionData = new SessionData(now, // last access
                                                  -1, // max inactive time,
                                                  (short) 0, // listener count 
                                                  null); // user name
        ArrayList<Object> newValue = sessionData.getArrayList();
        sessionInfoCache.put(INVAL_KEY, newValue);
    }

    /**
     * Copied from DatabaseHashMap.writeCachedLastAccessedTimes.
     * writeCachedLastAccessedTimes - if we have manual writes of time-based writes, we cache the last
     * accessed times and only write them to the persistent store prior to the inval thread running.
     */
    void writeCachedLastAccessedTimes() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // create a copy for the updates, then atomically clear the table
        // the hashtable table clone is shallow, it will not dup the keys/elements
        @SuppressWarnings("unchecked")
        Hashtable<String, Object> updTab = (Hashtable<String, Object>) cachedLastAccessedTimes.clone();
        cachedLastAccessedTimes.clear();
        Enumeration<String> updEnum = updTab.keys();

        while (updEnum.hasMoreElements()) {
            String id = (String) updEnum.nextElement();
            Long timeObj = (Long) updTab.get(id);
            long time = timeObj.longValue();
            try {
                for (int updateCount = -1; updateCount == -1; ) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Updating LastAccess for " + id);

                    ArrayList<?> oldValue = sessionInfoCache.get(id);
                    SessionData sessionData = oldValue == null ? null : new SessionData(oldValue).clone();
                    if (sessionData == null || sessionData.getLastAccess() >= time) {
                        updateCount = 0;
                    } else {
                        sessionData.setLastAccess(time);
                        ArrayList<Object> newValue = sessionData.getArrayList();
                        if (sessionInfoCache.replace(id, oldValue, newValue))
                            updateCount = 1;
                    }
                }
            } catch (Exception x) {
                FFDCFilter.processException(x, getClass().getName(), "649", id);
                throw x;
            }
        }
    }

    /**
     * This type of hash map is not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
