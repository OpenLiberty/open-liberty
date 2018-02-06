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
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStatistics;
import com.ibm.ws.session.store.cache.serializable.SessionData;
import com.ibm.ws.session.store.cache.serializable.SessionKey;
import com.ibm.ws.session.store.cache.serializable.SessionPropertyKey;
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

    // this is set to true for multirow in DatabaseHashMapMR if additional conditions are satisfied
    boolean appDataTablesPerThread = false;

    CacheStoreService cacheStoreService;
    IStore _iStore;
    SessionManagerConfig _smc;

    public CacheHashMap(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc);
        this.cacheStoreService = cacheStoreService;
        this._iStore = store;
        this._smc = smc;
        // TODO implement
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

        SessionKey key = new SessionKey(id, getIStore().getId());
        SessionData sessionData = cacheStoreService.cache.get(key);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key.toString(), sessionData);

        if (sessionData == null)
            return null;

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
        SessionKey key = new SessionKey(d2.getId(), d2.getAppName());

        listenerFlagUpdate(d2);

        long tmpCreationTime = d2.getCreationTime();
        d2.setLastWriteLastAccessTime(tmpCreationTime);

        SessionData sessionData = new SessionData();
        sessionData.setListenerCount(d2.listenerFlag);
        sessionData.setLastAccess(tmpCreationTime);
        sessionData.setCreationTime(tmpCreationTime);
        sessionData.setMaxInactiveTime(d2.getMaxInactiveInterval());
        sessionData.setUserName(d2.getUserName());

        if (!cacheStoreService.cache.putIfAbsent(key, sessionData))
            throw new IllegalStateException("Cache already contains " + key);

        d2.needToInsert = false;

        removeFromRecentlyInvalidatedList(d2.getId());

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
        throw new UnsupportedOperationException();
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

            SessionPropertyKey key = new SessionPropertyKey(id, attrName);
            byte[] bytes = cacheStoreService.getCache(appName).get(key);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, key.toString(), bytes.length);

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
        SessionKey key = new SessionKey(id, sess.getAppName());

        int updateCount;

        SessionData oldSessionData = cacheStoreService.cache.get(key);
        synchronized (sess) {
            if (oldSessionData == null || oldSessionData.getLastAccess() != sess.getCurrentAccessTime() || oldSessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                SessionData newSessionData = oldSessionData.clone();
                newSessionData.setLastAccess(nowTime);

                if (cacheStoreService.cache.replace(key, oldSessionData, newSessionData)) {
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
        throw new UnsupportedOperationException();
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
        SessionKey key = new SessionKey(id, d2.getAppName());

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
                SessionData oldSessionData = cacheStoreService.cache.get(key);
                if (oldSessionData == null)
                    return false;

                SessionData newSessionData = oldSessionData.clone();

                if (d2.userWriteHit) {
                    d2.userWriteHit = false;
                    newSessionData.setUserName(d2.getUserName());
                }

                if (d2.maxInactWriteHit) {
                    d2.maxInactWriteHit = false;
                    newSessionData.setMaxInactiveTime(d2.getMaxInactiveInterval());
                }

                if (d2.listenCntHit) {
                    d2.listenCntHit = false;
                    newSessionData.setListenerCount(d2.listenerFlag);
                }

                long time = d2.getCurrentAccessTime();
                if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                    d2.setLastWriteLastAccessTime(time);
                    newSessionData.setLastAccess(time);
                }

                if (propHit && !_smc.isUsingMultirow()) {
                    newSessionData.setBytes(objbuf);
                }

                if (trace & tc.isDebugEnabled())
                    Tr.debug(this, tc, key.toString(), newSessionData);

                updated = cacheStoreService.cache.replace(key, oldSessionData, newSessionData);
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
     * @see com.ibm.ws.session.store.common.BackedHashMap#readFromExternal(java.lang.String)
     */
    @Override
    protected BackedSession readFromExternal(String id) {
        String appName = getIStore().getId();
        SessionKey key = new SessionKey(id, appName);

        CacheSession sess = null;
        SessionData sessionData = cacheStoreService.cache.get(key);
        if (sessionData != null) {
            sess = new CacheSession(this, id, getIStore().getStoreCallback());
            sess.updateLastAccessTime(sessionData.getLastAccess());
            sess.setCreationTime(sessionData.getCreationTime());
            sess.internalSetMaxInactive(sessionData.getMaxInactiveTime());
            sess.internalSetUser(sessionData.getUserName());
            sess.setIsValid(true);
            sess.setListenerFlag(sessionData.getListenerCount());
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

        SessionKey key = new SessionKey(id, _iStore.getId());

        cacheStoreService.cache.remove(key);

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

        try {
            @SuppressWarnings("rawtypes")
            Hashtable ht;
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
        } catch (ConcurrentModificationException cme) {
            // TODO copied from DatabaseHashMap, but this seems suspicious. Need to investigate further. 
            Tr.event(this, tc,  "CacheHashMap.deferWrite", d2.getId());
        }
        return objbuf;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#updateLastAccessTime(com.ibm.ws.session.store.common.BackedSession, long)
     */
    @Override
    protected int updateLastAccessTime(BackedSession sess, long nowTime) {
        String appName = getIStore().getId();
        String id = sess.getId();
        SessionKey key = new SessionKey(id, appName);

        int updateCount = -1;

        while (updateCount == -1) {
            SessionData oldSessionData = cacheStoreService.cache.get(key);
            if (oldSessionData == null || oldSessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                SessionData newSessionData = oldSessionData.clone();
                newSessionData.setLastAccess(nowTime);
                if (cacheStoreService.cache.replace(key, oldSessionData, newSessionData))
                    updateCount = 1;
            }
        }

        return updateCount;
    }

    /**
     * This type of hash map is not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
