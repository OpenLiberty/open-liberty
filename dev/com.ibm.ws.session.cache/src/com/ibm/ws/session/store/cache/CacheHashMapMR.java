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
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStore;

/**
 * Hash map backed by JCache, with cache entry per session property.
 * TODO most methods are not implemented
 */
public class CacheHashMapMR extends CacheHashMap {
    private static final long serialVersionUID = 1L; // not serializable, rejects writeObject

    private static final TraceComponent tc = Tr.register(CacheHashMapMR.class);

    public CacheHashMapMR(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc, cacheStoreService);
        // We know we're running multi-row..if not writeAllProperties and not time-based writes,
        // we must keep the app data tables per thread (rather than per session)
        appDataTablesPerThread = (!_smc.writeAllProperties() && !_smc.getEnableTimeBasedWrite());
    }

    /**
     * Loads all the properties.
     * Copied from DatabaseHashMapMR.
     */
    protected Object getAllValues(BackedSession sess) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String id = sess.getId();
        String appName = getAppName();

        long startTime = System.nanoTime();
        long readSize = 0;

        String sessionKey = createSessionKey(id, appName);
        ArrayList<?> list = cacheStoreService.cache.get(sessionKey);
        Set<String> propIds = list == null ? null : new SessionInfo(list).getSessionPropertyIds();

        Hashtable<String, Object> h = new Hashtable<String, Object>();
        try {
            if (propIds != null) {
                Cache<String, byte[]> cache = cacheStoreService.getCache(appName);
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

                    String propertyKey = createSessionPropertyKey(id, propId);
                    byte[] b = cache.get(propertyKey);

                    if (b != null) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(b);
                        BufferedInputStream bis = new BufferedInputStream(bais);
                        Object obj;
                        try {
                            obj = ((CacheStore) getIStore()).getLoader().loadObject(bis);
                            readSize += b.length;
                        } catch (ClassNotFoundException x) {
                            FFDCFilter.processException(x, getClass().getName(), "91", sess);
                            throw new RuntimeException(x);
                        }

                        bis.close();
                        bais.close();

                        if (obj != null) {
                            if (trace && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "put prop in mSwappableData: " + propId);
                            }
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
            FFDCFilter.processException(e, getClass().getName(), "112", sess);
            throw new RuntimeException(e);
        }
        return h;
    }

    @Override
    protected Object getValue(String id, BackedSession s) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String sessId = s.getId();
        String appName = getIStore().getId();
        Object tmp = null;

        String key = createSessionPropertyKey(sessId, id);
        byte[] sessionPropBytes = cacheStoreService.getCache(appName).get(key);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key.toString(), sessionPropBytes == null ? null : sessionPropBytes.length);

        if (sessionPropBytes == null)
            return null;

        long startTime = System.currentTimeMillis();

        if (sessionPropBytes != null && sessionPropBytes.length > 0) {
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(sessionPropBytes));
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
            pmiStats.readTimes(sessionPropBytes == null ? 0 : sessionPropBytes.length, System.currentTimeMillis() - startTime);
        }

        // Before returning the value, confirm that the cache entry for the session hasn't expired
        if (!cacheStoreService.cache.containsKey(createSessionKey(sessId, appName))) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, sessId + " does not appear to be a valid session for " + appName);
            tmp = null;
            throw new UnsupportedOperationException(); // TODO implement code path where cache entry for session is expired. Delete the property entry?
        }

        return tmp;
    }

    /**
     * copied from DatabaseHashMapMR
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    boolean handlePropertyHits(BackedSession d2) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Thread t = Thread.currentThread();

        String id = d2.getId();
        String appName = getAppName();

        try {
            Set<String> propsToWrite = null;

            // we are not synchronized here - were not in old code either
            Hashtable sht = null;
            if (_smc.writeAllProperties()) {
                Hashtable ht = d2.getSwappableData();
                propsToWrite = ht.keySet();
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "doing app changes for ALL mSwappable Data", ht);
                }
            } else {
                if (d2.appDataChanges != null) {
                    if (appDataTablesPerThread) {
                        if ((sht = (Hashtable) d2.appDataChanges.get(t)) != null) {
                            if (trace && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "doing app changes for " + id + " on thread " + t);
                            }
                            propsToWrite = sht.keySet();
                        }
                    } else { // appDataTablesPerSession
                        propsToWrite = d2.appDataChanges.keySet();
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "doing app changes for TimeBasedWrite");
                        }
                    }
                }
            }

            if (propsToWrite != null) {
                Cache<String, byte[]> cache = cacheStoreService.getCache(appName);

                for (String propid : propsToWrite) {
                    long startTime = System.nanoTime();

                    if (id.equals(propid)) {
                        throw new IllegalArgumentException(propid); // internal error, should never occur
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    ObjectOutputStream oos = cacheStoreService.serializationService.createObjectOutputStream(baos);
                    oos.writeObject(d2.getSwappableData().get(propid));
                    oos.flush();

                    int size = baos.size();
                    byte[] objbuf = baos.toByteArray();

                    oos.close();
                    baos.close();

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "before update " + propid + " for session " + id + " size " + size);

                    String key = createSessionPropertyKey(id, propid);
                    cache.put(key, objbuf);

                    SessionStatistics pmiStats = _iStore.getSessionStatistics();
                    if (pmiStats != null) {
                        pmiStats.writeTimes(objbuf.length, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                    }
                }

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, propsToWrite.size() + " property writes are done");

                if (appDataTablesPerThread) {
                    if (d2.appDataChanges != null)
                        d2.appDataChanges.remove(t);
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "remove thread from appDataChanges for thread", t);
                    }
                } else { //appDataTablesPerSession
                    if (d2.appDataChanges != null)
                        d2.appDataChanges.clear();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "clearing appDataChanges");
                    }
                }
            }

            // see if any properties were REMOVED.
            // if so, process them

            Set<String> propsToRemove = null;

            if (d2.appDataRemovals != null) {
                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    propsToRemove = d2.appDataRemovals.keySet();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "doing app removals for " + id + " on ALL threads");
                    }
                } else { //appDataTablesPerThread
                    if ((sht = (Hashtable) d2.appDataRemovals.get(t)) != null) {
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "doing app removals for " + id + " on thread ", t);
                        }
                        propsToRemove = sht.keySet();
                    }
                }

                if (propsToRemove != null && !propsToRemove.isEmpty()) {
                    Cache<String, byte[]> cache = cacheStoreService.getCache(appName);

                    for (String propid : propsToRemove) {
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "deleting prop " + propid + " for session " + id);
                        }
                        String key = createSessionPropertyKey(id, propid);
                        cache.remove(key);
                    }
                }

                if (!appDataTablesPerThread) { // appDataTablesPerSession
                    if (d2.appDataRemovals != null)
                        d2.appDataRemovals.clear();
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "clearing appDataRemovals");
                    }
                } else { //appDataTablesPerThread
                    if (d2.appDataRemovals != null)
                        d2.appDataRemovals.remove(t);
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "remove thread from appDataRemovals", t);
                    }
                }
            }

            // Update the session's main cache entry per the identified changes
            if (propsToWrite != null || propsToRemove != null) {
                String sessionKey = createSessionKey(id, appName);
                ArrayList<?> oldValue, newValue;
                do {
                    oldValue = cacheStoreService.cache.get(sessionKey);
                    if (oldValue == null)
                        throw new UnsupportedOperationException(); // TODO implement code path where cache entry for session is expired. Delete the property entries?
                    SessionInfo sessionInfo = new SessionInfo(oldValue).clone();
                    if (propsToWrite != null)
                        sessionInfo.addSessionPropertyIds(propsToWrite);
                    if (propsToRemove != null)
                        sessionInfo.removeSessionPropertyIds(propsToRemove);
                    newValue = sessionInfo.getArrayList();
                } while (!cacheStoreService.cache.replace(sessionKey, oldValue, newValue));
            }
        } catch (IOException x) {
            FFDCFilter.processException(x, getClass().getName(), "256", d2);
            throw new RuntimeException(x);
        }

        return true;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        //If the app calls invalidate, it may not be removed from the local cache yet.
        superRemove(id);

        String sessionKey = createSessionKey(id, _iStore.getId());
        ArrayList<?> removed = cacheStoreService.cache.getAndRemove(sessionKey);

        addToRecentlyInvalidatedList(id);

        Set<String> propIds = removed == null ? null : new SessionInfo(removed).getSessionPropertyIds();
        if (propIds != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "remove session properties", propIds);
            Cache<String, byte[]> cache = cacheStoreService.getCache(_iStore.getId());
            for (String propId : propIds) {
                String propertyKey = createSessionPropertyKey(id, propId);
                cache.remove(propertyKey);
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
