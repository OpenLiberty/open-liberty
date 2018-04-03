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
import java.util.Arrays;
import java.util.Collection;
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
 * A CacheHashMap exists per application that uses HTTP sessions.
 * It is built upon 2 caches, both of which are specific to the application.
 * <ul>
 *  <li>com.ibm.ws.session.info.{PERCENT_ENCODED_APP_CONTEXT_ROOT}
 *   <br>Keys are session ids. Except for one additional key, {INVAL_KEY}, which is used to track the timestamp of invalidation
 *   <br>Value is an ArrayList, containing information about the session, but not any session attribute values. Its content is represented by SessionInfo.
 *  </li>
 *  <li>com.ibm.ws.session.prop.{PERCENT_ENCODED_APP_CONTEXT_ROOT}
 *   <br>Keys are {SESSION_ID}.{SESSION_ATTRIBUTE_NAME}
 *   <br>Value is a byte[] which is the session attribute value serialized to bytes.
 *  </li>
 * </ul>
 */
public class CacheHashMap extends BackedHashMap {
    private static final long serialVersionUID = 1L; // not serializable, rejects writeObject

    private static final TraceComponent tc = Tr.register(CacheHashMap.class);

    /**
     * Reusable patterns for String.replaceAll
     */
    private static final Pattern COLON = Pattern.compile(":"), PERCENT = Pattern.compile("%"), SLASH = Pattern.compile("/");

    /**
     * The end-of-line marker.
     */
    private static final String EOLN = String.format("%n");

    /**
     * Key in the session info cache that is reserved for coordinating invalidation
     */
    private static final String INVAL_KEY = ".inval";

    // this is set to true for multirow if additional conditions are satisfied
    private final boolean appDataTablesPerThread;

    final CacheStoreService cacheStoreService;
    private final IStore _iStore;
    private final SessionManagerConfig _smc;

    /**
     * Per-application session attribute cache.
     */
    private Cache<String, byte[]> sessionAttributeCache; // Because byte[] does instance-based .equals, it will not be possible to use Cache.replace operations, but we are okay with that.

    /**
     * Per-application cache that contains meta information about the session but not the session attribute values.
     */
    @SuppressWarnings("rawtypes")
    private Cache<String, ArrayList> sessionMetaCache;

    /**
     * Trace identifier for the session attribute cache
     */
    private String tcSessionAttrCache;

    /**
     * Trace identifier for the session meta information cache
     */
    private String tcSessionMetaCache;

    @FFDCIgnore(CacheException.class)
    CacheHashMap(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc);

        final boolean trace = TraceComponent.isAnyTracingEnabled();

        this.cacheStoreService = cacheStoreService;
        this._iStore = store;
        this._smc = smc;

        // We know we're running multi-row..if not writeAllProperties and not time-based writes,
        // we must keep the app data tables per thread (rather than per session)
        appDataTablesPerThread = (!_smc.writeAllProperties() && !_smc.getEnableTimeBasedWrite());

        // Build a unique per-application cache name by starting with the application context root and percent encoding
        // the / and : characters (JCache spec does not allow these in cache names)
        // and also the % character (which is necessary because of percent encoding)
        String a = PERCENT.matcher(store.getId()).replaceAll("%25"); // must be done first to avoid replacing % that is added when replacing the others
        a = SLASH.matcher(a).replaceAll("%2F");
        a = COLON.matcher(a).replaceAll("%3A");

        // Session Meta Information Cache

        String metaCacheName = new StringBuilder(24 + a.length()).append("com.ibm.ws.session.meta.").append(a).toString();

        if (trace && tc.isDebugEnabled())
            tcInvoke(cacheStoreService.tcCacheManager, "getCache", metaCacheName, "String", "ArrayList");

        sessionMetaCache = cacheStoreService.cacheManager.getCache(metaCacheName, String.class, ArrayList.class);
        boolean create;
        if (create = sessionMetaCache == null) {
            if (trace && tc.isDebugEnabled())
                tcReturn(cacheStoreService.tcCacheManager, "getCache", "null");

            @SuppressWarnings("rawtypes")
            MutableConfiguration<String, ArrayList> config = new MutableConfiguration<String, ArrayList>()
                            .setTypes(String.class, ArrayList.class)
                            .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
            if (cacheStoreService.supportsStoreByReference)
                config = config.setStoreByValue(false);
            try {
                if (trace && tc.isDebugEnabled())
                    tcInvoke(cacheStoreService.tcCacheManager, "createCache", metaCacheName, config);

                sessionMetaCache = cacheStoreService.cacheManager.createCache(metaCacheName, config);
            } catch (CacheException x) {
                create = false;
                if (trace && tc.isDebugEnabled()) {
                    tcReturn(cacheStoreService.tcCacheManager, "createCache", x);
                    tcInvoke(cacheStoreService.tcCacheManager, "getCache", metaCacheName, "String", "ArrayList");
                }
                sessionMetaCache = cacheStoreService.cacheManager.getCache(metaCacheName, String.class, ArrayList.class);
                if (sessionMetaCache == null)
                    throw x;
            }
        }

