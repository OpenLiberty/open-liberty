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
     * Array indices (and size) for Object array values that are stored in the cache.
     * TODO this is a temporary approach that will almost certainly be replaced
     */
    private static final int LISTENER_COUNT = 0, LAST_ACCESS = 1, CREATION_TIME = 2, MAX_INACTIVE_TIME = 3, USER_NAME = 4, BYTES = 5, SIZE = 6;

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
        return false; // TODO implement
    }

    /**
     * TODO rewrite this. For now, it is copied based on DatabaseHashMap.getValue
     */
    protected Object getValue(String id, BackedSession s) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Object tmp = null;

        String key = s.getId() + '+' + id + '@' + getIStore().getId();
        Object[] value = (Object[]) cacheStoreService.cache.get(key);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, key, value);

        if (value == null)
            return null;

        long startTime = System.currentTimeMillis();
        byte[] bytes = (byte[]) value[BYTES];

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
            pmiStats.readTimes(bytes == null ? 0 : bytes.length, System.currentTimeMillis() - startTime);
        }

        return tmp;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#insertSession(com.ibm.ws.session.store.common.BackedSession)
     */
    @Override
    protected void insertSession(BackedSession d2) {
        // TODO rewrite this. For now, it is copied based on DatabaseHashMap.insertSession
        String key = d2.getId() + '+' + d2.getId() + '@' + d2.getAppName();

        listenerFlagUpdate(d2);

        long tmpCreationTime = d2.getCreationTime();
        d2.setLastWriteLastAccessTime(tmpCreationTime);

        Object[] value = new Object[SIZE];
        value[LISTENER_COUNT] = d2.listenerFlag;
        value[LAST_ACCESS] = tmpCreationTime;
        value[CREATION_TIME] = tmpCreationTime;
        value[MAX_INACTIVE_TIME] = d2.getMaxInactiveInterval();
        value[USER_NAME] = d2.getUserName();
        //value[BYTES] = null;

        if (!cacheStoreService.cache.putIfAbsent(key, value))
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
     * @see com.ibm.ws.session.store.common.BackedHashMap#loadOneValue(java.lang.String, com.ibm.ws.session.store.common.BackedSession)
     */
    @Override
    protected Object loadOneValue(String id, BackedSession bs) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#overQualLastAccessTimeUpdate(com.ibm.ws.session.store.common.BackedSession, long)
     */
    @Override
    protected int overQualLastAccessTimeUpdate(BackedSession sess, long nowTime) {
        throw new UnsupportedOperationException();
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
        String key = id + '+' + id + '@' + d2.getAppName();

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
                objbuf = serializeAppData(d2);
            }

            Object[] previous = (Object[]) cacheStoreService.cache.get(key);
            Object[] newValue = new Object[SIZE];

            long startTimeNS = System.nanoTime();

            if (d2.userWriteHit) {
                d2.userWriteHit = false;
                newValue[USER_NAME] = d2.getUserName();
            } else {
                newValue[USER_NAME] = previous[USER_NAME];
            }

            if (d2.maxInactWriteHit) {
                d2.maxInactWriteHit = false;
                newValue[MAX_INACTIVE_TIME] = d2.getMaxInactiveInterval();
            } else {
                newValue[MAX_INACTIVE_TIME] = previous[MAX_INACTIVE_TIME];
            }

            if (d2.listenCntHit) {
                d2.listenCntHit = false;
                newValue[LISTENER_COUNT] = d2.listenerFlag;
            } else {
                newValue[LISTENER_COUNT] = previous[LISTENER_COUNT];
            }

            long time = d2.getCurrentAccessTime();
            if (!_smc.getEnableEOSWrite() || _smc.getScheduledInvalidation()) {
                d2.setLastWriteLastAccessTime(time);
                newValue[LAST_ACCESS] = time;
            } else {
                newValue[LAST_ACCESS] = previous[LAST_ACCESS];
            }

            newValue[CREATION_TIME] = previous[CREATION_TIME];

            if (propHit) {
                newValue[BYTES] = objbuf;
            } else {
                newValue[BYTES] = previous[BYTES];
            }

            if (trace & tc.isDebugEnabled())
                Tr.debug(this, tc, key, newValue);

            cacheStoreService.cache.put(key, newValue);

            if (objbuf != null && propHit) {
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
        String key = id + '+' + id + '@' + appName;

        CacheSession sess = null;
        Object[] value = (Object[]) cacheStoreService.cache.get(key);
        if (value != null) {
            sess = new CacheSession(this, id, getIStore().getStoreCallback());
            long lastaccess = (Long) value[LAST_ACCESS];
            long createTime = (Long) value[CREATION_TIME];
            int maxInact = (Integer) value[MAX_INACTIVE_TIME];
            String userName = (String) value[USER_NAME];
            short listenerflag = (Short) value[LISTENER_COUNT];

            sess.updateLastAccessTime(lastaccess);
            sess.setCreationTime(createTime);
            sess.internalSetMaxInactive(maxInact);
            sess.internalSetUser(userName);
            sess.setIsValid(true);
            sess.setListenerFlag(listenerflag);
        }
        return sess;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * This type of hash map is not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
