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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.util.logging.Level;

import javax.cache.CacheException;

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

    public CacheHashMap(IStore store, SessionManagerConfig smc, CacheStoreService cacheStoreService) {
        super(store, smc);
        this.cacheStoreService = cacheStoreService;
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
     * @see com.ibm.ws.session.store.common.BackedHashMap#persistSession(com.ibm.ws.session.store.common.BackedSession, boolean)
     */
    @Override
    protected boolean persistSession(BackedSession d2, boolean propHit) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#readFromExternal(java.lang.String)
     */
    @Override
    protected BackedSession readFromExternal(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedHashMap#removePersistedSession(java.lang.String)
     */
    @Override
    protected void removePersistedSession(String id) {
        throw new UnsupportedOperationException();
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
