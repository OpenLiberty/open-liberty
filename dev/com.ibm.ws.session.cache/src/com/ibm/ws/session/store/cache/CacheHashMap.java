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
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
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
        ArrayList<?> value = cacheStoreService.cache.get(key);

        if (value == null)
            return null;

        SessionData sessionData = new SessionData(value);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key.toString(), sessionData);

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
        String key = createSessionKey(d2.getId(), d2.getAppName());

        listenerFlagUpdate(d2);

        long tmpCreationTime = d2.getCreationTime();
        d2.setLastWriteLastAccessTime(tmpCreationTime);

        SessionData sessionData = new SessionData(tmpCreationTime, d2.getMaxInactiveInterval(), d2.listenerFlag, d2.getUserName());

        if (!cacheStoreService.cache.putIfAbsent(key, sessionData.getArrayList()))
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
        return cacheStoreService.cache.containsKey(createSessionKey(id, getAppName()));
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
            if (!cacheStoreService.cache.containsKey(createSessionKey(id, appName))) {
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
        String key = createSessionKey(id, sess.getAppName());

        int updateCount;

        ArrayList<?> oldValue = cacheStoreService.cache.get(key);
        SessionData sessionData = oldValue == null ? null : new SessionData(oldValue).clone();
        synchronized (sess) {
            if (sessionData == null || sessionData.getLastAccess() != sess.getCurrentAccessTime() || sessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionData.setLastAccess(nowTime);
                ArrayList<?> newValue = sessionData.getArrayList();

                if (cacheStoreService.cache.replace(key, oldValue, newValue)) {
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
    @Trivial
    protected void performInvalidation() {
        // no-op: JCache will invalidate sessions on its own
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
                ArrayList<?> oldValue = cacheStoreService.cache.get(key);
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
                    Tr.debug(this, tc, key.toString(), sessionData);

                ArrayList<?> newValue = sessionData.getArrayList();
                updated = cacheStoreService.cache.replace(key, oldValue, newValue);
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
        ArrayList<?> value = cacheStoreService.cache.get(key);
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

    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).toString();
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
            ArrayList<?> oldValue = cacheStoreService.cache.get(key);
            SessionData sessionData = oldValue == null ? null : new SessionData(oldValue).clone();
            if (sessionData == null || sessionData.getLastAccess() == nowTime) {
                updateCount = 0;
            } else {
                sessionData.setLastAccess(nowTime);
                ArrayList<Object> newValue = sessionData.getArrayList();
                if (cacheStoreService.cache.replace(key, oldValue, newValue))
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