        tcSessionMetaCache = "MetaCache" + Integer.toHexString(System.identityHashCode(sessionMetaCache));
        if (trace && tc.isDebugEnabled())
            tcReturn(cacheStoreService.tcCacheManager, create ? "createCache" : "getCache", tcSessionMetaCache, sessionMetaCache);

        // Session Attributes Cache

        String attrCacheName = new StringBuilder(24 + a.length()).append("com.ibm.ws.session.attr.").append(a).toString();

        if (trace && tc.isDebugEnabled())
            tcInvoke(cacheStoreService.tcCacheManager, "getCache", attrCacheName, "String", "byte[]");

        sessionAttributeCache = cacheStoreService.cacheManager.getCache(attrCacheName, String.class, byte[].class);

        if (create = sessionAttributeCache == null) {
            if (trace && tc.isDebugEnabled())
                tcReturn(cacheStoreService.tcCacheManager, "getCache", "null");

            MutableConfiguration<String, byte[]> config = new MutableConfiguration<String, byte[]>()
                            .setTypes(String.class, byte[].class)
                            .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
            if (cacheStoreService.supportsStoreByReference)
                config = config.setStoreByValue(false);
            try {
                if (trace && tc.isDebugEnabled())
                    tcInvoke(cacheStoreService.tcCacheManager, "createCache", attrCacheName, config);

                sessionAttributeCache = cacheStoreService.cacheManager.createCache(attrCacheName, config);
            } catch (CacheException x) {
                create = false;
                if (trace && tc.isDebugEnabled()) {
                    tcReturn(cacheStoreService.tcCacheManager, "createCache", x);
                    tcInvoke(cacheStoreService.tcCacheManager, "getCache", attrCacheName, "String", "byte[]");
                }
                sessionAttributeCache = cacheStoreService.cacheManager.getCache(attrCacheName, String.class, byte[].class);
                if (sessionAttributeCache == null)
                    throw x;
            }
        }

