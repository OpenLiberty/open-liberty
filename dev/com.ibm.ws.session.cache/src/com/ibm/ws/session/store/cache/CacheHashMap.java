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
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
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
     * Indices and size for ArrayList values that are stored in the cache.
     * 
     * Data types are:
     * long   - CREATION_TIME
     * long   - LAST_ACCESS
     * int    - MAX_INACTIVE_TIME
     * short  - LISTENER_COUNT
     * String - USER
     * BitSet - BITS
     * 
     * BitSet is used to wrap the byte[] because it is both Serializable and supports a .equals comparison that matches
     * its contents (and is provided by the JDK, so it can deserialize on the server side).
     * Direct use of byte[] would not be valid because its .equals method is an instance comparison.
     * TODO it will hopefully be possible to switch to byte[] once other fields are removed
     * such that we no longer require Cache.replace operations. 
     */
    static int CREATION_TIME = 0, LAST_ACCESS = 1, MAX_INACTIVE_TIME = 2, LISTENER_COUNT = 3, USER = 4, BITS = 5, SIZE = 6; 

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
     * Create a key for a session, of the form: SessionId@Application
     * 
     * @param sessionId the session id
     * @param app the application
     * @return the key
     */
    static final String createSessionKey(String sessionId, String app) {
        return new StringBuilder(sessionId.length() + 1 + app.length())
                        .append(sessionId)
                        .append('@')
                        .append(app)
                        .toString();
    }

    /**
     * Create a key for a session property, of the form: SessionId.PropertyId
     * 
     * @param sessionId the session id
     * @param propertyId the session property
     * @return the key
     */
    static final String createSessionPropertyKey(String sessionId, String propertyId) {
        return new StringBuilder(sessionId.length() + 1 + propertyId.length())
                        .append(sessionId)
                        .append('.')
                        .append(propertyId)
                        .toString();
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

        String key = createSessionKey(id, getIStore().getId());
        ArrayList<?> sessionData = cacheStoreService.cache.get(key);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key.toString(), sessionData);

        if (sessionData == null)
            return null;

        long startTime = System.nanoTime();
        BitSet bitset = (BitSet) sessionData.get(BITS);
        byte[] bytes = bitset == null ? null : bitset.toByteArray();

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
        String key = createSessionKey(d2.getId(), d2.getAppName());

        listenerFlagUpdate(d2);

        long tmpCreationTime = d2.getCreationTime();
        d2.setLastWriteLastAccessTime(tmpCreationTime);

        // When adding array elements, order must match the numeric value of the constants
        ArrayList<Object> sessionData = new ArrayList<Object>(SIZE);
        sessionData.add(tmpCreationTime); // CREATION_TIME
        sessionData.add(tmpCreationTime); // LAST_ACCESS
        sessionData.add(d2.getMaxInactiveInterval()); // MAX_INACTIVE_TIME
        sessionData.add(d2.listenerFlag); // LISTENER_COUNT
        sessionData.add(d2.getUserName()); // USER
        sessionData.add(null); // BITS

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

            String key = createSessionPropertyKey(id, attrName);
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
        String key = createSessionKey(id, sess.getAppName());

        int updateCount;

        ArrayList<?> oldSessionData = cacheStoreService.cache.get(key);
        synchronized (sess) {
            if (oldSessionData == null || (Long) oldSessionData.get(LAST_ACCESS) != sess.getCurrentAccessTime() || (Long) oldSessionData.get(LAST_ACCESS) == nowTime) {
                updateCount = 0;
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<Object> newSessionData = (ArrayList<Object>) oldSessionData.clone();
                newSessionData.set(LAST_ACCESS, nowTime);

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
        String key = createSessionKey(id, d2.getAppName());

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
                ArrayList<?> oldSessionData = cacheStoreService.cache.get(key);
                if (oldSessionData == null)
                    return false;

                @SuppressWarnings("unchecked")
                ArrayList<Object> newSessionData = (ArrayList<Object>) oldSessionData.clone();

                if (d2.userWriteHit) {
                    d2.userWriteHit = false;
                    newSessionData.set(USER, d2.getUserName());
                }

                if (d2.maxInactWriteHit) {
                    d2.maxInactWriteHit = false;
                    newSessionData.set(MAX_INACTIVE_TIME, d2.getMaxInactiveInterval());
                }

                if (d2.listenCntHit) {
                    d2.listenCntHit = false;
                    newSessionData.set(LISTENER_COUNT, d2.listenerFlag);
                }

                long time = d2.getCurrentAccessTime();
                if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                    d2.setLastWriteLastAccessTime(time);
                    newSessionData.set(LAST_ACCESS, time);
                }

                if (propHit && !_smc.isUsingMultirow()) {
                    newSessionData.set(BITS, BitSet.valueOf(objbuf));
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
        String key = createSessionKey(id, appName);

        CacheSession sess = null;
        ArrayList<?> sessionData = cacheStoreService.cache.get(key);
        if (sessionData != null) {
            sess = new CacheSession(this, id, getIStore().getStoreCallback());
            sess.updateLastAccessTime((Long) sessionData.get(LAST_ACCESS));
            sess.setCreationTime((Long) sessionData.get(CREATION_TIME));
            sess.internalSetMaxInactive((Integer) sessionData.get(MAX_INACTIVE_TIME));
            sess.internalSetUser((String) sessionData.get(USER));
            sess.setIsValid(true);
            sess.setListenerFlag((Short) sessionData.get(LISTENER_COUNT));
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

        String key = createSessionKey(id, _iStore.getId());

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
        String key = createSessionKey(id, appName);

        int updateCount = -1;

        while (updateCount == -1) {
            ArrayList<?> oldSessionData = cacheStoreService.cache.get(key);
            if (oldSessionData == null || (Long) oldSessionData.get(LAST_ACCESS) == nowTime) {
                updateCount = 0;
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<Object> newSessionData = (ArrayList<Object>) oldSessionData.clone();
                newSessionData.set(LAST_ACCESS, nowTime);
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