        tcSessionAttrCache = "AttrCache" + Integer.toHexString(System.identityHashCode(sessionAttributeCache));
        if (trace && tc.isDebugEnabled())
            tcReturn(cacheStoreService.tcCacheManager, create ? "createCache" : "getCache", tcSessionAttrCache, sessionAttributeCache);
    }

    /**
     * Create a key for a session attribute, of the form: SessionId.AttributeId
     * 
     * @param sessionId the session id
     * @param attributeId the session attribute
     * @return the key
     */
    @Trivial
    private static final String createSessionAttributeKey(String sessionId, String attributeId) {
        return new StringBuilder(sessionId.length() + 1 + attributeId.length())
                        .append(sessionId)
                        .append('.')
                        .append(attributeId)
                        .toString();
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

            if (trace && tc.isDebugEnabled())
                tcInvoke(tcSessionMetaCache, "iterator");

            @SuppressWarnings("rawtypes")
            Iterator<Cache.Entry<String, ArrayList>> it = sessionMetaCache.iterator();

            if (trace && tc.isDebugEnabled())
                tcReturn(tcSessionMetaCache, "iterator", it);

            while (it.hasNext()) {
                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "_iterator.next");

                @SuppressWarnings("rawtypes")
                Cache.Entry<String, ArrayList> entry = it.next();
                String id = entry == null ? null : entry.getKey();
                ArrayList<?> value = entry.getValue();

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "_iterator.next", id, value);

                if (id != null && !INVAL_KEY.equals(id)) {
                    SessionInfo sessionInfo = new SessionInfo(value);
                    long lastAccessTime = sessionInfo.getLastAccess();
                    short listenerTypes = sessionInfo.getListenerTypes();
                    int maxInactiveTime = sessionInfo.getMaxInactiveTime();
                    if ((listenerTypes & BackedSession.HTTP_SESSION_BINDING_LISTENER) == 0 // sessions that do NOT have binding listeners
                                    && maxInactiveTime >= 0
                                    && maxInactiveTime < (now - lastAccessTime) / 1000) {
                        if (now + _smc.getInvalidationCheckInterval() * 1000 <= System.currentTimeMillis()) {
                            // If the scan is taking more than pollInterval, just break and
                            // invalidate the sessions so far determined
                            break;
                        }

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionMetaCache, "remove", id, value);

                        boolean removed = sessionMetaCache.remove(id, value);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionMetaCache, "remove", removed);

                        if (removed) {
                            //delete sub rows
                            Set<String> propIds = sessionInfo.getSessionPropertyIds();
                            if (propIds != null && !propIds.isEmpty()) {
                                HashSet<String> propKeys = new HashSet<String>();
                                for (String propId : propIds)
                                    propKeys.add(createSessionAttributeKey(id, propId));

                                if (trace && tc.isDebugEnabled())
                                    tcInvoke(tcSessionAttrCache, "removeAll", propKeys);

                                sessionAttributeCache.removeAll(propKeys);

                                if (trace && tc.isDebugEnabled())
                                    tcReturn(tcSessionAttrCache, "removeAll");
                            }
                        }

                        /*
                         * We already did cleanUpCache during the invalidation processing. Therefore, the session is no longer
                         * in our cache, and superRemove should return null. We need to create a temporary Session with an
                         * accurate Id and creationTime to send to the PMI counters.
                         */
                        superRemove(id);
                        long createTime = sessionInfo.getCreationTime();
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
     * Loads all the session attributes.
     * Copied from DatabaseHashMapMR.
     */
    Object getAllValues(BackedSession sess) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = sess.getId();

        long startTime = System.nanoTime();
        long readSize = 0;

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "get", id);

        ArrayList<?> list = sessionMetaCache.get(id);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "get", list);

        Set<String> propIds = list == null ? null : new SessionInfo(list).getSessionPropertyIds();

        Hashtable<String, Object> h = new Hashtable<String, Object>();
        try {
            if (propIds != null) {
                for (String propId : propIds) {
                    // If an attribute is already in appDataRemovals or appDataChanges, then the attribute was already retrieved from the cache.  Skip retrieval from the cache here.
                    if (sess.appDataRemovals != null && sess.appDataRemovals.containsKey(propId)) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Found property " + propId + " in appDataRemovals, skipping query for this prop");
                        continue;
                    } else if (sess.appDataChanges != null && sess.appDataChanges.containsKey(propId)) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Found property " + propId + " in appDataChanges, skipping query for this prop");
                        continue;
                    }

                    String attributeKey = createSessionAttributeKey(id, propId);

                    if (trace && tc.isDebugEnabled())
                        tcInvoke(tcSessionAttrCache, "get", attributeKey);

                    byte[] b = sessionAttributeCache.get(attributeKey);

                    if (b == null) {
                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionAttrCache, "get", "null");
                    } else {
                        ByteArrayInputStream bais = new ByteArrayInputStream(b);
                        BufferedInputStream bis = new BufferedInputStream(bais);
                        Object obj;
                        try {
                            obj = ((CacheStore) getIStore()).getLoader().loadObject(bis);
                            readSize += b.length;

                            if (trace && tc.isDebugEnabled())
                                tcReturn(tcSessionAttrCache, "get", b, obj);
                        } catch (ClassNotFoundException x) {
                            if (trace && tc.isDebugEnabled())
                                tcReturn(tcSessionAttrCache, "get", b);

                            FFDCFilter.processException(x, getClass().getName(), "91", sess, new Object[] { Arrays.toString(b) });
                            throw new RuntimeException(x);
                        }

                        bis.close();
                        bais.close();

                        if (obj != null) {
                            h.put(propId, obj);
                        }
                    }
                }
            }

            SessionStatistics pmiStats = _iStore.getSessionStatistics();
            if (pmiStats != null) {
                pmiStats.readTimes(readSize, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            }
        } catch (IOException e) {
            FFDCFilter.processException(e, getClass().getName(), "319", sess);
            throw new RuntimeException(e);
        }
        return h;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#getAppDataTablesPerThread()
     */
    @Override
    public boolean getAppDataTablesPerThread() {
        return appDataTablesPerThread;
    }

    /**
     * copied from DatabaseHashMapMR
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean handlePropertyHits(BackedSession session) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Thread t = Thread.currentThread();

        String id = session.getId();

        try {
            Set<String> propsToWrite = null;

            // we are not synchronized here - were not in old code either
            Hashtable tht = null;
            if (_smc.writeAllProperties()) {
                Map<?, ?> ht = session.getSwappableData();
                propsToWrite = (Set<String>) ht.keySet();
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "doing app changes for ALL mSwappable Data", ht);
                }
            } else {
                if (session.appDataChanges != null) {
                    if (appDataTablesPerThread) {
                        if ((tht = (Hashtable) session.appDataChanges.get(t)) != null) {
                            if (trace && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "doing app changes for " + id + " on thread " + t);
                            }
                            propsToWrite = tht.keySet();
                        }
                    } else { // appDataTablesPerSession
                        propsToWrite = session.appDataChanges.keySet();
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "doing app changes for TimeBasedWrite");
                        }
                    }
                }
            }

            if (propsToWrite != null) {
                for (String propid : propsToWrite) {
                    long startTime = System.nanoTime();

                    if (id.equals(propid)) {
                        throw new IllegalArgumentException(propid); // internal error, should never occur
                    }

                    String key = createSessionAttributeKey(id, propid);
                    Object value = session.getSwappableData().get(propid);

                    if (value == null) {
                        // Value removed by another thread
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "ignoring " + propid + " because it is no longer found");
                    } else {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        ObjectOutputStream oos = cacheStoreService.serializationService.createObjectOutputStream(baos);
                        oos.writeObject(value);
                        oos.flush();

                        byte[] objbuf = baos.toByteArray();

                        oos.close();
                        baos.close();

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionAttrCache, "put", key, objbuf, value);

                        sessionAttributeCache.put(key, objbuf);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionAttrCache, "put");

                        SessionStatistics pmiStats = _iStore.getSessionStatistics();
                        if (pmiStats != null) {
                            pmiStats.writeTimes(objbuf.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                        }
                    }
                }

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, propsToWrite.size() + " property writes are done");

                if (appDataTablesPerThread) {
                    if (session.appDataChanges != null)
                        session.appDataChanges.remove(t);
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "remove thread from appDataChanges for thread", t);
                    }
                } else { //appDataTablesPerSession
                    if (session.appDataChanges != null)
                        session.appDataChanges.clear();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "clearing appDataChanges");
                    }
                }
            }

            // see if any properties were REMOVED.
            // if so, process them

            Set<String> propsToRemove = null;

            if (session.appDataRemovals != null) {
                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    propsToRemove = session.appDataRemovals.keySet();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "doing app removals for " + id + " on ALL threads");
                    }
                } else { //appDataTablesPerThread
                    if ((tht = (Hashtable) session.appDataRemovals.get(t)) != null) {
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "doing app removals for " + id + " on thread ", t);
                        }
                        propsToRemove = tht.keySet();
                    }
                }

                if (propsToRemove != null && !propsToRemove.isEmpty()) {
                    for (String propid : propsToRemove) {
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "deleting prop " + propid + " for session " + id);
                        }
                        String key = createSessionAttributeKey(id, propid);

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionAttrCache, "remove", key);

                        boolean removed = sessionAttributeCache.remove(key);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionAttrCache, "remove", removed);
                    }
                }

                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    if (session.appDataRemovals != null)
                        session.appDataRemovals.clear();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "clearing appDataRemovals");
                    }
                } else { //appDataTablesPerThread
                    if (session.appDataRemovals != null)
                        session.appDataRemovals.remove(t);
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "remove thread from appDataRemovals", t);
                    }
                }
            }

            // Update the session's main cache entry per the identified changes
            if (propsToWrite != null || propsToRemove != null) {
                ArrayList<?> oldValue, newValue;
                long backoff = 20; // allows first two attempts without delay, then a delay of 160-319ms, then a delay of 320-639 ms, ...
                for (boolean replaced = false; !replaced; ) {
                    if ((backoff *= 2) > 100)
                        try {
                            // TODO remove this error and switch to enforce a maximum on backoff time
                            // This error is temporarily here to identify how often this is reached
                            if (backoff > 500)
                                throw new RuntimeException("Giving up on retries"); 
                            TimeUnit.MILLISECONDS.sleep(backoff + (long) Math.random() * backoff);
                        } catch (InterruptedException x) {
                            FFDCFilter.processException(x, getClass().getName(), "324", new Object[] { id, backoff, propsToWrite, propsToRemove });
                            throw new RuntimeException(x);
                        }
                    if (trace && tc.isDebugEnabled())
                        tcInvoke(tcSessionMetaCache, "get", id);

                    oldValue = sessionMetaCache.get(id);

                    if (trace && tc.isDebugEnabled())
                        tcReturn(tcSessionMetaCache, "get", oldValue);
                    if (oldValue == null) 
                        {break;} // TODO implement code path where cache entry for session is expired. Delete the property entries?
                    SessionInfo sessionInfo = new SessionInfo(oldValue).clone();
                    if (propsToWrite != null)
                        sessionInfo.addSessionPropertyIds(propsToWrite);
                    if (propsToRemove != null)
                        sessionInfo.removeSessionPropertyIds(propsToRemove);
                    newValue = sessionInfo.getArrayList();

                    if (trace && tc.isDebugEnabled())
                        tcInvoke(tcSessionMetaCache, "replace", id, oldValue, newValue);

                    replaced = sessionMetaCache.replace(id, oldValue, newValue);

                    if (trace && tc.isDebugEnabled())
                        tcReturn(tcSessionMetaCache, "replace", replaced);
                }
            }
        } catch (IOException x) {
            FFDCFilter.processException(x, getClass().getName(), "256", session);
            throw new RuntimeException(x);
        }

        return true;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#insertSession(com.ibm.ws.session.store.common.BackedSession)
     */
    @Override
    protected void insertSession(BackedSession session) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // TODO rewrite this. For now, it is copied based on DatabaseHashMap.insertSession
        String id = session.getId();

        listenerFlagUpdate(session);

        long tmpCreationTime = session.getCreationTime();
        session.setLastWriteLastAccessTime(tmpCreationTime);

        SessionInfo sessionInfo = new SessionInfo(tmpCreationTime, session.getMaxInactiveInterval(), session.listenerFlag, session.getUserName());
        ArrayList<Object> list = sessionInfo.getArrayList();

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "putIfAbsent", id, list);

        boolean added = sessionMetaCache.putIfAbsent(id, list);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "putIfAbsent", added);

        if (!added)
            throw new IllegalStateException("Cache already contains " + id);

        session.needToInsert = false;

        removeFromRecentlyInvalidatedList(id);

        session.update = null;
        session.userWriteHit = false;
        session.maxInactWriteHit = false;
        session.listenCntHit = false;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#isPresent(java.lang.String)
     */
    @Override
    protected boolean isPresent(String id) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "containsKey", id);

        boolean contains = sessionMetaCache.containsKey(id);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "containsKey", contains);
        return contains;
    }

    /**
     * Copied from DatabaseHashMap.
     * Attempts to get the requested attr from the cache
     * Returns null if attr doesn't exist
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
        if (!((CacheSession) sess).getPopulatedAppData()) {
            String id = sess.getId();
            String appName = getIStore().getId();

            String key = createSessionAttributeKey(id, attrName);

            if (trace && tc.isDebugEnabled())
                tcInvoke(tcSessionAttrCache, "get", key);

            byte[] bytes = sessionAttributeCache.get(key);

            if (bytes == null || bytes.length == 0) {
                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionAttrCache, "get", bytes);
            } else {
                long startTime = System.nanoTime();

                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes));
                try {
                    try {
                        value = ((CacheStore) getIStore()).getLoader().loadObject(in);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionAttrCache, "get", bytes, value);
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException x) {
                    if (trace && tc.isDebugEnabled())
                        tcReturn(tcSessionAttrCache, "get", bytes);
                    FFDCFilter.processException(x, getClass().getName(), "197", sess, new Object[] { Arrays.toString(bytes) });
                    throw new RuntimeException(x);
                }

                SessionStatistics pmiStats = getIStore().getSessionStatistics();
                if (pmiStats != null) {
                    pmiStats.readTimes(bytes == null ? 0 : bytes.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                }
            }

            // Before returning the value, confirm that the session hasn't expired
            if (trace && tc.isDebugEnabled())
                tcInvoke(tcSessionMetaCache, "containsKey", id);

            boolean contains = sessionMetaCache.containsKey(id);

            if (trace && tc.isDebugEnabled())
                tcReturn(tcSessionMetaCache, "containsKey", contains);

            if (!contains) {
                value = null;
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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = sess.getId();

        int updateCount;

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "get", id);

        ArrayList<?> oldValue = sessionMetaCache.get(id);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "get", oldValue);

        SessionInfo sessionInfo = oldValue == null ? null : new SessionInfo(oldValue).clone();
        synchronized (sess) {
            if (sessionInfo == null || sessionInfo.getLastAccess() != sess.getCurrentAccessTime() || sessionInfo.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionInfo.setLastAccess(nowTime);
                ArrayList<?> newValue = sessionInfo.getArrayList();

                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "replace", id, oldValue, newValue);

                boolean replaced = sessionMetaCache.replace(id, oldValue, newValue);

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "replace", replaced);

                if (replaced) {
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
                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "get", INVAL_KEY);

                ArrayList<?> oldValue = sessionMetaCache.get(INVAL_KEY);

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "get", oldValue);

                if (oldValue == null) {
                    // If we are here, it means this is the first time this web module is
                    // trying to perform invalidation of sessions
                    SessionInfo sessionInfo = new SessionInfo(now, // last access
                                                              -1, // max inactive time,
                                                              (short) 0, // listener count 
                                                              null); // user name
                    ArrayList<Object> newValue = sessionInfo.getArrayList();

                    if (trace && tc.isDebugEnabled())
                        tcInvoke(tcSessionMetaCache, "put", INVAL_KEY, newValue);

                    sessionMetaCache.put(INVAL_KEY, newValue);

                    if (trace && tc.isDebugEnabled())
                        tcReturn(tcSessionMetaCache, "put");

                    doInvals = true;
                } else {
                    SessionInfo sessionInfo = new SessionInfo(oldValue);
                    long lastTime = sessionInfo.getLastAccess();

                    long lastCheck = now - _smc.getInvalidationCheckInterval() * 1000;

                    //check the value of lastCheck,lastTime,now
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "lastCheck/lastTime/now", lastCheck, lastTime, now);

                    //if no other server tried to process invalidation within the interval, this will be true
                    //similar to updateNukerTimeStamp, but we have an extra check here to test the last access time hasn't changed
                    if (lastCheck >= lastTime || lastTime > now) {
                        sessionInfo = sessionInfo.clone();
                        sessionInfo.setLastAccess(now);
                        ArrayList<Object> newValue = sessionInfo.getArrayList();

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionMetaCache, "replace", INVAL_KEY, oldValue, newValue);

                        doInvals = sessionMetaCache.replace(INVAL_KEY, oldValue, newValue);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionMetaCache, "replace", doInvals);
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
    @FFDCIgnore(Exception.class) // FFDC logged manually with extra info
    @Override
    protected boolean persistSession(BackedSession session, boolean propHit) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = session.getId();

        try {
            // if nothing changed, then just return
            if (!session.userWriteHit && !session.maxInactWriteHit && !session.listenCntHit && _smc.getEnableEOSWrite() && !_smc.getScheduledInvalidation() && !propHit) {
                session.update = null;
                session.userWriteHit = false;
                session.maxInactWriteHit = false;
                session.listenCntHit = false;
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "no changes");
                return true;
            }

            if (propHit && !handlePropertyHits(session)) {
                return false;
            }

            for (boolean updated = false; !updated;) {
                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "get", id);

                ArrayList<?> oldValue = sessionMetaCache.get(id);

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "get", oldValue);

                if (oldValue == null)
                    return false;

                SessionInfo sessionInfo = new SessionInfo(oldValue).clone();

                if (session.userWriteHit) {
                    session.userWriteHit = false;
                    sessionInfo.setUser(session.getUserName());
                }

                if (session.maxInactWriteHit) {
                    session.maxInactWriteHit = false;
                    sessionInfo.setMaxInactiveTime(session.getMaxInactiveInterval());
                }

                if (session.listenCntHit) {
                    session.listenCntHit = false;
                    sessionInfo.setListenerTypes(session.listenerFlag);
                }

                long time = session.getCurrentAccessTime();
                if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                    session.setLastWriteLastAccessTime(time);
                    sessionInfo.setLastAccess(time);
                }

                if (trace & tc.isDebugEnabled())
                    Tr.debug(this, tc, id, sessionInfo);

                ArrayList<?> newValue = sessionInfo.getArrayList();

                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "replace", id, oldValue, newValue);

                updated = sessionMetaCache.replace(id, oldValue, newValue);

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "replace", updated);
            }
        } catch (Exception ee) {
            FFDCFilter.processException(ee, getClass().getName(), "272", session);
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

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "iterator");

        @SuppressWarnings("rawtypes")
        Iterator<Cache.Entry<String, ArrayList>> it = sessionMetaCache.iterator();

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "iterator", it);

        while (it.hasNext()) {
            if (trace && tc.isDebugEnabled())
                tcInvoke(tcSessionMetaCache, "_iterator.next");

            @SuppressWarnings("rawtypes")
            Cache.Entry<String, ArrayList> entry = it.next();
            String id = entry == null ? null : entry.getKey();
            ArrayList<?> value = entry.getValue();

            if (trace && tc.isDebugEnabled())
                tcReturn(tcSessionMetaCache, "_iterator.next", id, value);

            if (id != null && !INVAL_KEY.equals(id)) {
                SessionInfo sessionInfo = new SessionInfo(value);
                long lastAccess = sessionInfo.getLastAccess();
                short listenerTypes = sessionInfo.getListenerTypes();
                int maxInactive = sessionInfo.getMaxInactiveTime();
                if ((listenerTypes & BackedSession.HTTP_SESSION_BINDING_LISTENER) != 0 // sessions that DO have binding listeners
                                && maxInactive >= 0
                                && maxInactive < (start - lastAccess) / 1000) {

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "processInvalidListeners for sessionID=" + id);

                    CacheSession session = new CacheSession(this, id, _iStore.getStoreCallback());
                    session.initSession(_iStore);
                    session.setIsValid(true);
                    session.setIsNew(false);
                    session.updateLastAccessTime(lastAccess);
                    session.setCreationTime(sessionInfo.getCreationTime());
                    session.internalSetMaxInactive(maxInactive);
                    session.internalSetUser(sessionInfo.getUser());
                    session.setListenerFlag(listenerTypes);

                    long now = System.currentTimeMillis();
                    //handle the subset of session listeners

                    lastAccess = session.getCurrentAccessTime(); // try using lastTouch again..

                    try {
                        // get the session ready and read in any listeners
                        session.setIsNew(false);
                        session.getSwappableListeners(BackedSession.HTTP_SESSION_BINDING_LISTENER);

                        sessionInfo = sessionInfo.clone();
                        sessionInfo.setLastAccess(lastAccess);

                        // only invalidate those which have not been accessed since
                        // check in computeInvalidList

                        ArrayList<Object> list = sessionInfo.getArrayList();

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionMetaCache, "remove", id, list);

                        boolean removed = sessionMetaCache.remove(id, list);

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionMetaCache, "remove", removed);

                        if (removed) {
                            // return of session done as a result of this call
                            session.internalInvalidate(true);

                            Set<String> propIds = sessionInfo.getSessionPropertyIds();
                            if (propIds != null && !propIds.isEmpty()) {
                                HashSet<String> propKeys = new HashSet<String>();
                                for (String propId : propIds)
                                    propKeys.add(createSessionAttributeKey(id, propId));

                                if (trace && tc.isDebugEnabled())
                                    tcInvoke(tcSessionAttrCache, "removeAll", propKeys);

                                sessionAttributeCache.removeAll(propKeys);

                                if (trace && tc.isDebugEnabled())
                                    tcReturn(tcSessionAttrCache, "removeAll");
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
                        FFDCFilter.processException(e, getClass().getName(), "652", session);
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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        CacheSession sess = null;

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "get", id);

        ArrayList<?> value = sessionMetaCache.get(id);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "get", value);

        if (value != null) {
            SessionInfo sessionInfo = new SessionInfo(value);
            sess = new CacheSession(this, id, getIStore().getStoreCallback());
            sess.updateLastAccessTime((Long) sessionInfo.getLastAccess());
            sess.setCreationTime((Long) sessionInfo.getCreationTime());
            sess.internalSetMaxInactive((Integer) sessionInfo.getMaxInactiveTime());
            sess.internalSetUser((String) sessionInfo.getUser());
            sess.setIsValid(true);
            sess.setListenerFlag((Short) sessionInfo.getListenerTypes());
        }
        return sess;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        //If the app calls invalidate, it may not be removed from the local cache yet.
        superRemove(id);

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "getAndRemove", id);

        ArrayList<?> removed = sessionMetaCache.getAndRemove(id);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "getAndRemove", removed);

        addToRecentlyInvalidatedList(id);

        Set<String> propIds = removed == null ? null : new SessionInfo(removed).getSessionPropertyIds();
        if (propIds != null) {
            for (String propId : propIds) {
                String attributeKey = createSessionAttributeKey(id, propId);

                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionAttrCache, "remove", attributeKey);

                sessionAttributeCache.remove(attributeKey);

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionAttrCache, "remove");
            }
        }
    }

    /**
     * Generates a trace string of the form,
     * ==> Instance12345678.methName arg0, arg1, byte[22]: DeserializedFormOfArg2Bytes
     * 
     * @param instance instance
     * @param methName method name
     * @param args method arguments
     */
    @Trivial
    static final void tcInvoke(String instance, String methName, Object... args) {
        StringBuilder b = new StringBuilder("==> ").append(instance);
        if (methName.charAt(0) != '_')
            b.append('.');
        b.append(methName).append(' ');
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                b.append(", ");
            if (args[i] instanceof byte[]) {
                byte[] bytes = (byte[]) args[i];
                b.append("byte[").append(bytes.length).append("]");
                if (++i < args.length && bytes.length < 1000)
                    b.append(": ").append(args[i]);
            } else {
                if (args[i] instanceof Collection)
                    b.append(EOLN).append(' ');
                b.append(args[i]);
            }
        }
        Tr.debug(tc, b.toString());
    }

    /**
     * Generates a trace string of the form,
     * <== Instance12345678.methName byte[100]: DeserializedFormOfResult
     *
     * @param instance instance
     * @param methName method name
     * @param result first argument is the result or bytes representing the result. If the first argument is bytes, then the second argument is the result.
     */
    @Trivial
    static final void tcReturn(String instance, String methName, Object... result) {
        StringBuilder b = new StringBuilder("<== ").append(instance);
        if (methName.charAt(0) != '_')
            b.append('.');
        b.append(methName).append(' ');
        if (result == null) {
            b.append("null");
        } else if (result.length > 0) {
            byte[] bytes;
            if (result[0] instanceof byte[]) {
                bytes = (byte[]) result[0];
                b.append("byte[").append(bytes.length).append("]");
            } else {
                bytes = null;
                if (result[0] instanceof Collection)
                    b.append(EOLN).append(' ');
                b.append(result[0]);
            }
            if (result.length > 1 && (bytes == null || bytes.length < 1000)) {
                b.append(bytes == null ? ' ' : ':');
                if (result[1] instanceof Collection)
                    b.append(EOLN);
                b.append(' ');
                b.append(result[1]);
            }
        }
        Tr.debug(tc, b.toString());
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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = sess.getId();

        int updateCount = -1;

        while (updateCount == -1) {
            if (trace && tc.isDebugEnabled())
                tcInvoke(tcSessionMetaCache, "get", id);

            ArrayList<?> oldValue = sessionMetaCache.get(id);

            if (trace && tc.isDebugEnabled())
                tcReturn(tcSessionMetaCache, "get", oldValue);

            SessionInfo sessionInfo = oldValue == null ? null : new SessionInfo(oldValue).clone();
            if (sessionInfo == null || sessionInfo.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionInfo.setLastAccess(nowTime);
                ArrayList<Object> newValue = sessionInfo.getArrayList();

                if (trace && tc.isDebugEnabled())
                    tcInvoke(tcSessionMetaCache, "replace", id, oldValue, newValue);

                if (sessionMetaCache.replace(id, oldValue, newValue))
                    updateCount = 1;

                if (trace && tc.isDebugEnabled())
                    tcReturn(tcSessionMetaCache, "replace", updateCount == 1);
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
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        long now = System.currentTimeMillis();
        SessionInfo sessionInfo = new SessionInfo(now, // last access
                                                  -1, // max inactive time,
                                                  (short) 0, // listener count 
                                                  null); // user name
        ArrayList<Object> newValue = sessionInfo.getArrayList();

        if (trace && tc.isDebugEnabled())
            tcInvoke(tcSessionMetaCache, "put", INVAL_KEY, newValue);

        sessionMetaCache.put(INVAL_KEY, newValue);

        if (trace && tc.isDebugEnabled())
            tcReturn(tcSessionMetaCache, "put");
    }

    /**
     * Copied from DatabaseHashMap.writeCachedLastAccessedTimes.
     * writeCachedLastAccessedTimes - if we have manual writes of time-based writes, we cache the last
     * accessed times and only write them to the persistent store prior to the inval thread running.
     */
    private void writeCachedLastAccessedTimes() throws Exception {
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
                        tcInvoke(tcSessionMetaCache, "get", id);

                    ArrayList<?> oldValue = sessionMetaCache.get(id);

                    if (trace && tc.isDebugEnabled())
                        tcReturn(tcSessionMetaCache, "get", oldValue);

                    SessionInfo sessionInfo = oldValue == null ? null : new SessionInfo(oldValue).clone();
                    if (sessionInfo == null || sessionInfo.getLastAccess() >= time) {
                        updateCount = 0;
                    } else {
                        sessionInfo.setLastAccess(time);
                        ArrayList<Object> newValue = sessionInfo.getArrayList();

                        if (trace && tc.isDebugEnabled())
                            tcInvoke(tcSessionMetaCache, "replace", id, oldValue, newValue);

                        if (sessionMetaCache.replace(id, oldValue, newValue))
                            updateCount = 1;

                        if (trace && tc.isDebugEnabled())
                            tcReturn(tcSessionMetaCache, "replace", updateCount == 1);
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
